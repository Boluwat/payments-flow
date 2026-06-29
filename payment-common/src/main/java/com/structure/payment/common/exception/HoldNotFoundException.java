package com.structure.payment.common.exception;

public class HoldNotFoundException extends RuntimeException {
    public HoldNotFoundException(String message) {
        super(message);
    }
}
