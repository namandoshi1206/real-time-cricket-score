package com.cricket.dto.live;

import java.time.Instant;
import java.util.List;

public record LiveFeedResponse(
        boolean success,
        boolean available,
        boolean stale,
        String message,
        Instant lastUpdated,
        List<LiveMatch> data
) {
    public LiveFeedResponse {
        data = data == null ? List.of() : List.copyOf(data);
    }
}