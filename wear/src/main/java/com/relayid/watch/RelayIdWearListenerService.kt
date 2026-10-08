package com.relayid.watch

import android.content.Context
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService

class RelayIdWearListenerService : WearableListenerService() {
    override fun onDataChanged(events: DataEventBuffer) {
        events.use { buffer ->
            buffer.forEach { event ->
                if (event.type != DataEvent.TYPE_CHANGED || event.dataItem.uri.path != "/relayid/state") return@forEach

                val payload = DataMapItem.fromDataItem(event.dataItem).dataMap.getString("payload") ?: return@forEach
                getSharedPreferences("relayid_wear", Context.MODE_PRIVATE)
                    .edit()
                    .putString("state", payload)
                    .apply()
            }
        }
    }
}
