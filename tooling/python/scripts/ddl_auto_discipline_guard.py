#!/usr/bin/env python3
"""Reprova `ddl-auto: update` num perfil que usa banco real com Flyway ligado.

`update` manda o Hibernate alterar o schema sozinho para casar com as entidades. Num perfil que
roda Flyway sobre PostgreSQL, isso significa que as migrations constroem o schema e o Hibernate
**remenda por cima** o que elas não criaram. O efeito prático é que a migration faltante fica
invisível para quem desenvolve: o banco local funciona, e o erro só aparece onde `ddl-auto` é
`validate` — `docker` e `prod`, ou seja, no deploy.

Era o caso do perfil `dev` até 2026-09-14.

## A distinção que este guard faz

`update` **não** é errado em qualquer lugar. Em `local` e `frontend-dev` o banco é H2 em memória e o
Flyway está desligado de propósito — as 318 migrations são PostgreSQL puro (`pgvector`,
`CREATE EXTENSION vector`) e não rodam em H2. Ali o Hibernate é a única coisa que cria o schema, e
trocar para `validate` deixaria o perfil sem schema nenhum.

A regra, então, não é "nunca use update". É: **`update` + banco real + Flyway ligado = migration
faltante fica escondida.** Essa combinação é que reprova.

## O que ele NÃO cobre

Não olha `ddl-auto` definido por variável de ambiente ou `@DynamicPropertySource` — e essa lacuna é
concreta, não teórica: `PjbIntegrationTestBase` fixava `ddl-auto` por `@DynamicPropertySource`, que
tem precedência sobre YAML, e tornava inerte o valor do perfil. Um guard que só lê YAML teria dito
OK enquanto nada era validado.
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

from project_roots import APP_MODULE

RESOURCES = APP_MODULE / "src" / "main" / "resources"

DDL_AUTO = re.compile(r"^\s*ddl-auto:\s*([a-z-]+)\s*$", re.MULTILINE)
FLYWAY_ENABLED = re.compile(r"flyway:\s*\n(?:\s+.*\n)*?\s*enabled:\s*(true|false)", re.MULTILINE)
URL = re.compile(r"^\s*url:\s*'?\"?([^'\"\n]+)", re.MULTILINE)


def _perfil(arquivo: Path) -> str:
    nome = arquivo.stem
    return nome[len("application-"):] if nome.startswith("application-") else "base"


def main() -> int:
    if not RESOURCES.is_dir():
        print("DDL AUTO DISCIPLINE GUARD: FAIL")
        print(f" - diretorio de resources nao encontrado: {RESOURCES}")
        return 1

    base = RESOURCES / "application.yml"
    flyway_na_base = True
    if base.exists():
        encontrado = FLYWAY_ENABLED.search(base.read_text(encoding="utf-8"))
        flyway_na_base = (encontrado.group(1) == "true") if encontrado else True

    problemas: list[str] = []
    conferidos = 0

    for arquivo in sorted(RESOURCES.glob("application*.yml")):
        conteudo = arquivo.read_text(encoding="utf-8")
        ddl = DDL_AUTO.search(conteudo)
        if not ddl:
            continue
        conferidos += 1
        if ddl.group(1) != "update":
            continue

        url = URL.search(conteudo)
        banco_em_memoria = bool(url and "jdbc:h2:mem" in url.group(1))

        flyway = FLYWAY_ENABLED.search(conteudo)
        flyway_ligado = (flyway.group(1) == "true") if flyway else flyway_na_base

        if not banco_em_memoria and flyway_ligado:
            problemas.append(
                f"{arquivo.name}: ddl-auto: update com banco real e Flyway ligado. As migrations "
                f"constroem o schema e o Hibernate remenda por cima o que elas nao criaram — "
                f"migration faltante fica invisivel aqui e so explode onde o ddl-auto e validate. "
                f"Use validate."
            )

    if problemas:
        print("DDL AUTO DISCIPLINE GUARD: FAIL")
        for problema in problemas:
            print(f" - {problema}")
        return 1

    print("DDL AUTO DISCIPLINE GUARD: OK")
    print(f"{conferidos} perfis com ddl-auto declarado; nenhum usa update sobre banco real com Flyway")
    return 0


if __name__ == "__main__":
    sys.exit(main())
