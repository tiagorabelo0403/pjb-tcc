param(
    [switch]$Staged,
    [switch]$All
)

$ErrorActionPreference = "Stop"
$Root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$MaxTextBytes = 2 * 1024 * 1024
$SkippedParts = @(
    ".git", ".idea", ".vscode", ".claude", ".codex",
    "target", "build", "out", "outcheck", "node_modules",
    "__pycache__", ".pytest_cache", ".mypy_cache", ".gradle", "logs"
)
$AllowlistValues = @(
    "changeme", "change_me", "replace_me", "example", "sample",
    "dummy", "placeholder", "password", "secret", "token"
)
$AllowlistSuffixes = @(".example", ".sample", ".template", ".md")
$PropertyAssignmentSuffixes = @(".conf", ".env", ".ini", ".properties", ".ps1", ".sh", ".tf", ".txt", ".yaml", ".yml")
$SecretPatterns = @(
    @{ Rule = "private-key-block"; Pattern = "-----BEGIN [A-Z ]*PRIVATE KEY-----" },
    @{ Rule = "aws-access-key"; Pattern = "\b(?:AKIA|ASIA)[0-9A-Z]{16}\b" },
    @{ Rule = "github-token"; Pattern = "\b(?:ghp|gho|ghu|ghs|ghr)_[A-Za-z0-9_]{30,}\b" },
    @{ Rule = "github-fine-grained-token"; Pattern = "\bgithub_pat_[A-Za-z0-9_]{50,}\b" },
    @{ Rule = "openai-key"; Pattern = "\bsk-[A-Za-z0-9]{32,}\b" },
    @{ Rule = "slack-token"; Pattern = "\bxox[baprs]-[A-Za-z0-9-]{20,}\b" },
    @{ Rule = "google-api-key"; Pattern = "\bAIza[0-9A-Za-z_-]{35}\b" },
    @{ Rule = "jwt"; Pattern = "\beyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}\b" }
)
$QuotedAssignmentPattern = "(?i)\b(password|passwd|pwd|secret|token|api[_-]?key|client[_-]?secret|private[_-]?key)\b\s*[:=]\s*(['""])([^'""\r\n]{12,})\2"
$PropertyAssignmentPattern = "(?im)^\s*[A-Z0-9_.-]*(PASSWORD|PASSWD|PWD|SECRET|TOKEN|API[_-]?KEY|CLIENT[_-]?SECRET|PRIVATE[_-]?KEY)[A-Z0-9_.-]*\s*[:=]\s*([^#\s]{12,})\s*$"
$SensitiveFilenamePattern = "(?i)(^|[\\/])(\.env(?:\..*)?|id_rsa.*|id_ed25519.*|.*\.(?:pem|key|p8|p12|pfx|jks|keystore|kubeconfig))$"

