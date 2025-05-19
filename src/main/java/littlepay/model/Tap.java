package littlepay.model;

import java.time.LocalDateTime;

public record Tap(
        String id,
        LocalDateTime dateTimeUTC,
        TapType tapType,
        Stop stopId,
        String companyId,
        String busId,
        String pan) {
}