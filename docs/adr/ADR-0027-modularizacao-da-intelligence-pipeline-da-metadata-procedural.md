# ADR-0027 — modularização da intelligence pipeline da metadata procedural

## Status
Aceito

## Contexto
Após a separação em fases do `NationalProceduralRouting`, a etapa de finalização ficou mais clara, mas a `NationalProceduralRoutingMetadataFactory` ainda acumulava duas responsabilidades relevantes:

1. montar a metadata-base do relatório procedural
2. executar o pipeline pesado de intelligence/advisory, quality, automation, executive explainability e acceleration

Isso mantinha a fábrica de metadata como ponto de acoplamento excessivo entre dados-base e inteligência estratégica, dificultando testes focados, guardrails de governança e evolução futura desse pipeline.

## Decisão
A rodada separa a montagem da metadata procedural em dois estágios explícitos:

- `NationalProceduralRoutingMetadataSeedFactory`, responsável pela metadata-base
- `NationalProceduralRoutingIntelligenceResolver`, responsável pelo pipeline estratégico de intelligence

Também foi criado o contrato interno `NationalProceduralRoutingIntelligenceBundle`, que concentra os cinco reports produzidos nessa trilha e a sua projeção flat para a metadata final.

Com isso, a `NationalProceduralRoutingMetadataFactory` passa a atuar apenas como orquestradora entre:

1. seed metadata
2. intelligence bundle
3. composição final da metadata procedural

## Consequências
### Positivas
- a metadata-base e a pipeline estratégica deixam de evoluir acopladas na mesma classe
- o pipeline de advisory/quality/automation/explainability/acceleration fica isolado, testável e mais governável
- a `NationalProceduralRoutingMetadataFactory` fica mais curta e mais aderente ao papel de composição
- o contrato externo da metadata do `ProceduralRoutingReport` permanece inalterado

### Negativas
- surge mais um contrato interno no eixo procedural
- a leitura completa da metadata final passa a depender de mais de um colaborador, exigindo testes de governança mais explícitos

## Guardrails
- `NationalProceduralRoutingMetadataFactory` não deve voltar a chamar diretamente `ProceduralIntelligenceAdvisor.analyzeRouting(...)`, `ProceduralDecisionQualityEngine.analyze(...)`, `ProceduralAutomationPolicyEngine.analyze(...)`, `ProceduralExecutiveExplainabilityService.analyze(...)` ou `ProceduralAccelerationEngine.analyze(...)`
- novas regras de metadata-base devem entrar primeiro em `NationalProceduralRoutingMetadataSeedFactory`
- novas regras do pipeline estratégico devem entrar primeiro em `NationalProceduralRoutingIntelligenceResolver` ou nos relatórios/engines já existentes
- `NationalProceduralRoutingIntelligenceBundle` deve permanecer contrato interno do eixo procedural
