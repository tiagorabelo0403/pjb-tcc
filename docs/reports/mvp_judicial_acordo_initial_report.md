# MVP Judicial de Sala de Acordo Processual Controlada - Relatorio Inicial

## 1. Estado inicial do git

- Diretorio: `C:\PJB`
- Branch: `master`
- `git fetch origin`: executado com sucesso.
- HEAD inicial: `dad8dfc (HEAD -> master, origin/master, origin/HEAD) feat(conexoes): polo -> ciencia (listarUsuarioIdsDestinatarios) e estado -> PainelSerieTemporalDiaria (autuado/sentenciado/arquivado)`
- `git status --short`: nao exibiu arquivos modificados, adicionados ou removidos. O comando emitiu apenas aviso de permissao ao ignore global em `C:\Users\tiago\.config\git\ignore`.
- Nenhum push foi executado.
- Nenhum merge foi executado.
- Nenhum reset foi executado.

## 2. Ultima migration encontrada

- Saida do comando lexicografico solicitado: `V99__security_reqhash_panic_watermark.sql`.
- Maior versao numerica real encontrada na pasta: `V267__carga_processo_fisico.sql`.
- Proxima migration planejada sem reutilizar numero: `V268__sala_acordo_processual.sql`.

## 3. Arquivos usados como referencia de estilo

- Service parecido: `pjb-api/src/main/java/com/tcc/pjb/backend/modules/atendimento/service/AtendimentoChatService.java`
- Service legado de acordo: `pjb-api/src/main/java/com/tcc/pjb/backend/service/AcordoService.java`
- Entity parecida: `pjb-api/src/main/java/com/tcc/pjb/backend/modules/atendimento/entity/AtendimentoThread.java`
- Entity legado de acordo: `pjb-api/src/main/java/com/tcc/pjb/backend/model/entity/PropostaAcordo.java`
- Homologacao legado: `pjb-api/src/main/java/com/tcc/pjb/backend/model/entity/AcordoHomologado.java`
- Migration parecida: `pjb-api/src/main/resources/db/migration/V90__atendimento_thread_settings_policy.sql`
- Migration com FKs e auditoria: `pjb-api/src/main/resources/db/migration/V91__atendimento_message_receipts_checklist.sql`
- Controller de referencia: `pjb-api/src/main/java/com/tcc/pjb/backend/modules/atendimento/controller/AtendimentoChatController.java`
- Teste parecido: `pjb-api/src/test/java/com/tcc/pjb/backend/modules/atendimento/service/AtendimentoChatMessagingSupportTest.java`
- Teste legado de acordo: `pjb-api/src/test/java/com/tcc/pjb/backend/service/AcordoServiceTest.java`
- Exception de negocio: `pjb-api/src/main/java/com/tcc/pjb/backend/service/exception/RegraNegocioException.java`
- Movimentacao processual: `pjb-api/src/main/java/com/tcc/pjb/backend/model/entity/workflow/MovimentacaoProcessual.java`
- Repository de movimentacao: `pjb-api/src/main/java/com/tcc/pjb/backend/model/repository/MovimentacaoProcessualRepository.java`

## 4. O que ja existe e sera preservado

- Existe chat comum e modulo de atendimento em `modules.atendimento`, voltado a conversa cidadao/advogado, recibos, anexos, TOS e moderacao.
- Existe `AcordoService` legado com proposta, homologacao judicial, notificacao, PDF e chat como efeito colateral.
- Existem `PropostaAcordo` e `AcordoHomologado`, que representam proposta e homologacao legadas, mas nao uma sala processual controlada com participantes, janela processual, confidencialidade, estado de sala, termo e trilha propria.
- Existe movimentacao processual em `tb_movimentacao_processual`, que sera acessada por adapter via port.
- Nada do legado sera removido ou movido.

## 5. O que sera criado

- Modulo `com.tcc.pjb.backend.modules.acordo` com camadas `domain`, `application`, `api` e `infrastructure`.
- Policy de janela processual `AcordoProcessualWindowPolicy`.
- Maquina de estado `AcordoProcessualStateMachine`.
- Ports para processo, usuario, auditoria e movimentacao.
- Application service transacional `AcordoProcessualApplicationService`.
- Persistencia propria da sala, participantes, mensagens, propostas, termos e auditoria.
- Migration `V268__sala_acordo_processual.sql`.
- Documentacao em `docs/architecture/modules/acordo.md`.
- Testes de dominio, application service e fronteiras arquiteturais.
- Relatorio final de implementacao.

## 6. Risco de duplicacao

- Risco medio se o modulo tentasse substituir `AcordoService`, `PropostaAcordo` ou `AtendimentoChatService`.
- Mitigacao: o novo modulo nao altera os fluxos existentes. Ele cria a Sala de Acordo Processual Controlada como contexto proprio e integra com processo, usuario e movimentacao por ports.

## 7. Risco de seguranca

- Risco alto em criar endpoint publico sem politica dedicada de autorizacao e rate limit.
- Mitigacao: a rodada nao criara controller HTTP. O application service exigira ator, participacao aceita, usuario existente, autorizacao de participacao e perfil de homologacao via ports.
- Mensagem confidencial nao sera convertida em movimentacao publica.
- Processo sigiloso gerara sala com sigilo e nivel de confidencialidade restrito.

## 8. Plano de implementacao

1. Criar dominio puro com enums, policy, decision record e state machine.
2. Criar contracts em `api` para processo, usuario, auditoria e movimentacao.
3. Criar application service com comandos, snapshots e persistencia por port.
4. Criar JPA entities, repositories e adapters em `infrastructure`.
5. Criar migration `V100__sala_acordo_processual.sql` com FKs, checks, indices e JSONB.
6. Criar documentacao arquitetural do modulo.
7. Criar testes positivos, negativos e de dependencia entre camadas.
8. Rodar `test-compile` e depois testes do modulo.

## 9. Plano de testes

- Janela processual permitida e negada.
- Processo sigiloso abrindo sala sigilosa.
- Participante pendente bloqueado ate aceite.
- Usuario nao autorizado bloqueado.
- Mensagem nao cria proposta.
- Proposta formal exige validade.
- Proposta expirada bloqueia termo/aceite.
- Proposta criada por IA exige revisao humana.
- Termo sem assinatura nao segue para homologacao.
- Termo assinado segue para homologacao.
- Homologacao e rejeicao atualizam estado e auditam.
- Sala expirada bloqueia mensagem.
- Mensagem confidencial nao gera movimentacao publica.
- Encerramento sem acordo audita e movimenta por port.
- Toda acao sensivel gera auditoria.
- Dominio sem dependencia de Spring.
- Application sem dependencia de web.

## 10. Confirmacao de que nao houve push

Nenhum push foi executado nesta fase inicial.
