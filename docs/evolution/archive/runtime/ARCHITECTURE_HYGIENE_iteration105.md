# Architecture Hygiene Round 105

## Objetivo

Ampliar a decomposição estrutural iniciada nas rodadas anteriores sem abrir trilha paralela, mantendo o monólito modular e reduzindo tipos aninhados dentro de facades e services críticos.

## O que entrou

### Processual participação ativa

Extração de tipos internos de `ProcessualParticipacaoAtivaFacadeService` para arquivos próprios no mesmo pacote:

- `Persona`
- `CapabilityMatrix`
- `PreparedPrimaryDocument`
- `PreparedAttachment`
- `ActionProfile`
- `ProcessIdentityView`
- `SignaturePolicy`
- `RepresentationGuardView`
- `SecurityGuardView`
- `DeadlineGuardView`
- `RoutingView`
- `PendingView`
- `SubmissionView`
- `ExperienceDifferentialView`
- `WorkspaceView`
- `SubmissionDocumentView`
- `SubmissionAuditView`
- `SubmissionResponse`
- `AttachmentRequest`
- `SubmissionRequest`

Resultado direto:

- `ProcessualParticipacaoAtivaFacadeService`: **1574 → 1242 linhas**
- o arquivo deixou de concentrar contratos públicos, requests e value objects auxiliares no mesmo corpo de classe
- o controller HTTP passou a depender de contratos top-level reais, não de tipos aninhados

### Recursal

Extração de tipos auxiliares de `RecursalPeticionamentoFacadeService`:

- `PerfilRecursalDescriptor`
- `MeshBundle`

Resultado direto:

- `RecursalPeticionamentoFacadeService`: **1526 → 1496 linhas**
- início da limpeza de descriptors internos do eixo recursal

### Magistratura

Extração de records auxiliares de `MagistraturaJudicialProvidenceAutomationService`:

- `ReusedWorkItem`
- `ProvidencePlan`
- `DeskTarget`

Resultado direto:

- `MagistraturaJudicialProvidenceAutomationService`: **1305 → 1268 linhas**

### Secretaria judicial

Extração de `BucketSpec` de `SecretariatQueueQueryService` para arquivo próprio no mesmo pacote.

Resultado direto:

- `SecretariatQueueQueryService`: **1573 → 1572 linhas**
- ganho pequeno em linhas, mas melhora a preparação para futuras extrações da malha de painel e summary

### DTO fora de controller

Migração de:

- `controller/forum/dto/ForumHabilitacaoDecisaoRequest`

para:

- `model/dto/forum/ForumHabilitacaoDecisaoRequest`

Resultado direto:

- `controllerNestedDtoFiles`: **1 → 0**
- a superfície HTTP ficou mais aderente à taxonomia nova

## Resultado de higiene após a rodada

- `ProcessualParticipacaoAtivaFacadeService` saiu da faixa dos maiores hotspots do relatório principal
- `controllerNestedDtoFiles` foi zerado
- houve redução adicional de tipos aninhados dentro de services/facades críticos

## Observação

A rodada foi deliberadamente conservadora: prioridade em extrações de baixo risco e baixo acoplamento funcional, evitando refactors profundos em regras processuais, engines transacionais e fluxos sensíveis de comunicação.
