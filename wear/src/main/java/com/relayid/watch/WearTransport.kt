package com.relayid.watch

import android.content.Context
import android.net.Uri
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable

object WearTransport {
    private const val PREFERENCES = "relayid_wear"
    private const val STATE_KEY = "state"
    private const val STATE_PATH = "/relayid/state"
    private const val REQUEST_PATH = "/relayid/request-code"

    fun readState(context: Context): WatchState? {
        val payload = context
            .getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .getString(STATE_KEY, null)
            ?: return null

        return WatchState.fromPayload(payload)
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
                    WatchState.fromPayload(payload)?.let(onState)
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
                        WatchState.fromPayload(payload)?.let(onState)
                    }
                }
            }
    }

    fun stopListening(context: Context, listener: DataClient.OnDataChangedListener) {
        Wearable.getDataClient(context).removeListener(listener)
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
