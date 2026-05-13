# Runtime transaction budgets 2026

Esta rodada fecha três pontos estruturais:

- rastreamento transacional também para fronteiras abertas por `TransactionTemplate`
- orçamento transacional por operação
- readiness unificado com leitura de pressão transacional e violação de budget

## O que mudou

### 1. Pressão transacional deixou de ter ponto cego

`PjbTransactionalExecutionSupport` agora registra cada fronteira transacional aberta por template no `PjbTransactionalPressureTracker`. Antes disso, o painel via apenas `@Transactional` interceptado por aspect.

### 2. Budget por operação

Foi introduzido `@PjbTransactionalBudget` para métodos anotados com `@Transactional`.

Também foi adicionado suporte explícito de budget em `PjbTransactionalExecutionSupport` para operações que usam `TransactionTemplate`.

### 3. Readiness centralizado

`PjbRuntimeReadinessService` passa a ser a fonte soberana de:

- runtime pressure
- drain
- live pressure
- kafka pressure
- transaction pressure

Isso evita lógica duplicada entre probe HTTP e health indicator.

### 4. Pipeline de acordo fatiado

`AcordoSuggestionPipelineAsyncService` foi reorganizado em três fases:

- leitura curta em transação read-only
- IA e análise fora da transação
- persistência final em nova transação curta

Isso reduz retenção de conexão durante inferência e análise estratégica.

## Sinais novos

O snapshot transacional agora expõe:

- `budgetViolations`
- `criticalBudgetViolations`
- `budgetPressure`
- budget por operação

## Próxima etapa

As próximas candidatas naturais para o mesmo padrão são pipelines que ainda misturam:

- leitura massiva
- inferência/integração remota
- persistência final

em um único fluxo lógico de aplicação.
