package com.relayid.watch

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.google.android.gms.wearable.Wearable

class InstallWatchActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val preferences = getSharedPreferences("relayid_wear", MODE_PRIVATE)
        val uri = preferences.getString("install_uri", null)?.let(Uri::parse)
        val phoneNodeId = preferences.getString("install_phone_node", null)
        if (uri == null) {
            finish()
            return
        }

        val installer = Intent("android.intent.action.INSTALL_PACKAGE").apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            clipData = android.content.ClipData.newRawUri("RelayId Watch", uri)
        }
        startActivity(installer)
        if (!phoneNodeId.isNullOrBlank()) {
            Wearable.getMessageClient(this).sendMessage(
                phoneNodeId,
                "/relayid/install-watch-status",
                "opened".toByteArray(Charsets.UTF_8),
            )
        }
        finish()
    }
}
