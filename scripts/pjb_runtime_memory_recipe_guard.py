#!/usr/bin/env python3
"""
Guard de regressao da receita anti-killed do pjb-runtime.sh.

Contexto: o script docker/pjb-runtime.sh calcula MaxRAMPercentage, InitialRAMPercentage,
MaxMetaspaceSize, MaxDirectMemorySize e ReservedCodeCache por tamanho do cgroup memory.max,
com reserva nativa proporcional (24-34%) — a receita que evita o container ser morto por OOM
sem heap dump. Este guard fixa a tabela de decisao (tamanho -> valores esperados) como
teste de regressao: se alguem alterar a formula sem atualizar o guard, a mudanca fica
visivel imediatamente.

Uso:
  python scripts/pjb_runtime_memory_recipe_guard.py

Roda o script real com bash, injetando PJB_JVM_* vazio e simulando cgroup via override
das funcoes bash, comparando saida com a tabela esperada. Sem docker, sem cgroup real.
"""
from __future__ import annotations

import shutil
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SCRIPT = ROOT / "pjb-api" / "src" / "main" / "resources" / "docker" / "pjb-runtime.sh"

# (limite_bytes, max_ram%, initial%, metaspace, direct_mem, code_cache, native_reserve%)
# Tabela derivada diretamente da leitura das funcoes resolve_*() no script atual.
# Se qualquer valor mudar, o guard falha e forca revisao consciente.
EXPECTED = [
    # 512Mi
    (536_870_912, 66, 6, "256m", "96m", "128m", 34),
    # 1Gi (fronteira inclusiva)
    (1_073_741_824, 66, 6, "256m", "96m", "128m", 34),
    # 2Gi
    (2_147_483_648, 70, 10, "384m", "128m", "128m", 30),
    # 4Gi
    (4_294_967_296, 72, 10, "384m", "192m", "192m", 26),
    # 8Gi
    (8_589_934_592, 72, 12, "512m", "256m", "192m", 24),
    # 16Gi (>8Gi branch)
    (17_179_869_184, 72, 12, "768m", "384m", "192m", 24),
]


def _run_bash(limit_bytes: int, cpus: int = 2) -> dict[str, str]:
    """Fonte o script e chama as funcoes puras, substituindo as detect_* por stubs."""
    bash = shutil.which("bash")
    if bash is None:
        raise SystemExit("bash nao encontrado no PATH (necessario para testar pjb-runtime.sh)")
    stub = f"""
set -euo pipefail
detect_memory_limit_bytes() {{ echo {limit_bytes}; }}
detect_cpu_limit() {{ echo {cpus}; }}
export -f detect_memory_limit_bytes detect_cpu_limit
source_only() {{
  # extrai so as funcoes resolve_*, ignora o "exec java" no final
  sed -n '/^resolve_/,/^}}$/p' "$1"
}}
eval "$(source_only "{SCRIPT.as_posix()}")"
echo "max_ram=$(resolve_max_ram_percentage)"
echo "initial=$(resolve_initial_ram_percentage)"
echo "metaspace=$(resolve_max_metaspace)"
echo "direct_mem=$(resolve_max_direct_memory)"
echo "code_cache=$(resolve_reserved_code_cache)"
echo "native_reserve=$(resolve_native_reserve_percentage)"
"""
    result = subprocess.run(
        [bash, "-c", stub],
        capture_output=True, text=True, timeout=30, cwd=str(ROOT),
    )
    if result.returncode != 0:
        raise SystemExit(f"bash falhou (limit={limit_bytes}): {result.stderr}")
    out: dict[str, str] = {}
    for line in result.stdout.splitlines():
        if "=" in line:
            k, v = line.split("=", 1)
            out[k.strip()] = v.strip()
    return out


def main() -> int:
    if not SCRIPT.exists():
        print("PJB RUNTIME MEMORY RECIPE GUARD: FAIL")
        print(f" - script ausente: {SCRIPT.relative_to(ROOT)}")
        return 1

    problems: list[str] = []
    for limit, exp_max, exp_init, exp_meta, exp_direct, exp_code, exp_native in EXPECTED:
        actual = _run_bash(limit)
        checks = [
            ("max_ram", str(exp_max)),
            ("initial", str(exp_init)),
            ("metaspace", exp_meta),
            ("direct_mem", exp_direct),
            ("code_cache", exp_code),
            ("native_reserve", str(exp_native)),
        ]
        for key, expected in checks:
            got = actual.get(key)
            if got != expected:
                problems.append(f"limit={limit} bytes: {key} esperado={expected} obtido={got}")

    if problems:
        print("PJB RUNTIME MEMORY RECIPE GUARD: FAIL")
        for p in problems:
            print(f" - {p}")
        print("\nSe a formula mudou intencionalmente, atualize EXPECTED em scripts/pjb_runtime_memory_recipe_guard.py.")
        return 1

    print("PJB RUNTIME MEMORY RECIPE GUARD: OK")
    print(f"validated: {SCRIPT.relative_to(ROOT)}")
    print(f"cenarios validados: {len(EXPECTED)} (512Mi, 1Gi, 2Gi, 4Gi, 8Gi, 16Gi)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
