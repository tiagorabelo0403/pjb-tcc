# Transferencia formal de carteira/processos entre escritorios

Fluxo materializado no PJB:

1. O escritorio de origem inicia a transferencia formal.
2. O sistema valida que os processos pertencem ao escritorio de origem.
3. O sistema valida que o responsavel de destino possui vinculo ativo com o escritorio de destino.
4. O sistema gera preview dos impactos com resumo da operacao.
5. A transferencia fica pendente de aceite do destino.
6. No aceite, o PJB atualiza o contexto responsavel de cada processo transferido.
7. Toda a operacao fica auditada com hash de impacto e trilha imutavel de eventos.

## Garantias desta rodada

- objeto formal de transferencia (`adv_office_process_transfer`)
- itens individualizados por processo (`adv_office_process_transfer_item`)
- idempotencia de criacao e aceite
- resumo operacional para o frontend
- contagem de itens sensiveis por sigilo/ramo
- preservacao do rastro de origem e destino por item

## Endpoints principais

- `GET /api/v1/frontend/app/offices/transfers/incoming`
- `GET /api/v1/frontend/app/offices/{equipeId}/transfers`
- `POST /api/v1/frontend/app/offices/transfers`
- `POST /api/v1/frontend/app/offices/transfers/{transferId}/accept`
- `POST /api/v1/frontend/app/offices/transfers/{transferId}/reject`


## Endurecimento adicional desta rodada

- preview vinculante antes da criacao da transferencia
- hash do preview retornado ao frontend para detectar desatualizacao do cenario
- bloqueio por ramo nao autorizado no escritorio de destino
- bloqueio por trust insuficiente para processo sigiloso ou penal-like
- bloqueio por politica de causas proprias quando o responsavel de destino conflita com a propria causa
- revalidacao completa no aceite para impedir execucao com cenario alterado desde a criacao
- warnings operacionais por assinatura patronal obrigatoria, sigilo e vinculo ainda novo do responsavel de destino

## Endpoint adicional

- `POST /api/v1/frontend/app/offices/transfers/preview`
