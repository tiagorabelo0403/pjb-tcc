#!/usr/bin/env python3
"""Reprova migration que quebra banco que JÁ migrou — a classe de defeito que o CI não pega.

O motivo de as três checagens viverem juntas é uma só: **o CI sempre parte de banco vazio.**
Testcontainers sobe um Postgres novo e aplica as 317 migrations em ordem crescente, então tudo
passa. O estrago aparece onde já existe dado — dev, staging, produção. Um defeito dessa família
atravessa o portão de integração verde e só se manifesta no deploy.

Aconteceu de verdade em 2026-09-14, na PR #21: ela adicionava `V335` porque em 21/08, quando foi
escrita, 335 era o próximo número livre. O `master` já estava em V354, e V335 virou um buraco no
meio da sequência. O projeto não configura `out-of-order`, então vale o padrão do Flyway
(`out-of-order=false`, `validate-on-migrate=true`): um banco que já aplicou até V354 recebe uma
migration com versão MENOR que o estado atual, o Flyway recusa, e a aplicação não sobe.

Peguei à mão. Este guard existe para que a próxima não dependa de alguém reparar.

## O que ele checa

1. **Versão duplicada** — dois arquivos com o mesmo `V<n>`. Já existia.
2. **Versão que regride** — migration nova com versão menor ou igual à maior já commitada na base.
   Há 39 buracos na numeração hoje (22-29, 34-35, 51-59, 63-69, 178-188, 216, 335); cada um é um
   número livre que uma PR longa pode ocupar sem perceber.
3. **Conteúdo alterado** — migration já commitada que muda de conteúdo. `validate-on-migrate=true`
   compara checksum: qualquer banco que já aplicou aquela versão passa a recusar o boot e exige
   `flyway repair`. O README documenta um episódio assim com a `V317`.

## O que ele NÃO checa

Semântica do SQL. Uma migration com versão correta e conteúdo novo pode continuar sendo
destrutiva — `DROP COLUMN` num banco com dado não é problema de numeração, e este guard não
promete pegar.

As checagens 2 e 3 comparam contra a base da branch via `git`. Sem histórico suficiente o guard
REPROVA dizendo isso, em vez de passar sem verificar: guard que não acha o que procura e responde
OK é o defeito que ele existe para evitar.
"""

from __future__ import annotations

import re
import subprocess
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MIGRATION_DIR = ROOT / "pjb-api" / "src" / "main" / "resources" / "db" / "migration"
CAMINHO_RELATIVO = "pjb-api/src/main/resources/db/migration"
PATTERN = re.compile(r"^(V(\d+))__.+\.sql$")


def _git(*args: str) -> str:
    resultado = subprocess.run(
        ["git", "-C", str(ROOT), *args], capture_output=True, text=True, encoding="utf-8"
    )
    if resultado.returncode != 0:
        raise RuntimeError(resultado.stderr.strip() or " ".join(args))
    return resultado.stdout


def _base_de_comparacao() -> str:
    """Commit contra o qual medir "nova" e "alterada".

    Em PR, o ponto em que a branch saiu do master. Em push para master, o commit anterior.
    """
    for referencia in ("origin/master", "master"):
        try:
            base = _git("merge-base", referencia, "HEAD").strip()
        except RuntimeError:
            continue
        if base and base != _git("rev-parse", "HEAD").strip():
            return base
        try:
            return _git("rev-parse", "HEAD~1").strip()
        except RuntimeError:
            continue
    raise RuntimeError(
        "nao foi possivel achar a base de comparacao (origin/master ou HEAD~1). "
        "Num checkout do GitHub Actions isso costuma ser clone raso: use fetch-depth: 0."
    )


def _migrations_em(commit: str) -> dict[str, str]:
    """Nome do arquivo -> hash do blob, no commit dado."""
    saida = _git("ls-tree", "-r", commit, "--", CAMINHO_RELATIVO)
    encontradas: dict[str, str] = {}
    for linha in saida.splitlines():
        if not linha.strip():
            continue
        meta, _, caminho = linha.partition("\t")
        partes = meta.split()
        if len(partes) < 3:
            continue
        nome = Path(caminho).name
        if PATTERN.match(nome):
            encontradas[nome] = partes[2]
    return encontradas


def _migrations_no_disco() -> dict[str, str]:
    """Nome do arquivo -> hash do blob do conteúdo EM DISCO.

    Deliberadamente o disco, e não a árvore de `HEAD`: assim uma migration ainda não commitada já é
    verificada localmente, antes de virar commit. Em CI os dois coincidem.
    """
    encontradas: dict[str, str] = {}
    for caminho in sorted(MIGRATION_DIR.glob("V*__*.sql")):
        if PATTERN.match(caminho.name):
            encontradas[caminho.name] = _git("hash-object", str(caminho)).strip()
    return encontradas


def _versao(nome: str) -> int:
    return int(PATTERN.match(nome).group(2))


def main() -> int:
    if not MIGRATION_DIR.exists():
        print("FLYWAY MIGRATION VERSION GUARD: FAIL")
        print(f" - missing migration directory: {CAMINHO_RELATIVO}")
        return 1

    problemas: list[str] = []

    # 1. versao duplicada — puramente do disco, nao depende de git
    versoes: dict[str, list[str]] = defaultdict(list)
    for path in sorted(MIGRATION_DIR.glob("V*__*.sql")):
        match = PATTERN.match(path.name)
        if match:
            versoes[match.group(1)].append(path.name)
    for versao, nomes in sorted(versoes.items()):
        if len(nomes) > 1:
            problemas.append(f"versao duplicada {versao}: {', '.join(sorted(nomes))}")

    # 2 e 3 — comparam contra a base
    novas: list[str] = []
    alteradas: list[str] = []
    maior_na_base = 0
    try:
        base = _base_de_comparacao()
        na_base = _migrations_em(base)
        agora = _migrations_no_disco()
        maior_na_base = max((_versao(n) for n in na_base), default=0)

        for nome, blob in sorted(agora.items()):
            if nome not in na_base:
                novas.append(nome)
                if _versao(nome) <= maior_na_base:
                    problemas.append(
                        f"migration nova com versao que regride: {nome} (V{_versao(nome)}) "
                        f"<= maior ja commitada (V{maior_na_base}). Com out-of-order=false, banco "
                        f"que ja aplicou ate V{maior_na_base} recusa esta e nao sobe. "
                        f"Renumere para V{maior_na_base + 1} ou maior."
                    )
            elif na_base[nome] != blob:
                alteradas.append(nome)
                problemas.append(
                    f"migration ja commitada teve o conteudo alterado: {nome}. Com "
                    f"validate-on-migrate=true, todo banco que ja a aplicou passa a recusar o boot "
                    f"por checksum divergente e exige flyway repair. Crie uma migration nova."
                )
    except RuntimeError as erro:
        problemas.append(str(erro))

    if problemas:
        print("FLYWAY MIGRATION VERSION GUARD: FAIL")
        print("Defeito que o CI nao pega, porque o CI sempre parte de banco vazio:")
        for problema in problemas:
            print(f" - {problema}")
        return 1

    maior_agora = max((int(v[1:]) for v in versoes), default=0)
    print("FLYWAY MIGRATION VERSION GUARD: OK")
    print(f"{len(versoes)} migrations, maior versao V{maior_agora}")
    print(f"nesta mudanca: {len(novas)} nova(s), {len(alteradas)} alterada(s); "
          f"maior na base era V{maior_na_base}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
