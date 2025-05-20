# Running the Littlepay Transit Fare Calculator

This guide provides detailed instructions on how to run the Littlepay Transit Fare Calculation application.

## Prerequisites

1.  **Java Development Kit (JDK):** Version 21 or higher must be installed. You can download it from [Oracle](https://www.oracle.com/java/technologies/downloads/) or use an alternative distribution like OpenJDK.

    - Verify your installation by opening a terminal or command prompt and typing: `java -version`

2.  **Gradle (Optional but Recommended):** The project includes a Gradle wrapper (`gradlew` or `gradlew.bat`), which means you don't need to install Gradle system-wide. However, if you prefer to use a system-installed Gradle, ensure it's a compatible version (e.g., Gradle 7.x or 8.x).

3.  **Project Files:** You need the project source code. If you haven't already, clone the repository or download the source files.

## Steps to Run the Application

There are two primary ways to run the application: using the Gradle `run` task (recommended for development) or by building and running the JAR file.

### Method 1: Using Gradle `run` Task

This method uses Gradle to compile and run the application directly. It's convenient as it handles dependencies and classpath automatically.

1.  **Open a Terminal or Command Prompt:** Navigate to the root directory of the project (where `build.gradle` and `gradlew` files are located).

2.  **Execute the `run` command:**

    - **With Default File Locations:**
      If you have your input file named `taps.csv` in the project root directory, and you want the output file `trips.csv` to be created in the same location, use:

      - On Windows:
        ```bash
        .\gradlew.bat run
        ```
      - On macOS/Linux:
        ```bash
        ./gradlew run
        ```

    - **Specifying Input and Output File Paths:**
      You can provide the paths to your input `taps.csv` file and the desired output `trips.csv` file as command-line arguments using `--args`.

      - On Windows:

        ```bash
        .\gradlew.bat run --args="data\input\taps.csv data\output\trips.csv"
        ```

        _(Replace with actual paths. Use quotes if paths contain spaces.)_

      - On macOS/Linux:
        ```bash
        ./gradlew run --args="data/input/taps.csv data/output/trips.csv"
        ```
        _(Replace with actual paths. Use quotes if paths contain spaces.)_

      **Example:**
      If your taps file is at `data\input\taps.csv` and you want the output at `data\output\trips.csv`:

      - Windows: `.\gradlew.bat run --args="data\input\taps.csv data\output\trips.csv"`
      - macOS/Linux: `./gradlew run --args="data/input/taps.csv data/output/trips.csv"`

3.  **Check Output:** After execution, the `trips.csv` file will be generated at the specified location (or default location). The console will also print processing information.

### Method 2: Building and Running the JAR File

This method involves first building a distributable JAR (Java Archive) file and then running it using the `java` command.

1.  **Build the Project (Create the JAR):**

    - Open a terminal or command prompt in the project root directory.
    - Execute the Gradle `build` task:
      - On Windows:
        ```bash
        .\gradlew.bat build
        ```
      - On macOS/Linux:
        ```bash
        ./gradlew build
        ```
    - This command compiles the code, runs tests, and creates the JAR file. The JAR is typically located in the `build/libs/` directory (e.g., `littlepay-coding-challenge-soreilly-1.0-SNAPSHOT.jar`). The exact name might vary based on `build.gradle` settings.

2.  **Run the JAR File:**

    - Once the JAR is built, navigate to the directory containing the JAR or use its full path.
    - Use the `java -jar` command:

      - **With Default File Locations:**
        If `taps.csv` is in the same directory where you run the command, and `trips.csv` should be created there:

        ```bash
        java -jar build/libs/<your-jar-name>.jar
        ```

        _(Replace `<your-jar-name>.jar` with the actual name of the JAR file, e.g., `littlepay-coding-challenge-soreilly-1.0-SNAPSHOT.jar`)_

      - **Specifying Input and Output File Paths:**
        Provide the file paths as arguments after the JAR name.

        ```bash
        java -jar build/libs/<your-jar-name>.jar data\input\taps.csv data\output\trips.csv
        ```

        **Examples:**

        ```bash
        # Windows Example
        java -jar build\libs\littlepay-coding-challenge-soreilly-1.0-SNAPSHOT.jar data\input\taps.csv data\output\trips.csv

        # macOS/Linux Example
        java -jar build/libs/littlepay-coding-challenge-soreilly-1.0-SNAPSHOT.jar data/input/taps.csv data/output/trips.csv
        ```

        If only one path is provided, it's treated as the input file path, and the output will be `trips.csv` in the directory where the command is run.

3.  **Check Output:** The `trips.csv` file will be created, and console messages will indicate the progress.

## Input Data (`taps.csv`)

- Ensure your input `taps.csv` file is formatted correctly with the following columns in order:
  `ID, DateTimeUTC, TapType, StopId, CompanyId, BusID, PAN`
- `DateTimeUTC` should be in `dd-MM-yyyy HH:mm:ss` format.
- An example `taps.csv` might be provided in the `documentation` or root directory of the project.

## Troubleshooting

- **`java: command not found` or `'java' is not recognized...`:** Java JDK is not installed or not added to your system's PATH. Install Java and ensure it's correctly configured.
- **`gradlew: command not found` or `'gradlew' is not recognized...`:** You are not in the root directory of the project, or the wrapper scripts do not have execute permissions (on macOS/Linux, use `chmod +x ./gradlew`).
- **File Not Found Errors:** Double-check the paths provided for input/output files. Ensure they are correct and the application has read/write permissions for those locations.
- **Build Failures:** Check the console output from Gradle for specific error messages. It might be due to code issues or dependency problems.

For further details on project structure and build configurations, refer to `README.md`.
