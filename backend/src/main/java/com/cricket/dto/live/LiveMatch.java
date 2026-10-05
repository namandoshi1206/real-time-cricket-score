package com.cricket.dto.live;

import java.time.Instant;
import java.util.List;

public record LiveMatch(
        String matchId,
        String title,
        String status,
        String venue,
        LiveTeam team1,
        LiveTeam team2,
        LiveScore score,
        List<LiveInnings> innings,
        List<LiveBatter> batsmen,
        List<LiveBowler> bowlers,
        List<String> recentDeliveries,
        String result,
        Instant updatedAt
) {
    public LiveMatch {
        innings = innings == null ? List.of() : List.copyOf(innings);
        batsmen = batsmen == null ? List.of() : List.copyOf(batsmen);
        bowlers = bowlers == null ? List.of() : List.copyOf(bowlers);
        recentDeliveries = recentDeliveries == null ? List.of() : List.copyOf(recentDeliveries);
    }
}