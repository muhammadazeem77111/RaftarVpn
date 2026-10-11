package com.raftar.vpn

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.raftar.vpn.vpn.VpnConnector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch

/**
 * Notification shade se one-tap VPN on/off.
 * Connect: permission granted ho to foran, warna app khul kar proper flow (permission + disclosure).
 */
class RaftarTileService : TileService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private fun app() = application as RaftarApp

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
        scope.launch {
            app().connector.state.collect { updateTile() }
        }
    }

    override fun onStopListening() {
        scope.coroutineContext.cancelChildren()
        super.onStopListening()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onClick() {
        super.onClick()
        val app = app()
        when (app.connector.state.value) {
            is VpnConnector.VpnState.Connected,
            is VpnConnector.VpnState.Connecting -> app.connector.disconnect()
            else -> {
                if (app.connector.prepare()) {
                    // Permission pehle se granted — seedha connect.
                    app.quickConnect?.invoke() ?: openApp(autoConnect = true)
                } else {
                    openApp(autoConnect = true)
                }
            }
        }
    }

    private fun openApp(autoConnect: Boolean) {
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (autoConnect) intent.putExtra(EXTRA_AUTO_CONNECT, true)
        if (Build.VERSION.SDK_INT >= 24) startActivityAndCollapse(intent)
        else startActivity(intent)
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        tile.state = when (app().connector.state.value) {
            is VpnConnector.VpnState.Connected,
            is VpnConnector.VpnState.Connecting -> Tile.STATE_ACTIVE
            else -> Tile.STATE_INACTIVE
        }
        tile.updateTile()
    }

    companion object {
        const val EXTRA_AUTO_CONNECT = "auto_connect"
    }
}
