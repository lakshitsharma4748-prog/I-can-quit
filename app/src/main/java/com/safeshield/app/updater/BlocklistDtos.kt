package com.safeshield.app.updater

import kotlinx.serialization.Serializable

/** Mirrors the backend's `GET /api/blocklist` response shape — see backend/src/services/blocklist.service.ts. */
@Serializable
data class BlocklistResponse(
    val version: String,
    val updatedAt: String,
    val domains: List<BlocklistEntryDto>
)

@Serializable
data class BlocklistEntryDto(
    val domain: String,
    val category: String
)
