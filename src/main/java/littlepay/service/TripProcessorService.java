package littlepay.service;

import littlepay.model.Tap;
import littlepay.model.TapType;
import littlepay.model.Trip;
import littlepay.model.TripStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class TripProcessorService {

    private final PricingService pricingService;

    public TripProcessorService(PricingService pricingService) {
        this.pricingService = pricingService;
    }

    /**
     * Groups taps by PAN and sorts them chronologically.
     * 
     * @param taps List of all taps.
     * @return Map with PAN as key and a chronologically sorted list of taps as
     *         value.
     */
    private Map<String, List<Tap>> groupAndSortTapsByPan(List<Tap> taps) {
        if (taps == null) {
            return new HashMap<>();
        }
        return taps.stream()
                .sorted(Comparator.comparing(Tap::dateTimeUTC))
                .collect(Collectors.groupingBy(Tap::pan));
    }

    /**
     * Processes a list of taps to generate a list of trips.
     * 
     * @param allTaps List of all tap events.
     * @return List of generated trips.
     */
    public List<Trip> generateTrips(List<Tap> allTaps) {
        Map<String, List<Tap>> tapsByPan = groupAndSortTapsByPan(allTaps);
        List<Trip> processedTrips = new ArrayList<>();

        for (List<Tap> panTaps : tapsByPan.values()) {
            Tap lastOnTap = null;
            for (Tap currentTap : panTaps) {
                if (currentTap.tapType() == TapType.ON) {
                    // If there was a previous ON tap that wasn't matched, it's incomplete.
                    if (lastOnTap != null) {
                        processedTrips.add(createIncompleteTrip(lastOnTap));
                    }
                    lastOnTap = currentTap;
                } else if (currentTap.tapType() == TapType.OFF) {
                    if (lastOnTap != null) {
                        // We have a potential pair
                        if (lastOnTap.stopId().equals(currentTap.stopId())) {
                            // Cancelled Trip
                            processedTrips.add(createCancelledTrip(lastOnTap, currentTap));
                        } else {
                            // Completed Trip
                            processedTrips.add(createCompletedTrip(lastOnTap, currentTap));
                        }
                        lastOnTap = null; // This ON tap is now matched
                    } else {
                        // OFF tap without a preceding ON tap for this PAN.
                        // As per plan: "OFF tap with no preceding ON tap (should be ignored or
                        // logged)".
                        // Currently ignoring. Logging can be added.
                    }
                }
            }
            // After iterating through all taps for a PAN, if there's an unmatched ON tap,
            // it's incomplete.
            if (lastOnTap != null) {
                processedTrips.add(createIncompleteTrip(lastOnTap));
            }
        }

        // Sort the trips before returning
        // Primary sort by start time, secondary sort by PAN for stability
        processedTrips.sort(Comparator.comparing(Trip::started)
                .thenComparing(Trip::pan));

        return processedTrips;
    }

    private Trip createCompletedTrip(Tap onTap, Tap offTap) {
        LocalDateTime started = onTap.dateTimeUTC();
        LocalDateTime finished = offTap.dateTimeUTC();
        long durationSeconds = ChronoUnit.SECONDS.between(started, finished);
        BigDecimal chargeAmount = pricingService.getFare(onTap.stopId(), offTap.stopId());

        return new Trip(
                started,
                finished,
                durationSeconds,
                onTap.stopId(),
                offTap.stopId(),
                chargeAmount,
                onTap.companyId(),
                onTap.busId(),
                onTap.pan(),
                TripStatus.COMPLETED);
    }

    private Trip createCancelledTrip(Tap onTap, Tap offTap) {
        LocalDateTime started = onTap.dateTimeUTC();
        LocalDateTime finished = offTap.dateTimeUTC();
        long durationSeconds = ChronoUnit.SECONDS.between(started, finished);

        return new Trip(
                started,
                finished,
                durationSeconds,
                onTap.stopId(),
                offTap.stopId(),
                BigDecimal.ZERO,
                onTap.companyId(),
                onTap.busId(),
                onTap.pan(),
                TripStatus.CANCELLED);
    }

    private Trip createIncompleteTrip(Tap onTap) {
        // Assumptions for Incomplete Trips from plan:
        // - ChargeAmount = maximum fare from ON tap's StopId
        // - ToStopId can be set to the FromStopId (or null - choosing null)
        // - Finished timestamp might be the same as Started (or null - choosing null)
        // - DurationSecs might be 0

        BigDecimal chargeAmount = pricingService.getMaxFare(onTap.stopId());

        return new Trip(
                onTap.dateTimeUTC(),
                null, // Finished timestamp set to null for INCOMPLETE
                0,
                onTap.stopId(),
                null, // ToStopId set to null for INCOMPLETE
                chargeAmount,
                onTap.companyId(),
                onTap.busId(),
                onTap.pan(),
                TripStatus.INCOMPLETE);
    }
}