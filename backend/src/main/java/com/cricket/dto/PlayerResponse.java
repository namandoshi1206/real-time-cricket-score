package com.cricket.dto;

import com.cricket.entity.BattingStyle;
import com.cricket.entity.BowlingStyle;
import com.cricket.entity.PlayerRole;

public record PlayerResponse(
        Long id,
        String firstName,
        String lastName,
        PlayerRole role,
        BattingStyle battingStyle,
        BowlingStyle bowlingStyle,
        Long teamId,
        String teamName
) {
}