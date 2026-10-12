#!/usr/bin/env python3

# SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
#
# SPDX-License-Identifier: EUPL-1.2

"""Generate a static accessibility review report from the a11y emulator captures.

Reads what the instrumented a11y tests write and scripts/a11y/capture.sh pulls to
app/build/a11y-snapshots, per screen <id> and variant (<id>, <id>.dark, <id>.land):

  <key>.semantics.txt   merged Compose semantics tree
  <key>.png             screenshot of the Compose root
  <key>.meta.json       description, density, window offset on screen
  <key>.atf.json        Accessibility Test Framework (ATF) results

and writes an HTML report with, per screen: screenshots with numbered markers, a plain-language
outline of what TalkBack gets (same numbers), ATF results linked to those numbers, a few extra
hints for what ATF doesn't cover, and a reviewer checklist. The raw semantics tree stays available
in a collapsed section.

Hints are candidates for a person to verify, not pass/fail results. Standard library only.

Usage: scripts/a11y/review.py [--snapshots DIR] [--out DIR]
"""

import argparse
import difflib
import html
import json
import re
import shutil
from dataclasses import dataclass, field
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent.parent
DEFAULT_SNAPSHOTS = REPO_ROOT / "app/build/a11y-snapshots"
DEFAULT_OUT = REPO_ROOT / "app/build/reports/a11y-review"
MANIFEST = REPO_ROOT / "app/src/main/AndroidManifest.xml"
VERSIONS = REPO_ROOT / "gradle/libs.versions.toml"

VARIANTS = {"": "Portrait, light", ".dark": "Portrait, dark", ".land": "Landscape"}
ATF_TYPES = ("ERROR", "WARNING", "INFO")
# An ATF element is linked to a numbered node when one box lies (almost) inside the other and
# they are of similar size. ATF reports the touch area, which Compose pads to 48 dp around a
# smaller visual node: a 40 dp icon button gives an overlap of only 0.69.
MIN_CONTAINMENT = 0.9
MIN_OVERLAP = 0.5

CHECKLIST = [
    "Every interactive element (button, switch, checkbox, clickable card) has a meaningful "
    "name: visible text or a content description.",
    "Touch targets are comfortably tappable (about 48 dp or larger).",
    "Reading order (the numbers) matches the visual order.",
    "Everything visible is in the outline, and nothing in the outline is invisible.",
    "The screen has a title exposed to accessibility (pane title or heading).",
    "Headings, if any, form a sensible outline.",
    "Form fields have labels, and error text is attached to the field, not just shown near it.",
    "Dark mode: all text and icons stay readable, and nothing disappears.",
    "Landscape: nothing overlaps or is cut off, and all content can be reached by scrolling.",
]

NODE_RE = re.compile(
    r"^(?P<prefix>.*?)(?:\|-)?Node at \(l=(?P<l>[-\d.]+), t=(?P<t>[-\d.]+), "
    r"r=(?P<r>[-\d.]+), b=(?P<b>[-\d.]+)\)px$"
)
PROP_RE = re.compile(r"^[\s|]*(?P<key>\w+) = (?P<value>.*)$")
SCROLL_MAX_RE = re.compile(r"maxValue=([\d.]+)")
INTERACTIVE_ROLES = {"Button", "Checkbox", "Switch", "RadioButton", "Tab", "DropdownList"}

# Rendering/plumbing details with no meaning for a TalkBack user. Everything else is shown,
# unknown properties included, so the outline never silently drops information.
NOISE_PROPS = {"Shape", "IsContainer", "MergeDescendants", "Focused"}
NOISE_ACTIONS = {
    "GetTextLayoutResult", "SetTextSubstitution", "ClearTextSubstitution",
    "ShowTextSubstitution", "RequestFocus", "ScrollByOffset",
}
GROUP_NOTE = ("<span class='muted'>– not announced itself; TalkBack reads everything inside "
              "before moving on</span>")
