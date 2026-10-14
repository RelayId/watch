package com.relayid.watch.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.FilledIconButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.relayid.watch.R
import com.relayid.watch.RelayChoice
import com.relayid.watch.WatchState
import com.relayid.watch.formatCode

private data class WatchPalette(
    val background: Color,
    val surface: Color,
    val soft: Color,
    val border: Color,
    val text: Color,
    val muted: Color,
    val primary: Color,
    val primaryText: Color,
    val logo: Int,
)

private val DarkPalette = WatchPalette(
    background = Color(0xFF09090B),
    surface = Color(0xFF18181B),
    soft = Color(0xFF27272A),
    border = Color(0xFF3F3F46),
    text = Color(0xFFFAFAFA),
    muted = Color(0xFFA1A1AA),
    primary = Color(0xFFFAFAFA),
    primaryText = Color(0xFF18181B),
    logo = R.drawable.logo_light,
)

private val LightPalette = WatchPalette(
    background = Color(0xFFFAFAFA),
    surface = Color(0xFFE4E4E7),
    soft = Color(0xFFD4D4D8),
    border = Color(0xFFD4D4D8),
    text = Color(0xFF18181B),
    muted = Color(0xFF71717A),
    primary = Color(0xFF18181B),
    primaryText = Color(0xFFFFFFFF),
    logo = R.drawable.logo_dark,
)

private val ErrorColor = Color(0xFFFCA5A5)

@Composable
fun RelayIdWatchApp(
    state: WatchState,
    demoMode: Boolean,
    onDemoTap: () -> Unit,
    onSelectRelay: (RelayChoice?) -> Unit,
    onRequestCode: () -> Unit,
) {
    val palette = if (isSystemInDarkTheme()) DarkPalette else LightPalette
    val listState = rememberScalingLazyListState()
    val colorScheme = MaterialTheme.colorScheme.copy(
        background = palette.background,
        onBackground = palette.text,
        surfaceContainerLow = palette.surface,
        surfaceContainer = palette.surface,
        surfaceContainerHigh = palette.surface,
        onSurface = palette.text,
        onSurfaceVariant = palette.muted,
        primary = palette.primary,
        onPrimary = palette.primaryText,
        outline = palette.border,
        outlineVariant = palette.border,
    )

    MaterialTheme(colorScheme = colorScheme) {
        LaunchedEffect(state.selectedRelay) {
            listState.animateScrollToItem(if (state.selectedRelay == null) 0 else 2)
        }

        BackHandler(enabled = state.selectedRelay != null) {
            onSelectRelay(null)
        }

        ScalingLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.background),
            state = listState,
            contentPadding = PaddingValues(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item {
                Header(palette, onDemoTap)
            }

            if (state.selectedRelay == null) {
                item {
                    RelayPicker(
                        relays = state.relays,
                        onSelectRelay = onSelectRelay,
                        palette = palette,
                    )
                }
            } else {
                item {
                    CodeHeader(relay = state.selectedRelay, palette = palette)
                }
                item {
                    CodeCard(
                        code = state.code,
                        codeGroups = state.codeGroups,
                        expiresInSeconds = state.expiresInSeconds,
                        onRequestCode = onRequestCode,
                        palette = palette,
                    )
                }
                state.errorMessage?.let { message ->
                    item {
                        Card(
                            onClick = onRequestCode,
                            modifier = Modifier.padding(horizontal = 12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = palette.surface,
                                contentColor = ErrorColor,
                            ),
                        ) {
                            Text(
                                text = message,
                                modifier = Modifier.fillMaxWidth(),
                                color = ErrorColor,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        FilledIconButton(
                            onClick = { onSelectRelay(null) },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = palette.primary,
                                contentColor = palette.primaryText,
                            ),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "Back",
                            )
                        }

                        FilledIconButton(
                            onClick = onRequestCode,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = palette.primary,
                                contentColor = palette.primaryText,
                            ),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = "Regenerate code",
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(palette: WatchPalette, onDemoTap: () -> Unit) {
    Row(
        modifier = Modifier.clickable(onClick = onDemoTap),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Image(
            painter = painterResource(
                palette.logo,
            ),
            contentDescription = "RelayId",
            modifier = Modifier
                .height(32.dp)
                .clip(CircleShape),
        )

        Text(
            text = "RelayId",
            color = palette.text,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun RelayPicker(
    relays: List<RelayChoice>,
    onSelectRelay: (RelayChoice?) -> Unit,
    palette: WatchPalette,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (relays.isEmpty()) {
            Text(
                text = "Unlock RelayId on your phone",
                color = palette.muted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )
        }

        relays.forEach { relay ->
            Card(
                onClick = { onSelectRelay(relay) },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = palette.soft,
                    contentColor = palette.text,
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = relay.email,
                            color = palette.text,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        RelayDetail(text = relay.host, palette = palette)
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(palette.soft, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                            contentDescription = "Select Relay",
                            tint = palette.text,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CodeHeader(relay: RelayChoice, palette: WatchPalette) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = relay.email,
            color = palette.text,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )

        Text(text = relay.host, color = palette.muted, fontSize = 11.sp)
    }
}

@Composable
private fun CodeCard(
    code: String?,
    codeGroups: Int,
    expiresInSeconds: Int?,
    onRequestCode: () -> Unit,
    palette: WatchPalette,
) {
    Card(
        onClick = onRequestCode,
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .height(88.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = formatCode(code, codeGroups),
                modifier = Modifier.fillMaxWidth(),
                color = palette.text,
                fontSize = if (codeGroups >= 4) 21.sp else 30.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = if (codeGroups >= 4) 1.sp else 2.sp,
                textAlign = TextAlign.Center,
            )

            if (expiresInSeconds != null) {
                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Expires in ${expiresInSeconds}s",
                    modifier = Modifier.fillMaxWidth(),
                    color = palette.muted,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun RelayDetail(text: String, palette: WatchPalette) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(text = "•", color = palette.muted, fontSize = 11.sp)
        Text(text = text, color = palette.muted, fontSize = 11.sp)
    }
}
