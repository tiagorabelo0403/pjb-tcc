# ADR-0038 — modularização dos hotspots de workspace institucional, leitura processual e comunicação nacional

## Status
Aceito

## Contexto

Após o fechamento forte do eixo procedural e da governança de audiências institucionais, permaneceram três hotspots fora desse eixo com concentração material e operacional excessiva:

- `InstitutionalProcessWorkspaceApplicationService`
- `ProcessReadingWorkspaceService`
- `NationalCommunicationFlowService`

Os três concentravam responsabilidades demais para a camada de entrada/orquestração, dificultando governança estrutural, endurecimento de fronteiras e futura extração por capacidade.

## Decisão

Foi adotado o seguinte desenho:

### 1. Workspace institucional

O serviço principal passa a atuar como orquestrador curto entre três colaboradores:

- `InstitutionalProcessWorkspaceSnapshotResolver`
- `InstitutionalProcessWorkspaceAssembler`
- `InstitutionalProcessWorkspaceDiagnosticResolver`

Com isso, snapshot processual, montagem do workspace e diagnóstico estrutural deixam de disputar o mesmo corpo de serviço.

### 2. Leitura processual

`ProcessReadingWorkspaceService` passa a atuar como casca fina de entrada, delegando a malha operacional pesada para `ProcessReadingWorkspaceFacade`.

Com isso, a malha de leitura deixa de ficar misturada com o ponto de entrada HTTP/service e pode ser evoluída como capacidade própria.

### 3. Comunicação nacional

`NationalCommunicationFlowService` passa a atuar como casca fina de entrada, delegando a massa operacional de expedição, roteamento, institucional, inbox, workflow, observabilidade e hardening para `NationalCommunicationFlowFacade`.

Com isso, o serviço principal deixa de ser um concentrador monolítico de fluxo e passa a atuar como ponto de contrato público estável.

## Consequências

### Positivas

- serviços de entrada ficam mais curtos e previsíveis
- responsabilidades pesadas ficam segregadas por capacidade operacional
- a base fica mais próxima de um monólito modular forte
- a futura extração por capacidade fica mais segura, porque as fronteiras ficam mais explícitas
- testes de governança conseguem travar melhor regressões estruturais

### Negativas

- parte da complexidade continua existindo, agora deslocada para facades/colaboradores dedicados
- ainda será necessário quebrar internamente essas facades quando o próximo hotspot real justificar
- a validação final continua dependendo do ambiente local com Maven disponível
