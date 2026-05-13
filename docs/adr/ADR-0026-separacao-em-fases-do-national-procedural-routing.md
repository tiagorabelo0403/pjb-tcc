# ADR-0026 — separação em fases do NationalProceduralRouting

## Status
Aceito

## Contexto
Mesmo após as extrações anteriores, o `NationalProceduralRoutingService` ainda mantinha um método central que conhecia duas fases completas do eixo procedural:

1. resolução material da análise procedural
2. finalização do relatório com gate econômico, metadata e assembly final

Embora boa parte da lógica já estivesse distribuída em colaboradores dedicados, o orquestrador principal ainda precisava conhecer detalhes demais da ordem dessas decisões e dos contratos intermediários, o que mantinha acoplamento excessivo e aumentava o risco de regressão estrutural nas próximas rodadas.

## Decisão
Foi introduzida uma separação explícita em duas fases:

- `NationalProceduralRoutingCoreAnalyzer`
- `NationalProceduralRoutingFinalizationResolver`

Também foi criado o contrato interno `NationalProceduralRoutingCoreResolution` para transportar, de forma explícita, o resultado consolidado da fase material até a fase de finalização.

Com isso:

- `NationalProceduralRoutingService` passou a cuidar apenas de:
  1. obter/copiar o payload
  2. acionar a fase de análise principal
  3. acionar a fase de finalização
- a fase material passou a concentrar canonicalização, competência, teto, juizado, classificação, placement judicial e síntese de revisão
- a fase de finalização passou a concentrar gate econômico, metadata e montagem final do `ProceduralRoutingReport`

## Consequências
### Positivas
- o `NationalProceduralRoutingService` fica mais curto, previsível e aderente ao papel de orquestrador
- a separação entre análise material e finalização passa a ser explícita, testável e governável
- novos ajustes no fluxo procedural podem ser localizados em uma fase específica, reduzindo risco de recontaminação do serviço principal
- o contrato externo do relatório procedural permanece inalterado

### Negativas
- surge mais um contrato interno de trânsito entre fases
- a leitura do fluxo completo passa a depender de dois colaboradores, exigindo guardrails claros para evitar duplicação cruzada

## Guardrails
- `NationalProceduralRoutingService` não deve voltar a chamar diretamente `canonicalRitoSelector.select(...)`, `competenceResolverService.resolve(...)`, `reviewSynthesisResolver.resolve(...)`, `economicGateFactory.build(...)` ou `metadataFactory.build(...)`
- regras novas da fase material devem entrar primeiro em `NationalProceduralRoutingCoreAnalyzer` ou nos resolvers já especializados
- regras novas de fechamento de relatório devem entrar primeiro em `NationalProceduralRoutingFinalizationResolver`, `NationalProceduralRoutingMetadataFactory` ou `NationalProceduralRoutingReportAssembler`
- o contrato `NationalProceduralRoutingCoreResolution` deve permanecer interno ao eixo procedural e não deve vazar para APIs públicas
