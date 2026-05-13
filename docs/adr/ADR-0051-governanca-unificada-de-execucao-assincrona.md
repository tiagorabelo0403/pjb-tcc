# ADR-0051 — governança unificada de execução assíncrona

## Status
Aceito

## Contexto

A base já possuía proteção de runtime, lanes de execução e leitura de pressão operacional. Mesmo assim, parte relevante do código ainda distribuía `CompletableFuture`, timeout, fallback e escolha de executor dentro dos próprios serviços de domínio e integração. Isso mantinha a política de concorrência dispersa, dificultava leitura de saturação por operação e ampliava o risco de timeout inconsistente, rejeição sem telemetria operacional consolidada e crescimento silencioso de fan-out.

## Decisão

Foi introduzido o bounded context interno `platform.runtime.execution` como porta canônica para submissão assíncrona governada.

Elementos centrais:

- `PjbExecutionOrchestrator`
- `PjbExecutionDescriptor`
- `PjbExecutionLane`
- exceções explícitas de rejeição e timeout
- visão administrativa em `/internal/runtime/execution-governance`
- sweep estático `scripts/runtime_concurrency_guard.py`

A política passa a ser:

- seleção de lane por intenção operacional
- budget temporal explícito por operação
- snapshot por lane e por operação
- redução gradual de `CompletableFuture.supplyAsync` espalhado fora de `platform.runtime`

## Consequências

Ganhos:

- leitura operacional por operação e por lane
- timeout padronizado
- melhor detecção de saturação por capacidade
- menor risco de deriva assíncrona silenciosa
- trilha mais limpa para extração futura de bounded contexts

Custos:

- fase de transição com coexistência de código legado
- necessidade de migrar gradualmente serviços que ainda executam `CompletableFuture` direto

## Próximos passos

- migrar integrações externas restantes para o orquestrador
- eliminar uso direto de `Executors.*` fora de `platform.runtime`
- endurecer gates de CI para o sweep de concorrência
