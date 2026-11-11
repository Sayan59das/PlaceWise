#!/usr/bin/env sh
# Runs only the tests tagged "concurrency" over and over and reports the pass rate.
#   sh scripts/loop-concurrency.sh 30
RUNS="${1:-30}"
cd "$(dirname "$0")/.." || exit 1
PASSED=0
FAILED=0
i=1
while [ "$i" -le "$RUNS" ]; do
  if ./mvnw -o -q test -Dgroups=concurrency > "target/loop-run-$i.txt" 2>&1; then
    PASSED=$((PASSED + 1))
    echo "run $i/$RUNS: PASS"
  else
    FAILED=$((FAILED + 1))
    echo "run $i/$RUNS: FAIL (see target/loop-run-$i.txt)"
  fi
  i=$((i + 1))
done
echo "Concurrency loop: $PASSED/$RUNS runs passed"
[ "$FAILED" -eq 0 ]
