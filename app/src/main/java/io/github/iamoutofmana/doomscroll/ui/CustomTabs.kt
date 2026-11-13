package io.github.iamoutofmana.doomscroll.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent

/** Opens articles in Custom Tabs; falls back to a plain VIEW intent if none is available. */
fun Context.openInCustomTab(url: String) {
    val uri = Uri.parse(url)
    try {
        CustomTabsIntent.Builder().build().launchUrl(this, uri)
    } catch (e: ActivityNotFoundException) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (e2: ActivityNotFoundException) {
            // No browser on the device — nothing sensible left to do.
        }
    }
}
