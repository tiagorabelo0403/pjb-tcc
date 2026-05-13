# ADR-0049 — fila persistente e refresh assíncrono da camada analítica AUTHZ

## Status
Aceito

## Contexto

A rodada anterior introduziu a materialização incremental dos buckets analíticos AUTHZ, mas a recomposição ainda era acionada diretamente no momento em que a trilha era persistida. Isso melhorava a granularidade da atualização, porém mantinha custo adicional síncrono no caminho de autorização, justamente a fronteira que mais precisa ser curta, previsível e resiliente sob alto volume.

Além disso, ainda faltavam mecanismos operacionais para:

- desacoplar a persistência da trilha do refresh analítico;
- reprocessar buckets falhos sem rebuild amplo;
- executar backfill controlado por janela;
- expor backlog e saúde da fila de recomposição;
- preparar a base para alto volume com índices mais econômicos para séries temporais.

## Decisão

Foi adotada uma fila persistente dedicada para o refresh incremental da camada analítica AUTHZ.

Entraram os componentes:

- `PjbAuthorizationTrailAnalyticsRefreshQueueEntry`
- `PjbAuthorizationTrailAnalyticsRefreshQueueRepository`
- `PjbAuthorizationTrailAnalyticsRefreshQueueService`
- `PjbAuthorizationTrailAnalyticsRefreshScheduler`
- `PjbAuthorizationTrailAnalyticsRefreshProperties`

### Regras principais

1. A persistência da trilha AUTHZ continua síncrona.
2. A recomposição analítica deixa de ocorrer diretamente no `PjbAuthorizationAuditFacade`.
3. O `AuditFacade` apenas enfileira os buckets impactados (`HOUR` e `DAY`).
4. Um scheduler próprio drena a fila em lote e recompõe apenas os buckets elegíveis.
5. Se um bucket receber novo evento enquanto ainda estiver em processamento, o registro fica marcado para requeue ao final do ciclo.
6. Falhas não derrubam a trilha AUTHZ nem o caminho de autorização; o bucket volta como `FAILED` com retry posterior.
7. Foram adicionados índices BRIN aos read models temporais de AUTHZ para reduzir custo em tabelas volumosas.

## Consequências

### Positivas

- o caminho de autorização fica mais curto e previsível sob carga;
- a recomposição analítica ganha backlog persistente, retry e backfill controlado;
- o estado da fila passa a ser observável pela superfície administrativa;
- o projeto fica mais pronto para múltiplas instâncias e maior volume;
- a estratégia segue compatível com o mini-monólito modular forte.

### Negativas

- a visão analítica pode ficar ligeiramente atrasada em relação ao último evento persistido;
- a fila adiciona mais uma estrutura operacional que precisa de observabilidade e retenção;
- a consistência analítica deixa de ser estritamente imediata e passa a ser assíncrona.
