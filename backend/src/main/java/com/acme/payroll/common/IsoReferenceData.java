package com.acme.payroll.common;

import com.acme.payroll.common.exception.BusinessValidationException;

import java.util.Currency;
import java.util.Locale;
import java.util.Set;

/**
 * Validates and normalises ISO country and currency codes. The UI offers a curated
 * list, but the API accepts any real ISO code and rejects anything else.
 */
public final class IsoReferenceData {

    private static final Set<String> ISO_COUNTRIES = Set.of(Locale.getISOCountries());

    private IsoReferenceData() {
    }

    public static String requireCountry(String countryCode) {
        String normalised = countryCode == null ? "" : countryCode.trim().toUpperCase(Locale.ROOT);
        if (!ISO_COUNTRIES.contains(normalised)) {
            throw new BusinessValidationException("country", "must be a valid ISO 3166-1 alpha-2 country code");
        }
        return normalised;
    }

    public static String requireCurrency(String currencyCode) {
        String normalised = currencyCode == null ? "" : currencyCode.trim().toUpperCase(Locale.ROOT);
        try {
            Currency.getInstance(normalised);
        } catch (IllegalArgumentException exception) {
            throw new BusinessValidationException("currency", "must be a valid ISO 4217 currency code");
        }
        if ("XXX".equals(normalised)) {
            throw new BusinessValidationException("currency", "must be a real currency, not XXX");
        }
        return normalised;
    }
}
