#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")/.."
mkdir -p build/core-tests
javac --release 17 -encoding UTF-8 -d build/core-tests \
  app/src/main/java/io/github/manorcompanion/TimerState.java \
  app/src/main/java/io/github/manorcompanion/Csv.java tests/CoreTest.java
java -cp build/core-tests CoreTest
