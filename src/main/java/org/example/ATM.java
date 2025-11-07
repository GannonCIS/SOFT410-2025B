package org.example;

public class ATM extends OptionMenu {
    public ATM(AuthenticationRepository auth,
               AccountRepository accounts,
               AccountService service) {
        super(auth, accounts, service);
    }
}
