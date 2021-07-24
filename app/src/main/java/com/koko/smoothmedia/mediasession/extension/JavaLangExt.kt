package com.koko.smoothmedia.mediasession.extension

import android.net.Uri
import java.util.*

/**
 * This file contains extension methods for the java.lang package.
 */

/**
 * Helper method to check if a [String] contains another in a case insensitive way.
 */

fun String?.containsCaseInsensitive(other: String?) =
    if (this != null && other != null) {
        lowercase(Locale.getDefault()).contains(other.lowercase(Locale.getDefault()))
    } else {
        this == other
    }
/**
 * Helper extension to convert a potentially null [String] to a [Uri] falling back to [Uri.EMPTY]
 */
fun String?.toUri(): Uri = this?.let { Uri.parse(it) } ?: Uri.EMPTY


