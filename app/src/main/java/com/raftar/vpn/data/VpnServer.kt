package com.raftar.vpn.data

/**
 * VPN Gate API se milne wala aik free server.
 * CSV columns: HostName, IP, Score, Ping, Speed, CountryLong, CountryShort,
 * NumVpnSessions, Uptime, TotalUsers, TotalTraffic, LogType, Operator, Message,
 * OpenVPN_ConfigData_Base64
 */
data class VpnServer(
    val hostName: String,
    val ip: String,
    val countryLong: String,
    val countryShort: String,
    val pingMs: Int,
    val score: Long,
    val speedBps: Long,
    val sessions: Int,
    val uptimeSec: Long,
    val configBase64: String
) {
    /** Session-scoped dead-server skip list ke liye stable id. */
    val id: String get() = "$hostName|$ip"
}
