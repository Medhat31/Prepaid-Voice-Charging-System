package com.telecom.reporting;

import com.telecom.domain.CDR;

public class ScreenReporter implements IReporter {
    @Override
    public void report(CDR cdr) {
        System.out.println("[MSC CDR LOG]: " + cdr.toString());
    }
}