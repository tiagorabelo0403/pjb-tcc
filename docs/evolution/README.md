# Rounds

Histórico operacional do projeto, separado para reduzir ruído visual.

## Active

- `active/README.md` — indica o que ainda está realmente corrente

## Archive

- `archive/foundation/` — base estrutural, desenho funcional e rounds iniciais
- `archive/runtime/` — hardening de runtime, guards e RLS
- `archive/evidence/` — evidência executável, contracts, Testcontainers e marcos por bounded context
- `archive/refinement/` — decomposição de hotspots e refinamentos estruturais
- `archive/repository-governance/` — saneamento de layout, limpeza visual e reorganização documental

## Regras

- documentos `*_ROUND*.md` não devem ficar na raiz do repositório
- documentos `*_ROUND*.md` também não devem ficar em `docs/features/`
- `docs/evolution/` mantém o histórico; `README.md` da raiz resume o estado atual
