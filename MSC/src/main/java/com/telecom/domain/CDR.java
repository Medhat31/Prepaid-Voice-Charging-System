package com.telecom.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CDR {
    private final String msisdn;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final long durationMinutes;
    private final CallResult result;
    private final BigDecimal callCost;
    private final BigDecimal balanceAfterCall;

    public CDR(String msisdn, LocalDateTime startTime, LocalDateTime endTime, 
               long durationMinutes, CallResult result, BigDecimal callCost, BigDecimal balanceAfterCall) {
        this.msisdn = msisdn;
        this.startTime = startTime;
        this.endTime = endTime;
        this.durationMinutes = durationMinutes;
        this.result = result;
        this.callCost = callCost;
        this.balanceAfterCall = balanceAfterCall;
    }

    // Getters
    public String getMsisdn() { return msisdn; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public long getDurationMinutes() { return durationMinutes; }
    public CallResult getResult() { return result; }
    public BigDecimal getCallCost() { return callCost; }
    public BigDecimal getBalanceAfterCall() { return balanceAfterCall; }

    /**
     * Formats the CDR exactly as required by the assignment specs:
     * MSISDN, Start Time, End Time, Duration, Call result, Call cost, Balance after call
     */
    @Override
    public String toString() {
        return String.format("%s, %s, %s, %d, %s, %s, %s",
                msisdn,
                startTime.toString(),
                endTime.toString(),
                durationMinutes,
                result.toString().replace("_", " ").toLowerCase(), // Converts NORMAL_CALL_CLEARING to "normal call clearing"
                callCost.toPlainString(),
                balanceAfterCall.toPlainString()
        );
    }
}