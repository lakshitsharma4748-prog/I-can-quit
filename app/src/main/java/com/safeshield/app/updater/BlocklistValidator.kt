package com.safeshield.app.updater

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Parses and validates a raw blocklist JSON response before anything from
 * it is allowed near Room (PRD Phase 12: "Validate downloaded data. Reject
 * malformed data."). Pure Kotlin — kotlinx.serialization's `Json` runs on
 * plain JVM, unlike `android.util.JsonReader`/`org.json`, which are Android
 * SDK stubs in local unit tests — so this is fully unit-testable without
 * Robolectric or a device.
 *
 * [BlocklistUpdater] is the only caller; it must never apply anything this
 * class doesn't return as [ParseResult.Valid].
 */
object BlocklistValidator {

    // A conservative hostname check: labels of letters/digits/hyphens (no
    // leading/trailing hyphen), at least two labels, each ≤63 chars, total
    // ≤253 — enough to catch obviously malformed entries, not a full RFC
    // 1035 implementation.
    private val DOMAIN_REGEX = Regex(
        "^[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?(\\.[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?)+$"
    )
    private const val MAX_DOMAIN_LENGTH = 253

    sealed class ParseResult {
        data class Valid(val response: BlocklistResponse) : ParseResult()
        data class Invalid(val reason: String) : ParseResult()
    }

    fun parseAndValidate(json: String): ParseResult {
        val response = try {
            Json { ignoreUnknownKeys = true }.decodeFromString<BlocklistResponse>(json)
        } catch (e: SerializationException) {
            return ParseResult.Invalid("Malformed JSON: ${e.message}")
        } catch (e: IllegalArgumentException) {
            return ParseResult.Invalid("Malformed JSON: ${e.message}")
        }

        if (response.version.isBlank()) return ParseResult.Invalid("Missing version")
        if (response.updatedAt.isBlank()) return ParseResult.Invalid("Missing updatedAt")
        if (response.domains.isEmpty()) return ParseResult.Invalid("Empty domain list")

        val invalidEntry = response.domains.firstOrNull { !isValidEntry(it) }
        if (invalidEntry != null) {
            return ParseResult.Invalid("Invalid entry: domain='${invalidEntry.domain}' category='${invalidEntry.category}'")
        }

        return ParseResult.Valid(response)
    }

    private fun isValidEntry(entry: BlocklistEntryDto): Boolean =
        entry.category.isNotBlank() && isValidDomain(entry.domain)

    private fun isValidDomain(domain: String): Boolean {
        val normalized = domain.trim().lowercase()
        return normalized.isNotEmpty() &&
            normalized.length <= MAX_DOMAIN_LENGTH &&
            DOMAIN_REGEX.matches(normalized)
    }
}
