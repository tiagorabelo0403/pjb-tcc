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
$AllowedEmailDomains = @("example.com", "example.org", "example.net", "example.invalid", "exemplo.com", "test.local", "localhost", "pjb.local", "pjb.test")
$AllowedEmailValues = @(
    "a@a.com", "a@b.com", "admin@pjb.local", "admin@test.local", "adv.teste@pjb.local",
    "adv@example.com", "adv@pjb.br", "adv@test.local", "adv@x.com", "adv2@x.com",
    "adva@test.local", "advb@test.local", "advogado.notificado@pjb.test", "advogado@test.local",
    "ana@oab.com", "b@b.com", "c1@test.local", "c2@test.local", "c3@test.local",
    "cidadao.seed@pjb.local", "cidadao@pjb.test", "colegiado@tribunal.jus.br",
    "contato@exemplo.jus.br", "defensor@pjb.br", "defensor@pjb.test", "delegado@pjb.test",
    "desembargador@test.local", "fulano@example.com", "fulano@exemplo.com", "juiz@pjb.br",
    "juiz@pjb.test", "juiz@test.local", "juiz@x.com", "magistrado.seed@pjb.local",
    "maria@email.com", "maria@estado.ce.gov.br", "maria@exemplo.com", "maria@office.com",
    "ministro@test.local", "mp@example.com", "mp@example.test", "notif.default@pjb.test",
    "notif.persist@pjb.test", "ocinaria.lima@exemplo.com", "oficial@pjb.test",
    "operador@pjb.test", "parte@example.com", "perito@pjb.local", "pf@x.com",
    "presidencia@tst.jus.br", "procurador@test.local", "psico@pjb.local",
    "quality@test.local", "registro@pjb.local", "seed@exemplo.com", "seg@pc.ce.gov.br",
    "seg@tjce.jus.br", "seguranca@mp.ce.gov.br", "seguranca@mpce.mp.br",
    "seguranca@pp.ce.gov.br", "seguranca@tjce.jus.br", "senior@example.com",
    "servidor.seed@pjb.local", "servidor@tjce.jus.br", "servidor@x.com",
    "sistema@pjb.gov.br", "stf@stf.jus.br", "stj@stj.jus.br", "suporte@tjce.jus.br",
    "tiago@example.com", "tiago@office.com", "usuario@mpce.mp.br"
)
$AllowedCpfValues = @("04106184389", "12345678909", "98765432100")
$AllowedCnpjValues = @("11222333000181", "12495454000160")
$AllowedPhoneValues = @("6130434000", "6132173000", "6133198000", "85900000000", "85999999999")
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
$EmailPattern = "(?i)\b[A-Z0-9._%+-]+@([A-Z0-9.-]+\.[A-Z]{2,}|localhost)\b"
$PhonePattern = "(?<!\d)(?:\+55\s*)?(?:\([1-9]{2}\)\s*9?\d{4}[-\s]?\d{4}|[1-9]{2}\s+9?\d{4}[-\s]\d{4})(?!\d)"
$CpfPattern = "(?<!\d)\d{3}\.?\d{3}\.?\d{3}-?\d{2}(?!\d)"
$CnpjPattern = "(?<!\d)\d{2}\.?\d{3}\.?\d{3}/?\d{4}-?\d{2}(?!\d)"
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

function Get-Digits {
    param([string]$Value)
    return ($Value -replace "\D", "")
}

function Test-RepeatedDigits {
    param([string]$Digits)
    return $Digits -match "^([0-9])\1+$"
}

function Test-Cpf {
    param([string]$Value)
    $digits = Get-Digits $Value
    if ($digits.Length -ne 11 -or (Test-RepeatedDigits $digits)) {
        return $false
    }
    $sum = 0
    for ($index = 0; $index -lt 9; $index++) {
        $sum += [int]::Parse([string]$digits[$index]) * (10 - $index)
    }
    $first = 11 - ($sum % 11)
    if ($first -ge 10) { $first = 0 }
    $sum = 0
    for ($index = 0; $index -lt 10; $index++) {
        $sum += [int]::Parse([string]$digits[$index]) * (11 - $index)
    }
    $second = 11 - ($sum % 11)
    if ($second -ge 10) { $second = 0 }
    return $first -eq [int]::Parse([string]$digits[9]) -and $second -eq [int]::Parse([string]$digits[10])
}

function Test-Cnpj {
    param([string]$Value)
    $digits = Get-Digits $Value
    if ($digits.Length -ne 14 -or (Test-RepeatedDigits $digits)) {
        return $false
    }
    $weights1 = @(5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2)
    $weights2 = @(6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2)
    $sum = 0
    for ($index = 0; $index -lt 12; $index++) {
        $sum += [int]::Parse([string]$digits[$index]) * $weights1[$index]
    }
    $first = $sum % 11
    $first = $(if ($first -lt 2) { 0 } else { 11 - $first })
    $sum = 0
    for ($index = 0; $index -lt 13; $index++) {
        $sum += [int]::Parse([string]$digits[$index]) * $weights2[$index]
    }
    $second = $sum % 11
    $second = $(if ($second -lt 2) { 0 } else { 11 - $second })
    return $first -eq [int]::Parse([string]$digits[12]) -and $second -eq [int]::Parse([string]$digits[13])
}

function Test-AllowedEmail {
    param([string]$Email)
    $normalized = $Email.ToLowerInvariant()
    if ($AllowedEmailValues -contains $normalized) {
        return $true
    }
    $domain = ($Email.Split("@")[-1]).ToLowerInvariant()
    return $AllowedEmailDomains -contains $domain
}

function Test-AllowedPhone {
    param([string]$Phone)
    $digits = Get-Digits $Phone
    if ($digits.StartsWith("55") -and $digits.Length -gt 11) {
        $digits = $digits.Substring(2)
    }
    if ($digits.Length -lt 10) {
        return $false
    }
    if ($AllowedPhoneValues -contains $digits) {
        return $true
    }
    $subscriber = $digits.Substring(2)
    return $subscriber -match "^([0-9])\1+$"
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

    foreach ($match in [regex]::Matches($text, $EmailPattern)) {
        if (-not (Test-AllowedEmail $match.Value)) {
            $Findings.Add("$relative [privacy-email]")
            break
        }
    }

    foreach ($match in [regex]::Matches($text, $CpfPattern)) {
        $cpfDigits = Get-Digits $match.Value
        if ((Test-Cpf $match.Value) -and -not ($AllowedCpfValues -contains $cpfDigits)) {
            $Findings.Add("$relative [privacy-cpf]")
            break
        }
    }

    foreach ($match in [regex]::Matches($text, $CnpjPattern)) {
        $cnpjDigits = Get-Digits $match.Value
        if ((Test-Cnpj $match.Value) -and -not ($AllowedCnpjValues -contains $cnpjDigits)) {
            $Findings.Add("$relative [privacy-cnpj]")
            break
        }
    }

    foreach ($match in [regex]::Matches($text, $PhonePattern)) {
        if (-not (Test-AllowedPhone $match.Value)) {
            $Findings.Add("$relative [privacy-phone]")
            break
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
    [Console]::Error.WriteLine("Data leak guard blocked this operation. Review these files before committing:")
    foreach ($finding in ($Findings | Sort-Object -Unique)) {
        [Console]::Error.WriteLine("  $finding")
    }
    [Console]::Error.WriteLine("No sensitive values were printed. Rotate credentials and remove personal data before retrying.")
    exit 1
}

Write-Output "Data leak guard: no obvious secrets or personal data found."
exit 0
