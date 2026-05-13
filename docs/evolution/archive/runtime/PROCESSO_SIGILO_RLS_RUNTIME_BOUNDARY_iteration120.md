# Round 120 — materialização runtime de `app.pjb_*` no boundary de conexão/transação

Esta rodada fecha o gap aberto no round 119.

Antes:
- o envelope sigiloso existia
- o filtro HTTP materializava headers/attributes
- a view SQL segura já lia `current_setting('app.pjb_*')`
- a sessão precisava ser povoada manualmente para o read model funcionar ponta a ponta

Agora:
- a request sigilosa vincula um contexto thread-local de sessão RLS
- o `DataSource` primário passa a aplicar `set_config('app.pjb_*', ..., false)` ao obter a conexão real
- a mesma conexão é devolvida ao pool com reset explícito para evitar vazamento silencioso entre requests

## O que entrou

- `PjbProcessoSigiloRlsContext`
- `PjbProcessoSigiloRlsDataSource`
- `PjbReadWriteDataSourceConfig` passou a envelopar o routing datasource com a camada RLS-aware antes do `LazyConnectionDataSourceProxy`
- `PjbProcessoSigiloRlsFilter` agora vincula/restaura o contexto RLS por request

## Variáveis materializadas

- `app.pjb_sigilo_clearance`
- `app.pjb_tribunal_code`
- `app.pjb_unit_code`
- `app.pjb_sigilo_scope`

## Garantias desta rodada

1. O escopo sigiloso deixa de depender de `SET` manual fora do runtime.
2. O `LazyConnectionDataSourceProxy` continua sendo preservado como borda principal.
3. O reset explícito na devolução da conexão evita memory leak lógico e vazamento de sessão no pool.
4. A view `vw_pjb_processo_sigilo_secure` fica pronta para operar em fluxo real, não só em fixture manual.

## Testes adicionados

- `PjbProcessoSigiloRlsContextTest`
- `PjbProcessoSigiloRlsDataSourceTest`
- `PjbProcessoSigiloRlsFilterTest` ampliado para validar bind/clear do contexto

## Próximo passo natural

- descer o mesmo envelope para boundaries não HTTP que também executam leitura sigilosa institucional
- integrar step-up/certificado qualificado com uma policy runtime que endureça rotas de escrita sensível
