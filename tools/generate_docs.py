#!/usr/bin/env python3
"""Generate the Compose UI Kit site from the library source.

Reads the components in lib/src/main/kotlin, pulls out signatures, KDoc, parameter
docs and the usage example from each, and writes:

    docs/index.html                 landing page, clickable component grid
    docs/components/<slug>.html     one page per component
    docs/style.css                  shared stylesheet
    docs/app.js                     copy-to-clipboard
    docs/media/MANIFEST.md          exactly which preview files to drop in, and what's missing

Preview media is NOT generated. Drop a file at docs/media/<slug>/preview.<ext>
and it appears on that component's page. See MANIFEST.md for the slugs.

Deterministic: no timestamps, stable ordering, so CI can diff the output.
"""

from __future__ import annotations

import html
import re
import subprocess
import sys
from dataclasses import dataclass, field
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
LIB = ROOT / "lib" / "src" / "main" / "kotlin" / "com" / "example" / "composeuikit"
DOCS = ROOT / "docs"
MEDIA = DOCS / "media"

VERSION = "v0.1.0"
MEDIA_EXTS = ("mp4", "webp", "png", "gif")  # preference order

# Landing-page order. Unknown components are appended alphabetically.
ORDER = [
    "top-app-bar",
    "clock",
    "clock-pill",
    "avatar",
    "chat-bubble",
    "chat-input",
    "showcase",
]

# Component slug -> the ShowcaseSection title that renders it in ComposeKitShowcase.kt.
# Used for the "Demo source" block, which is literally what the demo app draws.
SHOWCASE_TITLE = {
    "top-app-bar": "TopAppBar",
    "clock": "Clock",
    "clock-pill": "Clock pill",
    "avatar": "Avatars",
    "chat-bubble": "Chat bubbles",
    "chat-input": "Chat input",
}


# --------------------------------------------------------------------------- parsing


@dataclass
class Component:
    name: str
    slug: str
    source_file: str
    description: str
    summary: str
    usage: str | None
    demo: str | None = None
    params: list[tuple[str, str, str | None, str]] = field(default_factory=list)

    @property
    def preview(self) -> Path | None:
        for ext in MEDIA_EXTS:
            candidate = MEDIA / self.slug / f"preview.{ext}"
            if candidate.exists():
                return candidate
        return None


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def slugify(name: str) -> str:
    stripped = name[len("ComposeKit"):] if name.startswith("ComposeKit") else name
    return re.sub(r"(?<!^)(?=[A-Z])", "-", stripped).lower()


def matching_paren(text: str, open_idx: int) -> int:
    """Index of the ')' matching the '(' at open_idx. -1 if unbalanced."""
    depth = 0
    for i in range(open_idx, len(text)):
        c = text[i]
        if c == "(":
            depth += 1
        elif c == ")":
            depth -= 1
            if depth == 0:
                return i
    return -1


def matching_brace(text: str, open_idx: int) -> int:
    depth = 0
    for i in range(open_idx, len(text)):
        c = text[i]
        if c == "{":
            depth += 1
        elif c == "}":
            depth -= 1
            if depth == 0:
                return i
    return -1


def split_top_level(text: str) -> list[str]:
    parts: list[str] = []
    buf: list[str] = []
    depth = 0
    for c in text:
        if c in "([{<":
            depth += 1
        elif c in ")]}>":
            if c == ">" and depth == 0:
                pass  # '->' arrow, not a closing bracket
            else:
                depth -= 1
        if c == "," and depth == 0:
            parts.append("".join(buf))
            buf = []
        else:
            buf.append(c)
    if buf:
        parts.append("".join(buf))
    return [p.strip() for p in parts if p.strip()]


