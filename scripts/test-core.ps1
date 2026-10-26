$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$testOutput = Join-Path $projectRoot 'build/core-tests'
New-Item -ItemType Directory -Force -Path $testOutput | Out-Null
$sources = @(
    (Join-Path $projectRoot 'app/src/main/java/io/github/manorcompanion/TimerState.java'),
    (Join-Path $projectRoot 'app/src/main/java/io/github/manorcompanion/Csv.java'),
    (Join-Path $projectRoot 'tests/CoreTest.java')
)
& javac --release 17 -encoding UTF-8 -d $testOutput @sources
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& java -cp $testOutput CoreTest
exit $LASTEXITCODE
