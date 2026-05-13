# Peticionamento governado com assinatura patronal

## O que entrou

- endpoint frontend app para protocolar petição no workspace do escritório
- materialização da assinatura qualificada da petição no fluxo governado
- persistência de snapshot do signatário e do envelope da assinatura
- resultado frontend com signatário efetivo, modo de assinatura, hash e fila patronal

## Endpoint

`POST /api/v1/frontend/app/offices/workspace/processes/{processoId}/petitions`

## Comportamento

- em modo pessoal, a petição sai com assinatura do próprio advogado
- em modo escritório com política patronal e signatário efetivo distinto do executor, a petição entra em fila patronal
- quando aprovada pelo patrono, a execução materializa a petição com envelope de assinatura do patrono
- a operação persiste snapshot do nome do signatário, registro profissional, hash assinado e payload do envelope

## Resposta ao frontend

A resposta devolve:

- `operationId`
- `queueItemId`
- `workItemId`
- `effectiveSignerUserId`
- `effectiveSignerNome`
- `effectiveSignerRegistration`
- `signatureMode`
- `signatureEnvelopeReady`
- `signedContentHash`
- `renderedSignedContent`
- `signatureEnvelope`
- `warnings`

## Garantia funcional

O executor associado pode peticionar no contexto do escritório, mas a assinatura final respeita o signatário efetivo calculado pela política do workspace. Quando houver exigência patronal, a petição aprovada sai com o patrono como signatário materializado no envelope qualificado.