def parse_declaration(raw: str) -> tuple[str, str, str | None] | None:
    """'name: Type = default' -> (name, type, default)."""
    depth = 0
    colon = -1
    for i, c in enumerate(raw):
        if c in "([{":
            depth += 1
        elif c in ")]}":
            depth -= 1
        elif c == ":" and depth == 0:
            colon = i
            break
    if colon < 0:
        return None

    name = re.sub(r"^(@\w+\s*)+", "", raw[:colon].strip())
    rest = raw[colon + 1:].strip()

    depth = 0
    eq = -1
    for i, c in enumerate(rest):
        if c in "([{<":
            depth += 1
        elif c in ")]}>":
            if c == ">" and depth == 0:
                pass
            else:
                depth -= 1
        elif c == "=" and depth == 0:
            prev = rest[i - 1] if i > 0 else ""
            nxt = rest[i + 1] if i + 1 < len(rest) else ""
            if prev not in "=!<>" and nxt != "=":
                eq = i
                break

    if eq >= 0:
        return name, rest[:eq].strip(), rest[eq + 1:].strip()
    return name, rest, None


def clean_kdoc(block: str) -> str:
    body = block.strip()
    body = re.sub(r"^/\*\*", "", body)
    body = re.sub(r"\*/$", "", body)
    lines = []
    for line in body.split("\n"):
        line = re.sub(r"^\s*\*[ ]?", "", line)
        lines.append(line.rstrip())
    return "\n".join(lines).strip("\n")


def parse_kdoc(block: str) -> tuple[str, str, dict[str, str], str | None]:
    """-> (description, summary, {param: doc}, usage_code)"""
    cleaned = clean_kdoc(block)

    code_match = re.search(r"```(?:kotlin)?\s*\n(.*?)```", cleaned, re.DOTALL)
    usage = code_match.group(1).strip("\n") if code_match else None
    without_code = re.sub(r"```(?:kotlin)?\s*\n.*?```", "", cleaned, flags=re.DOTALL)

    params: dict[str, str] = {}
    desc_lines: list[str] = []
    current: str | None = None

    for line in without_code.split("\n"):
        stripped = line.strip()
        if stripped.startswith("@param "):
            rest = stripped[len("@param "):]
            name, _, text = rest.partition(" ")
            name = name.strip()
            params[name] = text.strip()
            current = name
        elif stripped.startswith("@"):
            current = None
        elif not stripped:
            continue
        elif current:
            params[current] = f"{params[current]} {stripped}".strip()
        else:
            desc_lines.append(stripped)

    description = " ".join(desc_lines).strip()
    summary = re.split(r"(?<=\.)\s", description)[0] if description else ""
    return description, summary, params, usage


def parse_components() -> list[Component]:
    components: list[Component] = []
    decl_re = re.compile(
        r"^(?P<vis>(?:(?:public|internal|private|protected)\s+))?"
        r"fun\s+(?P<name>[A-Za-z_]\w*)\s*\(",
        re.MULTILINE,
    )

    for path in sorted(LIB.glob("ComposeKit*.kt")):
        text = read(path)
        for decl in decl_re.finditer(text):
            # Previews are `private fun XPreview()`; skip them outright.
            if "private" in (decl.group("vis") or ""):
                continue

            name = decl.group("name")
            open_idx = text.index("(", decl.end() - 1)
            close_idx = matching_paren(text, open_idx)
            if close_idx < 0:
                raise ValueError(f"unbalanced parameter list for {name} in {path.name}")

            # Between the KDoc and `fun` there may only be annotations (@Composable, @Preview...).
            prefix = text[:decl.start()]
            kdoc = None
            marker = prefix.rfind("/**")
            if marker >= 0:
                end = prefix.find("*/", marker)
                if end >= 0:
                    between = prefix[end + 2:]
                    leftover = re.sub(r"@\w+(?:\([^)]*\))?", "", between)
                    if not leftover.strip():
                        kdoc = prefix[marker:end + 2]

            if kdoc is None:
                continue

            description, summary, param_docs, usage = parse_kdoc(kdoc)
            if not description:
                continue  # not a documented public component

            params: list[tuple[str, str, str | None, str]] = []
            for raw in split_top_level(text[open_idx + 1:close_idx]):
                parsed = parse_declaration(raw)
                if parsed is None:
                    continue
                pname, ptype, pdefault = parsed
                params.append((pname, ptype, pdefault, param_docs.get(pname, "")))

            components.append(
                Component(
                    name=name,
                    slug=slugify(name),
                    source_file=path.name,
                    description=description,
                    summary=summary,
                    usage=usage,
                    params=params,
                )
            )

    order_index = {slug: i for i, slug in enumerate(ORDER)}
    components.sort(key=lambda c: (order_index.get(c.slug, len(ORDER)), c.slug))
    return components


