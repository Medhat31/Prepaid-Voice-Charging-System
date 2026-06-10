
package com.telecom.charging;

import java.math.BigDecimal;

public interface ICharger {
    /**
     * Computes the total monetary cost based on call duration.
     * @param msisdn The phone number used to determine the rate rules.
     * @param durationMinutes The rounded-up call duration.
     * @return The final calculated cost as a BigDecimal.
     */
    BigDecimal calculateCost(String msisdn, long durationMinutes);
}