param(
    [ValidateSet('test', 'benchmark', 'all')]
    [string]$Mode = 'all',
    [string]$Jdk = $env:JAVA_HOME,
    [string]$Results = 'generated-results'
)

$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    if ($Jdk) {
        $compiler = Join-Path $Jdk 'bin/javac.exe'
        $runtime = Join-Path $Jdk 'bin/java.exe'
    } else {
        $compiler = (Get-Command javac -ErrorAction Stop).Source
        $runtime = Join-Path (Split-Path $compiler) 'java.exe'
    }
    New-Item -ItemType Directory -Path 'build/classes' -Force | Out-Null
    $sources = @(Get-ChildItem -Path 'src' -Filter '*.java' -Recurse | ForEach-Object { $_.FullName })
    & $compiler --release 17 -encoding UTF-8 -Xlint:all -Werror -d build/classes $sources
    if ($LASTEXITCODE -ne 0) { throw 'Compilation failed' }
    if ($Mode -eq 'test' -or $Mode -eq 'all') {
        & $runtime -cp build/classes analysis.StructureTests
        if ($LASTEXITCODE -ne 0) { throw 'Tests failed' }
    }
    if ($Mode -eq 'benchmark' -or $Mode -eq 'all') {
        & $runtime -Xms256m -Xmx1g -cp build/classes analysis.Benchmark $Results
        if ($LASTEXITCODE -ne 0) { throw 'Benchmark failed' }
    }
} finally {
    Pop-Location
}
