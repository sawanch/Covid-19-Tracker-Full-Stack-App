package com.outbreaktracker.api.aiinsights.service;

import com.outbreaktracker.api.aiinsights.model.CovidInsightsResponse;

/**
 * Service interface for AI-powered respiratory outbreak insights
 */
public interface AiInsightsService {
    
    /**
     * Generate AI-powered insights for a specific country
     * 
     * @param countryName Name of the country
     * @return Respiratory outbreak insights with recommendations
     */
    CovidInsightsResponse getCountryInsights(String countryName);
}
