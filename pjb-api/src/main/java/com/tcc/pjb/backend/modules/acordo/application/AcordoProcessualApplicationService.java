package com.tcc.pjb.backend.modules.acordo.application;

import com.tcc.pjb.backend.modules.acordo.api.AcordoAuditEntry;
import com.tcc.pjb.backend.modules.acordo.api.AuditoriaAcordoPort;
import com.tcc.pjb.backend.modules.acordo.api.MovimentacaoAcordoPort;
import com.tcc.pjb.backend.modules.acordo.api.ProcessoAcordoContexto;
import com.tcc.pjb.backend.modules.acordo.api.ProcessoAcordoPort;
import com.tcc.pjb.backend.modules.acordo.api.UsuarioAcordoPort;
import com.tcc.pjb.backend.modules.acordo.domain.AcordoAuditoriaEvento;
import com.tcc.pjb.backend.modules.acordo.domain.AcordoConfidencialidadeNivel;
import com.tcc.pjb.backend.modules.acordo.domain.AcordoMensagemTipo;
import com.tcc.pjb.backend.modules.acordo.domain.AcordoMensagemVisibilidade;
import com.tcc.pjb.backend.modules.acordo.domain.AcordoPapelParticipante;
import com.tcc.pjb.backend.modules.acordo.domain.AcordoParticipanteStatus;
import com.tcc.pjb.backend.modules.acordo.domain.AcordoProcessualStateMachine;
import com.tcc.pjb.backend.modules.acordo.domain.AcordoProcessualWindowDecision;
import com.tcc.pjb.backend.modules.acordo.domain.AcordoProcessualWindowInput;
import com.tcc.pjb.backend.modules.acordo.domain.AcordoProcessualWindowPolicy;
import com.tcc.pjb.backend.modules.acordo.domain.AcordoPropostaStatus;
import com.tcc.pjb.backend.modules.acordo.domain.AcordoPropostaTipo;
import com.tcc.pjb.backend.modules.acordo.domain.AcordoSessaoStatus;
import com.tcc.pjb.backend.modules.acordo.domain.AcordoTermoStatus;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AcordoProcessualApplicationService {

    private static final Duration DEFAULT_EXPIRACAO = Duration.ofDays(15);
    private static final Duration MAX_EXPIRACAO = Duration.ofDays(60);
    private static final int MAX_MENSAGEM = 8000;
    private static final int MAX_TERMO = 30000;

    private final AcordoProcessualStorePort store;
    private final ProcessoAcordoPort processoPort;
    private final UsuarioAcordoPort usuarioPort;
    private final AuditoriaAcordoPort auditoriaPort;
    private final MovimentacaoAcordoPort movimentacaoPort;
    private final Clock clock;
    private final AcordoProcessualWindowPolicy windowPolicy = new AcordoProcessualWindowPolicy();
    private final AcordoProcessualStateMachine stateMachine = new AcordoProcessualStateMachine();

    public AcordoProcessualApplicationService(AcordoProcessualStorePort store,
                                              ProcessoAcordoPort processoPort,
                                              UsuarioAcordoPort usuarioPort,
                                              AuditoriaAcordoPort auditoriaPort,
                                              MovimentacaoAcordoPort movimentacaoPort,
                                              Clock clock) {
        this.store = Objects.requireNonNull(store);
        this.processoPort = Objects.requireNonNull(processoPort);
        this.usuarioPort = Objects.requireNonNull(usuarioPort);
        this.auditoriaPort = Objects.requireNonNull(auditoriaPort);
        this.movimentacaoPort = Objects.requireNonNull(movimentacaoPort);
        this.clock = Objects.requireNonNull(clock);
    }

    @Transactional
    public AcordoSessaoSnapshot abrirSala(AbrirSalaCommand command) {
        Objects.requireNonNull(command, "command");
        Long processoId = requireId(command.processoId(), "processoId");
        Long abertaPorId = requireId(command.abertaPorId(), "abertaPorId");
        requireUsuarioExistente(abertaPorId);
        if (!processoPort.existeProcesso(processoId)) {
            throw new AcordoApplicationException("Processo nao encontrado.");
        }
        if (!usuarioPort.usuarioPodeParticipar(processoId, abertaPorId)) {
            throw new AcordoApplicationException("Usuario nao autorizado a abrir sala para o processo.");
        }
        ProcessoAcordoContexto contexto = processoPort.obterContextoProcessual(processoId);
        AcordoProcessualWindowDecision decision = windowPolicy.avaliar(toWindowInput(contexto, command));
        if (!decision.permitido()) {
            throw new AcordoApplicationException(decision.motivo());
        }
        Instant now = Instant.now(clock);
        Instant expiraEm = normalizarExpiracao(command.expiraEm(), now);
        boolean segredo = decision.exigeSigilo() || processoPort.processoEstaEmSegredo(processoId);
        AcordoSessaoSnapshot sessao = store.saveSessao(new AcordoSessaoSnapshot(
                null,
                processoId,
                decision.tipoSalaSugerido(),
                AcordoSessaoStatus.WAITING_PARTICIPANTS,
                abertaPorId,
                now,
                expiraEm,
                requireText(command.motivoAbertura(), "motivoAbertura", 1000),
                segredo,
                segredo ? AcordoConfidencialidadeNivel.SEGREDO_JUSTICA : AcordoConfidencialidadeNivel.RESTRITA_A_PARTICIPANTES,
                command.cejuscReferenciado(),
                null,
                null,
                now
        ));
        store.saveParticipante(new AcordoParticipanteSnapshot(
                null,
                sessao.id(),
                abertaPorId,
                command.papelAbertura() != null ? command.papelAbertura() : AcordoPapelParticipante.SERVIDOR_AUTORIZADO,
                AcordoParticipanteStatus.ACEITO,
                now,
                null,
                now
        ));
        audit(sessao.id(), abertaPorId, AcordoAuditoriaEvento.ABERTURA, metadata(command.metadata()), details(
                "processoId", processoId,
                "tipoSala", sessao.tipoSala().name(),
                "momento", decision.momento().name(),
                "segredoJustica", segredo,
                "expiraEm", expiraEm.toString()
        ));
        return sessao;
    }

    @Transactional
    public AcordoParticipanteSnapshot convidarParticipante(ConvidarParticipanteCommand command) {
        Objects.requireNonNull(command, "command");
        Instant now = Instant.now(clock);
        AcordoSessaoSnapshot sessao = requireSessaoForUpdate(command.sessaoId());
        requireSalaNaoExpirada(sessao, now);
        Long convidanteId = requireId(command.convidanteId(), "convidanteId");
        Long convidadoId = requireId(command.convidadoId(), "convidadoId");
        requireGestorOuParticipanteAceito(sessao, convidanteId);
        requireUsuarioExistente(convidadoId);
        if (!usuarioPort.usuarioPodeParticipar(sessao.processoId(), convidadoId)) {
            throw new AcordoApplicationException("Usuario convidado nao autorizado para o processo.");
        }
        if (store.findParticipante(sessao.id(), convidadoId).isPresent()) {
            throw new AcordoApplicationException("Usuario ja consta como participante da sala.");
        }
        AcordoParticipanteSnapshot participante = store.saveParticipante(new AcordoParticipanteSnapshot(
                null,
                sessao.id(),
                convidadoId,
                Objects.requireNonNull(command.papel(), "papel"),
                AcordoParticipanteStatus.CONVIDADO,
                null,
                null,
                now
        ));
        if (sessao.status() == AcordoSessaoStatus.WAITING_PARTICIPANTS) {
            store.saveSessao(sessao.withStatus(AcordoSessaoStatus.INVITED));
        }
        audit(sessao.id(), convidanteId, AcordoAuditoriaEvento.CONVITE, metadata(command.metadata()), details(
                "convidadoId", convidadoId,
                "papel", command.papel().name()
        ));
        return participante;
    }

    @Transactional
    public AcordoParticipanteSnapshot aceitarParticipacao(ParticipacaoCommand command) {
        Objects.requireNonNull(command, "command");
        Instant now = Instant.now(clock);
        AcordoSessaoSnapshot sessao = requireSessaoForUpdate(command.sessaoId());
        requireSalaNaoExpirada(sessao, now);
        Long usuarioId = requireId(command.usuarioId(), "usuarioId");
        AcordoParticipanteSnapshot participante = requireParticipante(sessao.id(), usuarioId);
        if (participante.status() == AcordoParticipanteStatus.REMOVIDO) {
            throw new AcordoApplicationException("Participante removido nao pode aceitar a sala.");
        }
        AcordoParticipanteSnapshot aceito = store.saveParticipante(participante.withAceite(now));
        AcordoSessaoStatus novoStatus = store.countParticipantesAceitos(sessao.id()) >= 2
                ? AcordoSessaoStatus.OPEN
                : AcordoSessaoStatus.WAITING_PARTICIPANTS;
        if (sessao.status() != novoStatus) {
            store.saveSessao(sessao.withStatus(novoStatus));
        }
        audit(sessao.id(), usuarioId, AcordoAuditoriaEvento.ACEITE, metadata(command.metadata()), details("statusSala", novoStatus.name()));
        return aceito;
    }

    @Transactional
    public AcordoParticipanteSnapshot recusarParticipacao(RecusaParticipacaoCommand command) {
        Objects.requireNonNull(command, "command");
        Instant now = Instant.now(clock);
        AcordoSessaoSnapshot sessao = requireSessaoForUpdate(command.sessaoId());
        requireSalaNaoExpirada(sessao, now);
        Long usuarioId = requireId(command.usuarioId(), "usuarioId");
        AcordoParticipanteSnapshot participante = requireParticipante(sessao.id(), usuarioId);
        AcordoParticipanteSnapshot recusado = store.saveParticipante(participante.withRecusa(now));
        audit(sessao.id(), usuarioId, AcordoAuditoriaEvento.RECUSA, metadata(command.metadata()), details(
                "motivo", limit(command.motivo(), 1000)
        ));
        return recusado;
    }

    @Transactional
    public AcordoMensagemSnapshot registrarMensagem(RegistrarMensagemCommand command) {
        Objects.requireNonNull(command, "command");
        Instant now = Instant.now(clock);
        AcordoSessaoSnapshot sessao = requireSessaoForUpdate(command.sessaoId());
        AcordoParticipanteSnapshot participante = requireParticipante(sessao.id(), requireId(command.autorId(), "autorId"));
        boolean expirada = sessao.expiradaEm(now);
        stateMachine.requireMensagemPermitida(sessao.status(), expirada, participante.aceito());
        AcordoMensagemVisibilidade visibilidade = command.visibilidade() != null ? command.visibilidade() : AcordoMensagemVisibilidade.PARTICIPANTES;
        if (sessao.segredoJustica() && visibilidade == AcordoMensagemVisibilidade.PUBLICA_PROCESSUAL) {
            throw new AcordoApplicationException("Sala sigilosa nao permite mensagem de visibilidade publica processual.");
        }
        if (command.confidencial() && visibilidade == AcordoMensagemVisibilidade.PUBLICA_PROCESSUAL) {
            throw new AcordoApplicationException("Mensagem confidencial nao pode ser publica processual.");
        }
        AcordoMensagemSnapshot mensagem = store.saveMensagem(new AcordoMensagemSnapshot(
                null,
                sessao.id(),
                command.autorId(),
                command.tipo() != null ? command.tipo() : AcordoMensagemTipo.TEXTO,
                requireText(command.conteudo(), "conteudo", MAX_MENSAGEM),
                command.confidencial(),
                command.confidencial() ? AcordoMensagemVisibilidade.CONFIDENCIAL : visibilidade,
                now
        ));
        audit(sessao.id(), command.autorId(), AcordoAuditoriaEvento.MENSAGEM, metadata(command.metadata()), details(
                "mensagemId", mensagem.id(),
                "tipo", mensagem.tipo().name(),
                "confidencial", mensagem.confidencial(),
                "visibilidade", mensagem.visibilidade().name()
        ));
        return mensagem;
    }

    @Transactional
    public AcordoPropostaSnapshot registrarProposta(RegistrarPropostaCommand command) {
        return registrarPropostaInterna(command, AcordoPropostaTipo.FORMAL, AcordoAuditoriaEvento.PROPOSTA, AcordoSessaoStatus.PROPOSAL_PENDING);
    }

    @Transactional
    public AcordoPropostaSnapshot registrarContraproposta(RegistrarPropostaCommand command) {
        return registrarPropostaInterna(command, AcordoPropostaTipo.CONTRAPROPOSTA, AcordoAuditoriaEvento.CONTRAPROPOSTA, AcordoSessaoStatus.COUNTERPROPOSAL_PENDING);
    }

    @Transactional
    public AcordoPropostaSnapshot marcarRevisaoHumana(MarcarRevisaoHumanaCommand command) {
        Objects.requireNonNull(command, "command");
        Instant now = Instant.now(clock);
        AcordoPropostaSnapshot proposta = requirePropostaForUpdate(command.propostaId());
        AcordoSessaoSnapshot sessao = requireSessaoForUpdate(proposta.sessaoId());
        Long revisorId = requireId(command.revisorId(), "revisorId");
        requireUsuarioExistente(revisorId);
        if (!proposta.criadaPorIa()) {
            throw new AcordoApplicationException("Apenas proposta criada por IA exige marcacao de revisao humana.");
        }
        if (!usuarioPort.usuarioPodeHomologar(revisorId)) {
            AcordoParticipanteSnapshot participante = requireParticipante(sessao.id(), revisorId);
            stateMachine.requireInteracaoParticipanteAceito(participante.aceito());
        }
        AcordoPropostaSnapshot revisada = store.saveProposta(proposta.withRevisaoHumana(revisorId, now));
        audit(sessao.id(), revisorId, AcordoAuditoriaEvento.REVISAO_HUMANA, metadata(command.metadata()), details(
                "propostaId", proposta.id(),
                "revisadaPorHumano", true
        ));
        return revisada;
    }

    @Transactional
    public AcordoTermoSnapshot gerarMinutaTermo(GerarMinutaTermoCommand command) {
        Objects.requireNonNull(command, "command");
        Instant now = Instant.now(clock);
        AcordoPropostaSnapshot proposta = requirePropostaForUpdate(command.propostaId());
        AcordoSessaoSnapshot sessao = requireSessaoForUpdate(proposta.sessaoId());
        AcordoParticipanteSnapshot participante = requireParticipante(sessao.id(), requireId(command.usuarioId(), "usuarioId"));
        stateMachine.requireGeracaoTermo(
                sessao.status(),
                sessao.expiradaEm(now),
                participante.aceito(),
                proposta.status(),
                proposta.expiradaEm(now),
                proposta.criadaPorIa(),
                proposta.revisadaPorHumano()
        );
        if (store.findTermoByProposta(proposta.id()).isPresent()) {
            throw new AcordoApplicationException("Proposta ja possui termo gerado.");
        }
        String conteudo = requireText(command.conteudoTermo(), "conteudoTermo", MAX_TERMO);
        AcordoTermoSnapshot termo = store.saveTermo(new AcordoTermoSnapshot(
                null,
                sessao.id(),
                proposta.id(),
                conteudo,
                sha256Hex(conteudo),
                AcordoTermoStatus.MINUTA,
                now
        ));
        store.saveSessao(sessao.withStatus(AcordoSessaoStatus.AGREEMENT_DRAFTED));
        store.saveProposta(proposta.withStatus(AcordoPropostaStatus.ACEITA));
        audit(sessao.id(), command.usuarioId(), AcordoAuditoriaEvento.GERACAO_TERMO, metadata(command.metadata()), details(
                "propostaId", proposta.id(),
                "termoId", termo.id(),
                "hashTermo", termo.hashTermo()
        ));
        return termo;
    }

    @Transactional
    public AcordoTermoSnapshot assinarTermo(AssinarTermoCommand command) {
        Objects.requireNonNull(command, "command");
        Instant now = Instant.now(clock);
        AcordoTermoSnapshot termo = requireTermoForUpdate(command.termoId());
        AcordoSessaoSnapshot sessao = requireSessaoForUpdate(termo.sessaoId());
        AcordoParticipanteSnapshot participante = requireParticipante(sessao.id(), requireId(command.usuarioId(), "usuarioId"));
        stateMachine.requireAssinatura(sessao.status(), sessao.expiradaEm(now), participante.aceito(), true, participante.papel().podeAssinarTermo());
        String hashAssinatura = requireText(command.hashAssinatura(), "hashAssinatura", 512);
        if (hashAssinatura.length() < 16) {
            throw new AcordoApplicationException("Assinatura logica exige hash minimo.");
        }
        AcordoTermoSnapshot assinado = store.saveTermo(termo.withStatus(AcordoTermoStatus.ASSINADO));
        store.saveSessao(sessao.withStatus(AcordoSessaoStatus.SIGNED));
        audit(sessao.id(), command.usuarioId(), AcordoAuditoriaEvento.ASSINATURA, metadata(command.metadata()), details(
                "termoId", termo.id(),
                "assinaturaHash", sha256Hex(hashAssinatura),
                "papel", participante.papel().name()
        ));
        return assinado;
    }

    @Transactional
    public AcordoTermoSnapshot enviarParaHomologacao(EnviarHomologacaoCommand command) {
        Objects.requireNonNull(command, "command");
        AcordoTermoSnapshot termo = requireTermoForUpdate(command.termoId());
        AcordoSessaoSnapshot sessao = requireSessaoForUpdate(termo.sessaoId());
        AcordoParticipanteSnapshot participante = requireParticipante(sessao.id(), requireId(command.usuarioId(), "usuarioId"));
        stateMachine.requireInteracaoParticipanteAceito(participante.aceito());
        stateMachine.requireEnvioHomologacao(sessao.status(), termo.status());
        AcordoTermoSnapshot enviado = store.saveTermo(termo.withStatus(AcordoTermoStatus.ENVIADO_HOMOLOGACAO));
        store.saveSessao(sessao.withStatus(AcordoSessaoStatus.SENT_TO_HOMOLOGATION));
        audit(sessao.id(), command.usuarioId(), AcordoAuditoriaEvento.ENVIO_HOMOLOGACAO, metadata(command.metadata()), details(
                "termoId", termo.id(),
                "propostaId", termo.propostaId()
        ));
        return enviado;
    }

    @Transactional
    public AcordoSessaoSnapshot homologar(HomologarCommand command) {
        Objects.requireNonNull(command, "command");
        Instant now = Instant.now(clock);
        AcordoSessaoSnapshot sessao = requireSessaoForUpdate(command.sessaoId());
        Long magistradoId = requireId(command.magistradoId(), "magistradoId");
        if (!usuarioPort.usuarioPodeHomologar(magistradoId)) {
            throw new AcordoApplicationException("Homologacao exige perfil autorizado de magistratura.");
        }
        AcordoTermoSnapshot termo = store.findTermoBySessao(sessao.id())
                .orElseThrow(() -> new AcordoApplicationException("Sessao sem termo para homologacao."));
        stateMachine.requireHomologacao(sessao.status(), termo.status());
        store.saveTermo(termo.withStatus(AcordoTermoStatus.HOMOLOGADO));
        AcordoSessaoSnapshot homologada = store.saveSessao(sessao.withHomologacao(AcordoSessaoStatus.HOMOLOGATED, now, magistradoId));
        String descricao = nonBlank(command.descricaoMovimentacao(), "Acordo processual homologado judicialmente.");
        movimentacaoPort.registrarHomologacao(sessao.processoId(), magistradoId, descricao);
        audit(sessao.id(), magistradoId, AcordoAuditoriaEvento.HOMOLOGACAO, metadata(command.metadata()), details(
                "termoId", termo.id(),
                "processoId", sessao.processoId()
        ));
        return homologada;
    }

    @Transactional
    public AcordoSessaoSnapshot rejeitarHomologacao(RejeitarHomologacaoCommand command) {
        Objects.requireNonNull(command, "command");
        AcordoSessaoSnapshot sessao = requireSessaoForUpdate(command.sessaoId());
        Long magistradoId = requireId(command.magistradoId(), "magistradoId");
        if (!usuarioPort.usuarioPodeHomologar(magistradoId)) {
            throw new AcordoApplicationException("Rejeicao de homologacao exige perfil autorizado de magistratura.");
        }
        AcordoTermoSnapshot termo = store.findTermoBySessao(sessao.id())
                .orElseThrow(() -> new AcordoApplicationException("Sessao sem termo para rejeicao."));
        String motivo = requireText(command.motivo(), "motivo", 4000);
        stateMachine.requireRejeicao(sessao.status(), termo.status(), motivo);
        store.saveTermo(termo.withStatus(AcordoTermoStatus.REJEITADO));
        AcordoSessaoSnapshot rejeitada = store.saveSessao(sessao.withStatus(AcordoSessaoStatus.REJECTED_BY_JUDGE));
        processoPort.registrarMovimentacaoAcordo(sessao.processoId(), "ACORDO_REJEITADO", "Homologacao do acordo rejeitada judicialmente: " + limit(motivo, 600));
        audit(sessao.id(), magistradoId, AcordoAuditoriaEvento.REJEICAO, metadata(command.metadata()), details(
                "termoId", termo.id(),
                "motivo", motivo
        ));
        return rejeitada;
    }

    @Transactional
    public AcordoSessaoSnapshot encerrarSemAcordo(EncerrarSemAcordoCommand command) {
        Objects.requireNonNull(command, "command");
        AcordoSessaoSnapshot sessao = requireSessaoForUpdate(command.sessaoId());
        Long usuarioId = requireId(command.usuarioId(), "usuarioId");
        if (!usuarioPort.usuarioPodeHomologar(usuarioId)) {
            AcordoParticipanteSnapshot participante = requireParticipante(sessao.id(), usuarioId);
            stateMachine.requireInteracaoParticipanteAceito(participante.aceito());
        }
        if (sessao.status().terminal()) {
            throw new AcordoApplicationException("Sala terminal nao pode ser encerrada novamente.");
        }
        String motivo = requireText(command.motivo(), "motivo", 2000);
        AcordoSessaoSnapshot encerrada = store.saveSessao(sessao.withStatus(AcordoSessaoStatus.CLOSED));
        movimentacaoPort.registrarEncerramentoSemAcordo(sessao.processoId(), usuarioId, "Sala de acordo encerrada sem composicao: " + limit(motivo, 600));
        audit(sessao.id(), usuarioId, AcordoAuditoriaEvento.ENCERRAMENTO, metadata(command.metadata()), details("motivo", motivo));
        return encerrada;
    }

    @Transactional
    public int expirarSalasVencidas(int limit) {
        Instant now = Instant.now(clock);
        int batchSize = Math.min(Math.max(limit, 1), 500);
        List<AcordoSessaoSnapshot> vencidas = store.findSessoesExpiradas(now, batchSize);
        int count = 0;
        for (AcordoSessaoSnapshot sessao : vencidas) {
            if (!sessao.status().terminal() && sessao.expiradaEm(now)) {
                store.saveSessao(sessao.withStatus(AcordoSessaoStatus.EXPIRED));
                audit(sessao.id(), null, AcordoAuditoriaEvento.EXPIRACAO, AcordoOperationMetadata.empty(), details(
                        "processoId", sessao.processoId(),
                        "expiraEm", sessao.expiraEm() != null ? sessao.expiraEm().toString() : null
                ));
                count++;
            }
        }
        return count;
    }

    @Transactional(readOnly = true)
    public AcordoSessaoSnapshot obterSala(Long sessaoId) {
        return store.findSessao(requireId(sessaoId, "sessaoId"))
                .orElseThrow(() -> new AcordoApplicationException("Sala de acordo nao encontrada."));
    }

    private AcordoPropostaSnapshot registrarPropostaInterna(RegistrarPropostaCommand command,
                                                           AcordoPropostaTipo tipo,
                                                           AcordoAuditoriaEvento evento,
                                                           AcordoSessaoStatus novoStatus) {
        Objects.requireNonNull(command, "command");
        Instant now = Instant.now(clock);
        AcordoSessaoSnapshot sessao = requireSessaoForUpdate(command.sessaoId());
        AcordoParticipanteSnapshot participante = requireParticipante(sessao.id(), requireId(command.autorId(), "autorId"));
        stateMachine.requirePropostaPermitida(sessao.status(), sessao.expiradaEm(now), participante.aceito(), command.validadeAte(), now);
        if (command.valor() != null && command.valor().compareTo(BigDecimal.ZERO) < 0) {
            throw new AcordoApplicationException("Valor de proposta nao pode ser negativo.");
        }
        String termosJson = requireJson(command.termosJson(), "termosJson");
        AcordoPropostaSnapshot proposta = store.saveProposta(new AcordoPropostaSnapshot(
                null,
                sessao.id(),
                command.autorId(),
                tipo,
                command.valor(),
                termosJson,
                command.validadeAte(),
                command.criadaPorIa() ? AcordoPropostaStatus.AGUARDANDO_REVISAO_HUMANA : AcordoPropostaStatus.PENDENTE,
                command.criadaPorIa(),
                false,
                null,
                null,
                now
        ));
        store.saveSessao(sessao.withStatus(novoStatus));
        audit(sessao.id(), command.autorId(), evento, metadata(command.metadata()), details(
                "propostaId", proposta.id(),
                "tipo", tipo.name(),
                "valorInformado", command.valor() != null,
                "validadeAte", command.validadeAte().toString(),
                "criadaPorIa", command.criadaPorIa()
        ));
        return proposta;
    }

    private AcordoProcessualWindowInput toWindowInput(ProcessoAcordoContexto contexto, AbrirSalaCommand command) {
        return new AcordoProcessualWindowInput(
                contexto.faseProcessual(),
                contexto.segredoJustica(),
                contexto.antesContestacao(),
                contexto.antesAudienciaConciliacaoMediacao(),
                contexto.aposContestacao(),
                contexto.propostaFormalExistente() || command.propostaFormalExistente(),
                contexto.aposPericiaOuLaudo(),
                contexto.antesSentenca(),
                contexto.faseRecursal(),
                contexto.cumprimentoSentencaOuExecucao(),
                contexto.mutiraoConciliacao() || command.mutiraoConciliacao(),
                contexto.requerimentoParte() || command.requerimentoParte(),
                contexto.determinacaoJudicial() || command.determinacaoJudicial(),
                contexto.cejuscReferenciado() || command.cejuscReferenciado(),
                command.parteSemAdvogado(),
                contexto.potencialAcordoScore()
        );
    }

    private AcordoSessaoSnapshot requireSessaoForUpdate(Long sessaoId) {
        return store.findSessaoForUpdate(requireId(sessaoId, "sessaoId"))
                .orElseThrow(() -> new AcordoApplicationException("Sala de acordo nao encontrada."));
    }

    private AcordoPropostaSnapshot requirePropostaForUpdate(Long propostaId) {
        return store.findPropostaForUpdate(requireId(propostaId, "propostaId"))
                .orElseThrow(() -> new AcordoApplicationException("Proposta de acordo nao encontrada."));
    }

    private AcordoTermoSnapshot requireTermoForUpdate(Long termoId) {
        return store.findTermoForUpdate(requireId(termoId, "termoId"))
                .orElseThrow(() -> new AcordoApplicationException("Termo de acordo nao encontrado."));
    }

    private AcordoParticipanteSnapshot requireParticipante(Long sessaoId, Long usuarioId) {
        return store.findParticipante(sessaoId, usuarioId)
                .orElseThrow(() -> new AcordoApplicationException("Usuario nao participa da sala de acordo."));
    }

    private void requireGestorOuParticipanteAceito(AcordoSessaoSnapshot sessao, Long usuarioId) {
        if (Objects.equals(sessao.abertaPorId(), usuarioId)) {
            return;
        }
        AcordoParticipanteSnapshot participante = requireParticipante(sessao.id(), usuarioId);
        stateMachine.requireInteracaoParticipanteAceito(participante.aceito());
        if (!participante.papel().gestorDaSala()) {
            throw new AcordoApplicationException("Convite exige gestor da sala ou servidor autorizado.");
        }
    }

    private void requireSalaNaoExpirada(AcordoSessaoSnapshot sessao, Instant now) {
        if (sessao.status().terminal()) {
            throw new AcordoApplicationException("Sala em estado terminal nao aceita operacao.");
        }
        if (sessao.expiradaEm(now)) {
            throw new AcordoApplicationException("Sala expirada nao aceita operacao.");
        }
    }

    private void requireUsuarioExistente(Long usuarioId) {
        if (!usuarioPort.existeUsuario(usuarioId)) {
            throw new AcordoApplicationException("Usuario nao encontrado.");
        }
    }

    private Long requireId(Long id, String field) {
        if (id == null || id <= 0) {
            throw new AcordoApplicationException(field + " invalido.");
        }
        return id;
    }

    private Instant normalizarExpiracao(Instant requested, Instant now) {
        Instant value = requested != null ? requested : now.plus(DEFAULT_EXPIRACAO);
        if (!value.isAfter(now.plus(Duration.ofMinutes(5)))) {
            throw new AcordoApplicationException("Expiracao da sala deve ser futura.");
        }
        if (value.isAfter(now.plus(MAX_EXPIRACAO))) {
            throw new AcordoApplicationException("Sala de acordo nao pode ficar aberta por mais de 60 dias.");
        }
        return value;
    }

    private String requireText(String value, String field, int max) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isBlank()) {
            throw new AcordoApplicationException(field + " obrigatorio.");
        }
        if (normalized.length() > max) {
            throw new AcordoApplicationException(field + " excede tamanho maximo.");
        }
        return normalized;
    }

    private String requireJson(String value, String field) {
        String normalized = requireText(value, field, 20000);
        if (!normalized.startsWith("{") && !normalized.startsWith("[")) {
            throw new AcordoApplicationException(field + " deve ser JSON estruturado.");
        }
        return normalized;
    }

    private String nonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String limit(String value, int max) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.length() <= max ? normalized : normalized.substring(0, max);
    }

    private AcordoOperationMetadata metadata(AcordoOperationMetadata metadata) {
        return metadata != null ? metadata : AcordoOperationMetadata.empty();
    }

    private void audit(Long sessaoId,
                       Long usuarioId,
                       AcordoAuditoriaEvento evento,
                       AcordoOperationMetadata metadata,
                       Map<String, Object> detalhes) {
        auditoriaPort.registrarEvento(new AcordoAuditEntry(
                sessaoId,
                usuarioId,
                evento,
                detalhes,
                metadata.ipHash(),
                metadata.userAgentHash(),
                Instant.now(clock)
        ));
    }

    private Map<String, Object> details(Object... items) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        for (int i = 0; i + 1 < items.length; i += 2) {
            Object key = items[i];
            if (key != null) {
                out.put(String.valueOf(key), items[i + 1]);
            }
        }
        return out;
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hashed.length * 2);
            for (byte item : hashed) {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("algoritmo SHA-256 indisponivel", ex);
        }
    }

    public record AbrirSalaCommand(
            Long processoId,
            Long abertaPorId,
            AcordoPapelParticipante papelAbertura,
            Instant expiraEm,
            String motivoAbertura,
            boolean propostaFormalExistente,
            boolean mutiraoConciliacao,
            boolean requerimentoParte,
            boolean determinacaoJudicial,
            boolean cejuscReferenciado,
            boolean parteSemAdvogado,
            AcordoOperationMetadata metadata
    ) {
    }

    public record ConvidarParticipanteCommand(
            Long sessaoId,
            Long convidanteId,
            Long convidadoId,
            AcordoPapelParticipante papel,
            AcordoOperationMetadata metadata
    ) {
    }

    public record ParticipacaoCommand(
            Long sessaoId,
            Long usuarioId,
            AcordoOperationMetadata metadata
    ) {
    }

    public record RecusaParticipacaoCommand(
            Long sessaoId,
            Long usuarioId,
            String motivo,
            AcordoOperationMetadata metadata
    ) {
    }

    public record RegistrarMensagemCommand(
            Long sessaoId,
            Long autorId,
            AcordoMensagemTipo tipo,
            String conteudo,
            boolean confidencial,
            AcordoMensagemVisibilidade visibilidade,
            AcordoOperationMetadata metadata
    ) {
    }

    public record RegistrarPropostaCommand(
            Long sessaoId,
            Long autorId,
            BigDecimal valor,
            String termosJson,
            Instant validadeAte,
            boolean criadaPorIa,
            AcordoOperationMetadata metadata
    ) {
    }

    public record MarcarRevisaoHumanaCommand(
            Long propostaId,
            Long revisorId,
            AcordoOperationMetadata metadata
    ) {
    }

    public record GerarMinutaTermoCommand(
            Long propostaId,
            Long usuarioId,
            String conteudoTermo,
            AcordoOperationMetadata metadata
    ) {
    }

    public record AssinarTermoCommand(
            Long termoId,
            Long usuarioId,
            String hashAssinatura,
            AcordoOperationMetadata metadata
    ) {
    }

    public record EnviarHomologacaoCommand(
            Long termoId,
            Long usuarioId,
            AcordoOperationMetadata metadata
    ) {
    }

    public record HomologarCommand(
            Long sessaoId,
            Long magistradoId,
            String descricaoMovimentacao,
            AcordoOperationMetadata metadata
    ) {
    }

    public record RejeitarHomologacaoCommand(
            Long sessaoId,
            Long magistradoId,
            String motivo,
            AcordoOperationMetadata metadata
    ) {
    }

    public record EncerrarSemAcordoCommand(
            Long sessaoId,
            Long usuarioId,
            String motivo,
            AcordoOperationMetadata metadata
    ) {
    }
}
