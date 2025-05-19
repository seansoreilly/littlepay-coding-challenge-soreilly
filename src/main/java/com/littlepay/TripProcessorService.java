package com.littlepay;

import littlepay.model.Tap;
import littlepay.model.TapType;
import littlepay.model.Trip;
import littlepay.model.TripStatus;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public class TripProcessorService {
    private final Pricing pricing;

    public TripProcessorService(Pricing pricing) {
        this.pricing = pricing;
    }

    public List<Trip> processTaps(List<Tap> taps) {
        if (taps == null || taps.isEmpty()) {
            System.out.println("TripProcessorService: No taps to process.");
            return Collections.emptyList();
        }

        // Sort taps by PAN and then by DateTimeUTC to process them in order for each
        // card
        taps.sort(Comparator.comparing(Tap::pan).thenComparing(Tap::dateTimeUTC));

        List<Trip> trips = new ArrayList<>();
        Map<String, List<Tap>> tapsByPan = taps.stream().collect(Collectors.groupingBy(Tap::pan));

        for (Map.Entry<String, List<Tap>> entry : tapsByPan.entrySet()) {
            String pan = entry.getKey();
            List<Tap> userTaps = entry.getValue(); // These are already sorted by time for this PAN

            Tap lastOnTap = null;
            for (Tap currentTap : userTaps) {
                if (currentTap.tapType() == TapType.ON) {
                    // If there was a previous ON tap that wasn't closed, it's an incomplete trip
                    if (lastOnTap != null) {
                        trips.add(createIncompleteTrip(lastOnTap));
                    }
                    lastOnTap = currentTap;
                } else if (currentTap.tapType() == TapType.OFF) {
                    if (lastOnTap != null) {
                        // Check for cancelled trip: ON and OFF at the same stop
                        if (lastOnTap.stopId() == currentTap.stopId()) {
                            trips.add(createCancelledTrip(lastOnTap, currentTap));
                        } else {
                            trips.add(createCompletedTrip(lastOnTap, currentTap));
                        }
                        lastOnTap = null; // Reset last ON tap as this trip is now closed
                    } else {
                        // OFF tap without a preceding ON tap - this is an invalid scenario
                        // As per current plan, these are ignored or could be logged.
                        // For now, we'll effectively ignore it by not creating a trip.
                        // Or, we could create a special "unmatched off tap" trip if required.
                        System.out.println(
                                "TripProcessorService: Encountered an OFF tap without a matching ON tap for PAN " + pan
                                        + " at " + currentTap.dateTimeUTC());
                    }
                }
            }
            // After iterating through all taps for a PAN, if there's an unclosed ON tap,
            // it's an incomplete trip
            if (lastOnTap != null) {
                trips.add(createIncompleteTrip(lastOnTap));
            }
        }
        System.out.println(
                "TripProcessorService: Processed " + taps.size() + " taps, generated " + trips.size() + " trips.");
        return trips;
    }

    private Trip createCompletedTrip(Tap onTap, Tap offTap) {
        long durationSecs = Duration.between(onTap.dateTimeUTC(), offTap.dateTimeUTC()).getSeconds();
        // Use .name() for Stop enums if Pricing expects String
        java.math.BigDecimal chargeAmount = pricing.getCost(onTap.stopId(), offTap.stopId());
        return new Trip(
                onTap.dateTimeUTC(),
                offTap.dateTimeUTC(),
                durationSecs,
                onTap.stopId(),
                offTap.stopId(),
                chargeAmount,
                onTap.companyId(),
                onTap.busId(),
                onTap.pan(),
                TripStatus.COMPLETED);
    }

    private Trip createIncompleteTrip(Tap onTap) {
        // For an incomplete trip, the charge is the maximum possible fare from the tap
        // ON stop
        // Use .name() for Stop enums if Pricing expects String
        java.math.BigDecimal chargeAmount = pricing.getMaxFare(onTap.stopId());
        return new Trip(
                onTap.dateTimeUTC(),
                null, // No finish time
                0, // Duration is not applicable or could be calculated differently if business
                   // rules specify
                onTap.stopId(),
                null, // No destination stop
                chargeAmount,
                onTap.companyId(),
                onTap.busId(),
                onTap.pan(),
                TripStatus.INCOMPLETE);
    }

    private Trip createCancelledTrip(Tap onTap, Tap offTap) {
        // Cancelled trips have zero charge and zero duration as per typical business
        // rules.
        return new Trip(
                onTap.dateTimeUTC(),
                offTap.dateTimeUTC(), // Duration is effectively zero
                0,
                onTap.stopId(),
                offTap.stopId(), // Same as fromStopId
                java.math.BigDecimal.ZERO, // No charge
                onTap.companyId(),
                onTap.busId(),
                onTap.pan(),
                TripStatus.CANCELLED);
    }
}