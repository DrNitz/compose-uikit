package com.example.composeuikit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Self-updating clock. Renders the device time and refreshes itself on a ticker,
 * so it keeps counting without the caller driving recomposition.
 *
 * @param showSeconds tick once per second and show `HH:mm:ss`; otherwise tick every 10s and show `HH:mm`.
 * @param use24Hour `true` for `HH:mm`, `false` for `hh:mm a`.
 *
 * ```kotlin
 * ComposeKitClock(showSeconds = true)
 * ```
 */
@Composable
fun ComposeKitClock(
    modifier: Modifier = Modifier,
    showSeconds: Boolean = true,
    use24Hour: Boolean = true,
    label: String? = null,
) {
    val pattern = buildString {
        append(if (use24Hour) "HH" else "hh")
        append(":mm")
        if (showSeconds) append(":ss")
        if (!use24Hour) append(" a")
    }
    val formatter = remember(pattern) { SimpleDateFormat(pattern, Locale.getDefault()) }

    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(showSeconds) {
        while (true) {
            now = Date()
            delay(if (showSeconds) 1_000L else 10_000L)
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (label != null) {
            Text(
                text = label.uppercase(Locale.getDefault()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = formatter.format(now),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/**
 * Compact variant suited to a toolbar or status strip: one line, no label, seconds on.
 *
 * ```kotlin
 * ComposeKitClockPill()
 * ```
 */
@Composable
fun ComposeKitClockPill(
    modifier: Modifier = Modifier,
    use24Hour: Boolean = true,
) {
    val formatter = remember(use24Hour) {
        SimpleDateFormat(if (use24Hour) "HH:mm:ss" else "hh:mm:ss a", Locale.getDefault())
    }
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Date()
            delay(1_000L)
        }
    }
    Text(
        text = formatter.format(now),
        style = MaterialTheme.typography.labelLarge,
        modifier = modifier
            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        color = MaterialTheme.colorScheme.onSecondaryContainer,
    )
}

@Preview(showBackground = true)
@Composable
private fun ComposeKitClockPreview() {
    MaterialTheme {
        Column(Modifier.padding(16.dp)) {
            ComposeKitClock(label = "Local time", showSeconds = true)
            ComposeKitClockPill()
        }
    }
}
