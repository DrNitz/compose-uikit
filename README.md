# Compose UI Kit

[![Build](https://github.com/DrNitz/compose-uikit/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/DrNitz/compose-uikit/actions/workflows/build.yml)

Live page: <https://drnitz.github.io/compose-uikit/>

A small, reusable Jetpack Compose component library with a live demo app.
Components are written once and shared: other developers add a dependency, you install
the demo APK on your phone, and the same component code feeds both.

Not an icon pack and not a CSS widget kit — these are real Kotlin composables.

## Layout

```
lib/      reusable library: the components other developers depend on
demo/     Android app that renders every component (this is the APK you install)
docs/     the generated site: index.html plus one page per component
tools/    generate_docs.py, which builds docs/ from the library source
```

| Module | Produces | Purpose |
|---|---|---|
| `:lib` | an AAR | the components, consumed as a dependency |
| `:demo` | an APK | on-device showcase, embeds `:lib` |

## Components

| Component | File |
|---|---|
| `ComposeKitTopAppBar` | `lib/.../ComposeKitTopAppBar.kt` |
| `ComposeKitClock`, `ComposeKitClockPill` | `lib/.../ComposeKitClock.kt` |
| `ComposeKitChatBubble` | `lib/.../ComposeKitChatBubble.kt` |
| `ComposeKitChatInput` | `lib/.../ComposeKitChatInput.kt` |
| `ComposeKitReaderCard` | `lib/.../ComposeKitReaderCard.kt` |
| `ComposeKitAvatar` | `lib/.../ComposeKitAvatar.kt` |
| `ComposeKitShowcase` | `lib/.../ComposeKitShowcase.kt` |

`ComposeKitShowcase` renders every component on one page. The demo app embeds it directly —
record your preview media from there, so what visitors see matches what ships.

## Get the APK onto your phone

CI attaches the APK to a release on every push to `main`, which makes it a direct
download instead of a zip:

```
https://github.com/DrNitz/compose-uikit/releases/download/latest/compose-uikit-demo.apk
```

Open that on the phone and install it (allow "install unknown apps" for your browser). The
link never changes — the file behind it is replaced on every push. Workflow *artifacts* are
always zipped, which is why the APK lives on a release instead.

The app is a gallery: a grid of tiles, one per component, and tapping a tile shows that
component on its own, at full size.

To build locally instead:

```bash
./gradlew :demo:assembleDebug
# -> demo/build/outputs/apk/debug/demo-debug.apk
```

## Use a component in your app

From JitPack:

```kotlin
implementation("com.github.DrNitz:compose-uikit:v0.1.0")
```

From a clone of this repo:

```kotlin
implementation(project(":lib"))
```

## Documentation site

`docs/` is generated from the library, never hand-edited:

```bash
python3 tools/generate_docs.py
```

It reads every documented `@Composable` in `lib/` and extracts the signature, the KDoc
description, the `@param` docs, the usage example from the KDoc code block, and the matching
`ShowcaseSection` block used as the "demo source" panel.

| Path | What |
|---|---|
| `docs/index.html` | landing page with a clickable component grid |
| `docs/components/<slug>.html` | one page per component |
| `docs/style.css`, `docs/app.js` | shared styles and the copy buttons |
| `docs/media/MANIFEST.md` | which preview files are still missing |

### Previews are recorded by hand

Nothing renders component images for you. Drop a file at:

```
docs/media/<slug>/preview.webp     (.png, .gif or .mp4 also work)
```

and it appears on that component's page. Preference order is `.mp4` → `.webp` → `.png` →
`.gif`. Use `.mp4` for anything that moves — the ticking clock, typing in the chat input —
because a still image cannot show it. Keep clips short; they live in git history forever.

Missing media is not an error. The page still shows the install line, usage, demo source and
parameter table, and the grid marks the card "preview pending". Add components and media in
any order.

Slugs are the component name minus `ComposeKit`, kebab-cased:

| Component | Slug |
|---|---|
| `ComposeKitTopAppBar` | `top-app-bar` |
| `ComposeKitClock` | `clock` |
| `ComposeKitClockPill` | `clock-pill` |
| `ComposeKitAvatar` | `avatar` |
| `ComposeKitChatBubble` | `chat-bubble` |
| `ComposeKitChatInput` | `chat-input` |
| `ComposeKitReaderCard` | `reader-card` |
| `ComposeKitShowcase` | `showcase` |

Push to `main` and CI regenerates `docs/` and commits the result, so you can add media
straight from a phone and the site catches up on its own.

### Publishing

```bash
git tag v0.1.0 && git push origin v0.1.0
```

JitPack builds the tag. `jitpack.yml` pins JDK 17 and publishes only `:lib` — the demo module
is a test harness, not an artifact.

The coordinate uses the repo name, not the module name:

```
com.github.<owner>:<repo>:<tag>      ->  com.github.DrNitz:compose-uikit:v0.1.0
```

That is what JitPack actually serves; `lib/build.gradle.kts` mirrors it so a local
`publishToMavenLocal` produces the same coordinates.

To check locally first:

```bash
./gradlew :lib:publishToMavenLocal
```

When you tag a new version, bump the version in `lib/build.gradle.kts` and `VERSION` in
`tools/generate_docs.py` so the install line on the site matches.

## Add a component

1. Create `lib/src/main/kotlin/com/example/composeuikit/ComposeKit<Name>.kt`.
2. Write a normal `@Composable` with parameters defaulted, plus a `@Preview` in the same file.
3. Add it to `ComposeKitShowcase` so it shows up in the demo app.
4. Push. CI rebuilds the APK and the component appears on your phone.

Keep components stateless where possible (take `value` + `onValueChange`) so callers
own the state. That is what makes them reusable.

## Toolchain notes

These are pinned deliberately, because the combination matters:

| Tool | Version | Why |
|---|---|---|
| Gradle | 9.9.2 | AGP 9.5.2 needs Gradle 9.1+ |
| AGP | 9.5.2 | `compileSdk 37.2` support |
| Kotlin | 2.2.10 | AGP 9 bundles KGP 2.2.10, and the Compose compiler plugin **must match the bundled KGP** |
| Compose BOM | 2026.11.01 | |
| compileSdk | 37 + minor 2 | androidx Compose 1.13 requires 37.1+ |

Two AGP 9 changes worth knowing:

- **Built-in Kotlin.** Do **not** apply `org.jetbrains.kotlin.android`; AGP 9 forbids it.
  Kotlin compilation is built in. The Compose compiler plugin is still applied, and its
  version has to match AGP's bundled KGP version.
- **`compileSdkMinor`.** Android SDKs are minor-versioned now (`37.2`, not just `37`).

Because `jvmTarget` defaults to `compileOptions.targetCompatibility`, neither module sets it.

## Not done yet

- More components (cards, list items, buttons, snackbar, badges) — each one picks up a docs
  page automatically, and a tile in the demo gallery
- Preview media (see `docs/media/MANIFEST.md` for the files to record)

## Credits

`ComposeKitClock` is a Compose reimplementation of a Uiverse.io element:

- **nice-fly-98** by chase2k25 — <https://uiverse.io/chase2k25/nice-fly-98>
  (MIT License, Copyright (c) chase2k25 (chandu.exe))

The dial's proportions, shadows and colours follow that design. The credit is repeated at the
top of `lib/src/main/kotlin/com/example/composeuikit/ComposeKitClock.kt`.

## License

MIT — see `LICENSE`.
