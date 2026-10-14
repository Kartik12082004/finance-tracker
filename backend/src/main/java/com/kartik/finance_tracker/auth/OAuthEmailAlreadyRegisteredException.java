package com.kartik.finance_tracker.auth;

public class OAuthEmailAlreadyRegisteredException
        extends IllegalArgumentException {

    private static final long serialVersionUID = 1L;

    public OAuthEmailAlreadyRegisteredException(String message) {
        super(message);
    }
}
