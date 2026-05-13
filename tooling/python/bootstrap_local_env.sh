#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
PYTHON_DIR="$ROOT_DIR/tooling/python"
VENV_DIR="$PYTHON_DIR/.venv"

python3 -m venv "$VENV_DIR"
if ! "$VENV_DIR/bin/python" -m pip install --upgrade pip; then
  echo "Aviso: nao foi possivel atualizar o pip no ambiente atual. Seguindo com o bootstrap local." >&2
fi
if [[ -f "$PYTHON_DIR/requirements.txt" ]]; then
  "$VENV_DIR/bin/pip" install -r "$PYTHON_DIR/requirements.txt"
fi

echo "Ambiente Python local pronto em $VENV_DIR"
