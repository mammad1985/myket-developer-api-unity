package ir.myket.developerapi.util

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Parcelable

fun <T : Parcelable?> Bundle.getParcelableFromBundle(key: String, clazz: Class<T>): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        this.getParcelable(key, clazz)
    else
        @Suppress("DEPRECATION") this.getParcelable(key)
}

fun <T : Parcelable?> Intent.getParcelableFromIntent(key: String, clazz: Class<T>): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        this.getParcelableExtra(key, clazz)
    else
        @Suppress("DEPRECATION") this.getParcelableExtra(key)
}

fun String.decodeHex(): String {
    require(length % 2 == 0) { "Must have an even length" }
    return chunked(2)
        .map { it.toInt(16).toByte() }
        .toByteArray()
        .toString(Charsets.UTF_8)
}