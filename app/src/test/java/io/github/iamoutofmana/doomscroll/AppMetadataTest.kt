package io.github.iamoutofmana.doomscroll

import io.github.iamoutofmana.doomscroll.domain.AppMetadata
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The applicationId is permanent once the app is published (stores + F-Droid).
 * This guard fails if one side changes without the other.
 */
class AppMetadataTest {
    @Test
    fun packageMetadataMatchesApplicationId() {
        assertEquals(AppMetadata.PACKAGE, BuildConfig.APPLICATION_ID)
    }
}
