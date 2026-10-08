package com.relayid.watch

import org.json.JSONObject

data class RelayChoice(
    val id: String,
    val name: String,
    val email: String,
    val host: String,
)

data class WatchState(
    val relays: List<RelayChoice> = emptyList(),
    val selectedRelay: RelayChoice? = null,
    val code: String? = null,
    val codeGroups: Int = 2,
    val expiresInSeconds: Int? = null,
) {
    companion object
}

fun WatchState.Companion.fromPayload(payload: String): WatchState? {
    return runCatching {
        val root = JSONObject(payload)
        val relaysJson = root.optJSONArray("relays") ?: return null
        val relays = buildList {
            for (index in 0 until relaysJson.length()) {
                val relay = relaysJson.getJSONObject(index)
                add(
                    RelayChoice(
                        id = relay.getString("id"),
                        name = relay.optString("name", "Relay"),
                        email = relay.getString("email"),
                        host = relay.getString("host"),
                    ),
                )
            }
        }
        val selectedId = root.optString("selected_id").takeIf { it.isNotBlank() }
        val selectedRelay = relays.firstOrNull { it.id == selectedId }
        WatchState(
            relays = relays,
            selectedRelay = selectedRelay,
            code = root.optString("code").takeIf { it.isNotBlank() },
            codeGroups = root.optInt("groups", 2),
            expiresInSeconds = root.optInt("expires_in", -1).takeIf { it >= 0 },
        )
    }.getOrNull()
}

fun formatCode(code: String?, groups: Int): String {
    if (code == null) return placeholderCode(groups)

    val normalized = code.filter { it.isLetterOrDigit() }
    val groupCount = groups.coerceIn(2, 4)

    return normalized
        .take(groupCount * 4)
        .chunked(4)
        .joinToString("-")
}

fun placeholderCode(groups: Int): String {
    return List(groups.coerceIn(2, 4)) { "----" }.joinToString("-")
}
