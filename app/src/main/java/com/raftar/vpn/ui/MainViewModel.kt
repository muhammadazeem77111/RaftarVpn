package com.raftar.vpn.ui

import android.app.Application
import android.content.Context
import android.net.TrafficStats
import android.net.Uri
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.raftar.vpn.RaftarApp
import com.raftar.vpn.data.BlocklistStore
import com.raftar.vpn.data.FavoritesStore
import com.raftar.vpn.data.FreeProxy
import com.raftar.vpn.data.PrivateServer
import com.raftar.vpn.data.PrivateServerStore
import com.raftar.vpn.data.ProxyApi
import com.raftar.vpn.data.VpnGateApi
import com.raftar.vpn.data.VpnServer
import com.raftar.vpn.data.isValidOvpn
import com.raftar.vpn.vpn.VpnConnector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Collections

data class RaceResult(val server: VpnServer, val ms: Long?)

sealed interface RaceState {
    data object Idle : RaceState
    data class Running(val results: List<RaceResult>) : RaceState
    data class Done(val winner: VpnServer?) : RaceState
}

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val ctx = app.applicationContext
    private val api = VpnGateApi(ctx)
    private val privateStore = PrivateServerStore(ctx)
    private val blocklist = BlocklistStore(ctx)
    private val favoritesStore = FavoritesStore(ctx)
    private val gameNotif = GameNotification(ctx)
    private val settings = ctx.getSharedPreferences("raftar_settings", Context.MODE_PRIVATE)

    /** App-scoped connector — Quick Settings Tile bhi isi ko use karta hai. */
    val connector: VpnConnector = (app as RaftarApp).connector

    // ---- free servers ----
    private val _servers = MutableStateFlow<List<VpnServer>>(emptyList())
    val servers: StateFlow<List<VpnServer>> = _servers.asStateFlow()

    /** Score desc, phir ping asc. Blocked (user-reported) servers pehle se nikle hue. Koi cap nahi. */
    private val _ranked = MutableStateFlow<List<VpnServer>>(emptyList())
    val ranked: StateFlow<List<VpnServer>> = _ranked.asStateFlow()

    /** server.id -> TCP reachable (session cache; null = abhi check nahi hua). */
    private val _reachability = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val reachability: StateFlow<Map<String, Boolean>> = _reachability.asStateFlow()

    private val _probing = MutableStateFlow(false)
    val probing: StateFlow<Boolean> = _probing.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _findingBest = MutableStateFlow(false)
    val findingBest: StateFlow<Boolean> = _findingBest.asStateFlow()

    private val _selected = MutableStateFlow<VpnServer?>(null)
    val selected: StateFlow<VpnServer?> = _selected.asStateFlow()

    private val _notice = MutableStateFlow<String?>(null)
    val notice: StateFlow<String?> = _notice.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    // ---- raftar race ----
    private val _race = MutableStateFlow<RaceState>(RaceState.Idle)
    val race: StateFlow<RaceState> = _race.asStateFlow()

    // ---- gaming mode ----
    private val _gaming = MutableStateFlow(false)
    val gaming: StateFlow<Boolean> = _gaming.asStateFlow()

    private val _gamePing = MutableStateFlow<Long?>(null)
    val gamePing: StateFlow<Long?> = _gamePing.asStateFlow()

    private var pingJob: Job? = null

    // ---- private servers ----
    private val _privateServers = MutableStateFlow(privateStore.list())
    val privateServers: StateFlow<List<PrivateServer>> = _privateServers.asStateFlow()

    // ---- favorites ----
    private val _favorites = MutableStateFlow(favoritesStore.all())
    val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()

    fun toggleFavorite(id: String) {
        _favorites.value = favoritesStore.toggle(id)
    }

    // ---- country filter (null = All countries) ----
    private val _countryFilter = MutableStateFlow<String?>(null)
    val countryFilter: StateFlow<String?> = _countryFilter.asStateFlow()

    fun setCountryFilter(iso: String?) {
        _countryFilter.value = iso
    }

    // ---- free proxies ----
    private val proxyApi = ProxyApi()
    private val _proxies = MutableStateFlow<List<FreeProxy>>(emptyList())
    val proxies: StateFlow<List<FreeProxy>> = _proxies.asStateFlow()

    private val _proxyLoading = MutableStateFlow(false)
    val proxyLoading: StateFlow<Boolean> = _proxyLoading.asStateFlow()

    /** Free SOCKS5 list lao + top 25 ka ping test (kaam karne wale pehle). */
    fun refreshProxies() {
        viewModelScope.launch {
            _proxyLoading.value = true
            try {
                val list = proxyApi.fetchSocks5()
                val head = list.take(25)
                val tested = coroutineScope {
                    head.map { p -> async { p.copy(pingMs = proxyApi.ping(p)) } }.awaitAll()
                }
                val alive = tested.filter { it.pingMs != null }.sortedBy { it.pingMs }
                val dead = tested.filter { it.pingMs == null }
                _proxies.value = alive + dead + list.drop(25)
            } catch (e: Exception) {
                _notice.value = e.message ?: Strings.t("proxyError")
            } finally {
                _proxyLoading.value = false
            }
        }
    }

    // ---- disclosure ----
    private val _showDisclosure = MutableStateFlow(false)
    val showDisclosure: StateFlow<Boolean> = _showDisclosure.asStateFlow()
    private var pendingConnectAction: (() -> Unit)? = null

    val vpnState: StateFlow<VpnConnector.VpnState> = connector.state

    /** Konsa server try ho raha hai (connect ke waqt progress dikhane ke liye). */
    val attemptInfo: StateFlow<String?> = connector.attemptInfo

    /** Connected- elapsed timer ke liye. */
    val connectedAt: StateFlow<Long?> = connector.connectedAt

    // ---- live speed (TrafficStats; engine traffic callback emit nahi karta) ----
    /** down, up bytes/sec — sirf connected rehne par. */
    private val _speed = MutableStateFlow<Pair<Long, Long>?>(null)
    val speed: StateFlow<Pair<Long, Long>?> = _speed.asStateFlow()
    private var speedJob: Job? = null

    // ---- auto-reconnect guard ----
    private var lastConnectServer: VpnServer? = null
    private var wasConnected = false
    private var autoRetried = false

    init {
        refresh(auto = true)
        // Quick Settings Tile se one-tap connect.
        (app as RaftarApp).quickConnect = { userStartConnect() }
        // State lifecycle: gaming ping + speed monitor, auto-reconnect guard.
        viewModelScope.launch {
            connector.state.collect { st ->
                when (st) {
                    is VpnConnector.VpnState.Connected -> {
                        wasConnected = true
                        autoRetried = false
                        if (_gaming.value) startPingMonitor(st.server)
                        startSpeedMonitor()
                    }
                    is VpnConnector.VpnState.Disconnected,
                    is VpnConnector.VpnState.Idle -> {
                        wasConnected = false
                        _gaming.value = false
                        stopPingMonitor()
                        stopSpeedMonitor()
                    }
                    is VpnConnector.VpnState.Failed -> {
                        _gaming.value = false
                        stopPingMonitor()
                        stopSpeedMonitor()
                        // Chalti connection achanak toot jaye to ek dafa khud dobara try.
                        if (wasConnected && !autoRetried) {
                            autoRetried = true
                            val s = lastConnectServer
                            if (s != null) {
                                viewModelScope.launch {
                                    delay(3000)
                                    if (connector.state.value is VpnConnector.VpnState.Failed) {
                                        startConnect(s, isAutoRetry = true)
                                    }
                                }
                            }
                        }
                        wasConnected = false
                    }
                    else -> Unit
                }
            }
        }
    }

    // ============ disclosure ============

    /** Pehli connect attempt par prominent disclosure — Accept ke baghair aagay nahi. */
    fun requestConnect(action: () -> Unit) {
        if (settings.getBoolean("disclosure_ok", false)) {
            action()
        } else {
            pendingConnectAction = action
            _showDisclosure.value = true
        }
    }

    fun acceptDisclosure() {
        settings.edit().putBoolean("disclosure_ok", true).apply()
        _showDisclosure.value = false
        pendingConnectAction?.invoke()
        pendingConnectAction = null
    }

    fun declineDisclosure() {
        _showDisclosure.value = false
        pendingConnectAction = null
        _notice.value = Strings.t("declined")
    }

    // ============ server list ============

    /** Pehle cache dikhao (foran), phir background me taza list. Kabhi fake list nahi. */
    fun refresh(auto: Boolean = false) {
        viewModelScope.launch {
            if (!auto) _loading.value = true
            try {
                val cached = api.loadCached()
                if (cached.isNotEmpty() && _servers.value.isEmpty()) applyList(cached)
                val fresh = api.fetchFresh()
                applyList(fresh)
            } catch (e: Exception) {
                if (_servers.value.isEmpty()) {
                    // Khaali haath: honest error, retry ka option — fake list kabhi nahi.
                    _notice.value = e.message ?: Strings.t("noticeNeedList")
                } else if (!auto) {
                    _notice.value = Strings.t("noticeRefreshFail")
                }
            } finally {
                _loading.value = false
            }
        }
    }

    private fun applyList(list: List<VpnServer>) {
        val visible = list.filter { !blocklist.isBlocked(it.id) }
        _servers.value = visible
        _ranked.value = api.rank(visible)
        if (_selected.value == null) _selected.value = _ranked.value.firstOrNull()
        probeTopServers()
    }

    /**
     * Top 20 ranked servers ki TCP reachability background me check karo.
     * Fail hone wala = is network par blocked/dead = recommended se bahar + dimmed.
     */
    private fun probeTopServers() {
        val targets = _ranked.value.filter { it.id !in _reachability.value }.take(20)
        if (targets.isEmpty()) return
        viewModelScope.launch {
            _probing.value = true
            try {
                val results = targets.map { s -> async { s.id to api.checkTcpReachable(s) } }.awaitAll()
                _reachability.value = _reachability.value + results.toMap()
            } finally {
                _probing.value = false
            }
        }
    }

    fun setQuery(q: String) {
        _query.value = q
    }

    fun select(server: VpnServer) {
        _selected.value = server
    }

    /** Long-press: user ne kharab server report kiya — is user ke liye hamesha ke liye bahar. */
    fun reportBad(server: VpnServer) {
        blocklist.block(server.id)
        _servers.value = _servers.value.filter { it.id != server.id }
        _ranked.value = _ranked.value.filter { it.id != server.id }
        _notice.value = Strings.t("reportedBad")
    }

    fun postNotice(msg: String) {
        _notice.value = msg
    }

    // ============ raftar race ============

    /**
     * Parallel TCP-connect race (top ~12 candidates). Live ranked results _race me,
     * winner return hota hai. Har result reachability cache me bhi jata hai.
     */
    private suspend fun raceServers(candidates: List<VpnServer>): VpnServer? = coroutineScope {
        _race.value = RaceState.Running(emptyList())
        val acc = Collections.synchronizedList(mutableListOf<RaceResult>())
        candidates.map { s ->
            async(Dispatchers.IO) {
                val ms = api.measureTcp(s)
                val r = RaceResult(s, ms)
                synchronized(acc) {
                    acc.add(r)
                    acc.sortWith(compareBy<RaceResult> { it.ms ?: Long.MAX_VALUE })
                }
                _race.value = RaceState.Running(acc.toList())
                _reachability.value = _reachability.value + (s.id to (ms != null))
                r
            }
        }.awaitAll()
        val winner = acc.firstOrNull { it.ms != null }?.server
        _race.value = RaceState.Done(winner)
        // Result card kuch second dikha kar auto-hide (connect me takheer na ho —
        // is liye viewModelScope me, is race ke scope me nahi).
        viewModelScope.launch {
            delay(8000)
            if (_race.value is RaceState.Done) _race.value = RaceState.Idle
        }
        winner
    }

    /** "Best Server — Auto Connect": race karao, winner ko connect karo. */
    fun autoConnectBest() {
        if (_ranked.value.isEmpty()) {
            _notice.value = Strings.t("noticeNeedList")
            return
        }
        viewModelScope.launch {
            _findingBest.value = true
            try {
                val candidates = _ranked.value.filter { api.isRecommended(it) }.take(12)
                    .ifEmpty { _ranked.value.take(12) }
                val winner = raceServers(candidates)
                if (winner == null) {
                    _notice.value = Strings.t("noticeNoServer")
                    return@launch
                }
                _selected.value = winner
                startConnect(winner)
            } finally {
                _findingBest.value = false
            }
        }
    }

    // ============ gaming mode ============

    /** Lowest-ping reachable server chuno, connect karo, live ping dikhao. */
    fun startGamingMode() {
        if (_ranked.value.isEmpty()) {
            _notice.value = Strings.t("noticeNeedList")
            return
        }
        viewModelScope.launch {
            _findingBest.value = true
            try {
                val candidates = _ranked.value.filter { api.isRecommended(it) }
                    .sortedBy { it.pingMs }.take(12)
                    .ifEmpty { _ranked.value.sortedBy { it.pingMs }.take(12) }
                val winner = raceServers(candidates)
                if (winner == null) {
                    _notice.value = Strings.t("noticeNoServer")
                    return@launch
                }
                _gaming.value = true
                _selected.value = winner
                startConnect(winner)
                startPingMonitor(winner)
            } finally {
                _findingBest.value = false
            }
        }
    }

    private fun startPingMonitor(server: VpnServer) {
        if (pingJob?.isActive == true) return
        pingJob = viewModelScope.launch {
            while (isActive) {
                val ms = api.measureTcp(server)
                _gamePing.value = ms
                gameNotif.show(ms)
                delay(10_000)
            }
        }
    }

    private fun stopPingMonitor() {
        pingJob?.cancel()
        pingJob = null
        _gamePing.value = null
        gameNotif.cancel()
    }

    private fun startSpeedMonitor() {
        if (speedJob?.isActive == true) return
        val uid = ctx.applicationInfo.uid
        speedJob = viewModelScope.launch {
            var lastRx = TrafficStats.getUidRxBytes(uid)
            var lastTx = TrafficStats.getUidTxBytes(uid)
            var lastT = SystemClock.elapsedRealtime()
            while (isActive) {
                delay(2000)
                val nowRx = TrafficStats.getUidRxBytes(uid)
                val nowTx = TrafficStats.getUidTxBytes(uid)
                val nowT = SystemClock.elapsedRealtime()
                val dt = (nowT - lastT) / 1000.0
                if (dt > 0 && lastRx >= 0 && nowRx >= lastRx && nowTx >= lastTx) {
                    _speed.value =
                        ((nowRx - lastRx) / dt).toLong() to ((nowTx - lastTx) / dt).toLong()
                }
                lastRx = nowRx
                lastTx = nowTx
                lastT = nowT
            }
        }
    }

    private fun stopSpeedMonitor() {
        speedJob?.cancel()
        speedJob = null
        _speed.value = null
    }

    // ============ connect ============

    /** Permission + disclosure ke baad selected server par connect. */
    fun userStartConnect() {
        val server = _selected.value ?: _ranked.value.firstOrNull()
        if (server == null) {
            _notice.value = Strings.t("noticeNeedList")
            return
        }
        _selected.value = server
        startConnect(server)
    }

    /** Country list me kisi server par tap → select + foran connect. */
    fun connectTo(server: VpnServer) {
        _selected.value = server
        startConnect(server)
    }

    private fun startConnect(server: VpnServer, isAutoRetry: Boolean = false) {
        if (!isAutoRetry) autoRetried = false
        lastConnectServer = server
        connector.clearDeadServers()
        // Selected server pehle (user ki choice), phir TCP-443 wale (firewall-friendly),
        // phir baqi. Pehle se dead/blocked (reachability == false) bahar.
        val reach = _reachability.value
        val rest = _ranked.value.filter { it.id != server.id && reach[it.id] != false }
        val (tcp443, others) = rest.partition { api.isTcp443(it) }
        val ordered = listOf(server) + tcp443 + others
        connector.connect(ordered, api::decodeConfig)
    }

    // ============ private servers ============

    fun importPrivateText(text: String, name: String) {
        val clean = text.trim()
        if (!isValidOvpn(clean)) {
            _notice.value = Strings.t("invalidConfig")
            return
        }
        privateStore.add(name.ifBlank { "Server" }, clean)
        _privateServers.value = privateStore.list()
        _notice.value = Strings.t("privateAdded")
    }

    fun importPrivateFile(uri: Uri) {
        try {
            val text = ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.readText().orEmpty()
            val name = uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: "Imported"
            importPrivateText(text, name)
        } catch (_: Exception) {
            _notice.value = Strings.t("fileReadError")
        }
    }

    fun deletePrivate(id: String) {
        privateStore.remove(id)
        _privateServers.value = privateStore.list()
        _notice.value = Strings.t("privateDeleted")
    }

    fun connectPrivate(server: PrivateServer) {
        connector.connectPrivate(server.name, server.config)
    }

    fun consumeNotice() {
        _notice.value = null
    }

    /** VpnService.prepare() system dialog deny hone par. */
    fun onPermissionDenied() {
        _notice.value = Strings.t("noticePermDenied")
    }
}
