package com.raftar.vpn.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(tr("appName"), fontWeight = FontWeight.Bold) },
                    actions = {
                        TextButton(onClick = { viewModel.refresh() }) { Text(tr("refresh")) }
                    }
                )
            },
            snackbarHost = { SnackbarHost(snackbar) }
        ) { pad ->
            Column(Modifier.padding(pad).fillMaxSize()) {
                HeroCard(
                    vpnState = vpnState,
                    selected = selected,
                    attemptInfo = attemptInfo,
                    gaming = gaming,
                    gamePing = gamePing,
                    onConnectClick = onConnectClick
                )

                QuickActions(
                    onBestServerClick = onBestServerClick,
                    onGamingClick = onGamingClick,
                    findingBest = findingBest,
                    gaming = gaming,
                    gamePing = gamePing,
                    enabled = ranked.isNotEmpty()
                )

                RaceCard(race = race)

                if (probing) {
                    Text(
                        tr("probing"),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                TabRow(selectedTabIndex = tab) {
                    Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(tr("tabFree")) })
                    Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(tr("tabPrivate")) })
                }

                if (tab == 0) {
                    if (activeGroup == null) {
                        OutlinedTextField(
                            value = query,
                            onValueChange = viewModel::setQuery,
                            label = { Text(tr("searchHint")) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().padding(12.dp)
                        )
                        if (loading && ranked.isEmpty()) {
                            Box(
                                Modifier.fillMaxWidth().padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        } else if (groups.isEmpty()) {
                            Box(
                                Modifier.fillMaxWidth().padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(tr("noResults"))
                            }
                        } else {
                            LazyColumn(Modifier.weight(1f)) {
                                items(groups, key = { it.name }) { g ->
                                    CountryRow(
                                        group = g,
                                        onClick = { openCountry = g.name }
                                    )
                                }
                            }
                        }
                        Text(
                            "${tr("sharedIpNote")} ${tr("reportHint")}.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(8.dp)
                        )
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

/** Bara hero card: status + gol connect button (dusri VPN apps ki tarah). */
@Composable
private fun HeroCard(
    vpnState: VpnConnector.VpnState,
    selected: VpnServer?,
    attemptInfo: String?,
    gaming: Boolean,
    gamePing: Long?,
    onConnectClick: () -> Unit
) {
    val connected = vpnState is VpnConnector.VpnState.Connected
    val connecting = vpnState == VpnConnector.VpnState.Connecting
    val failed = vpnState is VpnConnector.VpnState.Failed

    val statusText = when (vpnState) {
        VpnConnector.VpnState.Idle -> tr("statusDisconnected")
        VpnConnector.VpnState.Connecting -> attemptInfo ?: tr("statusConnecting")
        is VpnConnector.VpnState.Connected -> {
            val s = vpnState.server
            val where = s.countryLong.ifBlank { tr("tabPrivate") }
            "${tr("connectedTo")} • ${flagEmoji(s.countryShort)} $where"
        }
        is VpnConnector.VpnState.Failed -> tr("statusFailed")
        VpnConnector.VpnState.Disconnected -> tr("statusDisconnected")
    }
    val buttonLabel = when {
        connected -> tr("disconnect")
        connecting -> tr("cancel")
        failed -> tr("retry")
        else -> tr("connect")
    }
    val buttonColor = when {
        connected -> Color(0xFFE53935)
        else -> colorResource(R.color.raftar_teal)
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (connected) colorResource(R.color.raftar_dark)
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            Modifier.padding(20.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                statusText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (connected) Color.White else Color.Unspecified,
                textAlign = TextAlign.Center
            )
            if (!connected && !connecting && selected != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "${flagEmoji(selected.countryShort)} ${selected.countryLong} • ${selected.pingMs} ms",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (connected) Color.White else Color.Unspecified
                )
            }
            if (gaming && connected) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "${tr("gamingMode")} • ${tr("gamingPing")}: ${gamePing?.let { "$it ms" } ?: "…"}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(R.color.raftar_teal)
                )
            }
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .size(150.dp)
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
                    Text("⏻", fontSize = 56.sp, color = Color.White)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                buttonLabel.uppercase(),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall,
                color = if (connected) Color.White else Color.Unspecified
            )
        }
    }
}

/** Best Server + Gaming Mode — side by side. */
@Composable
private fun QuickActions(
    onBestServerClick: () -> Unit,
    onGamingClick: () -> Unit,
    findingBest: Boolean,
    gaming: Boolean,
    gamePing: Long?,
    enabled: Boolean
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onBestServerClick,
            enabled = !findingBest && enabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = colorResource(R.color.raftar_teal)
            ),
            modifier = Modifier.weight(1f)
        ) {
            if (findingBest) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text(tr("findingBest"))
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(tr("bestServer"), fontWeight = FontWeight.Bold)
                    Text(tr("bestServerSub"), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        OutlinedButton(
            onClick = onGamingClick,
            enabled = !findingBest && enabled,
            modifier = Modifier.weight(1f)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (gaming) "${tr("gamingMode")} • ${gamePing?.let { "$it ms" } ?: "…"}"
                    else tr("gamingMode"),
                    fontWeight = FontWeight.Bold
                )
                Text(tr("gamingModeSub"), style = MaterialTheme.typography.bodySmall)
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

/** Country row: flag + naam + servers count + best ping. Tap -> servers khulte hain. */
@Composable
private fun CountryRow(group: CountryGroup, onClick: () -> Unit) {
    val bestPing = group.servers.minOfOrNull { it.pingMs } ?: 0
    ListItem(
        headlineContent = {
            Text(
                "${flagEmoji(group.iso)}  ${group.name}",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        },
        supportingContent = {
            Text("${group.servers.size} ${tr("serversLabel")} • ${tr("bestPingLabel")} $bestPing ms")
        },
        trailingContent = {
            Text("›", fontSize = 24.sp, color = MaterialTheme.colorScheme.primary)
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
    HorizontalDivider()
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
    ListItem(
        headlineContent = {
            Text(
                "${server.hostName} • ${server.ip}",
                fontWeight = if (isConnected) FontWeight.Bold else FontWeight.Normal
            )
        },
        supportingContent = {
            Text(
                "${tr("gamingPing")} ${server.pingMs} ms • ${formatSpeed(server.speedBps)} • " +
                    "${server.sessions} users • up ${formatUptime(server.uptimeSec)} • ${tr("sharedIp")}"
            )
        },
        leadingContent = {
            Box(Modifier.size(10.dp).background(dotColor, CircleShape))
        },
        trailingContent = {
            when {
                isConnected -> Text("✓", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                isSelected -> Text("›", fontSize = 22.sp, color = MaterialTheme.colorScheme.primary)
            }
        },
        modifier = Modifier
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .alpha(if (dead) 0.45f else 1f)
    )
    HorizontalDivider()
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
    MaterialTheme {
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
