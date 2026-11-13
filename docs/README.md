# PlaceWise - Student Placement Eligibility Checker

Course project for **Programming in Java (25CSE0202), Chitkara University.**
Team: Sayan, Ikjot, Kabir, Manmohit.

PlaceWise is a menu-driven console application that

* stores **student academic records** and **company eligibility criteria**,
* checks **CGPA, active backlogs and branch** (all failed reasons are reported, not just the first),
* generates **eligible student lists** and **ranks** students (deterministic tie-breaks, top-N, pagination),
* **exports placement reports** as text and CSV,

and goes beyond that with a "why not eligible?" explainer, a what-if simulator, a **multithreaded placement drive**
(limited seats, waitlist, withdrawals), a **race-condition demo**, CSV import with a `rejects.csv`, and a thread-safe audit log.

## 1. Requirements

| Needed | Notes |
|---|---|
| JDK 25 or newer | The build uses `--release 25`, so it also works on newer JDKs (developed on JDK 27). No separate Maven install: the Maven Wrapper (`mvnw`) downloads Maven itself. |
| MySQL 8 *(optional)* | Only for permanent storage. Without it run with `--no-db` (data lives in memory). |

Tests use an in-memory **H2** database in MySQL mode, so `mvnw clean verify` needs nothing but the JDK.

## 2. Build, test, run

```text
Windows (PowerShell):   .\mvnw.cmd clean verify          # compile + all tests + coverage gate + jar
Linux / macOS:          ./mvnw clean verify

java -jar target/placewise.jar --no-db                     # try it without a database (data kept in memory)
java -jar target/placewise.jar                             # uses ./db.properties (MySQL) if the file exists
java -jar target/placewise.jar --db path/to/db.properties  # explicit settings file
java -jar target/placewise.jar --data-dir mydata           # where audit log, reports, rejects.csv go (default placewise-data)
java -jar target/placewise.jar --help
```

Keep `target/lib/` next to `target/placewise.jar`; it holds the MySQL driver.

### Using MySQL

1. Run `scripts/mysql-setup.sql` as a MySQL administrator (creates database `placewise` and user `placewise`).
2. `copy db.properties.example db.properties` (Linux/macOS: `cp`) and edit user/password.
   `db.properties` is git-ignored: **credentials are never committed**, only `db.properties.example` is.
3. Start the program. It creates the tables from `src/main/resources/schema.sql` on first start and
   fails *at start-up* with a clear message if the database cannot be reached.

### Useful commands

| Command | What it does |
|---|---|
| `mvnw clean verify` | Full build: tests, JaCoCo report (`target/site/jacoco/index.html`), 80% coverage gate on `eligibility`, `ranking`, `analytics`, `drive` |
| `mvnw test -Dgroups=concurrency` | Only the concurrency tests |
| `scripts\loop-concurrency.ps1 -Runs 30` (or `sh scripts/loop-concurrency.sh 30`) | Repeats the concurrency tests and prints the pass rate |
| `scripts\mutation-check.ps1` | Breaks the code on purpose 20 ways and requires the tests to notice |

## 3. The menu

```text
 1. Students                          6. What-if simulator
 2. Companies                         7. Placement drive simulation (threads)
 3. Check eligibility                 8. Reports and export
 4. Eligible and ranked lists         9. Import CSV
 5. Explain why not eligible          0. Exit (graceful shutdown)
```

Bad input never crashes the program: blank lines, text instead of numbers, out-of-range values and a closed input
stream are all handled (see `ConsoleIO`). Business errors print `Error: ...` and return to the main menu.

### CSV formats

```text
students:   rollNo,name,branch,semester,cgpa,activeBacklogs,email
companies:  name,minCgpa,maxBacklogs,allowedBranches,packageLpa,seats      (branches separated by |, empty = all)
```

Bad rows never stop an import: they are written to `rejects.csv` (`line,reason,row`) in file order. A wrong header,
an empty or missing file is reported as one clear error. Samples live in `sample-data/`
(50 students, 5 companies, and a deliberately dirty file for the rejects demo).

## 4. Rules at a glance

* **Eligible** = `CGPA >= company minimum` **and** `active backlogs <= company maximum` **and** branch allowed
  (an empty branch list means every branch). Equality passes; one hundredth below fails.
* **Ranking** = CGPA descending, then fewer backlogs, then name (ignoring case), then roll number. Roll numbers are
  unique, so the order is total: the same input in any order gives the same output.
* **Placement drive** = students submit applications at once into a bounded queue; recruiter threads check eligibility
  and allocate seats **by rank**; extra eligible students are waitlisted; when a selected student withdraws the best
  waitlisted student is promoted. Details and the locking argument: [ARCHITECTURE.md](ARCHITECTURE.md).

