package com.telecom.charging;

import java.math.BigDecimal;

public class Charger implements ICharger {
    private final IRateProvider rateProvider;

    public Charger(IRateProvider rateProvider) {
        this.rateProvider = rateProvider;
    }

    @Override
    public BigDecimal calculateCost(String msisdn, long durationMinutes) {
        BigDecimal rate = rateProvider.getRate(msisdn);
        return rate.multiply(BigDecimal.valueOf(durationMinutes));
    }
}