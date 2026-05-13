# Round 114 — evidência executável, isolamento modular e mitigação de N+1

Esta rodada foi guiada explicitamente pelo diagnóstico estrutural do PJB: a lacuna entre arquitetura declarada e evidência executável.

## Eixos atacados
- expansão de testes comportamentais reais com infraestrutura próxima do runtime
- isolamento modular por bounded context nas public APIs canônicas
- mitigação concreta de risco N+1 em filas e painéis operacionais
- aumento da cobertura de resiliência em integrações externas sensíveis
- início real de cobertura de testes no `pjb-core`

## O que entrou

### Integração / Testcontainers
- `ExpedicaoJudicialRepositoryIT`
- `RecursalMeshProjectionFlowIT`
- `DjePublicacaoFailureFlowIT`

### Arquitetura / bounded context
- `PjbBoundedContextPublicApiIsolationArchitectureTest`
- `PjbCoreModuleCatalogTest`

### Guard anti-N+1
- `WorkItemRepositoryEntityGraphGuardTest`
- `@EntityGraph(attributePaths = {"processo", "assignedUser"})` em queries de fila e agenda do `WorkItemRepository`

### Resiliência externa
- `DjePublicacaoService` com `@CircuitBreaker`, `@Retry` e `@Bulkhead`
- `MniRemessaService` com `@CircuitBreaker`, `@Retry` e `@Bulkhead`

## Resultado objetivo
- arquivos `*IT.java`: `27 -> 30`
- testes/guards de arquitetura ligados ao isolamento: `1 -> 12` artefatos do tipo arquitetura/isolamento relevantes no módulo de API
- `pjb-core` deixou de ter apenas `.gitkeep` em testes e passou a ter teste real de catálogo modular
- queries críticas de fila/painel agora têm trava reflexiva para impedir regressão sem `EntityGraph`
- cobertura de resiliência aumentou também para `DJe` e `MNI`, além da trilha já reforçada para SISBAJUD/RENAJUD/INFOJUD/DataJud/Gov.br

## Pendências que continuam abertas após esta rodada
- Pact provider verification ainda precisa sair do estado simbólico
- LGPD ainda carece de `DataClassificationCatalog` e implantação efetiva de RLS para sigilo
- bounded contexts ainda precisam de mais ITs por fluxo crítico institucional
- `ProceduralCatalogSupport`, `TribunalRuleEngine` e `PluginResolucaoTribunalService` seguem como hotspots relevantes
