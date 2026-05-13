# Platform Runtime Transaction Pressure 2026

## O que entrou

Foi adicionada uma malha específica para observar pressão transacional no monólito modular, sem misturar isso com o domínio.

Componentes novos:

- `PjbTransactionalExecutionSupport`
- `PjbTransactionalPressureTracker`
- `PjbTransactionalPressureAspect`
- `PjbTransactionPressureView`
- `PjbTransactionOperationView`

## Objetivo

Capturar sinais que costumam degradar o sistema antes de um incidente explícito:

- transações longas
- concorrência transacional acima do orçamento
- retenção silenciosa de conexão em pipelines extensos
- mistura de execução assíncrona com fronteira transacional fora de governança

## Como funciona

### 1. Execução assíncrona soberana

Serviços que antes usavam `@Async` passaram a submeter trabalho pelo `PjbExecutionOrchestrator` via `PjbTransactionalExecutionSupport`.

Isso unifica:

- lane
- timeout
- rejeição
- timeout controlado
- fronteira transacional explícita

### 2. Pressão transacional

`PjbTransactionalPressureAspect` observa a fronteira externa de métodos `@Transactional` e registra no tracker:

- nome da operação
- se a transação é read-only
- propagação
- concorrência ativa
- pico observado
- duração média
- duração máxima
- volume de transações longas
- falhas transacionais observadas

### 3. Painel interno

O endpoint interno agora expõe também:

- `transactions`

em:

- `GET /internal/runtime/execution-governance`

## Guardrails adicionados

O snapshot de guardrails agora marca:

- `TRANSACTION_ACTIVE_PRESSURE`
- `TRANSACTION_LONG_RUNNING`

## Migrações desta rodada

Removidos `@Async` remanescentes de:

- `CuradorEspecialAutomaticoService`
- `SobrestamentoTemaService`
- `JurisdicaoRepository`
- `AuditoriaInteligenteService`
- `AcordoSuggestionPipelineAsyncService`

## Resultado arquitetural

A base sai de um modelo onde concorrência e transação ainda tinham resíduos espalhados e passa a um modelo onde:

- async é soberano
- transação fica explícita na fronteira de execução
- pressão de runtime, pool e transação pode ser lida em conjunto
