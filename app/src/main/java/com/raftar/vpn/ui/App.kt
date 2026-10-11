package com.raftar.vpn.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.os.SystemClock
import kotlinx.coroutines.delay
import com.raftar.vpn.R
import com.raftar.vpn.data.PrivateServer
import com.raftar.vpn.data.VpnServer
import com.raftar.vpn.vpn.VpnConnector
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Ek country + uske servers. */
private data class CountryGroup(
    val name: String,
    val iso: String,
    val servers: List<VpnServer>
)

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
    onServerClick: (VpnServer) -> Unit,
    onPrivateConnect: (PrivateServer) -> Unit,
    onImportFileClick: () -> Unit,
    onScanQrClick: () -> Unit
) {
    var showPrivacy by remember { mutableStateOf(false) }

    if (showPrivacy) {
        PrivacyScreen(onBack = { showPrivacy = false })
        return
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

    // Connection timer (screenshot jaisa 00:00:00).
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

    // Country navigation: null = countries list, warna us country ke servers.
    var openCountry by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(query) { openCountry = null }

    val unknownLabel = tr("unknownCountry")
    val filtered = remember(ranked, query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) ranked
        else ranked.filter {
            it.countryLong.lowercase().contains(q) ||
                it.hostName.lowercase().contains(q) ||
                it.ip.contains(q)
        }
    }
    val groups = remember(filtered, unknownLabel) {
        filtered.groupBy { it.countryLong.ifBlank { unknownLabel } }
            .map { (name, list) ->
                CountryGroup(name, list.firstOrNull()?.countryShort.orEmpty(), list)
            }
            .sortedBy { it.name }
    }
    val activeGroup = remember(groups, openCountry) {
        groups.firstOrNull { it.name == openCountry }
    }
    val favorites by viewModel.favorites.collectAsState()
    val sortedGroups = remember(groups, favorites) {
        groups.sortedWith(
            compareByDescending<CountryGroup> { it.name in favorites }.thenBy { it.name }
        )
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
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        TextButton(onClick = { showMenu = true }) {
                            Text("☰", fontSize = 22.sp)
                        }
                    },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF0D47A1)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "R",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
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
                    Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(tr("tabPrivate")) })
                }

                if (tab == 0) {
                    if (activeGroup == null) {
                        LazyColumn(Modifier.weight(1f)) {
                            item {
                                AutoFastestCard(
                                    onClick = onBestServerClick,
                                    findingBest = findingBest,
                                    enabled = ranked.isNotEmpty()
                                )
                            }
                            item {
                                PowerButton(
                                    vpnState = vpnState,
                                    attemptInfo = attemptInfo,
                                    onConnectClick = onConnectClick
                                )
                            }
                            item {
                                StatusCard(
                                    vpnState = vpnState,
                                    elapsedSec = elapsedSec,
                                    gaming = gaming,
                                    gamePing = gamePing
                                )
                            }
                            item {
                                OutlinedButton(
                                    onClick = onGamingClick,
                                    enabled = !findingBest && ranked.isNotEmpty(),
                                    shape = RoundedCornerShape(18.dp),
                                    modifier = Modifier.fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        if (gaming) "${tr("gamingMode")} • ${gamePing?.let { "$it ms" } ?: "…"}"
                                        else tr("gamingMode"),
                                        fontWeight = FontWeight.Bold
                                    )
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
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
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
                                    modifier = Modifier.fillMaxWidth().padding(12.dp)
                                )
                            }
                            if (loading && ranked.isEmpty()) {
                                item {
                                    Box(
                                        Modifier.fillMaxWidth().padding(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }
                            } else if (sortedGroups.isEmpty()) {
                                item {
                                    Box(
                                        Modifier.fillMaxWidth().padding(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(tr("noResults"))
                                    }
                                }
                            } else {
                                items(sortedGroups, key = { it.name }) { g ->
                                    CountryCard(
                                        group = g,
                                        isFavorite = g.name in favorites,
                                        onOpen = { openCountry = g.name },
                                        onToggleFavorite = { viewModel.toggleFavorite(g.name) }
                                    )
                                }
                            }
                            item {
                                Text(
                                    "${tr("sharedIpNote")} ${tr("reportHint")}.",
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                                )
                            }
                        }
                    } else {
                        CountryDetailBar(
                            group = activeGroup,
                            onBack = { openCountry = null }
                        )
                        LazyColumn(Modifier.weight(1f)) {
                            items(activeGroup.servers, key = { it.id }) { server ->
                                ServerRow(
                                    server = server,
                                    isSelected = server.id == selected?.id,
                                    isConnected = (vpnState as? VpnConnector.VpnState.Connected)?.server?.id == server.id,
                                    reachable = reachability[server.id],
                                    onClick = { onServerClick(server) },
                                    onLongClick = { reportTarget = server }
                                )
                            }
                        }
                    }
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
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        tr("footer"),
                        style = MaterialTheme.typography.bodySmall,
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

/** Bara gol power button (screenshot jaisa) — beech me glow ke saath. */
@Composable
private fun PowerButton(
    vpnState: VpnConnector.VpnState,
    attemptInfo: String?,
    onConnectClick: () -> Unit
) {
    val connected = vpnState is VpnConnector.VpnState.Connected
    val connecting = vpnState == VpnConnector.VpnState.Connecting
    val buttonColor = if (connected) Color(0xFFE53935) else Color(0xFF00BFA5)

    val pulse = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by pulse.animateFloat(
        initialValue = 0.75f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Column(
        Modifier.fillMaxWidth().padding(top = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            // halka glow
            Box(
                Modifier
                    .size(230.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0x3300BFA5), Color.Transparent)
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .graphicsLayer { alpha = if (connecting) pulseAlpha else 1f }
                    .clip(CircleShape)
                    .background(buttonColor)
                    .clickable(onClick = onConnectClick),
                contentAlignment = Alignment.Center
            ) {
                if (connecting) {
                    CircularProgressIndicator(
                        Modifier.size(56.dp),
                        color = Color.White,
                        strokeWidth = 5.dp
                    )
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_power),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(56.dp)
                    )
                }
            }
        }
        val hint = when {
            connected -> tr("disconnect")
            connecting -> attemptInfo ?: tr("cancel")
            vpnState is VpnConnector.VpnState.Failed -> tr("retry")
            else -> tr("notConnectedHint")
        }
        Text(
            hint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 6.dp)
        )
    }
}

