package com.example.exchange_server;

package com.example.exchange_server.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

class ValidatePriceTests {

    private ValidatePrice validatePrice;

    // Baseline: currentPrice=100, openingPrice=100
    // Tick rule:  |proposed - current| must be >= 100 * 0.005 = 0.50
    //             → accepted if proposed <= 99.50 or proposed >= 100.50
    // Daily band: proposed must be within [80.00, 120.00]

    @BeforeEach
    void setUp() {
        validatePrice = new ValidatePrice();
    }

    // -------------------------------------------------------------------------
    // Rule 1 — 0.5% minimum tick
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Rule 1: minimum tick (0.5% from currentPrice)")
    class MinimumTickRule {

        @Test
        @DisplayName("price equal to currentPrice is rejected (zero change)")
        void noChange_rejected() {
            assertThat(validatePrice.validatePrice(100.0, 100.0, 100.0)).isEmpty();
        }

        @Test
        @DisplayName("price change of 0.49% upward is rejected (below minimum tick)")
        void justBelowMinTickUp_rejected() {
            // change = 0.49 < 0.50 required
            assertThat(validatePrice.validatePrice(100.0, 100.49, 100.0)).isEmpty();
        }

        @Test
        @DisplayName("price change of exactly 0.5% upward is accepted (at boundary)")
        void atMinTickBoundaryUp_accepted() {
            assertThat(validatePrice.validatePrice(100.0, 100.50, 100.0))
                    .isPresent().contains(100.50);
        }

        @Test
        @DisplayName("price change of exactly 0.5% downward is accepted (at boundary)")
        void atMinTickBoundaryDown_accepted() {
            assertThat(validatePrice.validatePrice(100.0, 99.50, 100.0))
                    .isPresent().contains(99.50);
        }

        @Test
        @DisplayName("price change of 0.49% downward is rejected")
        void justBelowMinTickDown_rejected() {
            // change = 0.49 < 0.50 required
            assertThat(validatePrice.validatePrice(100.0, 99.51, 100.0)).isEmpty();
        }

        @Test
        @DisplayName("minimum tick scales with currentPrice, not openingPrice")
        void tickScalesWithCurrentPrice_notOpeningPrice() {
            // currentPrice=200 → required move = 200 * 0.005 = 1.0
            // proposed=200.99: change=0.99 < 1.0 → rejected
            assertThat(validatePrice.validatePrice(200.0, 200.99, 100.0)).isEmpty();

            // proposed=201.0: change=1.0 = minimum → accepted
            assertThat(validatePrice.validatePrice(200.0, 201.0, 100.0))
                    .isPresent().contains(201.0);
        }
    }

    // -------------------------------------------------------------------------
    // Rule 2 — ±20% daily band from openingPrice
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Rule 2: daily band (±20% from openingPrice)")
    class DailyBandRule {

        // Use a current price far enough from the proposed prices so Rule 1
        // never interferes with the band-edge boundary tests.
        // currentPrice=100, opening=100 → band=[80, 120]
        // All proposed prices here move > 0.5 from current=100.

        @Test
        @DisplayName("price at exactly +20% of opening is accepted (inclusive upper boundary)")
        void atUpperBoundary_accepted() {
            assertThat(validatePrice.validatePrice(100.0, 120.0, 100.0))
                    .isPresent().contains(120.0);
        }

        @Test
        @DisplayName("price 0.01 above +20% of opening is rejected")
        void justAboveUpperBoundary_rejected() {
            assertThat(validatePrice.validatePrice(100.0, 120.01, 100.0)).isEmpty();
        }

        @Test
        @DisplayName("price at exactly -20% of opening is accepted (inclusive lower boundary)")
        void atLowerBoundary_accepted() {
            assertThat(validatePrice.validatePrice(100.0, 80.0, 100.0))
                    .isPresent().contains(80.0);
        }

        @Test
        @DisplayName("price 0.01 below -20% of opening is rejected")
        void justBelowLowerBoundary_rejected() {
            assertThat(validatePrice.validatePrice(100.0, 79.99, 100.0)).isEmpty();
        }

