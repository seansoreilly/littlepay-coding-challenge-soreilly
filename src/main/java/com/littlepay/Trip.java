package com.littlepay;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Placeholder Trip record based on implementation_plan.md
public record Trip(
        LocalDateTime Started,
        LocalDateTime Finished,
        long DurationSecs,
        String FromStopId, // Enum or String
        String ToStopId, // Enum or String
        BigDecimal ChargeAmount,
        String CompanyId,
        String BusID,
        String PAN,
        String Status // Enum: COMPLETED, INCOMPLETE, CANCELLED
) {
}