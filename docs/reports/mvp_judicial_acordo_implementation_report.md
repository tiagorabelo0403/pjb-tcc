# MVP Judicial - Sala de Acordo Processual Controlada

## 1. O que foi implementado

Foi implementado o modulo `com.tcc.pjb.backend.modules.acordo` como MVP judicial principal para ODR controlada, com separacao em `domain`, `application`, `api`, `infrastructure` e documentacao de fronteira.

O fluxo coberto vai de processo apto para acordo ate sala controlada, participantes, mensagens, propostas formais, contrapropostas, minuta de termo, assinatura logica inicial, envio para homologacao, homologacao ou rejeicao, encerramento sem acordo, expiracao e auditoria dos atos sensiveis.

Principais capacidades:

- Politica de janela processual por momento, sigilo e requisitos de conciliador, mediador, advogado ou determinacao judicial.
- State machine para bloquear interacoes fora de estado, sala expirada, proposta vencida, IA sem revisao humana, termo sem assinatura, homologacao sem termo assinado e rejeicao sem motivo.
- Application service transacional com validacao de processo, usuario, participante aceito, sigilo, expiracao, proposta, termo e perfil de homologacao.
- Ports para processo, usuario, auditoria e movimentacao processual, evitando acoplamento direto com outros modulos no dominio.
- Persistencia JPA propria para sessoes, participantes, mensagens, propostas, termos e auditoria.
- Adapters para `ProcessoRepository`, `UsuarioRepository` e `MovimentacaoProcessualRepository`.
- Migration PostgreSQL/Flyway com constraints, FKs, indices e uso controlado de `JSONB`.
- Testes positivos, negativos e de arquitetura.

## 2. O que ja existia

Antes desta rodada ja existiam funcionalidades parciais e distintas:

- `AcordoService`, `PropostaAcordo` e `AcordoHomologado`, voltados a proposta/acordo legados.
- `ChatService` e modulo `atendimento`, voltados a atendimento/chat operacional.
- `Processo`, `Usuario`, `MovimentacaoProcessual`, repositorios e padroes de exception/migration.

Nada disso foi removido ou substituido em massa. O novo modulo preserva os fluxos existentes e cria uma fronteira judicial especifica para sala processual controlada.

## 3. Arquivos de referencia usados

Referencias de estilo e integracao:

