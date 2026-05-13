# Round 95 — endurecimento adicional da entrega mobile externa soberana

## O que entrou
- `RecursalNotificationMobileHardeningRequest`
- `RecursalNotificationMobilePostureResponse`
- `RecursalNotificationMobileExternalDeliveryResponse`
- `RecursalNotificationMobileExternalHardeningService`
- `RecursalNotificationMobileExternalHardeningController`
- rotas novas:
  - `RecursalRoutes.NOTIFICATION_MOBILE_POSTURE`
  - `RecursalRoutes.NOTIFICATION_MOBILE_EXTERNAL_HARDENING`

## O que foi materializado
- postura móvel soberana com score de endurecimento por domínio, dispositivo, atestação do app, binding do token, anti-replay, criptografia ponta a ponta, relay soberano e biometria local;
- entrega mobile externa endurecida com degradação controlada quando a postura não fecha e bloqueio quando a superfície móvel não atende o envelope mínimo;
- reuso da política federada de entrega já existente, sem scheduler paralelo, sem executor paralelo e sem pipeline novo.

## Organização preservada
- labels centralizadas em `RecursalNotificationLabels`;
- DTOs em `model/dto/processual/recursal/notification`;
- service em `service/processual/recursal/notification`;
- controller em `controller/processual/recursal/notification`;
- reuso explícito do `RecursalNotificationPreferenceFederationService`.

## O que continua faltando
1. continuidade da recuperação de compile global do `pjb-api` fora do eixo recursal.
