"""O que o repositorio de fato versiona.

Guard de layout que varre o sistema de arquivos acusa o lixo local de quem desenvolve: `target/`,
`__pycache__`, `.idea`, `node_modules`. Isso nao e conteudo do repositorio -- o `.gitignore` ja disse
que nao e. A pergunta certa nao e "existe no disco" e sim "esta versionado".
"""
from __future__ import annotations

import subprocess
from functools import lru_cache

from project_roots import ROOT


class GitIndisponivel(RuntimeError):
    """Sem git nao da para distinguir conteudo do repositorio de lixo local.

    Falhar alto aqui e deliberado: devolver conjunto vazio faria todo guard que depende disto passar
    por vacuidade, que e exatamente o defeito que este modulo existe para corrigir.
    """


@lru_cache(maxsize=1)
def arquivos_versionados() -> frozenset[str]:
    """Caminhos relativos a raiz, com barra normal, de tudo que o git rastreia."""
    try:
        saida = subprocess.run(
            ["git", "ls-files", "-z"],
            cwd=ROOT,
            capture_output=True,
            check=True,
        )
    except (OSError, subprocess.CalledProcessError) as erro:
        raise GitIndisponivel(f"nao foi possivel listar os arquivos versionados: {erro}") from erro

    caminhos = frozenset(
        parte for parte in saida.stdout.decode("utf-8", errors="replace").split("\0") if parte
    )
    if not caminhos:
        raise GitIndisponivel("git ls-files nao devolveu nenhum arquivo; a verificacao seria vazia")
    return caminhos


@lru_cache(maxsize=1)
def entradas_de_raiz_versionadas() -> frozenset[str]:
    """Primeiro segmento de cada caminho versionado: os nomes que realmente ocupam a raiz."""
    return frozenset(caminho.split("/", 1)[0] for caminho in arquivos_versionados())


def e_versionado(caminho_relativo: str) -> bool:
    return caminho_relativo.replace("\\", "/") in arquivos_versionados()


def diretorio_tem_conteudo_versionado(caminho_relativo: str) -> bool:
    prefixo = caminho_relativo.replace("\\", "/").rstrip("/") + "/"
    return any(caminho.startswith(prefixo) for caminho in arquivos_versionados())
