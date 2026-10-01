package com.example.dns

import java.util.Locale

/**
 * Thread-safe domain matcher that checks domains against an in-memory blocklist.
 *
 * Supports exact domain matching and automatic subdomain matching:
 * - Rule "example.com" blocks "example.com" and "sub.example.com"
 * - Rule "example.com" does NOT block "notexample.com"
 *
 * The blocklist can be updated atomically at runtime without tunnel restarts.
 */
class DomainMatcher {

    @Volatile
    private var blockSet: Set<String> = emptySet()

    data class MatchResult(
        val blocked: Boolean,
        val matchedRule: String? = null,
        val category: String? = null
    )

    /**
     * Atomically replaces the blocklist. Thread-safe — readers see either
     * the old or the new set, never a partial update.
     */
    fun updateRules(domains: Set<String>) {
        blockSet = domains.map { normalize(it) }.toSet()
    }

    /**
     * Checks if [domain] matches any rule in the blocklist.
     *
     * Walks from the full domain up through parent domains:
     *   "sub.tracker.example.com" checks:
     *     sub.tracker.example.com → tracker.example.com → example.com → com
     */
    fun match(domain: String): MatchResult {
        val normalized = normalize(domain)
        if (normalized.isEmpty()) return MatchResult(blocked = false)

        val rules = blockSet // snapshot the volatile reference
        if (rules.isEmpty()) return MatchResult(blocked = false)

        // Walk up the domain hierarchy
        var check = normalized
        while (check.isNotEmpty()) {
            if (check in rules) {
                return MatchResult(blocked = true, matchedRule = check)
            }
            val dot = check.indexOf('.')
            check = if (dot >= 0) check.substring(dot + 1) else ""
        }
        return MatchResult(blocked = false)
    }

    companion object {
        /**
         * Normalizes a domain for matching:
         * - Lowercases
         * - Strips trailing dots
         * - Strips leading/trailing whitespace
         */
        fun normalize(domain: String): String {
            return domain
                .trim()
                .lowercase(Locale.ROOT)
                .trimEnd('.')
        }
    }
}
