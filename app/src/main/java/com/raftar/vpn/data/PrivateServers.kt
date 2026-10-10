package com.raftar.vpn.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** User ka apna (private) OpenVPN server — .ovpn config sirf device par encrypted rehti hai. */
data class PrivateServer(
    val id: String,
    val name: String,
    val config: String,
    val createdAt: Long
)

/** Minimal .ovpn validation: `remote` line honi chahiye. */
fun isValidOvpn(text: String): Boolean = "remote" in text.lowercase()

/**
 * Private servers EncryptedSharedPreferences me (AES256) — koi server, koi cloud nahi,
 * sab kuch sirf is phone par.
 */
class PrivateServerStore(context: Context) {

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "raftar_private_servers",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.ValueEncryptionScheme.AES256_GCM
    )

    fun list(): List<PrivateServer> {
        val arr = try {
            JSONArray(prefs.getString(KEY, "[]"))
        } catch (_: Exception) {
            return emptyList()
        }
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            PrivateServer(
                id = o.getString("id"),
                name = o.getString("name"),
                config = o.getString("config"),
                createdAt = o.getLong("createdAt")
            )
        }.sortedByDescending { it.createdAt }
    }

    fun add(name: String, config: String): PrivateServer {
        val ps = PrivateServer(UUID.randomUUID().toString(), name, config, System.currentTimeMillis())
        val arr = try {
            JSONArray(prefs.getString(KEY, "[]"))
        } catch (_: Exception) {
            JSONArray()
        }
        arr.put(
            JSONObject()
                .put("id", ps.id)
                .put("name", ps.name)
                .put("config", ps.config)
                .put("createdAt", ps.createdAt)
        )
        prefs.edit().putString(KEY, arr.toString()).apply()
        return ps
    }

    fun remove(id: String) {
        val arr = try {
            JSONArray(prefs.getString(KEY, "[]"))
        } catch (_: Exception) {
            return
        }
        val next = JSONArray()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            if (o.getString("id") != id) next.put(o)
        }
        prefs.edit().putString(KEY, next.toString()).apply()
    }

    private companion object {
        const val KEY = "servers"
    }
}
