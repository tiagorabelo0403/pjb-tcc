from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SCRIPT = ROOT / "scripts" / "pjb-api-clean-test-errors.ps1"


def main() -> int:
    text = SCRIPT.read_text(encoding="utf-8")
    required = (
        "cmd.exe /D /S /C",
        "ForEach-Object",
        "[System.IO.File]::AppendAllText",
        "Write-Host $line",
        "$LASTEXITCODE",
        "$ErrorActionPreference = \"Continue\"",
        "2>&1",
    )
    missing = [item for item in required if item not in text]
    forbidden = (
        "2>&1 | Tee-Object",
        "mvnw.cmd -pl $Module clean test -DtrimStackTrace=false 2>&1",
        "[System.Diagnostics.Process]::new()",
        "RedirectStandardOutput",
        "RedirectStandardError",
        "BeginOutputReadLine()",
        "BeginErrorReadLine()",
    )
    violations = [item for item in forbidden if item in text]
    if missing or violations:
        if missing:
            print("MISSING:" + ",".join(missing))
        if violations:
            print("FORBIDDEN:" + ",".join(violations))
        return 1
    print("powershell_test_collector_guard: OK")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
