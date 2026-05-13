# ADR-0046 — camada forense e exportável da trilha AUTHZ persistida

## Status
Aceito

## Contexto

Após a introdução do read model persistente da trilha AUTHZ, a base passou a reter decisões explicáveis e auditáveis de autorização além do runtime. Ainda assim, a superfície administrativa existente permanecia centrada em listagem filtrada e resumo operacional imediato, sem uma camada própria para leitura forense agregada, exportação controlada e leitura de retenção.

Isso deixava uma lacuna entre o registro da decisão e sua exploração administrativa em cenários de auditoria, investigação, resposta a incidente e governança institucional.

## Decisão

Foi criada uma camada administrativa própria sobre o read model persistente da trilha AUTHZ, composta por:

- `PjbAuthorizationTrailForensicsService` como orquestrador curto da leitura forense persistida;
- `PjbAuthorizationTrailForensicsProjectionAssembler` para agregação temporal e por dimensões;
- `PjbAuthorizationTrailCsvExporter` para exportação CSV governada;
- `PjbAuthorizationTrailTemporalGranularity` para normalização de buckets `HOUR` e `DAY`.

A superfície administrativa passou a expor três rotas adicionais:

- `GET /api/v1/admin/security/authz-trails/forensics`;
- `GET /api/v1/admin/security/authz-trails/export`;
- `GET /api/v1/admin/security/authz-trails/retention`.

Também foram adicionadas capacidades de leitura do primeiro evento persistido, último evento persistido e contagem elegível antes de um cutoff de retenção.

## Consequências

### Positivas

- a trilha AUTHZ persistida deixa de ser apenas armazenada e passa a sustentar leitura forense agregada;
- operação, segurança e auditoria ganham uma superfície exportável e temporalmente agrupada;
- a postura de retenção fica visível sem misturar governança destrutiva com a trilha de decisão;
- o serviço administrativo principal permanece curto, sem recontaminar a malha de decisão ABAC.

### Negativas

- a análise forense ainda depende do read model persistente e não materializa agregados pré-computados por janela;
- exportações amplas podem exigir paginação/streaming dedicado em rodadas futuras caso o volume cresça fortemente;
- a limpeza efetiva da retenção ainda não foi automatizada nesta rodada, ficando apenas a postura e o volume elegível.
