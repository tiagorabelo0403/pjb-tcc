#!/usr/bin/env python3
"""
Detecta (e opcionalmente encerra) JVMs de teste orfas do Surefire/Failsafe.

Contexto: quando um `mvnw test`/`verify` e morto abruptamente (SIGKILL do ambiente/CI),
a JVM forkada de teste (surefirebooter, -Xmx alto) sobrevive sem processo pai para reapea-la
e vai acumulando ate sufocar a memoria da maquina, matando os proximos runs. Nao e flag de JVM
errada — e processo zumbi. Este guard ataca exatamente isso.

Uso:
  python scripts/reap_orphan_test_jvms.py           # report-only: lista orfas, exit 1 se achar
  python scripts/reap_orphan_test_jvms.py --kill    # encerra as orfas e sai 0

Multiplataforma (Windows via PowerShell/CIM, Linux/macOS via ps), somente stdlib.
"""
from __future__ import annotations

import argparse
import os
import platform
import re
import subprocess
import sys

FORK_MARKERS = ("surefirebooter", "surefire_", "failsafe")


def _java_processes() -> list[tuple[int, int, str]]:
    system = platform.system()
    procs: list[tuple[int, int, str]] = []
    if system == "Windows":
        script = (
            "Get-CimInstance Win32_Process -Filter \"Name='java.exe'\" | "
            "ForEach-Object { \"$($_.ProcessId)`t$($_.ParentProcessId)`t$($_.CommandLine)\" }"
        )
        completed = subprocess.run(
            ["powershell", "-NoProfile", "-NonInteractive", "-Command", script],
            capture_output=True, text=True, timeout=90,
        )
        for line in completed.stdout.splitlines():
            parts = line.split("\t", 2)
            if len(parts) < 2:
                continue
            try:
                pid, ppid = int(parts[0]), int(parts[1])
            except ValueError:
                continue
            procs.append((pid, ppid, parts[2] if len(parts) > 2 else ""))
    else:
        completed = subprocess.run(
            ["ps", "-eo", "pid=,ppid=,command="], capture_output=True, text=True, timeout=90,
        )
        for line in completed.stdout.splitlines():
            match = re.match(r"\s*(\d+)\s+(\d+)\s+(.*)", line)
            if match and "java" in match.group(3):
                procs.append((int(match.group(1)), int(match.group(2)), match.group(3)))
    return procs


def _pid_alive(pid: int) -> bool:
    if pid <= 0:
        return False
    if platform.system() == "Windows":
        completed = subprocess.run(
            ["tasklist", "/FI", f"PID eq {pid}", "/NH"], capture_output=True, text=True,
        )
        return str(pid) in completed.stdout
    try:
        os.kill(pid, 0)
        return True
    except PermissionError:
        return True
    except OSError:
        return False


def _is_test_fork(cmdline: str) -> bool:
    lowered = cmdline.lower()
    return any(marker in lowered for marker in FORK_MARKERS)


def _is_orphan(ppid: int) -> bool:
    # Linux/macOS reparenta orfaos para PID 1 (ou subreaper); Windows deixa o ppid apontando
    # para um PID morto. Ambos os casos = sem pai vivo legitimo.
    if ppid == 1:
        return True
    return not _pid_alive(ppid)


def _kill(pid: int) -> bool:
    try:
        if platform.system() == "Windows":
            subprocess.run(["taskkill", "/F", "/PID", str(pid)], capture_output=True, text=True)
        else:
            os.kill(pid, 9)
        return True
    except Exception:
        return False


def main() -> int:
    parser = argparse.ArgumentParser(description="Detecta/encerra JVMs de teste orfas do Surefire/Failsafe.")
    parser.add_argument("--kill", action="store_true", help="Encerra as JVMs orfas encontradas (padrao: apenas reporta).")
    args = parser.parse_args()

    orphans = [
        (pid, ppid, cmd)
        for pid, ppid, cmd in _java_processes()
        if _is_test_fork(cmd) and _is_orphan(ppid)
    ]

    if not orphans:
        print("REAP ORPHAN TEST JVMS: OK")
        print("nenhuma JVM de teste orfa (surefire/failsafe) encontrada")
        return 0

    print("REAP ORPHAN TEST JVMS: FOUND")
    for pid, ppid, cmd in orphans:
        snippet = cmd if len(cmd) <= 120 else cmd[:117] + "..."
        print(f" - pid={pid} ppid_morto={ppid} :: {snippet}")

    if not args.kill:
        print(f"\n{len(orphans)} JVM(s) de teste orfa(s) segurando memoria. Rode com --kill para encerrar.")
        return 1

    reaped = sum(1 for pid, _, _ in orphans if _kill(pid))
    print(f"\nencerradas {reaped}/{len(orphans)} JVM(s) de teste orfa(s).")
    return 0 if reaped == len(orphans) else 1


if __name__ == "__main__":
    raise SystemExit(main())
