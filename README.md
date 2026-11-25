# Compose UI Kit

A small, reusable Jetpack Compose component library with a live demo app.
Components are written once and shared: other developers add a dependency, you install
the demo APK on your phone, and the same component code feeds both.

Not an icon pack and not a CSS widget kit — these are real Kotlin composables.

## Layout

```
lib/     reusable library: the components other developers depend on
demo/    Android app that renders every component (this is the APK you install)
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
| `ComposeKitAvatar` | `lib/.../ComposeKitAvatar.kt` |
| `ComposeKitShowcase` | `lib/.../ComposeKitShowcase.kt` |

`ComposeKitShowcase` renders every component on one page. The demo app embeds it directly,
and it is also the single place a future screenshot pipeline would render — so website
previews can't drift from the real components.

## Get the APK onto your phone

Push to `main`, then on GitHub open **Actions → the latest Build run → Artifacts →
`compose-uikit-demo-debug`**. Download it on the phone and open it (allow "install unknown
apps" for your browser). Rebuilds happen on every push, so the loop works entirely from a
phone.

To build locally instead:

```bash
./gradlew :demo:assembleDebug
# -> demo/build/outputs/apk/debug/demo-debug.apk
```

## Use a component in your app

While the project is local:

```kotlin
implementation(project(":lib"))
```

Once published to JitPack, it becomes a one-liner:

```kotlin
implementation("com.github.<user>.<repo>:lib:<tag>")
```

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

- Website that generates itself from the components (screenshots via Compose previews in CI)
- JitPack publish + semantic version tags
- More components

## License

MIT — see `LICENSE`.
