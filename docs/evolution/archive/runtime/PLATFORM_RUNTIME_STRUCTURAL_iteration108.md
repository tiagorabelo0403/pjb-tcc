# Platform Runtime Structural Round 108

## O que entrou

- `TransitoJulgadoArquivamentoEngine` teve a camada narrativa e de matrizes extraída para `TransitoJulgadoNarrativeSupport`, reduzindo concentração de lógica descritiva dentro da engine principal.
- `ExecutionMeshStateService` recebeu orçamento transacional explícito para todos os comandos de persistência e leituras críticas do estado executivo.
- O orçamento transacional foi estendido para a superfície de aplicação dos fluxos críticos de `DataJud`, `INFOJUD`, `RENAJUD`, `SISBAJUD`, ciclo financeiro judicial e sincronização Gov.br.
- Integrações críticas ganharam política de resiliência com `@CircuitBreaker`, `@Retry` e `@Bulkhead` para Gov.br OIDC, DataJud, SISBAJUD, RENAJUD e INFOJUD.
- O catálogo modular passou a refletir as superfícies públicas anotadas com `@PjbPublicApi`, com teste dedicado para coerência entre anotação e descriptor.
- O guard transacional agora separa sinalização de hotspot de falha por ausência de budget, permitindo endurecer o CI sem quebrar o pipeline por todo o passivo legado de uma vez.
- O guard de taxonomia de configuração foi corrigido para gerar relatórios válidos e confirmar raiz canônica `configs` sem deriva para `config` e `configuracao`.

## Resultado desta rodada

- `runtime_concurrency_guard.py`: zero fronteiras assíncronas cruas detectadas fora das raízes permitidas.
- `transactional_hotspot_guard.py --fail-on-missing-budgets`: zero hotspots críticos sem `@PjbTransactionalBudget`.
- `config_taxonomy_guard.py`: zero arquivos Java em raízes legadas de configuração.

## Observação de escopo

Esta rodada endurece governança de runtime, budgets transacionais, resiliência e coerência modular. Ela não encerra a frente de redução de god classes nem substitui a necessidade de ampliar testes de comportamento com infraestrutura real por bounded context.
