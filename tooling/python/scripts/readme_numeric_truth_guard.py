#!/usr/bin/env python3
"""Reprova quando um número afirmado no README não bate com a realidade do repositório.

Existe por causa de um caso concreto: a linha `Falhas IT: 0 (0E + 0F)` ficou no README por tempo
indeterminado enquanto a suíte de integração tinha 63 problemas, porque nada reverificava o número.
Ao procurar outros do mesmo tipo, a contagem de migrations estava errada em **oito lugares**, com
quatro valores distintos (300, 305, V331, V343), contra 316 arquivos e V354 reais.

Três decisões de desenho vêm de erros já cometidos neste repositório:

1. **Afirmação que some reprova.** Se o padrão de uma alegação não for encontrado no README, o guard
   falha em vez de passar. Guard que não acha o que procura e responde OK é exatamente o defeito que
   ele existe para pegar — renomear a frase silenciaria a verificação sem aviso nenhum.

2. **Medição impossível não vira chute.** O `target/surefire-reports/` é cumulativo entre execuções;
   somá-lo numa máquina de desenvolvimento dá número errado. Quando o alvo não permite medir, o guard
   diz isso e não afirma. Número errado num guard é pior que guard ausente: ensina a ignorar vermelho.

3. **O nome promete só o que ele cobre.** Ele não valida "o README é verdadeiro"; valida a lista
   explícita declarada em ALEGACOES. O que não estiver ali segue sem verificação, e isso está escrito
   aqui em vez de ficar implícito.
"""

from __future__ import annotations

import re
import sys
import xml.etree.ElementTree as ET
from dataclasses import dataclass
from pathlib import Path
from typing import Callable

from project_roots import ROOT, APP_MODULE

README_PT = ROOT / "README.md"
README_EN = ROOT / "README.en.md"
MIGRACOES = APP_MODULE / "src" / "main" / "resources" / "db" / "migration"
ADRS = ROOT / "docs" / "adr"

# "Elasticsearch 8.15. Flyway migrations" casava como se 8.15 fosse a contagem. Número de contagem
# tem separador de milhar (três dígitos depois do ponto/vírgula) ou nenhum separador; número de
# versão como 8.15 não é nem um nem outro. Falso positivo do meu próprio padrão, pego rodando.
CONTAGEM_PT = r"(\d{1,3}(?:\.\d{3})+|\d+)"
CONTAGEM_EN = r"(\d{1,3}(?:,\d{3})+|\d+)"

# `warmingUp`-style: o Surefire não limpa relatório de execução anterior. Numa máquina de
# desenvolvimento o diretório acumula classes já apagadas (sondas temporárias), classes `*IT` rodadas
# por `-Dtest=` e classes excluídas por tag que rodaram avulsas. Medido aqui: a soma crua deu 5396
# onde a execução real da suíte deu 5344, e nenhum filtro por nome corrigia — sobrava 5370.
JANELA_DE_EXECUCAO_COERENTE_S = 2 * 60 * 60

_motivo_sem_medicao = ""


def _numero(bruto: str) -> int:
    """`5.344` (pt) e `5,344` (en) são o mesmo número; o separador é de idioma, não de valor."""
    return int(bruto.replace(".", "").replace(",", ""))


def _arquivos_de_migracao() -> list[Path]:
    return sorted(MIGRACOES.glob("V*__*.sql"))


def _versoes_de_migracao() -> list[int]:
    versoes = []
    for arquivo in _arquivos_de_migracao():
        encontrado = re.match(r"V(\d+)__", arquivo.name)
        if encontrado:
            versoes.append(int(encontrado.group(1)))
    return sorted(versoes)


def total_de_migracoes() -> int:
    return len(_arquivos_de_migracao())


def maior_versao_de_migracao() -> int:
    return _versoes_de_migracao()[-1]


def numeros_de_versao_ausentes() -> int:
    versoes = _versoes_de_migracao()
    existentes = set(versoes)
    return sum(1 for n in range(versoes[0], versoes[-1] + 1) if n not in existentes)


