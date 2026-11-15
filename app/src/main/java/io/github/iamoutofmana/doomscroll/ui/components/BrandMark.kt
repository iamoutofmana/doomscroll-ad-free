package io.github.iamoutofmana.doomscroll.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.iamoutofmana.doomscroll.R

/**
 * The app's brand mark: the "Friendly Doom" cloud on a Midnight Plum tile.
 * Rendered from the same vector used for the adaptive launcher icon, so the
 * in-app logo and the launcher icon never drift apart.
 */
@Composable
fun BrandMark(
    size: Dp = 26.dp,
    cornerPercent: Int = 24,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size * cornerPercent / 100))
            .background(BRAND_INK),
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

private val BRAND_INK = Color(0xFF1C1226)
