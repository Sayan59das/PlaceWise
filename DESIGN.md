---
name: PlaceWise
description: A hand-lettered cassette J-card for placement results, with lists set as tape sides.
colors:
  ballpoint-blue: "#1E3A8A"
  pressed-ballpoint: "#172E6E"
  faded-ballpoint: "#4A62A6"
  blue-rule-strong: "#6B86C5"
  blue-rule: "#C9D3EC"
  blue-wash: "#EEF2FB"
  inlay-white: "#FCFBF7"
  card-white: "#FFFFFF"
  printed-ink: "#1B2233"
  pencil-grey: "#6B6B6B"
  pencil-rule: "#A6A6A6"
  red-pen: "#D32F2F"
  rail-ink: "#E8EDF8"
  rail-ink-soft: "#AFC0E6"
typography:
  page-title:
    fontFamily: "Patrick Hand SC, Segoe Print, cursive"
    fontSize: "3.1rem"
    fontWeight: 400
    lineHeight: 1
  strip-name:
    fontFamily: "Patrick Hand SC, Segoe Print, cursive"
    fontSize: "2.7rem"
    fontWeight: 400
    lineHeight: 1.05
  side-head:
    fontFamily: "Patrick Hand SC, Segoe Print, cursive"
    fontSize: "1.85rem"
    fontWeight: 400
    lineHeight: 1.15
  track:
    fontFamily: "Patrick Hand SC, Segoe Print, cursive"
    fontSize: "1.45rem"
    fontWeight: 400
    lineHeight: 1.25
  body:
    fontFamily: "Patrick Hand SC, Segoe Print, cursive"
    fontSize: "1.25rem"
    fontWeight: 400
    lineHeight: 1.45
  figure:
    fontFamily: "Bahnschrift, Arial Narrow, Segoe UI, system-ui, sans-serif"
    fontSize: "1.35rem"
    fontWeight: 600
    lineHeight: 1.25
  fact-figure:
    fontFamily: "Bahnschrift, Arial Narrow, Segoe UI, system-ui, sans-serif"
    fontSize: "2rem"
    fontWeight: 700
  sentence:
    fontFamily: "Bahnschrift, Arial Narrow, Segoe UI, system-ui, sans-serif"
    fontSize: "1.2rem"
    fontWeight: 400
    lineHeight: 1.4
  label:
    fontFamily: "Bahnschrift, Arial Narrow, Segoe UI, system-ui, sans-serif"
    fontSize: "1.1rem"
    fontWeight: 400
    lineHeight: 1.2
rounded:
  sm: "4px"
  md: "6px"
  lg: "8px"
spacing:
  xs: "0.5rem"
  sm: "0.75rem"
  md: "1.25rem"
  lg: "1.75rem"
  xl: "2.75rem"
components:
  button:
    backgroundColor: "{colors.card-white}"
    textColor: "{colors.ballpoint-blue}"
    typography: "{typography.body}"
    rounded: "{rounded.md}"
    padding: "0.55rem 1.2rem"
    height: "2.75rem"
  button-hover:
    backgroundColor: "{colors.blue-wash}"
  button-primary:
    backgroundColor: "{colors.ballpoint-blue}"
    textColor: "{colors.inlay-white}"
    rounded: "{rounded.md}"
  button-primary-hover:
    backgroundColor: "{colors.pressed-ballpoint}"
  button-danger:
    backgroundColor: "{colors.card-white}"
    textColor: "{colors.red-pen}"
    rounded: "{rounded.md}"
  field:
    backgroundColor: "{colors.card-white}"
    textColor: "{colors.ballpoint-blue}"
    rounded: "{rounded.md}"
    padding: "0.45rem 0.75rem"
    height: "2.75rem"
  sheet:
    backgroundColor: "{colors.card-white}"
    rounded: "{rounded.lg}"
    padding: "1.25rem 1.5rem 1.4rem"
  tape-side:
    backgroundColor: "{colors.card-white}"
    textColor: "{colors.ballpoint-blue}"
    rounded: "{rounded.lg}"
    padding: "1rem 1.25rem 1.2rem"
  track-pencil:
    textColor: "{colors.pencil-grey}"
  rail:
    backgroundColor: "{colors.ballpoint-blue}"
    textColor: "{colors.rail-ink}"
    width: "15rem"
---

# Design System: PlaceWise

## Overview

**Creative North Star: "The Hand-Lettered J-Card"**

Results are set like the inlay of a mixtape: a numbered tracklist split into hard halves, SIDE A (eligible, selected, unlocked) and SIDE B (not eligible, blocked), each row's key figure right-aligned where a running time would sit. The page is inlay-white card stock; everything written in is ballpoint blue; the waitlist is pencilled in grey; red pen appears only for gaps and failures. A printed red triple stripe on the ballpoint-blue rail is the one piece of pure ornament, and it is the brand mark.

