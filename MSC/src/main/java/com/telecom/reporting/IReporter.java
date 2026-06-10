
package com.telecom.reporting;

import com.telecom.domain.CDR;

public interface IReporter {
    /**
     * Dispatches a finalized Call Detail Record to an output target.
     * @param cdr The completed call metrics.
     */
    void report(CDR cdr);
}