- `pjb-api/src/main/java/com/tcc/pjb/backend/service/acordo/AcordoService.java`
- `pjb-api/src/main/java/com/tcc/pjb/backend/model/PropostaAcordo.java`
- `pjb-api/src/main/java/com/tcc/pjb/backend/model/AcordoHomologado.java`
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/atendimento/application/AtendimentoChatService.java`
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/atendimento/infrastructure/persistence/AtendimentoThread.java`
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/atendimento/web/AtendimentoChatController.java`
- `pjb-api/src/main/java/com/tcc/pjb/backend/model/MovimentacaoProcessual.java`
- `pjb-api/src/main/java/com/tcc/pjb/backend/repository/MovimentacaoProcessualRepository.java`
- `pjb-api/src/main/java/com/tcc/pjb/backend/exception/RegraNegocioException.java`
- Migrations `V90`, `V91`, `V99` e conferencia numerica posterior das migrations ate `V267`.

## 4. Como seguiu o estilo do PJB

O modulo segue o padrao Spring Boot 3/Java 21 ja presente no projeto:

- Injetou dependencias por construtor.
- Usou `@Transactional` nos casos de uso de escrita e `readOnly = true` em leitura.
- Manteve controller fora da rodada por nao haver autorizacao HTTP especifica consolidada para esta superficie sensivel.
- Nao expôs entity diretamente.
- Nao adicionou anotacoes Lombok inseguras em entity, carga ansiosa, injecao em campo, consulta total em fluxo produtivo, prints, stack traces diretos, marcadores de pendencia ou implementacao ficticia.
- Manteve dominio sem Spring/JPA e application sem dependencia de web, validado por ArchUnit.
- Usou adapters para conversar com processo, usuario e movimentacao.

## 5. Como a Sala substitui chat comum

A sala nao e apenas troca de mensagens. Ela representa um ambiente processual controlado:

- So abre dentro de janela processual autorizada.
- Herda sigilo do processo.
- Requer participantes autorizados e aceitos.
- Diferencia mensagem, proposta formal, contraproposta e termo.
- Bloqueia proposta vencida e IA sem revisao humana.
- Exige termo assinado antes de homologacao.
- Registra auditoria de atos sensiveis.
- Gera movimentacao processual por port/facade apenas em atos proprios.

## 6. Conexoes judiciais implementadas

Processo:

- `ProcessoAcordoPort` valida existencia, contexto processual, segredo de justica e registro de movimentacao.
- `PjbProcessoAcordoAdapter` consulta `ProcessoRepository` e grava `MovimentacaoProcessual`.

Participantes e usuarios:

- `UsuarioAcordoPort` valida existencia, autorizacao de participacao e permissao de homologacao.
- `PjbUsuarioAcordoAdapter` usa `UsuarioRepository` e `ProcessoRepository`.

Movimentacao:

- `MovimentacaoAcordoPort` registra homologacao e encerramento sem acordo.
- Rejeicao tambem registra movimento controlado por port de processo.

Seguranca:

- Application service exige participante aceito para interagir.
- Processo sigiloso cria sala com nivel de confidencialidade de segredo de justica.
- Mensagem publica processual e bloqueada em sala sigilosa.
- Homologacao/rejeicao exigem usuario com perfil autorizado.

Auditoria:

- `AuditoriaAcordoPort` recebe eventos sensiveis.
- `JpaAuditoriaAcordoAdapter` persiste evento, usuario, detalhes estruturados e hashes de IP/user-agent.

## 7. Migration criada

Migration criada:

- `pjb-api/src/main/resources/db/migration/V268__sala_acordo_processual.sql`

Tabelas:

- `tb_sessao_acordo_processual`
- `tb_acordo_participante`
- `tb_acordo_mensagem`
- `tb_acordo_proposta`
- `tb_acordo_termo`
- `tb_acordo_auditoria`

A migration possui FKs para processo e usuario quando aplicavel, FKs entre tabelas do modulo, check constraints para status/tipo/papel/nivel, indices por processo, sessao, status e expiracao.

`JSONB` foi usado em `tb_acordo_proposta.termos_json` e `tb_acordo_auditoria.detalhes_json` porque proposta e auditoria possuem dados estruturados variaveis por tipo de acordo, sem justificar colunas livres ou varchar sem governanca. Os estados e tipos criticos continuam normalizados por colunas com check constraints.

## 8. Classes criadas

Principais classes por camada:

- Domain: `AcordoProcessualWindowPolicy`, `AcordoProcessualStateMachine`, enums de status, tipo, papel, evento, visibilidade, confidencialidade, proposta e termo.
- Application: `AcordoProcessualApplicationService`, `AcordoProcessualStorePort`, snapshots, metadata e exception propria.
- API: `ProcessoAcordoPort`, `UsuarioAcordoPort`, `AuditoriaAcordoPort`, `MovimentacaoAcordoPort`, `ProcessoAcordoContexto`, `AcordoAuditEntry`.
- Infrastructure: adapters `PjbProcessoAcordoAdapter`, `PjbUsuarioAcordoAdapter`, `PjbMovimentacaoAcordoAdapter`, `JpaAuditoriaAcordoAdapter`.
- Persistence: entities e repositories JPA para sessao, participante, mensagem, proposta, termo e auditoria.

## 9. Testes criados

Foram criados:

- `pjb-api/src/test/java/com/tcc/pjb/backend/modules/acordo/application/AcordoProcessualApplicationServiceTest.java`
- `pjb-api/src/test/java/com/tcc/pjb/backend/modules/acordo/AcordoArchitectureTest.java`

Coberturas principais:

- Sala nao abre fora da janela.
- Sala abre em momento permitido.
- Processo sigiloso gera sala sigilosa.
- Participante precisa aceitar.
- Participante nao autorizado nao acessa.
- Mensagem nao cria proposta.
- Proposta formal exige validade.
- Proposta expirada nao gera termo.
- Proposta criada por IA exige revisao humana.
- Termo nao vai para homologacao sem assinatura.
- Termo assinado vai para homologacao.
- Homologacao muda status e registra movimentacao.
- Rejeicao registra motivo.
- Sala expirada nao aceita mensagem.
- Mensagem confidencial nao gera movimentacao publica.
- Encerramento sem acordo audita e movimenta.
- Toda acao sensivel testada gera auditoria.
- Expiracao de salas vencidas.
- Domain sem Spring/JPA, application sem web, controller sem repository caso exista.

## 10. Testes rodados

Comandos executados:

- `.\mvnw.cmd -B -pl pjb-api test-compile --no-transfer-progress`
- `.\mvnw.cmd -B -pl pjb-api "-Dtest=AcordoProcessualApplicationServiceTest,AcordoArchitectureTest" test --no-transfer-progress`
- `.\mvnw.cmd -B test -pl pjb-api --no-transfer-progress`

Resultado final:

- `test-compile`: BUILD SUCCESS.
- Testes direcionados do modulo: BUILD SUCCESS, 21 testes, 0 falhas, 0 erros.
- Teste completo do `pjb-api`: BUILD SUCCESS, 3264 testes, 0 falhas, 0 erros, 2 ignorados.

Durante a validacao, uma primeira execucao completa falhou apenas porque a regra ArchUnit de controller nao aceitava pacote vazio, ja que nenhum controller foi criado nesta rodada. A causa raiz foi corrigida com regra explicita para pacote vazio e a suite completa passou em seguida.

## 11. Riscos restantes

- Controller HTTP nao foi criado nesta rodada para evitar endpoint sensivel sem regra ABAC/step-up especifica consolidada.
- Assinatura e logica e inicial; ICP-Brasil, carimbo de tempo e validacao criptografica ficam para a proxima etapa.
- RLS em banco e politicas ABAC detalhadas por orgao/unidade ainda devem ser aprofundadas.
- Integracao documental do termo ainda esta limitada ao termo persistido no modulo; geracao de documento oficial fica para fase seguinte.
- Expiracao esta implementada como caso de uso, mas agendamento operacional nao foi ligado nesta rodada.

## 12. Proxima etapa

A proxima fase recomendada e expor endpoints seguros com DTOs, ABAC explicito, step-up para assinatura/homologacao, integracao documental do termo e assinatura ICP-Brasil, mantendo a sala como nucleo do fluxo processual de acordo.

Confirmacao operacional:

- Nao houve push.
- Nao houve merge.
- Nao houve `git reset`.
- Nao houve alteracao de migrations antigas.
