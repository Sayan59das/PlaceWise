# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Stack

Static HTML, CSS and vanilla JavaScript (no build step, no Node), served by the JDK's built-in HTTP server
(`com.sun.net.httpserver`) from inside the existing Java 25 / Maven application. No new libraries.
The web UI is a separate mode (`--web`); the terminal menu stays exactly as it is and is the backup.

## Users

- **The presenter:** the student author, running a live demo of the course project (25CSE0202, Chitkara University).
- **The audience:** faculty evaluators watching the demo on a projector in a room, reading from a distance,
  and asking viva questions about how it works.

## Product Purpose

PlaceWise is a student placement eligibility checker: it stores students and recruiting companies, decides who is
eligible for which company (CGPA, active backlogs, branch), ranks eligible students, explains rejections, simulates
"what if", runs a multithreaded placement drive, imports CSV data and exports reports. The web UI exists so the same
functionality can be shown clearly on a big screen. Success: every feature can be demonstrated live, readable from
the back of the room, without the presenter fighting the interface.

## Positioning

Every verdict is explained exactly: not just "not eligible" but the gap ("needs +0.70 CGPA and 1 fewer backlog"),
the same rules drive the what-if simulator, and the placement drive is proven correct by invariant checks after real
threads run.

## Operating Context

- Live demo on a projector, lights possibly on; evaluators sit several metres away.
- Runs on the presenter's laptop with `--no-db` (in-memory) or a local MySQL database.
- Sample data lives in `sample-data/` (50 students, 5 companies, a deliberately broken CSV).
- A five-minute demo script exists in `docs/DEMO_SCRIPT.md`; viva questions in `docs/VIVA_QA.md`.

## Capabilities and Constraints

- All nine terminal features, weighted equally: students, companies, eligibility check, eligible/ranked lists,
  why-not-eligible, what-if simulator, placement drive + race-condition + thread-lifecycle demos, reports and export,
  CSV import.
- The web layer must only call the existing services; business logic is never duplicated in JavaScript.
- Code must stay simple enough for the student to read and explain every line.
- Localhost only; single presenter; no login.

## Evidence on Hand

Real sample data in `sample-data/`; real test results in `docs/TEST_EVIDENCE.md`. No logos or brand assets exist.

## Product Principles

1. Readable from the back of the room beats dense.
2. Show the reason, not only the verdict.
3. One source of truth: the Java services decide, the page only displays.
4. The terminal must keep working as the fallback.

## Accessibility & Inclusion

Projector use: large type, strong contrast, and status never conveyed by colour alone.
