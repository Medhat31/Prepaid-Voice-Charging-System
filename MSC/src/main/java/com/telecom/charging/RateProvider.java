package com.telecom.charging;

import java.math.BigDecimal;

public class RateProvider implements IRateProvider {
    @Override
    public BigDecimal getRate(String msisdn) {
        
        /* For different rates, database has to contain the rate for different
        services (ex: 1 L.E. for calls , 0.5 L.E. for 1 MB of data usage), but 
        in our case it is voice calls charging only
        */
  
        // Hardcoded flat rate as per requirements: 1.00 L.E.
        
        return new BigDecimal("1.00");
    }
}