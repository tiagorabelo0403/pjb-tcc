param(
    [int]$IntervalSeconds = 45,
    [int]$DebounceSeconds = 10,
    [int]$MaxChangedFiles = 100,
    [string]$MessagePrefix = "chore: auto sync",
    [switch]$RunTests,
    [switch]$NoPush
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

function Get-GitChanges {
    return @(git -C $Root status --porcelain)
}

function Test-StagedChanges {
    git -C $Root diff --cached --quiet
    return $LASTEXITCODE -ne 0
}

function Clear-StagedChanges {
    git -C $Root restore --staged :/
}

function Invoke-SafeSync {
    $changes = Get-GitChanges
    if ($changes.Count -eq 0) {
        return
    }
    if ($changes.Count -gt $MaxChangedFiles) {
        Write-Warning "Blocked auto-sync: $($changes.Count) changed files exceed MaxChangedFiles=$MaxChangedFiles."
        return
    }

    if ($RunTests) {
        Invoke-Checked ".\mvnw.cmd" @("test", "-DtrimStackTrace=false")
    }

    Invoke-Checked "git" @("-C", $Root, "add", "-A")
    try {
        Invoke-Checked "powershell" @("-NoProfile", "-ExecutionPolicy", "Bypass", "-File", "scripts\git_secret_guard.ps1", "-Staged")
    } catch {
        Clear-StagedChanges
        Write-Warning "Blocked auto-sync by data leak guard. Changes remain local and unstaged."
        Write-Warning $_.Exception.Message
        return
    }

    if (-not (Test-StagedChanges)) {
        return
    }

    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm"
    Invoke-Checked "git" @("-C", $Root, "commit", "-m", "$MessagePrefix $timestamp")
    if (-not $NoPush) {
        Invoke-Checked "git" @("-C", $Root, "push")
    }
    Write-Output "Auto-sync complete at $timestamp."
}

Set-Location $Root
Write-Output "Safe Git auto-sync running for $Root. Press Ctrl+C to stop."
Write-Output "Interval=${IntervalSeconds}s Debounce=${DebounceSeconds}s MaxChangedFiles=$MaxChangedFiles RunTests=$RunTests NoPush=$NoPush"

while ($true) {
    try {
        if ((Get-GitChanges).Count -gt 0) {
            Start-Sleep -Seconds $DebounceSeconds
            Invoke-SafeSync
        }
    } catch {
        Write-Warning "Auto-sync cycle failed: $($_.Exception.Message)"
    }
    Start-Sleep -Seconds $IntervalSeconds
}
