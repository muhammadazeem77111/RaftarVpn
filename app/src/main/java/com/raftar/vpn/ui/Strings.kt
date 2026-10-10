package com.raftar.vpn.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * App ki tamam user-visible strings yahin se aati hain — UI me koi hardcoded text nahi.
 * Roman Urdu DEFAULT hai; English toggle settings/top-bar se.
 */
object Strings {

    enum class Lang { UR, EN }

    var current by mutableStateOf(Lang.UR)

    fun t(key: String): String = (if (current == Lang.UR) ur else en)[key] ?: en[key] ?: key

    // Toggle button par DUSRI language ka naam dikhta hai.
    fun toggleLabel(): String = if (current == Lang.UR) "English" else "Roman Urdu"

    private val ur = mapOf(
        "appName" to "Raftar VPN",
        "refresh" to "Refresh",
        "tabFree" to "Free Servers",
        "tabPrivate" to "Mere Servers",
        "connect" to "Connect",
        "disconnect" to "Disconnect",
        "cancel" to "Cancel",
        "retry" to "Dobara try karo",
        "statusConnecting" to "Server se jor raha hai…",
        "connectedTo" to "Connected",
        "statusFailed" to "Fail ho gaya",
        "statusDisconnected" to "Disconnected",
        "bestServer" to "Best Server — Auto Connect",
        "findingBest" to "Behtreen server dhoond raha hai…",
        "raceTitle" to "Raftar Race",
        "raceLive" to "Live race — sab se tez server jeetay ga",
        "raceWinner" to "Winner",
        "raceUnreachable" to "nahi pahuncha",
        "gamingMode" to "Gaming Mode",
        "gamingPing" to "Ping",
        "searchHint" to "Country ya server search karo",
        "probing" to "Servers check ho rahe hain…",
        "footer" to "Free public servers (VPN Gate) — sab countries feed se aati hain, speed/quality vary karti hai.",
        "unknownCountry" to "Unknown",
        "privacyPolicy" to "Privacy Policy",
        "privateEmpty" to "Abhi koi private server nahi — neeche se add karo",
        "addPaste" to "Text paste karo",
        "addFile" to "File import karo",
        "addQr" to "QR scan karo",
        "pasteTitle" to "Config paste karo",
        "nameHint" to "Naam (jaise: Mera Oracle Server)",
        "configHint" to ".ovpn text yahan paste karo",
        "save" to "Save karo",
        "delete" to "Delete",
        "reportBad" to "Report bad server",
        "reportedBad" to "Server report ho gaya — ab nahi dikhe ga",
        "invalidConfig" to "Ye valid .ovpn config nahi (remote line nahi mili)",
        "privateAdded" to "Server add ho gaya",
        "privateDeleted" to "Server delete ho gaya",
        "confirmDelete" to "Ye server delete karna hai?",
        "yes" to "Haan",
        "no" to "Nahi",
        "qrPrompt" to "QR code scan karo",
        "cameraDenied" to "QR scan ke liye camera permission chahiye",
        "fileReadError" to "File parh nahi saki",
        "noticeRefreshFail" to "Refresh fail — purani list dikhai ja rahi hai",
        "noticeNeedList" to "Pehle server list load honay do",
        "noticeNoServer" to "Koi server nahi mila",
        "noticePermDenied" to "VPN permission ke baghair connect nahi ho sakta",
        "noServersAvail" to "Koi server available nahi — list refresh karo",
        "failoverFailed" to "Koi server connect na ho saka — dobara try karo",
        "connLost" to "Connection toot gaya — dobara connect karo",
        "permNotGranted" to "VPN permission nahi mili",
        "gamingNotifTitle" to "Raftar VPN — Gaming Mode",
        "declined" to "Disclosure qubool kiye baghair VPN connect nahi ho sakta",
        "disclosureTitle" to "Zaroori baat",
        "accept" to "Qubool hai",
        "disclosureText" to "Raftar VPN aapka internet traffic VPN server ke zariye bhejta hai. " +
            "Free servers volunteers (VPN Gate, University of Tsukuba) chalate hain — " +
            "woh apne operational logs rakh sakte hain. Hum aapki browsing record nahi karte, " +
            "koi account nahi mangte, aur koi ads/analytics nahi lagate.\n\n" +
            "Raftar VPN routes your internet traffic through a VPN server. Free servers are run " +
            "by volunteers (VPN Gate, University of Tsukuba) — they may keep their own operational " +
            "logs. We don't record your browsing, require no account, and include no ads or analytics."
    )

