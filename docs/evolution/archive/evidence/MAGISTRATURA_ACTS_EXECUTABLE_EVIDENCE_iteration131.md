# Round 131 — Magistratura Acts Executable Evidence

## Objetivo
Fechar uma lacuna relevante de evidência executável e mitigação de leitura quente no eixo `/api/v1/magistratura/atos`, preservando budgets explícitos, governança central e zero regressão na espinha assíncrona/sigilosa.

## O que entrou
- `PjbTransactionalBudget` explícito em:
  - `MagistraturaJudicialActWorkbenchService.workspace(Long)`
  - `MagistraturaJudicialActWorkbenchService.preview(Long, String)`
  - `MagistraturaJudicialActWorkbenchService.preview(Long, MagistraturaJudicialActCommandRequest)`
  - `MagistraturaJudicialActWorkbenchService.execute(Long, MagistraturaJudicialActCommandRequest)`
- novo helper no `ProcessoRepository`:
  - `findMagistraturaActsScopedById(Long)` com `@EntityGraph(attributePaths = {"usuario", "jurisdicao", "equipe"})`
- `MagistraturaJudicialActWorkbenchService` passou a usar o carregamento escopado do repositório para workspace/preview/execução
- provider verification real:
  - `MagistraturaJudicialActsControllerProviderContractTest`
- pact versionado:
  - `PjbMagistraturaActsConsumer-PjbMagistraturaActsProvider.json`
- evidência HTTP real com MockMvc + PostgreSQL/Testcontainers:
  - `MagistraturaJudicialActsControllerIT`
- novas travas:
  - `PjbMagistraturaJudicialActsSurfaceArchitectureTest`
  - `PjbMagistraturaJudicialActsProviderContractCoverageArchitectureTest`
  - `ProcessoRepositoryMagistraturaJudicialActsEntityGraphGuardTest`

## O que passa a ficar comprovado
- `/api/v1/magistratura/atos` projeta workspace real com lane e atos habilitados para a trilha singular
- `/api/v1/magistratura/processos/{processoId}/atos/preview` projeta preview real com providência automática
- `/api/v1/magistratura/processos/{processoId}/atos/automation-preview` passa a ter prova HTTP real e contrato provider
- `/api/v1/magistratura/processos/{processoId}/atos` passa a ter prova HTTP real de execução do despacho e de preservação da providência projetada
- o carregamento do `Processo` nesses fluxos fica menos exposto a regressão silenciosa de N+1

## Observações honestas
- o serviço de atos da magistratura ainda concentra lógica material/operacional demais e continua candidato natural a decomposição adicional
- a execução completa de Maven continua bloqueada neste ambiente pelo download externo do Maven Wrapper
