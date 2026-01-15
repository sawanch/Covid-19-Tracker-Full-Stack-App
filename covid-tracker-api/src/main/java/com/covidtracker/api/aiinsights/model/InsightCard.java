package com.covidtracker.api.aiinsights.model;

/**
 * Model representing a single AI-generated insight card
 * Contains title, description, icon, and severity level
 */
public class InsightCard {
    
    private String icon;
    private String title;
    private String description;
    private String severity; // info, warning, success
    
    public InsightCard() {
    }
    
    public InsightCard(String icon, String title, String description, String severity) {
        this.icon = icon;
        this.title = title;
        this.description = description;
        this.severity = severity;
    }
    
    // Getters and Setters
    public String getIcon() {
        return icon;
    }
    
    public void setIcon(String icon) {
        this.icon = icon;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getSeverity() {
        return severity;
    }
    
    public void setSeverity(String severity) {
        this.severity = severity;
    }
    
    @Override
    public String toString() {
        return "InsightCard{" +
                "icon='" + icon + '\'' +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", severity='" + severity + '\'' +
                '}';
    }
}
