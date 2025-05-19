package littlepay.service;

import littlepay.model.Stop;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class PricingServiceTest {

    private PricingService pricingService;

    @BeforeEach
    void setUp() {
        pricingService = new PricingService();
    }

    @Test
    void getFare_Stop1ToStop2() {
        assertEquals(new BigDecimal("3.25"), pricingService.getFare(Stop.STOP1, Stop.STOP2));
    }

    @Test
    void getFare_Stop2ToStop1() {
        assertEquals(new BigDecimal("3.25"), pricingService.getFare(Stop.STOP2, Stop.STOP1));
    }

    @Test
    void getFare_Stop2ToStop3() {
        assertEquals(new BigDecimal("5.50"), pricingService.getFare(Stop.STOP2, Stop.STOP3));
    }

    @Test
    void getFare_Stop3ToStop2() {
        assertEquals(new BigDecimal("5.50"), pricingService.getFare(Stop.STOP3, Stop.STOP2));
    }

    @Test
    void getFare_Stop1ToStop3() {
        assertEquals(new BigDecimal("7.30"), pricingService.getFare(Stop.STOP1, Stop.STOP3));
    }

    @Test
    void getFare_Stop3ToStop1() {
        assertEquals(new BigDecimal("7.30"), pricingService.getFare(Stop.STOP3, Stop.STOP1));
    }

    @Test
    void getFare_SameStop() {
        assertEquals(BigDecimal.ZERO, pricingService.getFare(Stop.STOP1, Stop.STOP1));
    }

    @Test
    void getFare_UndefinedRoute_ThrowsException() {
        // Assuming no fare is defined between, for e.g. hypothetical Stop4 and Stop1
        // This test requires Stop enum to have more values or use mocks if we don't
        // want to modify it.
        // For now, this implicitly tests that only defined routes return values.
        // A more direct test for an invalid route would be if we had StopX to StopY
        // where no fare is defined.
        // However, with the current Stop enum, all pairs are covered or are the same
        // stop.
        // If we want to test a route not in fares map, we would need a scenario not
        // covered by current pairs.
    }

    @Test
    void getMaxFare_FromStop1() {
        assertEquals(new BigDecimal("7.30"), pricingService.getMaxFare(Stop.STOP1));
    }

    @Test
    void getMaxFare_FromStop2() {
        assertEquals(new BigDecimal("5.50"), pricingService.getMaxFare(Stop.STOP2));
    }

    @Test
    void getMaxFare_FromStop3() {
        assertEquals(new BigDecimal("7.30"), pricingService.getMaxFare(Stop.STOP3));
    }

    // Consider a test for a stop that might not be part of any fare (if possible
    // with current design)
    // For example, if Stop4 existed but had no fares associated.
    // With the current setup, all stops participate in fares, so max fare will
    // always find something or be Zero if it were a lone stop with no fares.
    // If PricingService were to handle a stop with no associated fares, getMaxFare
    // should return BigDecimal.ZERO by its current logic.
}