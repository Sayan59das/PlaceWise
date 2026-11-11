# Team contributions - PlaceWise

Course: Programming in Java (25CSE0202), Chitkara University.
Team: **Sayan, Ikjot, Kabir, Manmohit**.

> This is a **template**. The module split below is a suggestion that balances the work; edit it so it matches what each
> person really did, then fill in the empty columns (hours, commits, notes) from your own Git history and logbook.
> Every member must be able to explain **every** class in the viva, not only their own module (see `VIVA_QA.md`).

## 1. Module ownership

| Member | Main modules (packages / classes) | Syllabus topics to explain | Hours | Commits | Notes |
|---|---|---|---|---|---|
| **Sayan** | `concurrent` (`BoundedQueue`, `StartGate`, `ThreadLifecycleDemo`), `drive` (`PlacementDrive`, `CompanyBoard`, `Recruiter`, race demo, `DriveInvariants`), `audit` | threads, `synchronized`, `wait/notify`, race conditions, deadlock, `volatile`, `PriorityQueue` | | | |
| **Ikjot** | `persistence` (`Repository`, `JdbcRepositoryBase`, `Jdbc*Repository`, `InMemory*Repository`, `Database`, `DbConfig`, `SchemaInitializer`), `schema.sql`, `scripts/mysql-setup.sql` | JDBC, `PreparedStatement`, transactions, generics, abstract classes, exceptions | | | |
| **Kabir** | `model`, `validation`, `eligibility`, `ranking`, `analytics` (explainer, what-if, summaries) | OOP (interfaces, polymorphism), `Comparable`/`Comparator`, lambdas, streams, `BigDecimal` | | | |
| **Manmohit** | `ui` (`ConsoleIO`, `MainMenu`, screens), `io` (CSV), `report`, `service`, `Main`, `sample-data/` | strings, `StringBuilder`, file handling (byte and character streams), collections, exceptions, input handling | | | |

## 2. Shared work

| Area | Who | Notes |
|---|---|---|
| Test strategy and reference-model tests | all | each person writes the tests for the module they own |
| Documentation (`docs/`) | all | each person reviews the sections about their modules |
| Demo rehearsal (`DEMO_SCRIPT.md`) | all | agree who types and who speaks |
| Viva preparation (`VIVA_QA.md`) | all | practise explaining classes you did **not** write |

## 3. Self-check before the viva

Tick when you can explain it without looking at the code.

| Topic | Sayan | Ikjot | Kabir | Manmohit |
|---|---|---|---|---|
| Why `while` around `wait()` | | | | |
| What the race demo shows and why the safe version is safe | | | | |
| How the ranking order is built and why it is deterministic | | | | |
| What `PreparedStatement` protects against | | | | |
| How a transaction is committed or rolled back | | | | |
| Why CGPA is `BigDecimal` | | | | |
| How a bad CSV row is handled | | | | |
| Walk through one application inside the drive (`ARCHITECTURE.md`, section 2) | | | | |

## 4. Declaration

We confirm that this work is our own, that every member understands all of the code, and that the contribution
percentages below reflect the real distribution.

| Member | Contribution (%) | Signature / date |
|---|---|---|
| Sayan | | |
| Ikjot | | |
| Kabir | | |
| Manmohit | | |
