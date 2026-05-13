# ADR-0029 — subfases do core analyzer do NationalProceduralRouting

## Status

Aceito.

## Contexto

Mesmo após a separação entre `NationalProceduralRoutingService`, `NationalProceduralRoutingCoreAnalyzer` e `NationalProceduralRoutingFinalizationResolver`, o `CoreAnalyzer` ainda concentrava uma cadeia longa demais de dependências materiais e classificatórias. Isso mantinha acoplados, no mesmo arquivo, a fundação do diagnóstico procedural e o fechamento de classificação, placement judicial e síntese de revisão.

Esse desenho aumentava custo de evolução, risco de regressão e tendência de recontaminação do orquestrador core.

## Decisão

O eixo core foi separado em duas subfases explícitas:

- `NationalProceduralRoutingFoundationAnalyzer`
- `NationalProceduralRoutingClassificationAnalyzer`

Também foi criado o snapshot intermediário:

- `NationalProceduralRoutingFoundationResolution`

`NationalProceduralRoutingCoreAnalyzer` passa a atuar somente como orquestrador das duas subfases.

## Consequências

### Positivas

- a fundação do diagnóstico procedural fica isolada da classificação e da síntese decisória
- a fase core deixa de depender de uma cadeia longa de resolvers diretamente em um único arquivo
- placement judicial e review synthesis passam a evoluir sem recontaminar a fundação do diagnóstico
- a governança estrutural fica mais forte, com ponto explícito para travar regressões

### Negativas

- aumento do número de contratos internos e classes do eixo procedural
- necessidade de manter testes de governança coerentes com a nova separação por subfases

## Relações

Esta decisão aprofunda:

- ADR-0026 — separação em fases do NationalProceduralRouting
- ADR-0025 — extração do judicial placement do NationalProceduralRouting
- ADR-0023 — extração do residual material do NationalProceduralRouting