/** Status card: dot + status + timer (screenshot jaisa). */
@Composable
private fun StatusCard(
    vpnState: VpnConnector.VpnState,
    elapsedSec: Long,
    gaming: Boolean,
    gamePing: Long?
) {
    val connected = vpnState is VpnConnector.VpnState.Connected
    val connecting = vpnState == VpnConnector.VpnState.Connecting
    val statusText = when (vpnState) {
        is VpnConnector.VpnState.Connected -> {
            val s = vpnState.server
            val where = s.countryLong.ifBlank { tr("tabPrivate") }
            "${tr("connectedTo")} • ${flagEmoji(s.countryShort)} $where"
        }
        VpnConnector.VpnState.Connecting -> tr("statusConnecting")
        is VpnConnector.VpnState.Failed -> tr("statusFailed")
        else -> tr("statusDisconnected")
    }
    val dotColor = when {
        connected -> Color(0xFF00C853)
        connecting -> Color(0xFFFFB300)
        else -> Color(0xFFB0BEC5)
    }
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).background(dotColor, CircleShape))
                Spacer(Modifier.width(8.dp))
                Text(statusText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "🕐 ${formatElapsed(elapsedSec)}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
            if (gaming && connected) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "${tr("gamingMode")} • ${tr("gamingPing")}: ${gamePing?.let { "$it ms" } ?: "…"}",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private fun formatElapsed(totalSec: Long): String {
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return "%02d:%02d:%02d".format(h, m, s)
}

/** Signal strength bars (ping ke hisab se) — Green VPN jaisa look. */
@Composable
private fun SignalBars(pingMs: Int, modifier: Modifier = Modifier) {
    val filled = when {
        pingMs <= 0 -> 1
        pingMs < 120 -> 4
        pingMs < 220 -> 3
        pingMs < 400 -> 2
        else -> 1
    }
    Row(
        modifier,
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        repeat(4) { i ->
            Box(
                Modifier
                    .width(4.dp)
                    .height((7 + i * 5).dp)
                    .background(
                        if (i < filled) Color(0xFF00C853) else MaterialTheme.colorScheme.outline,
                        RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}

/** "Auto · Fastest" card — sab se tez server par foran connect. */
@Composable
private fun AutoFastestCard(
    onClick: () -> Unit,
    findingBest: Boolean,
    enabled: Boolean
) {
    Card(
        onClick = onClick,
        enabled = !findingBest && enabled,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.5.dp, Color(0xFF00BFA5)),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0D47A1)),
                contentAlignment = Alignment.Center
            ) {
                Text("⚡", fontSize = 22.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    tr("autoFastest"),
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    tr("quickConnect"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (findingBest) {
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.5.dp)
            } else {
                Text("⌄", fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun RaceCard(race: RaceState) {
    when (race) {
        is RaceState.Idle -> Unit
        is RaceState.Running -> {
            Card(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
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
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
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

/** Country card (screenshot jaisa): flag + "Name · FREE" + signal + star. Tap -> servers. */
@Composable
private fun CountryCard(
    group: CountryGroup,
    isFavorite: Boolean,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val bestPing = group.servers.minOfOrNull { it.pingMs } ?: 0
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(flagEmoji(group.iso), fontSize = 30.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "${group.name} · ${tr("freeLabel")}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    "${group.servers.size} ${tr("serversLabel")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            SignalBars(bestPing)
            Spacer(Modifier.width(10.dp))
            Text(
                text = if (isFavorite) "★" else "☆",
                fontSize = 24.sp,
                color = if (isFavorite) Color(0xFFFFB300) else MaterialTheme.colorScheme.outline,
                modifier = Modifier.clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onToggleFavorite
                )
            )
        }
    }
}

/** Country ke andar: back button + country header. */
@Composable
private fun CountryDetailBar(group: CountryGroup, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onBack) { Text("‹ ${tr("tabFree")}") }
        Text(
            "${flagEmoji(group.iso)}  ${group.name}",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.weight(1f)
        )
        Text(
            "${group.servers.size} ${tr("serversLabel")}",
            style = MaterialTheme.typography.bodySmall
        )
    }
    HorizontalDivider()
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ServerRow(
    server: VpnServer,
    isSelected: Boolean,
    isConnected: Boolean,
    reachable: Boolean?,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    // reachable == false -> is network par blocked/dead: dimmed. null = abhi check nahi hua.
    val dead = reachable == false
    val dotColor = when (reachable) {
        true -> Color(0xFF2E7D32)
        false -> Color(0xFFC62828)
        null -> Color(0xFF9E9E9E)
    }
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = if (isConnected) BorderStroke(1.5.dp, Color(0xFF00C853)) else null,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .alpha(if (dead) 0.5f else 1f)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(10.dp).background(dotColor, CircleShape))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    server.hostName,
                    fontWeight = if (isConnected) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = 15.sp
                )
                Text(
                    "${server.ip} • ${server.pingMs} ms • ${formatSpeed(server.speedBps)} • ${tr("sharedIp")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            SignalBars(server.pingMs)
            Spacer(Modifier.width(8.dp))
            when {
                isConnected -> Text("✓", color = Color(0xFF00C853), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                isSelected -> Text("›", fontSize = 22.sp, color = Color(0xFF00BFA5))
            }
        }
    }
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
            Modifier.fillMaxWidth().padding(12.dp),
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
                Text(tr("privateEmpty"))
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
                    HorizontalDivider()
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
            topBar = {
                TopAppBar(
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

private fun formatUptime(sec: Long): String = when {
    sec < 3600 -> "${sec / 60}m"
    sec < 86400 -> "${sec / 3600}h"
    else -> "${sec / 86400}d"
}
