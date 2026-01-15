package com.covidtracker.api.aiinsights.service;

import com.covidtracker.api.aiinsights.model.CovidInsightsResponse;

/**
 * Service interface for AI-powered COVID-19 insights
 */
public interface AiInsightsService {
    
    /**
     * Generate AI-powered insights for a specific country
     * 
     * @param countryName Name of the country
     * @return COVID insights with recommendations
     */
    CovidInsightsResponse getCountryInsights(String countryName);
}
