package com.cricket;

import com.cricket.dto.live.LiveFeedResponse;
import com.cricket.dto.live.LiveMatch;
import com.cricket.dto.live.LiveScore;
import com.cricket.dto.live.LiveTeam;
import com.cricket.live.CricketApiProperties;
import com.cricket.live.CricketDataProvider;
import com.cricket.live.CricketProviderUnavailableException;
import com.cricket.live.LiveCricketService;
import com.cricket.live.LiveFeedCategory;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

class LiveCricketServiceTest {

    @Test
    void returnsGracefulUnavailableResponseWhenProviderIsNotConfigured() {
        CricketDataProvider provider = Mockito.mock(CricketDataProvider.class);
        when(provider.fetchMatches(LiveFeedCategory.LIVE))
                .thenThrow(new CricketProviderUnavailableException("not configured"));
        LiveCricketService service = service(provider, new MutableClock());

        LiveFeedResponse response = service.getMatches();

        assertFalse(response.success());
        assertFalse(response.available());
        assertTrue(response.data().isEmpty());
        assertTrue(response.message().contains("unavailable"));
    }

    @Test
    void treatsAnEmptySuccessfulFeedAsAvailableWithoutInventingMatches() {
        CricketDataProvider provider = Mockito.mock(CricketDataProvider.class);
        when(provider.fetchMatches(LiveFeedCategory.LIVE)).thenReturn(List.of());
        LiveCricketService service = service(provider, new MutableClock());

        LiveFeedResponse response = service.getMatches();

        assertTrue(response.success());
        assertTrue(response.available());
        assertTrue(response.data().isEmpty());
        assertEquals("No live matches currently available.", response.message());
    }

    @Test
    void cachesLiveFeedUntilItsTtlExpires() {
        CricketDataProvider provider = Mockito.mock(CricketDataProvider.class);
        when(provider.fetchMatches(LiveFeedCategory.LIVE)).thenReturn(List.of(match()));
        LiveCricketService service = service(provider, new MutableClock());

        service.getMatches();
        LiveFeedResponse cached = service.getMatches();

        assertEquals(1, cached.data().size());
        Mockito.verify(provider, Mockito.times(1)).fetchMatches(LiveFeedCategory.LIVE);
    }

    @Test
    void servesLastCachedDataAsStaleWhenRefreshFails() {
        CricketDataProvider provider = Mockito.mock(CricketDataProvider.class);
        MutableClock clock = new MutableClock();
        when(provider.fetchMatches(LiveFeedCategory.LIVE)).thenReturn(List.of(match()));
        doThrow(new CricketProviderUnavailableException("network timeout"))
                .when(provider).fetchMatches(LiveFeedCategory.LIVE);
        Mockito.reset(provider);
        when(provider.fetchMatches(LiveFeedCategory.LIVE)).thenReturn(List.of(match()))
                .thenThrow(new CricketProviderUnavailableException("network timeout"));
        LiveCricketService service = service(provider, clock);
        service.getMatches();
        clock.advanceSeconds(16);

        LiveFeedResponse response = service.getMatches();

        assertFalse(response.success());
        assertTrue(response.stale());
        assertEquals("m-1", response.data().getFirst().matchId());
        assertTrue(response.message().contains("recently updated"));
    }

    private LiveCricketService service(CricketDataProvider provider, Clock clock) {
        CricketApiProperties properties = new CricketApiProperties();
        properties.setLiveCacheSeconds(15);
        properties.setScheduleCacheSeconds(300);
        return new LiveCricketService(provider, properties, clock);
    }

    private LiveMatch match() {
        return new LiveMatch("m-1", "Falcons vs Lions", "LIVE", null,
                new LiveTeam("t-1", "Falcons", "FAL", null),
                new LiveTeam("t-2", "Lions", "LIO", null),
                new LiveScore(84, 2, "10.3", 1, null, null),
                List.of(), List.of(), List.of(), List.of(), null, Instant.parse("2026-10-05T10:00:00Z"));
    }

    private static class MutableClock extends Clock {
        private Instant instant = Instant.parse("2026-10-05T10:00:00Z");

        void advanceSeconds(long seconds) {
            instant = instant.plusSeconds(seconds);
        }

        @Override
        public ZoneId getZone() { return ZoneId.of("UTC"); }

        @Override
        public Clock withZone(ZoneId zone) { return this; }

        @Override
        public Instant instant() { return instant; }
    }
}