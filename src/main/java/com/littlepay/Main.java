package com.littlepay;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import littlepay.util.CsvReader;
import littlepay.model.Tap;
import littlepay.model.Trip;

// Placeholder Main class
public class Main {

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java com.littlepay.Main <input_taps.csv> <output_trips.csv>");
            System.err.println("Using default file names: taps.csv and trips.csv");
            // Default paths relative to where the application is run from
            processFiles(Paths.get("taps.csv"), Paths.get("trips.csv"));
            return;
        }
        processFiles(Paths.get(args[0]), Paths.get(args[1]));
    }

    public static void processFiles(Path inputPath, Path outputPath) {
        System.out.println("Processing files: Input=" + inputPath + ", Output=" + outputPath);
        try {
            CsvReader reader = new CsvReader();
            Pricing pricing = new Pricing(); // Initialize pricing
            TripProcessorService processor = new TripProcessorService(pricing);
            CsvWriter writer = new CsvWriter();

            List<Tap> taps = reader.readTaps(inputPath.toString());
            List<Trip> trips = processor.processTaps(taps);
            writer.writeTrips(outputPath, trips);

            System.out.println("Processing complete.");

        } catch (IOException e) {
            System.err.println("An error occurred during processing: " + e.getMessage());
            e.printStackTrace();
        }
    }
}