def parse_showcase() -> dict[str, str]:
    """ShowcaseSection title -> the Kotlin block that renders it."""
    path = LIB / "ComposeKitShowcase.kt"
    if not path.exists():
        return {}
    text = read(path)
    sections: dict[str, str] = {}

    for match in re.finditer(r'ShowcaseSection\(\s*"([^"]+)"', text):
        title = match.group(1)
        brace = text.find("{", match.end())
        if brace < 0:
            continue
        close = matching_brace(text, brace)
        if close < 0:
            continue
        block = text[match.start():close + 1]
        sections[title] = dedent(block)
    return sections


def dedent(block: str) -> str:
    lines = block.split("\n")
    indents = [len(l) - len(l.lstrip()) for l in lines[1:] if l.strip()]
    strip = min(indents) if indents else 0
    out = [lines[0].strip()]
    for line in lines[1:]:
        out.append(line[strip:] if len(line) >= strip else line.lstrip())
    return "\n".join(out).rstrip()


def repo_slug() -> str:
    try:
        url = subprocess.run(
            ["git", "remote", "get-url", "origin"],
            cwd=ROOT, capture_output=True, text=True, check=True,
        ).stdout.strip()
    except Exception:
        return "DrNitz/compose-uikit"
    m = re.search(r"github\.com[:/]([^/]+)/([^/.]+)", url)
    return f"{m.group(1)}/{m.group(2)}" if m else "DrNitz/compose-uikit"


# ----------------------------------------------------------------------- highlighting

TOKEN_RE = re.compile(
    r'''
      (?P<comment>//[^\n]*|/\*.*?\*/)
    | (?P<ann>@[A-Za-z_]\w*)
    | (?P<string>"""(?:.*?)"""|"(?:\\.|[^"\\\n])*")
    | (?P<number>\b\d[\d_]*(?:\.[\d_]+)?[fFlL]?\b)
    | (?P<kw>\b(?:fun|val|var|class|object|interface|data|sealed|enum|when|if|else|for|while|return|import|package|private|public|internal|protected|override|suspend|inline|reified|companion|null|true|false|this|it|in|is|as|do|try|catch|finally|throw|break|continue|typealias|by|lateinit|const|abstract|open|init|get|set|constructor|super|out|where)\b)
    | (?P<type>\b[A-Z]\w*\b)
    ''',
    re.DOTALL | re.VERBOSE,
)


def esc(s: str) -> str:
    return html.escape(s, quote=False)


def inline_md(text: str) -> str:
    """Escape, then turn `code` spans from KDoc into real <code>."""
    if not text:
        return ""
    return re.sub(r"`([^`]+)`", r"<code>\1</code>", html.escape(text, quote=False))


def plain(text: str) -> str:
    """Drop KDoc backticks, for attribute values."""
    return re.sub(r"`([^`]+)`", r"\1", text or "")


def highlight(code: str) -> str:
    out: list[str] = []
    last = 0
    for m in TOKEN_RE.finditer(code):
        if m.start() < last:
            continue
        out.append(esc(code[last:m.start()]))
        out.append(f'<span class="t-{m.lastgroup}">{esc(m.group())}</span>')
        last = m.end()
    out.append(esc(code[last:]))
    return "".join(out)


# -------------------------------------------------------------------------- rendering

