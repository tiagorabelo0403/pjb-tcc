param(
    [string]$ProjectRoot = "C:\PJB",
    [string]$Module = "pjb-api",
    [string]$MavenOpts = "-Xms512m -Xmx4096m -XX:+UseG1GC"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

if (!(Test-Path $ProjectRoot)) {
    throw "Projeto não encontrado: $ProjectRoot"
}

Set-Location $ProjectRoot
$env:MAVEN_OPTS = $MavenOpts
New-Item -ItemType Directory -Force "$ProjectRoot\logs" | Out-Null
$timestamp = Get-Date -Format yyyyMMdd-HHmmss
$log = "$ProjectRoot\logs\$Module-clean-test-$timestamp.log"
$runner = "$ProjectRoot\logs\$Module-clean-test-$timestamp.cmd"
$utf8NoBom = [System.Text.UTF8Encoding]::new($false)

[System.IO.File]::WriteAllText($log, "", $utf8NoBom)

$mavenWrapper = Join-Path $ProjectRoot "mvnw.cmd"
$runnerLines = @(
    "@echo off",
    "cd /d `"$ProjectRoot`"",
    "call `"$mavenWrapper`" -pl `"$Module`" clean test -DtrimStackTrace=false 2>&1",
    "exit /b %ERRORLEVEL%"
)
[System.IO.File]::WriteAllLines($runner, $runnerLines, [System.Text.Encoding]::ASCII)

Write-Host "===== EXECUÇÃO AO VIVO ====="
Write-Host "Projeto: $ProjectRoot"
Write-Host "Módulo: $Module"
Write-Host "Log: $log"
Write-Host ""

$previousErrorActionPreference = $ErrorActionPreference
$exitCode = 1
try {
    $ErrorActionPreference = "Continue"
    & cmd.exe /D /S /C "`"$runner`"" | ForEach-Object {
        $line = [string]$_
        Write-Host $line
        [System.IO.File]::AppendAllText($log, $line + [Environment]::NewLine, $utf8NoBom)
    }
    $exitCode = $LASTEXITCODE
} finally {
    $ErrorActionPreference = $previousErrorActionPreference
    if (Test-Path $runner) {
        Remove-Item -Force $runner
    }
}

Write-Host ""
Write-Host "===== LOG COMPLETO ====="
Write-Host $log
Write-Host ""
Write-Host "===== ERROS IMPORTANTES ====="

& "$ProjectRoot\scripts\pjb-error-summary.ps1" -LogPath $log -Context 6

exit $exitCode
