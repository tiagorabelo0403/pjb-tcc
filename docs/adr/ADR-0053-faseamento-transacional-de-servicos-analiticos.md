# ADR-0053 — faseamento transacional de serviços analíticos

## Contexto

Alguns serviços analíticos ainda combinavam:

- carregamento de contexto por JPA
- cálculo pesado ou inferência externa
- persistência final
- auditoria e publicação de evento

na mesma fronteira transacional. Isso ampliava o tempo de retenção de conexão, tornava o budget transacional opaco e aumentava a chance de saturação silenciosa do pool em picos analíticos.

## Decisão

Padronizar para o seguinte desenho:

1. leitura curta read-only governada por `PjbTransactionalExecutionSupport`
2. cálculo fora da transação
3. persistência curta em `REQUIRES_NEW`
4. auditoria/outbox fora da transação sempre que viável

## Aplicação inicial

- `FacilitadorBatnaService`
- `RadarPadroesService`
- `AtlasAcessoJusticaService`

## Consequências

- menor retenção de conexão por análise
- transações mais previsíveis para readiness e guardrails
- sincronizações em lote com chunk menor em vez de transação única longa
- preparação para budgets operacionais mais finos por serviço