STYLE = """\
:root {
  --accent: #6750a4;
  --accent-soft: #efe9ff;
  --ink: #1b1b1f;
  --muted: #5f6368;
  --line: #e4e1ea;
  --bg: #fbfaff;
  --card: #ffffff;
}
* { box-sizing: border-box; }
body {
  margin: 0;
  background: var(--bg);
  color: var(--ink);
  font: 16px/1.6 -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
}
.wrap { max-width: 860px; margin: 0 auto; padding: 0 24px; }
header { padding: 72px 0 40px; border-bottom: 1px solid var(--line); }
header.compact { padding: 40px 0 28px; }
.eyebrow {
  display: inline-block;
  color: var(--accent);
  background: var(--accent-soft);
  border-radius: 999px;
  padding: 4px 12px;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: .02em;
}
h1 { font-size: clamp(34px, 6vw, 52px); line-height: 1.1; margin: 20px 0 12px; letter-spacing: -.02em; }
header.compact h1 { font-size: clamp(26px, 4.4vw, 36px); margin: 14px 0 8px; }
h1 code { font-size: .92em; }
.lede { font-size: 19px; color: var(--muted); max-width: 620px; margin: 0; }
header.compact .lede { font-size: 17px; }
.actions { display: flex; flex-wrap: wrap; gap: 12px; margin-top: 28px; }
.btn {
  display: inline-block;
  padding: 11px 18px;
  border-radius: 10px;
  text-decoration: none;
  font-weight: 600;
}
.btn-primary { background: var(--accent); color: #fff; }
.btn-primary:hover { background: #574490; }
.btn-secondary { background: var(--card); color: var(--accent); border: 1px solid var(--line); }
section { padding: 44px 0; border-bottom: 1px solid var(--line); }
section:last-of-type { border-bottom: 0; }
h2 { font-size: 24px; margin: 0 0 18px; letter-spacing: -.01em; }
h2 code { font-size: .95em; }
.grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(230px, 1fr)); gap: 12px; }
.card {
  display: block;
  background: var(--card);
  border: 1px solid var(--line);
  border-radius: 12px;
  padding: 16px 18px;
  text-decoration: none;
  color: inherit;
}
a.card:hover { border-color: var(--accent); }
.card code { color: var(--accent); font-size: 14px; font-weight: 600; }
.card p { margin: 8px 0 0; color: var(--muted); font-size: 14px; }
a.back { color: var(--accent); text-decoration: none; font-weight: 600; font-size: 15px; }
a.back:hover { text-decoration: underline; }
.preview {
  display: flex;
  align-items: center;
  justify-content: center;
  max-width: 380px;
  margin: 0 auto;
  padding: 18px;
  background: var(--card);
  border: 1px solid var(--line);
  border-radius: 16px;
}
.preview img, .preview video { display: block; width: 100%; height: auto; border-radius: 10px; }
.preview.empty {
  border-style: dashed;
  color: var(--muted);
  font-size: 14px;
  text-align: center;
  padding: 40px 18px;
}
.preview.empty code { color: var(--accent); }
.caption { text-align: center; color: var(--muted); font-size: 14px; margin: 12px 0 0; }
.tag {
  display: inline-block;
  margin-top: 10px;
  font-size: 12px;
  font-weight: 600;
  color: var(--muted);
  background: var(--accent-soft);
  border-radius: 999px;
  padding: 2px 10px;
}
.pending .tag { color: #8a6d00; background: #fff2c2; }
.block { position: relative; }
.block .label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  font-size: 13px;
  color: var(--muted);
  margin-bottom: 8px;
}
button.copy {
  font: inherit;
  font-size: 13px;
  font-weight: 600;
  color: var(--accent);
  background: var(--card);
  border: 1px solid var(--line);
  border-radius: 8px;
  padding: 4px 12px;
  cursor: pointer;
}
button.copy:hover { border-color: var(--accent); }
pre {
  background: #211f2b;
  color: #f2effa;
  padding: 18px;
  border-radius: 12px;
  overflow-x: auto;
  font-size: 14px;
  line-height: 1.5;
  margin: 0;
}
pre code { font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace; }
.t-kw { color: #c792ea; }
.t-string { color: #c3e88d; }
.t-comment { color: #7f849c; font-style: italic; }
.t-ann { color: #ffcb6b; }
.t-number { color: #f78c6c; }
.t-type { color: #82aaff; }
table.props { width: 100%; border-collapse: collapse; font-size: 15px; }
table.props th, table.props td {
  text-align: left;
  padding: 10px 12px;
  border-bottom: 1px solid var(--line);
  vertical-align: top;
}
table.props th { font-size: 13px; text-transform: uppercase; letter-spacing: .04em; color: var(--muted); }
table.props code { color: var(--accent); font-size: 14px; }
table.props .type code { color: var(--ink); }
.note { color: var(--muted); font-size: 15px; }
.note a { color: var(--accent); }
.note code { color: var(--accent); }
footer { padding: 28px 0 48px; color: var(--muted); font-size: 14px; }
@media (prefers-color-scheme: dark) {
  :root {
    --accent: #cfb8ff;
    --accent-soft: #33284a;
    --ink: #ece6f6;
    --muted: #b5adc4;
    --line: #38333f;
    --bg: #14131a;
    --card: #1d1b24;
  }
  .btn-primary { color: #1d1b24; }
  .btn-primary:hover { background: #e0d1ff; }
  .pending .tag { color: #ffe08a; background: #3d3410; }
}
"""

