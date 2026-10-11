package com.raftar.vpn

import android.Manifest
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.raftar.vpn.data.PrivateServer
import com.raftar.vpn.ui.App
import com.raftar.vpn.ui.MainViewModel
import com.raftar.vpn.ui.Strings
import com.raftar.vpn.vpn.VpnConnector

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    /** VPN permission milne ke baad jo action chalana ho (connect / best / gaming / private). */
    private var pendingAfterPermission: (() -> Unit)? = null

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            pendingAfterPermission?.invoke()
        } else {
            viewModel.onPermissionDenied()
        }
        pendingAfterPermission = null
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* optional permission; deny par bhi app chalti hai */ }

    private val filePicker = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) viewModel.importPrivateFile(uri)
    }

    private val qrScanner = registerForActivityResult(ScanContract()) { result ->
        val contents = result.contents
        if (!contents.isNullOrBlank()) {
            viewModel.importPrivateText(contents, "QR Server")
        }
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            qrScanner.launch(
                ScanOptions()
                    .setPrompt(Strings.t("qrPrompt"))
                    .setBeepEnabled(false)
                    .setOrientationLocked(false)
            )
        } else {
            viewModel.postNotice(Strings.t("cameraDenied"))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Android 13+: VPN foreground-service notification ke liye runtime permission.
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            App(
                viewModel = viewModel,
                onConnectClick = ::handleConnectClick,
                onBestServerClick = {
                    withVpnPermission { viewModel.requestConnect { viewModel.autoConnectBest() } }
                },
                onGamingClick = {
                    withVpnPermission { viewModel.requestConnect { viewModel.startGamingMode() } }
                },
                onServerClick = { server ->
                    withVpnPermission { viewModel.requestConnect { viewModel.connectTo(server) } }
                },
                onPrivateConnect = { ps: PrivateServer ->
                    withVpnPermission { viewModel.requestConnect { viewModel.connectPrivate(ps) } }
                },
                onImportFileClick = { filePicker.launch("*/*") },
                onScanQrClick = ::handleQrScanClick
            )
        }
    }

    /** VpnService.prepare() null = permission pehle se granted, warna system dialog. */
    private fun withVpnPermission(action: () -> Unit) {
        if (viewModel.connector.prepare()) {
            action()
        } else {
            pendingAfterPermission = action
            VpnService.prepare(this)?.let { vpnPermissionLauncher.launch(it) }
        }
    }

    private fun handleConnectClick() {
        when (viewModel.connector.state.value) {
            is VpnConnector.VpnState.Connected,
            is VpnConnector.VpnState.Connecting -> viewModel.connector.disconnect()
            else -> withVpnPermission { viewModel.requestConnect { viewModel.userStartConnect() } }
        }
    }

    private fun handleQrScanClick() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            qrScanner.launch(
                ScanOptions()
                    .setPrompt(Strings.t("qrPrompt"))
                    .setBeepEnabled(false)
                    .setOrientationLocked(false)
            )
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
}
