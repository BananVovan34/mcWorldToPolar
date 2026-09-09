package com.bananvovan.mcworldtopolar

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class AppMetadataTest {

    @Test
    fun loadsBuildMetadataAndIcon() {
        assertEquals("MC World to Polar", AppMetadata.name)
        assertEquals("BananVovan34", AppMetadata.vendor)
        assertFalse(AppMetadata.description.isBlank())
        assertFalse(AppMetadata.version.isBlank())
        assertFalse(AppMetadata.version.contains('$'))
        assertNotNull(AppMetadata.iconUrl)
    }
}
