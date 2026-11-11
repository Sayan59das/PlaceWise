# PlaceWise - Test evidence

Every number in this file comes from a real run on the author's machine: Windows 11, **JDK 27** (compiling with
`--release 21`), Maven 3.9.16 through the wrapper, 28 logical CPUs, H2 2.5 in MySQL mode as the test database.
No MySQL server was used (see "Not covered" at the end).

## 1. Result

```text
.\mvnw.cmd clean verify
Tests run: 467, Failures: 0, Errors: 0, Skipped: 0
All coverage checks have been met.
BUILD SUCCESS          (13 s with an offline Maven cache, including compile, tests, JaCoCo and the jar)
```

| Quality gate | Required | Measured | Result |
|---|---|---|---|
| All tests | 0 failures | 467 run, 0 failures, 0 errors, 0 skipped | pass |
| Line coverage `eligibility` | >= 80% | **100%** (51/51) | pass (enforced in the build) |
| Line coverage `ranking` | >= 80% | **100%** (35/35) | pass (enforced) |
| Line coverage `analytics` | >= 80% | **100%** (120/120) | pass (enforced) |
| Line coverage `drive` | >= 80% | **97.1%** (467/481) | pass (enforced) |
| Mutation check | every mutation caught | **20 of 20 killed** | pass |
| Concurrency loop | no failure in repeated runs | **30 of 30 runs passed** (110 tests per run, 3,300 executions) | pass |

The gate is the `jacoco:check` execution in `pom.xml` (`<minimum>0.80</minimum>` for the four packages); if a package drops
below 80% the build fails.

## 2. Tests by area

| Area | Test classes | Tests | What they check |
|---|---|---:|---|
| Model | `ModelTest` | 26 | CGPA scale 2 and half-up rounding; normalization of roll/branch/email; `withXxx` copies; `Comparable`; branch set is sorted and read-only; `Application` status |
| Validation | `ValidationTest` | 45 | every rule valid and invalid (parameterized); boundaries CGPA `0.00`, `10.00`, `10.004`, `10.005`, `10.01`, `-0.01`; semester 0/1/8/9; backlogs `-1`; all problems reported at once; generic `Range.isBetween` for Integer and BigDecimal |
| Eligibility | `EligibilityTest`, `ReferenceModelTest` | 30 | pass **exactly on** CGPA and backlog limits, fail one hundredth below; several simultaneous failures all reported; branch restriction; empty branch list = all; one-to-all, all-to-one; rule composition with `and`; **reference model: 5 seeds x 5,000 random students x 20 random companies, naive implementation vs production, identical eligibility, failure counts and ranked order** |
| Ranking | `StudentRankingTest` | 20 | CGPA, then backlogs, then name, then roll number; **100 shuffles give identical output**; top-N and pagination boundaries (0, n, more than n, past the end, `Integer.MAX_VALUE`); empty input; every `StudentSort` field |
| Analytics | `AnalyticsTest` | 19 | branch summaries (count, average, highest) and rounding; placement summary by hand; explainer (`Needs +0.70 CGPA and 1 fewer backlog`) and its agreement with the checker on 2,000 random pairs; what-if with hand-computed unlocks, caps, and **real data never modified** |
| Concurrency primitives | `BoundedQueueTest` | 14 | FIFO; blocks when full / empty (thread state `WAITING` observed by bounded polling, no bare `sleep`); `close()` wakes consumers and blocked producers; drain after close; interrupt not swallowed; **many woken producers/consumers still respect capacity / only one takes the item** (the `while`-around-`wait` test); **4 producers x 4 consumers x 10,000 items: nothing lost, nothing duplicated, per-producer order kept**; `StartGate`; `ThreadLifecycleDemo` sees all six states |
| Seat allocation | `CompanyBoardTest`, `DriveInvariantsTest` | 24 | free seats, waitlist, displacement by a better-ranked newcomer, promotion on withdrawal, duplicates, wrong-company application; **100 arrival orders give the same selected set**; 8 threads admitting 4,000 applicants at once end with exactly the top 25; the invariant checker detects each kind of corruption (so "invariants hold" means something) |
| Placement drive | `PlacementDriveTest` | 66 | **`@RepeatedTest(50)`: 200 students, 5 companies, limited seats, 4 recruiters, different seed each time** - selected never exceeds seats, nobody selected twice for a company, every application ends in exactly one final status, only eligible students selected, waitlist never outranks selected, **and the boards equal a naive "top seats by rank" prediction**; same seed -> same outcome 10 times; 100% withdrawals; no withdrawals; 100% duplicates; queue of 1 with 1 recruiter; 700 students with 32 recruiters; empty inputs; audit log content and thread names; **a failing recruiter or student thread aborts the drive without hanging or leaking threads**; interrupting the caller keeps the interrupt flag; the daemon progress monitor |
| Race demo | `RaceDemoTest` | 29 | **unsafe != safe** under the same workload (safe always sells exactly the seats, unsafe oversells); a **deterministic** forced interleaving (two threads rendezvous between check and update) oversells by exactly 1; 16 threads hammering the safe allocator sell exactly the seats; `NoSeatsException`; config validation |
| Persistence (H2, MySQL mode, the real `schema.sql`) | `JdbcRepositoriesTest`, `InMemoryRepositoriesTest`, `ConfigAndSchemaTest` | 69 | one contract run against **both** JDBC and in-memory repositories: CRUD round trips, missing id gives empty, duplicate roll number, unique company names, batch import; **rollback on a bad row leaves nothing behind**; **SQL-injection text stored literally, table intact**; auto-commit restored after a failed transaction; **zero connections left open** after successes and failures; foreign keys and cascades; `DbConfig` (password never printed) |
| File I/O (`@TempDir`) | `CsvLineTest`, `CsvImportTest`, `AuditTest` | 49 | valid file, missing file, empty, header only, wrong header, wrong column count, non-numeric CGPA, CGPA above 10, quoted comma, unclosed quote, duplicate roll numbers, blank lines, CRLF, BOM, `1e999999999`, **10,000 rows**, **export then re-import is identical**; rejects file format; audit log: **8 threads x 1,000 lines, no interleaved or lost line**, newline injection impossible, append-only across runs, byte-for-byte backup |
| Reports | `ReportTest` | 10 | exact text and CSV output, empty tables, file-name safety (`../../evil` stays inside the folder) |
| Services | `ServicesTest` | 18 | CRUD rules, all three eligibility questions with audit lines, filters/top-N/paging, import with exact line numbers in `rejects.csv`, storage failure -> "Nothing was imported", export-all writes every report in both formats (exact CSV content checked), export/re-import |
| Console UI | `ConsoleIOTest`, `MainMenuTest` | 38 | scripted input with text, blanks, `-1`, `42`, huge numbers and end-of-input; every screen (exporting every report writes 24 files for the 5 sample companies); an error in one screen returns to the main menu |
| Whole program | `MainTest` | 10 | boots `Main` against H2: import -> eligibility -> **drive on stored data** -> report -> exit, checks the database contents, the audit log, and that **data survives a restart**; `--help`, bad arguments, missing/unreachable database, graceful shutdown with audit backup |
| **Total** | 26 classes | **467** | |

