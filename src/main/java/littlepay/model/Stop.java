package littlepay.model;

public enum Stop {
    STOP1,
    STOP2,
    STOP3;

    public static Stop fromString(String text) {
        if (text != null) {
            for (Stop b : Stop.values()) {
                if (text.trim().equalsIgnoreCase(b.name()) || text.trim().equalsIgnoreCase(b.toPascalCase())) {
                    return b;
                }
            }
        }
        throw new IllegalArgumentException("Unknown stop ID: " + text);
    }

    public String toPascalCase() {
        String name = name(); // STOP1, STOP2, etc.
        if (name == null || name.isEmpty()) {
            return name;
        }

        // Extract the stop number (1, 2, 3, etc.)
        String stopNumber = name.substring(4); // Assumes format is "STOP1", "STOP2", etc.

        // Return in Pascal case format: "Stop1", "Stop2", etc.
        return "Stop" + stopNumber;
    }
}