ACTION_WORDS = {
    "OnClick": "can be tapped",
    "OnLongClick": "can be long-pressed",
    "SetText": "text can be edited",
    "Expand": "can expand",
    "Collapse": "can collapse",
    "Dismiss": "can be dismissed",
    "ScrollBy": None,  # shown as "Scrollable area" instead
}
DESCRIBED_PROPS = {
    "Text", "ContentDescription", "EditableText", "Role", "StateDescription", "ToggleableState",
    "Selected", "Disabled", "Heading", "PaneTitle", "Error", "Password", "IsTraversalGroup",
    "VerticalScrollAxisRange", "HorizontalScrollAxisRange", "Actions", "TraversalIndex",
}


@dataclass
class Node:
    depth: int
    left: float
    top: float
    right: float
    bottom: float
    props: dict = field(default_factory=dict)
    children: list = field(default_factory=list)
    index: int = 0  # position in the printed tree; tie-breaker when sorting
    number: int | None = None  # estimated TalkBack position, matches the screenshot marker

    @property
    def width(self):
        return self.right - self.left

    @property
    def height(self):
        return self.bottom - self.top

    def prop(self, key):
        """Property value without the quotes/brackets printToString wraps it in."""
        value = self.props.get(key)
        if value is None:
            return None
        return value.strip("'").removeprefix("[").removesuffix("]")

    @property
    def name(self):
        return self.prop("ContentDescription") or self.prop("Text") or self.prop("EditableText")

    @property
    def actions(self):
        # Match whole names: every text node has SetTextSubstitution, which is not SetText.
        return {a.strip() for a in (self.prop("Actions") or "").split(",") if a.strip()}

    @property
    def is_text_field(self):
        return "SetText" in self.actions

    @property
    def is_interactive(self):
        return (
            "OnClick" in self.actions
            or self.prop("Role") in INTERACTIVE_ROLES
            or "ToggleableState" in self.props
            or self.is_text_field
        )

    @property
    def is_heading(self):
        return "Heading" in self.props

    @property
    def scroll_axis(self):
        for key, axis in (("VerticalScrollAxisRange", "vertical"),
                          ("HorizontalScrollAxisRange", "horizontal")):
            if key in self.props:
                match = SCROLL_MAX_RE.search(self.props[key])
                return axis, bool(match and float(match.group(1)) > 0)
        return None

    @property
    def is_group(self):
        return self.prop("IsTraversalGroup") == "true"

    @property
    def is_focusable(self):
        """Can TalkBack land on it? Mirrors Compose's isScreenReaderFocusable for the merged tree:
        merging nodes (buttons, cards), or leaves that have something to say."""
        if "InvisibleToUser" in self.props or "HideFromAccessibility" in self.props:
            return False
        speaks = self.name or self.prop("StateDescription") or "ToggleableState" in self.props
        return self.prop("MergeDescendants") == "true" or (not self.children and bool(speaks))

    @property
    def traversal_index(self):
        try:
            return float(self.prop("TraversalIndex") or 0)
        except ValueError:
            return 0.0

    @property
    def other_props(self):
        return {k: v for k, v in self.props.items()
                if k not in DESCRIBED_PROPS and k not in NOISE_PROPS}

    @property
    def is_structural(self):
        """Pure layout: nothing a TalkBack user would notice, so the outline folds it away."""
        return not (self.name or self.is_interactive or self.is_heading or self.is_group
                    or self.scroll_axis or "PaneTitle" in self.props or self.other_props)

    @property
    def role_label(self):
        if self.prop("Role"):
            return self.prop("Role")
        if self.is_text_field:
            return "Text field"
        if self.is_interactive:
            return "Clickable"
        return "Heading" if self.is_heading else "Text"

    @property
    def state(self):
        parts = [
            self.prop("StateDescription"),
            self.prop("ToggleableState"),
            "selected" if self.prop("Selected") == "true" else None,
            "disabled" if "Disabled" in self.props else None,
            "password" if "Password" in self.props else None,
        ]
        return ", ".join(p for p in parts if p)


