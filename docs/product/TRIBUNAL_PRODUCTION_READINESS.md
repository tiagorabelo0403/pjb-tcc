# Tribunal Production Readiness

Este artefato define o fechamento operacional para um tribunal entrar em produção no AKASHIC-PJB OMEGA X sem criar módulos paralelos aos eixos já existentes.

## Objetivo

Garantir que cada tribunal possua capacidade técnica, processual, documental, operacional e de interoperabilidade suficiente para substituir PJe, PJe 2.x, e-SAJ, eproc, Creta ou Projudi em escopo controlado.

## Fontes internas reaproveitadas

| Capacidade | Caminho evolutivo |
|---|---|
| Substituição nacional | `core.plataforma.substituicao` |
| Homologação de tribunal | `PjbSubstituicaoTribunalHomologacaoProbeService` |
| Conectores judiciais | `integration.judicial` |
| MNI | `integration.mni` |
| Secretaria e gabinete | `service.secretariat` |
| Processo e ritos | `core.processo`, `core.procedural` |
| Sigilo e acesso | `core.security`, `core.lgpd` |
| Operação | `core.observability`, `platform.runtime` |

## Gates de produção

| Gate | Critério |
|---|---|
| Capacidades nacionais | Matriz de substituição sem lacuna bloqueante para o escopo do tribunal. |
| Interoperabilidade | MNI, DataJud e conectores exigidos com status verificado ou degradado controlado. |
| Migração | Dry-run de acervo com divergências classificadas, reconciliadas ou aceitas formalmente. |
| Documento e assinatura | Cadeia de custódia, hash, assinatura, verificação pública e política de preservação documental. |
| Secretaria | Filas, agenda, retorno ao processo, audiência, sessão e gabinete governados. |
| Operação | SLO, indisponibilidade, replay, fila morta, backup, restore e runbook aprovados. |
| Segurança | ABAC, RLS, Gov.br, ICP-Brasil, step-up, sigilo e trilha de decisão ativos. |

## Status produzidos

| Status | Significado |
|---|---|
| `READY_FOR_PILOT` | Tribunal apto a piloto controlado, com lacunas não críticas. |
| `READY_FOR_PRODUCTION` | Tribunal apto a produção no escopo declarado. |
| `BLOCKED_BY_GOVERNANCE` | Há lacuna de regra, capacidade nacional ou evidência de homologação. |
| `BLOCKED_BY_CONNECTOR` | Há falha em MNI, DataJud, PDPJ ou conector legado. |
| `BLOCKED_BY_MIGRATION` | Há risco material em acervo, documento, sigilo ou protocolo histórico. |
| `BLOCKED_BY_OPERATIONAL_RISK` | Há risco em SLO, disponibilidade, fila, replay ou resposta a incidente. |

## Implementação

A avaliação programática fica em:

```text
pjb-api/src/main/java/com/tcc/pjb/backend/core/plataforma/substituicao/readiness
```

A camada consulta o catálogo de capacidades existente e gera snapshot sem criar novo bounded context.
