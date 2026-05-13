# Platform Runtime Guardrails 2026

O runtime do PJB passou a expor um snapshot unificado de guardrails para três riscos que estavam muito espalhados na base:

- saturação de lane/thread pool
- pressão de pool de banco
- pressão de memória e GC

Superfícies envolvidas:

- `PjbExecutionOrchestrator`
- `PjbRuntimePressureService`
- `PjbRuntimeGuardrailsService`
- `GET /internal/runtime/execution-governance`

O snapshot agora entrega `execution`, `pressure` e `guardrails` na mesma resposta, permitindo leitura operacional em um único ponto.

Também houve endurecimento estrutural:

- erradicação de `CompletableFuture.supplyAsync/runAsync` cru na base varrida
- redução forte de `synchronized` em serviços de tribunal e edge runtime
- correção de análise probatória assíncrona que podia materializar resultado parcial silencioso
- migração de malhas críticas para lane bounded soberana

Esse desenho reduz drift silencioso entre domínio, infraestrutura assíncrona e observabilidade operacional.
