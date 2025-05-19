package littlepay.test;

import littlepay.Main;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TestHarness {

    private static final Path TEST_CASES_DIR = Paths.get("src", "test", "resources", "test-cases");
    private static final String INPUT_TAPS_CSV = "input_taps.csv";
    private static final String EXPECTED_TRIPS_CSV = "expected_trips.csv";
    private static final String GENERATED_TRIPS_CSV = "generated_trips.csv"; // Output from the app

    // Placeholder for main application entry or direct service call
    // This would typically be:
    // Main.processFiles(inputPath, outputPath) or
    // new CsvReader().read(...) -> new TripProcessorService().process(...) -> new
    // CsvWriter().write(...)
    private void runApplicationLogic(Path inputTapsFile, Path outputTripsFile) throws Exception {
        System.out.println("Running application logic: " + inputTapsFile + " -> " + outputTripsFile);
        // In a real scenario:
        Files.deleteIfExists(outputTripsFile); // Ensure a fresh run

        // Call the main processing logic
        Main.processFiles(inputTapsFile, outputTripsFile);

        // For now, let's create a dummy output file to simulate the app running
        // If an expected_trips.csv exists, we can copy it to generated_trips.csv
        // to make the test pass, or create a simple dummy file.
        /*
         * Path parentDir = inputTapsFile.getParent();
         * Path expectedFile = parentDir.resolve(EXPECTED_TRIPS_CSV);
         * 
         * if (Files.exists(expectedFile)) {
         * // Simulate the app producing the *exact* expected output for now
         * Files.copy(expectedFile, outputTripsFile,
         * StandardCopyOption.REPLACE_EXISTING);
         * } else {
         * // Or create a very basic dummy generated file if no expected is present for
         * some reason
         * List<String> dummyContent = List.of("Header1,Header2", "Data1,Data2");
         * Files.write(outputTripsFile, dummyContent);
         * }
         */
        System.out.println("Finished running application logic.");
    }

    static Stream<Path> testCaseProvider() throws IOException {
        if (!Files.isDirectory(TEST_CASES_DIR)) {
            return Stream.empty();
        }
        return Files.list(TEST_CASES_DIR)
                .filter(Files::isDirectory)
                .sorted();
    }

    @ParameterizedTest
    @MethodSource("testCaseProvider")
    void runTestCase(Path testCaseDir) throws Exception {
        Path inputTapsFile = testCaseDir.resolve(INPUT_TAPS_CSV);
        Path expectedTripsFile = testCaseDir.resolve(EXPECTED_TRIPS_CSV);
        Path generatedTripsFile = testCaseDir.resolve(GENERATED_TRIPS_CSV); // App will write here

        Assertions.assertTrue(Files.exists(inputTapsFile), "Input taps file missing: " + inputTapsFile);
        // Expected file is not strictly necessary for the app to run, but needed for
        // verification
        Assertions.assertTrue(Files.exists(expectedTripsFile), "Expected trips file missing: " + expectedTripsFile);

        // Clean up any previously generated file
        Files.deleteIfExists(generatedTripsFile);

        System.out.println("Running test case: " + testCaseDir.getFileName());
        runApplicationLogic(inputTapsFile, generatedTripsFile);

        Assertions.assertTrue(Files.exists(generatedTripsFile),
                "Generated trips file was not created: " + generatedTripsFile);

        compareCsvFiles(expectedTripsFile, generatedTripsFile);
    }

    private void compareCsvFiles(Path expectedFile, Path actualFile) throws IOException {
        List<String> expectedLines = Files.readAllLines(expectedFile).stream()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .collect(Collectors.toList());
        List<String> actualLines = Files.readAllLines(actualFile).stream()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .collect(Collectors.toList());

        // Could use a more sophisticated CSV comparison library for production
        // For now, simple line-by-line string comparison after trimming.
        Assertions.assertEquals(expectedLines.size(), actualLines.size(),
                "Number of lines differ. Expected: " + expectedLines.size() + ", Actual: " + actualLines.size() +
                        "\nExpected File: " + expectedFile.toAbsolutePath() +
                        "\nActual File: " + actualFile.toAbsolutePath());

        for (int i = 0; i < expectedLines.size(); i++) {
            Assertions.assertEquals(expectedLines.get(i), actualLines.get(i),
                    "Line " + (i + 1) + " differs." +
                            "\nExpected: <" + expectedLines.get(i) + ">" +
                            "\nActual:   <" + actualLines.get(i) + ">" +
                            "\nExpected File: " + expectedFile.toAbsolutePath() +
                            "\nActual File: " + actualFile.toAbsolutePath());
        }
        System.out.println("CSV files match: " + expectedFile.getFileName() + " and " + actualFile.getFileName());
    }
}