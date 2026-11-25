package com.example.composeuikit.demo

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.composeuikit.ComposeKitAvatar
import com.example.composeuikit.ComposeKitChatBubble
import com.example.composeuikit.ComposeKitChatInput
import com.example.composeuikit.ComposeKitClock
import com.example.composeuikit.ComposeKitClockPill
import com.example.composeuikit.ComposeKitReaderCard
import com.example.composeuikit.ComposeKitTopAppBar

/** One entry in the gallery: how a component is named, described, tinted and rendered. */
internal data class CatalogEntry(
    val id: String,
    val name: String,
    val summary: String,
    val gradient: List<Color>,
    val fullBleed: Boolean = false,
    val content: @Composable (Modifier) -> Unit,
)

@Composable
internal fun rememberCatalog(): List<CatalogEntry> = remember {
    listOf(
        CatalogEntry(
            id = "top-app-bar",
            name = "TopAppBar",
            summary = "Title, subtitle, leading slot and trailing actions.",
            gradient = listOf(Color(0xFF6750A4), Color(0xFF9A82DB)),
            content = { m -> DemoTopAppBar(m) },
        ),
        CatalogEntry(
            id = "clock",
            name = "Clock",
            summary = "Self-ticking time display in two sizes.",
            gradient = listOf(Color(0xFF00696E), Color(0xFF4FB3B8)),
            content = { m -> DemoClock(m) },
        ),
        CatalogEntry(
            id = "avatar",
            name = "Avatar",
            summary = "Initials in a circle with a presence dot.",
            gradient = listOf(Color(0xFF8C4A60), Color(0xFFD98BA4)),
            content = { m -> DemoAvatars(m) },
        ),
        CatalogEntry(
            id = "chat-bubble",
            name = "ChatBubble",
            summary = "Incoming and outgoing message bubbles.",
            gradient = listOf(Color(0xFF7A5900), Color(0xFFC9A227)),
            content = { m -> DemoChatBubbles(m) },
        ),
        CatalogEntry(
            id = "chat-input",
            name = "ChatInput",
            summary = "Composer with a send button that waits for text.",
            gradient = listOf(Color(0xFF2F5D3A), Color(0xFF6FBF83)),
            content = { m -> DemoChatInput(m) },
        ),
        CatalogEntry(
            id = "reader-card",
            name = "ReaderCard",
            summary = "A story card that grows into a full reader.",
            gradient = listOf(Color(0xFF7B2D26), Color(0xFFD37157)),
            fullBleed = true,
            content = { m -> DemoReaderCard(m) },
        ),
    )
}

/* ─────────────────────────────── catalogue ─────────────────────────────── */

@Composable
private fun DemoTopAppBar(modifier: Modifier) {
    ComposeKitTopAppBar(
        title = "Inbox",
        subtitle = "3 unread",
        modifier = modifier,
    )
}

@Composable
private fun DemoClock(modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        ComposeKitClock(label = "Local time")
        Spacer(Modifier.height(14.dp))
        ComposeKitClockPill()
    }
}

@Composable
private fun DemoAvatars(modifier: Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ComposeKitAvatar(name = "Ada Lovelace", size = 48.dp, online = true)
        ComposeKitAvatar(name = "Grace Hopper", size = 48.dp)
        ComposeKitAvatar(name = "Alan Turing", size = 48.dp, online = true)
    }
}

@Composable
private fun DemoChatBubbles(modifier: Modifier) {
    Column(modifier) {
        ComposeKitChatBubble(message = "Hey, is the build green?", timestamp = "09:41")
        ComposeKitChatBubble(
            message = "Yep — the APK is on the artifacts page",
            isMine = true,
            timestamp = "09:42",
        )
    }
}

@Composable
private fun DemoChatInput(modifier: Modifier) {
    var draft by remember { mutableStateOf("") }
    ComposeKitChatInput(
        value = draft,
        onValueChange = { draft = it },
        onSend = { draft = "" },
        modifier = modifier,
    )
}

@Composable
private fun DemoReaderCard(modifier: Modifier) {
    ComposeKitReaderCard(
        title = "The Clockmaker's Apprentice",
        chapterLabel = "Chapter One",
        meta = "Demo content",
        excerpt = ReaderExcerpt,
        chapter = ReaderChapter,
        resumeAfter = ReaderResume,
        modifier = modifier,
    )
}

/* ──────────────────────────────── gallery ──────────────────────────────── */

@Composable
internal fun ComponentGallery(onOpen: (CatalogEntry) -> Unit) {
    val catalog = rememberCatalog()

    Scaffold(
        topBar = {
            ComposeKitTopAppBar(
                title = "Compose UI Kit",
                subtitle = "${catalog.size} components",
            )
        },
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) { GalleryIntro(catalog.size) }
            items(catalog, key = { it.id }) { entry ->
                ComponentTile(entry = entry, onClick = { onOpen(entry) })
            }
        }
    }
}

@Composable
private fun GalleryIntro(count: Int) {
    Column(Modifier.padding(bottom = 4.dp)) {
        Text(
            text = "Every component, on its own",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Tap a tile to see $count interactive components at full size, " +
                "away from the rest of the page.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ComponentTile(entry: CatalogEntry, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f),
        ),
    ) {
        Column(Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(entry.gradient)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = entry.name.take(1),
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = entry.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = entry.summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/* ──────────────────────────────── detail ───────────────────────────────── */

@Composable
internal fun ComponentDetail(entry: CatalogEntry, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            ComposeKitTopAppBar(
                title = entry.name,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text(text = "←", style = MaterialTheme.typography.titleLarge)
                    }
                },
            )
        },
    ) { padding ->
        if (entry.fullBleed) {
            // Large pieces own the whole screen: no frame, no scroll competing with them.
            Box(Modifier.fillMaxSize().padding(padding)) {
                entry.content(Modifier.fillMaxSize())
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                Text(
                    text = entry.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box(Modifier.padding(20.dp)) {
                        entry.content(Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

/* ───────────────────────────── sample content ──────────────────────────── */

private val ReaderExcerpt = listOf(
    "The shop smelled of brass filings and cold tea. Neve had swept the same floor every morning " +
        "for a year, and every morning the broom found a gear she had never seen before.",
    "Her master never explained the clocks. He handed her a screwdriver and said the rest was patience.",
)

private val ReaderChapter = listOf(
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

private const val ReaderResume = "On the bench lay a watch with no face"