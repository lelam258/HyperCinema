package com.cinema.hyperCinema.service.pricing;

public record TicketPriceBreakdown(
        int basePrice,
        int effectivePrice,
        boolean weekendAdjusted,
        String adjustmentType,
        Integer adjustmentValue,
        int adjustmentAmount,
        String adjustmentLabel) {
}
