package com.littlepay;

import littlepay.model.Stop;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

// Placeholder Pricing class
public class Pricing {
    private final Map<String, BigDecimal> routeCosts = new HashMap<>();
    private final Map<String, BigDecimal> maxFares = new HashMap<>();

    public Pricing() {
        // Initialize with some dummy data as per plan
        // Stop1-Stop2: $3.25
        // Stop2-Stop3: $5.50
        // Stop1-Stop3: $7.30
        routeCosts.put(Stop.STOP1.name() + "-" + Stop.STOP2.name(), new BigDecimal("3.25"));
        routeCosts.put(Stop.STOP2.name() + "-" + Stop.STOP1.name(), new BigDecimal("3.25"));
        routeCosts.put(Stop.STOP2.name() + "-" + Stop.STOP3.name(), new BigDecimal("5.50"));
        routeCosts.put(Stop.STOP3.name() + "-" + Stop.STOP2.name(), new BigDecimal("5.50"));
        routeCosts.put(Stop.STOP1.name() + "-" + Stop.STOP3.name(), new BigDecimal("7.30"));
        routeCosts.put(Stop.STOP3.name() + "-" + Stop.STOP1.name(), new BigDecimal("7.30"));

        // Max fares (example)
        maxFares.put(Stop.STOP1.name(), new BigDecimal("7.30"));
        maxFares.put(Stop.STOP2.name(), new BigDecimal("5.50"));
        maxFares.put(Stop.STOP3.name(), new BigDecimal("7.30"));
    }

    public BigDecimal getCost(Stop fromStop, Stop toStop) {
        if (fromStop == null || toStop == null)
            return BigDecimal.ZERO;
        return routeCosts.getOrDefault(fromStop.name() + "-" + toStop.name(), BigDecimal.ZERO);
    }

    public BigDecimal getMaxFare(Stop fromStop) {
        if (fromStop == null)
            return BigDecimal.ZERO;
        return maxFares.getOrDefault(fromStop.name(), BigDecimal.ZERO); // Default to 0 if stop not found
    }
}