def parse_tree(text):
    """Parse Compose printToString() output; returns the root and all nodes in tree order."""
    nodes, stack = [], []
    for line in text.splitlines():
        match = NODE_RE.match(line)
        if match:
            prefix = match.group("prefix")
            # Root has no prefix; each nesting level adds three columns (" |-", "   ", " | ").
            depth = 0 if not prefix and not line.startswith(" ") else (len(prefix) + 2) // 3
            node = Node(depth, *(float(match.group(k)) for k in "ltrb"))
            while stack and stack[-1].depth >= depth:
                stack.pop()
            if stack:
                stack[-1].children.append(node)
            stack.append(node)
            nodes.append(node)
            continue
        prop = PROP_RE.match(line)
        if prop and nodes:
            nodes[-1].props[prop.group("key")] = prop.group("value")
    if not nodes:
        return None, [], [], []
    for i, n in enumerate(nodes):
        n.index = i
    entries = talkback_order([nodes[0]])
    spoken = [e.node for e in flatten(entries) if e.node.is_focusable]
    for i, n in enumerate(spoken, start=1):
        n.number = i
    return nodes[0], nodes, spoken, entries


# --- Estimated TalkBack order -------------------------------------------------------------------
# A port of Compose UI's SemanticsSort.kt (subtreeSortedByGeometryGrouping, 1.11), which decides
# the order TalkBack moves through on swipe. It runs on the merged tree the tests print, so it is
# an estimate: confirm on a device.

@dataclass
class Entry:
    node: Node
    children: list | None = None  # set for traversal groups: their contents, already sorted


def talkback_order(nodes_to_sort):
    containers = {}
    geometry = []
    for n in nodes_to_sort:
        _collect(n, geometry, containers)
    return _sort_by_rows(geometry, containers)


def _collect(node, geometry, containers):
    """Traversal groups and focusable nodes are sorted at this level; other nodes are see-through."""
    if (node.is_group or node.is_focusable) and node.width > 0 and node.height > 0:
        geometry.append(node)
    if node.is_group:
        containers[node.index] = talkback_order(node.children)
    else:
        for child in node.children:
            _collect(child, geometry, containers)


def _sort_by_rows(items, containers):
    rows = []  # [top, bottom, [nodes]]: nodes whose vertical ranges overlap share a row
    for i, n in enumerate(items):
        row = next((r for r in rows if i > 0 and n.top < n.bottom and r[0] < r[1]
                    and max(n.top, r[0]) < min(n.bottom, r[1])), None)
        if row:
            row[0], row[1] = max(row[0], n.top), min(row[1], n.bottom)
            row[2].append(n)
        else:
            rows.append([n.top, n.bottom, [n]])
    rows.sort(key=lambda r: (r[0], r[1]))
    ordered = [n for r in rows
               for n in sorted(r[2], key=lambda n: (n.left, n.top, n.bottom, n.right, n.index))]
    ordered.sort(key=lambda n: n.traversal_index)  # stable, like Kotlin's sortWith
    return [Entry(n, containers.get(n.index)) for n in ordered]


def flatten(entries):
    for e in entries:
        yield e
        if e.children:
            yield from flatten(e.children)


def unreached(nodes, entries):
    """Meaningful nodes TalkBack never lands on: they'd otherwise vanish from the outline."""
    placed = {e.node.index for e in flatten(entries)}
    return [n for n in nodes[1:] if n.index not in placed and not n.is_structural
            and not n.is_group]


def hints_for(root, nodes, spoken, entries):
    """Checks ATF has no equivalent for. Names, touch targets, labels and contrast are left to
    ATF, which measures them on the device."""
    hints = []
    for n in unreached(nodes, entries):
        label = f"“{n.name}”" if n.name else f"at {describe_position(n, root)}"
        hints.append(f"{n.role_label} {label} is in the tree but TalkBack does not land on it.")
    for n in spoken:
        label = f"“{n.name}”" if n.name else f"at {describe_position(n, root)}"
        if n.is_interactive and not n.prop("Role") and not n.is_text_field:
            hints.append(f"Clickable element {label} has no role, so TalkBack cannot say what "
                         "kind of control it is (for example “button”).")
    if not any(n.is_heading for n in nodes):
        hints.append("No element is marked as a heading, so TalkBack heading navigation finds "
                     "nothing.")
        if not any("PaneTitle" in n.props for n in nodes):
            hints.append("No pane title or heading, so the screen has no title exposed to "
                         "accessibility.")
    return hints


