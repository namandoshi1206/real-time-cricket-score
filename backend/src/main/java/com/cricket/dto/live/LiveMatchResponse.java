package com.cricket.dto.live;

import java.time.Instant;

public record LiveMatchResponse(
        boolean success,
        boolean stale,
        String message,
        Instant lastUpdated,
        LiveMatch data
) {
}