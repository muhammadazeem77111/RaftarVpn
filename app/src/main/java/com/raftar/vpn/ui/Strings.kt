package com.raftar.vpn.ui

import androidx.compose.runtime.Composable

/**
 * All user-visible strings. The app is English-only.
 */
object Strings {

    fun t(key: String): String = en[key] ?: key

    private val en = mapOf(
        "appName" to "Raftar VPN",
        "refresh" to "Refresh",
        "tabFree" to "Servers",
        "tabProxy" to "Proxy",
        "tabPrivate" to "My Servers",
        "connect" to "Connect",
        "disconnect" to "Disconnect",
        "cancel" to "Cancel",
        "retry" to "Retry",
        "statusConnecting" to "Connecting…",
        "statusConnected" to "Connected",
        "findingServer" to "Finding a working server…",
        "notConnectedHint" to "Pick a server below, then tap CONNECT.",
        "connectedTo" to "Connected",
        "statusFailed" to "Connection failed",
        "statusDisconnected" to "Not connected",
        "notConnectedHint" to "Pick a country below, then tap the button to connect.",
        "bestServer" to "Best Server",
        "bestServerSub" to "Auto-pick fastest",
        "autoFastest" to "Auto · Fastest",
        "quickConnect" to "FREE · Quick connect",
        "freeLabel" to "FREE",
        "findingBest" to "Finding best server…",
        "gamingMode" to "Gaming",
        "gamingModeSub" to "Lowest ping",
        "gamingPing" to "Ping",
        "raceTitle" to "Raftar Race",
        "raceLive" to "Testing servers live — fastest wins",
        "raceWinner" to "Winner",
        "raceUnreachable" to "unreachable",
        "searchHint" to "Search server or country…",
        "probing" to "Checking server reachability…",
        "footer" to "Free public servers (VPN Gate). Speed and quality vary.",
        "sharedIpNote" to "All servers use shared IPs — VPN Gate does not offer static IPs.",
        "sharedIp" to "Shared IP",
        "unknownCountry" to "Unknown",
        "serversLabel" to "servers",
        "allServers" to "All servers",
        "all" to "All",
        "proxyWarning" to "Free proxies are NOT encrypted like VPN — good for unblocking, never for banking or passwords.",
        "proxyLoad" to "Load proxies",
        "proxyEmpty" to "Tap “Load proxies” to fetch the free list.",
        "proxyCopied" to "Copied!",
        "proxyError" to "Proxy list could not be loaded",
        "speedDown" to "↓",
        "speedUp" to "↑",
        "l2tpGuide" to "L2TP Guide",
        "l2tpTitle" to "If OpenVPN is blocked, try L2TP",
        "l2tpBody" to "Android has a built-in VPN client that can use VPNGate's free L2TP servers (different protocol — sometimes works where OpenVPN is blocked):\n\n1. Open Android Settings → Network → VPN → Add VPN\n2. Type: L2TP/IPsec PSK\n3. Server address: pick any hostname from the server list + \".opengw.net\"\n   (e.g. public-vpn-52.opengw.net)\n4. IPSec pre-shared key: vpn\n5. Username: vpn\n6. Password: vpn\n7. Save, then tap to connect.",
        "l2tpServerHint" to "Example server for L2TP:",
        "ok" to "OK",
        "bestPingLabel" to "best",
        "noResults" to "No countries match your search.",
        "privacyPolicy" to "Privacy Policy",
        "privateEmpty" to "No private servers yet — add one below",
        "addPaste" to "Paste text",
        "addFile" to "Import file",
        "addQr" to "Scan QR",
        "pasteTitle" to "Paste config",
        "nameHint" to "Name (e.g. My Server)",
        "configHint" to "Paste .ovpn text here",
        "save" to "Save",
        "delete" to "Delete",
        "reportBad" to "Report bad server",
        "reportHint" to "Long-press a server to report it as bad",
        "reportedBad" to "Server reported — it won't show again",
        "invalidConfig" to "Not a valid .ovpn config (no remote line found)",
        "privateAdded" to "Server added",
        "privateDeleted" to "Server deleted",
        "confirmDelete" to "Delete this server?",
        "yes" to "Yes",
        "no" to "No",
        "qrPrompt" to "Scan the QR code",
        "cameraDenied" to "Camera permission is needed for QR scan",
        "fileReadError" to "Couldn't read file",
        "noticeRefreshFail" to "Refresh failed — showing cached list",
        "noticeNeedList" to "Wait for the server list to load",
        "noticeNoServer" to "No server found",
        "noticePermDenied" to "Can't connect without VPN permission",
        "noServersAvail" to "No servers available — refresh the list",
        "failoverFailed" to "Could not connect to any server — try again",
        "connLost" to "Connection lost — reconnect",
        "permNotGranted" to "VPN permission not granted",
        "gamingNotifTitle" to "Raftar VPN — Gaming Mode",
        "declined" to "You must accept the notice to connect",
        "disclosureTitle" to "Important notice",
        "accept" to "Accept",
        "attempting" to "Trying %1\$s (%2\$d/%3\$d)…",
        "disclosureText" to "Raftar VPN routes your internet traffic through a VPN server. " +
            "Free servers are run by volunteers (VPN Gate, University of Tsukuba) — " +
            "they may keep their own operational logs. We don't record your browsing, " +
            "require no account, and include no ads or analytics."
    )
}

/** Composable string lookup. */
@Composable
fun tr(key: String): String = Strings.t(key)
