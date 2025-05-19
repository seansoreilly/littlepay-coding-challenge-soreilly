package littlepay;

import littlepay.model.Stop;
import littlepay.model.Tap;
import littlepay.model.TapType;
import littlepay.model.Trip;
import littlepay.model.TripStatus;
import littlepay.service.PricingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class TripProcessorServiceTest {

    private PricingService pricingService;
    private TripProcessorService tripProcessorService;

    private static final String DEFAULT_PAN = "1234567890123456";
    private static final String DEFAULT_COMPANY_ID = "Company1";
    private static final String DEFAULT_BUS_ID = "BusA";
    private static final LocalDateTime BASE_TIME = LocalDateTime.of(2023, 1, 1, 10, 0, 0);

    @BeforeEach
    void setUp() {
        pricingService = new PricingService(); // Real instance, as its logic is simple and defined
        tripProcessorService = new TripProcessorService(pricingService);
    }

    private Tap createTap(String id, LocalDateTime dateTime, TapType type, Stop stop, String pan) {
        return new Tap(id, dateTime, type, stop, DEFAULT_COMPANY_ID, DEFAULT_BUS_ID, pan);
    }

    private Tap createTap(String id, LocalDateTime dateTime, TapType type, Stop stop) {
        return createTap(id, dateTime, type, stop, DEFAULT_PAN);
    }

    @Test
    @DisplayName("Should group taps by PAN and sort them chronologically")
    void testGroupAndSortTaps() {
        Tap tap1Pan1 = createTap("1", BASE_TIME.plusMinutes(10), TapType.ON, Stop.STOP1, "PAN1");
        Tap tap2Pan1 = createTap("2", BASE_TIME.plusMinutes(20), TapType.OFF, Stop.STOP1, "PAN1");
        Tap tap1Pan2 = createTap("3", BASE_TIME.plusMinutes(5), TapType.ON, Stop.STOP2, "PAN2");
        Tap tap2Pan2Later = createTap("5", BASE_TIME.plusMinutes(25), TapType.ON, Stop.STOP3, "PAN2");
        Tap tap3Pan1Unsorted = createTap("4", BASE_TIME.plusMinutes(1), TapType.ON, Stop.STOP3, "PAN1");

        List<Tap> taps = Arrays.asList(tap1Pan1, tap2Pan1, tap1Pan2, tap2Pan2Later, tap3Pan1Unsorted);
        List<Trip> trips = tripProcessorService.generateTrips(taps);

        // PAN1: tap3Unsorted (01 min, ON S3), tap1 (10 min, ON S1), tap2 (20 min, OFF
        // S1)
        // Expected: 1 Incomplete (S3), 1 Cancelled (S1)
        // PAN2: tap1 (05 min, ON S2), tap2Later (25 min, ON S3)
        // Expected: 1 Incomplete (S2), 1 Incomplete (S3)

        assertEquals(4, trips.size(), "Should generate 4 trips in total");

        long pan1IncompleteCount = trips.stream()
                .filter(t -> t.pan().equals("PAN1") && t.status() == TripStatus.INCOMPLETE
                        && t.fromStopId() == Stop.STOP3)
                .count();
        assertEquals(1, pan1IncompleteCount, "PAN1 should have one incomplete trip from STOP3 due to sorting");

        long pan1CancelledCount = trips.stream()
                .filter(t -> t.pan().equals("PAN1") && t.status() == TripStatus.CANCELLED
                        && t.fromStopId() == Stop.STOP1)
                .count();
        assertEquals(1, pan1CancelledCount, "PAN1 should have one cancelled trip at STOP1");

        long pan2IncompleteCount = trips.stream()
                .filter(t -> t.pan().equals("PAN2") && t.status() == TripStatus.INCOMPLETE)
                .count();
        assertEquals(2, pan2IncompleteCount, "PAN2 should have two incomplete trips");

        // More specific checks on order if generateTrips preserved order of PANs,
        // but the primary check is that taps for *each* PAN are processed
        // chronologically.
        // The internal map `tapsByPan` is not directly testable here without changing
        // the service's API.
        // We infer correct sorting by observing the trip outcomes.
    }

    @ParameterizedTest
    @EnumSource(Stop.class)
    @DisplayName("Should create CANCELLED trip when tap ON and OFF at the same stop")
    void testCancelledTrip(Stop stop) {
        Tap onTap = createTap("1", BASE_TIME, TapType.ON, stop);
        Tap offTap = createTap("2", BASE_TIME.plusMinutes(5), TapType.OFF, stop);
        List<Tap> taps = Arrays.asList(onTap, offTap);

        List<Trip> trips = tripProcessorService.generateTrips(taps);

        assertEquals(1, trips.size(), "Should generate one trip");
        Trip trip = trips.get(0);

        assertEquals(TripStatus.CANCELLED, trip.status());
        assertEquals(onTap.dateTimeUTC(), trip.started());
        assertEquals(offTap.dateTimeUTC(), trip.finished());
        assertEquals(TimeUnit.MINUTES.toSeconds(5), trip.durationSecs());
        assertEquals(stop, trip.fromStopId());
        assertEquals(stop, trip.toStopId());
        assertEquals(BigDecimal.ZERO.setScale(2), trip.chargeAmount().setScale(2)); // Ensure scale for comparison
        assertEquals(DEFAULT_PAN, trip.pan());
        assertEquals(DEFAULT_COMPANY_ID, trip.companyId());
        assertEquals(DEFAULT_BUS_ID, trip.busId());
    }

    @Test
    @DisplayName("Should create COMPLETED trip - Stop1 to Stop2")
    void testCompletedTrip_Stop1_Stop2() {
        Tap onTap = createTap("1", BASE_TIME, TapType.ON, Stop.STOP1);
        Tap offTap = createTap("2", BASE_TIME.plusHours(1).plusMinutes(10).plusSeconds(5), TapType.OFF, Stop.STOP2);
        List<Tap> taps = Arrays.asList(onTap, offTap);

        List<Trip> trips = tripProcessorService.generateTrips(taps);

        assertEquals(1, trips.size());
        Trip trip = trips.get(0);

        assertEquals(TripStatus.COMPLETED, trip.status());
        assertEquals(onTap.dateTimeUTC(), trip.started());
        assertEquals(offTap.dateTimeUTC(), trip.finished());
        assertEquals(3600 + 10 * 60 + 5, trip.durationSecs());
        assertEquals(Stop.STOP1, trip.fromStopId());
        assertEquals(Stop.STOP2, trip.toStopId());
        assertEquals(new BigDecimal("3.25").setScale(2), trip.chargeAmount().setScale(2));
        assertEquals(DEFAULT_PAN, trip.pan());
    }

    @Test
    @DisplayName("Should create COMPLETED trip - Stop2 to Stop3")
    void testCompletedTrip_Stop2_Stop3() {
        Tap onTap = createTap("1", BASE_TIME, TapType.ON, Stop.STOP2);
        Tap offTap = createTap("2", BASE_TIME.plusMinutes(30), TapType.OFF, Stop.STOP3);
        List<Tap> taps = Arrays.asList(onTap, offTap);

        List<Trip> trips = tripProcessorService.generateTrips(taps);

        assertEquals(1, trips.size());
        Trip trip = trips.get(0);

        assertEquals(TripStatus.COMPLETED, trip.status());
        assertEquals(Stop.STOP2, trip.fromStopId());
        assertEquals(Stop.STOP3, trip.toStopId());
        assertEquals(new BigDecimal("5.50").setScale(2), trip.chargeAmount().setScale(2));
        assertEquals(TimeUnit.MINUTES.toSeconds(30), trip.durationSecs());
    }

    @Test
    @DisplayName("Should create COMPLETED trip - Stop1 to Stop3")
    void testCompletedTrip_Stop1_Stop3() {
        Tap onTap = createTap("1", BASE_TIME, TapType.ON, Stop.STOP1);
        Tap offTap = createTap("2", BASE_TIME.plusMinutes(45), TapType.OFF, Stop.STOP3);
        List<Tap> taps = Arrays.asList(onTap, offTap);

        List<Trip> trips = tripProcessorService.generateTrips(taps);

        assertEquals(1, trips.size());
        Trip trip = trips.get(0);

        assertEquals(TripStatus.COMPLETED, trip.status());
        assertEquals(Stop.STOP1, trip.fromStopId());
        assertEquals(Stop.STOP3, trip.toStopId());
        assertEquals(new BigDecimal("7.30").setScale(2), trip.chargeAmount().setScale(2));
        assertEquals(TimeUnit.MINUTES.toSeconds(45), trip.durationSecs());
    }

    // Test for reverse completed trips (e.g., Stop2 to Stop1)
    @Test
    @DisplayName("Should create COMPLETED trip - Stop2 to Stop1 (Reverse)")
    void testCompletedTrip_Stop2_Stop1_Reverse() {
        Tap onTap = createTap("1", BASE_TIME, TapType.ON, Stop.STOP2);
        Tap offTap = createTap("2", BASE_TIME.plusMinutes(20), TapType.OFF, Stop.STOP1);
        List<Tap> taps = Arrays.asList(onTap, offTap);

        List<Trip> trips = tripProcessorService.generateTrips(taps);
        assertEquals(1, trips.size());
        Trip trip = trips.get(0);

        assertEquals(TripStatus.COMPLETED, trip.status());
        assertEquals(Stop.STOP2, trip.fromStopId());
        assertEquals(Stop.STOP1, trip.toStopId());
        assertEquals(new BigDecimal("3.25").setScale(2), trip.chargeAmount().setScale(2));
        assertEquals(TimeUnit.MINUTES.toSeconds(20), trip.durationSecs());
    }

    @ParameterizedTest
    @EnumSource(Stop.class)
    @DisplayName("Should create INCOMPLETE trip when only ON tap present")
    void testIncompleteTrip_OnlyOnTap(Stop stop) {
        Tap onTap = createTap("1", BASE_TIME, TapType.ON, stop);
        List<Tap> taps = Arrays.asList(onTap);

        List<Trip> trips = tripProcessorService.generateTrips(taps);

        assertEquals(1, trips.size());
        Trip trip = trips.get(0);

        assertEquals(TripStatus.INCOMPLETE, trip.status());
        assertEquals(onTap.dateTimeUTC(), trip.started());
        assertEquals(onTap.dateTimeUTC(), trip.finished()); // As per current implementation assumption
        assertEquals(0, trip.durationSecs()); // As per current implementation assumption
        assertEquals(stop, trip.fromStopId());
        assertEquals(stop, trip.toStopId()); // As per current implementation assumption
        assertEquals(pricingService.getMaxFare(stop).setScale(2), trip.chargeAmount().setScale(2));
        assertEquals(DEFAULT_PAN, trip.pan());
    }

    @Test
    @DisplayName("Should handle OFF tap with no preceding ON tap (ignored)")
    void testOffTapWithNoOnTap_IsIgnored() {
        Tap offTap = createTap("1", BASE_TIME, TapType.OFF, Stop.STOP1);
        List<Tap> taps = Arrays.asList(offTap);

        List<Trip> trips = tripProcessorService.generateTrips(taps);

        assertTrue(trips.isEmpty(), "No trips should be generated for an unmatched OFF tap");
    }

    @Test
    @DisplayName("Should handle multiple ON taps before an OFF tap (first ON matches OFF)")
    void testMultipleOnTapsBeforeOffTap() {
        Tap onTap1 = createTap("1", BASE_TIME, TapType.ON, Stop.STOP1); // Should be incomplete
        Tap onTap2 = createTap("2", BASE_TIME.plusMinutes(5), TapType.ON, Stop.STOP2); // Should complete with offTap
        Tap offTap = createTap("3", BASE_TIME.plusMinutes(10), TapType.OFF, Stop.STOP3);
        List<Tap> taps = Arrays.asList(onTap1, onTap2, offTap);

        List<Trip> trips = tripProcessorService.generateTrips(taps);

        assertEquals(2, trips.size(), "Should generate two trips");

        Trip incompleteTrip = trips.stream().filter(t -> t.status() == TripStatus.INCOMPLETE).findFirst().orElse(null);
        assertNotNull(incompleteTrip, "Should be one incomplete trip");
        assertEquals(Stop.STOP1, incompleteTrip.fromStopId());
        assertEquals(pricingService.getMaxFare(Stop.STOP1).setScale(2), incompleteTrip.chargeAmount().setScale(2));

        Trip completedTrip = trips.stream().filter(t -> t.status() == TripStatus.COMPLETED).findFirst().orElse(null);
        assertNotNull(completedTrip, "Should be one completed trip");
        assertEquals(Stop.STOP2, completedTrip.fromStopId());
        assertEquals(Stop.STOP3, completedTrip.toStopId());
        assertEquals(pricingService.getFare(Stop.STOP2, Stop.STOP3).setScale(2),
                completedTrip.chargeAmount().setScale(2));
        assertEquals(TimeUnit.MINUTES.toSeconds(5), completedTrip.durationSecs()); // 10 (OFF) - 5 (ON Tap2)
    }

    @Test
    @DisplayName("Mixed scenario: Completed, then Incomplete for a single PAN")
    void testMixedScenario_CompletedThenIncomplete() {
        Tap onTap1 = createTap("1", BASE_TIME, TapType.ON, Stop.STOP1);
        Tap offTap1 = createTap("2", BASE_TIME.plusMinutes(10), TapType.OFF, Stop.STOP2);
        Tap onTap2 = createTap("3", BASE_TIME.plusMinutes(20), TapType.ON, Stop.STOP3);
        List<Tap> taps = Arrays.asList(onTap1, offTap1, onTap2);

        List<Trip> trips = tripProcessorService.generateTrips(taps);
        assertEquals(2, trips.size());

        Trip completedTrip = trips.stream().filter(t -> t.status() == TripStatus.COMPLETED).findFirst().orElse(null);
        assertNotNull(completedTrip);
        assertEquals(Stop.STOP1, completedTrip.fromStopId());
        assertEquals(Stop.STOP2, completedTrip.toStopId());

        Trip incompleteTrip = trips.stream().filter(t -> t.status() == TripStatus.INCOMPLETE).findFirst().orElse(null);
        assertNotNull(incompleteTrip);
        assertEquals(Stop.STOP3, incompleteTrip.fromStopId());
    }

    @Test
    @DisplayName("Mixed scenario: Incomplete, then Cancelled for a single PAN")
    void testMixedScenario_IncompleteThenCancelled() {
        // This scenario tests if an unclosed ON tap is correctly marked incomplete
        // even if subsequent taps form a cancelled trip.
        Tap onTap1 = createTap("1", BASE_TIME, TapType.ON, Stop.STOP1); // Should be Incomplete
        Tap onTap2 = createTap("2", BASE_TIME.plusMinutes(10), TapType.ON, Stop.STOP2); // Part of cancelled
        Tap offTap2 = createTap("3", BASE_TIME.plusMinutes(20), TapType.OFF, Stop.STOP2); // Part of cancelled

        List<Tap> taps = Arrays.asList(onTap1, onTap2, offTap2);
        List<Trip> trips = tripProcessorService.generateTrips(taps);

        assertEquals(2, trips.size(), "Should generate two trips");

        Trip incompleteTrip = trips.stream()
                .filter(t -> t.status() == TripStatus.INCOMPLETE && t.fromStopId() == Stop.STOP1)
                .findFirst().orElse(null);
        assertNotNull(incompleteTrip, "First trip should be INCOMPLETE from Stop1");
        assertEquals(pricingService.getMaxFare(Stop.STOP1).setScale(2), incompleteTrip.chargeAmount().setScale(2));

        Trip cancelledTrip = trips.stream()
                .filter(t -> t.status() == TripStatus.CANCELLED && t.fromStopId() == Stop.STOP2)
                .findFirst().orElse(null);
        assertNotNull(cancelledTrip, "Second trip should be CANCELLED at Stop2");
        assertEquals(BigDecimal.ZERO.setScale(2), cancelledTrip.chargeAmount().setScale(2));
        assertEquals(TimeUnit.MINUTES.toSeconds(10), cancelledTrip.durationSecs());
    }

    @Test
    @DisplayName("Multiple completed trips for a single PAN")
    void testMultipleCompletedTrips_SinglePAN() {
        Tap onTap1 = createTap("1", BASE_TIME, TapType.ON, Stop.STOP1);
        Tap offTap1 = createTap("2", BASE_TIME.plusMinutes(10), TapType.OFF, Stop.STOP2);
        Tap onTap2 = createTap("3", BASE_TIME.plusMinutes(20), TapType.ON, Stop.STOP2);
        Tap offTap2 = createTap("4", BASE_TIME.plusMinutes(30), TapType.OFF, Stop.STOP3);
        List<Tap> taps = Arrays.asList(onTap1, offTap1, onTap2, offTap2);

        List<Trip> trips = tripProcessorService.generateTrips(taps);
        assertEquals(2, trips.size());

        assertTrue(trips.stream().allMatch(t -> t.status() == TripStatus.COMPLETED));
        assertEquals(Stop.STOP1, trips.get(0).fromStopId());
        assertEquals(Stop.STOP2, trips.get(0).toStopId());
        assertEquals(Stop.STOP2, trips.get(1).fromStopId());
        assertEquals(Stop.STOP3, trips.get(1).toStopId());
    }

    @Test
    @DisplayName("Taps for multiple PANs processed correctly")
    void testMultiplePANs() {
        String pan1 = "PAN_A";
        String pan2 = "PAN_B";

        Tap p1Tap1 = createTap("1", BASE_TIME, TapType.ON, Stop.STOP1, pan1); // PAN A: ON Stop1
        Tap p2Tap1 = createTap("2", BASE_TIME.plusMinutes(5), TapType.ON, Stop.STOP2, pan2); // PAN B: ON Stop2
        Tap p1Tap2 = createTap("3", BASE_TIME.plusMinutes(10), TapType.OFF, Stop.STOP1, pan1);// PAN A: OFF Stop1
                                                                                              // (Cancelled)
        Tap p2Tap2 = createTap("4", BASE_TIME.plusMinutes(15), TapType.ON, Stop.STOP3, pan2); // PAN B: ON Stop3
                                                                                              // (Incomplete)
        Tap p1Tap3 = createTap("5", BASE_TIME.plusMinutes(20), TapType.ON, Stop.STOP2, pan1); // PAN A: ON Stop2
                                                                                              // (Incomplete)
        Tap p2Tap3 = createTap("6", BASE_TIME.plusMinutes(25), TapType.OFF, Stop.STOP3, pan2);// PAN B: OFF Stop3
                                                                                              // (Matches p2Tap2 -
                                                                                              // Cancelled)

        // Chronological order of all taps: p1Tap1, p2Tap1, p1Tap2, p2Tap2, p1Tap3,
        // p2Tap3
        List<Tap> taps = Arrays.asList(p1Tap1, p2Tap1, p1Tap2, p2Tap2, p1Tap3, p2Tap3);
        List<Trip> trips = tripProcessorService.generateTrips(taps);

        // PAN A: (ON S1 @0m, OFF S1 @10m -> Cancelled S1), (ON S2 @20m -> Incomplete
        // S2)
        // PAN B: (ON S2 @5m -> Incomplete S2), (ON S3 @15m, OFF S3 @25m -> Cancelled
        // S3)

        assertEquals(4, trips.size(), "Should be 4 trips in total");

        // PAN A assertions
        List<Trip> panATrips = trips.stream().filter(t -> t.pan().equals(pan1)).toList();
        assertEquals(2, panATrips.size(), "PAN A should have 2 trips");
        assertTrue(panATrips.stream().anyMatch(t -> t.status() == TripStatus.CANCELLED && t.fromStopId() == Stop.STOP1),
                "PAN A should have a cancelled trip from Stop1");
        assertTrue(
                panATrips.stream().anyMatch(t -> t.status() == TripStatus.INCOMPLETE && t.fromStopId() == Stop.STOP2),
                "PAN A should have an incomplete trip from Stop2");

        // PAN B assertions
        List<Trip> panBTrips = trips.stream().filter(t -> t.pan().equals(pan2)).toList();
        assertEquals(2, panBTrips.size(), "PAN B should have 2 trips");
        // Correcting PAN B logic based on how TripProcessorService handles consecutive
        // ON taps:
        // p2Tap1 (ON S2 @5m) -> becomes INCOMPLETE because p2Tap2 (ON S3 @15m) arrives
        // before any OFF for p2Tap1.
        // p2Tap2 (ON S3 @15m) pairs with p2Tap3 (OFF S3 @25m) -> becomes CANCELLED S3.

        assertTrue(
                panBTrips.stream().anyMatch(t -> t.status() == TripStatus.INCOMPLETE && t.fromStopId() == Stop.STOP2),
                "PAN B should have an incomplete trip from Stop2");
        assertTrue(panBTrips.stream().anyMatch(t -> t.status() == TripStatus.CANCELLED && t.fromStopId() == Stop.STOP3),
                "PAN B should have a cancelled trip from Stop3");

    }

    @Test
    @DisplayName("Ensure duration and fare calculations are correct for various trip types")
    void testDurationAndFareCalculations() {
        // Completed Trip
        Tap onTapCompleted = createTap("c1", BASE_TIME, TapType.ON, Stop.STOP1);
        Tap offTapCompleted = createTap("c2", BASE_TIME.plusMinutes(15).plusSeconds(30), TapType.OFF, Stop.STOP2);

        // Cancelled Trip
        Tap onTapCancelled = createTap("can1", BASE_TIME.plusHours(1), TapType.ON, Stop.STOP3);
        Tap offTapCancelled = createTap("can2", BASE_TIME.plusHours(1).plusMinutes(5), TapType.OFF, Stop.STOP3);

        // Incomplete Trip
        Tap onTapIncomplete = createTap("inc1", BASE_TIME.plusHours(2), TapType.ON, Stop.STOP1);

        List<Tap> taps = Arrays.asList(onTapCompleted, offTapCompleted, onTapCancelled, offTapCancelled,
                onTapIncomplete);
        List<Trip> trips = tripProcessorService.generateTrips(taps);

        assertEquals(3, trips.size());

        Trip completed = trips.stream().filter(t -> t.status() == TripStatus.COMPLETED).findFirst().get();
        assertEquals(15 * 60 + 30, completed.durationSecs());
        assertEquals(pricingService.getFare(Stop.STOP1, Stop.STOP2).setScale(2), completed.chargeAmount().setScale(2));

        Trip cancelled = trips.stream().filter(t -> t.status() == TripStatus.CANCELLED).findFirst().get();
        assertEquals(5 * 60, cancelled.durationSecs());
        assertEquals(BigDecimal.ZERO.setScale(2), cancelled.chargeAmount().setScale(2));

        Trip incomplete = trips.stream().filter(t -> t.status() == TripStatus.INCOMPLETE).findFirst().get();
        assertEquals(0, incomplete.durationSecs());
        assertEquals(pricingService.getMaxFare(Stop.STOP1).setScale(2), incomplete.chargeAmount().setScale(2));
    }

    // More tests to be added here based on the plan
}