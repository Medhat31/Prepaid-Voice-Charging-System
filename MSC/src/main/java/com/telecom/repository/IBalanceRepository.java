package com.telecom.repository;

import java.math.BigDecimal;

public interface IBalanceRepository {
    /**
     * Checks the database to verify if a given MSISDN exists.
     * @param msisdn The phone number to look up.
     * @return true if the user exists, false otherwise.
     */
    boolean userExists(String msisdn);

    /**
     * Retrieves the current prepaid balance for a given subscriber.
     * @param msisdn The phone number to look up.
     * @return The account balance as a BigDecimal.
     */
    BigDecimal getBalance(String msisdn);

    /**
     * Atomically deducts a specific monetary amount from a user's balance.
     * @param msisdn The phone number of the caller.
     * @param amount The amount in L.E. to subtract.
     */
    void deductBalance(String msisdn, BigDecimal amount);
}