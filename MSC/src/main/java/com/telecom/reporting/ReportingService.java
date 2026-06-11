package com.telecom.reporting;

import com.telecom.domain.CDR;
import java.util.ArrayList;
import java.util.List;

public class ReportingService implements IReporter {
    private final List<IReporter> reporters = new ArrayList<>();

    public ReportingService() {
        // Automatically link both output destinations
        reporters.add(new ScreenReporter());
        reporters.add(new FileReporter());
    }

    @Override
    public void report(CDR cdr) {
        // Broadcast the CDR to all registered output systems
        for (IReporter reporter : reporters) {
            reporter.report(cdr);
        }
    }
}