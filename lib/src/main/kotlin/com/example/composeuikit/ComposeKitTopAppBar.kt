package com.example.composeuikit

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

/**
 * Material 3 top app bar with a title, an optional subtitle, a leading slot and trailing actions.
 *
 * @param title main label, usually the screen name.
 * @param subtitle small secondary line under the title, hidden when null.
 * @param navigationIcon optional leading content, typically a back button.
 * @param actions trailing content, typically icon buttons.
 *
 * ```kotlin
 * ComposeKitTopAppBar(title = "Inbox", subtitle = "3 unread")
 * ```
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeKitTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        modifier = modifier,
        navigationIcon = { navigationIcon?.invoke() },
        title = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        actions = actions,
    )
}

@Preview(showBackground = true)
@Composable
private fun ComposeKitTopAppBarPreview() {
    MaterialTheme {
        ComposeKitTopAppBar(title = "Inbox", subtitle = "3 unread")
    }
}