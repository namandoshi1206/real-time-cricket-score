package com.cricket.live;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cricket.api")
public class CricketApiProperties {

    private String baseUrl = "";
    private String apiKey = "";
    private String apiKeyHeader = "Authorization";
    private String apiKeyPrefix = "Bearer";
    private String livePath = "/matches/live";
    private String upcomingPath = "/matches/upcoming";
    private String recentPath = "/matches/recent";
    private long liveCacheSeconds = 15;
    private long scheduleCacheSeconds = 300;

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getApiKeyHeader() { return apiKeyHeader; }
    public void setApiKeyHeader(String apiKeyHeader) { this.apiKeyHeader = apiKeyHeader; }
    public String getApiKeyPrefix() { return apiKeyPrefix; }
    public void setApiKeyPrefix(String apiKeyPrefix) { this.apiKeyPrefix = apiKeyPrefix; }
    public String getLivePath() { return livePath; }
    public void setLivePath(String livePath) { this.livePath = livePath; }
    public String getUpcomingPath() { return upcomingPath; }
    public void setUpcomingPath(String upcomingPath) { this.upcomingPath = upcomingPath; }
    public String getRecentPath() { return recentPath; }
    public void setRecentPath(String recentPath) { this.recentPath = recentPath; }
    public long getLiveCacheSeconds() { return liveCacheSeconds; }
    public void setLiveCacheSeconds(long liveCacheSeconds) { this.liveCacheSeconds = Math.max(1, liveCacheSeconds); }
    public long getScheduleCacheSeconds() { return scheduleCacheSeconds; }
    public void setScheduleCacheSeconds(long scheduleCacheSeconds) { this.scheduleCacheSeconds = Math.max(1, scheduleCacheSeconds); }

    public String pathFor(LiveFeedCategory category) {
        return switch (category) {
            case LIVE -> livePath;
            case UPCOMING -> upcomingPath;
            case RECENT -> recentPath;
        };
    }
}