package com.br.betterreads.model;

/**
 * Helper class for validation results at signup / login
 */
public record ValidationResult(boolean valid, String errorMessage) {

    /**
     * @return valid ValidationResult
     */
    public static ValidationResult success() {
        return new ValidationResult(true, null);
    }

    /**
     * @param errorMessage the error message to return
     * @return invalid ValidationResult with error messade
     */
    public static ValidationResult error(String errorMessage) {
        return new ValidationResult(false, errorMessage);
    }
}
