# ADR-0037 — subfases da governança por rito das audiências institucionais

## Contexto

Depois da rodada anterior, o `InstitutionalHearingSchedulingGovernanceApplicationService` já estava reduzido a um orquestrador curto. O próximo hotspot real passou a ser o `InstitutionalHearingRiteGovernanceResolver`, que ainda concentrava:

- derivação de atores operacionais por rito
- fechamento de broad flags e escopos
- cálculo de CEJUSC e justiça militar federal
- montagem integral de dezenas de ritos com `buildRite(...)`
- costura de segregação, allowed acts, forbidden acts e fundamentos

Esse formato deixava a governança por rito muito densa, mais difícil de auditar e pouco previsível para evolução futura por capacidade.

## Decisão

A governança por rito das audiências institucionais passa a operar em subfases explícitas:

- `InstitutionalHearingRiteGovernanceContextFactory` fecha o contexto único com atores, escopos, broad flags e gatilhos de conciliação/CEJUSC
- `InstitutionalHearingRiteGovernanceFactory` concentra a montagem final de cada `InstitutionalHearingRiteGovernance`
- `InstitutionalHearingCivilAndJuizadosRiteResolver` resolve civil comum e juizados
- `InstitutionalHearingPublicProtectionAndPenalRiteResolver` resolve fazenda pública, infância e penal
- `InstitutionalHearingSpecializedJusticeRiteResolver` resolve trabalho, eleitoral, militar, conciliação/mediação e trilhas transversais
- `InstitutionalHearingRecursalRiteResolver` resolve a trilha recursal/colegiada
- `InstitutionalHearingRiteGovernanceResolver` permanece apenas como orquestrador curto dessas subfases

## Consequências

### Positivas

- a matriz de ritos deixa de ficar concentrada em um único arquivo gigante
- CEJUSC, justiça militar federal e broad flags passam a ter ponto único de derivação
- o eixo institucional fica mais pronto para modularização por capacidade sem cair em microserviço prematuro
- a montagem de governança por rito passa a ter um padrão mais parecido com o que já foi aplicado no eixo procedural

### Riscos controlados

- houve risco de perder a lógica material de CEJUSC ao mover contexto; isso foi preservado no `ContextFactory` com os gatilhos por `processProfile`
- houve risco de quebrar chamadas existentes a `oversightActors(...)`; o contrato foi mantido no resolver-orquestrador