APP_JS = """\
document.querySelectorAll("button.copy").forEach(function (btn) {
  btn.addEventListener("click", function () {
    var target = document.getElementById(btn.dataset.copy);
    if (!target) return;
    var text = target.innerText.replace(/\\n+$/, "");
    var done = function () {
      var old = btn.textContent;
      btn.textContent = "Copied";
      setTimeout(function () { btn.textContent = old; }, 1400);
    };
    if (navigator.clipboard && navigator.clipboard.writeText) {
      navigator.clipboard.writeText(text).then(done, done);
    } else {
      var ta = document.createElement("textarea");
      ta.value = text;
      document.body.appendChild(ta);
      ta.select();
      document.execCommand("copy");
      document.body.removeChild(ta);
      done();
    }
  });
});
"""


def code_block(label: str, code: str, block_id: str) -> str:
    return (
        f'<div class="block">\n'
        f'  <div class="label"><span>{esc(label)}</span>'
        f'<button class="copy" data-copy="{block_id}">Copy</button></div>\n'
        f'  <pre><code id="{block_id}">{highlight(code)}</code></pre>\n'
        f'</div>'
    )


def head(title: str, description: str, prefix: str) -> str:
    return (
        "<!doctype html>\n"
        '<html lang="en">\n'
        "<head>\n"
        '  <meta charset="utf-8">\n'
        '  <meta name="viewport" content="width=device-width, initial-scale=1">\n'
        f"  <title>{esc(title)}</title>\n"
        f'  <meta name="description" content="{html.escape(plain(description), quote=True)}">\n'
        f'  <link rel="stylesheet" href="{prefix}style.css">\n'
        "</head>\n"
    )


def page_foot(prefix: str) -> str:
    return f'  <script src="{prefix}app.js"></script>\n</body>\n</html>\n'


