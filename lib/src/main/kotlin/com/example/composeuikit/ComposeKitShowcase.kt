package com.example.composeuikit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * One page that renders every component in the kit, live.
 *
 * This is the single source of truth for "what the library looks like": the demo app
 * embeds it directly, and the screenshot pipeline can render the same composable, so the
 * website previews can never drift from the real components.
 */
@Composable
fun ComposeKitShowcase(modifier: Modifier = Modifier) {
    var draft by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        item {
            ShowcaseSection("TopAppBar") {
                ComposeKitTopAppBar(title = "Inbox", subtitle = "3 unread")
            }
        }
        item {
            ShowcaseSection("Clock") {
                ComposeKitClock(label = "Local time", showSeconds = true)
            }
        }
        item {
            ShowcaseSection("Clock pill") {
                ComposeKitClockPill()
            }
        }
        item {
            ShowcaseSection("Avatars") {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ComposeKitAvatar(name = "Ada Lovelace", size = 48.dp, online = true)
                    ComposeKitAvatar(name = "Grace Hopper", size = 48.dp)
                    ComposeKitAvatar(name = "Alan Turing", size = 48.dp, online = true)
                }
            }
        }
        item {
            ShowcaseSection("Chat bubbles", contentPadding = PaddingValues(0.dp)) {
                Column {
                    ComposeKitChatBubble(
                        message = "Hey, is the build green?",
                        timestamp = "09:41",
                    )
                    ComposeKitChatBubble(
                        message = "Yep — APK is on the artifacts page",
                        isMine = true,
                        timestamp = "09:42",
                    )
                }
            }
        }
        item {
            ShowcaseSection("Chat input", contentPadding = PaddingValues(0.dp)) {
                ComposeKitChatInput(
                    value = draft,
                    onValueChange = { draft = it },
                    onSend = { draft = "" },
                )
            }
        }
        item {
            ShowcaseSection("Reader card", contentPadding = PaddingValues(0.dp)) {
                ComposeKitReaderCard(
                    title = "The Clockmaker's Apprentice",
                    chapterLabel = "Chapter One",
                    meta = "Demo content",
                    excerpt = SampleExcerpt,
                    chapter = SampleChapter,
                    resumeAfter = ResumeSnippet,
                    modifier = Modifier.fillMaxWidth().height(640.dp),
                )
            }
        }
    }
}

private val SampleExcerpt = listOf(
    "The shop smelled of brass filings and cold tea. Neve had swept the same floor every morning " +
        "for a year, and every morning the broom found a gear she had never seen before.",
    "Her master never explained the clocks. He handed her a screwdriver and said the rest was patience.",
)

private val SampleChapter = listOf(
    "Morning came grey through the shop window, and the hundred clocks on the wall disagreed about " +
        "it, as they always had.",
    "The shop smelled of brass filings and cold tea. She had swept the same floor every morning for " +
        "a year, and every morning the broom found a gear she had never seen before.",
    "On the bench lay a watch with no face, its hands turning in opposite directions. Her master had " +
        "left it there without a note, which meant he expected her to ask nothing and finish it.",
    "By noon she had it running. By evening she understood that it had been running all along, and " +
        "that the hands were not telling time but counting something else.",
    "She put down the screwdriver. Somewhere behind her the clocks on the wall fell silent at once, " +
        "and she decided she would not mention it.",
)

private const val ResumeSnippet = "On the bench lay a watch with no face"

@Composable
private fun ShowcaseSection(
    title: String,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp),
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 10.dp),
        )
        Column(modifier = Modifier.padding(contentPadding)) {
            content()
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun ComposeKitShowcasePreview() {
    MaterialTheme {
        ComposeKitShowcase()
    }
}
