# Relatorio inicial da fronteira de notificacoes

## 1. Estado inicial do git

- Branch: `master`.
- Working tree antes da fronteira de notificacoes: limpo.
- Commits locais ainda nao enviados antes desta frente:
  - `ea2c16f arch(modular): criar fronteira modular para prazos processuais`
  - `f5bf6cb arch(modular): bloquear crescimento de violacoes por baseline`
  - `8e53215 arch(modular): isolar modulo acordo do legado com ports e facades oficiais`
- `HEAD..origin/master`: vazio.
- Push: nao realizado.

## 2. Recorte escolhido

Criar `modules.notificacoes` apenas para alerta de prazo processual, consumindo o resultado de `modules.prazos`. Auditoria global permanece fora.

## 3. O que ja existia

- `NotificationService`.
- `NotificationHistoryRepository`.
- `UserNotificationPreferenceRepository`.
- `CalendarNotificationEventPublisher`.
- `CalendarNotificationDispatchService`.
- `CalendarNotificationEnvelope`.

## 4. Problema arquitetural

Novos modulos poderiam chamar `NotificationService`, repositories de historico/preferencia, channels ou DTOs de calendario diretamente. Isso espalharia regra de entrega e persistencia por novos bounded contexts.

## 5. Plano

- Criar port modular de alerta de prazo.
- Criar policy de dominio para texto, prioridade, chave e data de notificacao.
- Criar application service consumindo `PrazoProcessualCalculoResult`.
- Criar adapter para `CalendarNotificationEventPublisher`.
- Criar testes de dominio, application, adapter e arquitetura.
- Nao reescrever canais, controllers, historico ou preferencias.

## 6. Riscos

- Entrega real ainda depende do legado.
- Politica de sigilo enriquecida fica para onda posterior.
- A fronteira inicial cobre somente alerta de prazo, nao notificacao universal.

## 7. O que nao sera mexido agora

- Auditoria global.
- Channels legados.
- `NotificationService`.
- Repositories de notificacao.
- Controllers de preferencia/tracking.
- Jobs de calendario.