function Invoke-GitLines {
    param([string[]]$Arguments)
    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    $output = & git @Arguments 2>$null
    $exitCode = $LASTEXITCODE
    $ErrorActionPreference = $previousErrorActionPreference
    if ($exitCode -ne 0 -or $null -eq $output) {
        return @()
    }
    return @($output | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
}

function Test-GitRepo {
    return (Invoke-GitLines @("rev-parse", "--show-toplevel")).Count -gt 0
}

function Get-RelativePathText {
    param([string]$Path)
    $full = [System.IO.Path]::GetFullPath($Path)
    $rootPath = [System.IO.Path]::GetFullPath($Root)
    if (-not $rootPath.EndsWith([System.IO.Path]::DirectorySeparatorChar)) {
        $rootPath += [System.IO.Path]::DirectorySeparatorChar
    }
    $rootUri = New-Object System.Uri($rootPath)
    $pathUri = New-Object System.Uri($full)
    return [System.Uri]::UnescapeDataString($rootUri.MakeRelativeUri($pathUri).ToString()).Replace("\", "/")
}

function Test-SkippedPath {
    param([string]$Path)
    $relative = Get-RelativePathText $Path
    foreach ($part in ($relative -split "/")) {
        if ($SkippedParts -contains $part) {
            return $true
        }
    }
    return $false
}

function Test-AllowedFilename {
    param([string]$RelativePath)
    $lower = $RelativePath.ToLowerInvariant()
    foreach ($suffix in $AllowlistSuffixes) {
        if ($lower.EndsWith($suffix)) {
            return $true
        }
    }
    return $false
}

function Test-AllowedValue {
    param([string]$Value)
    $normalized = $Value.Trim().Trim("'", '"').ToLowerInvariant()
    if ($AllowlistValues -contains $normalized) {
        return $true
    }
    if ($normalized.StartsWith('${{') -or $normalized.StartsWith('${')) {
        return $true
    }
    if ($normalized.StartsWith('replace_') -or $normalized.StartsWith('change_')) {
        return $true
    }
    if ($normalized.StartsWith('trocar-por-')) {
        return $true
    }
    return $false
}

function Test-PropertyAssignmentCandidate {
    param([string]$RelativePath)
    $lower = $RelativePath.ToLowerInvariant()
    foreach ($suffix in $AllowlistSuffixes) {
        if ($lower.EndsWith($suffix)) {
            return $false
        }
    }
    $name = [System.IO.Path]::GetFileName($lower)
    if ($name.StartsWith(".env")) {
        return $true
    }
    $extension = [System.IO.Path]::GetExtension($lower)
    return $PropertyAssignmentSuffixes -contains $extension
}

function Get-Entropy {
    param([string]$Value)
    if ([string]::IsNullOrEmpty($Value)) {
        return 0.0
    }
    $counts = @{}
    foreach ($character in $Value.ToCharArray()) {
        $key = [string]$character
        $counts[$key] = 1 + $(if ($counts.ContainsKey($key)) { $counts[$key] } else { 0 })
    }
    $entropy = 0.0
    foreach ($count in $counts.Values) {
        $p = [double]$count / [double]$Value.Length
        $entropy += -1.0 * $p * [Math]::Log($p, 2)
    }
    return $entropy
}

function Test-Binary {
    param([byte[]]$Bytes)
    $limit = [Math]::Min(4096, $Bytes.Length)
    for ($index = 0; $index -lt $limit; $index++) {
        if ($Bytes[$index] -eq 0) {
            return $true
        }
    }
    return $false
}

function Get-CandidatePaths {
    if ($Staged) {
        return Invoke-GitLines @("diff", "--cached", "--name-only", "--diff-filter=ACMRTUXB") |
            ForEach-Object { Join-Path $Root $_ }
    }
    if (Test-GitRepo) {
        return Invoke-GitLines @("ls-files", "--cached", "--others", "--exclude-standard") |
            ForEach-Object { Join-Path $Root $_ }
    }
    return Get-ChildItem -Path $Root -Recurse -Force -File | ForEach-Object { $_.FullName }
}

$Findings = New-Object System.Collections.Generic.List[string]
foreach ($path in (Get-CandidatePaths)) {
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
        continue
    }
    if (Test-SkippedPath $path) {
        continue
    }

    $relative = Get-RelativePathText $path
    if ($relative -match $SensitiveFilenamePattern -and -not (Test-AllowedFilename $relative)) {
        $Findings.Add("$relative [sensitive-filename]")
    }

    $bytes = [System.IO.File]::ReadAllBytes($path)
    if ($bytes.Length -gt $MaxTextBytes -or (Test-Binary $bytes)) {
        continue
    }
    $text = [System.Text.Encoding]::UTF8.GetString($bytes)

    foreach ($entry in $SecretPatterns) {
        if ([regex]::IsMatch($text, $entry.Pattern)) {
            $Findings.Add("$relative [$($entry.Rule)]")
        }
    }

    foreach ($match in [regex]::Matches($text, $QuotedAssignmentPattern)) {
        $value = $match.Groups[3].Value
        if (Test-AllowedValue $value) {
            continue
        }
        if ($value.Length -ge 20 -or (Get-Entropy $value) -ge 3.6) {
            $Findings.Add("$relative [secret-assignment]")
            break
        }
    }

    if (Test-PropertyAssignmentCandidate $relative) {
        foreach ($match in [regex]::Matches($text, $PropertyAssignmentPattern)) {
            $value = $match.Groups[2].Value
            if (Test-AllowedValue $value) {
                continue
            }
            if ($value.Length -ge 20 -or (Get-Entropy $value) -ge 3.6) {
                $Findings.Add("$relative [secret-assignment]")
                break
            }
        }
    }
}

if ($Findings.Count -gt 0) {
    [Console]::Error.WriteLine("Secret guard blocked this operation. Review these files before committing:")
    foreach ($finding in ($Findings | Sort-Object -Unique)) {
        [Console]::Error.WriteLine("  $finding")
    }
    [Console]::Error.WriteLine("No secret values were printed. Rotate any credential that was ever committed.")
    exit 1
}

Write-Output "Secret guard: no obvious credentials found."
exit 0
