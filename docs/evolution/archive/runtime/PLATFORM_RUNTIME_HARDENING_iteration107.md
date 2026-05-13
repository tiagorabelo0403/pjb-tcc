# Platform runtime hardening round 107

Esta rodada endurece a base em cinco pontos operacionais diretamente ligados a saturação, timeout, race e governança assíncrona.

## 1. Fronteiras assíncronas do domínio passaram para a espinha oficial

Foram removidos usos diretos de `CompletableFuture.supplyAsync(...)` e `handleAsync(...)` nos fluxos:

- `LegalSkillsControllerV58`
- `JuridicaMeshControllerV58`
- `FinanceiraMeshControllerV58`
- `IAOrchestrator`
- `UnifiedProcessoIntentRouter`
- `MotorInterceptacaoAtiva`

Todos agora passam por `PjbExecutionOrchestrator`, com lane explícita e timeout explícito por operação.

## 2. Timeout não deixa mais trabalho zumbi consumir lane

`PjbExecutionOrchestrator` agora aborta a execução do fornecedor quando o `CompletableFuture` já foi concluído por timeout ou cancelamento antes do início efetivo do trabalho. Isso evita computação tardia inútil após expiração do budget.

## 3. Scheduler isolado da blocklist foi removido

`InMemoryBlocklistStore` deixou de criar `newSingleThreadScheduledExecutor()` local e passou a usar o scheduler governado `pjbTimeoutScheduler`, cancelando apenas sua tarefa periódica no `close()`.

## 4. Guarda estática de concorrência ficou mais precisa

`scripts/runtime_concurrency_guard.py` agora detecta:

- `.supplyAsync(...)`
- `.runAsync(...)`
- `handleAsync/then*Async/whenCompleteAsync/exceptionallyAsync`
- `Executors.newSingleThreadScheduledExecutor(...)`

Resultado desta rodada: nenhuma fronteira assíncrona crua detectada no código de produção fora das raízes permitidas.

## 5. Teste explícito para timeout antes do agendamento

`PjbExecutionOrchestratorTest` ganhou cenário cobrindo expiração antes da execução efetiva, garantindo que a operação expirada não continue consumindo CPU após sair do budget.
