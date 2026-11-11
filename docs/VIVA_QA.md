# PlaceWise - 30 likely viva questions

Short answers. The class name in brackets is where to point on the screen.

## Threads and synchronization

**1. Why is `wait()` always inside a `while` loop?** [`BoundedQueue.put/take`]
A thread can wake up for a reason other than "the condition is now true": a spurious wakeup, or `notifyAll()` woke several
threads and another one already used the free slot. After waking it must re-check the condition. With `if`, three blocked
producers woken together would all insert and the queue would exceed its capacity (a test, `manyBlockedProducersWokenTogetherStillRespectTheCapacity`, proves this).

**2. What is the difference between `wait()` and `sleep()`?**
`wait()` belongs to `Object`, must be called while holding that object's monitor, **releases** the monitor, and ends when another
thread calls `notify`/`notifyAll` (or on interrupt). `sleep()` belongs to `Thread`, keeps any monitors it holds, and ends after a
time. `wait` = "until something happens" (`BoundedQueue`); `sleep` = "for a while" (`ProgressMonitor`).

**3. Why `notifyAll()` and not `notify()`?** [`BoundedQueue`]
Producers and consumers wait on the *same* monitor. `notify()` wakes one arbitrary thread, which might be a thread of the wrong kind
(a producer woken when the queue is still full); that wakeup is wasted and everyone can sleep forever. `notifyAll()` wakes all, and
the `while` loops sort it out.

**4. Where could a race condition occur in this project?**
(a) Two recruiters allocating the last seat of a company at the same time: prevented by the per-company lock in `CompanyBoard`.
(b) Two threads writing audit lines: prevented by `synchronized` in `FileAuditLog.log`. (c) Two threads using the queue: the
`BoundedQueue` monitor. The deliberately broken `UnsafeSeatAllocator` shows (a) without the lock: check-then-act oversells seats.

**5. What does `synchronized` guarantee?**
Two things: **mutual exclusion** (one thread at a time per lock object) and **visibility** (what a thread wrote before releasing the
lock is visible to the next thread that acquires the same lock).

**6. Why `volatile`, and how is it different from `synchronized`?** [`Application.status`, `ProgressMonitor.running`]
`volatile` only guarantees visibility and ordering of reads/writes of that one variable; it gives no mutual exclusion, so
`count++` on a volatile is still a race. It is enough for a single-writer flag or a status read by another thread.

**7. How is deadlock avoided?** [`CompanyBoard`, `ARCHITECTURE.md` section 5]
No thread ever requests a lock while holding another. Boards do not log inside their lock; they return `Decision`s that are logged
after the lock is released. No hold-and-wait means no circular wait, so no deadlock. `wait()` releases its monitor anyway.

**8. What does `join()` do?** [`PlacementDrive.joinAll`]
The calling thread waits until the target thread has finished. Besides waiting, it creates a **happens-before** edge: everything the
finished thread wrote is visible afterwards. We join all students, then close the queue, then join the recruiters, so results are read
only when they are complete.

**9. What is a daemon thread, and which one do we use?** [`ProgressMonitor`]
A daemon thread does not keep the JVM alive; when only daemon threads remain, the JVM exits. The progress monitor is a daemon because
it is merely a helper. Normal ("user") threads such as recruiters keep the program running until they finish.

**10. Which thread states exist? What do BLOCKED and WAITING mean?** [`ThreadLifecycleDemo`]
NEW, RUNNABLE, BLOCKED, WAITING, TIMED_WAITING, TERMINATED. BLOCKED: trying to *enter* a synchronized block whose lock another thread
holds. WAITING: inside `wait()`/`join()` with no timeout. TIMED_WAITING: `sleep(ms)` or a timed wait. The demo makes one worker visit all six.

**11. How do you handle `InterruptedException`?** [`StudentApplicant`, `Recruiter`, `PlacementDrive`]
Never swallow it. Either let it propagate (`BoundedQueue.put/take` declare it) or, if you cannot rethrow, restore the flag with
`Thread.currentThread().interrupt()` and stop. `PlacementDrive` even delays re-setting the flag until its own shutdown is done,
otherwise its `join()` calls would throw immediately.

**12. Why one lock per company instead of one global lock?** [`CompanyBoard`]
Different companies never conflict, so a global lock would make recruiters wait for each other for no reason. One lock per company
keeps the critical section tiny and still makes "check seats, then take one" atomic where it matters.

**13. Why does `CompanyBoard` return `Decision` objects instead of writing to the audit log itself?**
Logging inside the board lock would nest two locks (board then audit log), which is how deadlocks start. Returning the decisions and
logging after the lock is released keeps every critical section free of outgoing calls.

**14. How do you test concurrent code when timing is random?** [`PlacementDriveTest`, `BoundedQueueTest`, `scripts/`]
Test *invariants* (truths that hold for every interleaving), not exact output; poll thread states with a deadline (`Await`) instead of
`sleep` and hope; repeat (`@RepeatedTest(50)`, a loop script); compare with a naive reference model; and run a **mutation check**
(remove a lock or a `while` on purpose and confirm a test fails). A passing concurrent test that cannot fail proves nothing.

## Data, JDBC and exceptions

