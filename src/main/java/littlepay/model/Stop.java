package littlepay.model;

public enum Stop {
    STOP1,
    STOP2,
    STOP3;

    public static Stop fromString(String text) {
        if (text != null) {
            for (Stop b : Stop.values()) {
                if (text.trim().equalsIgnoreCase(b.name())) {
                    return b;
                }
            }
        }
        throw new IllegalArgumentException("Unknown stop ID: " + text);
    }
}