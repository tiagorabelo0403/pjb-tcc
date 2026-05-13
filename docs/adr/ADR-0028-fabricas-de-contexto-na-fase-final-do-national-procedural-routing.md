# ADR-0028 — fábricas de contexto na fase final do NationalProceduralRouting

## Status
Aceito

## Contexto
Após a separação em fases e a modularização da pipeline de metadata, a `NationalProceduralRoutingFinalizationResolver` ainda mantinha um acoplamento estrutural relevante: ela continuava responsável por montar diretamente dois contratos internos extensos.

1. `NationalProceduralRoutingMetadataContext`
2. `NationalProceduralRoutingReportAssemblyContext`

Isso deixava a fase final com aparência de orquestradora, mas ainda concentrando costura detalhada de campos do `NationalProceduralRoutingCoreResolution`, o que dificultava a leitura, aumentava o risco de regressão e mantinha o crescimento dessa classe por adição de parâmetros.

## Decisão
A rodada introduz duas fábricas internas dedicadas para a fase final:

- `NationalProceduralRoutingMetadataContextFactory`
- `NationalProceduralRoutingReportAssemblyContextFactory`

Com isso, a `NationalProceduralRoutingFinalizationResolver` passa a ficar restrita a três passos explícitos:

1. resolver o gate econômico
2. solicitar os contextos dedicados para metadata e assembly final
3. acionar a fábrica de metadata e o assembler do relatório

Também foi endurecida a borda do `NationalProceduralEconomicGateFactory`, que agora aceita `Collection<String>` em vez de `Set<String>` para inputs de checklist, bloqueios e campos faltantes, evitando acoplamento artificial com a estrutura interna da síntese de revisão.

## Consequências
### Positivas
- a fase final fica mais curta e mais fiel ao papel de orquestração
- a montagem dos contextos internos passa a ter ponto próprio, testável e governável
- a evolução futura de metadata e assembly final não exige crescimento proporcional da fase final
- o encaixe com `NationalProceduralReviewSynthesis` fica mais resiliente ao usar `Collection<String>` no gate econômico

### Negativas
- surgem dois colaboradores internos adicionais no eixo procedural
- a leitura completa da fase final passa a depender de mais um salto entre fábricas, exigindo testes de governança mais claros

## Guardrails
- `NationalProceduralRoutingFinalizationResolver` não deve voltar a instanciar diretamente `NationalProceduralRoutingMetadataContext` nem `NationalProceduralRoutingReportAssemblyContext`
- novas traduções de `NationalProceduralRoutingCoreResolution` para metadata devem entrar em `NationalProceduralRoutingMetadataContextFactory`
- novas traduções de `NationalProceduralRoutingCoreResolution` para assembly final devem entrar em `NationalProceduralRoutingReportAssemblyContextFactory`
- o `NationalProceduralEconomicGateFactory` deve continuar aceitando coleções genéricas para esse eixo, sem reintroduzir dependência artificial de `Set`
