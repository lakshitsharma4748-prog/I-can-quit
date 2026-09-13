package com.safeshield.app.device

import org.junit.Assert.assertEquals
import org.junit.Test

class ManagementStateTest {

    @Test
    fun `display labels match the PRD's exact wording`() {
        assertEquals("NOT CONFIGURED", ManagementState.NOT_MANAGED.displayLabel())
        assertEquals("DEVICE ADMIN", ManagementState.DEVICE_ADMIN.displayLabel())
        assertEquals("DEVICE OWNER", ManagementState.DEVICE_OWNER.displayLabel())
        assertEquals("MANAGED PROFILE", ManagementState.MANAGED_PROFILE.displayLabel())
    }
}
