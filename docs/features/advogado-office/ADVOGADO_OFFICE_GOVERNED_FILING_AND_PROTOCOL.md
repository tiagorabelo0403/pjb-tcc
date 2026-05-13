# ADVOGADO OFFICE GOVERNED FILING AND PROTOCOL

Esta rodada liga o workspace de escritório à juntada documental e à submissão de protocolo externo.

## Fluxo de juntada documental governada

1. O frontend cria e abastece um upload batch no fluxo já existente de upload.
2. O frontend consulta o preview governado do batch por processo.
3. O backend valida:
   - escopo do workspace ativo
   - aderência do batch ao processo
   - propriedade do batch pelo ator logado
   - status do batch
   - fingerprint do batch
4. A juntada nasce como operação governada.
5. Se a política exigir patrono, a operação vai para fila patronal.
6. Na aprovação, o backend recalcula o fingerprint do batch e só então finaliza a juntada.
7. Os documentos finalizados recebem metadados operacionais de escritório.

## Fluxo de protocolo externo governado

1. O frontend informa processo e protocolPackageId.
2. O backend revalida o escopo do workspace para PROTOCOL_SUBMIT_PJE.
3. O backend confere a propriedade do pacote e o integrity hash informado pelo frontend.
4. A submissão segue para o módulo Laiane, que já materializa:
   - guardrails
   - preflight
   - fila patronal quando necessária
   - idempotência
   - submission job
   - referência de protocolo externo

## Endpoints frontend app

- GET /api/v1/frontend/app/offices/workspace/queue
- POST /api/v1/frontend/app/offices/workspace/queue/{queueItemId}/approve
- POST /api/v1/frontend/app/offices/workspace/queue/{queueItemId}/reject
- GET /api/v1/frontend/app/offices/workspace/processes/{processoId}/document-batches/{batchId}/preview
- POST /api/v1/frontend/app/offices/workspace/processes/{processoId}/document-batches/link
- POST /api/v1/frontend/app/offices/workspace/processes/{processoId}/protocol-packages/{protocolPackageId}/submit

## Garantias desta rodada

- sem contexto misturado entre escritórios
- sem juntada de batch alterado após preview
- sem submissão de pacote de protocolo alterado após revisão frontend
- fila patronal consumível pelo frontend app
- replay íntegro da juntada governada por batch
