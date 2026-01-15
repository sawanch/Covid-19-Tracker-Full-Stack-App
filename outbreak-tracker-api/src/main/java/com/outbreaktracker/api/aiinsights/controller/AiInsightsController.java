package com.outbreaktracker.api.aiinsights.controller;

import com.outbreaktracker.api.aiinsights.model.CovidInsightsResponse;
import com.outbreaktracker.api.aiinsights.service.AiInsightsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for AI-powered respiratory outbreak insights
 * Provides endpoints for generating country-specific recommendations
 */
@RestController
@RequestMapping("/api/ai-insights")
@CrossOrigin(origins = "*")
@Tag(name = "AI Insights", description = "AI-Powered Respiratory Outbreak Analysis Endpoints")
public class AiInsightsController {
    
    private static final Logger logger = LoggerFactory.getLogger(AiInsightsController.class);
    
    private final AiInsightsService aiInsightsService;
    
    public AiInsightsController(AiInsightsService aiInsightsService) {
        this.aiInsightsService = aiInsightsService;
    }
    
    /**
     * GET /api/ai-insights/country/{countryName} - Get AI insights for a country
     */
    @GetMapping("/country/{countryName}")
    @Operation(
            summary = "Get AI insights for a country",
            description = "Returns AI-generated safety recommendations and trend analysis based on respiratory outbreak data for the specified country"
    )
    public ResponseEntity<CovidInsightsResponse> getCountryInsights(@PathVariable String countryName) {
        logger.info("GET /api/ai-insights/country/{} - Fetching AI insights", countryName);
        
        try {
            CovidInsightsResponse insights = aiInsightsService.getCountryInsights(countryName);
            return ResponseEntity.ok(insights);
        } catch (RuntimeException e) {
            logger.error("Error fetching insights for country: {}", countryName, e);
            return ResponseEntity.notFound().build();
        }
    }
}
