package com.cricket.dto;

import com.cricket.entity.ExtraType;
import com.cricket.entity.WicketType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class DeliveryRequest {

    @NotNull
    @Min(1)
    private Integer overNumber;

    @NotNull
    @Min(1)
    private Integer ballNumber;

    @NotNull
    private Long batsmanId;

    @NotNull
    private Long nonStrikerId;

    @NotNull
    private Long bowlerId;

    private Long dismissedBatsmanId;

    @NotNull
    @Min(0)
    private Integer runs = 0;

    private ExtraType extraType = ExtraType.NONE;

    @NotNull
    @Min(0)
    private Integer extraRuns = 0;

    private WicketType wicketType;

    public Integer getOverNumber() {
        return overNumber;
    }

    public void setOverNumber(Integer overNumber) {
        this.overNumber = overNumber;
    }

    public Integer getBallNumber() {
        return ballNumber;
    }

    public void setBallNumber(Integer ballNumber) {
        this.ballNumber = ballNumber;
    }

    public Long getBatsmanId() {
        return batsmanId;
    }

    public void setBatsmanId(Long batsmanId) {
        this.batsmanId = batsmanId;
    }

    public Long getNonStrikerId() {
        return nonStrikerId;
    }

    public void setNonStrikerId(Long nonStrikerId) {
        this.nonStrikerId = nonStrikerId;
    }

    public Long getBowlerId() {
        return bowlerId;
    }

    public void setBowlerId(Long bowlerId) {
        this.bowlerId = bowlerId;
    }

    public Long getDismissedBatsmanId() {
        return dismissedBatsmanId;
    }

    public void setDismissedBatsmanId(Long dismissedBatsmanId) {
        this.dismissedBatsmanId = dismissedBatsmanId;
    }

    public Integer getRuns() {
        return runs;
    }

    public void setRuns(Integer runs) {
        this.runs = runs;
    }

    public ExtraType getExtraType() {
        return extraType;
    }

    public void setExtraType(ExtraType extraType) {
        this.extraType = extraType;
    }

    public Integer getExtraRuns() {
        return extraRuns;
    }

    public void setExtraRuns(Integer extraRuns) {
        this.extraRuns = extraRuns;
    }

    public WicketType getWicketType() {
        return wicketType;
    }

    public void setWicketType(WicketType wicketType) {
        this.wicketType = wicketType;
    }
}