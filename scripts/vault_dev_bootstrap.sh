#!/usr/bin/env bash
# Popula o Vault dev com o segredo esperado pelo VaultDbCredentialsProvider.
# Uso: bash scripts/vault_dev_bootstrap.sh
#
# Pre-requisitos:
#   - docker compose --profile vault up -d vault
#   - PJB_VAULT_DEV_ROOT_TOKEN setada no .env
#
# O que faz:
#   1. Habilita o secret engine KV v2 no path pjb/ (se ainda nao existir).
#   2. Grava credenciais de dev do banco em pjb/db/pjb.
#   3. Verifica lendo de volta.
#
# Producao: NUNCA use este script. Em prod, use um Vault gerenciado e provisione via
# Terraform/config-management, com auth method proprio (AppRole/Kubernetes/etc.), NAO
# com root token.
set -euo pipefail

VAULT_ADDR="${VAULT_ADDR:-http://localhost:${PJB_VAULT_PORT:-8200}}"
VAULT_TOKEN="${PJB_VAULT_DEV_ROOT_TOKEN:-}"

if [ -z "$VAULT_TOKEN" ]; then
  echo "ERRO: PJB_VAULT_DEV_ROOT_TOKEN nao setada. Configure no .env antes." >&2
  exit 1
fi

if ! curl -fsS -o /dev/null "$VAULT_ADDR/v1/sys/health?sealedcode=200&standbyok=true"; then
  echo "ERRO: Vault nao responde em $VAULT_ADDR. Suba com: docker compose --profile vault up -d vault" >&2
  exit 1
fi

echo "== habilitando secret engine KV v2 em pjb/ =="
curl -fsS -X POST \
  -H "X-Vault-Token: $VAULT_TOKEN" \
  -d '{"type":"kv","options":{"version":"2"}}' \
  "$VAULT_ADDR/v1/sys/mounts/pjb" 2>&1 | grep -v "^$" | head -3 || echo "(ja existente)"

DB_USER="${PJB_DB_USER:-pjb}"
DB_PASS="${PJB_DB_PASS:-pjb}"

echo "== gravando credenciais em pjb/data/db/pjb =="
curl -fsS -X POST \
  -H "X-Vault-Token: $VAULT_TOKEN" \
  -d "{\"data\":{\"username\":\"$DB_USER\",\"password\":\"$DB_PASS\"}}" \
  "$VAULT_ADDR/v1/pjb/data/db/pjb" >/dev/null

echo "== verificando leitura =="
curl -fsS \
  -H "X-Vault-Token: $VAULT_TOKEN" \
  "$VAULT_ADDR/v1/pjb/data/db/pjb" | grep -oE '"username":"[^"]*"' || {
  echo "ERRO: nao consegui ler de volta o segredo gravado" >&2
  exit 1
}

echo ""
echo "== OK =="
echo "Para o backend puxar do Vault, no .env:"
echo "  PJB_DB_CREDENTIALS_ROTATION_ENABLED=true"
echo "  PJB_DB_CREDENTIALS_ROTATION_VAULT_URL=$VAULT_ADDR"
echo "  PJB_DB_CREDENTIALS_ROTATION_VAULT_PATH=pjb/data/db/pjb"
echo "  PJB_VAULT_TOKEN=<token do backend, NAO o root>"