def total_de_classes_it() -> int:
    return sum(1 for _ in ROOT.rglob("src/test/java/**/*IT.java"))


def total_de_adrs() -> int:
    return sum(1 for _ in ADRS.glob("*.md"))


class AlvoSujo(Exception):
    """O conjunto de relatórios não veio de uma execução única; medir ali seria inventar."""


def _classes_de_teste_no_fonte() -> set[str]:
    return {caminho.stem for caminho in ROOT.rglob("src/test/java/**/*.java")}


def _relatorios_coerentes() -> list[Path]:
    diretorios = [
        ROOT / "pjb-core" / "target" / "surefire-reports",
        APP_MODULE / "target" / "surefire-reports",
    ]
    arquivos = [a for d in diretorios if d.is_dir() for a in d.glob("TEST-*.xml")]
    if not arquivos:
        raise AlvoSujo("nenhum relatorio de Surefire no disco")

    no_fonte = _classes_de_teste_no_fonte()
    orfaos = [a for a in arquivos if a.stem[len("TEST-"):].split(".")[-1] not in no_fonte]
    if orfaos:
        exemplos = sorted(a.stem[len("TEST-"):].split(".")[-1] for a in orfaos)[:4]
        raise AlvoSujo(
            f"{len(orfaos)} relatorio(s) de classe que nao existe mais no fonte "
            f"({', '.join(exemplos)}): o target acumulou execucoes"
        )

    mais_novo = max(a.stat().st_mtime for a in arquivos)
    antigos = [a for a in arquivos if mais_novo - a.stat().st_mtime > JANELA_DE_EXECUCAO_COERENTE_S]
    if antigos:
        raise AlvoSujo(
            f"{len(antigos)} relatorio(s) com mais de 2h de diferenca do mais recente: "
            f"o target mistura execucoes diferentes"
        )
    return arquivos


def total_de_testes_unitarios() -> int | None:
    """Soma os DOIS módulos. `None` quando o `target` não permite medir.

    A primeira execução em CI achou o motivo de este guard existir: o README dizia 5.344, que é o
    total do `pjb-api` sozinho — o número que o Maven imprime no fim do módulo. Num reator
    multi-módulo não existe linha agregada; cada módulo imprime a sua, e ler a última como se fosse
    o total do projeto omite os 26 testes do `pjb-core`. O número tinha sido propagado assim por
    várias PRs sem que nada o conferisse.
    """
    global _motivo_sem_medicao
    try:
        arquivos = _relatorios_coerentes()
    except AlvoSujo as motivo:
        _motivo_sem_medicao = str(motivo)
        return None

    total = 0
    for arquivo in arquivos:
        try:
            total += int(ET.parse(arquivo).getroot().attrib.get("tests", 0))
        except ET.ParseError:
            _motivo_sem_medicao = f"relatorio ilegivel: {arquivo.name}"
            return None
    return total


@dataclass(frozen=True)
class Alegacao:
    nome: str
    arquivo: Path
    padrao: re.Pattern[str]
    valor_real: Callable[[], int | None]
    opcional_sem_medicao: bool = False


