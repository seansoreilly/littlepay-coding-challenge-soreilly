package littlepay.util;

import littlepay.model.Stop;
import littlepay.model.Trip;
import littlepay.model.TripStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvWriterTest {

    private final CsvWriter csvWriter = new CsvWriter();

    @TempDir
    Path tempDir;

    private final LocalDateTime time1 = LocalDateTime.of(2023, 1, 15, 8, 30, 0);
    private final LocalDateTime time2 = LocalDateTime.of(2023, 1, 15, 8, 45, 15);
    private final LocalDateTime time3 = LocalDateTime.of(2023, 1, 15, 9, 5, 30);

    @Test
    void testWriteTrips_CompletedIncompleteCancelled() throws Exception {
        Path outputFile = tempDir.resolve("trips_mixed.csv");
        List<Trip> trips = new ArrayList<>();

        // COMPLETED Trip
        trips.add(new Trip(
                time1, time2, 915L, Stop.STOP1, Stop.STOP2,
                new BigDecimal("3.25"), "CompanyA", "Bus001", "1234567890123456", TripStatus.COMPLETED));

        // INCOMPLETE Trip
        trips.add(new Trip(
                time2, null, 0L, Stop.STOP1, null, // ToStopId and Finished can be null for INCOMPLETE
                new BigDecimal("7.30"), "CompanyB", "Bus002", "9876543210987654", TripStatus.INCOMPLETE));

        // CANCELLED Trip
        trips.add(new Trip(
                time3, time3.plusMinutes(2), 120L, Stop.STOP3, Stop.STOP3,
                BigDecimal.ZERO, "CompanyC", "Bus003", "1122334455667788", TripStatus.CANCELLED));

        csvWriter.writeTrips(trips, outputFile.toString());

        try (BufferedReader reader = new BufferedReader(new FileReader(outputFile.toFile()))) {
            assertEquals("Started,Finished,DurationSecs,FromStopId,ToStopId,ChargeAmount,CompanyId,BusID,PAN,Status",
                    reader.readLine());
            assertEquals(
                    "15-01-2023 08:30:00,15-01-2023 08:45:15,915,STOP1,STOP2,$3.25,CompanyA,Bus001,1234567890123456,COMPLETED",
                    reader.readLine());
            assertEquals("15-01-2023 08:45:15,,0,STOP1,,$7.30,CompanyB,Bus002,9876543210987654,INCOMPLETE",
                    reader.readLine());
            assertEquals(
                    "15-01-2023 09:05:30,15-01-2023 09:07:30,120,STOP3,STOP3,$0.00,CompanyC,Bus003,1122334455667788,CANCELLED",
                    reader.readLine());
        }
    }

    @Test
    void testWriteTrips_EmptyList() throws Exception {
        Path outputFile = tempDir.resolve("trips_empty.csv");
        List<Trip> trips = new ArrayList<>();

        csvWriter.writeTrips(trips, outputFile.toString());

        try (BufferedReader reader = new BufferedReader(new FileReader(outputFile.toFile()))) {
            assertEquals("Started,Finished,DurationSecs,FromStopId,ToStopId,ChargeAmount,CompanyId,BusID,PAN,Status",
                    reader.readLine());
            assertTrue(reader.readLine() == null); // No data rows
        }
    }

    @Test
    void testWriteTrips_Formatting() throws Exception {
        Path outputFile = tempDir.resolve("trips_formatting.csv");
        List<Trip> trips = new ArrayList<>();

        // Test specific charge amounts
        trips.add(new Trip(
                time1, time2, 915L, Stop.STOP1, Stop.STOP2,
                new BigDecimal("7.30"), "CompX", "BusX1", "PAN_X1", TripStatus.COMPLETED));
        trips.add(new Trip(
                time1, time2, 915L, Stop.STOP2, Stop.STOP3,
                new BigDecimal("5.50"), "CompY", "BusY1", "PAN_Y1", TripStatus.COMPLETED));
        trips.add(new Trip(
                time1, time1, 0L, Stop.STOP1, Stop.STOP1,
                BigDecimal.ZERO, "CompZ", "BusZ1", "PAN_Z1", TripStatus.CANCELLED));

        csvWriter.writeTrips(trips, outputFile.toString());

        try (BufferedReader reader = new BufferedReader(new FileReader(outputFile.toFile()))) {
            assertEquals("Started,Finished,DurationSecs,FromStopId,ToStopId,ChargeAmount,CompanyId,BusID,PAN,Status",
                    reader.readLine());
            assertEquals("15-01-2023 08:30:00,15-01-2023 08:45:15,915,STOP1,STOP2,$7.30,CompX,BusX1,PAN_X1,COMPLETED",
                    reader.readLine());
            assertEquals("15-01-2023 08:30:00,15-01-2023 08:45:15,915,STOP2,STOP3,$5.50,CompY,BusY1,PAN_Y1,COMPLETED",
                    reader.readLine());
            assertEquals("15-01-2023 08:30:00,15-01-2023 08:30:00,0,STOP1,STOP1,$0.00,CompZ,BusZ1,PAN_Z1,CANCELLED",
                    reader.readLine());
        }
    }

    @Test
    void testWriteTrips_NullFieldsInTrip() throws Exception {
        Path outputFile = tempDir.resolve("trips_null_fields.csv");
        List<Trip> trips = new ArrayList<>();

        // Trip with nulls that should be handled (e.g., for an INCOMPLETE trip)
        trips.add(new Trip(
                time1, // started
                null, // finished
                0L, // durationSecs (can be 0 for incomplete)
                Stop.STOP1, // fromStopId
                null, // toStopId
                new BigDecimal("5.50"), // chargeAmount (max fare)
                "CompanyD", // companyId
                "BusD1", // busId
                "PAN_D1", // pan
                TripStatus.INCOMPLETE // status
        ));
        // Trip with potentially other nulls, though less likely in valid scenarios but
        // good for robustness
        trips.add(new Trip(
                time2, time3, 600L, Stop.STOP2, Stop.STOP3,
                new BigDecimal("3.25"), null, null, null, TripStatus.COMPLETED));

        csvWriter.writeTrips(trips, outputFile.toString());

        try (BufferedReader reader = new BufferedReader(new FileReader(outputFile.toFile()))) {
            assertEquals("Started,Finished,DurationSecs,FromStopId,ToStopId,ChargeAmount,CompanyId,BusID,PAN,Status",
                    reader.readLine());
            assertEquals("15-01-2023 08:30:00,,0,STOP1,,$5.50,CompanyD,BusD1,PAN_D1,INCOMPLETE", reader.readLine());
            assertEquals("15-01-2023 08:45:15,15-01-2023 09:05:30,600,STOP2,STOP3,$3.25,,,,COMPLETED",
                    reader.readLine());
        }
    }
}