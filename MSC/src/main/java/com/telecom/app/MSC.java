package com.telecom.app;

import com.telecom.domain.CallResult;
import com.telecom.domain.CallSession;
import com.telecom.domain.CDR;
import com.telecom.repository.IBalanceRepository;
import com.telecom.charging.ICharger;
import com.telecom.reporting.IReporter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class MSC {

    private final IBalanceRepository balanceRepository;
    private final ICharger charger;
    private final IReporter reportingService;

    // Thread-safe map to track active calls by their MSISDN
    private final Map<String, CallSession> activeSessions = new ConcurrentHashMap<>();
    
    // Thread-safe map to track active background billing tasks
    private final Map<String, ScheduledFuture<?>> activeTimers = new ConcurrentHashMap<>();

    // Multi-threaded scheduler to handle real-time minute-by-minute deductions
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);

    public MSC(IBalanceRepository balanceRepository, ICharger charger, IReporter reportingService) {
        this.balanceRepository = balanceRepository;
        this.charger = charger;
        this.reportingService = reportingService;
    }

    /**
     * Triggered when a TCP client signaling packet requests "Start Call"
     */
    public synchronized void onCallStart(String msisdn) {
   
        if (!balanceRepository.userExists(msisdn)) {
            System.err.println("System dropped call. MSISDN not found: " + msisdn);
            return;
        }

        BigDecimal currentBalance = balanceRepository.getBalance(msisdn);
        if (currentBalance.compareTo(new BigDecimal("1.00")) < 0) {
            System.err.println("Insufficient funds to initialize call for: " + msisdn);
            return;
        }

        if (activeSessions.containsKey(msisdn)) {
            System.err.println("Call already active for: " + msisdn);
            return;
        }

        CallSession session = new CallSession(msisdn);
        activeSessions.put(msisdn, session);
        System.out.println("Call established for " + msisdn + " at " + session.getStartTime());


        balanceRepository.deductBalance(msisdn, new BigDecimal("1.00"));

        // 4. Schedule Recurring 60-Second Charging Loop
        ScheduledFuture<?> billingTask = scheduler.scheduleAtFixedRate(() -> {
            handleMidCallBilling(msisdn);
        }, 60, 60, TimeUnit.SECONDS);

        activeTimers.put(msisdn, billingTask);
    }

    /**
     * Executed asynchronously every 60 seconds for active calls
     */
    private void handleMidCallBilling(String msisdn) {
        CallSession session = activeSessions.get(msisdn);
        if (session == null) {
            return;
        }

        BigDecimal balance = balanceRepository.getBalance(msisdn);
        BigDecimal nextMinuteCost = new BigDecimal("1.00");

        if (balance.compareTo(nextMinuteCost) >= 0) {
            balanceRepository.deductBalance(msisdn, nextMinuteCost);
            System.out.println("Deducted 1.00 L.E. from " + msisdn + ". Call continues.");
        } else {
            System.out.println("Credit exhausted for " + msisdn + "! Force terminating call.");
            onCallEnd(msisdn, CallResult.INSUFFICIENT_BALANCE);
        }
    }

    /**
     * Triggered when a user hangs up normally
     */
    public void onCallEnd(String msisdn) {
        onCallEnd(msisdn, CallResult.NORMAL_CALL_CLEARING);
    }

    /**
     * Handles final teardown, cleanup, and CDR logging
     */
    private synchronized void onCallEnd(String msisdn, CallResult result) {
        CallSession session = activeSessions.remove(msisdn);
        ScheduledFuture<?> billingTask = activeTimers.remove(msisdn);

        if (session == null) {
            System.err.println("Attempted to end non-existent call for: " + msisdn);
            return;
        }

        if (billingTask != null) {
            billingTask.cancel(false);
        }

        LocalDateTime endTime = LocalDateTime.now();
        long durationSeconds = java.time.Duration.between(session.getStartTime(), endTime).getSeconds();

        long chargedMinutes = (long) Math.ceil((double) durationSeconds / 60.0);
        if (chargedMinutes == 0) {
            chargedMinutes = 1;
        }
        BigDecimal totalCost = charger.calculateCost(msisdn, chargedMinutes);
        BigDecimal remainingBalance = balanceRepository.getBalance(msisdn);

        // 3. Compile Immutable CDR History Entity
        CDR cdr = new CDR(
                msisdn,
                session.getStartTime(),
                endTime,
                (durationSeconds / 60.0),
                result,
                totalCost,
                remainingBalance
        );

        // 4. Dispatch to Composite Reporting Pipeline (Prints to screen AND appends to file)
        reportingService.report(cdr);
    }
}
