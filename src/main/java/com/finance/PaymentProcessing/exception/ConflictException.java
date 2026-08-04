package com.finance.PaymentProcessing.exception;
public class ConflictException extends RuntimeException {
    private final String errorCode;
    public ConflictException(String errorCode, String message) { super(message); this.errorCode = errorCode; }
    public String getErrorCode() { return errorCode; }
}
