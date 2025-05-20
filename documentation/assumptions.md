# Assumptions

This document lists the assumptions made during the development of the Littlepay Transit Fare Calculation System.

## CSV Data Processing

1.  **`taps.csv` Format:**

    - The input `taps.csv` is expected to have a header row.
    - The column order is assumed to be: `ID, DateTimeUTC, TapType, StopId, CompanyId, BusID, PAN`.
    - `DateTimeUTC` is in "dd-MM-yyyy HH:mm:ss" format.
    - `TapType` is either "ON" or "OFF".
    - `StopId` corresponds to one of the predefined stops (Stop1, Stop2, Stop3).

2.  **Malformed Rows in `taps.csv`:**

    - If a row in `taps.csv` is malformed (e.g., incorrect number of columns, unparsable date, invalid `TapType` or `StopId`), it will be logged as an error to `System.err` and skipped. The processing will continue with the next valid row.
    - A `Tap` object will not be created for a malformed row.

3.  **Empty `taps.csv`:**
    - If `taps.csv` is empty or contains only a header row, an empty `trips.csv` (with only headers) will be generated.

## Trip Processing Logic

1.  **Chronological Order:**

    - Taps for each PAN (Primary Account Number) are processed in strict chronological order based on `DateTimeUTC`.

2.  **Matching Taps:**

    - An `ON` tap is matched with the _next_ chronological `OFF` tap for the same PAN.

3.  **OFF Tap Without Preceding ON Tap:**

    - If an `OFF` tap is encountered for a PAN without a preceding `ON` tap (i.e., no unmatched `ON` tap exists for that PAN), this `OFF` tap is ignored and logged. It does not result in a trip.

4.  **Multiple ON Taps Before an OFF Tap:**

    - If multiple `ON` taps occur for a PAN before an `OFF` tap (e.g., ON-ON-OFF):
      - The first `ON` tap will be treated as an INCOMPLETE trip (charged max fare from its `StopId`).
      - The second `ON` tap will then be paired with the `OFF` tap to form a COMPLETED or CANCELLED trip.
      - This is because each `ON` tap must be resolved before the next tap for that PAN is considered for pairing. If an `ON` tap is followed by another `ON` tap for the same PAN, the first one becomes incomplete.

5.  **Incomplete Trips:**

    - An `ON` tap that remains unmatched after processing all subsequent taps for that PAN results in an INCOMPLETE trip.
    - `ChargeAmount`: The maximum possible fare from the `ON` tap's `StopId` (as defined in `PricingService`).
    - `ToStopId`: Set to be the same as the `FromStopId` (the `ON` tap's `StopId`).
    - `Finished` Timestamp: Set to be the same as the `Started` timestamp (the `ON` tap's `DateTimeUTC`).
    - `DurationSecs`: Set to 0.
    - `Status`: INCOMPLETE.

6.  **Cancelled Trips:**

    - Occur when an `ON` tap and its subsequent matching `OFF` tap for the same PAN have the same `StopId`.
    - `ChargeAmount`: $0.00.
    - `Status`: CANCELLED.

7.  **Pricing:**
    - The fare prices are fixed and defined in `PricingService.java`.
    - `Stop1 - Stop2: $3.25`
    - `Stop2 - Stop3: $5.50`
    - `Stop1 - Stop3: $7.30`
    - The fare is the same regardless of direction (e.g., Stop1 to Stop2 costs the same as Stop2 to Stop1).
    - Maximum fare from a stop is defined as the highest fare for any trip originating from that stop.
      - Max from Stop1: $7.30 (to Stop3)
      - Max from Stop2: $5.50 (to Stop3)
      - Max from Stop3: $7.30 (to Stop1)

## Output `trips.csv`

1.  **Column Order:** `Started, Finished, DurationSecs, FromStopId, ToStopId, ChargeAmount, CompanyId, BusID, PAN, Status`.
2.  **Date/Time Formatting:** `Started` and `Finished` fields are formatted as "dd-MM-yyyy HH:mm:ss".
3.  **ChargeAmount Formatting:** Formatted as a string with a dollar sign and two decimal places (e.g., "$3.25", "$0.00").
4.  **Null/Empty Values:**
    - For INCOMPLETE trips, `Finished` will be the same as `Started`, and `ToStopId` will be the same as `FromStopId`.
    - If any original tap data for `CompanyId`, `BusID`, or `PAN` is missing or null (though not expected per current `Tap` record definition), it would result in an empty string in the output for those fields.

## Application Behavior

1.  **Command-Line Arguments:**

    - The application can accept two optional command-line arguments: the input `taps.csv` file path and the output `trips.csv` file path.
    - If no arguments are provided, it defaults to `taps.csv` in the current working directory for input and `trips.csv` in the current working directory for output.
    - If one argument is provided, it's used as the input file path; output remains the default.

2.  **File Handling:**
    - If the input `taps.csv` file is not found, an error is printed, and the application exits gracefully.
    - The application will overwrite the output `trips.csv` file if it already exists.

## General

1.  **Data Integrity:** The system assumes that the PAN, CompanyID, and BusID are consistent between an ON tap and its corresponding OFF tap. The values from the ON tap are primarily used for trip details where applicable, but the OFF tap's existence (and its StopID/Timestamp) is key for completing or cancelling a trip.
