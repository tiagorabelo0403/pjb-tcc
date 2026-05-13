# ADR-0048 — materialização incremental e governança operacional da camada analítica AUTHZ

## Status
Aceito

## Contexto

A rodada anterior introduziu uma camada analítica persistida para a trilha AUTHZ, com buckets soberanos por granularidade e dimensão. Esse desenho resolveu a consulta de alto volume, mas ainda dependia majoritariamente de materializações explícitas por janela ou de rebuild sob demanda.

Para um sistema judicial de grande escala, isso deixava três lacunas operacionais relevantes:

- eventos novos de autorização não atualizavam automaticamente a camada analítica;
- não havia uma superfície administrativa curta para inspecionar a saúde da materialização analítica;
- refresh ingênuo por delta comprometeria contagens exatas de `uniqueRequestCount` e `uniqueActorCount`.

## Decisão

Foi adotado um desenho incremental orientado a evento, mas sem aplicar incrementos cegos nos buckets analíticos.

Entrou o colaborador dedicado `PjbAuthorizationTrailAnalyticsIncrementalRefreshService`, responsável por:

- receber a trilha AUTHZ recém-persistida no read model;
- identificar os buckets impactados em `HOUR` e `DAY`;
- recompor integralmente apenas esses buckets a partir do read model persistente;
- fazer upsert por `analyticsKey`, preservando a separação entre trilha bruta, visão forense e analytics.

Além disso, a superfície administrativa passou a expor:

- `GET /api/v1/admin/security/authz-trails/analytics/status`
- `POST /api/v1/admin/security/authz-trails/analytics/refresh-bucket`

A rota de status informa o volume do read model persistente e o estado dos buckets analíticos por granularidade. A rota de refresh permite recompor explicitamente um bucket específico quando houver necessidade operacional.

## Consequências

### Positivas

- a camada analítica passa a reagir ao fluxo real de autorização sem depender apenas de rebuild amplo;
- buckets impactados por eventos novos são recompostos com exatidão, preservando contagens únicas por request e ator;
- a operação ganha visibilidade curta sobre a saúde da materialização analítica;
- a separação arquitetural entre AUTHZ principal, trilha persistida, forense e analytics fica preservada.

### Negativas

- cada evento AUTHZ persistido passa a acionar recomposição de buckets `HOUR` e `DAY`, aumentando custo local por evento em troca de consistência analítica;
- em picos extremos, ainda poderá ser desejável evoluir essa recomposição incremental para uma malha ainda mais assíncrona e particionada por janela/dimensão.