def render_index(components: list[Component], slug: str) -> str:
    missing = sum(1 for c in components if c.preview is None)

    cards = []
    for c in components:
        tag = "" if c.preview else '<span class="tag">preview pending</span>'
        cls = "card" if c.preview else "card pending"
        cards.append(
            f'        <a class="{cls}" href="components/{c.slug}.html">'
            f"<code>{esc(c.name)}</code><p>{inline_md(c.summary)}</p>{tag}</a>"
        )

    preview_note = ""
    if missing:
        preview_note = (
            f'\n      <p class="note">{missing} of {len(components)} components still need a '
            f"preview. Drop a file at <code>docs/media/&lt;slug&gt;/preview.webp</code> "
            f'(or <code>.mp4</code>, <code>.png</code>, <code>.gif</code>) and it shows up here. '
            f'Slugs are listed in <a href="https://github.com/{slug}/blob/main/docs/media/MANIFEST.md">'
            f"media/MANIFEST.md</a>.</p>"
        )

    jitpack = f'implementation("com.github.{slug}:lib:{VERSION}")'

    return (
        head(
            "Compose UI Kit",
            "A small, reusable Jetpack Compose component library with a live demo app.",
            "",
        )
        + "<body>\n"
        '  <div class="wrap">\n'
        "    <header>\n"
        '      <span class="eyebrow">Jetpack Compose</span>\n'
        "      <h1>Compose UI Kit</h1>\n"
        '      <p class="lede">Reusable composables written once and shared: the library is a '
        "dependency for other apps, the demo app is the APK on your phone, and both render the "
        "same component code.</p>\n"
        '      <div class="actions">\n'
        f'        <a class="btn btn-primary" href="https://github.com/{slug}">View on GitHub</a>\n'
        f'        <a class="btn btn-secondary" href="https://github.com/{slug}/actions">Get the demo APK</a>\n'
        "      </div>\n"
        "    </header>\n"
        "\n"
        "    <section>\n"
        "      <h2>Components</h2>\n"
        '      <div class="grid">\n'
        + "\n".join(cards)
        + "\n      </div>"
        + preview_note
        + "\n    </section>\n"
        "\n"
        "    <section>\n"
        "      <h2>Use it</h2>\n"
        + code_block("build.gradle.kts", jitpack, "install")
        + '\n      <p class="note">Published with JitPack from tag <code>'
        + VERSION
        + "</code>. Not tagged yet? Clone the repo and use "
        '<code>implementation(project(":lib"))</code>. The debug APK is attached to every CI '
        f'run as the artifact <code>compose-uikit-demo-debug</code> — see '
        f'<a href="https://github.com/{slug}/actions">Actions</a>.</p>\n'
        "    </section>\n"
        "\n"
        "    <section>\n"
        "      <h2>Toolchain</h2>\n"
        '      <div class="grid">\n'
        '        <div class="card"><code>Gradle 9.9.2</code><p>AGP 9.5.2 needs Gradle 9.1+.</p></div>\n'
        '        <div class="card"><code>AGP 9.5.2</code><p>compileSdk 37.2 support.</p></div>\n'
        "        <div class=\"card\"><code>Kotlin 2.2.10</code><p>Matches AGP's bundled KGP; "
        "Compose compiler must match.</p></div>\n"
        "        <div class=\"card\"><code>Compose BOM 2026.11.01</code><p>Requires "
        "compileSdk 37.1+.</p></div>\n"
        "      </div>\n"
        "    </section>\n"
        "\n"
        "    <footer>MIT licensed. Built with Jetpack Compose.</footer>\n"
        "  </div>\n"
        + page_foot("")
    )


def render_component(component: Component, slug: str) -> str:
    preview = component.preview
    parts: list[str] = []

    if preview is not None:
        rel = f"../media/{component.slug}/{preview.name}"
        if preview.suffix == ".mp4":
            media = (
                '<video autoplay loop muted playsinline '
                f'src="{rel}"></video>'
            )
        else:
            media = f'<img src="{rel}" alt="{esc(component.name)} preview">'
        parts.append(f'      <div class="preview">\n        {media}\n      </div>\n')
        parts.append('      <p class="caption">Recorded from the demo app.</p>\n')
    else:
        parts.append(
            '      <div class="preview empty">\n'
            f"        No preview yet.<br>Add "
            f"<code>docs/media/{component.slug}/preview.webp</code>\n"
            "      </div>\n"
        )

    sections = [
        "<section>\n" + "".join(parts) + "</section>\n",
    ]

    jitpack = f'implementation("com.github.{slug}:lib:{VERSION}")'
    sections.append(
        "<section>\n      <h2>Install</h2>\n"
        + code_block("build.gradle.kts", jitpack, "install")
        + '\n      <p class="note">Or from a clone: '
        + '<code>implementation(project(":lib"))</code>.</p>\n    </section>\n'
    )

    if component.usage:
        sections.append(
            "<section>\n      <h2>Usage</h2>\n"
            + code_block("Kotlin", component.usage, "usage")
            + "\n    </section>\n"
        )

    if component.demo:
        sections.append(
            "<section>\n      <h2>Demo source</h2>\n"
            + code_block("Kotlin", component.demo, "demo")
            + "\n      <p class=\"note\">This is the block the demo app renders, so it is what "
            "produced the preview above.</p>\n    </section>\n"
        )

    if component.params:
        rows = []
        for pname, ptype, pdefault, pdoc in component.params:
            default = f"<code>{esc(pdefault)}</code>" if pdefault is not None else "—"
            rows.append(
                f"        <tr><td><code>{esc(pname)}</code></td>"
                f'<td class="type"><code>{esc(ptype)}</code></td>'
                f"<td>{default}</td><td>{inline_md(pdoc) if pdoc else '—'}</td></tr>"
            )
        sections.append(
            "<section>\n      <h2>Parameters</h2>\n"
            '      <table class="props">\n'
            "        <thead><tr><th>Parameter</th><th>Type</th><th>Default</th>"
            "<th>Description</th></tr></thead>\n"
            "        <tbody>\n" + "\n".join(rows) + "\n        </tbody>\n"
            "      </table>\n    </section>\n"
        )

    body = "".join(sections)

    return (
        head(
            f"{component.name} — Compose UI Kit",
            component.summary or component.description,
            "../",
        )
        + "<body>\n"
        '  <div class="wrap">\n'
        '    <header class="compact">\n'
        '      <a class="back" href="../index.html">← All components</a>\n'
        f"      <h1><code>{esc(component.name)}</code></h1>\n"
        f'      <p class="lede">{inline_md(component.description)}</p>\n'
        "    </header>\n"
        "\n"
        "    " + body.replace("\n", "\n    ").rstrip() + "\n"
        "\n"
        f'    <footer>Source: <code>{esc(component.source_file)}</code> · '
        f'<a href="https://github.com/{slug}">GitHub</a></footer>\n'
        "  </div>\n"
        + page_foot("../")
    )


