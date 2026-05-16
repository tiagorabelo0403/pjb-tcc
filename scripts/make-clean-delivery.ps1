#Requires -Version 7
<#
.SYNOPSIS
    Gera entrega ZIP limpa do PJB a partir do HEAD do Git.
.DESCRIPTION
    Usa git archive para empacotar apenas arquivos rastreados pelo Git,
    excluindo automaticamente .git/, target/, __pycache__, node_modules e
    demais artefatos de build/desenvolvimento. Valida o conteúdo do ZIP
    antes de finalizar.
.PARAMETER OutputPath
    Caminho do arquivo ZIP de saída. Default: dist/PJB-clean.zip
.PARAMETER Ref
    Referência Git (branch, tag ou commit). Default: HEAD
.EXAMPLE
    .\scripts\make-clean-delivery.ps1
    .\scripts\make-clean-delivery.ps1 -OutputPath dist/PJB-v1.0.zip -Ref v1.0.0
#>
param(
    [string]$OutputPath = "dist/PJB-clean.zip",
    [string]$Ref = "HEAD"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$RepoRoot = Split-Path -Parent $PSScriptRoot

# ── Verificações prévias ─────────────────────────────────────────────────────
Write-Host "[1/5] Verificando pré-requisitos..."

Push-Location $RepoRoot
try {
    $gitStatus = git status --porcelain 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Repositório Git não encontrado em $RepoRoot"
        exit 1
    }
    if ($gitStatus) {
        Write-Warning "Há alterações não commitadas no repositório:"
        $gitStatus | ForEach-Object { Write-Warning "  $_" }
        Write-Warning "O ZIP conterá apenas o que está commitado ($Ref)."
    }

    $resolvedRef = git rev-parse --short $Ref 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Referência Git inválida: $Ref"
        exit 1
    }
    Write-Host "  Ref: $Ref ($resolvedRef)"

    # ── Criar diretório de saída ──────────────────────────────────────────────
    Write-Host "[2/5] Criando diretório de saída..."
    $OutputDir = Split-Path -Parent (Join-Path $RepoRoot $OutputPath)
    if (-not (Test-Path $OutputDir)) {
        New-Item -ItemType Directory -Path $OutputDir | Out-Null
    }
    $AbsOutputPath = Join-Path $RepoRoot $OutputPath

    # ── Gerar ZIP via git archive ─────────────────────────────────────────────
    Write-Host "[3/5] Gerando $OutputPath via git archive..."
    git archive --format=zip --output="$AbsOutputPath" $Ref
    if ($LASTEXITCODE -ne 0) {
        Write-Error "git archive falhou."
        exit 1
    }
    $sizeMB = [math]::Round((Get-Item $AbsOutputPath).Length / 1MB, 2)
    Write-Host "  ZIP gerado: $AbsOutputPath ($sizeMB MB)"

    # ── Validação de conteúdo proibido ───────────────────────────────────────
    Write-Host "[4/5] Validando conteúdo do ZIP..."
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip = [System.IO.Compression.ZipFile]::OpenRead($AbsOutputPath)

    $forbidden = @(
        "target/",
        "build/",
        "node_modules/",
        "__pycache__/",
        ".idea/",
        ".vscode/",
        ".claude/",
        "application-local.",
        ".env`$",
        "\.key`$",
        "\.pem`$",
        "\.jks`$",
        "\.p12`$"
    )

    $violations = @()
    foreach ($entry in $zip.Entries) {
        $name = $entry.FullName.Replace("\", "/")
        foreach ($pattern in $forbidden) {
            if ($name -match $pattern) {
                $violations += "  PROIBIDO: $name"
                break
            }
        }
    }
    $totalEntries = $zip.Entries.Count
    $zip.Dispose()

    if ($violations.Count -gt 0) {
        Write-Warning "Conteúdo proibido encontrado no ZIP ($($violations.Count) entradas):"
        $violations | ForEach-Object { Write-Warning $_ }
        Write-Warning "Revisar .gitignore ou excluir via --worktree-attributes."
    } else {
        Write-Host "  Nenhum conteúdo proibido detectado ($totalEntries entradas)."
    }

    # ── Sumário ──────────────────────────────────────────────────────────────
    Write-Host "[5/5] Entrega gerada com sucesso."
    Write-Host ""
    Write-Host "Resumo:"
    Write-Host "  Ref       : $Ref ($resolvedRef)"
    Write-Host "  Arquivo   : $AbsOutputPath"
    Write-Host "  Tamanho   : $sizeMB MB"
    Write-Host "  Entradas  : $totalEntries"
    if ($violations.Count -gt 0) {
        Write-Host "  Violações : $($violations.Count) — revisar antes de distribuir"
        exit 2
    }
    Write-Host "  Violações : nenhuma"

} finally {
    Pop-Location
}
