package com.relayid.watch

import android.content.Context
import android.net.Uri
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable

object WearTransport {
    private const val PREFERENCES = "relayid_wear"
    private const val STATE_KEY = "state"
    private const val SELECTED_RELAY_KEY = "selected_relay_id"
    private const val STATE_PATH = "/relayid/state"
    private const val REQUEST_PATH = "/relayid/request-code"
    private const val RESPONSE_PATH = "/relayid/code-response"

    fun readState(context: Context): WatchState? {
        val payload = context
            .getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .getString(STATE_KEY, null)
            ?: return null

        return stateFromPayload(context, payload)
    }

    fun setSelectedRelay(context: Context, relayId: String?) {
        context
            .getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .edit()
            .putString(SELECTED_RELAY_KEY, relayId)
            .apply()
    }

    private fun stateFromPayload(context: Context, payload: String): WatchState? {
        val parsed = WatchState.fromPayload(payload) ?: return null
        val selectedId = context
            .getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .getString(SELECTED_RELAY_KEY, null)
        return parsed.copy(selectedRelay = parsed.relays.firstOrNull { it.id == selectedId })
    }

    fun listen(context: Context, onState: (WatchState) -> Unit): DataClient.OnDataChangedListener {
        val listener = DataClient.OnDataChangedListener { events ->
            events.use { buffer ->
                buffer.forEach { event ->
                    if (event.type != DataEvent.TYPE_CHANGED || event.dataItem.uri.path != STATE_PATH) return@forEach

                    val payload = DataMapItem.fromDataItem(event.dataItem).dataMap.getString("payload") ?: return@forEach
                    context
                        .getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                        .edit()
                        .putString(STATE_KEY, payload)
                        .apply()
                    stateFromPayload(context, payload)?.let(onState)
                }
            }
        }
        Wearable.getDataClient(context).addListener(listener)
        return listener
    }

    fun sync(context: Context, onState: (WatchState) -> Unit) {
        Wearable.getDataClient(context)
            .getDataItems(Uri.parse("wear://*/relayid/state"))
            .addOnSuccessListener { items ->
                items.use { buffer ->
                    buffer.firstOrNull()?.let { dataItem ->
                        val payload = DataMapItem.fromDataItem(dataItem).dataMap.getString("payload") ?: return@let
                        context
                            .getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                            .edit()
                            .putString(STATE_KEY, payload)
                            .apply()
                        stateFromPayload(context, payload)?.let(onState)
                    }
                }
            }
    }

    fun stopListening(context: Context, listener: DataClient.OnDataChangedListener) {
        Wearable.getDataClient(context).removeListener(listener)
    }

    fun listenForCode(
        context: Context,
        onError: (String) -> Unit,
        onCode: (relayId: String, code: String, groups: Int, expiresIn: Int) -> Unit,
    ): MessageClient.OnMessageReceivedListener {
        val listener = MessageClient.OnMessageReceivedListener { event: MessageEvent ->
            if (event.path != RESPONSE_PATH) return@OnMessageReceivedListener

            runCatching {
                val response = org.json.JSONObject(event.data.toString(Charsets.UTF_8))
                val relayId = response.getString("relay_id")
                response.optString("error").takeIf { it.isNotBlank() }?.let {
                    onError(it)
                    return@runCatching
                }
                val code = response.getString("code")
                val groups = response.getInt("groups")
                val expiresIn = response.getInt("expires_in")
                val current = readState(context) ?: WatchState()
                val updatedPayload = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                    .getString(STATE_KEY, null)
                    ?.let { org.json.JSONObject(it) }
                    ?.put("code", code)
                    ?.put("groups", groups)
                    ?.put("expires_in", expiresIn)
                    ?.toString()
                if (updatedPayload != null) {
                    context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                        .edit()
                        .putString(STATE_KEY, updatedPayload)
                        .apply()
                }
                if (current.selectedRelay?.id == relayId) onCode(relayId, code, groups, expiresIn)
            }
        }
        Wearable.getMessageClient(context).addListener(listener)
        return listener
    }

    fun stopListeningForCode(context: Context, listener: MessageClient.OnMessageReceivedListener) {
        Wearable.getMessageClient(context).removeListener(listener)
    }

    fun requestCode(context: Context, relayId: String) {
        Wearable.getNodeClient(context).connectedNodes.addOnSuccessListener { nodes ->
            nodes.forEach { node ->
                Wearable.getMessageClient(context).sendMessage(
                    node.id,
                    REQUEST_PATH,
                    relayId.toByteArray(Charsets.UTF_8),
                )
            }
        }
    }
}
