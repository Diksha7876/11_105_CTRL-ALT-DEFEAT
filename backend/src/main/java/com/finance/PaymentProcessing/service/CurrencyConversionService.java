package com.finance.PaymentProcessing.service;

import com.finance.PaymentProcessing.exception.BadRequestException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class CurrencyConversionService {

    // INR value for one unit of target currency.
    private static final Map<String, BigDecimal> INR_PER_CURRENCY = Map.of(
            "INR", BigDecimal.ONE,
            "USD", new BigDecimal("83.10"),
            "EUR", new BigDecimal("90.25"),
            "GBP", new BigDecimal("105.40"));

    public BigDecimal convertToInr(BigDecimal amount, String fromCurrency) {
        if (amount == null) {
            throw new BadRequestException("INVALID_AMOUNT", "Amount is required");
        }
        String normalizedCurrency = normalizeCurrency(fromCurrency);
        BigDecimal rate = INR_PER_CURRENCY.get(normalizedCurrency);
        if (rate == null) {
            throw new BadRequestException("UNSUPPORTED_CURRENCY", "Unsupported currency: " + fromCurrency);
        }
        return amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getInrRate(String fromCurrency) {
        String normalizedCurrency = normalizeCurrency(fromCurrency);
        BigDecimal rate = INR_PER_CURRENCY.get(normalizedCurrency);
        if (rate == null) {
            throw new BadRequestException("UNSUPPORTED_CURRENCY", "Unsupported currency: " + fromCurrency);
        }
        return rate;
    }

    private String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return "INR";
        }
        return currency.trim().toUpperCase();
    }
}
