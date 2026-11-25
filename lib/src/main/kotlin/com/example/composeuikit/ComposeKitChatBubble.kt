package com.example.composeuikit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * A single chat message. Own messages align right and use the container colour,
 * incoming ones align left, so bubble tails point the right way.
 *
 * @param message body text.
 * @param isMine `true` for messages the local user sent.
 * @param timestamp small label under the bubble, hidden when null.
 *
 * ```kotlin
 * ComposeKitChatBubble(message = "On my way", isMine = true, timestamp = "09:41")
 * ```
 */
@Composable
fun ComposeKitChatBubble(
    message: String,
    modifier: Modifier = Modifier,
    isMine: Boolean = false,
    timestamp: String? = null,
) {
    val bubbleColor = if (isMine) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val textColor = if (isMine) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }
    val shape = if (isMine) {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
    ) {
        Column(horizontalAlignment = if (isMine) Alignment.End else Alignment.Start) {
            Surface(
                color = bubbleColor,
                shape = shape,
                modifier = Modifier.widthIn(max = 280.dp),
            ) {
                Text(
                    text = message,
                    color = textColor,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                )
            }
            if (timestamp != null) {
                Text(
                    text = timestamp,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, start = 6.dp, end = 6.dp),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ComposeKitChatBubblePreview() {
    MaterialTheme {
        Column(Modifier.padding(vertical = 8.dp)) {
            ComposeKitChatBubble(message = "Hey, is the build green?", timestamp = "09:41")
            ComposeKitChatBubble(message = "Yep, APK is on the artifacts page", isMine = true, timestamp = "09:42")
        }
    }
}
