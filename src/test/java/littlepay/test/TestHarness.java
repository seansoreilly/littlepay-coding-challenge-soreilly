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
    private static final String taps_CSV = "taps.csv";
    private static final String EXPECTED_TRIPS_CSV = "expected_trips.csv";
    private static final String GENERATED_TRIPS_CSV = "trips.csv"; // Output from the app

    private void runApplicationLogic(Path inputTapsFile, Path outputTripsFile) throws Exception {
        System.out.println("Running application logic: " + inputTapsFile + " -> " + outputTripsFile);

        Files.deleteIfExists(outputTripsFile); // Ensure a fresh run

        Main.processFiles(inputTapsFile, outputTripsFile);

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
        Path inputTapsFile = testCaseDir.resolve(taps_CSV);
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