def describe_position(node, root):
    cx, cy = node.left - root.left + node.width / 2, node.top - root.top + node.height / 2
    horizontal = "left" if cx < root.width / 3 else "right" if cx > root.width * 2 / 3 else "centre"
    vertical = "top" if cy < root.height / 3 else "bottom" if cy > root.height * 2 / 3 else "middle"
    return f"{vertical} {horizontal}"


def esc(value):
    return html.escape(str(value), quote=True)


def describe_node(n, density):
    """One outline line: what TalkBack gets for this element, in words."""
    scroll = n.scroll_axis
    if scroll and not n.name:
        axis, scrolls = scroll
        more = "more content beyond the screen" if scrolls else "all content fits"
        group = " group" if n.is_group else ""
        return (f"<strong>Scrollable{group}</strong> ({axis}, {more}) {GROUP_NOTE if group else ''}")
    if n.is_group and not n.name and not n.is_interactive:
        return f"<strong>Group</strong> {GROUP_NOTE}"

    parts = [f"<strong>{esc(n.role_label)}</strong>"]
    if n.name:
        parts.append(f"“{esc(n.name)}”")
    elif n.is_interactive:
        parts.append('<span class="bad">no name</span>')
    if n.state:
        parts.append(esc(n.state))
    if n.is_interactive:
        parts.append(f"{n.width / density:.0f}×{n.height / density:.0f} dp")
    words = [ACTION_WORDS[a] for a in sorted(n.actions) if ACTION_WORDS.get(a)]
    unknown = sorted(a for a in n.actions if a not in ACTION_WORDS and a not in NOISE_ACTIONS)
    parts += words + [f"action {esc(a)}" for a in unknown]
    if "PaneTitle" in n.props:
        parts.append(f"pane title “{esc(n.prop('PaneTitle'))}”")
    if n.prop("Error"):
        parts.append(f'<span class="bad">error “{esc(n.prop("Error"))}”</span>')
    parts += [f"other: {esc(k)} = {esc(v)}" for k, v in n.other_props.items()]
    return " · ".join(parts)


def render_outline(entries, density):
    """Nested list in estimated TalkBack order; groups contain their (sorted) contents."""
    items = []
    for e in entries:
        badge = f'<span class="num">{e.node.number}</span> ' if e.node.number else ""
        nested = f"<ul>{render_outline(e.children, density)}</ul>" if e.children else ""
        items.append(f"<li>{badge}{describe_node(e.node, density)}{nested}</li>")
    return "".join(items)


def render_screenshot(png, root, spoken, alt, assets_dir, markers=True):
    if not png.exists():
        return "<p>No screenshot found.</p>"
    shutil.copyfile(png, assets_dir / png.name)
    spans = "".join(
        # Top-left corner of the element, so the marker doesn't cover its text.
        f'<span class="marker" style="left:{(n.left - root.left) / root.width * 100:.2f}%;'
        f'top:{(n.top - root.top) / root.height * 100:.2f}%">{n.number}</span>'
        for n in spoken if markers and n.top < root.bottom and n.left < root.right
    )
    return (f'<a class="shot" href="assets/{esc(png.name)}"><img src="assets/{esc(png.name)}" '
            f'alt="{esc(alt)}"><span aria-hidden="true">{spans}</span></a>')


def render_view(capture, density, shots_html, layout):
    scrolls = any(n.scroll_axis and n.scroll_axis[1] for n in capture.nodes)
    missed = unreached(capture.nodes, capture.entries)
    not_reached = ("<h5>Not reached by TalkBack</h5><ul class='outline'>"
                   + "".join(f"<li>{describe_node(n, density)}</li>" for n in missed)
                   + "</ul>") if missed else ""
    scroll_note = ("<p class='note'>This screen scrolls: the screenshot shows only the visible "
                   "part. Elements beyond it are in the outline but have no marker.</p>"
                   if scrolls else "")
    return f"""
  <div class="view {layout}">
    <div class="shots">{shots_html}</div>
    <div>
      <h4>What TalkBack gets, in estimated order</h4>
      <p class="note">Numbers are the order TalkBack moves through when swiping, estimated with
      Compose's own ordering rules: top to bottom, left to right, and a group is finished before
      moving on. They match the markers. Confirm on a device. Pure layout is left out.</p>
      {scroll_note}
      <ul class="outline">{render_outline(capture.entries, density)}</ul>
      {not_reached}
      <details>
        <summary>Raw semantics tree (for specialists)</summary>
        <pre>{esc(capture.text)}</pre>
      </details>
    </div>
  </div>"""