### Test strategy notes

* **No `Thread.sleep` as synchronization.** Tests wait with `Await.state(thread, WAITING)` / `Await.until(condition)`: bounded polling with a deadline and a readable failure message.
* **Invariants, not exact interleavings.** Concurrent tests assert truths that hold for *every* schedule; the seeded plan and the rank-based allocation additionally make the final outcome independent of timing, so it can be compared with a naive reference.
* **`@Timeout` everywhere** concurrency is involved, so a deadlock fails the test instead of hanging the build.
* Nothing was weakened to get green. The failures that did occur during development are listed in section 6.

## 3. Race demo: before and after

Menu `7` -> `3` (or `RaceDemoTest`). The same 8 threads make the same 160 attempts at 6 seats; only the allocator differs.

```text
Workload: 8 threads x 20 attempts = 160 attempts for 6 seats (3 companies x 2)
UNSAFE (no synchronization):  granted 16 seats -> oversold 10
SAFE   (lock per company):    granted 6 seats -> oversold 0
Same workload, different result: that is a race condition, and the lock cures it.
```

This output was identical in the two independent demo runs recorded while writing this document. The exact unsafe figure can differ from
run to run; the safe line is always `6 / 0`. `RaceDemoTest` also forces the bad interleaving deterministically
(`theUnsafeAllocatorOversellsDeterministicallyWhenTwoThreadsInterleave`: one seat, two threads, both pass the check before either updates -> 2 seats granted).

## 4. Mutation check (do the tests have teeth?)

`scripts\mutation-check.ps1` changes the production code on purpose, one small change at a time, and requires the relevant tests to fail.
**All 20 were killed.**

| # | Deliberate bug | Caught |
|---|---|---|
| 1 | `BoundedQueue.put` waits with `if` instead of `while` | yes |
| 2 | `BoundedQueue.take` waits with `if` instead of `while` | yes |
| 3 | `BoundedQueue.take` forgets `notifyAll` | yes |
| 4 | `CompanyBoard` lock removed everywhere | yes |
| 5 | board: a newcomer displaces when it should not | yes |
| 6 | board: a withdrawal promotes nobody | yes |
| 7 | recruiter skips the eligibility check | yes |
| 8 | ranking: backlog tie-break removed | yes |
| 9 | `CgpaRule` boundary becomes exclusive | yes |
| 10 | `BacklogRule` boundary becomes exclusive | yes |
| 11 | checker reports only the first failure | yes |
| 12 | validator accepts CGPA 10.01 | yes |
| 13 | what-if no longer caps CGPA at 10 | yes |
| 14 | `FileAuditLog.log` not synchronized | yes |
| 15 | `SafeSeatAllocator` lock removed | yes |
| 16 | JDBC transaction never switches auto-commit off | yes |
| 17 | JDBC duplicate keys not recognised | yes |
| 18 | import rejects not sorted by line | yes |
| 19 | invariant checker's seat-overflow check disabled | yes |
| 20 | CSV doubled quotes no longer unescaped | yes |

