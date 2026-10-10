package com.relayid.watch

import android.content.Context
import android.content.Intent
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.os.Build
import android.util.Log
import java.io.File
import kotlin.concurrent.thread
import androidx.core.content.FileProvider
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.Asset
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
                    "/relayid/install-watch" -> {
                        val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                        val targetNodeId = dataMap.getString("target_node_id") ?: return@forEach
                        val localNodeId = Tasks.await(Wearable.getNodeClient(this).localNode).id
                        Log.i("RelayIdWatch", "Installer target=$targetNodeId local=$localNodeId")
                        if (targetNodeId != localNodeId) {
                            Log.i("RelayIdWatch", "Ignoring installer for another watch")
                            return@forEach
                        }
                        val asset = dataMap.getAsset("apk") ?: return@forEach
                        val phoneNodeId = event.dataItem.uri.host
                        Log.i("RelayIdWatch", "Installer asset received; downloading")
                        thread(name = "relayid-watch-installer") { installApk(asset, phoneNodeId) }
                    }
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

    private fun installApk(asset: Asset, phoneNodeId: String?) {
        runCatching {
            val response = Tasks.await(Wearable.getDataClient(this).getFdForAsset(asset))
            val apk = File(cacheDir, "relayid-watch.apk")
            response.getInputStream().use { input ->
                apk.outputStream().use { output -> input.copyTo(output) }
            }
            check(apk.length() > 0) { "Downloaded Watch APK is empty" }

            val uri = FileProvider.getUriForFile(this, "com.relayid.app.relayid-files", apk)
            Log.i("RelayIdWatch", "APK saved path=${apk.absolutePath} bytes=${apk.length()} uri=$uri")
            getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                .edit()
                .putString(INSTALL_URI, uri.toString())
                .putString(INSTALL_PHONE_NODE, phoneNodeId)
                .apply()
            showInstallerNotification()
            Log.i("RelayIdWatch", "Installer notification posted for $uri")
            if (!phoneNodeId.isNullOrBlank()) {
                Wearable.getMessageClient(this).sendMessage(
                    phoneNodeId,
                    "/relayid/install-watch-status",
                    "ready".toByteArray(Charsets.UTF_8),
                )
            }
        }.onFailure { error ->
            Log.e("RelayIdWatch", "Could not prepare Watch installer", error)
        }
    }

    private fun showInstallerNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    INSTALL_CHANNEL,
                    "RelayId updates",
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = "RelayId Watch installation prompts"
                },
            )
        }

        val openInstaller = PendingIntent.getActivity(
            this,
            INSTALL_NOTIFICATION_ID,
            Intent(this, InstallWatchActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = android.app.Notification.Builder(this, INSTALL_CHANNEL)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("RelayId Watch update ready")
            .setContentText("Tap to open the installer")
            .setContentIntent(openInstaller)
            .setAutoCancel(true)
            .setCategory(android.app.Notification.CATEGORY_PROGRESS)
            .build()
        manager.notify(INSTALL_NOTIFICATION_ID, notification)
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
private const val INSTALL_URI = "install_uri"
private const val INSTALL_PHONE_NODE = "install_phone_node"
private const val INSTALL_CHANNEL = "relayid-install"
private const val INSTALL_NOTIFICATION_ID = 1801