@dataclass
class Capture:
    """Everything captured for one screen in one variant."""
    text: str
    root: Node | None
    nodes: list
    spoken: list
    entries: list
    meta: dict
    atf: dict | None
    png: Path

    @property
    def window_offset(self):
        return tuple(self.meta.get("windowOffset", (0, 0)))


def load_json(path):
    return json.loads(path.read_text(encoding="utf-8")) if path.exists() else None


def load_capture(snapshots, key):
    tree = snapshots / f"{key}.semantics.txt"
    if not tree.exists():
        return None
    text = tree.read_text(encoding="utf-8")
    root, nodes, spoken, entries = parse_tree(text)
    return Capture(text, root, nodes, spoken, entries,
                   load_json(snapshots / f"{key}.meta.json") or {},
                   load_json(snapshots / f"{key}.atf.json"), snapshots / f"{key}.png")


def overlap(a, b):
    """(containment, intersection over union) of two (left, top, right, bottom) boxes, where
    containment is how much of the smaller box lies inside the other."""
    width = min(a[2], b[2]) - max(a[0], b[0])
    height = min(a[3], b[3]) - max(a[1], b[1])
    area_a = (a[2] - a[0]) * (a[3] - a[1])
    area_b = (b[2] - b[0]) * (b[3] - b[1])
    if width <= 0 or height <= 0 or area_a <= 0 or area_b <= 0:
        return 0.0, 0.0
    inter = width * height
    return inter / min(area_a, area_b), inter / (area_a + area_b - inter)


def atf_box(result, capture):
    """ATF bounds are in screen coordinates; the semantics tree is in window coordinates."""
    bounds = result.get("bounds")
    if not bounds:
        return None
    dx, dy = capture.window_offset
    return bounds[0] - dx, bounds[1] - dy, bounds[2] - dx, bounds[3] - dy


def atf_node(box, capture):
    """The numbered node an ATF result is about: (node, True) when the bounds match, or
    (node, False) for the smallest numbered node it lies inside, such as text merged into a
    card. (None, False) when neither."""
    if box is None:
        return None, False
    scored = [(overlap(box, (n.left, n.top, n.right, n.bottom)), n) for n in capture.spoken]
    (containment, iou), node = max(scored, key=lambda s: s[0], default=((0.0, 0.0), None))
    if containment >= MIN_CONTAINMENT and iou >= MIN_OVERLAP:
        return node, True
    area = (box[2] - box[0]) * (box[3] - box[1])
    containers = [n for (c, _), n in scored
                  if c >= MIN_CONTAINMENT and n.width * n.height >= area]
    if containers:
        return min(containers, key=lambda n: n.width * n.height), False
    return None, False


def atf_element(result, box, capture):
    name = result.get("text") or result.get("contentDescription")
    if name:
        return f"“{name}”"
    kind = (result.get("className") or "element").rsplit(".", 1)[-1]
    if box is None or capture.root is None:
        return kind
    probe = Node(0, *box)
    return f"{kind} at {describe_position(probe, capture.root)}"