    private val en = mapOf(
        "appName" to "Raftar VPN",
        "refresh" to "Refresh",
        "tabFree" to "Free Servers",
        "tabPrivate" to "My Servers",
        "connect" to "Connect",
        "disconnect" to "Disconnect",
        "cancel" to "Cancel",
        "retry" to "Retry",
        "statusConnecting" to "Connecting…",
        "connectedTo" to "Connected",
        "statusFailed" to "Failed",
        "statusDisconnected" to "Disconnected",
        "bestServer" to "Best Server — Auto Connect",
        "findingBest" to "Finding best server…",
        "raceTitle" to "Raftar Race",
        "raceLive" to "Live race — fastest server wins",
        "raceWinner" to "Winner",
        "raceUnreachable" to "unreachable",
        "gamingMode" to "Gaming Mode",
        "gamingPing" to "Ping",
        "searchHint" to "Search country or server",
        "probing" to "Checking servers…",
        "footer" to "Free public servers (VPN Gate) — all countries come from the live feed; speed/quality varies.",
        "unknownCountry" to "Unknown",
        "privacyPolicy" to "Privacy Policy",
        "privateEmpty" to "No private servers yet — add one below",
        "addPaste" to "Paste text",
        "addFile" to "Import file",
        "addQr" to "Scan QR",
        "pasteTitle" to "Paste config",
        "nameHint" to "Name (e.g. My Oracle Server)",
        "configHint" to "Paste .ovpn text here",
        "save" to "Save",
        "delete" to "Delete",
        "reportBad" to "Report bad server",
        "reportedBad" to "Server reported — won't show again",
        "invalidConfig" to "Not a valid .ovpn config (no remote line found)",
        "privateAdded" to "Server added",
        "privateDeleted" to "Server deleted",
        "confirmDelete" to "Delete this server?",
        "yes" to "Yes",
        "no" to "No",
        "qrPrompt" to "Scan the QR code",
        "cameraDenied" to "Camera permission needed for QR scan",
        "fileReadError" to "Couldn't read file",
        "noticeRefreshFail" to "Refresh failed — showing cached list",
        "noticeNeedList" to "Wait for the server list to load",
        "noticeNoServer" to "No server found",
        "noticePermDenied" to "Can't connect without VPN permission",
        "noServersAvail" to "No servers available — refresh the list",
        "failoverFailed" to "No server connected — try again",
        "connLost" to "Connection lost — reconnect",
        "permNotGranted" to "VPN permission not granted",
        "gamingNotifTitle" to "Raftar VPN — Gaming Mode",
        "declined" to "VPN can't connect unless you accept the disclosure",
        "disclosureTitle" to "Important notice",
        "accept" to "Accept",
        "disclosureText" to "Raftar VPN aapka internet traffic VPN server ke zariye bhejta hai. " +
            "Free servers volunteers (VPN Gate, University of Tsukuba) chalate hain — " +
            "woh apne operational logs rakh sakte hain. Hum aapki browsing record nahi karte, " +
            "koi account nahi mangte, aur koi ads/analytics nahi lagate.\n\n" +
            "Raftar VPN routes your internet traffic through a VPN server. Free servers are run " +
            "by volunteers (VPN Gate, University of Tsukuba) — they may keep their own operational " +
            "logs. We don't record your browsing, require no account, and include no ads or analytics."
    )
}

/** Composable version — language toggle par recompose hota hai. */
@Composable
fun tr(key: String): String {
    Strings.current // subscribe for recomposition
    return Strings.t(key)
}
