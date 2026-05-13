# ADR-0025 — extração do judicial placement do NationalProceduralRouting

## Status
Aceito

## Contexto
Mesmo após as extrações anteriores, o `NationalProceduralRoutingService` ainda concentrava uma faixa mutável importante da análise: leitura de cidade/UF base, acionamento da distribuição dinâmica, composição de tribunal/vara/tipo de vara, merge com o `forumAllocation`, fechamento do `connector system` e montagem final do rótulo de foro.

Esse trecho mantinha o orquestrador principal excessivamente responsável por mutações intermediárias de placement judicial, o que dificultava leitura, teste isolado e contenção de regressão estrutural.

## Decisão
Foi criado o colaborador `NationalProceduralJudicialPlacementResolver`, com dois contratos próprios:

- `NationalProceduralJudicialPlacementContext`
- `NationalProceduralJudicialPlacement`

Esse colaborador passou a:

1. derivar a base territorial inicial a partir do payload
2. acionar `NationalProceduralDistributionResolver`
3. compor tribunal, vara, tipo de vara e judicial system base
4. acionar `NationalProceduralForumAllocationResolver`
5. consolidar o resultado final de cidade, UF, tribunal, vara, connector system e foro sugerido

O `NationalProceduralRoutingService` deixou de executar diretamente essa costura intermediária e passou a consumir apenas o placement já consolidado.

## Consequências
### Positivas
- o orquestrador principal fica mais curto e mais aderente ao papel de coordenação
- a mutação territorial/judicial intermediária fica isolada em um estágio próprio, com contexto e resultado explícitos
- a sequência entre distribuição dinâmica, forum allocation e labels judiciais fica testável sem depender do serviço principal
- o contrato externo do `ProceduralRoutingReport` permanece preservado

### Negativas
- há mais dois contratos internos no eixo procedural
- a sequência de placement passa a depender de um colaborador adicional, exigindo governança para evitar duplicação futura

## Guardrails
- o `NationalProceduralRoutingService` não deve voltar a chamar diretamente `distributionResolver.resolve(...)`, `forumAllocationResolver.resolve(...)` nem `forumLabelFactory.buildForoLabel/buildVaraLabel`
- qualquer nova regra de placement judicial deve entrar primeiro no `NationalProceduralJudicialPlacementResolver`
- mensagens operacionais desse eixo continuam fora do serviço principal e fora de helpers genéricos
