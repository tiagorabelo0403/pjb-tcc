from __future__ import annotations

import argparse
import math
import os
import re
import subprocess
import sys
from dataclasses import dataclass
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
MAX_TEXT_BYTES = 2 * 1024 * 1024
SKIPPED_PARTS = {
    ".git",
    ".idea",
    ".vscode",
    ".claude",
    ".codex",
    "target",
    "build",
    "out",
    "outcheck",
    "node_modules",
    "__pycache__",
    ".pytest_cache",
    ".mypy_cache",
    ".gradle",
    "logs",
}
ALLOWLIST_VALUES = {
    "changeme",
    "change_me",
    "replace_me",
    "example",
    "sample",
    "dummy",
    "placeholder",
    "password",
    "secret",
    "token",
}
ALLOWLIST_SUFFIXES = (
    ".example",
    ".sample",
    ".template",
    ".md",
)
PROPERTY_ASSIGNMENT_SUFFIXES = {
    ".conf",
    ".env",
    ".ini",
    ".properties",
    ".ps1",
    ".sh",
    ".tf",
    ".txt",
    ".yaml",
    ".yml",
}


@dataclass(frozen=True)
class Finding:
    path: Path
    rule: str


SECRET_PATTERNS = (
    ("private-key-block", re.compile(r"-----BEGIN [A-Z ]*PRIVATE KEY-----")),
    ("aws-access-key", re.compile(r"\b(?:AKIA|ASIA)[0-9A-Z]{16}\b")),
    ("github-token", re.compile(r"\b(?:ghp|gho|ghu|ghs|ghr)_[A-Za-z0-9_]{30,}\b")),
    ("github-fine-grained-token", re.compile(r"\bgithub_pat_[A-Za-z0-9_]{50,}\b")),
    ("openai-key", re.compile(r"\bsk-[A-Za-z0-9]{32,}\b")),
    ("slack-token", re.compile(r"\bxox[baprs]-[A-Za-z0-9-]{20,}\b")),
    ("google-api-key", re.compile(r"\bAIza[0-9A-Za-z_-]{35}\b")),
    ("jwt", re.compile(r"\beyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}\b")),
)
QUOTED_ASSIGNMENT_PATTERN = re.compile(
    r"(?i)\b(password|passwd|pwd|secret|token|api[_-]?key|client[_-]?secret|private[_-]?key)"
    r"\b\s*[:=]\s*(['\"])([^'\"\r\n]{12,})\2"
)
PROPERTY_ASSIGNMENT_PATTERN = re.compile(
    r"(?im)^\s*[A-Z0-9_.-]*(PASSWORD|PASSWD|PWD|SECRET|TOKEN|API[_-]?KEY|CLIENT[_-]?SECRET|PRIVATE[_-]?KEY)"
    r"[A-Z0-9_.-]*\s*[:=]\s*([^#\s]{12,})\s*$"
)
SENSITIVE_FILENAMES = re.compile(
    r"(?i)(^|[\\/])(\.env(?:\..*)?|id_rsa.*|id_ed25519.*|.*\.(?:pem|key|p8|p12|pfx|jks|keystore|kubeconfig))$"
)


def run_git(args: list[str]) -> list[str]:
    completed = subprocess.run(
        ["git", *args],
        cwd=ROOT,
        check=False,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.DEVNULL,
    )
    if completed.returncode != 0:
        return []
    return [line for line in completed.stdout.splitlines() if line.strip()]


def is_git_repo() -> bool:
    return bool(run_git(["rev-parse", "--show-toplevel"]))


def staged_paths() -> list[Path]:
    paths = run_git(["diff", "--cached", "--name-only", "--diff-filter=ACMRTUXB"])
    return [ROOT / path for path in paths]


def all_candidate_paths() -> list[Path]:
    if is_git_repo():
        paths = run_git(["ls-files", "--cached", "--others", "--exclude-standard"])
        return [ROOT / path for path in paths]
    return [path for path in ROOT.rglob("*") if path.is_file()]


