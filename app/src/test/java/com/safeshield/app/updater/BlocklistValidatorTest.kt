package com.safeshield.app.updater

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BlocklistValidatorTest {

    private fun validJson(domains: String = """{"domain":"blocked-example.test","category":"test"}""") =
        """{"version":"1","updatedAt":"2026-01-01T00:00:00Z","domains":[$domains]}"""

    @Test
    fun `well-formed response is accepted`() {
        val result = BlocklistValidator.parseAndValidate(validJson())
        assertTrue(result is BlocklistValidator.ParseResult.Valid)
        val response = (result as BlocklistValidator.ParseResult.Valid).response
        assertEquals("1", response.version)
        assertEquals(1, response.domains.size)
    }

    @Test
    fun `multiple domains and categories are accepted`() {
        val json = validJson(
            """{"domain":"a.example","category":"adult"},{"domain":"b.example","category":"adult"}"""
        )
        val result = BlocklistValidator.parseAndValidate(json)
        assertTrue(result is BlocklistValidator.ParseResult.Valid)
        assertEquals(2, (result as BlocklistValidator.ParseResult.Valid).response.domains.size)
    }

    @Test
    fun `not JSON at all is rejected`() {
        val result = BlocklistValidator.parseAndValidate("this is not json")
        assertTrue(result is BlocklistValidator.ParseResult.Invalid)
    }

    @Test
    fun `truncated JSON is rejected`() {
        val result = BlocklistValidator.parseAndValidate("""{"version":"1","domains":[""")
        assertTrue(result is BlocklistValidator.ParseResult.Invalid)
    }

    @Test
    fun `missing required field is rejected`() {
        val result = BlocklistValidator.parseAndValidate("""{"updatedAt":"2026-01-01T00:00:00Z","domains":[]}""")
        assertTrue(result is BlocklistValidator.ParseResult.Invalid)
    }

    @Test
    fun `empty domain list is rejected`() {
        val json = """{"version":"1","updatedAt":"2026-01-01T00:00:00Z","domains":[]}"""
        val result = BlocklistValidator.parseAndValidate(json)
        assertTrue(result is BlocklistValidator.ParseResult.Invalid)
    }

    @Test
    fun `blank version is rejected`() {
        val json = """{"version":"","updatedAt":"2026-01-01T00:00:00Z","domains":[{"domain":"a.example","category":"test"}]}"""
        assertTrue(BlocklistValidator.parseAndValidate(json) is BlocklistValidator.ParseResult.Invalid)
    }

    @Test
    fun `entry with a blank category is rejected`() {
        val json = validJson("""{"domain":"a.example","category":""}""")
        assertTrue(BlocklistValidator.parseAndValidate(json) is BlocklistValidator.ParseResult.Invalid)
    }

    @Test
    fun `entry with an invalid domain is rejected`() {
        val cases = listOf(
            """{"domain":"","category":"test"}""",
            """{"domain":"not a domain","category":"test"}""",
            """{"domain":"-leadinghyphen.example","category":"test"}""",
            """{"domain":"nodot","category":"test"}""",
            """{"domain":"trailing-dot.example.","category":"test"}"""
        )
        for (case in cases) {
            val result = BlocklistValidator.parseAndValidate(validJson(case))
            assertTrue("expected '$case' to be rejected", result is BlocklistValidator.ParseResult.Invalid)
        }
    }

    @Test
    fun `one bad entry invalidates the whole response`() {
        // A partially-bad payload must not be partially applied — the
        // caller either gets a fully valid response or none at all.
        val json = validJson(
            """{"domain":"good.example","category":"test"},{"domain":"bad domain","category":"test"}"""
        )
        assertTrue(BlocklistValidator.parseAndValidate(json) is BlocklistValidator.ParseResult.Invalid)
    }

    @Test
    fun `unknown extra fields in the response are ignored, not rejected`() {
        val json = """{"version":"1","updatedAt":"2026-01-01T00:00:00Z","domains":[{"domain":"a.example","category":"test"}],"extra":"field"}"""
        assertTrue(BlocklistValidator.parseAndValidate(json) is BlocklistValidator.ParseResult.Valid)
    }
}