        @Test
        @DisplayName("daily band is anchored to openingPrice, not currentPrice")
        void bandAnchoredToOpeningPrice() {
            // After a big intraday move: current=115, opening=100 → band still [80, 120]
            // proposed=119: tick change=4 > 0.575 ✓, and 119 <= 120 ✓
            assertThat(validatePrice.validatePrice(115.0, 119.0, 100.0))
                    .isPresent().contains(119.0);

            // proposed=121: tick passes, but 121 > 120 → rejected
            assertThat(validatePrice.validatePrice(115.0, 121.0, 100.0)).isEmpty();
        }
    }

    // -------------------------------------------------------------------------
    // Both rules must pass simultaneously
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Interaction: both rules are independent gates")
    class BothRulesInteraction {

        @Test
        @DisplayName("price passes tick rule but fails daily band → rejected")
        void passesTickFailsBand() {
            // current=100, proposed=125: change=25 >> 0.5 ✓  but 125 > 120 ✗
            assertThat(validatePrice.validatePrice(100.0, 125.0, 100.0)).isEmpty();
        }

        @Test
        @DisplayName("price fails tick rule but would pass daily band → rejected")
        void failsTickPassesBand() {
            // current=100, proposed=100.49: within band ✓  but change=0.49 < 0.5 ✗
            assertThat(validatePrice.validatePrice(100.0, 100.49, 100.0)).isEmpty();
        }

        @Test
        @DisplayName("price passes both rules → accepted, value preserved exactly")
        void passesBothRules_acceptedWithExactValue() {
            // current=100, proposed=101: change=1.0 >= 0.5 ✓  and 80 <= 101 <= 120 ✓
            assertThat(validatePrice.validatePrice(100.0, 101.0, 100.0))
                    .isPresent().contains(101.0);
        }

        @Test
        @DisplayName("valid downward move inside band")
        void validDownwardMove() {
            // current=100, proposed=95: change=5 >= 0.5 ✓  and 95 >= 80 ✓
            assertThat(validatePrice.validatePrice(100.0, 95.0, 100.0))
                    .isPresent().contains(95.0);
        }

        @Test
        @DisplayName("price near lower band edge that also satisfies tick rule")
        void nearLowerBandEdge_satisfiesBothRules() {
            // current=82, proposed=80: change=2 >= 82*0.005=0.41 ✓  and 80 == lower bound ✓
            assertThat(validatePrice.validatePrice(82.0, 80.0, 100.0))
                    .isPresent().contains(80.0);
        }

        @Test
        @DisplayName("price near lower band edge that fails tick rule")
        void nearLowerBandEdge_failsTick() {
            // current=80.2, proposed=80.0: change=0.2 < 80.2*0.005=0.401 → rejected
            assertThat(validatePrice.validatePrice(80.2, 80.0, 100.0)).isEmpty();
        }
    }

    // -------------------------------------------------------------------------
    // validateOrThrow — throwing variant
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("validateOrThrow: throwing variant")
    class ValidateOrThrow {

        @Test
        @DisplayName("returns price when both rules pass")
        void validPrice_returnsValue() {
            double result = validatePrice.validateOrThrow(100.0, 101.0, 100.0);
            assertThat(result).isEqualTo(101.0);
        }

        @Test
        @DisplayName("throws IllegalArgumentException when tick rule fails")
        void failsTick_throws() {
            assertThatThrownBy(() -> validatePrice.validateOrThrow(100.0, 100.49, 100.0))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("throws IllegalArgumentException when daily band fails")
        void failsBand_throws() {
            assertThatThrownBy(() -> validatePrice.validateOrThrow(100.0, 125.0, 100.0))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("exception message includes proposed price, current price, and band limits")
        void exceptionMessageIsInformative() {
            assertThatThrownBy(() -> validatePrice.validateOrThrow(100.0, 125.0, 100.0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("125") // proposed
                    .hasMessageContaining("80")  // lower bound
                    .hasMessageContaining("120") // upper bound
                    .hasMessageContaining("100"); // current price
        }
    }
}