**15. Why `PreparedStatement`?** [`JdbcRepositoryBase`]
User text is sent as *data* in `?` parameters, never glued into the SQL, so `'; DROP TABLE students;--` is stored as a harmless
string (a test does exactly that). It also lets the database reuse the compiled statement.

**16. What is a transaction? How do you commit and roll back?** [`JdbcStudentRepository.saveAll`]
A group of statements that succeeds or fails as a whole. We call `setAutoCommit(false)`, run the statements, `commit()`; on any
failure `rollback()`. Auto-commit is restored in `finally`. The student CSV batch import uses it: one bad row stores nothing.

**17. Why try-with-resources?**
`Connection`, `PreparedStatement`, `ResultSet`, readers and writers implement `AutoCloseable`; the block closes them in reverse order even
when an exception is thrown, so nothing leaks. A test counts opened vs closed connections.

**18. Why `BigDecimal` for CGPA and not `double`?** [`Cgpa`]
Doubles cannot represent most decimals exactly (`0.1 + 0.2 != 0.3`), so "CGPA >= minimum" could fail exactly on the boundary.
`BigDecimal` is exact. We store scale 2 and compare with `compareTo`, never `equals` (`7.5` and `7.50` are different `BigDecimal`s).

**19. Why are `PlaceWiseException` and its children unchecked?**
They report business-rule failures that most callers cannot fix locally; checked exceptions would add `throws` clauses to every layer.
The menu catches `PlaceWiseException` in one place and prints the message. `SQLException` (checked) is wrapped into `PersistenceException`
at the repository boundary so the layers above never see `java.sql`.

**20. Why is there an in-memory repository when we have MySQL?** [`InMemoryRepository`]
The program runs without MySQL (`--no-db`), the demo is instant, and it proves the design: services depend on the `Repository`
interface, so JDBC and in-memory are interchangeable (polymorphism). The same contract tests run against both.

**21. How does the CSV import "never crash"?** [`CsvImporter`]
Problems with the whole file (missing, empty, wrong header) raise one clear `ImportException`. Problems with a *row* are caught
(multi-catch) and the row is written to `rejects.csv` with its line number and reason; the import continues. Even `1e999999999` is
refused before it can allocate memory.

## Java language features

**22. `Comparable` vs `Comparator`?** [`Student`, `StudentRanking`]
`Comparable` is the *one natural order* built into the class (`Student.compareTo` = roll number). A `Comparator` is an *external*
order you can have many of: `StudentRanking.ORDER`, one per `StudentSort` constant. Ranking is not the natural order of a student,
so it is a Comparator.

**23. Why generics in the repository (`Repository<T, ID>`)?**
One interface and one JDBC base class serve students (key `String`), companies (key `Long`) and applications (key `ApplicationKey`)
with compile-time type checks and no casts: `findById("R1")` returns `Optional<Student>`, and passing a `Long` to the student repository
does not compile.

**24. What does it mean that streams are lazy?** [`StudentRanking.top/page`]
Intermediate operations (`filter`, `map`, `sorted`, `skip`, `limit`) do nothing until a terminal operation (`toList`, `count`, ...)
pulls elements through. `limit(n)` can stop the pipeline early. A stream also cannot be reused after a terminal operation.

**25. What is a functional interface? Lambda vs method reference?** [`EligibilityRule`, `ConnectionProvider`]
An interface with exactly one abstract method, so a lambda or method reference can implement it:
`(s, c) -> Result.pass("ok")` or `Student::cgpa`. A method reference is shorthand for a lambda that only calls an existing method.

**26. Why a `PriorityQueue` in `CompanyBoard`? What is its catch?**
It gives the best (or worst) applicant in O(1) with `peek()` and O(log n) insert/removal, which is exactly "who is the weakest selected
student?". Catch: iteration order is *not* sorted (only the head is guaranteed), so snapshots sort a copy; removing an arbitrary element
is O(n), done with an explicit `Iterator`.

**27. `HashMap` vs `TreeMap` vs `LinkedHashMap`: where did you use each?**
`HashMap`: fast lookup by key, no order (`CsvImporter` first-line-of-key). `TreeMap`: sorted by key (`InMemoryStudentRepository` returns students by roll number; branch summaries alphabetical).
`LinkedHashMap`: insertion order (the boards in company order; report rows in display order).

**28. Why are `Student` and `Company` immutable records?**
Immutable objects are automatically thread-safe (many recruiter threads share them with no locks), and the what-if simulator can build
a modified *copy* so real data cannot change. A record generates constructor, accessors, `equals`, `hashCode`, `toString`.

**29. How is the ranking deterministic, and why does it matter?** [`StudentRanking.ORDER`]
The last tie-breaker is the unique roll number, so the order is a strict total order: no two different students compare equal. The same
students in any input order give the identical list (tested with 100 shuffles), which makes reports repeatable and seat allocation fair.

**30. What is "happens-before" and where do we rely on it?**
A guarantee that one action's effects are visible to another. We rely on: `Thread.start()` (everything built before `start` is visible
to the thread, e.g. plans and boards), `join()` (everything a thread wrote is visible after joining it), `synchronized` unlock→lock, and
`volatile` write→read. This is why the main thread may read final statuses after joining the workers without any extra locking.
