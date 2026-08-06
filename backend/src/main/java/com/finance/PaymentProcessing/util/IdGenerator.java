package com.finance.PaymentProcessing.util;

import java.security.SecureRandom;

public final class IdGenerator {
    private static final SecureRandom RANDOM = new SecureRandom();

    private IdGenerator() {
    }

    public static String generate9DigitId() {
        int value = 100_000_000 + RANDOM.nextInt(900_000_000);
        return String.valueOf(value);
    }
}