def render_atf(capture):
    atf = capture.atf
    if atf is None:
        return "<p class='note'>No ATF results were captured for this view.</p>"
    order = {t: i for i, t in enumerate(ATF_TYPES)}
    results = sorted(atf.get("results", []),
                     key=lambda r: (order.get(r["type"], len(order)), r["check"]))
    warn = ""
    if atf.get("elementCount", 0) < len(capture.nodes) / 2:
        warn = (f"<p class='note warn'>ATF saw {atf.get('elementCount', 0)} elements but the "
                f"semantics tree has {len(capture.nodes)} nodes, so it probably did not evaluate "
                "the Compose content. Don't read few results as few problems.</p>")
    if not atf.get("screenshot"):
        warn += ("<p class='note warn'>No screenshot reached ATF, so its contrast checks did not "
                 "run.</p>")
    rows = []
    for r in results:
        box = atf_box(r, capture)
        node, exact = atf_node(box, capture)
        number = ""
        if node and exact:
            number = f'<span class="num">{node.number}</span> '
        elif node:
            number = f'<span class="muted">in</span> <span class="num">{node.number}</span> '

        help_link = (f' <a href="{esc(r["helpUrl"])}">More</a>' if r.get("helpUrl") else "")
        rows.append(
            f'<tr><td><span class="atf {esc(r["type"].lower())}">{esc(r["type"])}</span></td>'
            f'<td>{number}{esc(atf_element(r, box, capture))}</td>'
            f'<td><code>{esc(r["check"])}</code></td>'
            f'<td>{esc(r["message"])}{help_link}</td></tr>')
    counts = ", ".join(f"{sum(r['type'] == t for r in results)} {t.lower()}" for t in ATF_TYPES)
    summary = (f"<p class='note'>{counts}. {len(atf.get('checks', []))} checks ran; "
               f"{atf.get('notRun', 0)} “not applicable” results are left out.</p>")
    table = ("<div class='table'><table class='atf-results'><thead><tr><th>Type</th>"
             "<th>Element</th><th>Check</th><th>Message</th></tr></thead><tbody>"
             + "".join(rows) + "</tbody></table></div>") if rows else "<p>No results.</p>"
    return warn + summary + table


def render_screen(screen_id, snapshots, assets_dir, portrait_locked):
    captures = {suffix: load_capture(snapshots, screen_id + suffix) for suffix in VARIANTS}
    main, dark, land = captures[""], captures[".dark"], captures[".land"]
    meta = main.meta if main else {}
    description = meta.get("description", screen_id)
    if main is None or main.root is None:
        return f'<article class="screen"><h2>{esc(description)}</h2><p>The tree is empty: ' \
               f'nothing is exposed to accessibility services.</p></article>'
    density = float(meta.get("density", 3.0))

    hints = hints_for(main.root, main.nodes, main.spoken, main.entries)
    tree_diff = ""
    if dark and dark.text != main.text:
        diff = list(difflib.unified_diff(main.text.splitlines(), dark.text.splitlines(),
                                         "light", "dark", lineterm=""))
        changed = sum(1 for line in diff[2:] if line[:1] in "+-")
        hints.append(f"Dark mode changes the semantics tree ({changed} lines differ), so TalkBack "
                     "users get something different in dark mode. Colours alone should not do "
                     "that. The diff is under the dark ATF results.")
        tree_diff = ("<details><summary>Semantics tree, light vs dark</summary>"
                     f"<pre>{esc(chr(10).join(diff))}</pre></details>")

    shots = render_screenshot(main.png, main.root, main.spoken,
                              f"Screenshot, light theme: {description}", assets_dir)
    if dark and dark.png.exists():
        shots += render_screenshot(dark.png, dark.root, dark.spoken,
                                   f"Screenshot, dark theme: {description}", assets_dir,
                                   markers=False)
    views = [f"<h3>Portrait{' · light and dark' if dark else ''}</h3>"
             + render_view(main, density, shots, "portrait")]

    if land and land.root is not None:
        hints += [f"Landscape: {h}"
                  for h in hints_for(land.root, land.nodes, land.spoken, land.entries)
                  if h not in hints]
        lock = ("<p class='note warn'>The app is locked to portrait "
                "(<code>android:screenOrientation=\"portrait\"</code>), so users cannot see this "
                "today. WCAG 1.3.4 asks that orientation is not restricted unless essential.</p>"
                if portrait_locked else "")
        views.append("<h3>Landscape</h3>" + lock + render_view(
            land, density,
            render_screenshot(land.png, land.root, land.spoken,
                              f"Screenshot, landscape: {description}", assets_dir),
            "landscape"))

    atf_sections = "".join(
        f"<h4>{esc(VARIANTS[suffix])}</h4>" + render_atf(capture)
        + (tree_diff if suffix == ".dark" else "")
        for suffix, capture in captures.items() if capture is not None)

    headings = [n.name for n in main.nodes if n.is_heading]
    pane_titles = [n.prop("PaneTitle") for n in main.nodes if "PaneTitle" in n.props]
    title = ", ".join(pane_titles or headings) or "(none found)"
    hint_items = "".join(f"<li>{esc(h)}</li>" for h in hints) or "<li>None.</li>"
    checklist = "".join(f'<li><label><input type="checkbox"> {esc(item)}</label></li>'
                        for item in CHECKLIST)
    sid = esc(screen_id)
    return f"""
<article class="screen" id="{sid}" aria-labelledby="{sid}-h">
  <h2 id="{sid}-h">{esc(description)} <code>{sid}</code></h2>
  <dl class="meta">
    <div><dt>Accessibility title</dt><dd>{esc(title)}</dd></div>
    <div><dt>Density</dt><dd>{density:g}×</dd></div>
  </dl>
  {"".join(views)}
  <h3>Accessibility Test Framework results</h3>
  <p class="note">Google's ATF, the same checks as Accessibility Scanner, run on the emulator. A
  number links a result to the outline and markers above. Errors are likely real problems;
  warnings and info need a person to judge.</p>
  {atf_sections}
  <h3>Other hints (to verify)</h3>
  <p class="note">Checks ATF does not do, made from the semantics tree.</p>
  <ul class="hints">{hint_items}</ul>
  <h3>Reviewer checklist</h3>
  <ul class="checklist">{checklist}</ul>
</article>"""


