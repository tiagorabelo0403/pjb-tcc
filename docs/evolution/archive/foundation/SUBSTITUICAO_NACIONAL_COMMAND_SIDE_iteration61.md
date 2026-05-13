# Substituição nacional - command side round 61

## O que entrou

- trilha persistida de execução nacional em `tb_pjb_substituicao_execucao`
- trilha persistida de evidência/eventos em `tb_pjb_substituicao_execucao_evento`
- command side com idempotência de request
- enfileiramento no motor de jobs existente do PJB
- handler dedicado `PJB_SUBSTITUICAO_NACIONAL_EXECUCAO`
- orquestrador com fases explícitas por ação
- controller com `POST`, `GET` e `PUT`

## Endpoints

- `POST /api/v1/processual/plataforma/substituicao-nacional/execucoes`
- `GET /api/v1/processual/plataforma/substituicao-nacional/execucoes`
- `GET /api/v1/processual/plataforma/substituicao-nacional/execucoes/{execucaoId}`
- `PUT /api/v1/processual/plataforma/substituicao-nacional/execucoes/{execucaoId}/controle`

## Ações disponíveis

- `HOMOLOGAR_TRIBUNAL`
- `INICIAR_MIGRACAO_SOMBRA`
- `SINCRONIZAR_COMUNICACOES_NACIONAIS`
- `CONFIRMAR_CUTOVER`
- `ACIONAR_ROLLBACK`

## Controles disponíveis

- `PAUSAR_JOB`
- `RETOMAR_JOB`
- `FORCAR_REPROCESSAMENTO`