Two hands share the card. Block-cap hand lettering (Patrick Hand SC) is the written-in content: names, titles, headings, buttons, inputs. A printed condensed sans (Bahnschrift) is the card's template: labels, sentences, and every figure that matters. In the build, sentences and figures were moved off the hand face because hand-lettered digits and long lines read oddly from the back of a room (the surface is shown on a projector). Density is moderate and legibility comes first: an 18px root and a 1.05-1.2rem floor for secondary text.

**Key Characteristics:**
- Lists are tape sides: numbered, ruled, figure right-aligned.
- Colour tells the story: blue written, grey tentative, red wrong.
- Flat, ruled, bordered; no panel shadow.
- Hand face for what is written in; printed face for sentences and numbers.
- Rows ink in left to right, once.

## Colors

A ballpoint-on-paper palette: one blue in several pressures, warm white stock, pencil grey and a single red.

### Primary
- **Ballpoint Blue** (#1E3A8A): everything written in: text, borders of sheets and sides, the title rule, primary button fill, the rail.
- **Pressed Ballpoint** (#172E6E): hover and pressed fill on the primary button and rail links; selection text.
- **Faded Ballpoint** (#4A62A6): secondary text, row numbers, input borders, read-only values.

### Secondary
- **Red Pen** (#D32F2F): gaps, failures, struck-through unmet criteria, danger buttons, the rail stripe, the caret, and the oversold part of the capacity bar.

### Neutral
- **Inlay White** (#FCFBF7): page background, read-only fields, log blocks.
- **Card White** (#FFFFFF): sheets, tape sides, inputs, secondary buttons.
- **Printed Ink** (#1B2233): the template's printed text: labels, the capacity limit line.
- **Pencil Grey** (#6B6B6B) and **Pencil Rule** (#A6A6A6): tentative entries (waitlist rows), placeholders, empty states, disabled buttons, the dashed end-of-seats rule.
- **Blue Rule** (#C9D3EC), **Blue Rule Strong** (#6B86C5), **Blue Wash** (#EEF2FB): row dividers, focus ring and scrollbar, hover wash.
- **Rail Ink** (#E8EDF8) and **Rail Ink Soft** (#AFC0E6): text on the blue rail.

### Named Rules
**The Red Pen Rule.** Red marks a gap or a failure and nothing else; the one exception is the stripe on the rail.

**The Pencil Rule.** Tentative means grey and dashed: waitlisted rows take pencil text and a dashed divider.

**The Blue Is Written Rule.** Content someone entered or the system wrote is blue; template text is printed ink.

## Typography

**Display Font:** Patrick Hand SC (self-hosted woff2, with Segoe Print, cursive)
**Body Font:** Patrick Hand SC for written-in content
**Label/Mono Font:** Bahnschrift (with Arial Narrow, Segoe UI, system-ui, sans-serif) for the printed template; tabular figures

**Character:** Hand-lettered block caps against a printed condensed sans, like a hand-filled J-card. Headings are weight 400 (the hand face has one weight); the printed face carries 600-700 for figures.

### Hierarchy
- **Page title** (400, 3.1rem, 1): one per screen, on a 2px blue rule. 2.6rem under 1366px, 2.3rem under 860px.
- **Strip name** (400, 2.7rem, 1.05): the chosen student or company; its figures are printed bold 1.9rem.
- **Side head** (400, 1.85rem, 1.15): tape side title, with the count right-aligned at 1.35rem.
- **Track** (400, 1.45rem, 1.25): row title in the hand face.
- **Figure** (600, 1.35rem, printed, tabular): the right-aligned key figure. Fact figures are 700 at 2rem.
- **Sentence** (400, 1.2rem, 1.4, printed): row detail, reasons, verdicts (1.4rem), notes (1.15rem).
- **Label** (400-600, 1.1rem, printed): field labels, table headers, counts, the end-of-seats line.
- **Body** (400, 1.25rem, 1.45, hand): inputs and default text. Root is 112.5% (18px); 100% under 860px.

### Named Rules
**The Two Hands Rule.** Written-in names, headings and controls take the hand face; sentences and any figure a reader must compare take the printed face with tabular numerals.

**The Projector Floor Rule.** No text below 1.05rem on the 18px root.

## Layout

A two-column shell: a sticky full-height blue rail (15rem; 14rem under 1366px) and a main area padded 2rem 2.75rem 5rem. Screens open on a title row (title left, controls right, 2px blue rule below), then stacked sheets. Lists sit in auto-fill grids (minimum 18.5rem per side), or as the opened J-card: two equal sides joined by a 2px blue fold. Rhythm runs on 0.75, 1.25 and 1.75rem steps between blocks. Single-side result lists cap at 42rem.

Breakpoints: 1366px tightens the rail, padding, headings and row padding so a 1280-wide projector shows the whole card above the fold, and long emails truncate to keep row actions visible; 1100px stacks the two-column layout; 860px drops the root to 16px, turns the rail into a top bar with a scrolling nav, and moves each figure under its row title.

## Elevation & Depth

Flat. Depth comes from borders (1.5-2px ballpoint blue), the 2px fold between sides, and ruled lines, not shadow. The only shadow is the floating message toast (`box-shadow: 0 8px 24px rgba(30, 58, 138, .18)`), a blue-tinted ambient lift because it overlays content.

### Named Rules
**The Flat Card Rule.** Panels sit on the page with a border and no shadow; only the transient message floats.

## Shapes

Gently rounded, never pill-shaped: 4px on small controls and the capacity bar, 6px on buttons, inputs and nav links, 8px on sheets and tape sides. A segmented control squares its inner corners. Rules are 1px blue-rule for rows, 2px ballpoint for heads and headers, 2px dashed pencil for the end of seats.

## Components

### Buttons
- **Shape:** 6px radius, 2px ballpoint border, min-height 2.75rem (small 2.2rem), hand face 1.3rem.
- **Default:** card-white fill, blue text; hover blue wash.
- **Primary:** ballpoint fill, inlay-white text; hover pressed ballpoint.
- **Danger:** red border and text; the confirm state fills red.
- **Disabled:** pencil text and border on inlay; a pressed toggle (aria-pressed) fills blue.

### Inputs / Fields
- **Style:** card-white, 1.5px faded-ballpoint border, 6px radius, 2.75rem tall, hand face; label above in the printed face.
- **Focus:** 2px ballpoint outline and border; elsewhere a 3px blue-rule-strong focus-visible ring. Red caret. Read-only fields drop to inlay with faded text.

### Navigation
- Blue rail: label block in inlay with wordmark, red stripe and printed tagline; links at 1.3rem in rail ink with a small printed key; hover pressed ballpoint; the current page inverts to an inlay chip with blue text.

### Tape Side (signature)
- A card-white section with 8px radius and blue border, a head with a 2px rule, then an ordered list. Each track is a three-column row: two-digit number (printed, faded), title with a printed detail sentence under it, and the figure right-aligned. A gap figure becomes a red sentence written under the row. Lists without a meaningful order use the plain variant (no numbers). Side A and Side B are placed as the opened card (two sides, 2px fold) or as a grid.

### Seats and the Waitlist
- Seats are exactly as many ruled rows as seats. Below them a dashed "end of seats · waitlist" rule, then pencilled rows (grey, dashed divider) for the waitlist.

### Capacity Bar
- A 2.4rem bar with a 1.5px border: ballpoint fill for granted seats, a dark printed limit line with a label naming the real seat count, and the oversold part filled with red 45-degree hatching (red and pale red stripes). The hatching carries meaning (seats granted beyond the limit); it is a component detail, not decoration.

### Strip and Facts
- The chosen student or company is a flowing strip: hand-face name, then a printed sentence of fixed facts in a fixed order, with bold figures. Unmet criteria are struck through in red with a screen-reader "(not met)". Facts are joined by dots and break only between facts.

### Ruled Tables
- Full-width, 2px blue header rule in the printed face, 1px blue-rule row lines, right-aligned tabular numerals, blue wash on row hover, sorted header underlined.

### Messages
- A fixed toast bottom-right: card-white, 2px blue border (red for errors), 8px radius. Errors do not auto-dismiss; click to dismiss.

### Motion
- Rows ink in: a left-to-right `clip-path` reveal, 0.55s, `cubic-bezier(.16, 1, .3, 1)`, staggered 40ms per row up to the 12th, played once and only when reduced motion is not requested.

## Do's and Don'ts

### Do:
- **Do** set every result list as a tape side: number, title, detail sentence, right-aligned figure.
- **Do** split opposites into hard halves, Side A and Side B, rather than one mixed list.
- **Do** use red only for a gap or failure, and write a gap as a sentence under its row.
- **Do** show waitlist and other tentative entries in pencil grey with a dashed divider.
- **Do** set sentences and comparable figures in the printed face with tabular numerals; keep text at or above 1.05rem on the 18px root.
- **Do** keep panels flat with 1.5-2px ballpoint borders and 4/6/8px corners.
- **Do** respect reduced motion; let errors stay until dismissed.

### Don't:
- **Don't** use red for emphasis or decoration beyond the rail stripe.
- **Don't** set long sentences or figures in the hand face.
- **Don't** add shadow to panels; the toast is the only floating element.
- **Don't** use a hosted font; the faces are self-hosted so the demo works offline.
- **Don't** introduce a second accent hue; tint the blue instead.
