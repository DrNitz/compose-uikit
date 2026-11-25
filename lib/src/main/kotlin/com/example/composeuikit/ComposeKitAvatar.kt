package com.example.composeuikit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Circular avatar built from initials, with an optional presence dot.
 * Deliberately no image loading: pass a name and it works offline.
 *
 * @param name source for the initials, e.g. `"Ada Lovelace"` -> `"AL"`.
 * @param size overall diameter.
 * @param online draws the green presence dot when true.
 *
 * ```kotlin
 * ComposeKitAvatar(name = "Ada Lovelace", size = 48.dp, online = true)
 * ```
 */
@Composable
fun ComposeKitAvatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    online: Boolean = false,
) {
    val initials = remember(name) {
        name.trim()
            .split(Regex("\\s+"))
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .take(2)
            .joinToString("")
            .ifEmpty { "?" }
    }

    Box(modifier = modifier.size(size)) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.tertiaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = initials,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }

        if (online) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(size / 3)
                    .clip(CircleShape)
                    .background(Color(0xFF22C55E))
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ComposeKitAvatarPreview() {
    MaterialTheme {
        Box(Modifier.padding(16.dp)) {
            ComposeKitAvatar(name = "Ada Lovelace", size = 56.dp, online = true)
        }
    }
}
