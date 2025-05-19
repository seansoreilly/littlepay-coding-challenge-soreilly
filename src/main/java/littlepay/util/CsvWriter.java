package littlepay.util;

import com.opencsv.CSVWriter;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import littlepay.model.Stop;
import littlepay.model.Trip;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class CsvWriter {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("$0.00");
    private static final String[] CSV_HEADER = {
            "Started", "Finished", "DurationSecs", "FromStopId", "ToStopId",
            "ChargeAmount", "CompanyId", "BusID", "PAN", "Status"
    };

    public void writeTrips(List<Trip> trips, String filePath)
            throws IOException, CsvRequiredFieldEmptyException, CsvDataTypeMismatchException {
        try (Writer writer = new FileWriter(filePath)) {
            // Write header manually
            CSVWriter csvWriter = new CSVWriter(writer,
                    CSVWriter.DEFAULT_SEPARATOR,
                    CSVWriter.NO_QUOTE_CHARACTER, // To avoid quotes around all fields
                    CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                    CSVWriter.DEFAULT_LINE_END);
            csvWriter.writeNext(CSV_HEADER);
            csvWriter.flush(); // Ensure header is written before bean writer takes over or writes its own

            // Use a custom strategy for StatefulBeanToCsv to handle formatting and nulls
            // However, direct iteration and manual construction is more straightforward for
            // precise formatting.

            List<String[]> stringArray = new ArrayList<>();
            for (Trip trip : trips) {
                String started = trip.started() != null ? trip.started().format(DATE_TIME_FORMATTER) : "";
                String finished = trip.finished() != null ? trip.finished().format(DATE_TIME_FORMATTER) : "";
                String durationSecs = String.valueOf(trip.durationSecs());
                String fromStopId = trip.fromStopId() != null ? trip.fromStopId().name() : "";
                String toStopId = trip.toStopId() != null ? trip.toStopId().name() : "";
                String chargeAmount = trip.chargeAmount() != null ? CURRENCY_FORMAT.format(trip.chargeAmount())
                        : CURRENCY_FORMAT.format(BigDecimal.ZERO);
                String companyId = trip.companyId() != null ? trip.companyId() : "";
                String busId = trip.busId() != null ? trip.busId() : "";
                String pan = trip.pan() != null ? trip.pan() : "";
                String status = trip.status() != null ? trip.status().name() : "";

                stringArray.add(new String[] {
                        started, finished, durationSecs, fromStopId, toStopId,
                        chargeAmount, companyId, busId, pan, status
                });
            }
            csvWriter.writeAll(stringArray);
        }
    }
}