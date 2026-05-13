#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

echo "[1/4] Verificando Java"
java -version

echo "[2/4] Verificando Maven local"
if command -v mvn >/dev/null 2>&1; then
  mvn -version
else
  echo "Maven local indisponivel neste ambiente"
fi

echo "[3/4] Tentando Maven Wrapper"
if ./mvnw -version; then
  echo "Wrapper acessivel"
else
  echo "Wrapper bloqueado: distribuicao Maven nao pode ser obtida neste ambiente"
fi

echo "[4/4] Sweep estatico"
python scripts/static_sweep.py
