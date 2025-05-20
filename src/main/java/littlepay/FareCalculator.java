package littlepay;

import littlepay.model.Tap;
import littlepay.model.Trip;
import littlepay.service.PricingService;
import littlepay.service.TripProcessorService;
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
public class FareCalculator {

    private static final String DEFAULT_TAPS_FILE = "data\\input\\taps.csv";
    private static final String DEFAULT_TRIPS_FILE = "data\\output\\trips.csv";

    /**
     * Processes taps from an input file and writes the resulting trips to an output
     * file.
     * This method contains the core logic of the application.
     *
     * @param rawInputPath  Path to the input taps CSV file.
     * @param rawOutputPath Path to the output trips CSV file.
     * @throws Exception if any error occurs during processing.
     */
    public static void processFiles(Path rawInputPath, Path rawOutputPath) throws Exception {
        Path baseDir = Paths.get(".").toAbsolutePath().normalize();

        Path inputPath = rawInputPath.toAbsolutePath().normalize();
        if (!inputPath.startsWith(baseDir)) {
            throw new SecurityException("Input path is outside the allowed working directory: " + inputPath);
        }

        Path outputPath = rawOutputPath.toAbsolutePath().normalize();
        if (!outputPath.startsWith(baseDir)) {
            throw new SecurityException("Output path is outside the allowed working directory: " + outputPath);
        }

        System.out.println("Processing taps from: " + inputPath);
        System.out.println("Outputting trips to: " + outputPath);

        CsvReader csvReader = new CsvReader();
        PricingService pricingService = new PricingService();
        TripProcessorService tripProcessorService = new TripProcessorService(pricingService);
        CsvWriter csvWriter = new CsvWriter();

        List<Tap> taps = csvReader.readTaps(inputPath.toString());

        if (taps == null || taps.isEmpty()) {
            System.out.println("No taps found or error reading taps file. Creating empty trips file.");
            csvWriter.writeTrips(new java.util.ArrayList<>(), outputPath.toString());
            return;
        }

        List<Trip> trips = tripProcessorService.generateTrips(taps);

        csvWriter.writeTrips(trips, outputPath.toString());

        System.out.println("Successfully processed " + taps.size() + " taps and generated "
                + (trips == null ? 0 : trips.size()) + " trips to " + outputPath);
    }

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

        try {
            Path inputPath = Paths.get(tapsFilePath);
            Path outputPath = Paths.get(tripsFilePath);

            processFiles(inputPath, outputPath);

        } catch (InvalidPathException e) {
            System.err.println("Error: Invalid file path provided. " + e.getMessage());
        } catch (SecurityException e) {
            System.err.println("Security Error: Path access denied. " + e.getMessage());
        } catch (FileNotFoundException e) {
            System.err.println("Error: Input or Output path is invalid. " + e.getMessage());
        } catch (Exception e) {
            System.err.println("An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
