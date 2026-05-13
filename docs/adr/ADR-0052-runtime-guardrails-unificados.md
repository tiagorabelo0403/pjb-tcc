# ADR-0052 — Guardrails unificados de runtime

## Contexto

A base tinha progresso importante em bounded executors e pressure runtime, mas a leitura operacional ainda estava fragmentada entre execução, banco e memória.

## Decisão

Consolidar um snapshot unificado de guardrails via `PjbRuntimeGuardrailsService`, acoplado ao endpoint interno já existente de governança de execução.

## Consequências

- leitura única para risco de saturação, banco, memória e GC
- melhor priorização de remediação operacional
- redução de `CompletableFuture` cru em serviços críticos
- redução de `synchronized` em serviços de mutação plugin/runtime
