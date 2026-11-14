---
version: 1
slug: "src-main-resources-web-index-html"
primary_target: "src/main/resources/web/index.html"
related_targets: ["src/main/resources/web/style.css"]
---

# PlaceWise web UI

Scope: the whole browser front end (`src/main/resources/web/`), served by `com.placewise.web.WebServer` with `--web`.
Mode: Operate. Audience: faculty evaluators reading a projector in a lit room; the student author presents.
Job: demonstrate all nine terminal features live; every verdict readable from the back row.
Constraints: plain HTML/CSS/vanilla JS, no build step, works offline (fonts self-hosted), code simple enough to
read line by line; the Java services decide everything, the page only displays.

## Direction contract

THESIS: Results are set like a mixtape's hand-lettered J-card: numbered tracklists split into hard halves, SIDE A
(eligible, selected, unlocked) and SIDE B (not eligible, blocked), each row's key figure right-aligned tight where a
running time would sit. Refuses the category default of KPI cards, slate text and status pills on a grey dashboard.

OWN-WORLD: Ballpoint blue #1E3A8A ink on inlay white #FCFBF7; faded blue rules; pencil grey for tentative (waitlisted)
entries; red pen #D32F2F only for gaps and failures; one printed red triple stripe atop a ballpoint-blue rail.
Hand-lettered block caps (Patrick Hand SC, self-hosted) for everything written in; a printed condensed sans for the
card's template (field labels, column heads, help). Filled blue primary buttons, outlined secondary, red caret.

STORY: The evaluator sees a student's figures, then which companies sit on Side A and exactly what each Side B
company still needs; the drive shows seats filling as fixed ruled lines that never overflow, except in the race demo.

FIRST VIEWPORT: Left blue rail with the stripe, wordmark and the nine numbered screens (keys 1-9). Main area: screen
title in hand caps at poster size, picker and primary action on the title row; below, the unfolded card: Side A and
Side B tracklists side by side, gap figures in red at the right margin.

FORM: Challenger world vernacular-ephemera-cassette-j-card, adopted by the user over the assigned direction (round 2
of the roll). seed:ce6a4a6352f11b36. Signature move: lists as tape sides, numbered, figure right-aligned; a company's
side holds exactly as many ruled lines as seats, the waitlist pencilled in below the end-of-tape line. Motion: rows
ink in left to right once when a result arrives; nothing else moves.

FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance
