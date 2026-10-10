package com.raftar.vpn.data

import android.content.Context
import android.os.SystemClock
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.TimeUnit

/**
 * VPN Gate public API: https://www.vpngate.net/api/iphone/
 * Plain CSV (~1.3 MB), koi key/account nahi. Pehli line '*' se (skip), header '#' se (skip),
 * aakhri line '*' footer (skip). Message field me commas ho sakte hain -> split limit=15.
 * Config base64 column 14 (last) me hai.
 *
 * Fetch resilience: vpngate.net kuch networks par blocked hota hai. Verified baat ye hai
 * ke public "mirror" sites CSV cache nahi kartin — sab wapas isi endpoint par jati hain,
 * is liye koi working mirror URL nahi. Is liye: https primary -> http fallback ->
 * exponential backoff retry -> saaf error message.
 */
class VpnGateApi(private val context: Context) {

    companion object {
        private val ENDPOINTS = listOf(
            "https://www.vpngate.net/api/iphone/",
            "http://www.vpngate.net/api/iphone/"
        )

        /** Recommended section ke liye minimum uptime: 1 hour. */
        const val UPTIME_THRESHOLD_SEC = 3600L

        /** TCP reachability probe ka timeout. */
        const val REACHABILITY_TIMEOUT_MS = 3000
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build()

    private val cacheFile: File get() = File(context.filesDir, "vpngate_cache.csv")

    /** Aakhri kamyab fetch ka raw CSV — taake app khulne par list foran nazar aaye. */
    suspend fun loadCached(): List<VpnServer> = withContext(Dispatchers.IO) {
        val f = cacheFile
        if (!f.exists()) emptyList() else parseCsv(f.readText())
    }

    /** Network se taza list lao + cache update karo. Fail ho to wazeh error ke sath throw. */
    suspend fun fetchFresh(): List<VpnServer> = withContext(Dispatchers.IO) {
        var lastError: Exception? = null
        for (url in ENDPOINTS) {
            var backoffMs = 2000L
            repeat(3) { attempt ->
                try {
                    val request = Request.Builder()
                        .url(url)
                        .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) RaftarVpn/1.0")
                        .build()
                    client.newCall(request).execute().use { resp ->
                        if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}")
                        val body = resp.body?.string() ?: throw IOException("khaali jawab")
                        cacheFile.writeText(body)
                        return@withContext parseCsv(body)
                    }
                } catch (e: Exception) {
                    lastError = e
                    if (attempt < 2) delay(backoffMs)
                    backoffMs *= 2
                }
            }
        }
        throw IOException(
            "VPN Gate ki server list nahi mil saki — lagta hai vpngate.net is network par " +
                "blocked hai ya internet down hai. Connection check karo aur dobara Refresh dabao."
        )
    }

    private fun parseCsv(text: String): List<VpnServer> {
        val out = ArrayList<VpnServer>(512)
        for (rawLine in text.lineSequence()) {
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("*")) continue
            val p = line.split(",", limit = 15)
            if (p.size < 15) continue
            val score = p[2].toLongOrNull() ?: 0L
            val ping = p[3].toIntOrNull() ?: 0
            if (score <= 0 || ping <= 0) continue
            val b64 = p[14].trim()
            if (b64.isEmpty()) continue
            out.add(
                VpnServer(
                    hostName = p[0].trim(),
                    ip = p[1].trim(),
                    countryLong = p[5].trim(),
                    countryShort = p[6].trim(),
                    pingMs = ping,
                    score = score,
                    speedBps = p[4].toLongOrNull() ?: 0L,
                    sessions = p[7].toIntOrNull() ?: 0,
                    uptimeSec = p[8].toLongOrNull() ?: 0L,
                    configBase64 = b64
                )
            )
        }
        return rank(out)
    }

    /** Ranking: pehle score desc, phir ping asc. */
    fun rank(servers: List<VpnServer>): List<VpnServer> =
        servers.sortedWith(compareByDescending<VpnServer> { it.score }.thenBy { it.pingMs })

    /**
     * "Recommended" server: healthy + kam-az-kam 1 hour se online.
     * TCP reachability is se alag probe hoti hai (session cache).
     */
    fun isRecommended(server: VpnServer): Boolean =
        server.score > 0 && server.pingMs > 0 && server.uptimeSec >= UPTIME_THRESHOLD_SEC

    /** Base64 config -> .ovpn text. Null = corrupt row, skip karo. */
    fun decodeConfig(server: VpnServer): String? = try {
        val bytes = Base64.decode(server.configBase64, Base64.DEFAULT)
        val text = String(bytes, Charsets.UTF_8)
        if ("remote" in text) text else null
    } catch (_: Exception) {
        null
    }

    /**
     * Quick TCP reachability check: config se OpenVPN port nikalo, server IP par
     * 3s timeout ke sath socket kholo. Band/dead server foran bahar.
     */
    suspend fun checkTcpReachable(server: VpnServer): Boolean =
        measureTcp(server) != null

    /**
     * TCP connect timing (ms) — Raftar Race aur Gaming Mode ke liye.
     * Null = unreachable (yehi "blocked/dead" ka empirical filter hai).
     */
    suspend fun measureTcp(server: VpnServer): Long? = withContext(Dispatchers.IO) {
        try {
            val cfg = decodeConfig(server) ?: return@withContext null
            val ep = parseRemoteEndpoint(cfg)
            if (ep.host.isBlank()) return@withContext null
            val start = SystemClock.elapsedRealtime()
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ep.host, ep.port), REACHABILITY_TIMEOUT_MS)
            }
            SystemClock.elapsedRealtime() - start
        } catch (_: Exception) {
            null
        }
    }
}

