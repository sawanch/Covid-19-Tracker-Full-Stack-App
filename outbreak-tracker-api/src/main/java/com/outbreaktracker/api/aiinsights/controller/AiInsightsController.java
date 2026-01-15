package com.outbreaktracker.api.aiinsights.controller;

import com.outbreaktracker.api.aiinsights.model.CovidInsightsResponse;
import com.outbreaktracker.api.aiinsights.service.AiInsightsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for AI insights endpoints
 * Provides AI-generated safety recommendations and trend analysis
 * Powered by OpenAI for intelligent outbreak monitoring
 */
@RestController
@RequestMapping("/api/ai-insights")
public class AiInsightsController {

    private static final Logger logger = LoggerFactory.getLogger(AiInsightsController.class);

    private final AiInsightsService aiInsightsService;

    public AiInsightsController(AiInsightsService aiInsightsService) {
        this.aiInsightsService = aiInsightsService;
    }

    /**
     * GET /api/ai-insights/country/{countryName} - Returns AI insights for a country
     * Returns AI-generated safety recommendations and trend analysis based on respiratory outbreak data
     */
    @GetMapping("/country/{countryName}")
    public ResponseEntity<CovidInsightsResponse> getCountryInsights(@PathVariable String countryName) {
        logger.info("GET /api/ai-insights/country/{} - Fetching AI insights for country: {}", countryName, countryName);
        
        CovidInsightsResponse insights = aiInsightsService.getCountryInsights(countryName);
        return ResponseEntity.ok(insights);
    }
}
