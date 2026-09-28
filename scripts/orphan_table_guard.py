#!/usr/bin/env python3
"""Reprova migration que cria tabela sem software — o achado central do Plano de Melhoria v4.

Uma tabela logica so existe para ser lida ou escrita por codigo. Quando uma migration cria
tabela que nenhum arquivo Java menciona, o esquema cresce sem implementacao: entra no
flyway_schema_history, aparece em toda ferramenta de modelagem e nao serve a nada. Foi assim
que 50 tabelas orfas entraram de uma vez pelos commits round-28AH / round-28ai.

O guard mede o estado liquido do esquema (CREATE menos DROP) e reprova a tabela existente que
nenhum Java referencia. Particoes declarativas (PARTITION OF) ficam de fora: quem as referencia
e a tabela-mae, nao cada particao mensal pelo nome.
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MIGRATION_DIR = ROOT / "pjb-api" / "src" / "main" / "resources" / "db" / "migration"
JAVA_ROOTS = [ROOT / "pjb-core" / "src", ROOT / "pjb-api" / "src"]

CREATE = re.compile(r'create\s+table\s+(?:if\s+not\s+exists\s+)?"?([a-zA-Z0-9_]+)"?(.*)$', re.IGNORECASE)
DROP = re.compile(r'drop\s+table\s+(?:if\s+exists\s+)?"?([a-zA-Z0-9_]+)"?', re.IGNORECASE)
PARTITION_OF = re.compile(r'partition\s+of', re.IGNORECASE)
TOKEN = re.compile(r'[A-Za-z_][A-Za-z0-9_]*')
TABLE_ANNOTATION = re.compile(r'@Table\s*\(\s*name\s*=\s*"([a-zA-Z0-9_]+)"')


def _schema_state() -> tuple[set[str], set[str]]:
    created: set[str] = set()
    dropped: set[str] = set()
    partitions: set[str] = set()
    for sql in sorted(MIGRATION_DIR.glob("V*.sql")):
        for line in sql.read_text(encoding="utf-8", errors="ignore").splitlines():
            create = CREATE.search(line)
            if create:
                name = create.group(1).lower()
                created.add(name)
                if PARTITION_OF.search(create.group(2)):
                    partitions.add(name)
            drop = DROP.search(line)
            if drop:
                dropped.add(drop.group(1).lower())
    return created - dropped - partitions, partitions


def _java_references() -> set[str]:
    tokens: set[str] = set()
    for root in JAVA_ROOTS:
        for java in root.rglob("*.java"):
            text = java.read_text(encoding="utf-8", errors="ignore")
            tokens.update(TOKEN.findall(text))
            tokens.update(name.lower() for name in TABLE_ANNOTATION.findall(text))
    return tokens


def main() -> int:
    if not MIGRATION_DIR.is_dir():
        print("ORPHAN TABLE GUARD: FAIL")
        print(f"Diretorio de migrations nao encontrado: {MIGRATION_DIR}")
        return 1
    candidates, partitions = _schema_state()
    referenced = _java_references()
    orphans = sorted(table for table in candidates if table not in referenced)
    if orphans:
        print("ORPHAN TABLE GUARD: FAIL")
        print("Tabela existente que nenhum Java referencia (esquema sem software):")
        for table in orphans:
            print(f"  - {table}")
        print("Conecte a tabela a codigo ou remova-a por migration nova (DROP ... CASCADE).")
        return 1
    print("ORPHAN TABLE GUARD: OK")
    print(f"{len(candidates)} tabelas nao-particao conferidas, {len(partitions)} particoes ignoradas, 0 orfas")
    return 0


if __name__ == "__main__":
    sys.exit(main())
