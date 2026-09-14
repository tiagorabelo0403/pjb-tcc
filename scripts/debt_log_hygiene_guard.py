#!/usr/bin/env python3
from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DEBT_LOG = ROOT / "docs" / "quality" / "DEBT_LOG.md"

CABECALHO_ENTRADA = re.compile(r"^## (D-[\w\-]+)", re.M)
CAMPO_STATUS = re.compile(r"\*\*Status:\*\*\s*([^\n]+)")
STATUS_FECHADA = re.compile(r"^\s*\**\s*fechad[ao]\b", re.IGNORECASE)


def entradas(texto: str) -> list[tuple[str, str]]:
    partes = re.split(r"\n(?=## D-)", texto)
    resultado = []
    for parte in partes:
        cabecalho = CABECALHO_ENTRADA.match(parte.lstrip("\n"))
        if cabecalho:
            resultado.append((cabecalho.group(1), parte))
    return resultado


def main() -> int:
    if not DEBT_LOG.exists():
        print("DEBT LOG HYGIENE GUARD: OK (registro ausente)")
        return 0

    texto = DEBT_LOG.read_text(encoding="utf-8")
    sem_status: list[str] = []
    fechadas: list[str] = []

    for nome, bloco in entradas(texto):
        campo = CAMPO_STATUS.search(bloco)
        if not campo:
            sem_status.append(nome)
            continue
        if STATUS_FECHADA.match(campo.group(1)):
            fechadas.append(nome)

    if not sem_status and not fechadas:
        print("DEBT LOG HYGIENE GUARD: OK")
        return 0

    print("DEBT LOG HYGIENE GUARD: FAIL")
    if sem_status:
        print(
            "Entradas sem campo **Status:**. Sem ele o registro nao e legivel por ferramenta "
            "e a triagem vira leitura manual:"
        )
        for nome in sem_status:
            print(f" - {nome}")
    if fechadas:
        print(
            "Entradas marcadas como fechadas ainda no registro. O cabecalho do proprio arquivo "
            "diz que cada entrada sai daqui quando a divida e fechada; manter dividas quitadas "
            "esconde o backlog real:"
        )
        for nome in fechadas:
            print(f" - {nome}")
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
