package com.raftar.vpn.vpn

import android.content.Context
import android.net.VpnService
import android.os.SystemClock
import com.raftar.vpn.MainActivity
import com.raftar.vpn.data.VpnServer
import com.raftar.vpn.data.parseRemoteEndpoint
import com.raftar.vpn.data.sanitizeOvpnConfig
import com.raftar.vpn.ui.Strings
import com.tim.basevpn.connection.VpnServiceConnection
import com.tim.basevpn.state.ConnectionState
import com.tim.openvpn.configuration.OpenVPNConfig
import com.tim.openvpn.service.OpenVPNService
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.InetSocketAddress
import java.net.Socket

/**
 * tim06 OpenVPN engine ka wrapper.
 *
 * Integration (verified against openvpn-1.1.3 / basevpnprotocols-1.1.1 bytecode):
 * - Config `OpenVPNService.startService(...)` ke zariye CONFIGURATION_KEY intent extra me
 *   jati hai — `VpnServiceConnection.start(...)` config drop kar deta hai, is liye sirf
 *   companion call config deliver karti hai.
 * - `VpnServiceConnection` (abstract) sirf bind + state callbacks ke liye hai:
 *   anonymous subclass banao aur `attachListener()` call karo.
 * - `OpenVPNConfigParser` VPN Gate configs par crash karta hai (tls-crypt block nahi hota),
 *   is liye OpenVPNConfig manually banate hain aur `configuration` me raw sanitized config
 *   rakhte hain — engine `conf.configuration ?: conf.buildConfig()` use karta hai.
 */
class VpnConnector(private val appContext: Context) {

    sealed interface VpnState {
        data object Idle : VpnState
        data object Connecting : VpnState
        data class Connected(val server: VpnServer) : VpnState
        data class Failed(val reason: String) : VpnState
        data object Disconnected : VpnState
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow<VpnState>(VpnState.Idle)
    val state: StateFlow<VpnState> = _state.asStateFlow()

    /** UI ke liye: is waqt konsa server try ho raha hai ("Trying X (2/5)…"), null = koi attempt nahi. */
    private val _attemptInfo = MutableStateFlow<String?>(null)
    val attemptInfo: StateFlow<String?> = _attemptInfo.asStateFlow()

    /** Connected kab hue (timer ke liye, elapsedRealtime ms). null = connected nahi. */
    private val _connectedAt = MutableStateFlow<Long?>(null)
    val connectedAt: StateFlow<Long?> = _connectedAt.asStateFlow()

    private var connection: VpnServiceConnection? = null
    private var connectJob: Job? = null
    private var attemptSignal: CompletableDeferred<Boolean>? = null
    private val deadServers = mutableSetOf<String>()

    @Volatile private var userStopped = false
    @Volatile private var activeServer: VpnServer? = null

    /** true = VPN permission pehle se granted; false = caller VpnService.prepare() intent launch kare. */
    fun prepare(): Boolean = VpnService.prepare(appContext) == null

    /**
     * [servers] rank-ordered honi chahiye; pehla server user ka selected/best hota hai.
     * Har server se pehle tez TCP pre-check (2.5s) — dead server foran skip, engine ke
     * 20s wait se bachat. Fail hone par aglay server par auto-failover (max 8 attempts),
     * attempts ke darmiyan ~1.5s ka waqfa.
     */
    fun connect(servers: List<VpnServer>, getConfig: (VpnServer) -> String?) {
        disconnectInternal(silent = true)
        userStopped = false
        _state.value = VpnState.Connecting
        connectJob = scope.launch {
            val candidates = servers.filter { it.id !in deadServers }.take(8)
            if (candidates.isEmpty()) {
                _state.value = VpnState.Failed(Strings.t("noServersAvail"))
                return@launch
            }
            for ((index, server) in candidates.withIndex()) {
                if (userStopped) return@launch
                _attemptInfo.value =
                    Strings.t("attempting").format(server.hostName, index + 1, candidates.size)
                val raw = getConfig(server)
                if (raw.isNullOrBlank()) {
                    deadServers.add(server.id)
                    continue
                }
                if (attemptConnect(server, raw)) return@launch // Connected state callback set kar chuka
                deadServers.add(server.id)
                if (index < candidates.lastIndex && !userStopped) delay(1500)
            }
            _attemptInfo.value = null
            if (!userStopped) {
                _state.value = VpnState.Failed(Strings.t("failoverFailed"))
            }
        }
    }

