# Round 130 - Institutional workbench executable evidence and contract hardening

## Objetivo
Fechar um bloco de maior impacto no `InstitutionalWorkbench`, que ainda estava mais fraco em prova executável e malha contratual do que consulta pública e ajuizamento.

A rodada foi desenhada para atacar ao mesmo tempo:

- leitura quente institucional ainda sem budgets explícitos suficientes
- cobertura provider contract inexistente no workbench institucional
- evidência HTTP real ausente na surface `/api/v1/institucional/workbench`
- risco silencioso de regressão de leitura quente via carregamento simples de `Processo` no preview/quick actions

## O que entrou

### 1. Budgets transacionais explícitos no eixo do InstitutionalWorkbench
Entraram budgets explícitos em leituras centrais:

- `InstitutionalWorkbenchService.workspace()`
- `InstitutionalWorkbenchService.actionPreview(...)`
- `InstitutionalWorkbenchProjectionService.quickActions(...)`
- `InstitutionalWorkbenchProjectionService.operationalQueue(...)`
- `InstitutionalWorkbenchProjectionService.previewAction(...)`
- `InstitutionalWorkbenchProjectionService.previewExplainability(...)`

Isso reduz opacidade operacional nas leituras quentes do shell institucional e impede regressão para transações sem orçamento declarado.

### 2. Uso explícito de carregamento scoped com `EntityGraph`
Os caminhos que carregam `Processo` para quick actions e preview deixaram de depender de `findById(...)` genérico e passaram a usar `findWorkspaceScopedById(...)`, preservando o carregamento com `EntityGraph` de:

- `usuario`
- `jurisdicao`
- `equipe`

Isso não resolve toda a trilha de N+1 do sistema, mas endurece um hotspot quente institucional com trava refletiva dedicada.

### 3. Provider verification real do InstitutionalWorkbench
Entrou `InstitutionalWorkbenchControllerProviderContractTest` cobrindo:

- `GET /api/v1/institucional/workbench`
- `GET /api/v1/institucional/workbench/quick-actions?processoId=...`
- `GET /api/v1/institucional/workbench/operational-queue?limit=...`
- `GET /api/v1/institucional/workbench/action-preview?action=...&processoId=...`

Também entrou o pact versionado:

- `PjbInstitutionalWorkbenchConsumer-PjbInstitutionalWorkbenchProvider.json`

### 4. Evidência HTTP real com PostgreSQL/Testcontainers
Entrou `InstitutionalWorkbenchControllerIT`, cobrindo com MockMvc + PostgreSQL real:

- workspace institucional consolidado
- quick actions vinculadas ao processo com rota materializada
- fila operacional com `primaryAction` e `explainability`
- action preview institucional com `ALLOW` e rota processual concreta

### 5. Travas arquiteturais do bloco
Entraram:

- `PjbInstitutionalWorkbenchSurfaceArchitectureTest`
- `PjbInstitutionalWorkbenchProviderContractCoverageArchitectureTest`
- `ProcessoRepositoryInstitutionalWorkbenchEntityGraphGuardTest`

## Efeito sobre o diagnóstico sênior

### Pact provider verification insuficiente
Melhora de forma concreta. O eixo institucional passa a ter contrato provider real, deixando de concentrar a malha de contratos apenas em autenticação, peticionamento, consulta pública e ajuizamento.

### Razão teste/produção baixa
Melhora qualitativamente com mais um bounded context crítico coberto por IT HTTP real e contrato provider. A razão global ainda continua baixa.

### Risco N+1 em endpoints quentes
Melhora de forma localizada no `InstitutionalWorkbench`, com carregamento `scoped` + `EntityGraph` mantido por teste refletivo.

### 2.165 `@Transactional` sem budget explícito
Melhora de forma localizada no shell institucional, que agora passa a declarar orçamento explícito nas leituras centrais.

## Limitações honestas
- o Maven Wrapper continua bloqueado pelo download externo do Maven neste ambiente
- por isso a rodada foi validada por inspeção estrutural, guards Python, diff limpo, commit local e testes adicionados no código, mas não por execução completa do build Maven neste container

## Próximo alvo natural
- ampliar a mesma trilha de prova executável e contrato provider para a surface de magistratura em `/api/v1/magistratura/atos`
- continuar mitigação de N+1 em painéis institucionais e workbenches quentes
- ampliar Testcontainers/specification tests jurídicos em fluxos de decisão, publicação e intimação
