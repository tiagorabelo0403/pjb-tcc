param(
    [Parameter(Position = 0)]
    [string]$Message,

    [switch]$NoPush,
    [switch]$RunTests
)

$ErrorActionPreference = "Stop"
$Root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path

function Invoke-Checked {
    param(
        [string]$Command,
        [string[]]$Arguments
    )
    & $Command @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "$Command failed with exit code $LASTEXITCODE"
    }
}

Set-Location $Root

$changes = git status --porcelain
if (-not $changes) {
    Write-Output "No local changes to commit."
    exit 0
}

if ($RunTests) {
    Invoke-Checked ".\mvnw.cmd" @("test", "-DtrimStackTrace=false")
}

Invoke-Checked "git" @("-C", $Root, "add", "-A")
Invoke-Checked "powershell" @("-NoProfile", "-ExecutionPolicy", "Bypass", "-File", "scripts\git_secret_guard.ps1", "-Staged")

if (-not $Message -or $Message.Trim().Length -eq 0) {
    $Message = "chore: sync local changes $(Get-Date -Format 'yyyy-MM-dd HH:mm')"
}

Invoke-Checked "git" @("-C", $Root, "commit", "-m", $Message)

if (-not $NoPush) {
    Invoke-Checked "git" @("-C", $Root, "push")
}

Write-Output "Git sync complete."
