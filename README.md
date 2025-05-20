# Littlepay Transit Fare Calculation System

This project implements a system to process tap data from a transit system, calculate fares for trips, and output a list of processed trips.

## Project Overview

The system reads a CSV file (`data\input\taps.csv`) containing tap-on and tap-off events from passengers. It then processes these taps to determine individual trips. For each trip, it calculates the duration, fare, and status (COMPLETED, INCOMPLETE, CANCELLED). The processed trip data is then written to another CSV file (`data\output\trips.csv`).

The core logic involves:

- Parsing tap data.
- Grouping taps by passenger (PAN).
- Matching tap-on and tap-off events chronologically.
- Calculating fares based on predefined stop-to-stop prices and rules for incomplete or cancelled trips.
- Formatting and writing trip data to CSV.

## Prerequisites

- Java 21 JDK
- Gradle (the project uses the Gradle wrapper, so a local Gradle installation is not strictly required if using `./gradlew`)

## How to Build

The project uses Gradle as its build tool.

1.  **Clone the repository:**

    ```bash
    git clone <repository_url>
    cd littlepay-coding-challenge-soreilly
    ```

2.  **Build the project:**
    You can use the Gradle wrapper included in the project.
    - On Windows:
      ```bash
      .\gradlew.bat build
      ```
    - On macOS/Linux:
      ```bash
      ./gradlew build
      ```
      This command will compile the source code, run tests, and assemble the project. A runnable JAR will typically be created in `build/libs/`.

## How to Run

After building the project, you can run the application in two ways:

1.  **Using the Gradle `run` task (recommended for development):**

    - On Windows:
      ```bash
      .\gradlew.bat run
      ```
    - On macOS/Linux:
      ```bash
      ./gradlew run
      ```
      This will use `data\input\taps.csv` and `data\output\trips.csv` by default.

    To specify custom input and output file paths:

    - On Windows:
      ```bash
      .\gradlew.bat run --args="C:\path\to\your\taps.csv C:\path\to\your\trips.csv"
      ```
    - On macOS/Linux:
      ```bash
      ./gradlew run --args="/path/to/your/taps.csv /path/to/your/trips.csv"
      ```

2.  **Running the JAR file:**
    After building, a runnable JAR is created in the `build\libs\` directory:

    ```bash
    java -jar build\libs\littlepay-1.0-SNAPSHOT.jar [path\to\taps.csv] [path\to\trips.csv]
    ```

    If no arguments are provided, it will look for `data\input\taps.csv` and create `data\output\trips.csv`.

**Note:** For security reasons, the application only allows file paths within the project directory.

## Project Structure

- `src\main\java`: Contains the main application source code.
  - `littlepay\`: Root package.
    - `FareCalculator.java`: Main application class.
    - `model\`: Data model classes (Tap, Trip, enums).
    - `service\`: Services like PricingService and TripProcessorService.
    - `util\`: Utility classes like CsvReader, CsvWriter.
- `src\test\java`: Contains unit tests.
- `build.gradle`: Gradle build script.
- `documentation\`: Contains project documentation like requirements and implementation plan.
- `data\`: Contains input and output data files.
  - `input\`: Directory for input files.
    - `taps.csv`: Default input file.
  - `output\`: Directory for output files.
    - `trips.csv`: Default output file.

## Testing

Unit tests are written using JUnit 5 and can be run with Gradle:

- On Windows:
  ```bash
  .\gradlew.bat test
  ```
- On macOS/Linux:
  ```bash
  ./gradlew test
  ```
  Test reports are generated in `build\reports\tests\test\index.html`.

## Key Assumptions

The system operates under the following key assumptions:

1. **Input Format**: The `taps.csv` file has a header row with columns in this order: `ID, DateTimeUTC, TapType, StopId, CompanyId, BusID, PAN`.

2. **Trip Processing**:
   - Taps are processed in chronological order for each PAN.
   - An ON tap is matched with the next chronological OFF tap for the same PAN.
   - Multiple ON taps before an OFF tap result in the first ON tap being treated as an INCOMPLETE trip.
   - An ON tap without a matching OFF tap results in an INCOMPLETE trip charged at the maximum fare.
   - ON and OFF taps at the same stop result in a CANCELLED trip with $0.00 fare.

3. **Pricing**:
   - Stop1 - Stop2: $3.25
   - Stop2 - Stop3: $5.50
   - Stop1 - Stop3: $7.30
   - Fares are the same regardless of direction.

For more detailed assumptions, see `documentation\ASSUMPTIONS.md`.
