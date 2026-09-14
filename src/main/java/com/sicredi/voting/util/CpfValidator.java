package com.sicredi.voting.util;

public final class CpfValidator {

    private CpfValidator() {
    }

    public static boolean isValid(String cpf) {
        if (cpf == null) {
            return false;
        }

        String digits = cpf.replaceAll("\\D", "");
        if (digits.length() != 11) {
            return false;
        }

        // Rejeita sequências repetidas (00000000000, 11111111111, ...)
        if (digits.chars().distinct().count() == 1) {
            return false;
        }

        try {
            int d1 = calculateDigit(digits, 9);
            int d2 = calculateDigit(digits, 10);
            return digits.charAt(9) == Character.forDigit(d1, 10)
                    && digits.charAt(10) == Character.forDigit(d2, 10);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // Cada dígito é multiplicado por um peso decrescente
    // (a partir de length+1) e o dígito verificador vem do resto da soma por 11.
    private static int calculateDigit(String digits, int length) {
        int sum = 0;
        for (int i = 0; i < length; i++) {
            sum += Character.getNumericValue(digits.charAt(i)) * (length + 1 - i);
        }
        int remainder = sum % 11;
        return (remainder < 2) ? 0 : 11 - remainder;
    }
}