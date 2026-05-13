# Governança de upload e workspace multimídia por escritório

## Objetivo

Fechar a trilha de frontend para que upload, composição multimídia e finalização de lote nasçam dentro do escopo operacional do workspace do escritório.

## O que entrou

### Upload governado desde a origem

Serviço:
- `OfficeGovernedUploadIngressService`

Views:
- `PjbFrontendOfficeGovernedUploadBatchView`
- `PjbFrontendOfficeGovernedUploadItemReservationView`
- `PjbFrontendOfficeGovernedUploadIngressView`
- `PjbFrontendOfficeGovernedUploadFinalizeView`

DTOs:
- `FrontendOfficeGovernedUploadBatchCreateRequest`
- `FrontendOfficeGovernedUploadReserveItemRequest`
- `FrontendOfficeGovernedUploadFinalizeRequest`

Fluxo:
1. o frontend abre lote no contexto do processo e do escritório ativo
2. reserva item com hash previsto e metadados do arquivo
3. envia o binário no endpoint governado
4. consulta fingerprint do lote
5. finaliza o lote com `expectedFingerprint`
6. usa o lote na juntada governada já existente

Validações:
- processo do lote precisa coincidir com o processo da rota
- lote precisa pertencer ao usuário autenticado
- ação precisa estar dentro do escopo do workspace
- fingerprint divergente bloqueia finalização
- warnings de itens reservados ou falhos são devolvidos ao frontend

### Workspace multimídia governado

Serviço:
- `OfficeGovernedMultimediaWorkspaceService`

View:
- `PjbFrontendOfficeGovernedMultimediaWorkspaceView`

DTO:
- `FrontendOfficeGovernedMultimediaWorkspaceRequest`

Entrega ao frontend:
- modo atual do workspace
- equipe ativa
- signatário efetivo
- blockers e warnings de acesso
- `nextAction`
- `pieceProfile`
- estrutura multimídia enriquecida com governança de upload

## Endpoints do frontend app

- `POST /api/v1/frontend/app/offices/workspace/processes/{processoId}/uploads/batches`
- `GET /api/v1/frontend/app/offices/workspace/processes/{processoId}/uploads/batches/{batchId}`
- `POST /api/v1/frontend/app/offices/workspace/processes/{processoId}/uploads/batches/{batchId}/items`
- `PUT /api/v1/frontend/app/offices/workspace/processes/{processoId}/uploads/direct/{batchId}/{itemId}`
- `POST /api/v1/frontend/app/offices/workspace/processes/{processoId}/uploads/batches/{batchId}/finalize`
- `POST /api/v1/frontend/app/offices/workspace/processes/{processoId}/multimedia/workspace`

## Resultado prático

O frontend não precisa mais sair da trilha do frontend app para:
- iniciar ingestão de lote
- reservar item
- enviar binário
- consultar fingerprint
- finalizar o lote
- montar workspace multimídia contextualizado pelo escritório

## Observação operacional

A trilha de upload governado endurece a origem do lote. A trilha de juntada e protocolo governados continua sendo a responsável pela execução final do ato processual e pelo eventual encaminhamento à fila patronal.
