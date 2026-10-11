package com.raftar.vpn

import android.app.Application
import com.raftar.vpn.vpn.VpnConnector

/**
 * App-scoped VpnConnector — MainActivity ka ViewModel aur Quick Settings Tile
 * dono isi ko use karte hain, taake tile se bhi VPN on/off ho sake.
 */
class RaftarApp : Application() {

    val connector: VpnConnector by lazy { VpnConnector(this) }

    /**
     * Tile se one-tap connect (sirf jab VPN permission pehle se granted ho).
     * MainViewModel init me set karta hai.
     */
    var quickConnect: (() -> Unit)? = null
}
