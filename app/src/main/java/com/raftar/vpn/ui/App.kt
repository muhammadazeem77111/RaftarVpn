package com.raftar.vpn.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.os.SystemClock
import kotlinx.coroutines.delay
import com.raftar.vpn.R
import com.raftar.vpn.data.FreeProxy
import com.raftar.vpn.data.PrivateServer
import com.raftar.vpn.data.VpnServer
import com.raftar.vpn.vpn.VpnConnector
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** ISO country code ("US") -> flag emoji ("🇺🇸"). */
fun flagEmoji(iso: String): String {
    val up = iso.trim().uppercase()
    if (up.length != 2 || !up.all { it in 'A'..'Z' }) return "🏳️"
    return up.map { ch -> String(Character.toChars(0x1F1E6 + (ch - 'A'))) }.joinToString("")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun App(
    viewModel: MainViewModel,
    onConnectClick: () -> Unit,
    onBestServerClick: () -> Unit,
    onGamingClick: () -> Unit,
    onPrivateConnect: (PrivateServer) -> Unit,
    onImportFileClick: () -> Unit,
    onScanQrClick: () -> Unit
) {
    var showPrivacy by remember { mutableStateOf(false) }

    if (showPrivacy) {
        PrivacyScreen(onBack = { showPrivacy = false })
        return
    }
    if (showL2tp) {
        AlertDialog(
            onDismissRequest = { showL2tp = false },
            title = { Text(tr("l2tpTitle"), fontWeight = FontWeight.Bold) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(tr("l2tpBody"), fontSize = 13.sp, lineHeight = 19.sp)
                    val example = ranked.firstOrNull()?.hostName
                    if (example != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "${tr("l2tpServerHint")} $example.opengw.net",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showL2tp = false }) { Text(tr("ok")) }
            }
        )
    }

    val ranked by viewModel.ranked.collectAsState()
    val reachability by viewModel.reachability.collectAsState()
    val probing by viewModel.probing.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val findingBest by viewModel.findingBest.collectAsState()
    val selected by viewModel.selected.collectAsState()
    val vpnState by viewModel.vpnState.collectAsState()
    val attemptInfo by viewModel.attemptInfo.collectAsState()
    val notice by viewModel.notice.collectAsState()
    val query by viewModel.query.collectAsState()
    val race by viewModel.race.collectAsState()
    val gaming by viewModel.gaming.collectAsState()
    val gamePing by viewModel.gamePing.collectAsState()
    val privateServers by viewModel.privateServers.collectAsState()
    val showDisclosure by viewModel.showDisclosure.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val proxies by viewModel.proxies.collectAsState()
    val proxyLoading by viewModel.proxyLoading.collectAsState()
    val speed by viewModel.speed.collectAsState()
    val countryFilter by viewModel.countryFilter.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(notice) {
        notice?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeNotice()
        }
    }
    LaunchedEffect(vpnState) {
        val s = vpnState
        if (s is VpnConnector.VpnState.Failed) snackbar.showSnackbar(s.reason)
    }

    var tab by remember { mutableIntStateOf(0) }
    var reportTarget by remember { mutableStateOf<VpnServer?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    var showL2tp by remember { mutableStateOf(false) }

    // Connection timer (00:00:00).
    val connectedAt by viewModel.connectedAt.collectAsState()
    var elapsedSec by remember { mutableStateOf(0L) }
    LaunchedEffect(connectedAt) {
        val start = connectedAt
        if (start == null) {
            elapsedSec = 0L
        } else {
            while (true) {
                elapsedSec = (SystemClock.elapsedRealtime() - start) / 1000
                delay(1000)
            }
        }
    }

    // Tamam countries ke servers — aik hi flat list (favorites sab se pehle).
    // Country chips: har available country, count ke sath.
    val countryCounts = remember(ranked) {
        ranked.groupBy { it.countryShort }
            .map { (iso, list) -> Triple(iso, list.first().countryLong, list.size) }
            .sortedByDescending { it.third }
    }
    val filtered = remember(ranked, query, countryFilter) {
        val q = query.trim().lowercase()
        ranked.filter { s ->
            (countryFilter == null || s.countryShort == countryFilter) &&
                (q.isEmpty() ||
                    s.countryLong.lowercase().contains(q) ||
                    s.hostName.lowercase().contains(q) ||
                    s.ip.contains(q))
        }
    }
    val allServers = remember(filtered, favorites) {
        val fav = filtered.filter { it.id in favorites }
        val rest = filtered.filter { it.id !in favorites }
        fav + rest
    }

    if (showDisclosure) {
        DisclosureDialog(
            onAccept = { viewModel.acceptDisclosure() },
            onDecline = { viewModel.declineDisclosure() }
        )
    }
    reportTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { reportTarget = null },
            title = { Text(tr("reportBad")) },
            text = { Text("${target.hostName} (${target.ip})") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.reportBad(target)
                    reportTarget = null
                }) { Text(tr("yes")) }
            },
            dismissButton = {
                TextButton(onClick = { reportTarget = null }) { Text(tr("no")) }
            }
        )
    }

    RaftarTheme {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    ),
                    navigationIcon = {
                        TextButton(onClick = { showMenu = true }) {
                            Text("☰", fontSize = 22.sp)
                        }
                    },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                                contentDescription = null,
                                modifier = Modifier.size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(tr("appName"), fontWeight = FontWeight.Bold)
                        }
                    },
                    actions = {
                        TextButton(onClick = { viewModel.refresh() }) { Text(tr("refresh")) }
                    }
                )
            },
            snackbarHost = { SnackbarHost(snackbar) }
        ) { pad ->
            if (showMenu) {
                AlertDialog(
                    onDismissRequest = { showMenu = false },
                    title = { Text(tr("appName"), fontWeight = FontWeight.Bold) },
                    text = {
                        Column {
                            TextButton(
                                onClick = { showMenu = false; viewModel.refresh() },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(tr("refresh")) }
                            TextButton(
                                onClick = { showMenu = false; showPrivacy = true },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(tr("privacyPolicy")) }
                            TextButton(
                                onClick = { showMenu = false; showL2tp = true },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(tr("l2tpGuide")) }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showMenu = false }) { Text(tr("cancel")) }
                    }
                )
            }
            Column(Modifier.padding(pad).fillMaxSize()) {
                TabRow(selectedTabIndex = tab) {
                    Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(tr("tabFree")) })
                    Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(tr("tabProxy")) })
                    Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text(tr("tabPrivate")) })
                }

                if (tab == 0) {
                    LazyColumn(Modifier.weight(1f)) {
                        item {
                            StatusHero(
                                vpnState = vpnState,
                                selected = selected,
                                elapsedSec = elapsedSec,
                                speed = speed
                            )
                        }
                        item {
                            ConnectButton(
                                vpnState = vpnState,
                                attemptInfo = attemptInfo,
                                onConnectClick = onConnectClick
                            )
                        }
                        item {
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onBestServerClick,
                                    enabled = !findingBest && ranked.isNotEmpty(),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        if (findingBest) "…" else "⚡ ${tr("autoFastest")}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                                OutlinedButton(
                                    onClick = onGamingClick,
                                    enabled = !findingBest && ranked.isNotEmpty(),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        if (gaming) "🎮 ${gamePing?.let { "$it ms" } ?: "…"}"
                                        else "🎮 ${tr("gamingMode")}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                        if (race is RaceState.Running || race is RaceState.Done) {
                            item { RaceCard(race = race) }
                        }
                        if (probing) {
                            item {
                                Text(
                                    tr("probing"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                                )
                            }
                        }
                        item {
                            OutlinedTextField(
                                value = query,
                                onValueChange = viewModel::setQuery,
                                label = { Text(tr("searchHint")) },
                                singleLine = true,
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier.fillMaxWidth().padding(20.dp)
                            )
                        }
                        item {
                            Text(
                                "${tr("allServers")} (${allServers.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                            )
                        }
                        // Country filter chips — kisi bhi country ke servers foran dekho.
                        item {
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                item {
                                    FilterChip(
                                        selected = countryFilter == null,
                                        onClick = { viewModel.setCountryFilter(null) },
                                        label = { Text("${tr("all")} (${ranked.size})") }
                                    )
                                }
                                items(countryCounts, key = { it.first }) { (iso, name, count) ->
                                    FilterChip(
                                        selected = countryFilter == iso,
                                        onClick = {
                                            viewModel.setCountryFilter(if (countryFilter == iso) null else iso)
                                        },
                                        label = { Text("${flagEmoji(iso)} $name ($count)") }
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                        if (loading && allServers.isEmpty()) {
                            item {
                                Box(
                                    Modifier.fillMaxWidth().padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        } else if (allServers.isEmpty()) {
                            item {
                                Box(
                                    Modifier.fillMaxWidth().padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(tr("noResults"))
                                }
                            }
                        } else {
                            items(allServers, key = { it.id }) { server ->
                                ServerRow(
                                    server = server,
                                    isSelected = server.id == selected?.id,
                                    isConnected = (vpnState as? VpnConnector.VpnState.Connected)?.server?.id == server.id,
                                    isFavorite = server.id in favorites,
                                    onToggleFavorite = { viewModel.toggleFavorite(server.id) },
                                    reachable = reachability[server.id],
                                    onClick = { viewModel.select(server) },
                                    onLongClick = { reportTarget = server }
                                )
                            }
                        }
                        item {
                            Text(
                                "${tr("sharedIpNote")} ${tr("reportHint")}.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth().padding(20.dp)
                            )
                        }
                    }
                } else if (tab == 1) {
                    ProxyTab(
                        viewModel = viewModel,
                        proxies = proxies,
                        loading = proxyLoading,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    PrivateTab(
                        viewModel = viewModel,
                        privateServers = privateServers,
                        onPrivateConnect = onPrivateConnect,
                        onImportFileClick = onImportFileClick,
                        onScanQrClick = onScanQrClick,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        tr("footer"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { showPrivacy = true }) {
                        Text(tr("privacyPolicy"))
                    }
                }
            }
        }
    }
}

/** Surfshark-style status hero: bara status + selected/connected server. */
@Composable
private fun StatusHero(
    vpnState: VpnConnector.VpnState,
    selected: VpnServer?,
    elapsedSec: Long,
    speed: Pair<Long, Long>?
) {
    val connected = vpnState is VpnConnector.VpnState.Connected
    val connecting = vpnState == VpnConnector.VpnState.Connecting
    val failed = vpnState is VpnConnector.VpnState.Failed
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp)) {
        Text(
            when {
                connected -> tr("statusConnected")
                connecting -> tr("statusConnecting")
                failed -> tr("statusFailed")
                else -> tr("statusDisconnected")
            },
            fontSize = 30.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (connected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(6.dp))
        val detail = when {
            connected -> {
                val s = (vpnState as VpnConnector.VpnState.Connected).server
                "${flagEmoji(s.countryShort)} ${s.countryLong} · ${s.hostName}\n🕐 ${formatElapsed(elapsedSec)}"
            }
            connecting -> tr("findingServer")
            selected != null ->
                "${flagEmoji(selected.countryShort)} ${selected.countryLong} · ${selected.hostName}"
            else -> tr("notConnectedHint")
        }
        Text(
            detail,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
        if (connected && speed != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                "${tr("speedDown")} ${formatLiveSpeed(speed.first)}   ${tr("speedUp")} ${formatLiveSpeed(speed.second)}",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun formatLiveSpeed(bps: Long): String = when {
    bps < 1024 -> "$bps B/s"
    bps < 1024 * 1024 -> String.format(Locale.US, "%.1f KB/s", bps / 1024.0)
    else -> String.format(Locale.US, "%.1f MB/s", bps / (1024.0 * 1024.0))
}

/** Bari full-width CONNECT button (Surfshark jaisi). */
@Composable
private fun ConnectButton(
    vpnState: VpnConnector.VpnState,
    attemptInfo: String?,
    onConnectClick: () -> Unit
) {
    val connected = vpnState is VpnConnector.VpnState.Connected
    val connecting = vpnState == VpnConnector.VpnState.Connecting
    val label = when {
        connected -> tr("disconnect")
        connecting -> attemptInfo ?: tr("statusConnecting")
        vpnState is VpnConnector.VpnState.Failed -> tr("retry")
        else -> tr("connect")
    }
    Button(
        onClick = onConnectClick,
        enabled = !connecting,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (connected) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.primary,
            contentColor = if (connected) MaterialTheme.colorScheme.onError
            else MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .height(58.dp)
    ) {
        if (connecting) {
            CircularProgressIndicator(
                Modifier.size(22.dp),
                strokeWidth = 2.5.dp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(10.dp))
        }
        Text(
            if (connecting) label else label.uppercase(),
            fontWeight = FontWeight.ExtraBold,
            fontSize = if (connecting) 14.sp else 17.sp
        )
    }
}

@Composable
private fun RaceCard(race: RaceState) {
    when (race) {
        is RaceState.Idle -> Unit
        is RaceState.Running -> {
            Card(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        "${tr("raceTitle")} — ${tr("raceLive")}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(Modifier.height(8.dp))
                    race.results.take(12).forEachIndexed { i, r ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                            Text("${i + 1}.", modifier = Modifier.width(28.dp))
                            Text(
                                "${flagEmoji(r.server.countryShort)} ${r.server.hostName}",
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                r.ms?.let { "$it ms" } ?: tr("raceUnreachable"),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
        is RaceState.Done -> {
            val w = race.winner ?: return
            Card(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Text(
                    "${tr("raceWinner")}: ${flagEmoji(w.countryShort)} ${w.hostName} (${w.countryLong})",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}

/** Surfshark-style flat server row: flag + naam + ping + star. Tap = select. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ServerRow(
    server: VpnServer,
    isSelected: Boolean,
    isConnected: Boolean,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    reachable: Boolean?,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val dead = reachable == false
    val dotColor = when (reachable) {
        true -> Color(0xFF2E7D32)
        false -> Color(0xFFC62828)
        null -> Color(0xFF9E9E9E)
    }
    Column(Modifier.fillMaxWidth().alpha(if (dead) 0.45f else 1f)) {
        Row(
            Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .background(
                    if (isSelected || isConnected) MaterialTheme.colorScheme.primaryContainer
                    else Color.Transparent
                )
                .padding(horizontal = 20.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(flagEmoji(server.countryShort), fontSize = 22.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).background(dotColor, CircleShape))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        server.hostName,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    "${server.countryLong} • ${server.pingMs} ms • ${formatSpeed(server.speedBps)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isConnected) {
                Text(
                    "✓",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(Modifier.width(8.dp))
            } else if (isSelected) {
                Text(
                    "✓",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = if (isFavorite) "★" else "☆",
                fontSize = 22.sp,
                color = if (isFavorite) Color(0xFFFFB300) else MaterialTheme.colorScheme.outline,
                modifier = Modifier.clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onToggleFavorite
                )
            )
        }
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
        )
    }
}

private fun formatElapsed(totalSec: Long): String {
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return "%02d:%02d:%02d".format(h, m, s)
}

@Composable
private fun PrivateTab(
    viewModel: MainViewModel,
    privateServers: List<PrivateServer>,
    onPrivateConnect: (PrivateServer) -> Unit,
    onImportFileClick: () -> Unit,
    onScanQrClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showPaste by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<PrivateServer?>(null) }

    if (showPaste) {
        PasteDialog(
            onSave = { name, config ->
                viewModel.importPrivateText(config, name)
                showPaste = false
            },
            onDismiss = { showPaste = false }
        )
    }
    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(tr("delete")) },
            text = { Text("${tr("confirmDelete")}\n${target.name}") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deletePrivate(target.id)
                    deleteTarget = null
                }) { Text(tr("yes")) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text(tr("no")) }
            }
        )
    }

    Column(modifier) {
        Row(
            Modifier.fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = { showPaste = true }, modifier = Modifier.weight(1f)) {
                Text(tr("addPaste"))
            }
            OutlinedButton(onClick = onImportFileClick, modifier = Modifier.weight(1f)) {
                Text(tr("addFile"))
            }
            OutlinedButton(onClick = onScanQrClick, modifier = Modifier.weight(1f)) {
                Text(tr("addQr"))
            }
        }
        if (privateServers.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    tr("privateEmpty"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(Modifier.weight(1f)) {
                items(privateServers, key = { it.id }) { ps ->
                    ListItem(
                        headlineContent = { Text(ps.name, fontWeight = FontWeight.Bold) },
                        supportingContent = {
                            Text(
                                SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                                    .format(Date(ps.createdAt))
                            )
                        },
                        trailingContent = {
                            Row {
                                TextButton(onClick = { onPrivateConnect(ps) }) {
                                    Text(tr("connect"))
                                }
                                TextButton(onClick = { deleteTarget = ps }) {
                                    Text(tr("delete"))
                                }
                            }
                        }
                    )
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PasteDialog(onSave: (String, String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var config by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("pasteTitle")) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(tr("nameHint")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = config,
                    onValueChange = { config = it },
                    label = { Text(tr("configHint")) },
                    minLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, config) }) { Text(tr("save")) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(tr("cancel")) }
        }
    )
}

@Composable
private fun DisclosureDialog(onAccept: () -> Unit, onDecline: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDecline,
        title = { Text(tr("disclosureTitle"), fontWeight = FontWeight.Bold) },
        text = {
            Text(
                tr("disclosureText"),
                modifier = Modifier.verticalScroll(rememberScrollState())
            )
        },
        confirmButton = {
            TextButton(onClick = onAccept) { Text(tr("accept")) }
        },
        dismissButton = {
            TextButton(onClick = onDecline) { Text(tr("no")) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrivacyScreen(onBack: () -> Unit) {
    RaftarTheme {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    ),
                    title = { Text(tr("privacyPolicy")) },
                    navigationIcon = {
                        TextButton(onClick = onBack) { Text("←") }
                    }
                )
            }
        ) { pad ->
            Text(
                PRIVACY_POLICY_TEXT,
                modifier = Modifier
                    .padding(pad)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            )
        }
    }
}

private fun formatSpeed(bps: Long): String = when {
    bps <= 0 -> "speed ?"
    bps >= 1_000_000 -> "%.1f Mbps".format(bps / 1_000_000.0)
    bps >= 1_000 -> "%.0f Kbps".format(bps / 1_000.0)
    else -> "$bps bps"
}

/** Free SOCKS5 proxy list tab — copy karke Telegram waghera me use karo. */
@Composable
private fun ProxyTab(
    viewModel: MainViewModel,
    proxies: List<FreeProxy>,
    loading: Boolean,
    modifier: Modifier = Modifier
) {
    val clipboard = LocalClipboardManager.current
    Column(modifier = modifier.padding(horizontal = 20.dp)) {
        Text(
            tr("proxyWarning"),
            color = Color(0xFFFFB74D),
            fontSize = 12.sp,
            lineHeight = 17.sp,
            modifier = Modifier.padding(vertical = 10.dp)
        )
        Button(
            onClick = { viewModel.refreshProxies() },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                if (proxies.isEmpty()) tr("proxyLoad") else tr("refresh"),
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(8.dp))
        if (proxies.isEmpty() && !loading) {
            Text(
                tr("proxyEmpty"),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(proxies, key = { "${it.ip}:${it.port}" }) { p ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "${p.ip}:${p.port}",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                "SOCKS5 · " + (p.pingMs?.let { "$it ms" } ?: "…"),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = {
                            clipboard.setText(AnnotatedString("${p.ip}:${p.port}"))
                        }) {
                            Text(tr("copy"), color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline,
                        thickness = 0.5.dp
                    )
                }
            }
        }
    }
}
