# Config Taxonomy 2026

## Diretriz canônica

- Configurações Java do runtime devem viver sob `com.tcc.pjb.backend.configs`.
- As raízes Java `config` e `configuracao` não devem receber novos arquivos.
- A pasta de repositório `config/` fora da árvore Java continua reservada para toolchain, quality gates e análise estática.

## Rodada aplicada

Nesta rodada:

- `JurisdictionEngineConfig` foi movido para `configs.runtime`
- `OrganizacaoJudiciaria` saiu de `configuracao` e passou a viver junto do domínio de roteamento em `core.forum.routing`
- o guard `scripts/config_taxonomy_guard.py` passou a reportar deriva de taxonomia de configuração

## Regra prática

- `configs.*` para beans Spring, properties, filtros, wiring e runtime posture
- `core.*` para heurísticas e componentes de domínio/roteamento que não são configuração de container
