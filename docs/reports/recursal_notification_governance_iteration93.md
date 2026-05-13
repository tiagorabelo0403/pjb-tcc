# Round 93 — Governança mobile/notificacional recursal

## O que entrou
- suíte notificacional recursal governada
- preview mobile de pendências recursais
- governança notificacional recursal
- ciência notificacional recursal
- labels centralizadas para política de canais, criticidade e vedação de scheduler/executor paralelos

## Artefatos principais
- `RecursalNotificationLabels`
- `RecursalNotificationGovernanceRequest`
- `RecursalNotificationMobilePreviewResponse`
- `RecursalNotificationGovernanceResponse`
- `RecursalNotificationScienceResponse`
- `RecursalNotificationGovernanceService`
- `RecursalNotificationGovernanceController`

## Rotas materializadas
- `/api/v1/processual/recursal/analytics/mobile-acompanhamento`
- `/api/v1/processual/recursal/analytics/notifica-pendencias`
- `/api/v1/processual/recursal/notification/science`

## Direção técnica preservada
- sem scheduler paralelo
- sem executor paralelo
- reuso de preferências globais
- reuso de preview/calendário
- reuso de ciência/rastreamento multicanal
- sigilo por perfil e processo

## O que ainda falta
1. preferências finas por perfil/canal com política federada mais profunda
2. endurecimento adicional de entrega mobile externa em ambiente soberano
3. continuidade da recuperação de compile global do `pjb-api`