    /**
     * Apna private server ("Mere Servers"): single attempt, koi failover nahi —
     * config user ki apni hai, is liye wahi final hai.
     */
    fun connectPrivate(name: String, rawConfig: String) {
        val pseudo = VpnServer(
            hostName = name,
            ip = "",
            countryLong = "",
            countryShort = "",
            pingMs = 0,
            score = 0,
            speedBps = 0,
            sessions = 0,
            uptimeSec = 0,
            configBase64 = ""
        )
        connect(listOf(pseudo)) { rawConfig }
    }

    fun disconnect() = disconnectInternal(silent = false)

    /** Naye manual connect par skip-list reset (purane dead marks bhool jao). */
    fun clearDeadServers() {
        deadServers.clear()
    }

    private suspend fun attemptConnect(server: VpnServer, rawConfig: String): Boolean {
        teardownAttempt()
        val sanitized = sanitizeOvpnConfig(rawConfig)
        // NOTE: VPNGate ke OpenVPN servers ko username/password NAHI chahiye
        // (auth sirf L2TP ke liye hai: vpn/vpn). Is liye koi auth-user-pass nahi lagate.
        // Pehle ghalat file-based auth lagaya tha jo OpenVPN3 ke sath kaam nahi karta.
        val ep = parseRemoteEndpoint(sanitized)
        if (ep.host.isBlank()) return false
        // Tez pre-check: TCP server zinda hai? Dead par engine ke 20s zaya nahi karte.
        // (UDP ka pre-check mumkin nahi — woh seedha engine try karega.)
        if (ep.proto == "tcp" && !tcpPreCheck(ep.host, ep.port)) return false

        val cfg = OpenVPNConfig(
            name = server.hostName,
            host = ep.host,
            port = ep.port,
            type = if (ep.proto == "udp") "udp-client" else "tcp-client",
            configuration = sanitized
        )

        activeServer = server
        attemptSignal = CompletableDeferred()

        // 1) Config service tak pahunchana + foreground service start (engine isi se chalta hai)
        OpenVPNService.startService(appContext, cfg, MainActivity::class.java.name, emptyArray<String>())
        // 2) Sirf state callbacks ke liye bind (VpnServiceConnection.start config drop kar deta hai)
        connection = object : VpnServiceConnection(
            appContext,
            OpenVPNService::class.java,
            { st -> onEngineState(st) },
            scope
        ) {}
        connection?.attachListener()

        val connected = withTimeoutOrNull(20_000) { attemptSignal?.await() } ?: false
        if (!connected) teardownAttempt()
        return connected
    }

    private fun onEngineState(st: ConnectionState) {
        when (st) {
            ConnectionState.CONNECTED -> {
                if (userStopped) return
                val s = activeServer ?: return
                _attemptInfo.value = null
                _connectedAt.value = SystemClock.elapsedRealtime()
                _state.value = VpnState.Connected(s)
                attemptSignal?.complete(true)
            }
            ConnectionState.CONNECTING, ConnectionState.READYFORCONNECT -> {
                if (!userStopped) _state.value = VpnState.Connecting
            }
            ConnectionState.DISCONNECTING, ConnectionState.DISCONNECTED -> {
                val signaled = attemptSignal?.complete(false) ?: false
                val wasConnected = _state.value is VpnState.Connected
                if (!signaled && wasConnected && !userStopped) {
                    // Connect ke BAAD connection toot gayi — failover nahi, wazeh error.
                    teardownAttempt()
                    _state.value = VpnState.Failed(Strings.t("connLost"))
                }
            }
            ConnectionState.PERMISSION_NOT_GRANTED -> {
                attemptSignal?.complete(false)
                if (!userStopped) _state.value = VpnState.Failed(Strings.t("permNotGranted"))
            }
            ConnectionState.IDLE -> Unit
        }
    }

    /**
     * Engine start karne se pehle tez TCP check (2.5s timeout).
     * Dead/band server foran bahar — 20s ka engine wait zaya nahi hota.
     */
    private suspend fun tcpPreCheck(host: String, port: Int): Boolean =
        withContext(Dispatchers.IO) {
            try {
                Socket().use { it.connect(InetSocketAddress(host, port), 5000) }
                true
            } catch (_: Exception) {
                false
            }
        }

    private fun teardownAttempt() {
        try {
            connection?.stop()
        } catch (_: Exception) {
        }
        try {
            connection?.stopServiceIfNeed(true)
        } catch (_: Exception) {
        }
        try {
            OpenVPNService.stopService(appContext)
        } catch (_: Exception) {
        }
        connection = null
        attemptSignal = null
    }

    private fun disconnectInternal(silent: Boolean) {
        userStopped = true
        connectJob?.cancel()
        connectJob = null
        teardownAttempt()
        activeServer = null
        _attemptInfo.value = null
        _connectedAt.value = null
        if (!silent) _state.value = VpnState.Disconnected
    }
}
