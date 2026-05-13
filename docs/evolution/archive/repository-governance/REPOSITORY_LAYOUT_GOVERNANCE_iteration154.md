# Round 154 — Repository Layout Governance

## Objetivo

Fechar a diferença entre limpeza visual pontual e organização de repositório governada.

## O que entrou

- criação de `scripts/repository_layout_guard.py`
- separação explícita entre documentação funcional (`docs/features/`) e histórica (`docs/evolution/`)
- movimentação de documentos `*_ROUND*.md` que ainda poluíam `docs/features/` e `docs/evolution/` raiz
- índices novos em `docs/database/`, `docs/openapi/`, `docs/postman/`, `docs/security/` e `infra/`

## Regras novas

- arquivo histórico de round não pode ficar na raiz do repositório
- arquivo histórico de round não pode ficar em `docs/features/`
- `docs/evolution/` raiz deve conter apenas `README.md`
- raiz do repositório passa a ser validada por whitelist estrutural

## Resultado

- menos poluição visual
- separação clara entre especificação funcional e histórico operacional
- redução do risco de regressão organizacional em próximas rodadas
