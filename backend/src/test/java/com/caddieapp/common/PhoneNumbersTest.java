package com.caddieapp.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PhoneNumbersTest {

    @Test
    void stripsSpacesDashesAndBrackets() {
        assertThat(PhoneNumbers.normalise(" 087 123-4567 ")).isEqualTo("0871234567");
        assertThat(PhoneNumbers.normalise("(087) 123.4567")).isEqualTo("0871234567");
    }

    @Test
    void keepsLeadingPlus() {
        assertThat(PhoneNumbers.normalise("+353 87 123 4567")).isEqualTo("+353871234567");
    }

    @Test
    void nullBecomesEmpty() {
        assertThat(PhoneNumbers.normalise(null)).isEmpty();
    }
}
