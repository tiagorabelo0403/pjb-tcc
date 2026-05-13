#!/usr/bin/env python3
from __future__ import annotations
import json
import re
from pathlib import Path

from project_roots import SRC_MAIN

ALLOWED_PREFIXES = (
    'com/tcc/pjb/backend/platform/runtime/',
    'com/tcc/pjb/backend/platform/concurrent/',
)

RULES = {
    'rawCompletableFutureAsync': re.compile(r'(?:CompletableFuture\s*\.\s*)?(?:supplyAsync|runAsync)\s*\('),
    'rawCompletionStageAsync': re.compile(r'\.(handleAsync|thenApplyAsync|thenAcceptAsync|thenRunAsync|thenComposeAsync|whenCompleteAsync|exceptionallyAsync)\s*\('),
    'rawParallelStream': re.compile(r'\.parallelStream\s*\('),
    'rawExecutorFactory': re.compile(r'Executors\.(newFixedThreadPool|newCachedThreadPool|newWorkStealingPool|newScheduledThreadPool|newSingleThreadExecutor|newSingleThreadScheduledExecutor)\s*\('),
    'rawSynchronizedMethod': re.compile(r'(^|\n)\s*(public|protected|private)\s+synchronized\s+', re.MULTILINE),
    'asyncAnnotation': re.compile(r'(^|\n)\s*@Async(\s*\(|\b)', re.MULTILINE),
}


def allowed(path: Path) -> bool:
    posix = path.as_posix()
    return any(prefix in posix for prefix in ALLOWED_PREFIXES)


def scan_file(path: Path) -> dict[str, list[int]]:
    text = path.read_text(encoding='utf-8', errors='ignore')
    findings: dict[str, list[int]] = {}
    for name, pattern in RULES.items():
        lines = []
        for match in pattern.finditer(text):
            line = text.count('\n', 0, match.start()) + 1
            lines.append(line)
        if lines:
            findings[name] = lines
    if '@Async' in text and 'CompletableFuture.supplyAsync' in text:
        findings['rawNestedAsyncBoundary'] = [line_no for line_no, line in enumerate(text.splitlines(), start=1) if '@Async' in line or 'CompletableFuture.supplyAsync' in line][:6]
    return findings


def main() -> None:
    report = {
        'summary': {
            'filesScanned': 0,
            'filesFlagged': 0,
            'findings': {key: 0 for key in RULES},
        },
        'flaggedFiles': [],
    }
    for path in sorted(SRC_MAIN.rglob('*.java')):
        report['summary']['filesScanned'] += 1
        if allowed(path):
            continue
        findings = scan_file(path)
        if not findings:
            continue
        report['summary']['filesFlagged'] += 1
        for key, lines in findings.items():
            report['summary']['findings'].setdefault(key, 0)
            report['summary']['findings'][key] += len(lines)
        report['flaggedFiles'].append({
            'file': str(path.relative_to(SRC_MAIN)),
            'findings': findings,
        })
    print(json.dumps(report, indent=2, ensure_ascii=False))


if __name__ == '__main__':
    main()
