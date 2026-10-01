package com.example.dns

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [DomainMatcher] — exact matching, subdomain matching,
 * case insensitivity, and false-positive resistance.
 */
class DomainMatcherTest {

    private lateinit var matcher: DomainMatcher

    @Before
    fun setUp() {
        matcher = DomainMatcher()
    }

    @Test
    fun `exact domain match`() {
        matcher.updateRules(setOf("tracker.example.com"))
        val result = matcher.match("tracker.example.com")
        assertTrue(result.blocked)
        assertEquals("tracker.example.com", result.matchedRule)
    }

    @Test
    fun `subdomain match`() {
        matcher.updateRules(setOf("example.com"))
        val result = matcher.match("sub.example.com")
        assertTrue(result.blocked)
        assertEquals("example.com", result.matchedRule)
    }

    @Test
    fun `deep subdomain match`() {
        matcher.updateRules(setOf("example.com"))
        val result = matcher.match("a.b.c.d.example.com")
        assertTrue(result.blocked)
        assertEquals("example.com", result.matchedRule)
    }

    @Test
    fun `no false positive on partial domain name`() {
        matcher.updateRules(setOf("example.com"))
        val result = matcher.match("notexample.com")
        assertFalse(result.blocked)
        assertNull(result.matchedRule)
    }

    @Test
    fun `no false positive on embedded domain`() {
        matcher.updateRules(setOf("oogle.com"))
        val result = matcher.match("google.com")
        assertFalse(result.blocked)
    }

    @Test
    fun `case insensitive matching`() {
        matcher.updateRules(setOf("Tracker.EXAMPLE.com"))
        val result = matcher.match("tracker.example.COM")
        assertTrue(result.blocked)
    }

    @Test
    fun `trailing dot normalization`() {
        matcher.updateRules(setOf("example.com."))
        val result = matcher.match("example.com")
        assertTrue(result.blocked)
    }

    @Test
    fun `query with trailing dot`() {
        matcher.updateRules(setOf("example.com"))
        val result = matcher.match("example.com.")
        assertTrue(result.blocked)
    }

    @Test
    fun `empty blocklist allows everything`() {
        matcher.updateRules(emptySet())
        val result = matcher.match("anything.example.com")
        assertFalse(result.blocked)
    }

    @Test
    fun `empty domain does not match`() {
        matcher.updateRules(setOf("example.com"))
        val result = matcher.match("")
        assertFalse(result.blocked)
    }

    @Test
    fun `whitespace-only domain does not match`() {
        matcher.updateRules(setOf("example.com"))
        val result = matcher.match("   ")
        assertFalse(result.blocked)
    }

    @Test
    fun `multiple rules match most specific`() {
        matcher.updateRules(setOf("example.com", "sub.example.com"))
        val result = matcher.match("sub.example.com")
        assertTrue(result.blocked)
        // Should match the more specific rule
        assertEquals("sub.example.com", result.matchedRule)
    }

    @Test
    fun `live rule update`() {
        matcher.updateRules(setOf("old-tracker.com"))
        assertTrue(matcher.match("old-tracker.com").blocked)
        assertFalse(matcher.match("new-tracker.com").blocked)

        // Update rules atomically
        matcher.updateRules(setOf("new-tracker.com"))
        assertFalse(matcher.match("old-tracker.com").blocked)
        assertTrue(matcher.match("new-tracker.com").blocked)
    }
}
