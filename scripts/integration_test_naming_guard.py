#!/usr/bin/env python3
from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
TEST_ROOT = ROOT / "pjb-api" / "src" / "test" / "java"

MARCADORES_DE_INTEGRACAO = re.compile(
    r"Testcontainers"
    r"|PostgreSQLContainer"
    r"|KafkaContainer"
    r"|GenericContainer"
    r"|@ServiceConnection"
    r"|@SpringBootTest"
    r"|@DataJpaTest"
    r"|@WebMvcTest"
    r"|@AutoConfigureMockMvc"
    r"|extends\s+\w*(ItBase|IntegrationTestBase|PjbFlowItBase|AbstractIT)\w*"
)


def main() -> int:
    if not TEST_ROOT.exists():
        print("INTEGRATION TEST NAMING GUARD: OK (sem arvore de teste)")
        return 0

    violacoes: list[str] = []
    for path in sorted(TEST_ROOT.rglob("*IT.java")):
        texto = path.read_text(encoding="utf-8", errors="ignore")
        if not MARCADORES_DE_INTEGRACAO.search(texto):
            violacoes.append(path.relative_to(ROOT).as_posix())

    if violacoes:
        print("INTEGRATION TEST NAMING GUARD: FAIL")
        print(
            "Classes com sufixo IT sem nenhum marcador de integracao. "
            "O Surefire ignora pelo nome e o CI nao executa Failsafe, "
            "entao elas nao rodam em lugar nenhum. Renomeie para *Test.java "
            "ou promova a teste de integracao real."
        )
        for violacao in violacoes:
            print(f" - {violacao}")
        return 1

    print("INTEGRATION TEST NAMING GUARD: OK")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
