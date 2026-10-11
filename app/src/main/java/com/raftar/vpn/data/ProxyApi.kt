package com.raftar.vpn.data

import android.os.SystemClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.TimeUnit

/**
 * Free SOCKS5 proxy list (no signup, no key).
 * Source: proxyscrape free tier. Quality mukhtalif hoti hai — is liye ping test
 * ke sath dikhate hain. WARNING: free proxy VPN jaisa encrypted/secure NAHI hota;
 * sirf unblocking ke liye, banking/passwords ke liye kabhi nahi.
 */
data class FreeProxy(
    val ip: String,
    val port: Int,
    val proto: String = "SOCKS5",
    val pingMs: Long? = null
)

class ProxyApi {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun fetchSocks5(): List<FreeProxy> = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url("https://api.proxyscrape.com/v2/?request=displayproxies&protocol=socks5&timeout=10000&country=all&ssl=all&anonymity=all")
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) RaftarVpn/1.0")
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}")
            val body = resp.body?.string().orEmpty()
            body.lineSequence()
                .map { it.trim() }
                .filter { it.contains(":") }
                .mapNotNull { line ->
                    val parts = line.split(":")
                    if (parts.size < 2) return@mapNotNull null
                    val port = parts[1].toIntOrNull() ?: return@mapNotNull null
                    FreeProxy(parts[0].trim(), port)
                }
                .distinctBy { "${it.ip}:${it.port}" }
                .take(100)
                .toList()
        }
    }

    /** TCP ping — kaam karne wale proxies pehle. */
    suspend fun ping(proxy: FreeProxy): Long? = withContext(Dispatchers.IO) {
        try {
            val start = SystemClock.elapsedRealtime()
            Socket().use { it.connect(InetSocketAddress(proxy.ip, proxy.port), 3000) }
            SystemClock.elapsedRealtime() - start
        } catch (_: Exception) {
            null
        }
    }
}
