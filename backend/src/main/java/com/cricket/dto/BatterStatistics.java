package com.cricket.dto;

public record BatterStatistics(
        Long playerId,
        String playerName,
        int runs,
        int ballsFaced,
        int fours,
        int sixes,
        boolean dismissed,
        double strikeRate
) {
}