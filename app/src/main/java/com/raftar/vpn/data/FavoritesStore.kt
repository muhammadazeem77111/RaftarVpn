package com.raftar.vpn.data

import android.content.Context

/** Favorite countries (star wale) — sirf device par save hote hain. */
class FavoritesStore(context: Context) {
    private val prefs =
        context.getSharedPreferences("raftar_favorites", Context.MODE_PRIVATE)

    fun all(): Set<String> =
        prefs.getStringSet(KEY, emptySet())?.toSet() ?: emptySet()

    fun isFavorite(countryName: String): Boolean = all().contains(countryName)

    fun toggle(countryName: String): Set<String> {
        val next = all().toMutableSet()
        if (!next.add(countryName)) next.remove(countryName)
        prefs.edit().putStringSet(KEY, next).apply()
        return next
    }

    private companion object {
        const val KEY = "favorite_countries"
    }
}
