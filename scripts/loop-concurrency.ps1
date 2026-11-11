# Runs only the tests tagged "concurrency" over and over and reports the pass rate.
# Timing bugs are probabilistic: one green run proves little, hundreds of green runs prove a lot.
#
#   powershell -ExecutionPolicy Bypass -File scripts\loop-concurrency.ps1 -Runs 30

param([int]$Runs = 30)

$ErrorActionPreference = 'Continue'
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$passed = 0
$failed = 0
$testsPerRun = 0
New-Item -ItemType Directory -Force -Path target | Out-Null
for ($i = 1; $i -le $Runs; $i++) {
  $log = "target\loop-run.txt"
  # via cmd.exe so PowerShell 5.1 does not treat the test JVM's stderr as an error
  cmd /c ".\mvnw.cmd -o test -Dgroups=concurrency > $log 2>&1"
  $code = $LASTEXITCODE
  $output = [IO.File]::ReadAllText((Join-Path $root $log))
  $summary = [regex]::Matches($output, 'Tests run: (\d+), Failures: (\d+), Errors: (\d+), Skipped: (\d+)\r?\n') | Select-Object -Last 1
  if ($code -eq 0) { $passed++ } else { $failed++ }
  if ($summary) { $testsPerRun = $summary.Groups[1].Value }
  Write-Host ("run {0,3}/{1}: {2}  ({3} tests)" -f $i, $Runs, $(if ($code -eq 0) { 'PASS' } else { 'FAIL' }), $testsPerRun)
  if ($code -ne 0) { Copy-Item $log "target\loop-failure-$i.txt" }
}
$rate = [math]::Round(100.0 * $passed / $Runs, 1)
$line = "Concurrency loop: $passed/$Runs runs passed ($rate%), $testsPerRun tests per run, $($passed * [int]$testsPerRun) test executions green."
Write-Host $line
New-Item -ItemType Directory -Force -Path target | Out-Null
$line | Out-File -FilePath target\loop-concurrency-summary.txt -Encoding utf8
if ($failed -gt 0) { exit 1 }
