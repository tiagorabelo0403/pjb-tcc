# ADR-0033 — subfases finais de public law e juizado track

## Status
Aceito

## Contexto

Após a rodada anterior, os principais mini-monólitos remanescentes do eixo procedural deixaram de estar no `NationalProceduralRoutingService`, mas permaneceram em dois colaboradores ainda densos:

- `NationalProceduralActionProfilePublicLawResolver`, que ainda concentrava num único corpo a classificação de writs constitucionais, eleitoral, trabalhista, penal/militar e ações contra o poder público
- `NationalProceduralJuizadoTrackResolver`, que ainda concentrava classificação de trilha e fechamento de JEF, Juizado da Fazenda Pública, JEC, JECRIM e fallback final

Isso mantinha dois pontos importantes de decisão material e de aderência a juizados com baixa granularidade interna, reduzindo previsibilidade para endurecimento e evolução segura.

## Decisão

Foram introduzidas subfases finais nesses dois eixos.

### Public law action profile

- `NationalProceduralActionProfileEconomicRitoResolver`
- `NationalProceduralActionProfileSpecialProcedureResolver`
- `NationalProceduralActionProfileLaborCriminalResolver`
- `NationalProceduralActionProfilePublicEntityResolver`

`NationalProceduralActionProfilePublicLawResolver` passa a atuar apenas como orquestrador entre essas subfases.

A decomposição ficou assim:

- `SpecialProcedureResolver`: mandado de segurança, habeas corpus, execução fiscal e trilhas eleitorais especiais/gerais
- `LaborCriminalResolver`: trabalhista, militar, júri, execução penal, menor potencial ofensivo e ação penal comum
- `PublicEntityResolver`: improbidade, ação civil pública, desapropriação, previdenciário e fazenda pública
- `EconomicRitoResolver`: inferência econômica de ritos trabalhista e previdenciário

### Juizado track

- `NationalProceduralJuizadoTrackLane`
- `NationalProceduralJuizadoTrackClassifier`
- `NationalProceduralJuizadoFederalTrackResolver`
- `NationalProceduralJuizadoFazendaTrackResolver`
- `NationalProceduralJuizadoCivelTrackResolver`
- `NationalProceduralJuizadoCriminalTrackResolver`

`NationalProceduralJuizadoTrackResolver` passa a atuar apenas como orquestrador entre classificação de trilha e fechamento especializado por lane.

A decomposição ficou assim:

- `TrackClassifier`: escolhe a trilha `FEDERAL`, `FAZENDA`, `CIVEL`, `CRIMINAL` ou `NONE`
- resolvers especializados: fecham regras econômicas, alertas, checklist e confiança por cada juizado aplicável

## Consequências

### Positivas

- elimina-se a concentração residual mais densa que ainda restava fora do orquestrador principal
- melhora-se a previsibilidade de evolução e endurecimento do eixo procedural
- separa-se melhor classificação material, fechamento econômico e exclusões/fallbacks
- aumenta-se a governança para impedir que essas decisões voltem a crescer em um único arquivo

### Custos

- cresce o número de colaboradores do pacote procedural
- exige disciplina de governança para que futuras regras novas entrem no resolver especializado correto

## Relações

- ADR-0031 — blindagem do payload procedural e da fronteira de forum allocation
- ADR-0032 — subfases de action profile e juizado decision
