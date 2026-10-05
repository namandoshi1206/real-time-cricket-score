package com.cricket;

import com.cricket.dto.live.LiveMatch;
import com.cricket.live.ConfiguredCricketApiClient;
import com.cricket.live.CricketApiProperties;
import com.cricket.live.CricketProviderUnavailableException;
import com.cricket.live.InvalidCricketProviderResponseException;
import com.cricket.live.LiveFeedCategory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ConfiguredCricketApiClientTest {

    @Test
    void mapsDocumentedProviderEnvelopeToNormalizedMatch() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        CricketApiProperties properties = configuredProperties();
        server.expect(requestTo("https://provider.example.test/matches/live"))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andRespond(withSuccess("""
                        {"data":[{"matchId":"m-1","title":"Falcons vs Lions","status":"LIVE",
                        "team1":{"id":"t-1","name":"Falcons","shortName":"FAL"},
                        "score":{"runs":84,"wickets":2,"overs":"10.3"}}]}
                        """, MediaType.APPLICATION_JSON));
        ConfiguredCricketApiClient client = new ConfiguredCricketApiClient(
                builder.build(), new ObjectMapper().findAndRegisterModules(), properties);

        LiveMatch match = client.fetchMatches(LiveFeedCategory.LIVE).getFirst();

        assertEquals("m-1", match.matchId());
        assertEquals("Falcons", match.team1().name());
        assertEquals(84, match.score().runs());
        server.verify();
    }

    @Test
    void rejectsProviderResponseWithoutDataArray() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://provider.example.test/matches/live"))
                .andRespond(withSuccess("{\"matches\":[]}", MediaType.APPLICATION_JSON));
        ConfiguredCricketApiClient client = new ConfiguredCricketApiClient(
                builder.build(), new ObjectMapper(), configuredProperties());

        assertThrows(InvalidCricketProviderResponseException.class,
                () -> client.fetchMatches(LiveFeedCategory.LIVE));
        server.verify();
    }

    @Test
    void doesNotCallProviderWithoutCredentials() {
        ConfiguredCricketApiClient client = new ConfiguredCricketApiClient(
                RestClient.builder().build(), new ObjectMapper(), new CricketApiProperties());

        assertThrows(CricketProviderUnavailableException.class,
                () -> client.fetchMatches(LiveFeedCategory.LIVE));
    }

    private CricketApiProperties configuredProperties() {
        CricketApiProperties properties = new CricketApiProperties();
        properties.setBaseUrl("https://provider.example.test");
        properties.setApiKey("test-key");
        return properties;
    }
}