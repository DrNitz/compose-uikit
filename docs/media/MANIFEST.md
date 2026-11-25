# Preview media

Drop one file per component here. Nothing is generated — if you want a preview,
you record it and add it.

Path: `docs/media/<slug>/preview.<ext>`

Accepted extensions, in the order the generator prefers them:
`.mp4` → `.webp` → `.png` → `.gif`

Use `.mp4` for anything animated (the clock ticking, typing in the chat input) — a
still image cannot show those. Keep clips short; they live in git history forever.

| Component | Slug | Path | Status |
|---|---|---|---|
| `ComposeKitTopAppBar` | `top-app-bar` | `docs/media/top-app-bar/preview.webp` | **missing** |
| `ComposeKitClock` | `clock` | `docs/media/clock/preview.webp` | **missing** |
| `ComposeKitClockPill` | `clock-pill` | `docs/media/clock-pill/preview.webp` | **missing** |
| `ComposeKitAvatar` | `avatar` | `docs/media/avatar/preview.webp` | **missing** |
| `ComposeKitChatBubble` | `chat-bubble` | `docs/media/chat-bubble/preview.webp` | **missing** |
| `ComposeKitChatInput` | `chat-input` | `docs/media/chat-input/preview.webp` | **missing** |
| `ComposeKitReaderCard` | `reader-card` | `docs/media/reader-card/preview.webp` | **missing** |
| `ComposeKitShowcase` | `showcase` | `docs/media/showcase/preview.webp` | **missing** |

After adding a file, run `python3 tools/generate_docs.py` (or push — CI does it)
and the preview appears on that component's page.
