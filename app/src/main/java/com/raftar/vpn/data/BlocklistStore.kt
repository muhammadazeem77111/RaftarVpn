package com.raftar.vpn.data

import android.content.Context

/**
 * User-reported bad servers ki local blocklist.
 * Koi global blocklist reliable nahi — is liye empirical tareeqa:
 * jo server reachability check fail kare woh auto-filter hota hai,
 * aur user long-press karke kisi bhi server ko report kar sakta hai;
 * reported server is user ke liye dobara kabhi list me nahi aayega.
 */
class BlocklistStore(context: Context) {

    private val prefs = context.getSharedPreferences("raftar_blocklist", Context.MODE_PRIVATE)

    fun isBlocked(serverId: String): Boolean = prefs.getBoolean(key(serverId), false)

    fun block(serverId: String) {
        prefs.edit().putBoolean(key(serverId), true).apply()
    }

    fun unblock(serverId: String) {
        prefs.edit().remove(key(serverId)).apply()
    }

    private fun key(serverId: String) = "b_${serverId.hashCode()}"
}