# O total de migrations aparece com redações diferentes nos dois arquivos, e as oito ocorrências
# estavam erradas com quatro valores distintos. Cada redação vira alegação própria: padrão que
# cobrisse só uma delas seria mais estreito que o nome do guard.
ALEGACOES: tuple[Alegacao, ...] = (
    Alegacao("total de migrations (pt)", README_PT,
             re.compile(CONTAGEM_PT + r"\s+migrations\b"), total_de_migracoes),
    Alegacao("total de migrations (en)", README_EN,
             re.compile(CONTAGEM_EN + r"\s+(?:applied migrations|Flyway migrations)\b"),
             total_de_migracoes),
    Alegacao("maior versao — 'numeracao ate' (pt)", README_PT,
             re.compile(r"numeração até V(\d+)\b"), maior_versao_de_migracao),
    Alegacao("maior versao — 'numbered up to' (en)", README_EN,
             re.compile(r"numbered up to V(\d+)\b"), maior_versao_de_migracao),
    Alegacao("maior versao — faixa (pt)", README_PT,
             re.compile(r"de V0 a V(\d+)\b"), maior_versao_de_migracao),
    Alegacao("maior versao — faixa (en)", README_EN,
             re.compile(r"from V0 to V(\d+)\b"), maior_versao_de_migracao),
    Alegacao("numeros de versao ausentes (pt)", README_PT,
             re.compile(r"(\d+)\s+números da sequência"), numeros_de_versao_ausentes),
    Alegacao("numeros de versao ausentes (en)", README_EN,
             re.compile(r"(\d+)\s+sequence numbers\b"), numeros_de_versao_ausentes),
    Alegacao("classes de teste de integracao (pt)", README_PT,
             re.compile(CONTAGEM_PT + r"\s+classes de (?:teste de )?integração"),
             total_de_classes_it),
    Alegacao("classes de teste de integracao (en)", README_EN,
             re.compile(CONTAGEM_EN + r"\s+integration test classes\b"), total_de_classes_it),
    Alegacao("ADRs (pt)", README_PT,
             re.compile(CONTAGEM_PT + r"\s+ADRs\b"), total_de_adrs),
    Alegacao("ADRs (en)", README_EN,
             re.compile(CONTAGEM_EN + r"\s+ADRs\b"), total_de_adrs),
    Alegacao("testes unitarios (pt)", README_PT,
             re.compile(CONTAGEM_PT + r"\s+(?:testes unitários|unitários \(Surefire\))"),
             total_de_testes_unitarios, opcional_sem_medicao=True),
    Alegacao("testes unitarios (en)", README_EN,
             re.compile(CONTAGEM_EN + r"\s+unit tests\b"),
             total_de_testes_unitarios, opcional_sem_medicao=True),
)


def main() -> int:
    problemas: list[str] = []
    nao_medidas: list[str] = []

    for alegacao in ALEGACOES:
        if not alegacao.arquivo.exists():
            problemas.append(f"{alegacao.nome}: {alegacao.arquivo.name} nao existe")
            continue

        encontrados = alegacao.padrao.findall(alegacao.arquivo.read_text(encoding="utf-8"))
        if not encontrados:
            problemas.append(
                f"{alegacao.nome}: nenhuma ocorrencia de /{alegacao.padrao.pattern}/ em "
                f"{alegacao.arquivo.name}. A alegacao sumiu ou foi reescrita, e este guard passaria "
                f"a nao verificar nada — por isso reprova em vez de ignorar."
            )
            continue

        real = alegacao.valor_real()
        if real is None:
            if alegacao.opcional_sem_medicao:
                nao_medidas.append(alegacao.nome)
                continue
            problemas.append(f"{alegacao.nome}: nao foi possivel calcular o valor real")
            continue

        divergentes = sorted({bruto for bruto in encontrados if _numero(bruto) != real})
        if divergentes:
            problemas.append(
                f"{alegacao.nome}: README afirma {', '.join(divergentes)}; real = {real} "
                f"({alegacao.arquivo.name})"
            )

    if problemas:
        print("README NUMERIC TRUTH GUARD: FAIL")
        print("Numero afirmado no README que nao corresponde ao repositorio:")
        for problema in problemas:
            print(f" - {problema}")
        return 1

    print("README NUMERIC TRUTH GUARD: OK")
    print(f"{len(ALEGACOES) - len(nao_medidas)} de {len(ALEGACOES)} alegacoes conferidas")
    if nao_medidas:
        print(
            f"NAO conferidas: {', '.join(nao_medidas)} — {_motivo_sem_medicao}. "
            "Em CI o checkout e novo e elas entram na verificacao; localmente, `mvnw clean test` "
            "produz um target coerente."
        )
    return 0


if __name__ == "__main__":
    sys.exit(main())
