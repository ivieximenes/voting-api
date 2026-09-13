package com.sicredi.voting.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CpfValidatorTest {

    @Test
    void shouldAcceptValidCpf() {
        assertThat(CpfValidator.isValid("11144477735")).isTrue();
        assertThat(CpfValidator.isValid("52998224725")).isTrue();
    }

    @Test
    void shouldAcceptCpfWithMask() {
        assertThat(CpfValidator.isValid("111.444.777-35")).isTrue();
    }

    @Test
    void shouldRejectNull() {
        assertThat(CpfValidator.isValid(null)).isFalse();
    }

    @Test
    void shouldRejectBlank() {
        assertThat(CpfValidator.isValid("")).isFalse();
        assertThat(CpfValidator.isValid("   ")).isFalse();
    }

    @Test
    void shouldRejectShortCpf() {
        assertThat(CpfValidator.isValid("123")).isFalse();
        assertThat(CpfValidator.isValid("1234567890")).isFalse();
    }

    @Test
    void shouldRejectRepeatedDigits() {
        assertThat(CpfValidator.isValid("00000000000")).isFalse();
        assertThat(CpfValidator.isValid("11111111111")).isFalse();
        assertThat(CpfValidator.isValid("99999999999")).isFalse();
    }

    @Test
    void shouldRejectInvalidCheckDigits() {
        assertThat(CpfValidator.isValid("11144477736")).isFalse();
        assertThat(CpfValidator.isValid("52998224726")).isFalse();
    }

    @Test
    void shouldRejectLetters() {
        assertThat(CpfValidator.isValid("abcdefghijk")).isFalse();
        assertThat(CpfValidator.isValid("1114447773a")).isFalse();
    }
}