PAGE = """<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Accessibility review – Wallet Android</title>
<style>
  :root {{ color-scheme: light; --line: #d0d7de; --muted: #57606a; }}
  body {{ font-family: system-ui, sans-serif; margin: 0; padding: 1rem 1.5rem 4rem;
    background: #f6f8fa; color: #1f2328; line-height: 1.45; }}
  header p {{ max-width: 75ch; }}
  nav, .screen {{ background: #fff; border: 1px solid var(--line); border-radius: 8px;
    padding: 1rem 1.5rem; margin-bottom: 1.5rem; }}
  h2 code {{ font-size: 0.8rem; font-weight: 400; color: var(--muted); margin-left: 0.5rem; }}
  h3 {{ border-top: 1px solid var(--line); padding-top: 1rem; margin-top: 1.5rem; }}
  .meta {{ display: flex; flex-wrap: wrap; gap: 0.5rem 2rem; font-size: 0.9rem; margin: 0; }}
  .meta div {{ display: flex; gap: 0.4rem; }}
  .meta dt {{ font-weight: 600; }}
  .meta dd {{ margin: 0; }}
  .hints {{ padding: 0; }}
  .hints li {{ background: #fff8c5; border-left: 4px solid #bf8700; padding: 0.3rem 0.6rem;
    margin: 0.3rem 0; list-style: none; }}
  .view {{ display: grid; gap: 1.5rem; align-items: start; }}
  .view > * {{ min-width: 0; }}
  .view.portrait {{ grid-template-columns: minmax(0, 520px) 1fr; }}
  .view.portrait .shots {{ display: grid; grid-template-columns: 1fr 1fr; gap: 0.5rem; }}
  .view.landscape .shots {{ max-width: 820px; }}
  .shot {{ position: relative; display: block; }}
  .shot img {{ display: block; width: 100%; border: 1px solid var(--line); border-radius: 6px; }}
  .marker, .num {{ display: inline-block; min-width: 1.3rem; height: 1.3rem; line-height: 1.3rem;
    border-radius: 999px; background: #0969da; color: #fff; font-size: 0.75rem;
    font-weight: 700; text-align: center; }}
  .marker {{ position: absolute; transform: translate(-35%, -35%); box-shadow: 0 0 0 2px #fff; }}
  .outline, .outline ul {{ list-style: none; padding-left: 1.1rem; margin: 0.2rem 0; }}
  .outline {{ padding-left: 0; }}
  .outline li {{ margin: 0.35rem 0; }}
  .outline ul {{ border-left: 2px solid var(--line); }}
  .bad {{ color: #cf222e; font-weight: 600; }}
  .table {{ overflow-x: auto; }}
  .atf-results {{ border-collapse: collapse; width: 100%; font-size: 0.9rem; }}
  .atf-results th, .atf-results td {{ border-bottom: 1px solid var(--line); padding: 0.35rem 0.5rem;
    text-align: left; vertical-align: top; }}
  .atf {{ display: inline-block; padding: 0 0.4rem; border-radius: 4px; font-size: 0.75rem;
    font-weight: 700; }}
  .atf.error {{ background: #ffebe9; color: #a40e26; }}
  .atf.warning {{ background: #fff8c5; color: #7d4e00; }}
  .atf.info {{ background: #ddf4ff; color: #0550ae; }}
  .muted, .note {{ color: var(--muted); }}
  .note {{ font-size: 0.85rem; }}
  .warn {{ background: #fff1e5; border-left: 4px solid #bc4c00; padding: 0.4rem 0.6rem;
    color: #1f2328; }}
  pre {{ background: #0d1117; color: #e6edf3; padding: 0.75rem; border-radius: 6px;
    overflow: auto; max-height: 28rem; font-size: 0.75rem; }}
  summary {{ cursor: pointer; font-weight: 600; margin-top: 1rem; }}
  .checklist {{ list-style: none; padding: 0; }}
  .checklist li {{ margin: 0.3rem 0; }}
  @media (max-width: 900px) {{
    body {{ padding: 1rem 16px 3rem; }}
    nav, .screen {{ padding: 1rem; }}
    .view.portrait {{ grid-template-columns: 1fr; }}
  }}
</style>
</head>
<body>
<header>
  <h1>Accessibility review – Wallet Android</h1>
  <p>Generated by <code>scripts/a11y/review.py</code> from the emulator captures in
  <code>app/build/a11y-snapshots/</code> (<code>just a11y-review</code>). For each screen:
  results from the Accessibility Test Framework {atf_version}, screenshots (light, dark and
  landscape where captured), a plain-language outline of what TalkBack gets, a few extra hints and
  a checklist. Hints are candidates for a person to confirm, not failures.</p>
  <p class="note">The outline leaves out only rendering details that TalkBack users never notice
  (shape, focus state, text-layout and selection actions). Any other property is listed as
  “other”, and the full raw tree is under each outline.</p>
</header>
<nav aria-labelledby="toc-h">
  <h2 id="toc-h">Screens</h2>
  <ul>{toc}</ul>
</nav>
<main>{screens}</main>
</body>
</html>
"""


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--snapshots", type=Path, default=DEFAULT_SNAPSHOTS)
    parser.add_argument("--out", type=Path, default=DEFAULT_OUT)
    args = parser.parse_args()

    variant_suffixes = tuple(f"{s}.semantics.txt" for s in VARIANTS if s)
    screen_ids = sorted(p.name.removesuffix(".semantics.txt")
                        for p in args.snapshots.glob("*.semantics.txt")
                        if not p.name.endswith(variant_suffixes))
    if not screen_ids:
        parser.error(f"no *.semantics.txt files in {args.snapshots}; run `just a11y-review`")

    portrait_locked = MANIFEST.exists() and 'screenOrientation="portrait"' in MANIFEST.read_text()
    assets = args.out / "assets"
    assets.mkdir(parents=True, exist_ok=True)
    sections = [render_screen(s, args.snapshots, assets, portrait_locked) for s in screen_ids]
    toc = "".join(f'<li><a href="#{esc(s)}">{esc(s)}</a></li>' for s in screen_ids)
    atf = re.search(r'^accessibilityTestFramework = "([^"]+)"', VERSIONS.read_text(), re.M) \
        if VERSIONS.exists() else None
    atf_version = esc(atf.group(1)) if atf else ""
    out_file = args.out / "index.html"
    out_file.write_text(PAGE.format(toc=toc, screens="\n".join(sections),
                                    atf_version=atf_version), encoding="utf-8")
    print(f"Wrote {out_file} ({len(screen_ids)} screens)")


if __name__ == "__main__":
    main()
