package com.outbreaktracker.api.analytics.service.impl;

import com.outbreaktracker.api.analytics.model.AnalyticsInsightsResponse;
import com.outbreaktracker.api.analytics.model.AnalyticsInsightCard;
import com.outbreaktracker.api.analytics.service.AnalyticsService;
import com.outbreaktracker.api.analytics.service.AnalyticsAiService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * AI-powered analytics insights service implementation
 * Integrates with OpenAI API to generate intelligent system performance analysis
 * Similar architecture to AiInsightsServiceImpl but focused on system metrics
 */
@Service
public class AnalyticsAiServiceImpl implements AnalyticsAiService {

    private static final Logger logger = LoggerFactory.getLogger(AnalyticsAiServiceImpl.class);

    private final AnalyticsService analyticsService;
    private final WebClient webClient;
    private final String model;
    private final boolean aiEnabled;
    private final ObjectMapper objectMapper;

    public AnalyticsAiServiceImpl(AnalyticsService analyticsService,
                                  @Value("${openai.api.key:}") String apiKey,
                                  @Value("${openai.model:gpt-4o-mini}") String model,
                                  @Value("${openai.enabled:true}") boolean enabled) {
        this.analyticsService = analyticsService;
        this.model = model;
        this.aiEnabled = enabled && apiKey != null && !apiKey.trim().isEmpty();
        this.objectMapper = new ObjectMapper();

        if (this.aiEnabled) {
            this.webClient = WebClient.builder()
                    .baseUrl("https://api.openai.com/v1")
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .build();
            logger.info("Analytics AI service initialized with model: {}", model);
        } else {
            this.webClient = null;
            logger.warn("Analytics AI running in fallback mode (OpenAI disabled or API key not set)");
        }
    }
    
    @Override
    public AnalyticsInsightsResponse getSystemInsights() {
        logger.debug("Generating AI insights for system performance");

        // Fetch analytics data using individual service methods
        Map<String, Long> endpointStats = analyticsService.getEndpointStats();
        Map<String, Double> responseTimes = analyticsService.getResponseTimeStats();
        Map<String, Map<String, Long>> successError = analyticsService.getSuccessErrorRates();
        
        AnalyticsInsightsResponse response = new AnalyticsInsightsResponse();
        
        // Calculate total requests
        int totalRequests = endpointStats.values().stream().mapToInt(Long::intValue).sum();
        response.setTotalRequests(totalRequests);
        
        // Calculate metrics
        String slowestEndpoint = findSlowestEndpoint(responseTimes);
        Double maxResponseTime = responseTimes.values().stream().max(Double::compareTo).orElse(0.0);
        Double avgResponseTime = responseTimes.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        
        response.setSlowestEndpoint(slowestEndpoint);
        response.setAvgResponseTime(avgResponseTime);
        
        // Calculate error rate
        double errorRate = calculateOverallErrorRate(successError);
        response.setErrorRate(errorRate);

        if (aiEnabled) {
            try {
                // Call OpenAI API
                String prompt = buildAnalyticsPrompt(response.getTotalRequests(), 
                                                     slowestEndpoint, 
                                                     maxResponseTime, 
                                                     avgResponseTime, 
                                                     errorRate);
                String aiResponse = callOpenAI(prompt);
                parseAiResponse(aiResponse, response);
                
                response.setModel(model);
                response.setTemperature(0.7);
            } catch (Exception e) {
                logger.error("Error calling OpenAI for analytics insights", e);
                generateFallbackInsights(response);
            }
        } else {
            generateFallbackInsights(response);
        }
        
        response.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        return response;
    }
    
    /**
     * Builds AI prompt for system performance analysis
     */
    private String buildAnalyticsPrompt(int totalRequests, String slowestEndpoint, 
                                       double maxResponseTime, double avgResponseTime, 
                                       double errorRate) {
        String performanceLevel;
        if (avgResponseTime < 100) {
            performanceLevel = "excellent";
        } else if (avgResponseTime < 500) {
            performanceLevel = "good";
        } else if (avgResponseTime < 2000) {
            performanceLevel = "moderate";
        } else {
            performanceLevel = "poor";
        }
        
        return String.format(
                "You are a DevOps expert analyzing API performance metrics. Provide a CONCISE, PROFESSIONAL assessment.\n\n" +
                "System Metrics:\n" +
                "Total API Requests: %,d\n" +
                "Average Response Time: %.1f ms (%s performance)\n" +
                "Slowest Endpoint: %s (%.1f ms)\n" +
                "Error Rate: %.2f%%\n\n" +
                "CRITICAL INSTRUCTIONS:\n" +
                "- BE CONCISE - Use short, direct sentences\n" +
                "- Avoid verbose explanations\n" +
                "- Get straight to the point\n" +
                "- Use professional, constructive language\n" +
                "- Avoid: unacceptable, terrible, bad, poor, critical, failing\n\n" +
                "Provide a JSON response with:\n" +
                "1. overallAssessment: ONE concise sentence (max 25 words) that identifies the key issue and action needed.\n" +
                "   Example: \"System needs attention - focus on reducing the 3051ms response time and addressing the 7.29%% error rate.\"\n\n" +
                "2. recommendations: EXACTLY 2 concise recommendations:\n" +
                "   \n" +
                "   FIRST - \"Latency Issues\":\n" +
                "   - Title: \"Latency Issues\"\n" +
                "   - Description: ONE sentence (max 20 words) with specific action for endpoint: %s\n" +
                "   - Example: \"Optimize %s endpoint - investigate database queries and external API calls.\"\n" +
                "   - Emoji: ⚡\n" +
                "   \n" +
                "   SECOND - \"Error Monitoring\":\n" +
                "   - Title: \"Error Monitoring\"\n" +
                "   - Description: ONE sentence (max 20 words) about error tracking with rate: %.2f%%\n" +
                "   - Example: \"Implement error tracking to address the %.2f%% error rate and improve stability.\"\n" +
                "   - Emoji: 🔍\n\n" +
                "Return ONLY valid JSON:\n" +
                "{\n" +
                "  \"overallAssessment\": \"one concise sentence\",\n" +
                "  \"recommendations\": [\n" +
                "    {\"icon\": \"⚡\", \"title\": \"Latency Issues\", \"description\": \"one short sentence\", \"severity\": \"info|warning|success\"},\n" +
                "    {\"icon\": \"🔍\", \"title\": \"Error Monitoring\", \"description\": \"one short sentence\", \"severity\": \"info|warning|success\"}\n" +
                "  ]\n" +
                "}",
                totalRequests,
                avgResponseTime,
                performanceLevel,
                slowestEndpoint,
                maxResponseTime,
                errorRate,
                slowestEndpoint,
                slowestEndpoint,
                errorRate,
                errorRate
        );
    }
    
