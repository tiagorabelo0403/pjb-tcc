#!/usr/bin/env python3
"""Impede que um guard volte a depender do diretorio de trabalho.

Os guards do PJB sao invocados pelo ci.yml com `working-directory: scripts`, mas um caminho
literal do repositorio passado a pathlib resolve contra o cwd. Um guard escrito assim varre o
conjunto vazio e reporta sucesso -- foi exatamente o que aconteceu com
internal_reference_drift_guard (9.638 arquivos varridos a partir da raiz, 0 a partir de
scripts/) e architecture_hygiene_guard (7.928 contra 0), ambos verdes no CI sem olhar nada.

A raiz canonica ja existe: project_roots.ROOT sobe de __file__ ate achar pom.xml + pjb-api.

A varredura e sintatica (ast), nao textual, para nao confundir prosa de docstring ou comentario
com chamada real -- a primeira versao deste proprio arquivo se acusava por citar o padrao no
cabecalho.
"""
from __future__ import annotations

import ast
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DIRETORIOS_DE_SCRIPT = (ROOT / "scripts", ROOT / "tooling" / "python" / "scripts")

PREFIXOS_DO_REPOSITORIO = ("pjb-api", "pjb-core", "docs", "tooling", "k8s", "infra")

CONSTRUTORES_DE_CAMINHO = {"Path", "PurePath", "PosixPath", "WindowsPath"}

ARQUIVOS_ISENTOS = {
    "project_roots.py",  # e quem define a raiz; resolve por __file__
}


def e_caminho_do_repositorio(valor: str) -> bool:
    primeiro = valor.replace("\\", "/").lstrip("./").split("/", 1)[0]
    return primeiro in PREFIXOS_DO_REPOSITORIO


class VisitanteDeCaminhos(ast.NodeVisitor):
    def __init__(self) -> None:
        self.achados: list[tuple[int, str]] = []

    def visit_Call(self, node: ast.Call) -> None:  # noqa: N802 - nome exigido pelo ast
        alvo = node.func
        nome = alvo.attr if isinstance(alvo, ast.Attribute) else getattr(alvo, "id", None)
        if nome in CONSTRUTORES_DE_CAMINHO and node.args:
            primeiro = node.args[0]
            if isinstance(primeiro, ast.Constant) and isinstance(primeiro.value, str):
                if e_caminho_do_repositorio(primeiro.value):
                    self.achados.append((primeiro.lineno, primeiro.value))
        self.generic_visit(node)


def varrer() -> tuple[int, list[tuple[str, int, str]], list[tuple[str, str]]]:
    vistos = 0
    achados: list[tuple[str, int, str]] = []
    ilegiveis: list[tuple[str, str]] = []
    for diretorio in DIRETORIOS_DE_SCRIPT:
        if not diretorio.exists():
            continue
        for arquivo in sorted(diretorio.glob("*.py")):
            vistos += 1
            if arquivo.name in ARQUIVOS_ISENTOS:
                continue
            relativo = arquivo.relative_to(ROOT).as_posix()
            texto = arquivo.read_text(encoding="utf-8", errors="ignore")
            try:
                arvore = ast.parse(texto, filename=str(arquivo))
            except SyntaxError as erro:
                ilegiveis.append((relativo, f"linha {erro.lineno}: {erro.msg}"))
                continue
            visitante = VisitanteDeCaminhos()
            visitante.visit(arvore)
            for linha, valor in visitante.achados:
                achados.append((relativo, linha, valor))
    return vistos, achados, ilegiveis


def main() -> int:
    vistos, achados, ilegiveis = varrer()

    if vistos == 0:
        print("GUARD CWD INDEPENDENCE GUARD: FAIL")
        print("Nenhum script Python encontrado; a varredura passaria por vacuidade.")
        return 1

    if not achados and not ilegiveis:
        print(f"GUARD CWD INDEPENDENCE GUARD: OK ({vistos} scripts varridos)")
        return 0

    print("GUARD CWD INDEPENDENCE GUARD: FAIL")
    if achados:
        print(
            "Caminho do repositorio resolvido contra o cwd. O ci.yml roda os guards de dentro de "
            "`scripts/`, onde esses caminhos nao existem: o guard varre o conjunto vazio e reporta "
            "sucesso. Trocar por `from project_roots import ROOT` e ancorar em `ROOT / ...`:"
        )
        for arquivo, linha, valor in achados:
            print(f" - {arquivo}:{linha}  -> {valor}")
    if ilegiveis:
        print("Scripts que nao chegam a ser analisados porque nao parseiam:")
        for arquivo, motivo in ilegiveis:
            print(f" - {arquivo}  ({motivo})")
    return 1


if __name__ == "__main__":
    sys.exit(main())
