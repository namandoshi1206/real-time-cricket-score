package com.cricket.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class InningsRequest {

    @NotNull
    private Long battingTeamId;

    @NotNull
    private Long bowlingTeamId;

    @NotNull
    @Min(1)
    private Integer inningsNumber;

    @Min(1)
    private Integer targetRuns;

    public Long getBattingTeamId() {
        return battingTeamId;
    }

    public void setBattingTeamId(Long battingTeamId) {
        this.battingTeamId = battingTeamId;
    }

    public Long getBowlingTeamId() {
        return bowlingTeamId;
    }

    public void setBowlingTeamId(Long bowlingTeamId) {
        this.bowlingTeamId = bowlingTeamId;
    }

    public Integer getInningsNumber() {
        return inningsNumber;
    }

    public void setInningsNumber(Integer inningsNumber) {
        this.inningsNumber = inningsNumber;
    }

    public Integer getTargetRuns() {
        return targetRuns;
    }

    public void setTargetRuns(Integer targetRuns) {
        this.targetRuns = targetRuns;
    }
}