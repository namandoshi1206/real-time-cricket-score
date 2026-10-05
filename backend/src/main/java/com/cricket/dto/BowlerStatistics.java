package com.cricket.dto;

public record BowlerStatistics(
        Long playerId,
        String playerName,
        int legalBalls,
        String overs,
        int runsConceded,
        int wickets,
        double economy
) {
}