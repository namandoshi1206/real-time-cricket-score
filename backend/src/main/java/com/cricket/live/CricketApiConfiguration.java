package com.cricket.live;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Clock;

@Configuration
public class CricketApiConfiguration {

    @Bean
    CricketApiProperties cricketApiProperties(
            @Value("${CRICKET_API_BASE_URL:}") String baseUrl,
            @Value("${CRICKET_API_KEY:}") String apiKey,
            @Value("${CRICKET_API_KEY_HEADER:Authorization}") String apiKeyHeader,
            @Value("${CRICKET_API_KEY_PREFIX:Bearer}") String apiKeyPrefix,
            @Value("${CRICKET_API_LIVE_PATH:/matches/live}") String livePath,
            @Value("${CRICKET_API_UPCOMING_PATH:/matches/upcoming}") String upcomingPath,
            @Value("${CRICKET_API_RECENT_PATH:/matches/recent}") String recentPath,
            @Value("${CRICKET_API_LIVE_CACHE_SECONDS:15}") long liveCacheSeconds,
            @Value("${CRICKET_API_SCHEDULE_CACHE_SECONDS:300}") long scheduleCacheSeconds) {
        CricketApiProperties properties = new CricketApiProperties();
        properties.setBaseUrl(baseUrl);
        properties.setApiKey(apiKey);
        properties.setApiKeyHeader(apiKeyHeader);
        properties.setApiKeyPrefix(apiKeyPrefix);
        properties.setLivePath(livePath);
        properties.setUpcomingPath(upcomingPath);
        properties.setRecentPath(recentPath);
        properties.setLiveCacheSeconds(liveCacheSeconds);
        properties.setScheduleCacheSeconds(scheduleCacheSeconds);
        return properties;
    }

    @Bean
    Clock liveCricketClock() {
        return Clock.systemUTC();
    }

    @Bean
    RestClient cricketRestClient(RestClient.Builder builder) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build());
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        return builder.requestFactory(requestFactory).build();
    }
}