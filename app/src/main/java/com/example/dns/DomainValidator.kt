package com.example.dns

import java.util.Locale

/**
 * Validates and normalizes domain input from the user before it enters the blocklist.
 *
 * Handles common user mistakes:
 * - Stripping "http://", "https://" prefixes
 * - Removing paths and ports
 * - Removing leading wildcard "*." (we match subdomains automatically)
 * - Rejecting IP addresses, empty input, overly long domains
 * - Validating label structure per RFC 1035
 */
object DomainValidator {

    private val VALID_LABEL = Regex("^[a-zA-Z0-9]([a-zA-Z0-9-]*[a-zA-Z0-9])?\$")
    private val IPV4_PATTERN = Regex("^\\d{1,3}(\\.\\d{1,3}){3}\$")

    sealed class ValidationResult {
        data class Valid(val normalizedDomain: String) : ValidationResult()
        data class Invalid(val errorMessage: String) : ValidationResult()
    }

    /**
     * Normalizes and validates user-provided domain input.
     *
     * Returns [ValidationResult.Valid] with the cleaned domain, or
     * [ValidationResult.Invalid] with a user-facing error message.
     */
    fun validate(input: String): ValidationResult {
        var domain = input.trim()

        if (domain.isBlank()) {
            return ValidationResult.Invalid("Domain cannot be empty")
        }

        // Strip protocol prefixes
        domain = domain.removePrefix("http://").removePrefix("https://")

        // Strip paths (everything after the first /)
        domain = domain.substringBefore('/')

        // Strip port (everything after : in host)
        domain = domain.substringBefore(':')

        // Lowercase for consistent matching
        domain = domain.lowercase(Locale.ROOT)

        // Remove trailing dot
        domain = domain.trimEnd('.')

        // Remove leading wildcard — we match subdomains automatically
        domain = domain.removePrefix("*.")

        // Post-cleanup checks
        if (domain.isBlank()) {
            return ValidationResult.Invalid("Domain cannot be empty after cleanup")
        }

        // Reject bare IP addresses
        if (IPV4_PATTERN.matches(domain)) {
            return ValidationResult.Invalid("Enter a domain name, not an IP address")
        }

        // RFC length limits
        if (domain.length > 253) {
            return ValidationResult.Invalid("Domain exceeds maximum length (253 characters)")
        }

        val labels = domain.split('.')
        if (labels.size < 2) {
            return ValidationResult.Invalid("Enter a full domain (e.g. example.com)")
        }

        for (label in labels) {
            if (label.isEmpty()) {
                return ValidationResult.Invalid("Domain contains empty labels (consecutive dots)")
            }
            if (label.length > 63) {
                return ValidationResult.Invalid("Label \"$label\" exceeds maximum length (63 characters)")
            }
            if (!VALID_LABEL.matches(label)) {
                return ValidationResult.Invalid("Invalid characters in \"$label\" — use letters, numbers, and hyphens only")
            }
        }

        return ValidationResult.Valid(domain)
    }
}
