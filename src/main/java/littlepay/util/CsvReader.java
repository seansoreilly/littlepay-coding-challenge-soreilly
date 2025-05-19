package littlepay.util;

import com.opencsv.exceptions.CsvValidationException;
import littlepay.model.Stop;
import littlepay.model.Tap;
import littlepay.model.TapType;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CsvReader {

    private static final Logger LOGGER = Logger.getLogger(CsvReader.class.getName());
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    public List<Tap> readTaps(String filePath) throws IOException {
        List<Tap> taps = new ArrayList<>();
        try (Reader reader = new FileReader(filePath);
                com.opencsv.CSVReader csvReader = new com.opencsv.CSVReader(reader)) {

            String[] headers = csvReader.readNext(); // Read and skip header row
            if (headers == null) {
                LOGGER.info("CSV file is empty or has no headers: " + filePath);
                return taps;
            }

            String[] line;
            int lineNumber = 1; // Row number after header
            while ((line = csvReader.readNext()) != null) {
                lineNumber++;
                try {
                    if (line.length < 7) {
                        LOGGER.warning("Skipping malformed row (not enough columns: expected 7, got " + line.length
                                + ") at line " + lineNumber + " in file " + filePath);
                        continue;
                    }
                    // Expected columns: ID, DateTimeUTC, TapType, StopId, CompanyId, BusID, PAN
                    String id = line[0].trim();
                    LocalDateTime dateTimeUTC = LocalDateTime.parse(line[1].trim(), DATE_TIME_FORMATTER);
                    TapType tapType = TapType.valueOf(line[2].trim().toUpperCase());
                    Stop stopId = Stop.fromString(line[3].trim());
                    String companyId = line[4].trim();
                    String busId = line[5].trim();
                    String pan = line[6].trim();

                    taps.add(new Tap(id, dateTimeUTC, tapType, stopId, companyId, busId, pan));

                } catch (DateTimeParseException e) {
                    LOGGER.log(
                            Level.WARNING, "Skipping row due to invalid date format at line " + lineNumber + " in file "
                                    + filePath + ": " + (line.length > 1 ? line[1] : "[DATE_MISSING_OR_ROW_TOO_SHORT]")
                                    + ". Error: " + e.getMessage());
                } catch (IllegalArgumentException e) { // Catches TapType or Stop.fromString errors
                    LOGGER.log(Level.WARNING, "Skipping row due to invalid enum value at line " + lineNumber
                            + " in file " + filePath + ": " + e.getMessage());
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING,
                            "Skipping row due to an unexpected error at line " + lineNumber + " in file " + filePath,
                            e);
                }
            }
        } catch (CsvValidationException e) {
            LOGGER.log(Level.SEVERE, "CSV validation error while reading file " + filePath, e);
            throw new IOException("Failed to validate CSV content from " + filePath, e);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "I/O error while reading file " + filePath, e);
            throw e; // Re-throw IOException
        }
        return taps;
    }
}