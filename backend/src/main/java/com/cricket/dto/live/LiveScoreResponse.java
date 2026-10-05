package com.cricket.dto.live;

import java.time.Instant;

public record LiveScoreResponse(
        boolean success,
        boolean stale,
        String message,
        Instant lastUpdated,
        String matchId,
        String status,
        LiveScore data
) {
}