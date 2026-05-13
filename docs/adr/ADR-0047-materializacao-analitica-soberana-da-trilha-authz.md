# ADR-0047 — materialização analítica soberana da trilha AUTHZ

## Status
Aceito

## Contexto

A trilha AUTHZ do PJB já possuía:
- persistência de eventos individuais para consulta administrativa
- camada forense exportável sobre o read model persistente
- filtros por integração, risco, step-up, governança e capacidade institucional

Essa malha já permitia auditoria e inspeção operacional, mas ainda dependia de leitura relativamente densa do read model de eventos quando a necessidade fosse construir visão analítica recorrente por janela temporal e dimensão soberana. Para ambientes maiores, essa dependência aumenta custo de consulta, repete agregação em runtime e pressiona desnecessariamente a superfície administrativa.

## Decisão

Foi criada uma nova camada de materialização analítica própria da trilha AUTHZ, separada da trilha bruta e da camada forense.

Essa capacidade foi organizada em torno de:

- `PjbAuthorizationTrailAnalyticsEntry`
- `PjbAuthorizationTrailAnalyticsRepository`
- `PjbAuthorizationTrailAnalyticsMaterializationAssembler`
- `PjbAuthorizationTrailAnalyticsMaterializationService`
- `PjbAuthorizationTrailAnalyticsProjectionAssembler`
- `PjbAuthorizationTrailAnalyticsService`

A nova materialização persiste buckets por granularidade (`HOUR` e `DAY`) e por dimensões soberanas relevantes para governança e operação:

- `OVERVIEW`
- `ACTION`
- `RESOURCE_TYPE`
- `INTEGRATION`
- `INSTITUTIONAL_UNIT`
- `GOVERNANCE_SCOPE`
- `CAPABILITY`

A leitura administrativa da visão analítica passa a consultar esse read model agregado, e não a refazer agregação completa sobre o conjunto bruto a cada requisição.

## Consequências

### Positivas

- painéis analíticos passam a depender de agregados persistidos próprios
- consultas recorrentes por janela e dimensão ficam mais baratas
- a trilha forense continua preservada para inspeção detalhada, sem virar dependência obrigatória de toda visualização agregada
- a separação entre evento bruto, visão forense e visão analítica fica mais explícita
- a base se aproxima de uma malha de observabilidade soberana com governança por capacidade

### Negativas

- existe custo adicional de materialização e armazenamento dos agregados
- a precisão da camada analítica depende da disciplina de rebuild ou materialização incremental
- a base passa a ter mais uma superfície administrativa a ser governada
