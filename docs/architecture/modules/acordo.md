# Modulo Acordo

## 1. Responsabilidade

O modulo `acordo` implementa a Sala de Acordo Processual Controlada. Ele governa janela processual, participantes, mensagens, propostas formais, contrapropostas, termo, assinatura logica inicial, envio para homologacao, homologacao, rejeicao, encerramento e expiracao.

## 2. Fronteira

O dominio fica em `com.tcc.pjb.backend.modules.acordo.domain` e nao depende de Spring, JPA, web ou repositories. A aplicacao orquestra casos de uso e transacoes. A infraestrutura persiste os dados e adapta processo, usuario, auditoria e movimentacao por ports.

## 3. Por que substitui chat comum

O chat comum registra conversa. A sala de acordo registra ato processual controlado. Ela exige janela processual valida, participante aceito, confidencialidade, proposta formal com validade, revisao humana para proposta de IA, termo, assinatura e decisao judicial.

O chat processual legado agora funciona como superficie de entrada para a sala quando o canal e negocial. Mensagens em canal explicito de acordo exigem sala ativa, participante convidado e aceite previo; mensagens comuns continuam preservadas no chat legado e so sao espelhadas na sala quando houver sala ativa e contexto negocial.

## 4. Conexoes com processo

`ProcessoAcordoPort` consulta existencia, contexto processual, segredo de justica e registra movimentacao generica de acordo. O adapter usa `ProcessoRepository` como fonte publicada e converte `FaseProcessual`, sigilo e sinais textuais da janela de acordo para o contrato do modulo.

## 5. Conexoes com usuario/participante

`UsuarioAcordoPort` valida existencia, participacao no processo e permissao de homologacao. O adapter autoriza parte por CPF, advogado vinculado como usuario do processo, conciliador, mediador, magistrado, servidor judiciario e administrador ativo.

## 6. Conexoes com movimentacao

`MovimentacaoAcordoPort` registra homologacao e encerramento sem acordo em `tb_movimentacao_processual`. Mensagem confidencial nao gera movimentacao publica.

## 7. Conexoes com seguranca

O application service exige usuario existente, participante aceito para interagir, autorizacao por processo no convite e perfil de magistratura para homologar ou rejeitar. Processo sigiloso cria sala com `SEGREDO_JUSTICA`.

## 8. Conexoes com auditoria

`AuditoriaAcordoPort` grava todos os atos sensiveis em `tb_acordo_auditoria`: abertura, convite, aceite, recusa, mensagem, proposta, contraproposta, termo, revisao humana, assinatura, envio para homologacao, homologacao, rejeicao, encerramento e expiracao.

## 9. Tabelas

- `tb_sessao_acordo_processual`
- `tb_acordo_participante`
- `tb_acordo_mensagem`
- `tb_acordo_proposta`
- `tb_acordo_termo`
- `tb_acordo_auditoria`

`termos_json` e `detalhes_json` usam JSONB porque representam conteudo estruturado, variavel por tipo de proposta/evento e consultavel por chave no PostgreSQL sem criar colunas artificiais para cada rito.

## 10. Services

- `AcordoProcessualApplicationService`
- `AcordoProcessualChatBridgeService`
- `AcordoProcessualWindowPolicy`
- `AcordoProcessualStateMachine`

## 11. Eventos

Nesta rodada os eventos sensiveis sao materializados na auditoria propria. A integracao externa por outbox ou ledger fica para etapa posterior.

## 12. Testes

Os testes cobrem janela processual, sigilo, aceite de participante, usuario nao autorizado, mensagem, proposta, validade, IA com revisao humana, termo, assinatura, homologacao, rejeicao, expiracao, auditoria e fronteiras arquiteturais.

## 13. Riscos

- A politica de participacao ainda depende de dados disponiveis no processo legado e nao de uma tabela nacional completa de poderes/mandatos.
- A assinatura logica inicial nao substitui ICP-Brasil.
- Os endpoints de chat para sala usam a autorizacao ja existente do chat processual e o application service da sala; a proxima fase deve adicionar step-up e idempotencia para assinatura, termo e homologacao.

## 14. Proxima fase

Ampliar controller seguro com rate limit por capacidade, idempotencia por comando critico, step-up para assinatura/homologacao e integracao com assinatura ICP-Brasil.
