package com.cricket.live;

import com.cricket.dto.live.LiveMatch;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.List;

@Component
public class ConfiguredCricketApiClient implements CricketDataProvider {

    private static final TypeReference<List<LiveMatch>> MATCH_LIST = new TypeReference<>() { };

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final CricketApiProperties properties;

    public ConfiguredCricketApiClient(RestClient cricketRestClient, ObjectMapper objectMapper,
                                     CricketApiProperties properties) {
        this.restClient = cricketRestClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public List<LiveMatch> fetchMatches(LiveFeedCategory category) {
        String baseUrl = properties.getBaseUrl();
        String apiKey = properties.getApiKey();
        if (baseUrl == null || baseUrl.isBlank() || apiKey == null || apiKey.isBlank()) {
            throw new CricketProviderUnavailableException("Live provider credentials/data source are not configured");
        }

        try {
            String path = properties.pathFor(category);
            URI uri = URI.create(baseUrl.replaceAll("/+$", "") + "/" + path.replaceAll("^/+", ""));
            JsonNode response = restClient.get().uri(uri)
                    .header(properties.getApiKeyHeader(), authorizationValue(apiKey))
                    .retrieve()
                    .body(JsonNode.class);
            JsonNode data = response == null ? null : response.get("data");
            if (data == null || !data.isArray()) {
                throw new InvalidCricketProviderResponseException("Provider response must contain a data array");
            }
            List<LiveMatch> matches = objectMapper.convertValue(data, MATCH_LIST);
            if (matches.stream().anyMatch(match -> match == null || blank(match.matchId())
                    || blank(match.title()) || blank(match.status()))) {
                throw new InvalidCricketProviderResponseException(
                        "Each provider match requires matchId, title, and status");
            }
            return List.copyOf(matches);
        } catch (InvalidCricketProviderResponseException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new CricketProviderUnavailableException("Live cricket provider request failed", exception);
        }
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private String authorizationValue(String apiKey) {
        String prefix = properties.getApiKeyPrefix();
        if (blank(prefix)) {
            return apiKey;
        }
        return prefix.endsWith(" ") ? prefix + apiKey : prefix + " " + apiKey;
    }
}