#!/usr/bin/env python3
"""Reprova entrada de DEBT_LOG criada ou alterada sem evidência bruta colada.

Existe por um erro concreto, cometido e mesclado em 2026-09-13: registrei que um `429` vinha de
`CapabilityRateLimitExceededException` e que isso "confirmava" uma hipótese anterior. Inferi pelo
código de status, sem ler o corpo da resposta. O corpo dizia `runtime_warming_up`, de outro filtro,
e o dump trazia `Handler: Type = null` — nenhum controller havia sido alcançado. Duas entradas
ficaram com a causa errada, e uma delas foi para o `master`.

O que teria impedido isso não é cuidado, é **ter de colar a evidência**: se eu fosse obrigado a
colar o corpo da resposta, teria lido `runtime_warming_up` antes de escrever a frase errada. Citar
o nome de uma classe entre crases não serve — a entrada errada fazia exatamente isso.

**Escopo, dito no lugar de ficar implícito:** a regra vale para entrada nova ou alterada em relação
à base de comparação, medida por `git`. As entradas que já existiam antes da regra não são
reescritas nem listadas numa allowlist — allowlist vira baseline, e baseline apodrece. Uma delas só
precisa de evidência quando alguém a tocar.
"""

from __future__ import annotations

import re
import subprocess
import sys

from project_roots import ROOT

CAMINHO_RELATIVO = "docs/quality/DEBT_LOG.md"
DEBT_LOG = ROOT / CAMINHO_RELATIVO
BLOCO_DE_CODIGO = re.compile(r"^```", re.MULTILINE)


def _git(*args: str) -> str:
    resultado = subprocess.run(
        ["git", "-C", str(ROOT), *args], capture_output=True, text=True, encoding="utf-8"
    )
    if resultado.returncode != 0:
        raise RuntimeError(resultado.stderr.strip() or " ".join(args))
    return resultado.stdout


def _entradas(texto: str) -> dict[str, str]:
    entradas: dict[str, str] = {}
    for parte in re.split(r"\n(?=## D-)", texto):
        if not parte.startswith("## D-"):
            continue
        titulo = parte.split("\n", 1)[0][3:].strip()
        entradas[titulo] = parte
    return entradas


def _base_de_comparacao() -> str:
    """O commit contra o qual medir "novo ou alterado".

    Em PR, o ponto em que a branch saiu do master. Em push para master, o commit anterior. Sem
    historico suficiente (clone raso), levanta — configuracao errada precisa aparecer, nao virar
    verificacao silenciosamente desligada.
    """
    for referencia in ("origin/master", "master"):
        try:
            base = _git("merge-base", referencia, "HEAD").strip()
        except RuntimeError:
            continue
        cabeca = _git("rev-parse", "HEAD").strip()
        if base and base != cabeca:
            return base
        try:
            return _git("rev-parse", "HEAD~1").strip()
        except RuntimeError:
            continue
    raise RuntimeError(
        "nao foi possivel achar a base de comparacao (origin/master ou HEAD~1). "
        "Num checkout do GitHub Actions isso costuma ser clone raso: use fetch-depth: 0."
    )


def main() -> int:
    if not DEBT_LOG.exists():
        print("DEBT EVIDENCE GUARD: FAIL")
        print(f" - {CAMINHO_RELATIVO} nao existe")
        return 1

    try:
        base = _base_de_comparacao()
        texto_base = _git("show", f"{base}:{CAMINHO_RELATIVO}")
    except RuntimeError as erro:
        print("DEBT EVIDENCE GUARD: FAIL")
        print(f" - {erro}")
        return 1

    atuais = _entradas(DEBT_LOG.read_text(encoding="utf-8"))
    anteriores = _entradas(texto_base)

    tocadas = {
        titulo: corpo
        for titulo, corpo in atuais.items()
        if anteriores.get(titulo) != corpo
    }
    sem_evidencia = sorted(
        titulo for titulo, corpo in tocadas.items() if not BLOCO_DE_CODIGO.search(corpo)
    )

    if sem_evidencia:
        print("DEBT EVIDENCE GUARD: FAIL")
        print(
            "Entrada criada ou alterada sem evidencia bruta. Cole o que voce observou — saida do "
            "comando, corpo da resposta, linha de log, trecho do dump — num bloco de codigo. "
            "Nome de classe entre crases nao e evidencia: a entrada que originou esta regra tinha "
            "um, e a causa estava errada."
        )
        for titulo in sem_evidencia:
            print(f" - {titulo}")
        return 1

    print("DEBT EVIDENCE GUARD: OK")
    if tocadas:
        print(f"{len(tocadas)} entrada(s) tocada(s) nesta mudanca, todas com evidencia colada")
    else:
        print(f"nenhuma entrada tocada em relacao a {base[:8]}")
    print(
        f"{len(atuais) - len(tocadas)} entrada(s) anteriores a regra nao sao verificadas ate que "
        "alguem as toque — por escolha: allowlist viraria baseline, e baseline apodrece."
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
