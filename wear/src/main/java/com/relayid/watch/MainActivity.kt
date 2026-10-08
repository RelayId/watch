package com.relayid.watch

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

        setContent {
            val context = LocalContext.current
            var state by remember { mutableStateOf(WearTransport.readState(context) ?: WatchState()) }

            DisposableEffect(context) {
                val listener = WearTransport.listen(context) { incoming -> state = incoming }
                onDispose { WearTransport.stopListening(context, listener) }
            }

            LaunchedEffect(Unit) {
                WearTransport.sync(context) { incoming -> state = incoming }
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
                    )
                    relay?.let { WearTransport.requestCode(context, it.id) }
                },
                onRequestCode = {
                    state.selectedRelay?.let { WearTransport.requestCode(context, it.id) }
                },
            )
        }
    }
}