def render_manifest(components: list[Component]) -> str:
    lines = [
        "# Preview media",
        "",
        "Drop one file per component here. Nothing is generated — if you want a preview,",
        "you record it and add it.",
        "",
        "Path: `docs/media/<slug>/preview.<ext>`",
        "",
        "Accepted extensions, in the order the generator prefers them:",
        "`.mp4` → `.webp` → `.png` → `.gif`",
        "",
        "Use `.mp4` for anything animated (the clock ticking, typing in the chat input) — a",
        "still image cannot show those. Keep clips short; they live in git history forever.",
        "",
        "| Component | Slug | Path | Status |",
        "|---|---|---|---|",
    ]
    for c in components:
        preview = c.preview
        status = f"present (`{preview.name}`)" if preview else "**missing**"
        lines.append(
            f"| `{c.name}` | `{c.slug}` | `docs/media/{c.slug}/preview.webp` | {status} |"
        )
    lines += [
        "",
        "After adding a file, run `python3 tools/generate_docs.py` (or push — CI does it)",
        "and the preview appears on that component's page.",
        "",
    ]
    return "\n".join(lines)


# ------------------------------------------------------------------------------- main


def main() -> int:
    if not LIB.is_dir():
        print(f"error: {LIB} not found", file=sys.stderr)
        return 1

    slug = repo_slug()
    components = parse_components()
    if not components:
        print("error: no components found", file=sys.stderr)
        return 1

    showcase = parse_showcase()
    for c in components:
        title = SHOWCASE_TITLE.get(c.slug)
        if title and title in showcase:
            c.demo = showcase[title]
        elif c.slug == "showcase":
            c.demo = None

    (DOCS / "components").mkdir(parents=True, exist_ok=True)
    MEDIA.mkdir(parents=True, exist_ok=True)

    (DOCS / "style.css").write_text(STYLE, encoding="utf-8")
    (DOCS / "app.js").write_text(APP_JS, encoding="utf-8")
    (DOCS / "index.html").write_text(render_index(components, slug), encoding="utf-8")

    written = []
    for c in components:
        path = DOCS / "components" / f"{c.slug}.html"
        path.write_text(render_component(c, slug), encoding="utf-8")
        written.append(path)

    (MEDIA / "MANIFEST.md").write_text(render_manifest(components), encoding="utf-8")

    missing = [c.slug for c in components if c.preview is None]
    print(f"generated {len(components)} component pages + index.html")
    for c in components:
        marks = []
        if c.usage:
            marks.append("usage")
        if c.demo:
            marks.append("demo")
        marks.append("preview" if c.preview else "no preview")
        print(f"  {c.slug:<14} {'/'.join(marks):<28} {len(c.params)} params")
    if missing:
        print(f"\nwaiting for preview media: {', '.join(missing)}")
        print("  see docs/media/MANIFEST.md")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
