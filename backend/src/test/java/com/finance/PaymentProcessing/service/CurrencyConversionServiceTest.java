package com.finance.PaymentProcessing.service;

import com.finance.PaymentProcessing.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

class CurrencyConversionServiceTest {

    private CurrencyConversionService service;

    @BeforeEach
    void setUp() {
        service = new CurrencyConversionService();
    }

    // -------------------------------------------------------------------------
    // convertToInr – happy path for each supported currency
    // -------------------------------------------------------------------------

    @Test
    void convertToInr_inr_returnsSameAmount() {
        BigDecimal result = service.convertToInr(new BigDecimal("500.00"), "INR");
        assertThat(result).isEqualByComparingTo("500.00");
    }

    @Test
    void convertToInr_usd_appliesRate() {
        // 2 USD × 83.10 = 166.20
        BigDecimal result = service.convertToInr(new BigDecimal("2"), "USD");
        assertThat(result).isEqualByComparingTo("166.20");
    }

    @Test
    void convertToInr_eur_appliesRate() {
        // 3 EUR × 90.25 = 270.75
        BigDecimal result = service.convertToInr(new BigDecimal("3"), "EUR");
        assertThat(result).isEqualByComparingTo("270.75");
    }

    @Test
    void convertToInr_gbp_appliesRate() {
        // 1 GBP × 105.40 = 105.40
        BigDecimal result = service.convertToInr(new BigDecimal("1"), "GBP");
        assertThat(result).isEqualByComparingTo("105.40");
    }

    // -------------------------------------------------------------------------
    // convertToInr – rounding (HALF_UP, scale 2)
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "{0} USD → {1} INR")
    @CsvSource({
        "1.005, 83.52",   // 1.005 × 83.10 = 83.5155 → rounds to 83.52
        "0.001, 0.08",    // 0.001 × 83.10 = 0.0831  → rounds to 0.08
        "100,   8310.00"  // exact
    })
    void convertToInr_usd_roundsHalfUp(String input, String expected) {
        BigDecimal result = service.convertToInr(new BigDecimal(input), "USD");
        assertThat(result).isEqualByComparingTo(expected);
    }

    @Test
    void convertToInr_resultAlwaysHasScale2() {
        BigDecimal result = service.convertToInr(new BigDecimal("10"), "INR");
        assertThat(result.scale()).isEqualTo(2);
    }

    // -------------------------------------------------------------------------
    // convertToInr – currency normalisation
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "currency=''{0}'' treated as USD")
    @ValueSource(strings = {"usd", "Usd", "USD", " USD ", " usd "})
    void convertToInr_currencyIsCaseAndWhitespaceInsensitive(String currency) {
        BigDecimal result = service.convertToInr(new BigDecimal("1"), currency);
        assertThat(result).isEqualByComparingTo("83.10");
    }

    @ParameterizedTest(name = "null-or-blank currency defaults to INR")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void convertToInr_nullOrBlankCurrencyDefaultsToInr(String currency) {
        BigDecimal result = service.convertToInr(new BigDecimal("200"), currency);
        assertThat(result).isEqualByComparingTo("200.00");
    }

    // -------------------------------------------------------------------------
    // convertToInr – error cases
    // -------------------------------------------------------------------------

    @Test
    void convertToInr_nullAmount_throwsBadRequestException() {
        assertThatThrownBy(() -> service.convertToInr(null, "USD"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Amount is required");

        assertThatThrownBy(() -> service.convertToInr(null, "USD"))
                .isInstanceOf(BadRequestException.class)
                .satisfies(ex -> assertThat(((BadRequestException) ex).getErrorCode())
                        .isEqualTo("INVALID_AMOUNT"));
    }

    @Test
    void convertToInr_unsupportedCurrency_throwsBadRequestException() {
        assertThatThrownBy(() -> service.convertToInr(new BigDecimal("100"), "JPY"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("JPY")
                .satisfies(ex -> assertThat(((BadRequestException) ex).getErrorCode())
                        .isEqualTo("UNSUPPORTED_CURRENCY"));
    }

    // -------------------------------------------------------------------------
    // getInrRate – happy path
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "rate for {0} = {1}")
    @CsvSource({
        "INR, 1",
        "USD, 83.10",
        "EUR, 90.25",
        "GBP, 105.40"
    })
    void getInrRate_supportedCurrencies_returnsCorrectRate(String currency, String expectedRate) {
        BigDecimal rate = service.getInrRate(currency);
        assertThat(rate).isEqualByComparingTo(expectedRate);
    }

    @ParameterizedTest(name = "currency=''{0}'' normalised for rate lookup")
    @ValueSource(strings = {"gbp", "Gbp", " GBP ", " gbp"})
    void getInrRate_normalisesBeforeLookup(String currency) {
        BigDecimal rate = service.getInrRate(currency);
        assertThat(rate).isEqualByComparingTo("105.40");
    }

    // -------------------------------------------------------------------------
    // getInrRate – error cases
    // -------------------------------------------------------------------------

    @Test
    void getInrRate_unsupportedCurrency_throwsBadRequestException() {
        assertThatThrownBy(() -> service.getInrRate("AUD"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("AUD")
                .satisfies(ex -> assertThat(((BadRequestException) ex).getErrorCode())
                        .isEqualTo("UNSUPPORTED_CURRENCY"));
    }

    @ParameterizedTest(name = "null-or-blank currency defaults to INR rate")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void getInrRate_nullOrBlankCurrencyDefaultsToInr(String currency) {
        BigDecimal rate = service.getInrRate(currency);
        assertThat(rate).isEqualByComparingTo(BigDecimal.ONE);
    }
}
