package littlepay.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Trip(
        LocalDateTime started,
        LocalDateTime finished,
        long durationSecs,
        Stop fromStopId,
        Stop toStopId,
        BigDecimal chargeAmount,
        String companyId,
        String busId,
        String pan,
        TripStatus status) {
}