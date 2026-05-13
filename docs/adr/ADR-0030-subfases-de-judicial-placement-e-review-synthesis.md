# ADR-0030 — subfases de judicial placement e review synthesis

## Status
Aceito

## Contexto

Mesmo após a separação do `NationalProceduralRoutingService`, do `CoreAnalyzer` e das fases finais, ainda restavam dois concentradores relevantes dentro do eixo procedural:

- `NationalProceduralJudicialPlacementResolver`, que ainda misturava leitura territorial-base, distribuição dinâmica, forum allocation e consolidação final do placement judicial
- `NationalProceduralReviewSynthesisResolver`, que ainda misturava coleta de sinais contextuais, validação de entradas obrigatórias e avaliação final de confiança

Isso mantinha lógica material e operacional demais em dois resolvers que já deveriam estar atuando como coordenadores curtos.

## Decisão

Foram introduzidas subfases explícitas nos dois eixos:

### Judicial placement

- `NationalProceduralJudicialPlacementSeedResolver`
- `NationalProceduralJudicialPlacementFinalizer`
- `NationalProceduralJudicialPlacementSeed`

`NationalProceduralJudicialPlacementResolver` passa a atuar apenas como orquestrador entre a semente inicial e a consolidação final do placement.

### Review synthesis

- `NationalProceduralReviewSignalCollector`
- `NationalProceduralReviewInputRequirementResolver`
- `NationalProceduralReviewDraft`
- `NationalProceduralReviewInputAssessment`

`NationalProceduralReviewSynthesisResolver` passa a atuar como orquestrador entre coleta de sinais, verificação de entradas obrigatórias e avaliação de confiança.

## Consequências

### Positivas

- reduz-se a concentração residual de regra material e operacional em resolvers centrais
- o placement judicial passa a ter fase de semente e fase de consolidação claramente separadas
- a síntese de revisão passa a ter fase de sinais e fase de requisitos obrigatórios claramente separadas
- melhora-se a testabilidade e a governança estrutural sem alterar o contrato externo do `ProceduralRoutingReport`

### Custos

- aumenta a quantidade de contratos internos e classes auxiliares do eixo procedural
- exige manutenção da disciplina de governança para não reintroduzir concentração nos resolvers centrais

## Relações

- ADR-0022 — modularização da síntese decisória final e do forum allocation do NationalProceduralRouting
- ADR-0025 — extração do judicial placement do NationalProceduralRouting
- ADR-0029 — subfases do core analyzer do NationalProceduralRouting
