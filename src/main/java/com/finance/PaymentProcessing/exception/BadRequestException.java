package com.finance.PaymentProcessing.exception;
public class BadRequestException extends RuntimeException {
    private final String errorCode;
    public BadRequestException(String message) { this("VALIDATION_FAILED", message); }
    public BadRequestException(String errorCode, String message) { super(message); this.errorCode = errorCode; }
    public String getErrorCode() { return errorCode; }
}
