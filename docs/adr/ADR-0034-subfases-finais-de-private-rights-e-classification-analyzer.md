# ADR-0034 — subfases finais de private rights e classification analyzer

## Status
Aceito

## Contexto

Após as rodadas anteriores, o `NationalProceduralRoutingService` já havia sido reduzido a um orquestrador curto. Mesmo assim, ainda existiam dois concentradores remanescentes no entorno do eixo procedural:

- `NationalProceduralActionProfilePrivateRightsResolver`, que ainda reunia em um único corpo família, sucessões, imobiliário, monitória, consignação, empresarial, consumo e fallback civil geral
- `NationalProceduralRoutingClassificationAnalyzer`, que ainda reunia banda de complexidade, tipo de justiça, regime/trilha, placement judicial e review synthesis

Esses dois pontos mantinham acoplamento desnecessário entre classificação material, classificação procedimental e fechamento territorial/revisional.

## Decisão

Foram introduzidas subfases finais nesses dois eixos.

### Private rights action profile

- `NationalProceduralActionProfileFamilyResolver`
- `NationalProceduralActionProfilePropertyResolver`
- `NationalProceduralActionProfileBusinessResolver`
- `NationalProceduralActionProfileConsumerResolver`

`NationalProceduralActionProfilePrivateRightsResolver` passa a atuar apenas como orquestrador entre essas lanes.

A decomposição ficou assim:

- `FamilyResolver`: família, alimentos, divórcio/união estável e sucessões
- `PropertyResolver`: usucapião e possessórias/imobiliário
- `BusinessResolver`: monitória, consignação e insolvência empresarial
- `ConsumerResolver`: consumo, obrigação de fazer do consumo e fallback civil geral

### Classification analyzer

- `NationalProceduralRoutingClassificationSnapshot`
- `NationalProceduralRoutingPlacementReviewResolution`
- `NationalProceduralRoutingTrackClassificationResolver`
- `NationalProceduralRoutingPlacementReviewResolver`

`NationalProceduralRoutingClassificationAnalyzer` passa a atuar apenas como orquestrador entre classificação track/regime/tipo de justiça e fechamento placement/review.

A decomposição ficou assim:

- `TrackClassificationResolver`: banda de complexidade, rito sugerido, tipo de justiça, regime e trilha procedural
- `PlacementReviewResolver`: placement judicial e síntese de revisão a partir do snapshot já classificado

## Consequências

### Positivas

- elimina-se a concentração residual mais nítida que ainda restava em private rights e classificação procedural
- melhora-se a previsibilidade para endurecimento seguro de cada trilha material e classificatória
- reduz-se o risco de regressão por mistura entre classificação track/regime e fechamento territorial/revisional
- aumenta-se a governança para impedir que esse peso volte a um único arquivo

### Custos

- cresce o número de colaboradores internos do pacote procedural
- exige disciplina para novas regras entrarem no lane/resolver correto

## Relações

- ADR-0032 — subfases de action profile e juizado decision
- ADR-0033 — subfases finais de public law e juizado track
