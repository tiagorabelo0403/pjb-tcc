from __future__ import annotations

import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
README = ROOT / "README.md"
PATH_PATTERN = re.compile(r"(?:^|[\s`])((?:docs|scripts|config|infra|tooling|pjb-api|pjb-core)/(?:[A-Za-z0-9_.@=+:-]+/)*[A-Za-z0-9_.@=+:-]+)(?:[\s`.,)]|$)")

# O README documenta o estado do projeto, nao a cronica de quem mexeu nele. O que se proibe e a
# narrativa de sessao ("nesta rodada nao implementamos X"), nao a palavra: "rodadas de teste" e
# portugues tecnico legitimo e significa execucoes do build. Banir por substring reprovava as duas
# coisas do mesmo jeito -- e "round" como substring ainda pegava "background" e "around".
NARRACAO_DE_SESSAO = re.compile(
    r"\b(?:n?est[ae]|ness[ae]|dess[ae]|dest[ae])\s+rodada\b"
    r"|\brodada\s+(?:anterior|passada|atual)\b"
    r"|\b(?:this|last|next|current)\s+round\b"
    r"|\bin\s+this\s+round\b",
    re.IGNORECASE,
)

# Caminho sob target/ e saida de build. Exigir que exista num checkout limpo e erro de categoria:
# o README cita `java -jar pjb-api/target/pjb-api.jar` justamente para dizer o que o build produz.
SAIDA_DE_BUILD = re.compile(r"(?:^|/)target/")


def existing(path: str) -> bool:
    return (ROOT / path).exists()


def main() -> int:
    if not README.exists():
        print(json.dumps({"missingReadme": True}, ensure_ascii=False, indent=2))
        return 1
    text = README.read_text(encoding="utf-8")

    narrativa = sorted({match.group(0).strip() for match in NARRACAO_DE_SESSAO.finditer(text)})
    paths = sorted(set(match.group(1).rstrip(".") for match in PATH_PATTERN.finditer(text)))
    saidas_de_build = [path for path in paths if SAIDA_DE_BUILD.search(path)]
    missing_paths = [
        path for path in paths if path not in saidas_de_build and not existing(path)
    ]

    result = {
        "narrativaDeSessao": narrativa,
        "referencedPaths": paths,
        "buildOutputsIgnorados": saidas_de_build,
        "missingPaths": missing_paths,
    }
    print(json.dumps(result, ensure_ascii=False, indent=2))
    return 1 if narrativa or missing_paths else 0


if __name__ == "__main__":
    raise SystemExit(main())
