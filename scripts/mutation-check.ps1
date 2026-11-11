# Mutation check: breaks the production code ON PURPOSE, one small change at a time, and requires
# the named tests to FAIL. A mutation that the tests do not notice ("SURVIVED") is a hole in the tests.
#
#   powershell -ExecutionPolicy Bypass -File scripts\mutation-check.ps1
#
# Never run another Maven build while this runs: it edits files under src/ (and always restores them).

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root
$utf8 = New-Object System.Text.UTF8Encoding($false)

# Each entry: Name, File (relative), Old text, New text, All (replace every occurrence), Tests (-Dtest value)
$mutations = @(
  @{ Name='queue: put() waits with if instead of while'; File='src/main/java/com/placewise/concurrency/BoundedQueue.java';
     Old='while (items.size() == capacity && !closed) {'; New='if (items.size() == capacity && !closed) {'; All=$false; Tests='BoundedQueueTest' },
  @{ Name='queue: take() waits with if instead of while'; File='src/main/java/com/placewise/concurrency/BoundedQueue.java';
     Old='while (items.isEmpty() && !closed) {'; New='if (items.isEmpty() && !closed) {'; All=$false; Tests='BoundedQueueTest' },
  @{ Name='queue: take() forgets notifyAll'; File='src/main/java/com/placewise/concurrency/BoundedQueue.java';
     Old='notifyAll(); // wake producers waiting for space'; New='// notifyAll removed'; All=$false; Tests='BoundedQueueTest' },
  @{ Name='board: company lock removed everywhere'; File='src/main/java/com/placewise/drive/CompanyBoard.java';
     Old='synchronized (lock) {'; New='synchronized (new Object()) {'; All=$true; Tests='CompanyBoardTest,PlacementDriveTest' },
  @{ Name='board: newcomer displaces when it should not'; File='src/main/java/com/placewise/drive/CompanyBoard.java';
     Old='BEST_FIRST.compare(application, worst) < 0'; New='BEST_FIRST.compare(application, worst) > 0'; All=$false; Tests='CompanyBoardTest,PlacementDriveTest' },
  @{ Name='board: withdrawal promotes nobody'; File='src/main/java/com/placewise/drive/CompanyBoard.java';
     Old='Application next = waitlist.poll();'; New='Application next = null;'; All=$false; Tests='CompanyBoardTest,PlacementDriveTest' },
  @{ Name='recruiter: skips the eligibility check'; File='src/main/java/com/placewise/drive/Recruiter.java';
     Old='if (!result.eligible()) {'; New='if (false) {'; All=$false; Tests='PlacementDriveTest' },
  @{ Name='ranking: backlog tie-break removed'; File='src/main/java/com/placewise/ranking/StudentRanking.java';
     Old='int byBacklogs = Integer.compare(a.activeBacklogs(), b.activeBacklogs());'; New='int byBacklogs = 0;'; All=$false; Tests='StudentRankingTest,ReferenceModelTest' },
  @{ Name='cgpa rule: boundary becomes exclusive'; File='src/main/java/com/placewise/eligibility/CgpaRule.java';
     Old='compareTo(company.minCgpa()) >= 0'; New='compareTo(company.minCgpa()) > 0'; All=$false; Tests='EligibilityTest,ReferenceModelTest' },
  @{ Name='backlog rule: boundary becomes exclusive'; File='src/main/java/com/placewise/eligibility/BacklogRule.java';
     Old='backlogs <= company.maxBacklogs()'; New='backlogs < company.maxBacklogs()'; All=$false; Tests='EligibilityTest,ReferenceModelTest' },
  @{ Name='checker: reports only the first failure'; File='src/main/java/com/placewise/eligibility/EligibilityChecker.java';
     Old='failedReasons.add(result.reason());'; New='if (failedReasons.isEmpty()) { failedReasons.add(result.reason()); }'; All=$false; Tests='EligibilityTest,AnalyticsTest' },
  @{ Name='validator: CGPA 10.01 accepted'; File='src/main/java/com/placewise/validation/StudentValidator.java';
     Old='if (!Range.isBetween(student.cgpa(), Cgpa.MIN, Cgpa.MAX)) {'; New='if (!Range.isBetween(student.cgpa(), Cgpa.MIN, new java.math.BigDecimal("10.01"))) {'; All=$false; Tests='ValidationTest' },
  @{ Name='what-if: CGPA no longer capped at 10'; File='src/main/java/com/placewise/analytics/WhatIfSimulator.java';
     Old='student.cgpa().add(cgpaIncrease).min(Cgpa.MAX)'; New='student.cgpa().add(cgpaIncrease)'; All=$false; Tests='AnalyticsTest' },
  @{ Name='audit log: log() not synchronized'; File='src/main/java/com/placewise/audit/FileAuditLog.java';
     Old='public synchronized void log('; New='public void log('; All=$false; Tests='AuditTest' },
  @{ Name='safe allocator: lock removed'; File='src/main/java/com/placewise/concurrency/race/SafeSeatAllocator.java';
     Old='synchronized (locks[companyIndex]) {'; New='synchronized (new Object()) {'; All=$true; Tests='RaceDemoTest' },
  @{ Name='jdbc: transaction never switches auto-commit off'; File='src/main/java/com/placewise/storage/database/JdbcStudentRepository.java';
     Old='connection.setAutoCommit(false);     // start the transaction'; New='// auto-commit left on'; All=$false; Tests='JdbcRepositoriesTest' },
  @{ Name='jdbc: duplicate keys not recognised'; File='src/main/java/com/placewise/storage/database/JdbcRepositoryBase.java';
     Old='boolean h2 = "23505".equals(s.getSQLState());'; New='boolean h2 = false;'; All=$false; Tests='JdbcRepositoriesTest' },
  @{ Name='import: rejects not sorted by line'; File='src/main/java/com/placewise/service/ImportService.java';
     Old='rejected.sort((a, b) -> Integer.compare(a.lineNumber(), b.lineNumber()));'; New='// rejects left unsorted'; All=$false; Tests='ServicesTest' },
  @{ Name='invariants: seat-overflow check disabled'; File='src/main/java/com/placewise/drive/DriveInvariants.java';
     Old='if (board.selected().size() > seats) {'; New='if (false) {'; All=$false; Tests='DriveInvariantsTest' },
  @{ Name='csv: doubled quotes no longer unescaped'; File='src/main/java/com/placewise/csv/CsvLine.java';
     Old='} else if (i + 1 < line.length() && line.charAt(i + 1) == ''"'') {'; New='} else if (false) {'; All=$false; Tests='CsvLineTest,CsvImportTest' }
)