## 5. Project structure

```text
src/main/java/com/placewise/
  Main, Options, AppContext      START HERE: program start, command-line options, builds every object once
  model/          the data: Student, Company, Application, ApplicationStatus, ApplicationKey, Cgpa
  exception/      PlaceWiseException + 9 more specific errors
  validation/     StudentValidator, CompanyValidator (plain if-checks), Range (generic isBetween helper)
  eligibility/    EligibilityRule (interface), CgpaRule, BacklogRule, BranchRule, EligibilityChecker, EligibilityResult
  ranking/        StudentRanking (ranking order), RankedStudent, StudentSort
  analytics/      WhyNotEligibleExplainer, WhatIfSimulator, StudentAnalytics + their small result records
  storage/        Repository<T,ID> and the Student/Company/Application repository interfaces
    memory/       InMemory*Repository: keeps data in maps (used with --no-db and in fast tests)
    database/     Database, DbConfig, SchemaInitializer, ConnectionProvider, JdbcRepositoryBase, Jdbc*Repository
  csv/            CsvLine (reads/writes one CSV line), CsvImporter<T> (abstract), StudentCsv, CompanyCsv,
                  ImportResult, RejectedRow, CsvFiles
  report/         Table, ReportRenderer (abstract), TextReportRenderer, CsvReportRenderer, ReportBuilder, ReportExporter
  audit/          AuditLog, FileAuditLog, AuditBackup, AuditEvent
  service/        Student/Company/Eligibility/Listing/Import/Report/Drive services: one per menu area
  ui/             ConsoleBase + ConsoleIO (reading typed input), Menu (abstract), MainMenu, Views
    screens/      one Menu subclass per screen: StudentMenu, CompanyMenu, EligibilityMenu, ListsMenu, ...
  concurrency/    BoundedQueue<T> (wait/notifyAll), StartGate, ThreadLifecycleDemo
    race/         the race-condition demo: SeatAllocator, UnsafeSeatAllocator, SafeSeatAllocator, RaceDemo
  drive/          the placement drive: PlacementDrive, CompanyBoard, Recruiter, StudentApplicant, ...
src/main/resources/schema.sql    same file for MySQL and for H2 (tests)
src/test/java/...                tests, in the same folders as the code they test (see TEST_EVIDENCE.md)
sample-data/                     50 students, 5 companies, a dirty file for the rejects demo
scripts/                         mysql-setup.sql, loop-concurrency.*, mutation-check.ps1
docs/                            this file, ARCHITECTURE, TEST_EVIDENCE, DEMO_SCRIPT, VIVA_QA, CONTRIBUTIONS
```

**Coding style.** The code is written to be read by first-year students: plain `if` statements and `for` loops,
one job per class, and a short comment on every class and public method. Advanced Java features (lambdas, streams,
generics) appear only where the syllabus asks for them, each in one obvious place listed in section 6.

Immutable value types (`Student`, `Company`, results) are Java **records** (an immutable class with generated
constructor, accessors, `equals`, `hashCode`, `toString`; Java 16+). Stateful classes are ordinary classes.

## 6. Syllabus coverage map

Every row points at real code. Search the class name to find it.

