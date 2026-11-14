# PlaceWise - five-minute demo script

Everything below was run for real against this code (outputs are quoted from those runs).
Run from the **project folder** so `sample-data/` is found. No database is needed (`--no-db`).

## Before you start (one minute, off stage)

```text
.\mvnw.cmd clean verify          # builds target\placewise.jar and target\lib\ (do this once, beforehand)
```

* Open **two terminals** in the project folder. Terminal A is the demo. In terminal B, at the start of the demo, run
  `.\mvnw.cmd clean verify` so the real test report is ready for the last minute.
* Start the demo in terminal A:

```text
java -jar target\placewise.jar --no-db
```

> **Plan B (typing goes wrong):** replay the exact keystrokes of this script, which were verified:
> `java -jar target\placewise.jar --no-db --data-dir demo-data < sample-data\demo-keystrokes.txt`
> (PowerShell: `cmd /c "java -jar target\placewise.jar --no-db --data-dir demo-data < sample-data\demo-keystrokes.txt"`).

Legend: **type** = what you enter, *Enter* = press Enter on an empty line, **say** = one sentence for the audience.

---

## Showing it in the browser (screen or projector)

The terminal script below stays the **backup**. For a big screen, start the browser version instead of terminal A:

```text
java -jar target\placewise.jar --no-db --web        # then open http://localhost:8080/ (Enter in the terminal stops it)
```

Everything shown comes from the same Java services as the terminal. Keys `1`-`9` switch screens, like the terminal menu.
Values below are from real runs of the sample data; the drive and race counts depend only on the seed or the luck of the threads.

| Screen | Click | What the audience sees |
|---|---|---|
| Overview | **Load the sample data** | 50 students, 5 companies, 19 seats; eligible for at least one company: 39 |
| 3 Check eligibility | student **CU2520 - Tara Menon** | Side A: Bluepeak Systems. Side B: Acme Technologies `Needs +0.15 CGPA and 1 fewer backlog` with the failed criteria struck through in red |
| 5 Why not eligible | same student | the exact gap for each of the 4 companies |
| 6 What-if | drag *CGPA increase* to +0.15 and *Backlogs cleared* to 1 | Acme Technologies and Cedar Analytics move to Side A while the slider moves; the stored record is unchanged |
| 4 Ranked lists | Acme Technologies, *Per page* 5 | 11 eligible students, ranked, page by page |
| 7 Placement drive | **Run the drive** (defaults: 200 students, 5 companies, 8 seats, 4 threads, seed 42) | 426 applications, 40 selected, 104 waitlisted, "All invariants hold", and each company's 8 seats as ruled lines with the waitlist pencilled in below |
| 7 Race condition | **Run both** | without a lock more seats are granted than exist (hatched red); with a lock never |
| 9 Import CSV | **Students file with mistakes** | 3 of 13 rows imported; the 10 rejected rows listed with line number and reason |

> **If the browser misbehaves**, stop the server (Enter) and carry on with the terminal script below: nothing else changes.

---

## 0:00 - 1:00  Add data, and bad input does not crash it

**Say:** "Menu-driven console app. First a student, typing mistakes on purpose."

| Menu | Type |
|---|---|
| `1` Students → `1` Add | roll `cu9001`, name `Demo Student`, branch `cse`, semester **`abc`** (rejected), `6`, CGPA **`11`** (rejected), `7.49`, backlogs `1`, email *Enter* |

Expected:

```text
Semester (1-8): 'abc' is not a whole number.
CGPA (0.00-10.00): Please enter a value between 0.00 and 10.00.
Added CU9001 - Demo Student.
```

Then `0` (back). **Say:** "Roll number was normalized to upper case. Now bulk data from CSV."

| Menu | Type |
|---|---|
| `9` Import CSV → `1` students | *Enter*, *Enter* (default sample file, default rejects file) |
| `2` companies | *Enter*, *Enter* |

```text
Imported 50 of 50 rows; 0 rejected
Imported 5 of 5 rows; 0 rejected
```

Back (`0`), then add one company by hand: `2` Companies → `1` Add: `Demo Corp`, min CGPA `7.5`, backlogs `1`,
branches `CSE, IT`, package `10`, seats `2` → `Added company 6 - Demo Corp.` Back (`0`).

## 1:00 - 1:45  Eligibility, exactly on the boundary

**Say:** "CGPA is a BigDecimal, so 7.49 against a minimum of 7.50 fails, and 7.50 would pass."

`3` Check eligibility → `1` one student, one company → `CU9001` → company id `6` →

```text
CU9001 (Demo Student) vs Demo Corp: NOT ELIGIBLE - CGPA 7.49 is below required 7.50
```

Back (`0`).

## 1:45 - 2:30  Ranked list (ties, deterministic order)

`4` Eligible and ranked lists → company `2` (Bluepeak) → *Enter*, *Enter* (no filters) → `2` ranked.

**Say:** "Highest CGPA first; ties by fewer backlogs, then name, then roll number. Look at ranks 6 and 7: same CGPA
and backlogs, ordered by name. Ranks 20 and 21 sit exactly on Bluepeak's limits (7.50 CGPA, 1 backlog)."

