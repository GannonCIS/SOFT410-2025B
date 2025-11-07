package org.example;

import java.util.List;

public interface AccountRepository {
    List<Account> findAllByCustomer(int customerNumber);

    /** Only returns the account if it belongs to the given customer. */
    Account findOneForCustomer(int customerNumber, int accountNumber);

    /** Persist balance (and any other mutable fields) for this account. */
    void save(Account account);

    /**
     * Create a new account for a customer.
     * @param initialCents initial balance in cents (0 is fine)
     * @return the newly allocated account number
     */
    int create(int customerNumber, AccountType type, long initialCents);

    // OPTIONAL (for cross-customer transfers later):
    // Account findByAccountNumber(int accountNumber);
}
