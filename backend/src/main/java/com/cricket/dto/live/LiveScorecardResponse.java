package com.cricket.dto.live;

import java.time.Instant;
import java.util.List;

public record LiveScorecardResponse(
        boolean success,
        boolean stale,
        String message,
        Instant lastUpdated,
        String matchId,
        List<LiveInnings> innings,
        List<LiveBatter> batting,
        List<LiveBowler> bowling
) {
    public LiveScorecardResponse {
        innings = innings == null ? List.of() : List.copyOf(innings);
        batting = batting == null ? List.of() : List.copyOf(batting);
        bowling = bowling == null ? List.of() : List.copyOf(bowling);
    }
}