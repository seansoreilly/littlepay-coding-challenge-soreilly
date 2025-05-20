package littlepay.service;

import littlepay.model.Stop;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

public class PricingService {

    private record StopPair(Stop stop1, Stop stop2) {
        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            StopPair stopPair = (StopPair) o;
            return (Objects.equals(stop1, stopPair.stop1) && Objects.equals(stop2, stopPair.stop2)) ||
                    (Objects.equals(stop1, stopPair.stop2) && Objects.equals(stop2, stopPair.stop1));
        }

        @Override
        public int hashCode() {
            return Objects.hash(stop1, stop2) + Objects.hash(stop2, stop1);
        }
    }

    private final Map<StopPair, BigDecimal> fares = new HashMap<>();
    private static final String FARES_CONFIG_FILE = "config/fares.properties";

    public PricingService() {
        loadFaresFromConfig();
    }

    private void loadFaresFromConfig() {
        Properties properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(FARES_CONFIG_FILE)) {
            if (input == null) {
                System.err.println("Unable to find " + FARES_CONFIG_FILE);
                // Fall back to default values if config file is not found
                addFare(Stop.STOP1, Stop.STOP2, "3.25");
                addFare(Stop.STOP2, Stop.STOP3, "5.50");
                addFare(Stop.STOP1, Stop.STOP3, "7.30");
                return;
            }

            properties.load(input);

            // Load fares from properties
            for (String key : properties.stringPropertyNames()) {
                String[] stops = key.split("_");
                if (stops.length == 2) {
                    try {
                        Stop stop1 = Stop.valueOf(stops[0]);
                        Stop stop2 = Stop.valueOf(stops[1]);
                        String fareValue = properties.getProperty(key);
                        addFare(stop1, stop2, fareValue);
                    } catch (IllegalArgumentException e) {
                        System.err.println("Invalid stop in config: " + key);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading fares configuration: " + e.getMessage());
            // Fall back to default values if there's an error
            addFare(Stop.STOP1, Stop.STOP2, "3.25");
            addFare(Stop.STOP2, Stop.STOP3, "5.50");
            addFare(Stop.STOP1, Stop.STOP3, "7.30");
        }
    }

    private void addFare(Stop s1, Stop s2, String amount) {
        fares.put(new StopPair(s1, s2), new BigDecimal(amount));
    }

    public BigDecimal getFare(Stop fromStop, Stop toStop) {
        if (fromStop == toStop) {
            return BigDecimal.ZERO;
        }
        BigDecimal fare = fares.get(new StopPair(fromStop, toStop));
        if (fare == null) {
            throw new IllegalArgumentException("No fare defined for route between " + fromStop + " and " + toStop);
        }
        return fare;
    }

    public BigDecimal getMaxFare(Stop fromStop) {
        return fares.entrySet().stream()
                .filter(entry -> entry.getKey().stop1() == fromStop || entry.getKey().stop2() == fromStop)
                .map(Map.Entry::getValue)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }
}
