package com.relayid.watch

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import org.json.JSONObject

class RelayIdWearListenerService : WearableListenerService() {
    override fun onDataChanged(events: DataEventBuffer) {
        events.use { buffer ->
            Log.i("RelayIdWatch", "DataLayer changed count=${buffer.count}")
            buffer.forEach { event ->
                if (event.type != DataEvent.TYPE_CHANGED) return@forEach
                Log.i("RelayIdWatch", "DataItem changed path=${event.dataItem.uri.path}")
                when (event.dataItem.uri.path) {
                    "/relayid/state" -> {
                        val payload = DataMapItem.fromDataItem(event.dataItem).dataMap.getString("payload") ?: return@forEach
                        getSharedPreferences("relayid_wear", Context.MODE_PRIVATE)
                            .edit()
                            .putString("state", payload)
                            .apply()
                    }
                }
            }
        }
    }

    override fun onMessageReceived(event: MessageEvent) {
        if (event.path != "/relayid/code-response") return

        runCatching {
            val response = JSONObject(event.data.toString(Charsets.UTF_8))
            val preferences = getSharedPreferences("relayid_wear", Context.MODE_PRIVATE)
            val statePayload = preferences.getString("state", null) ?: return
            val state = JSONObject(statePayload)
            if (state.optString("selected_id") != response.getString("relay_id")) return

            val updated = state
                .put("code", response.getString("code"))
                .put("groups", response.getInt("groups"))
                .put("expires_in", response.getInt("expires_in"))
                .toString()
            preferences.edit().putString("state", updated).apply()

        }
    }

}

private const val PREFERENCES = "relayid_wear"
