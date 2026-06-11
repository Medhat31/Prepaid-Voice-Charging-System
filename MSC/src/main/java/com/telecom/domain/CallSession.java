package com.telecom.domain;

import java.io.PrintWriter;
import java.time.Duration;
import java.time.LocalDateTime;

public class CallSession {

    private final String msisdn;
    private final LocalDateTime startTime;
    private final PrintWriter writer;

    public CallSession(String msisdn, PrintWriter writer) {
        this.msisdn = msisdn;
        this.startTime = LocalDateTime.now();
        this.writer = writer;
    }

    public String getMsisdn() {
        return msisdn;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public PrintWriter getWriter() {
        return writer;
    }

    public long getElapsedMinutes(LocalDateTime endTime) {
        Duration duration = Duration.between(startTime, endTime);
        long seconds = duration.getSeconds();

        if (seconds == 0) {
            return 0;
        }
        return (seconds + 59) / 60;
    }
}