    /**
     * Calls OpenAI API with the constructed prompt
     */
    private String callOpenAI(String prompt) throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("temperature", 0.7);
        
        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);
        messages.add(message);
        requestBody.put("messages", messages);

        String response = webClient.post()
                .uri("/chat/completions")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        JsonNode jsonResponse = objectMapper.readTree(response);
        String content = jsonResponse.get("choices").get(0).get("message").get("content").asText();
        
        logger.debug("Received AI response for analytics insights");
        return content;
    }
    
    /**
     * Parses AI response and populates the response object
     */
    private void parseAiResponse(String aiResponse, AnalyticsInsightsResponse response) {
        try {
            // Clean the response (remove markdown code blocks if present)
            String cleanedResponse = aiResponse.trim();
            if (cleanedResponse.startsWith("```")) {
                cleanedResponse = cleanedResponse.replaceFirst("```json\\n?", "").replaceFirst("```\\n?$", "");
            }
            
            JsonNode root = objectMapper.readTree(cleanedResponse);
            
            // Parse overall assessment
            if (root.has("overallAssessment")) {
                response.setOverallAssessment(root.get("overallAssessment").asText());
            }
            
            // Parse recommendations
            if (root.has("recommendations")) {
                List<AnalyticsInsightCard> cards = new ArrayList<>();
                for (JsonNode recNode : root.get("recommendations")) {
                    AnalyticsInsightCard card = new AnalyticsInsightCard();
                    card.setIcon(recNode.get("icon").asText());
                    card.setTitle(recNode.get("title").asText());
                    card.setDescription(recNode.get("description").asText());
                    card.setSeverity(recNode.get("severity").asText());
                    cards.add(card);
                }
                response.setRecommendations(cards);
            }
            
        } catch (Exception e) {
            logger.error("Error parsing AI response", e);
            generateFallbackInsights(response);
        }
    }
    
    /**
     * Generates rule-based recommendations when AI is unavailable
     */
    private void generateFallbackInsights(AnalyticsInsightsResponse response) {
        // Concise assessment
        if (response.getAvgResponseTime() != null && response.getAvgResponseTime() > 1000) {
            response.setOverallAssessment(String.format("System needs attention - reduce %.1f ms response time and address %.2f%% error rate.",
                    response.getAvgResponseTime(), response.getErrorRate()));
        } else {
            response.setOverallAssessment("System is stable - maintain current performance and monitor for improvements.");
        }
        
        List<AnalyticsInsightCard> cards = new ArrayList<>();
        
        // 1. Latency Issues - Concise
        String latencyDescription;
        String latencySeverity;
        if (response.getAvgResponseTime() != null && response.getAvgResponseTime() > 1000) {
            latencyDescription = String.format("Optimize %s - investigate database queries and external API calls.",
                    response.getSlowestEndpoint());
            latencySeverity = "warning";
        } else {
            latencyDescription = String.format("Monitor %s endpoint to maintain optimal performance.",
                    response.getSlowestEndpoint());
            latencySeverity = "info";
        }
        cards.add(new AnalyticsInsightCard("⚡", "Latency Issues", latencyDescription, latencySeverity));
        
        // 2. Error Monitoring - Concise
        String errorDescription;
        String errorSeverity;
        if (response.getErrorRate() != null && response.getErrorRate() > 5) {
            errorDescription = String.format("Implement error tracking to address %.2f%% error rate and improve stability.",
                    response.getErrorRate());
            errorSeverity = "warning";
        } else if (response.getErrorRate() != null && response.getErrorRate() > 1) {
            errorDescription = String.format("Enhance error monitoring to maintain %.2f%% error rate.",
                    response.getErrorRate());
            errorSeverity = "info";
        } else {
            errorDescription = "Continue monitoring error patterns to maintain system reliability.";
            errorSeverity = "success";
        }
        cards.add(new AnalyticsInsightCard("🔍", "Error Monitoring", errorDescription, errorSeverity));
        
        response.setRecommendations(cards);
    }
    
    /**
     * Finds the slowest endpoint from response time stats
     */
    private String findSlowestEndpoint(Map<String, Double> responseTimes) {
        return responseTimes.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("/unknown");
    }
    
    /**
     * Calculates overall system error rate
     */
    private double calculateOverallErrorRate(Map<String, Map<String, Long>> successError) {
        long totalSuccess = 0;
        long totalError = 0;
        
        for (Map<String, Long> rates : successError.values()) {
            totalSuccess += rates.getOrDefault("success", 0L);
            totalError += rates.getOrDefault("error", 0L);
        }
        
        long total = totalSuccess + totalError;
        return total > 0 ? (totalError * 100.0) / total : 0.0;
    }
}
