package com.relayid.watch

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.relayid.watch.ui.RelayIdWatchApp
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 4001)
        }
        // Always open on the relay picker so a stale code screen is never restored.
        WearTransport.setSelectedRelay(this, null)

        setContent {
            val context = LocalContext.current
            var state by remember { mutableStateOf(WearTransport.readState(context) ?: WatchState()) }

            DisposableEffect(context) {
                val listener = WearTransport.listen(context) { incoming -> state = incoming }
                val codeListener = WearTransport.listenForCode(context, onError = { message ->
                    state = state.copy(errorMessage = message)
                }) { _, code, groups, expiresIn ->
                    state = (WearTransport.readState(context) ?: state).copy(
                        code = code,
                        codeGroups = groups,
                        expiresInSeconds = expiresIn,
                        errorMessage = null,
                    )
                }
                onDispose {
                    WearTransport.stopListening(context, listener)
                    WearTransport.stopListeningForCode(context, codeListener)
                }
            }

            LaunchedEffect(Unit) {
                WearTransport.sync(context) { incoming -> state = incoming }
            }

            // Request a fresh code whenever an explicitly selected relay enters
            // the code screen, including after the watch app is reopened.
            LaunchedEffect(state.selectedRelay?.id) {
                state.selectedRelay?.let { WearTransport.requestCode(context, it.id) }
            }

            LaunchedEffect(state.selectedRelay?.id, state.code, state.expiresInSeconds) {
                val expiresInSeconds = state.expiresInSeconds ?: return@LaunchedEffect
                if (expiresInSeconds <= 0) {
                    state.selectedRelay?.let { WearTransport.requestCode(context, it.id) }
                    return@LaunchedEffect
                }

                delay(1000)
                state = state.copy(expiresInSeconds = expiresInSeconds - 1)
            }

            RelayIdWatchApp(
                state = state,
                onSelectRelay = { relay ->
                    state = state.copy(
                        selectedRelay = relay,
                        code = null,
                        expiresInSeconds = null,
                        errorMessage = null,
                    )
                    WearTransport.setSelectedRelay(context, relay?.id)
                },
                onRequestCode = {
                    state = state.copy(errorMessage = null)
                    state.selectedRelay?.let { WearTransport.requestCode(context, it.id) }
                },
            )
        }
    }
}
