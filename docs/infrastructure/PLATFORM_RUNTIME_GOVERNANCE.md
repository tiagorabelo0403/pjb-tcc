# Platform Runtime Governance

A topologia de runtime do PJB passa a tratar concorrência como bounded context interno soberano.

## Porta canônica

`com.tcc.pjb.backend.platform.runtime.execution.PjbExecutionOrchestrator`

## Objetivo

Fechar os riscos mais comuns do monólito modular em alta carga:

- saturação de lane sem leitura por operação
- `CompletableFuture` espalhado no domínio
- timeout inconsistente
- fan-out excessivo
- rejeição sem governança
- corrida entre timeout, fallback e retorno tardio

## Lanes

- `IO`
- `BURST`
- `EXTERNAL_IO`
- `LIVE`
- `JOB`

## Uso esperado

Serviços de domínio e integração devem descrever:

- nome operacional
- lane
- budget temporal
- criticidade

A partir disso, a submissão deve ocorrer pelo orquestrador.

## Superfície operacional

- `/internal/runtime/pressure`
- `/internal/runtime/execution-governance`
- `scripts/runtime_concurrency_guard.py`

## Migração gradual

A transição correta não é mover tudo de uma vez. O alvo é reduzir primeiro os hotspots de maior risco operacional:

1. integrações externas
2. malhas recursais e analíticas
3. secretarias e dossiês com fan-out paralelo
4. jobs internos restantes
