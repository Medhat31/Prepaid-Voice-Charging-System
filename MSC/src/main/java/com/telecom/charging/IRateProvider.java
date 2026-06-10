
package com.telecom.charging;

import java.math.BigDecimal;

public interface IRateProvider {
    /**
     * Determines the billing rate per minute for a given phone number.
     * @param msisdn The subscriber's phone number.
     * @return The rate per minute (e.g., 1.00 L.E.).
     */
    BigDecimal getRate(String msisdn);
}