## 5. Concurrency loop

`scripts\loop-concurrency.ps1 -Runs 30` runs only the tests tagged `concurrency` (`BoundedQueueTest`, `CompanyBoardTest`'s 8-thread test,
`PlacementDriveTest`, `RaceDemoTest`) 30 times in a row; each run includes the 50 repeated drives.

```text
run   1/30: PASS  (110 tests)
run   2/30: PASS  (110 tests)
 ...                                  (runs 3-29 also PASS; no FAIL line, no target\loop-failure-*.txt file)
run  30/30: PASS  (110 tests)
Concurrency loop: 30/30 runs passed (100%), 110 tests per run, 3300 test executions green.
```

That is 1,500 complete 200-student drives (50 repetitions x 30 runs) plus the queue, board and race tests, with no failure. A pass rate of
100% over 30 runs does not *prove* the absence of a timing bug; together with the invariants, the deterministic forced-interleaving
tests and the 20 caught mutations it is strong evidence.

## 6. What testing and review found while building this

Recorded honestly; each was fixed at its cause.

| Found by | Problem | Fix |
|---|---|---|
| Test (`ServicesTest`) | `rejects.csv` listed parse-time rejects first and database-clash rejects after, so lines were out of order | `ImportService` sorts rejects by line number |
| Adversarial review | `PlacementDrive` re-set the thread's interrupt flag *before* its `finally` block, so the shutdown `join()` calls would throw at once and leave workers running | the flag is re-asserted after the shutdown; `interruptingTheCallerStopsTheDrive...` tests it |
| Review of the first `BoundedQueue` tests | they passed even with `if` instead of `while` (items still all arrive; only the capacity is violated) | two deterministic tests added; mutations 1-2 now fail them |
| First mutation run | the harness crashed because Windows PowerShell 5.1 turned the test JVM's stderr into a terminating error | Maven is now run through `cmd /c` |
| Review | `AppContext.close()` skipped closing the database if closing the audit log threw | every step now runs; failures are chained as suppressed exceptions |
| Self-check | `ConsoleIO` was 285 lines (target: under 250) | plumbing moved to `ConsoleBase`; the largest class is now 234 lines |
| Doclint | public default constructors and one-line `@return` tags had no proper Javadoc | `javac -Xdoclint:all/public` is clean for all public API |
| Review (CSV) | a crafted CGPA such as `1e999999999` could allocate huge numbers | refused by size before conversion (`StudentCsv.parseCgpa`, `ConsoleIO`); tested |

Review checklist outcome: no empty `catch` block (searched); every `InterruptedException` is propagated, wrapped, or the flag is restored (two
documented exceptions: `PlacementDrive` re-asserts it after shutdown, and the daemon `ProgressMonitor` simply ends its own thread); the single place an exception is deliberately not rethrown (`restoreAutoCommit`) logs a warning; all
shared mutable state is either `synchronized`, `volatile`, immutable, or confined to one thread (see `ARCHITECTURE.md` section 5); every JDBC and file resource uses try-with-resources.

## 7. Reproduce everything

```text
.\mvnw.cmd clean verify                                             # 467 tests, coverage gate, jar   (Linux/macOS: ./mvnw clean verify)
start target\site\jacoco\index.html                                 # coverage report
.\mvnw.cmd test -Dgroups=concurrency                                # only the concurrency tests
powershell -ExecutionPolicy Bypass -File scripts\loop-concurrency.ps1 -Runs 30
powershell -ExecutionPolicy Bypass -File scripts\mutation-check.ps1  # takes several minutes; edits src/ temporarily, never run another build meanwhile
java -jar target\placewise.jar --no-db --data-dir demo-data < sample-data\demo-keystrokes.txt   # the verified demo
```

Per-test results are in `target\surefire-reports\`.

## 8. Not covered (limitations)

* **A real MySQL server was never used.** The tests run the same `schema.sql` and the same SQL on H2 in MySQL mode (including `CHECK`, foreign keys with `ON DELETE CASCADE`, `AUTO_INCREMENT`, inline `INDEX`).
  Duplicate-key detection accepts both H2 (`23505`) and MySQL (`23000`/1062) codes, but the MySQL branch is exercised only by reading the code.
  Please run the program once against your own MySQL (`scripts/mysql-setup.sql`, `db.properties`) before the viva.
* Race outcomes are statistical by nature: the unsafe allocator is *very* likely to oversell in any run, so the statistical test retries up to 20 rounds;
  the deterministic rendezvous test is the hard proof.
* Coverage of the other packages (96.1% overall) is reported but not gated; uncovered lines are mostly defensive `catch` branches for I/O errors that are hard to provoke.
