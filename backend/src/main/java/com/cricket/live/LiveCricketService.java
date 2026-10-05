package com.cricket.live;

import com.cricket.dto.live.LiveFeedResponse;
import com.cricket.dto.live.LiveMatch;
import com.cricket.dto.live.LiveMatchResponse;
import com.cricket.dto.live.LiveScoreResponse;
import com.cricket.dto.live.LiveScorecardResponse;
import com.cricket.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LiveCricketService {

    private static final String UNAVAILABLE_MESSAGE = "Live cricket data is temporarily unavailable.";

    private final CricketDataProvider provider;
    private final CricketApiProperties properties;
    private final Clock clock;
    private final Map<LiveFeedCategory, CachedFeed> cache = new ConcurrentHashMap<>();

    public LiveCricketService(CricketDataProvider provider, CricketApiProperties properties, Clock clock) {
        this.provider = provider;
        this.properties = properties;
        this.clock = clock;
    }

    public LiveFeedResponse getMatches() {
        return getFeed(LiveFeedCategory.LIVE);
    }

    public LiveFeedResponse getUpcoming() {
        return getFeed(LiveFeedCategory.UPCOMING);
    }

    public LiveFeedResponse getRecent() {
        return getFeed(LiveFeedCategory.RECENT);
    }

    public LiveMatchResponse getMatch(String matchId) {
        for (LiveFeedCategory category : LiveFeedCategory.values()) {
            LiveFeedResponse feed = getFeed(category);
            LiveMatch match = feed.data().stream()
                    .filter(item -> item.matchId().equals(matchId))
                    .findFirst().orElse(null);
            if (match != null) {
                return new LiveMatchResponse(feed.success(), feed.stale(), feed.message(),
                        feed.lastUpdated(), match);
            }
        }
        throw new ResourceNotFoundException("Live match not found: " + matchId);
    }

    public LiveScoreResponse getScore(String matchId) {
        LiveMatchResponse matchResponse = getMatch(matchId);
        LiveMatch match = matchResponse.data();
        return new LiveScoreResponse(matchResponse.success(), matchResponse.stale(), matchResponse.message(),
                matchResponse.lastUpdated(), match.matchId(), match.status(), match.score());
    }

    public LiveScorecardResponse getScorecard(String matchId) {
        LiveMatchResponse matchResponse = getMatch(matchId);
        LiveMatch match = matchResponse.data();
        return new LiveScorecardResponse(matchResponse.success(), matchResponse.stale(), matchResponse.message(),
                matchResponse.lastUpdated(), match.matchId(), match.innings(), match.batsmen(), match.bowlers());
    }

    private LiveFeedResponse getFeed(LiveFeedCategory category) {
        Instant now = clock.instant();
        CachedFeed current = cache.get(category);
        if (current != null && now.isBefore(current.retryAt())) {
            return response(category, current, false);
        }

        try {
            List<LiveMatch> matches = List.copyOf(provider.fetchMatches(category));
            CachedFeed refreshed = new CachedFeed(matches, now, now.plus(ttlFor(category)), true, false,
                    matches.isEmpty() ? emptyMessage(category) : null);
            cache.put(category, refreshed);
            return response(category, refreshed, false);
        } catch (RuntimeException exception) {
            String message = UNAVAILABLE_MESSAGE;
            List<LiveMatch> matches = List.of();
            Instant lastUpdated = null;
            boolean stale = false;
            if (current != null && !current.matches().isEmpty()) {
                matches = current.matches();
                lastUpdated = current.lastUpdated();
                stale = true;
                message = "Showing recently updated data; live provider is temporarily unavailable.";
            }
            CachedFeed failed = new CachedFeed(matches, lastUpdated, now.plus(ttlFor(category)), false, stale,
                    message);
            cache.put(category, failed);
            return response(category, failed, stale);
        }
    }

    private Duration ttlFor(LiveFeedCategory category) {
        long seconds = category == LiveFeedCategory.LIVE
                ? properties.getLiveCacheSeconds() : properties.getScheduleCacheSeconds();
        return Duration.ofSeconds(Math.max(1, seconds));
    }

    private String emptyMessage(LiveFeedCategory category) {
        return switch (category) {
            case LIVE -> "No live matches currently available.";
            case UPCOMING -> "No upcoming matches currently available.";
            case RECENT -> "No recent results currently available.";
        };
    }

    private LiveFeedResponse response(LiveFeedCategory category, CachedFeed feed, boolean stale) {
        String message = feed.message() == null ? emptyMessage(category) : feed.message();
        return new LiveFeedResponse(feed.available(), feed.available(), stale || feed.stale(),
                message, feed.lastUpdated(), feed.matches());
    }

    private record CachedFeed(List<LiveMatch> matches, Instant lastUpdated, Instant retryAt,
                             boolean available, boolean stale, String message) {
    }
}