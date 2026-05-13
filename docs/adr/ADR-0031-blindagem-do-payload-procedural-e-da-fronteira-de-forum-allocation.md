# ADR-0031 — blindagem do payload procedural e da fronteira de forum allocation

## Status
Aceito

## Contexto

Após a redução do `NationalProceduralRoutingService` e a modularização das fases internas, os dois principais pontos residuais de risco estrutural passaram a ser:

- a fronteira de entrada do eixo procedural, onde `NationalProceduralRoutingPayloadFactory` ainda misturava tradução por origem com cópia de payload e aceitava grafos arbitrários com pouca blindagem
- a fronteira externa do `NationalProceduralForumAllocationResolver`, que ainda concentrava seed territorial/material, integração com roteamento judicial externo, preflight estrutural e montagem final do relatório

Isso mantinha trust boundaries relevantes com mais acoplamento, mais superfície de erro e menos previsibilidade do que o desejado para o endurecimento do eixo procedural.

## Decisão

Foram introduzidas duas linhas explícitas de blindagem.

### Payload procedural

- `NationalProceduralProcessoRequestPayloadAssembler`
- `NationalProceduralLaianePayloadAssembler`
- `NationalProceduralProcessoEntityPayloadAssembler`
- `NationalProceduralRoutingPayloadSecurityPolicy`

`NationalProceduralRoutingPayloadFactory` passa a atuar apenas como orquestrador entre montagem por origem e snapshot endurecido do payload.

A política de segurança do payload passa a aplicar:

- cópia profunda
- saneamento de chaves/strings
- descarte de objetos arbitrários fora de chaves internas controladas
- limitação de profundidade e cardinalidade de coleções aninhadas

### Forum allocation

- `NationalProceduralForumAllocationSeedResolver`
- `NationalProceduralForumRoutingReadinessResolver`
- `NationalProceduralForumAllocationReportAssembler`
- `NationalProceduralForumAllocationSeed`
- `NationalProceduralForumRoutingReadiness`

`NationalProceduralForumAllocationResolver` passa a atuar apenas como orquestrador entre seed, readiness externa e assembly final.

A fase de readiness assume explicitamente a aplicação de fail-safe na fronteira externa do roteamento e do preflight, sem deixar a decisão operacional do conector espalhada no assembly final.

## Consequências

### Positivas

- reduz-se a superfície de risco na entrada procedural por meio de snapshot controlado e cópia defensiva
- a fronteira externa do forum allocation passa a ter seed, readiness e assembly claramente separados
- melhora-se a previsibilidade operacional do conector e do pré-protocolo
- metadata operacional passa a ser saneada profundamente antes de compor o relatório final
- aumenta a governança sobre trust boundaries do eixo procedural sem alterar o contrato externo do `ProceduralRoutingReport`

### Custos

- aumenta o número de colaboradores internos e contratos intermediários
- exige disciplina para manter a política de snapshot e o fail-safe do readiness fora dos orquestradores principais

## Relações

- ADR-0022 — modularização da síntese decisória final e do forum allocation do NationalProceduralRouting
- ADR-0028 — fábricas de contexto na fase final do NationalProceduralRouting
- ADR-0030 — subfases de judicial placement e review synthesis
