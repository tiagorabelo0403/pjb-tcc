# ADR-0035 — fechamento consolidado do seed territorial/forum, da síntese de revisão e da auditoria final do eixo procedural

## Contexto

Depois da modularização das fases principais do `NationalProceduralRoutingService`, os pontos densos remanescentes do eixo procedural ficaram concentrados em:

- `NationalProceduralForumAllocationSeedResolver`
- `NationalProceduralReviewSignalCollector`
- `NationalProceduralReviewInputRequirementResolver`

Além disso, ainda restavam mensagens operacionais sensíveis concentradas em um catálogo genérico (`NationalProceduralRoutingMessages`) e snapshots/contextos sem endurecimento uniforme de payload, listas e strings.

## Decisão

Foi adotado um fechamento consolidado em uma única rodada arquitetural, com quatro medidas coordenadas:

1. quebrar o seed de `forum allocation` em três subfases reais:
   - `NationalProceduralForumAllocationClassSeedResolver`
   - `NationalProceduralForumAllocationBaseSeedResolver`
   - `NationalProceduralForumAllocationProfileResolver`
2. quebrar a coleta de sinais de revisão em dois estágios:
   - `NationalProceduralReviewReasonCollector`
   - `NationalProceduralReviewPolicySignalResolver`
3. quebrar a avaliação de requisitos de entrada em quatro estágios:
   - `NationalProceduralReviewCoreFieldRequirementResolver`
   - `NationalProceduralReviewEconomicRequirementResolver`
   - `NationalProceduralReviewLocationRequirementResolver`
   - `NationalProceduralReviewPartyRequirementResolver`
4. auditar o eixo procedural final para:
   - segregar mensagens operacionais em catálogos próprios (`NationalProceduralReviewMessages` e `NationalProceduralForumAllocationMessages`)
   - endurecer contextos e snapshots com cópia defensiva, trimming, deduplicação e clamp de score

## Consequências

### Positivas

- o seed territorial/forum deixa de ser mini-monólito material
- a síntese de revisão passa a separar fundamentos de política operacional/risco
- a avaliação de inputs faltantes deixa de ficar comprimida num único método longo
- os catálogos de mensagens passam a respeitar melhor os bounded responsibilities do eixo procedural
- contextos e snapshots ficam mais previsíveis contra nulos, duplicidade e payload residual não saneado

### Custos

- aumento do número de colaboradores internos do eixo procedural
- necessidade de mais testes de governança para travar a arquitetura final

## Estado

Aceito.
