package com.littlepay;

import com.opencsv.CSVWriterBuilder;
import com.opencsv.ICSVWriter;
import littlepay.model.Trip;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CsvWriter {
    // Define the DateTimeFormatter for output, matching the input format or a
    // desired output format
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
    private static final String[] HEADER = {
            "Started", "Finished", "DurationSecs", "FromStopId", "ToStopId",
            "ChargeAmount", "CompanyId", "BusID", "PAN", "Status"
    };

    public void writeTrips(Path filePath, List<Trip> trips) throws IOException {
        System.out.println("CsvWriter: Writing " + (trips == null ? 0 : trips.size()) + " trips to " + filePath);
        try (FileWriter writer = new FileWriter(filePath.toFile());
                ICSVWriter csvWriter = new CSVWriterBuilder(writer)
                        .withSeparator(ICSVWriter.DEFAULT_SEPARATOR)
                        .withQuoteChar(ICSVWriter.NO_QUOTE_CHARACTER) // Or DEFAULT_QUOTE_CHARACTER based on
                                                                      // requirements
                        .build()) {

            csvWriter.writeNext(HEADER);

            if (trips == null || trips.isEmpty()) {
                System.out.println("CsvWriter: No trips to write.");
                return; // Header written, but no data lines
            }

            for (Trip trip : trips) {
                String started = trip.started() != null ? trip.started().format(DATE_TIME_FORMATTER) : "";
                String finished = trip.finished() != null ? trip.finished().format(DATE_TIME_FORMATTER) : "";
                String fromStopId = trip.fromStopId() != null ? trip.fromStopId().toPascalCase() : "";
                String toStopId = trip.toStopId() != null ? trip.toStopId().toPascalCase() : "";
                String chargeAmount = "$" + trip.chargeAmount().toPlainString();
                String status = trip.status() != null ? trip.status().name() : "";

                csvWriter.writeNext(new String[] {
                        started,
                        finished,
                        String.valueOf(trip.durationSecs()),
                        fromStopId,
                        toStopId,
                        chargeAmount,
                        trip.companyId(),
                        trip.busId(),
                        trip.pan(),
                        status
                });
            }
            System.out.println("CsvWriter: Successfully wrote trips to " + filePath);
        } catch (IOException e) {
            System.err.println("CsvWriter: Error writing trips to " + filePath + ": " + e.getMessage());
            throw e;
        }
    }
}