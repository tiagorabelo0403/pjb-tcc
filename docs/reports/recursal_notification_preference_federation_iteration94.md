# Round 94 — preferencias finas e politica federada de notificacao recursal

## Escopo
Aprofundar a suite notificacional recursal criada no round 93 com preferencias finas por perfil/canal e politica federada de entrega, sem scheduler paralelo, sem executor paralelo e sem criar mini-sistema fora do eixo real do PJB.

## O que entrou
- `core/processo/recursal/domain/foundation/RecursalNotificationLabels`
- `controller/processual/recursal/routes/RecursalRoutes`
- `core/processo/recursal/domain/foundation/RecursalWorkbenchSurfaceCatalog`
- `model/dto/processual/recursal/notification/RecursalNotificationPreferencePolicyRequest`
- `model/dto/processual/recursal/notification/RecursalNotificationPreferencePolicyResponse`
- `model/dto/processual/recursal/notification/RecursalNotificationFederatedDeliveryResponse`
- `service/processual/recursal/notification/RecursalNotificationPreferenceFederationService`
- `controller/processual/recursal/notification/RecursalNotificationPreferenceFederationController`
- `service/processual/recursal/notification/RecursalNotificationPreferenceFederationServiceTest`
- `controller/processual/recursal/notification/RecursalNotificationPreferenceFederationControllerIT`
- `service/processual/recursal/notification/RecursalNotificationPreferenceFederationRound94ArchitectureTest`

## Resultado estrutural
- preferencias finas por perfil/canal agora possuem DTO, service e boundary HTTP proprios;
- politica federada de entrega recursal deixou de ficar apenas no backlog e passou a existir como surface propria;
- o eixo de inteligencia/avisos continuou reutilizando a espinha recursal existente;
- o catalogo recursal foi atualizado para refletir que preferencias finas e politica federada ja entraram, restando o endurecimento adicional da entrega externa soberana e o compile global fora do recursal.

## Validacao honesta
- compilacao dirigida com `javac` e stubs minimos passou para os artefatos novos;
- runner local da `RecursalNotificationPreferenceFederationService` passou;
- `runtime_concurrency_guard.py` passou;
- nao ha afirmacao de build Maven global verde;
- nao ha afirmacao de compile total do `pjb-api`;
- nao ha afirmacao de Docker estavel.
