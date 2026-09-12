package com.tango.recall.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri

/** The first http(s) address in a piece of text, if there is one. */
internal fun firstUrl(text: String): String? =
    Regex("""https?://[^\s、。）)\]】"']+""").find(text)?.value

/** Open a link in the browser. Nothing happens if the device has none. */
internal fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, url.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (_: ActivityNotFoundException) {
        // No browser installed; there is nothing useful to fall back to.
    }
}