# Run through cmd.exe so that Windows PowerShell 5.1 never sees the test JVM's stderr (a mutated class
# prints stack traces there, and PowerShell would turn them into terminating errors).
function Run-Maven([string[]]$mavenArgs) {
  cmd /c ".\mvnw.cmd $($mavenArgs -join ' ') > nul 2>&1"
  return $LASTEXITCODE
}

$results = @()
foreach ($m in $mutations) {
  $path = Join-Path $root $m.File
  $original = [IO.File]::ReadAllText($path)
  $count = ([regex]::Matches($original, [regex]::Escape($m.Old))).Count
  if ($count -eq 0 -or (-not $m.All -and $count -ne 1)) {
    $results += [pscustomobject]@{ Mutation=$m.Name; Verdict="BAD PATTERN ($count matches)" }
    continue
  }
  try {
    [IO.File]::WriteAllText($path, $original.Replace($m.Old, $m.New), $utf8)
    if ((Run-Maven @('-o', '-q', 'compile')) -ne 0) {
      $verdict = 'INVALID (does not compile)'
    } else {
      $exit = Run-Maven @('-o', '-q', 'test', "-Dtest=$($m.Tests)")
      $verdict = if ($exit -ne 0) { 'KILLED' } else { 'SURVIVED' }
    }
  } finally {
    [IO.File]::WriteAllText($path, $original, $utf8)
  }
  Write-Host ("{0,-10} {1}" -f $verdict, $m.Name)
  $results += [pscustomobject]@{ Mutation=$m.Name; Verdict=$verdict }
}

Run-Maven @('-o', '-q', 'compile') | Out-Null   # leave target/ consistent with the restored sources
$killed = ($results | Where-Object Verdict -eq 'KILLED').Count
Write-Host ""
Write-Host "Killed $killed of $($results.Count) mutations."
$results | Format-Table -AutoSize | Out-String | Write-Host
if ($killed -ne $results.Count) { exit 1 }
