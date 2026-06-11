package com.telecom.app;

import java.io.PrintWriter;

/**
 * Interface defining the capabilities of the Mobile Switching Center (MSC) orchestrator.
 * Handles the state management, real-time charging triggers, and teardown of active voice sessions.
 */
public interface IMSC {

    /**
     * Initiates an up-front credit check and starts an active call session
     * alongside a background real-time recurring billing thread.
     *
     * @param msisdn The phone number initiating the voice session.
     */
   void onCallStart(String msisdn, PrintWriter clientWriter);;

    /**
     * Terminates an active call session normally when a user hangs up.
     * Cleans up running background tasks and generates a Call Detail Record (CDR).
     *
     * @param msisdn The phone number terminating the voice session.
     */
    void onCallEnd(String msisdn);
}