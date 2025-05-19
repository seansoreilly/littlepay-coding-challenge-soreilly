package littlepay;

import littlepay.model.Tap;
import littlepay.model.Trip;
import littlepay.service.PricingService;
import littlepay.util.CsvReader;
import littlepay.util.CsvWriter;

import java.io.FileNotFoundException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Main application class for the Littlepay transit fare calculation system.
 * This class orchestrates the reading of tap data, processing of taps into
 * trips,
 * and writing the resulting trips to an output file.
 */
public class Main {

    private static final String DEFAULT_TAPS_FILE = "taps.csv";
    private static final String DEFAULT_TRIPS_FILE = "trips.csv";

    /**
     * Entry point of the application.
     * Parses command-line arguments for input and output file paths,
     * then initiates the tap processing workflow.
     *
     * @param args Command-line arguments. Expects up to two arguments:
     *             args[0]: Path to the input taps CSV file (optional, defaults to
     *             "taps.csv").
     *             args[1]: Path to the output trips CSV file (optional, defaults to
     *             "trips.csv").
     */
    public static void main(String[] args) {
        String tapsFilePath = DEFAULT_TAPS_FILE;
        String tripsFilePath = DEFAULT_TRIPS_FILE;

        // Override default file paths if command-line arguments are provided
        if (args.length >= 1) {
            tapsFilePath = args[0];
        }
        if (args.length >= 2) {
            tripsFilePath = args[1];
        }

        System.out.println("Processing taps from: " + tapsFilePath);
        System.out.println("Outputting trips to: " + tripsFilePath);

        try {
            Path inputPath = Paths.get(tapsFilePath);
            Path outputPath = Paths.get(tripsFilePath);

            // Initialize core components
            CsvReader csvReader = new CsvReader();
            // PricingService loads its data internally (e.g., from a static block or
            // constructor)
            PricingService pricingService = new PricingService();
            TripProcessorService tripProcessorService = new TripProcessorService(pricingService);
            CsvWriter csvWriter = new CsvWriter();

            // Read taps from the input CSV file
            List<Tap> taps = csvReader.readTaps(inputPath.toString());

            // Handle cases where no taps are found or the file is empty
            if (taps == null || taps.isEmpty()) {
                System.out.println("No taps found or error reading taps file. Exiting.");
                // As per requirements/assumptions, create an empty trips.csv with headers
                csvWriter.writeTrips(new java.util.ArrayList<>(), outputPath.toString());
                return;
            }

            // Process taps to generate trips
            List<Trip> trips = tripProcessorService.generateTrips(taps);

            // Write the generated trips to the output CSV file
            csvWriter.writeTrips(trips, outputPath.toString());

            System.out.println("Successfully processed " + taps.size() + " taps and generated "
                    + (trips == null ? 0 : trips.size()) + " trips.");

        } catch (InvalidPathException e) {
            System.err.println("Error: Invalid file path provided. " + e.getMessage());
        } catch (FileNotFoundException e) {
            // CsvReader might throw this if the file doesn't exist
            System.err.println("Error: Taps file not found at " + tapsFilePath + ". " + e.getMessage());
        } catch (Exception e) {
            // Catch-all for other unexpected errors during processing
            System.err.println("An unexpected error occurred during processing: " + e.getMessage());
            e.printStackTrace(); // Provides stack trace for debugging
        }
    }
}