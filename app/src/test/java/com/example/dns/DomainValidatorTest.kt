package com.example.dns

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [DomainValidator] — normalization, rejection of invalid input,
 * and handling of common user mistakes (URLs, wildcards, IPs, etc).
 */
class DomainValidatorTest {

    private fun assertValid(input: String, expectedNormalized: String) {
        val result = DomainValidator.validate(input)
        assertTrue("Expected valid for '$input' but got: $result", result is DomainValidator.ValidationResult.Valid)
        assertEquals(expectedNormalized, (result as DomainValidator.ValidationResult.Valid).normalizedDomain)
    }

    private fun assertInvalid(input: String) {
        val result = DomainValidator.validate(input)
        assertTrue("Expected invalid for '$input' but got: $result", result is DomainValidator.ValidationResult.Invalid)
    }

    // --- Valid inputs that should normalize correctly ---

    @Test
    fun `simple domain passes`() {
        assertValid("example.com", "example.com")
    }

    @Test
    fun `subdomain passes`() {
        assertValid("tracker.example.com", "tracker.example.com")
    }

    @Test
    fun `strips http prefix`() {
        assertValid("http://tracker.example.com", "tracker.example.com")
    }

    @Test
    fun `strips https prefix`() {
        assertValid("https://tracker.example.com", "tracker.example.com")
    }

    @Test
    fun `strips path`() {
        assertValid("tracker.example.com/some/path", "tracker.example.com")
    }

    @Test
    fun `strips port`() {
        assertValid("tracker.example.com:8080", "tracker.example.com")
    }

    @Test
    fun `strips URL with path and port`() {
        assertValid("https://tracker.example.com:443/api/v1", "tracker.example.com")
    }

    @Test
    fun `lowercases domain`() {
        assertValid("TRACKER.Example.COM", "tracker.example.com")
    }

    @Test
    fun `strips trailing dot`() {
        assertValid("example.com.", "example.com")
    }

    @Test
    fun `strips leading wildcard`() {
        assertValid("*.example.com", "example.com")
    }

    @Test
    fun `strips whitespace`() {
        assertValid("  example.com  ", "example.com")
    }

    @Test
    fun `hyphenated labels pass`() {
        assertValid("my-tracker.example.com", "my-tracker.example.com")
    }

    @Test
    fun `numeric labels pass`() {
        assertValid("123.example.com", "123.example.com")
    }

    // --- Invalid inputs that should be rejected ---

    @Test
    fun `empty string rejected`() {
        assertInvalid("")
    }

    @Test
    fun `whitespace only rejected`() {
        assertInvalid("   ")
    }

    @Test
    fun `single label rejected`() {
        assertInvalid("localhost")
    }

    @Test
    fun `IP address rejected`() {
        assertInvalid("192.168.1.1")
    }

    @Test
    fun `label starting with hyphen rejected`() {
        assertInvalid("-invalid.example.com")
    }

    @Test
    fun `label ending with hyphen rejected`() {
        assertInvalid("invalid-.example.com")
    }

    @Test
    fun `label with special characters rejected`() {
        assertInvalid("tra_cker.example.com")
    }

    @Test
    fun `consecutive dots rejected`() {
        assertInvalid("example..com")
    }

    @Test
    fun `domain exceeding 253 chars rejected`() {
        val longDomain = "a".repeat(64) + "." + "b".repeat(64) + "." + "c".repeat(64) + "." + "d".repeat(63)
        assertInvalid(longDomain)
    }

    @Test
    fun `label exceeding 63 chars rejected`() {
        val longLabel = "a".repeat(64) + ".com"
        assertInvalid(longLabel)
    }

    // --- Edge cases ---

    @Test
    fun `protocol-only rejected`() {
        assertInvalid("https://")
    }

    @Test
    fun `just a dot rejected`() {
        assertInvalid(".")
    }
}