| Syllabus topic | Where it appears |
|---|---|
| **Abstract class** | `ReportRenderer` (parent of `TextReportRenderer`, `CsvReportRenderer`); `CsvImporter` (parent of `StudentCsv`, `CompanyCsv`); `JdbcRepositoryBase`; `InMemoryRepository`; `Menu` (parent of every screen) |
| **Interface** | `EligibilityRule`, `Repository`, `StudentRepository`, `AuditLog`, `SeatAllocator`, `ConnectionProvider` |
| **Inheritance** | `PlaceWiseException` and its 9 subclasses; `JdbcStudentRepository extends JdbcRepositoryBase`; `StudentMenu extends Menu` |
| **Polymorphism** | `ReportService.exportAll` loops over `List<ReportRenderer>`; `EligibilityChecker` runs a `List<EligibilityRule>`; `RaceDemo` runs the same workload with a safe or an unsafe `SeatAllocator`; `AppContext` swaps JDBC and in-memory repositories; `StudentCsv` / `CompanyCsv` override `parseRow` of `CsvImporter` |
| **Strings, StringBuilder, wrappers** | `CsvLine.parse/format` (StringBuilder state machine); `TextReportRenderer` (StringBuilder + `String.format`); `Student.normalizeRoll` (trim, toUpperCase); `Explanation.summary`; `Integer.parseInt` / `Long.parseLong` and boxing in `StudentCsv`, `ConsoleIO`, `Range.isBetween` with `Integer` |
| **Exceptions** | custom hierarchy in `exception/`; `try/catch/finally` with commit / rollback in `JdbcStudentRepository.saveAll` (auto-commit switched back on in `finally`); **multi-catch**: `CsvImporter.readRows` (`IllegalArgumentException \| ArithmeticException \| PlaceWiseException`); try-with-resources on every connection, statement and file |
| **Collections** | `ArrayList` (everywhere); `LinkedList` (`BoundedQueue`); `HashMap` (`CsvImporter`, `JdbcCompanyRepository`, `DriveInvariants`); `LinkedHashMap` (`PlacementDrive` boards, `PlacementSummary.toRows`, `InMemoryCompanyRepository`); `TreeMap` (`InMemoryStudentRepository`, `StudentAnalytics.branchSummaries`, `WhatIfSimulator.cohortUnlocks`); `HashSet` (`CompanyBoard`, `ImportService`); `TreeSet` (`Company` branches); `PriorityQueue` (`CompanyBoard`); `Iterator` (`CompanyBoard.removeFrom`) |
| **Comparable / Comparator** | `Student implements Comparable<Student>` (natural order = roll number); `StudentRanking.RankingOrder implements Comparator<Student>` (written out step by step); `StudentSort.comparator()`; `CompanyBoard.BEST_FIRST` |
| **Threads: lifecycle, join, sleep, daemon** | `ThreadLifecycleDemo` shows all six states; `PlacementDrive.joinAll` / `join()`; `Thread.sleep` in `ProgressMonitor`; `ProgressMonitor.start` sets `setDaemon(true)` |
| **synchronized, race conditions** | `CompanyBoard` (one lock per company), `SafeSeatAllocator` vs `UnsafeSeatAllocator`, `RaceDemo`, `FileAuditLog.log`, `ProgressCounter`; `volatile`: `Application.status`, `ProgressMonitor.running`, `Database.closed`, `ThreadLifecycleDemo.spinning` |
| **wait / notify producer-consumer** | `BoundedQueue<T>` (`put` / `take` / `close`, `while` around `wait()`, `notifyAll()`); `StartGate` |
| **JDBC** | `Database` (`DriverManager`), `JdbcStudentRepository`, `JdbcCompanyRepository`, `JdbcApplicationRepository` (CRUD with `PreparedStatement` and `ResultSet`, transactions with commit / rollback), `SchemaInitializer`, `DbConfig` |
| **Lambdas, functional interfaces** | `EligibilityRule` is a `@FunctionalInterface` (a rule can be a lambda: see `EligibilityTest`); `ConnectionProvider`; lambdas: `StudentSort.comparator()`, `CompanyBoard.BEST_FIRST`, `AuditLog.none()`, the `Runnable` racers in `RaceDemo` |
| **Streams** | `filter`: `ListingService.eligibleStudents`; `filter` + `map`: `WhyNotEligibleExplainer.explainRejections`; `skip` + `limit`: `StudentRanking.top/page`. Everywhere else the code uses plain loops on purpose |
| **Generics** | generic classes: `Repository<T,ID>`, `BoundedQueue<T>`, `CsvImporter<T>`, `ImportResult<T>`; generic method with a **bounded type**: `Range.isBetween(V value, V min, V max)` with `<V extends Comparable<V>>` |
| **File handling** | **byte streams**: `AuditBackup` (`FileInputStream` / `FileOutputStream`); **character streams**: `FileAuditLog`, `CsvImporter` (`FileReader`/`BufferedReader`), `CsvFiles`, `ReportExporter`, `DbConfig` (`Properties` + `FileReader`); **CSV**: `CsvLine`, `StudentCsv`, `CompanyCsv` |

## 7. Known limitations

* **MySQL itself was never exercised** by the author's automated runs (no MySQL credentials were available on the
  build machine). The same `schema.sql` and the same SQL run against H2 in MySQL mode in the tests, and the
  Connector/J driver is on the runtime classpath. Please try `db.properties` once on your own MySQL before the viva.
* CSV: a quoted field cannot contain a line break (one physical line = one record).
* The `applications` table holds the **latest** drive only; running a new drive on stored data replaces it.
* Names beginning with `=`, `+`, `-` or `@` are written to exported CSV as they are; a spreadsheet may treat them as formulas.
* One OS thread per student (capped at 256; with more students each thread submits for several).

## 8. Extension ideas

Student self-service login, per-company custom rules via the existing `EligibilityRule` interface, a web front-end
over the same services, application deadlines, offer acceptance rounds, richer reports (charts), connection pooling.
