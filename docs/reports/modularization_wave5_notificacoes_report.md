# Relatorio da fronteira de notificacoes

## 1. O que foi implementado

- Novo modulo `com.tcc.pjb.backend.modules.notificacoes`.
- Contrato `NotificacaoPrazoPort`.
- Records internos `NotificacaoPrazoCommand` e `NotificacaoPrazoDispatchResult`.
- Policy de dominio `NotificacaoPrazoPolicy`.
- Application service `NotificacaoPrazoApplicationService`.
- Adapter `CalendarPrazoNotificacaoAdapter`.
- Migracao do fluxo de prazo do `CalendarNotificationScheduler` para a fronteira modular com fallback legado.
- Testes de dominio, application, adapter e arquitetura.

## 2. O que foi preservado

- `NotificationService`.
- `CalendarNotificationEventPublisher`.
- `CalendarNotificationDispatchService`.
- Repositories de historico e preferencias.
- Channels de email, push, webhook, WhatsApp, AR digital e log.
- Controllers de notificacao existentes.

## 3. Dependencias isoladas

Novos modulos podem publicar alerta de prazo sem importar diretamente:

- `NotificationService`.
- `NotificationHistoryRepository`.
- `UserNotificationPreferenceRepository`.
- `CalendarNotificationDispatchService`.
- Channels de notificacao.
- Entities de usuario, processo e historico.

## 4. Conexao com prazos

`NotificacaoPrazoApplicationService.notificarPrazoCalculado` recebe `PrazoProcessualCalculoResult` do modulo `prazos` e transforma em alerta seguro, com prioridade calculada e texto reduzido.

## 4.1 Fluxo migrado

`CalendarNotificationScheduler` passou a identificar envelopes de lane ou segmento de prazo. Esses envelopes sao publicados por `NotificacaoPrazoApplicationService`. Envelopes de agenda, audiencia e demais trilhas continuam no `CalendarNotificationEventPublisher`.

Se a fronteira modular nao aceitar o alerta ou falhar, o scheduler publica o envelope original no caminho legado. Assim a migracao fica ativa, mas nao interrompe entrega existente.

## 5. Arquivos criados

- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/notificacoes/api/NotificacaoPrazoPort.java`.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/notificacoes/api/NotificacaoPrazoCommand.java`.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/notificacoes/api/NotificacaoPrazoDispatchResult.java`.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/notificacoes/domain/NotificacaoPrazoPolicy.java`.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/notificacoes/domain/NotificacaoPrazoPrioridade.java`.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/notificacoes/domain/NotificacaoPrazoNormalizada.java`.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/notificacoes/domain/NotificacaoPrazoDomainException.java`.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/notificacoes/application/NotificacaoPrazoApplicationService.java`.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/notificacoes/infrastructure/CalendarPrazoNotificacaoAdapter.java`.
- `pjb-api/src/test/java/com/tcc/pjb/backend/modules/notificacoes/NotificacoesArchitectureTest.java`.
- `pjb-api/src/test/java/com/tcc/pjb/backend/modules/notificacoes/domain/NotificacaoPrazoPolicyTest.java`.
- `pjb-api/src/test/java/com/tcc/pjb/backend/modules/notificacoes/application/NotificacaoPrazoApplicationServiceTest.java`.
- `pjb-api/src/test/java/com/tcc/pjb/backend/modules/notificacoes/infrastructure/CalendarPrazoNotificacaoAdapterTest.java`.
- `docs/architecture/modules/notificacoes.md`.
- `docs/reports/modularization_wave5_notificacoes_initial_report.md`.
- `docs/reports/modularization_wave5_notificacoes_report.md`.

## 6. Arquivos alterados

- `docs/architecture/facades_and_ports_strategy.md`.
- `docs/architecture/modules/README.md`.
- `docs/architecture/modularization_wave_plan.md`.
- `docs/architecture/modules/notificacoes.md`.
- `pjb-api/src/main/java/com/tcc/pjb/backend/service/calendar/CalendarNotificationScheduler.java`.

## 7. Testes e guards

- `scripts/modular_monolith_guard.py`: aprovado com 0 errors, 419 warnings e 0 baseline issues.
- `scripts/architecture_hygiene_guard.py`: aprovado.
- `scripts/constructor_injection_guard.py`: aprovado.
- `.\mvnw.cmd -B -pl pjb-api test-compile --no-transfer-progress`: aprovado.
- `.\mvnw.cmd -B -pl pjb-api test "-Dtest=CalendarNotificationSchedulerTest,NotificacaoPrazoPolicyTest,NotificacaoPrazoApplicationServiceTest,CalendarPrazoNotificacaoAdapterTest,NotificacoesArchitectureTest,PrazosArchitectureTest,ModularMonolithArchitectureTest" "-DfailIfNoTests=false" --no-transfer-progress`: aprovado com 35 testes, 0 falhas, 0 erros e 0 ignorados.

Nesta sessao, `python` nao estava no PATH. Os guards foram executados com o Python local encontrado em `C:\Program Files\PostgreSQL\18\pgAdmin 4\python\python.exe`, preservando os scripts e parametro `-B`.

## 8. Riscos restantes

- O fluxo legado de calendario ainda decide entrega real.
- Ainda nao ha policy completa de sigilo para enriquecer corpo da notificacao com detalhes processuais.
- O modulo cobre alerta de prazo; notificacao universal fica para onda futura.
- Auditoria global continua fora desta rodada.

## 9. Proxima etapa recomendada

Migrar uma segunda entrada pequena de alerta de prazo ou preview operacional para a mesma fronteira, depois avaliar port de notificacao geral.
