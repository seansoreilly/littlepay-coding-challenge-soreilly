package littlepay.service;

import littlepay.model.Stop;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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

    public PricingService() {
        addFare(Stop.STOP1, Stop.STOP2, "3.25");
        addFare(Stop.STOP2, Stop.STOP3, "5.50");
        addFare(Stop.STOP1, Stop.STOP3, "7.30");
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