```text
Rank  Roll No  Name             Branch  Sem  CGPA  Backlogs  Email
----  -------  ---------------  ------  ---  ----  --------  ------------------------
1     CU2501   Aarav Sharma     CSE     6    9.42  0         aarav.sharma@example.edu
2     CU2502   Ananya Gupta     CSE     6    9.21  0         ananya.gupta@example.edu
...
6     CU2506   Ishita Kapoor    IT      6    8.70  0
7     CU2507   Kabir Malhotra   CSE     6    8.70  0
...
20    CU2527   Atharv Desai     CSE     6    7.50  1
21    CU2528   Zoya Qureshi     ECE     6    7.50  1

21 rows
```

Back (`0`, `0`).

## 2:30 - 3:00  Why not eligible? Exact gaps

`5` → `CU9001`

```text
Why CU9001 (Demo Student, CGPA 7.49, 1 backlog(s), CSE) is rejected:
  Acme Technologies: Needs +0.51 CGPA and 1 fewer backlog
  Bluepeak Systems: Needs +0.01 CGPA
  Cedar Analytics: Needs 1 fewer backlog
  Delta Robotics: Needs +0.01 CGPA; Branch CSE is not allowed (allowed: ECE, ME)
  Evergreen Infra: Branch CSE is not allowed (allowed: CE, ME)
  Demo Corp: Needs +0.01 CGPA
```

**Say:** "Every failed rule is listed, not just the first, with the exact gap."

## 3:00 - 3:20  What-if

`6` → `CU9001`, CGPA increase `0.01`, backlogs cleared *Enter*, `n`

```text
Scenario: CGPA 7.49 -> 7.50, backlogs 1 -> 1 (your stored record is not changed).
Newly unlocked:   Bluepeak Systems, Demo Corp
```

**Say:** "A one-hundredth improvement unlocks two companies. The stored student is untouched: records are immutable."

## 3:20 - 3:50  Race condition: unsafe vs safe

`7` Placement drive → `3` Race demo → *Enter*, *Enter*, *Enter* (defaults)

```text
Workload: 8 threads x 20 attempts = 160 attempts for 6 seats (3 companies x 2)
UNSAFE (no synchronization):  granted 16 seats -> oversold 10
SAFE   (lock per company):    granted 6 seats -> oversold 0
Same workload, different result: that is a race condition, and the lock cures it.
```

**Say:** "Same threads, same choices. The only difference is `synchronized`. Unsafe checks 'a seat is left' and takes it
in two steps, so several threads take the 'last' seat. The exact unsafe number changes from run to run; the safe line is always 6 and 0."

## 3:50 - 4:20  The 200-student placement drive

Still in the Drive menu: `1` generated data → five times *Enter* (200 students, 5 companies, 8 seats, 4 recruiters, seed 42)

```text
Starting: 200 student thread(s), 4 recruiter thread(s), shared queue of 16 ...
Acme Tech            seats   8 | selected   8 | waitlisted  36
Bluepeak Systems     seats   8 | selected   8 | waitlisted  30
Cedar Analytics      seats   8 | selected   8 | waitlisted   6
Delta Robotics       seats   8 | selected   8 | waitlisted  22
Evergreen Cloud      seats   8 | selected   8 | waitlisted  10
Applications: 426 (selected 40, waitlisted 104, rejected 276, withdrawn 6)
Students placed: 33 | time: 59 ms
Invariants: ALL HOLD (no company over its seats, nobody selected twice, every application has one final status, ...)
```

**Say:** "200 student threads put applications into a bounded queue, four recruiter threads take them, each company has its own lock.
No company exceeds 8 seats. With the same seed the result is identical every time, whatever the thread timing, because seats go by rank.
Six selected students withdrew and the waitlist was promoted automatically." (Optional: `4` shows a thread passing through all six states.)

Back (`0`).

## 4:20 - 4:40  Reports and export, rejected rows

`8` Reports → `5` Export EVERY report → *Enter* (default folder) → `Wrote 28 files:` (text and CSV: eligible + ranked per company,
branch summary, overall summary). Back (`0`).

`9` Import → `1` → `sample-data/students-with-errors.csv` → *Enter*

```text
Imported 3 of 13 rows; 10 rejected (see ...\rejects.csv)
```

**Say:** "Ten bad rows (wrong column count, letters in CGPA, 10.50, duplicate roll, blank name, semester 9, unclosed quote,
negative backlogs, bad email, a huge exponent) went to `rejects.csv` with line numbers and reasons. Nothing crashed." Exit with `0`:

```text
Goodbye.
Audit log backed up to ...\audit-backup.log
```

## 4:40 - 5:00  Test report

Switch to terminal B (started at 0:00). Show the final lines and the coverage page:

```text
Tests run: <N>, Failures: 0, Errors: 0, Skipped: 0
All coverage checks have been met.
BUILD SUCCESS
```

Open `target\site\jacoco\index.html`. **Say:** "The real numbers are in `docs/TEST_EVIDENCE.md`: all tests green, 80% line-coverage gate on
`eligibility`, `ranking`, `analytics` and `drive`, concurrency tests repeated, and a mutation check that breaks the code on purpose."
