# Round 71 — guard rail material institucional

## O que entrou

- `InstitutionalMaterialActionGuardService`
- `InstitutionalMaterialActionGuardController`
- integração do guard rail nos atos sensíveis de delegado, Ministério Público, Defensoria e Procuradoria
- teste unitário do núcleo novo

## O que a malha decide

- ramo institucional do ator
- esfera provável do caso
- aderência entre justiça, ramo, rito e sinais textuais do caso
- bloqueio, revisão ou permissão do ato material

## Onde passou a atuar

- diligência e peça de inquérito da delegacia
- manifestação, parecer, recurso e requisição de diligência do Ministério Público
- petição, recurso e gratuidade da Defensoria
- contestação, parecer, recurso e execução fiscal da Procuradoria

## Superfície nova

- `GET /api/v1/institucional/material-guard/processos/{processoId}?action=...`

## Observação

O núcleo reutiliza a malha já existente da Defensoria para evitar duplicação de regra e concentra o restante da decisão material em um único serviço.
