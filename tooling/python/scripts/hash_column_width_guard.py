#!/usr/bin/env python3
r"""Reprova coluna que afirma guardar hash e não cabe num SHA-256.

Um SHA-256 em hexadecimal tem 64 caracteres. Uma coluna `varchar(32)` que recebe um aceita a
inserção até o dia em que recebe, e aí o Postgres devolve `value too long for type character
varying(32)` — em produção, com dado real, longe do CI.

Aconteceu neste projeto: `tb_cadeia_custodia_digital.evidence_chave_custodia` era `varchar(32)` e
recebia `Hashes.sha256Hex(...)` de `DocumentTrustChainService:130`. Foi descoberto porque alguém
escreveu um teste de integração contra Postgres real — não por leitura de código, e não pelo CI.

## O que ele cobre

Coluna `varchar(n)` cujo **nome afirma** guardar hash — segmento `hash`, `sha256`, `digest`,
`checksum` ou `fingerprint` — precisa ter `n >= 64`. Hoje são 193 declarações e **nenhuma** abaixo
disso, então a regra entra sem baseline e sem allowlist.

## O que ele NÃO cobre, e isto importa

**O caso que o motivou.** `evidence_chave_custodia` não tem `hash` no nome — chama-se "chave de
custódia" — então esta regra não a pegaria. Uma regra por nome só alcança quem se nomeia.

Ela também não distingue hash de outro tamanho: um SHA-512 hex precisa de 128, e uma coluna de 64
passaria por aqui. 64 é o piso, não a prova.

E não olha o lado Java: `@Column(length = 32)` numa entidade contradizendo o `varchar(64)` da
migration passa batido. O `ddl-auto: validate` também não pega isso — ele confere existência e
tipo, não largura.

**Só enxerga coluna declarada em início de linha.** O padrão é ancorado em `^\s*`, então um
`create table t ( audit_hash varchar(32) )` escrito numa linha só escapa. Descobri isso porque a
primeira sonda que escrevi passou verde: a sonda estava errada, não o guard, mas o limite é real.
As 193 declarações do projeto são multi-linha, e afrouxar a âncora traria falso positivo em
comentário e expressão — fica assim, dito em vez de suposto.

O nome do guard diz "width", e é isso que ele faz: piso de largura para coluna que se declara hash.

## Contra-exemplo deliberado

`chave_custodia` (sem o prefixo `evidence_`) guarda `"proc:" + processoId` —
`DocumentTrustChainService:75` — e está correta em `varchar(32)`. O padrão foi mantido estreito de
propósito para não acusá-la: alargar coluna por semelhança de nome seria cargo cult.
"""

from __future__ import annotations

import re
import sys
from collections import defaultdict
from pathlib import Path

from project_roots import APP_MODULE

MIGRACOES = APP_MODULE / "src" / "main" / "resources" / "db" / "migration"
LARGURA_MINIMA = 64

COLUNA_VARCHAR = re.compile(
    r"^\s*([a-z_][a-z0-9_]*)\s+(?:varchar|character\s+varying)\s*\(\s*(\d+)\s*\)",
    re.IGNORECASE | re.MULTILINE,
)
# Segmento inteiro, nao substring: evita casar "hashtag" ou nome que contenha as letras por acaso.
NOME_DE_HASH = re.compile(r"(^|_)(hash|sha256|sha_256|digest|checksum|fingerprint)(_|$)", re.IGNORECASE)


def main() -> int:
    if not MIGRACOES.is_dir():
        print("HASH COLUMN WIDTH GUARD: FAIL")
        print(f" - diretorio de migrations nao encontrado: {MIGRACOES}")
        return 1

    estreitas: dict[tuple[str, int], set[str]] = defaultdict(set)
    conferidas = 0

    for arquivo in sorted(MIGRACOES.glob("V*__*.sql")):
        conteudo = arquivo.read_text(encoding="utf-8", errors="ignore")
        for encontrado in COLUNA_VARCHAR.finditer(conteudo):
            nome = encontrado.group(1).lower()
            largura = int(encontrado.group(2))
            if not NOME_DE_HASH.search(nome):
                continue
            conferidas += 1
            if largura < LARGURA_MINIMA:
                estreitas[(nome, largura)].add(arquivo.name)

    if estreitas:
        print("HASH COLUMN WIDTH GUARD: FAIL")
        print(
            f"Coluna com nome de hash e menos de {LARGURA_MINIMA} caracteres. Um SHA-256 em hex "
            f"tem 64: a insercao passa ate o dia em que o valor chega inteiro, e ai o Postgres "
            f"recusa com 'value too long' — em producao, longe do CI."
        )
        for (nome, largura), arquivos in sorted(estreitas.items()):
            print(f" - {nome} varchar({largura}) em {', '.join(sorted(arquivos))}")
        return 1

    print("HASH COLUMN WIDTH GUARD: OK")
    print(f"{conferidas} declaracoes de coluna com nome de hash, todas >= {LARGURA_MINIMA}")
    return 0



if __name__ == "__main__":
    sys.exit(main())
