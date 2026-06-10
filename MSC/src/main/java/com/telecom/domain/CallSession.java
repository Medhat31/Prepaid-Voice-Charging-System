package com.telecom.domain;

import java.time.Duration;
import java.time.LocalDateTime;

public class CallSession {
    private final String msisdn;
    private final LocalDateTime startTime;

    public CallSession(String msisdn) {
        this.msisdn = msisdn;
        this.startTime = LocalDateTime.now();
    }

    public String getMsisdn() {
        return msisdn;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public long getElapsedMinutes(LocalDateTime endTime) {
        Duration duration = Duration.between(startTime, endTime);
        long seconds = duration.getSeconds();
        
        if (seconds == 0) return 0;
        return (seconds + 59) / 60; 
    }
}