def should_skip(path: Path) -> bool:
    try:
        relative = path.relative_to(ROOT)
    except ValueError:
        return True
    return any(part in SKIPPED_PARTS for part in relative.parts)


def looks_binary(data: bytes) -> bool:
    return b"\0" in data[:4096]


def is_allowlisted_value(value: str) -> bool:
    normalized = value.strip().strip("'\"").lower()
    if normalized in ALLOWLIST_VALUES:
        return True
    if normalized.startswith("${{") or normalized.startswith("${"):
        return True
    if normalized.startswith("replace_") or normalized.startswith("change_"):
        return True
    if normalized.startswith("trocar-por-"):
        return True
    return False


def should_scan_property_assignments(relative: Path) -> bool:
    name = relative.name.lower()
    if name.endswith(ALLOWLIST_SUFFIXES):
        return False
    if name.startswith(".env"):
        return True
    return relative.suffix.lower() in PROPERTY_ASSIGNMENT_SUFFIXES


def entropy(value: str) -> float:
    if not value:
        return 0.0
    frequencies = {character: value.count(character) for character in set(value)}
    return -sum((count / len(value)) * math.log2(count / len(value)) for count in frequencies.values())


def scan_path(path: Path) -> list[Finding]:
    findings: list[Finding] = []
    if should_skip(path) or not path.exists() or not path.is_file():
        return findings

    relative = path.relative_to(ROOT)
    relative_text = relative.as_posix()
    if SENSITIVE_FILENAMES.search(relative_text) and not relative_text.endswith(ALLOWLIST_SUFFIXES):
        findings.append(Finding(relative, "sensitive-filename"))

    try:
        data = path.read_bytes()
    except OSError:
        return findings
    if len(data) > MAX_TEXT_BYTES or looks_binary(data):
        return findings

    text = data.decode("utf-8", errors="ignore")
    for rule, pattern in SECRET_PATTERNS:
        if pattern.search(text):
            findings.append(Finding(relative, rule))

    for match in QUOTED_ASSIGNMENT_PATTERN.finditer(text):
        value = match.group(3)
        if is_allowlisted_value(value):
            continue
        if len(value) >= 20 or entropy(value) >= 3.6:
            findings.append(Finding(relative, "secret-assignment"))
            break

    if should_scan_property_assignments(relative):
        for match in PROPERTY_ASSIGNMENT_PATTERN.finditer(text):
            value = match.group(2)
            if is_allowlisted_value(value):
                continue
            if len(value) >= 20 or entropy(value) >= 3.6:
                findings.append(Finding(relative, "secret-assignment"))
                break
    return findings


def main() -> int:
    parser = argparse.ArgumentParser(description="Block obvious credentials before they reach Git history.")
    parser.add_argument("--staged", action="store_true", help="Scan staged files only.")
    parser.add_argument("--all", action="store_true", help="Scan tracked and untracked files that are not ignored.")
    args = parser.parse_args()

    paths = staged_paths() if args.staged else all_candidate_paths()
    findings: list[Finding] = []
    for path in paths:
        findings.extend(scan_path(path))

    if findings:
        print("Secret guard blocked this operation. Review these files before committing:", file=sys.stderr)
        for finding in sorted(set(findings), key=lambda item: (item.path.as_posix(), item.rule)):
            print(f"  {finding.path.as_posix()} [{finding.rule}]", file=sys.stderr)
        print("No secret values were printed. Rotate any credential that was ever committed.", file=sys.stderr)
        return 1

    print("Secret guard: no obvious credentials found.")
    return 0


if __name__ == "__main__":
    if os.name == "nt":
        sys.stdout.reconfigure(encoding="utf-8")
        sys.stderr.reconfigure(encoding="utf-8")
    raise SystemExit(main())
