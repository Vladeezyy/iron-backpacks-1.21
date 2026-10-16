#!/bin/sh
# Runs the PerfScene on NeoForge (max 15 min): tools/perf.sh [-Pjfr]. Prints the [perf] lines and the errors; the flight
# recording of -Pjfr goes to neoforge/run/perf.jfr.
cd "$(dirname "$0")/.."
rm -f build-perf.log neoforge/run/perf.jfr
perl -e 'alarm 900; exec @ARGV' ./gradlew :neoforge:runPerf --console=plain "$@" > build-perf.log 2>&1
code=$?
# after the time limit the game outlives Gradle and keeps the world locked
pkill -f 'neoforge/build/moddev/perfRunProgramArgs' 2>/dev/null
echo "exit $code"
grep -E '\[perf\] (PERF|DONE)' build-perf.log | sed -E 's/^.*\[perf\] //'
echo "--- errors"
grep -E '/(ERROR|FATAL)\]|Caused by|Exception' build-perf.log | sed -E 's/^\[[^]]*\] //' | cut -c1-250 | sort | uniq -c | sort -rn | head -15
