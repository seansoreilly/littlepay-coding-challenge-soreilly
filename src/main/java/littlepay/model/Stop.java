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
        // Converts STOP1 to Stop1
        return name.charAt(0) + name.substring(1).toLowerCase();
    }
}