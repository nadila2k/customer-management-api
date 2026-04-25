package com.nadila.customer_management_api.exception;

public class SelfFamilyReferenceException extends RuntimeException {
    public SelfFamilyReferenceException(String message) {
        super(message);
    }
}
