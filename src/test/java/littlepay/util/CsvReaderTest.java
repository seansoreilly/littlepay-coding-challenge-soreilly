package littlepay.util;

import littlepay.model.Stop;
import littlepay.model.Tap;
import littlepay.model.TapType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CsvReaderTest {

    private final CsvReader csvReader = new CsvReader();
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    @TempDir
    Path tempDir;

    private File createTestCsvFile(String fileName, String... lines) throws IOException {
        Path filePath = tempDir.resolve(fileName);
        try (BufferedWriter writer = Files.newBufferedWriter(filePath)) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        }
        return filePath.toFile();
    }

    @Test
    void readTaps_ValidFile() throws IOException {
        File testFile = createTestCsvFile("valid_taps.csv",
                "ID, DateTimeUTC, TapType, StopId, CompanyId, BusID, PAN",
                "1, 20-08-2023 10:00:00, ON, Stop1, CompanyA, Bus1, 123456",
                "2, 20-08-2023 10:05:00, OFF, Stop2, CompanyA, Bus1, 123456");

        List<Tap> taps = csvReader.readTaps(testFile.getAbsolutePath());

        assertEquals(2, taps.size());
        Tap tap1 = taps.get(0);
        assertEquals("1", tap1.id());
        assertEquals(LocalDateTime.parse("20-08-2023 10:00:00", DATE_TIME_FORMATTER), tap1.dateTimeUTC());
        assertEquals(TapType.ON, tap1.tapType());
        assertEquals(Stop.STOP1, tap1.stopId());
        assertEquals("CompanyA", tap1.companyId());
        assertEquals("Bus1", tap1.busId());
        assertEquals("123456", tap1.pan());

        Tap tap2 = taps.get(1);
        assertEquals("2", tap2.id());
        assertEquals(LocalDateTime.parse("20-08-2023 10:05:00", DATE_TIME_FORMATTER), tap2.dateTimeUTC());
        assertEquals(TapType.OFF, tap2.tapType());
        assertEquals(Stop.STOP2, tap2.stopId());
    }

    @Test
    void readTaps_EmptyFile() throws IOException {
        File testFile = createTestCsvFile("empty_taps.csv");
        List<Tap> taps = csvReader.readTaps(testFile.getAbsolutePath());
        assertTrue(taps.isEmpty());
    }

    @Test
    void readTaps_FileWithOnlyHeaders() throws IOException {
        File testFile = createTestCsvFile("header_only_taps.csv",
                "ID, DateTimeUTC, TapType, StopId, CompanyId, BusID, PAN");
        List<Tap> taps = csvReader.readTaps(testFile.getAbsolutePath());
        assertTrue(taps.isEmpty());
    }

    @Test
    void readTaps_MalformedDate() throws IOException {
        File testFile = createTestCsvFile("malformed_date.csv",
                "ID, DateTimeUTC, TapType, StopId, CompanyId, BusID, PAN",
                "1, 20-AUG-2023 10:00:00, ON, Stop1, CompanyA, Bus1, 123456", // Invalid date format
                "2, 20-08-2023 10:05:00, OFF, Stop2, CompanyA, Bus1, 123456");
        List<Tap> taps = csvReader.readTaps(testFile.getAbsolutePath());
        assertEquals(1, taps.size()); // Only the valid row should be parsed
        assertEquals("2", taps.get(0).id());
    }

    @Test
    void readTaps_InvalidTapType() throws IOException {
        File testFile = createTestCsvFile("invalid_taptype.csv",
                "ID, DateTimeUTC, TapType, StopId, CompanyId, BusID, PAN",
                "1, 20-08-2023 10:00:00, INVALID, Stop1, CompanyA, Bus1, 123456",
                "2, 20-08-2023 10:05:00, OFF, Stop2, CompanyA, Bus1, 123456");
        List<Tap> taps = csvReader.readTaps(testFile.getAbsolutePath());
        assertEquals(2, taps.size());
        assertEquals("1", taps.get(0).id());
    }

    @Test
    void readTaps_InvalidStopId() throws IOException {
        File testFile = createTestCsvFile("invalid_stopid.csv",
                "ID, DateTimeUTC, TapType, StopId, CompanyId, BusID, PAN",
                "1, 20-08-2023 10:00:00, ON, StopX, CompanyA, Bus1, 123456", // Invalid StopId
                "2, 20-08-2023 10:05:00, OFF, Stop2, CompanyA, Bus1, 123456");
        List<Tap> taps = csvReader.readTaps(testFile.getAbsolutePath());
        assertEquals(1, taps.size());
        assertEquals("2", taps.get(0).id());
    }

    @Test
    void readTaps_MissingColumns() throws IOException {
        File testFile = createTestCsvFile("missing_columns.csv",
                "ID, DateTimeUTC, TapType, StopId, CompanyId, BusID, PAN",
                "1, 20-08-2023 10:00:00, ON, Stop1, CompanyA, Bus1", // Missing PAN
                "2, 20-08-2023 10:05:00, OFF, Stop2, CompanyA, Bus1, 123456");
        List<Tap> taps = csvReader.readTaps(testFile.getAbsolutePath());
        assertEquals(1, taps.size()); // Should skip the malformed row
        assertEquals("2", taps.get(0).id());
    }

    @Test
    void readTaps_FileNotFound() {
        File nonExistentFile = new File(tempDir.toFile(), "non_existent_taps.csv");
        assertThrows(IOException.class, () -> csvReader.readTaps(nonExistentFile.getAbsolutePath()));
    }
}