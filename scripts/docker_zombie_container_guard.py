#!/usr/bin/env python3
"""
Detecta (e opcionalmente remove) containers Docker zumbis: unhealthy ha muito
tempo ou presos em loop de restart, consumindo CPU/RAM sem servir a nenhum
proposito.

Contexto: um container `pjb-backend-1` rodou 7 dias em loop de retry do
Flyway porque seu Postgres de dependencia nunca foi de fato iniciado,
consumindo 8.45GB de uma VM de 11.6GB do Docker Desktop sozinho e travando
qualquer outro container — incluindo os que o Testcontainers precisa subir
num `mvnw verify`. Este guard ataca exatamente esse padrao.

Uso:
  python scripts/docker_zombie_container_guard.py           # report-only: lista zumbis, exit 1 se achar
  python scripts/docker_zombie_container_guard.py --kill    # para e remove os zumbis encontrados

Multiplataforma (usa apenas o CLI `docker`, presume-se no PATH), somente stdlib.
Se o daemon Docker estiver indisponivel, sai 0 silenciosamente (nada a verificar).
"""
from __future__ import annotations

import argparse
import json
import subprocess
from datetime import datetime, timezone

DEFAULT_UNHEALTHY_THRESHOLD_MINUTES = 30
DEFAULT_RESTART_COUNT_THRESHOLD = 5


def _docker_available() -> bool:
    completed = subprocess.run(
        ["docker", "version", "--format", "{{.Server.Version}}"],
        capture_output=True, text=True, timeout=15,
    )
    return completed.returncode == 0


def _container_ids() -> list[str]:
    completed = subprocess.run(
        ["docker", "ps", "-aq"], capture_output=True, text=True, timeout=30,
    )
    if completed.returncode != 0:
        return []
    return [line.strip() for line in completed.stdout.splitlines() if line.strip()]


def _inspect(container_id: str) -> dict | None:
    completed = subprocess.run(
        ["docker", "inspect", container_id], capture_output=True, text=True, timeout=30,
    )
    if completed.returncode != 0:
        return None
    try:
        data = json.loads(completed.stdout)
    except json.JSONDecodeError:
        return None
    return data[0] if data else None


def _parse_docker_time(value: str) -> datetime | None:
    if not value or value.startswith("0001-01-01"):
        return None
    # Docker retorna RFC3339 com nanossegundos; fromisoformat so aceita ate 6 digitos de fracao.
    if "." in value:
        head, frac_and_tz = value.split(".", 1)
        frac, tz = frac_and_tz, ""
        for i, ch in enumerate(frac_and_tz):
            if not ch.isdigit():
                frac, tz = frac_and_tz[:i], frac_and_tz[i:]
                break
        value = f"{head}.{frac[:6]}{tz}"
    value = value.replace("Z", "+00:00")
    try:
        return datetime.fromisoformat(value)
    except ValueError:
        return None


def _diagnose(info: dict, unhealthy_threshold_minutes: int, restart_count_threshold: int) -> str | None:
    state = info.get("State", {})
    health = state.get("Health", {})
    health_status = health.get("Status")
    restart_count = info.get("RestartCount", 0)
    started_at = _parse_docker_time(state.get("StartedAt", ""))

    if health_status == "unhealthy" and started_at is not None:
        age_minutes = (datetime.now(timezone.utc) - started_at).total_seconds() / 60
        if age_minutes >= unhealthy_threshold_minutes:
            return f"unhealthy ha {age_minutes / 60:.1f}h (limite: {unhealthy_threshold_minutes}min)"

    if restart_count >= restart_count_threshold:
        return f"{restart_count} restarts (limite: {restart_count_threshold})"

    return None


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Detecta/remove containers Docker zumbis (unhealthy prolongado ou restart loop).",
    )
    parser.add_argument("--kill", action="store_true", help="Para e remove os zumbis encontrados (padrao: apenas reporta).")
    parser.add_argument(
        "--unhealthy-threshold-minutes", type=int, default=DEFAULT_UNHEALTHY_THRESHOLD_MINUTES,
        help=f"Minutos de unhealthy continuo antes de considerar zumbi (padrao: {DEFAULT_UNHEALTHY_THRESHOLD_MINUTES}).",
    )
    parser.add_argument(
        "--restart-count-threshold", type=int, default=DEFAULT_RESTART_COUNT_THRESHOLD,
        help=f"Numero de restarts antes de considerar zumbi (padrao: {DEFAULT_RESTART_COUNT_THRESHOLD}).",
    )
    args = parser.parse_args()

    if not _docker_available():
        print("DOCKER ZOMBIE CONTAINER GUARD: SKIP")
        print("docker daemon indisponivel — nada a verificar")
        return 0

    zombies: list[tuple[str, str, str]] = []
    for container_id in _container_ids():
        info = _inspect(container_id)
        if info is None:
            continue
        reason = _diagnose(info, args.unhealthy_threshold_minutes, args.restart_count_threshold)
        if reason:
            name = info.get("Name", "").lstrip("/") or container_id[:12]
            zombies.append((container_id, name, reason))

    if not zombies:
        print("DOCKER ZOMBIE CONTAINER GUARD: OK")
        print("nenhum container zumbi (unhealthy prolongado ou restart loop) encontrado")
        return 0

    print("DOCKER ZOMBIE CONTAINER GUARD: FOUND")
    for container_id, name, reason in zombies:
        print(f" - {name} ({container_id[:12]}): {reason}")

    if not args.kill:
        print(f"\n{len(zombies)} container(es) zumbi(s) segurando CPU/RAM. Rode com --kill para parar e remover.")
        return 1

    stopped = 0
    for container_id, name, _ in zombies:
        stop = subprocess.run(["docker", "stop", container_id], capture_output=True, text=True, timeout=60)
        rm = subprocess.run(["docker", "rm", container_id], capture_output=True, text=True, timeout=30)
        if stop.returncode == 0 and rm.returncode == 0:
            stopped += 1
        else:
            print(f"falha ao remover {name} ({container_id[:12]})")
    print(f"\nremovidos {stopped}/{len(zombies)} container(es) zumbi(s).")
    return 0 if stopped == len(zombies) else 1


if __name__ == "__main__":
    raise SystemExit(main())
