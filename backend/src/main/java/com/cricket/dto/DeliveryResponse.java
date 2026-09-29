package com.cricket.dto;

import com.cricket.entity.ExtraType;
import com.cricket.entity.WicketType;

public record DeliveryResponse(
        Long id,
        Long inningsId,
        Integer overNumber,
        Integer ballNumber,
        Long batsmanId,
        Long nonStrikerId,
        Long bowlerId,
        Integer runs,
        ExtraType extraType,
        Integer extraRuns,
        Integer totalRuns,
        boolean legalDelivery,
        WicketType wicketType,
        Long dismissedBatsmanId
) {
}