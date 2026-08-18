# Modulo Notificacoes

## 1. Responsabilidade

O modulo `notificacoes` cria uma fronteira modular para publicacao de alertas de prazo processual. Ele nao substitui o sistema legado de canais, preferencias, historico ou outbox nesta onda; ele encapsula a entrada por port e adapter para impedir que novos modulos chamem services e repositories legados diretamente.

## 2. Fronteira

- `domain`: valida destinatario, processo, vencimento, prioridade, origem, textos e chave de notificacao.
- `application`: publica alerta de prazo e consome resultado de `modules.prazos.api`.
- `api`: expoe `NotificacaoPrazoPort`, comando e resultado.
- `infrastructure`: adapta para `CalendarNotificationEventPublisher`.
- `web`: nao criado nesta onda.

## 3. Conexao com prazos

`NotificacaoPrazoApplicationService.notificarPrazoCalculado` recebe `PrazoProcessualCalculoResult` do modulo `prazos`. A prioridade e definida pela policy:

- `CRITICA` quando ha conferencia manual ou marco inicial nao util.
- `ALTA` quando o vencimento esta muito proximo.
- `NORMAL` para os demais alertas.

## 4. Conexao com legado

`CalendarPrazoNotificacaoAdapter` publica `CalendarNotificationEnvelope` usando `CalendarNotificationEventPublisher`. O adapter nao acessa repository diretamente. A entrega real, deduplicacao, canais, preferencias, historico e outbox continuam no legado existente.

## 4.1 Fluxo legado migrado

`CalendarNotificationScheduler` passou a rotear envelopes de lane ou segmento `PRAZO`/`PRAZOS` para `NotificacaoPrazoApplicationService`. Envelopes que nao sao prazo continuam indo direto para `CalendarNotificationEventPublisher`.

Se a fronteira modular recusar o alerta ou lancar excecao operacional, o scheduler usa o publisher legado com o mesmo envelope. Isso preserva entrega e evita que a migracao modular interrompa notificacoes existentes.

## 5. Segurança e sigilo

O comando modular nao recebe conteudo processual sensivel. O corpo padrao informa somente vencimento forense e necessidade de conferencia manual. Qualquer enriquecimento futuro deve passar por policy de sigilo antes de chegar ao adapter.

## 6. Ports e adapters

- `NotificacaoPrazoPort` -> `CalendarPrazoNotificacaoAdapter`.
- Consumidor modular previsto: `NotificacaoPrazoApplicationService`.
- Produtor de dado de prazo previsto: `PrazoProcessualApplicationService`.

## 7. Tabelas

Nenhuma tabela nova foi criada. A persistencia de historico permanece no legado de notificacoes.

## 8. Services

- `NotificacaoPrazoApplicationService`.
- `NotificacaoPrazoPolicy`.
- `CalendarPrazoNotificacaoAdapter`.

## 9. Eventos

O adapter publica envelope no fluxo de calendario. Quando Kafka estiver habilitado, o publisher legado encaminha para o topico existente; quando nao estiver, ele chama o dispatch local ja existente.

## 10. Testes

- Teste de policy de notificacao de prazo.
- Teste de application service consumindo resultado de `modules.prazos.api`.
- Teste de adapter capturando `CalendarNotificationEnvelope`.
- Teste ArchUnit do modulo `notificacoes`.

## 11. Riscos

- A entrega real ainda depende do legado de calendario/notificacao.
- O modulo ainda cobre apenas alerta de prazo, nao notificacao geral.
- Politica de sigilo enriquecida ficou para proxima onda.
- Auditoria global continua fora desta rodada.

## 12. Proxima fase

Migrar outro fluxo pequeno, de preferencia uma consulta/preview de prazo com baixo risco, para consumir o mesmo application service. Depois disso, avaliar um port de notificacao geral sem acoplar channels e repositories a novos modulos.
