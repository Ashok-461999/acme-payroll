package com.acme.payroll.common;

import com.acme.payroll.common.exception.BusinessValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IsoReferenceDataTest {

    @Test
    void normalisesValidCodes() {
        assertThat(IsoReferenceData.requireCountry(" in ")).isEqualTo("IN");
        assertThat(IsoReferenceData.requireCurrency(" usd ")).isEqualTo("USD");
    }

    @Test
    void rejectsInvalidCodes() {
        assertThatThrownBy(() -> IsoReferenceData.requireCountry("ZZ"))
                .isInstanceOf(BusinessValidationException.class);
        assertThatThrownBy(() -> IsoReferenceData.requireCurrency("ZZZ"))
                .isInstanceOf(BusinessValidationException.class);
        assertThatThrownBy(() -> IsoReferenceData.requireCurrency("XXX"))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("XXX");
    }
}
