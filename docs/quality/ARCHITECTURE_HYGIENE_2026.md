# Architecture Hygiene 2026

## Objetivo
Esta rodada endurece a organização do monólito modular em dois eixos complementares:

- decomposição de query services inflados em resolvedores e assemblers explícitos
- guarda estática para detectar regressão estrutural de tamanho, espalhamento e taxonomia de pacotes

## O que entrou

### Secretaria judicial e institucional
`SecretariatQueueQueryService` deixou de concentrar sozinho:

- resolução de inbox autorizada
- normalização de status
- leitura de load profile e desk profile
- montagem do snapshot de summary

Entraram colaboradores dedicados:

- `SecretariatQueueInboxContext`
- `SecretariatQueueInboxContextResolver`
- `SecretariatQueueSummaryProjection`
- `SecretariatQueueSummaryAssembler`

O efeito arquitetural é reduzir responsabilidade transversal dentro da facade de consulta e abrir caminho para novas extrações sem quebrar a malha operacional existente.

### Guarda estática de higiene arquitetural
Entrou o script:

- `scripts/architecture_hygiene_guard.py`

Relatórios gerados:

- `docs/reports/architecture_hygiene_guard.json`
- `docs/reports/architecture_hygiene_guard.md`

A guarda mede, entre outros pontos:

- classes gigantes
- services/engines gigantes
- controllers gigantes
- raízes semânticas duplicadas como `config`, `configs` e `configuracao`
- DTO aninhado sob `controller`
- pacotes com espalhamento excessivo

## Sinais encontrados nesta base
Os sinais mais relevantes desta fotografia foram:

- coexistência de `config`, `configs` e `configuracao`
- coexistência de `api` e `controller` para superfícies HTTP
- classes acima de 1000 linhas em eixos críticos
- crescimento excessivo em `model.dto.processual.comunicacao` e `core.procedural`

## Próximos alvos recomendados

1. convergir a taxonomia `config` / `configs` / `configuracao`
2. extrair mais montagem de snapshot de `SecretariatQueueQueryService`
3. reduzir hotspots acima de 1500 linhas, começando por trânsito, assinatura qualificada e regras de tribunal
4. mover DTO aninhado sob `controller` para contratos ou modelos por bounded context
5. transformar a guarda em etapa formal de quality gate local/CI
