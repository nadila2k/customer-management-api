package com.nadila.customer_management_api.exception;

public class DuplicateFamilyMemberException extends RuntimeException {
    public DuplicateFamilyMemberException(String message) {
        super(message);
    }
}
