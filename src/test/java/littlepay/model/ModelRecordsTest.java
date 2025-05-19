package littlepay.model;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class ModelRecordsTest {

    @Test
    void tapRecordCreationAndAccessors() {
        LocalDateTime now = LocalDateTime.now();
        Tap tap = new Tap("1", now, TapType.ON, Stop.STOP1, "CompanyA", "Bus1", "1234567890123456");

        assertEquals("1", tap.id());
        assertEquals(now, tap.dateTimeUTC());
        assertEquals(TapType.ON, tap.tapType());
        assertEquals(Stop.STOP1, tap.stopId());
        assertEquals("CompanyA", tap.companyId());
        assertEquals("Bus1", tap.busId());
        assertEquals("1234567890123456", tap.pan());
    }

    @Test
    void tripRecordCreationAndAccessors() {
        LocalDateTime started = LocalDateTime.now();
        LocalDateTime finished = started.plusHours(1);
        BigDecimal charge = new BigDecimal("3.25");

        Trip trip = new Trip(started, finished, 3600, Stop.STOP1, Stop.STOP2, charge, "CompanyB", "Bus2",
                "9876543210987654", TripStatus.COMPLETED);

        assertEquals(started, trip.started());
        assertEquals(finished, trip.finished());
        assertEquals(3600, trip.durationSecs());
        assertEquals(Stop.STOP1, trip.fromStopId());
        assertEquals(Stop.STOP2, trip.toStopId());
        assertEquals(charge, trip.chargeAmount());
        assertEquals("CompanyB", trip.companyId());
        assertEquals("Bus2", trip.busId());
        assertEquals("9876543210987654", trip.pan());
        assertEquals(TripStatus.COMPLETED, trip.status());
    }

    @Test
    void stopFromStringValid() {
        assertEquals(Stop.STOP1, Stop.fromString("STOP1"));
        assertEquals(Stop.STOP2, Stop.fromString("Stop2")); // Test case-insensitivity
        assertEquals(Stop.STOP3, Stop.fromString(" stop3 ")); // Test trimming
    }

    @Test
    void stopFromStringInvalid() {
        assertThrows(IllegalArgumentException.class, () -> Stop.fromString("STOP4"));
        assertThrows(IllegalArgumentException.class, () -> Stop.fromString(null));
        assertThrows(IllegalArgumentException.class, () -> Stop.fromString(""));
    }
}