# ADR-0062 — Preparação faseada para multi-module Maven

## Contexto

A base já tem fronteiras lógicas relevantes entre `core`, `model`, `integration`, `platform`, `controller` e `service`, mas o build ainda é um módulo único. O roadmap técnico prevê uma extração gradual para multi-module, sem big-bang e sem quebrar o build principal.

## Decisão

A preparação para multi-module deve seguir três regras:

1. nenhuma rodada remove o `pom.xml` raiz monolítico enquanto os ITs de regressão das migrations recentes não estiverem estáveis;
2. toda fronteira candidata a virar módulo precisa primeiro ser protegida por ArchUnit e por testes de regressão de schema/flow;
3. a extração será feita por pacotes coesos, começando por fronteiras de baixo acoplamento operacional.

## Ordem alvo

1. `pjb-core`
2. `pjb-processo-lifecycle`
3. `pjb-integration`
4. `pjb-authz`
5. `pjb-api`

## Sinais de prontidão

- `PjbArchitectureTest` sem violações nas fronteiras principais
- ITs de schema para migrations recentes
- serviços de `integration` sem dependência de `controller`
- ausência de wiring circular entre `core`, `service` e `integration`

## Consequências

A base continua buildando como mini-monólito forte, mas passa a ganhar travas que diminuem o risco da futura extração multi-module.