/**
 * VPN Gate (SoftEther) configs ke liye sanitizer — OpenVPN3 core ke data-channel
 * cipher rules ke mutabiq:
 * - CBC force karne wali cipher/data-ciphers/keysize lines hatao
 *   ("bad cipher for data channel use" loop se bachne ke liye)
 * - connect-retry hatao (fail fast, taake app-level failover chal sake)
 * - Phir CHACHA20-POLY1305 (pehle) + AES-GCM fallback lagao — SoftEther servers
 *   par AES-GCM ka DECRYPT_ERROR bug hota hai, is liye ChaCha20 pehle.
 * - DNS inject karo (kuch servers DNS push nahi karte) aur IPv6 block karo
 *   (Android IPv6-first lookup par atak jata hai).
 */
fun sanitizeOvpnConfig(raw: String): String {
    val dropPrefixes = setOf("cipher", "data-ciphers", "data-ciphers-fallback", "keysize", "connect-retry")
    val kept = raw.lineSequence()
        .filter { line ->
            val t = line.trimStart()
            if (t.isEmpty() || t.startsWith("#") || t.startsWith(";")) return@filter true
            val head = t.substringBefore(' ').substringBefore('\t').lowercase()
            head !in dropPrefixes
        }
        .joinToString("\n")
    return buildString {
        append(kept.trimEnd())
        append("\ncipher CHACHA20-POLY1305")
        append("\ndata-ciphers CHACHA20-POLY1305:AES-256-GCM:AES-128-GCM")
        append("\ndhcp-option DNS 1.1.1.1")
        append("\ndhcp-option DNS 8.8.8.8")
        append("\nblock-ipv6\n")
    }
}

data class RemoteEndpoint(val host: String, val port: Int, val proto: String)

/** Config ki `remote` / `proto` lines se host, port aur proto nikalo (comments skip). */
fun parseRemoteEndpoint(config: String): RemoteEndpoint {
    var host = ""
    var port = 1194
    var proto = "tcp"
    for (raw in config.lineSequence()) {
        val line = raw.trim()
        if (line.isEmpty() || line.startsWith("#") || line.startsWith(";")) continue
        val parts = line.split(Regex("\\s+"))
        when (parts[0].lowercase()) {
            "remote" -> if (parts.size > 1 && host.isEmpty()) {
                host = parts[1]
                port = parts.getOrNull(2)?.toIntOrNull() ?: 1194
            }
            "proto" -> {
                val p = parts.getOrNull(1)?.lowercase().orEmpty()
                proto = if ("udp" in p) "udp" else "tcp"
            }
        }
    }
    return RemoteEndpoint(host, port, proto)
}
