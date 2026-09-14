#!/usr/bin/env python3
from __future__ import annotations

import re
import sys
from pathlib import Path

try:
    import yaml
except ImportError:
    yaml = None

ROOT = Path(__file__).resolve().parent.parent
MAIN = ROOT / "pjb-api" / "src" / "main"
JAVA = MAIN / "java"
RESOURCES = MAIN / "resources"

CONDICIONAL = re.compile(r"@ConditionalOnProperty\(([^)]*)\)", re.S)
MATCH_IF_MISSING = re.compile(r"matchIfMissing\s*=\s*true")
NOME = re.compile(r"name\s*=\s*\"([^\"]+)\"")
VALOR = re.compile(r"value\s*=\s*\"([^\"]+)\"")
PREFIXO = re.compile(r"prefix\s*=\s*\"([^\"]+)\"")

PREFIXOS_DINAMICOS = ("pjb.runtime.barrier.",)


def achatar(no, prefixo: str = "") -> set[str]:
    chaves: set[str] = set()
    if isinstance(no, dict):
        for chave, valor in no.items():
            caminho = f"{prefixo}.{chave}" if prefixo else str(chave)
            chaves.add(caminho)
            chaves |= achatar(valor, caminho)
    return chaves


def chaves_declaradas() -> set[str]:
    chaves: set[str] = set()
    if not RESOURCES.exists():
        return chaves
    for arquivo in RESOURCES.rglob("*"):
        if arquivo.suffix in {".yml", ".yaml"} and yaml is not None:
            try:
                for documento in yaml.safe_load_all(arquivo.read_text(encoding="utf-8", errors="ignore")):
                    chaves |= achatar(documento)
            except Exception:
                continue
        elif arquivo.suffix == ".properties":
            for linha in arquivo.read_text(encoding="utf-8", errors="ignore").splitlines():
                if "=" in linha and not linha.strip().startswith("#"):
                    chaves.add(linha.split("=", 1)[0].strip())
    return chaves


def main() -> int:
    if yaml is None:
        print("CONDITIONAL PROPERTY DECLARED GUARD: OK (pyyaml ausente, verificacao pulada)")
        return 0

    declaradas = chaves_declaradas()
    faltando: list[tuple[str, str]] = []

    for arquivo in JAVA.rglob("*.java"):
        texto = arquivo.read_text(encoding="utf-8", errors="ignore")
        if "@ConditionalOnProperty" not in texto:
            continue
        for bloco in CONDICIONAL.finditer(texto):
            corpo = bloco.group(1)
            if MATCH_IF_MISSING.search(corpo):
                continue
            nome = NOME.search(corpo) or VALOR.search(corpo)
            if not nome:
                continue
            prefixo = PREFIXO.search(corpo)
            propriedade = f"{prefixo.group(1)}.{nome.group(1)}" if prefixo else nome.group(1)
            if propriedade.startswith(PREFIXOS_DINAMICOS):
                continue
            if propriedade not in declaradas:
                faltando.append((propriedade, arquivo.stem))

    if not faltando:
        print("CONDITIONAL PROPERTY DECLARED GUARD: OK")
        return 0

    print("CONDITIONAL PROPERTY DECLARED GUARD: FAIL")
    print(
        "Beans exigem propriedade que nao existe em nenhum application*.yml/properties e nao tem "
        "matchIfMissing. Ficam desligados em todo ambiente, e o operador nao tem como descobrir "
        "que a flag existe:"
    )
    for propriedade, classe in sorted(set(faltando)):
        print(f" - {propriedade}  ({classe})")
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
