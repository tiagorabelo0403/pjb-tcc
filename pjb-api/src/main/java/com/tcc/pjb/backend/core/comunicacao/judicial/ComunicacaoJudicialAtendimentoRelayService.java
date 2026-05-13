package com.tcc.pjb.backend.core.comunicacao.judicial;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.comunicacao.judicial.state.ComunicacaoJudicialStateStore;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.dto.ui.UiToken;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.modules.advocacia.entity.util.CriptografiaPJB;
import com.tcc.pjb.backend.modules.advocacia.repository.ClienteRepository;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoMessage;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoMessageReceipt;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoMessageStatus;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoThread;
import com.tcc.pjb.backend.modules.atendimento.model.AtendimentoThreadStatus;
import com.tcc.pjb.backend.modules.atendimento.repository.AtendimentoMessageReceiptRepository;
import com.tcc.pjb.backend.modules.atendimento.repository.AtendimentoMessageRepository;
import com.tcc.pjb.backend.modules.atendimento.repository.AtendimentoThreadRepository;
import com.tcc.pjb.backend.modules.laiane.model.LaianeProcuracaoStatus;
import com.tcc.pjb.backend.modules.laiane.repository.LaianeProcuracaoRepository;
import com.tcc.pjb.backend.service.ui.UiHistoryService;

@Service
public class ComunicacaoJudicialAtendimentoRelayService {

    private static final String DOMAIN = "COM_JUD_ATENDIMENTO_RELAY";
    private static final String SYSTEM_SENDER_TYPE = "PJB_SISTEMA";
    private static final long SYSTEM_SENDER_ID = 0L;

    public record RelaySnapshot(Long threadId,
                                Long advogadoId,
                                Long cidadaoId,
                                String processoNumero,
                                String evento,
                                String expedicaoUuid,
                                String hashMensagem,
                                Instant emitidoEm) {
    }

    private final AtendimentoThreadRepository threadRepository;
    private final AtendimentoMessageRepository messageRepository;
    private final AtendimentoMessageReceiptRepository receiptRepository;
    private final UsuarioRepository usuarioRepository;
    private final LaianeProcuracaoRepository procuracaoRepository;
    private final ClienteRepository clienteRepository;
    private final UiHistoryService uiHistoryService;
    private final ComunicacaoJudicialStateStore stateStore;
    private final ComunicacaoJudicialMensagemInteligenteService mensagemInteligenteService;
    private final ComunicacaoJudicialMensagemPrazoVivoService mensagemPrazoVivoService;
    private final Clock clock;

    public ComunicacaoJudicialAtendimentoRelayService(AtendimentoThreadRepository threadRepository,
                                                      AtendimentoMessageRepository messageRepository,
                                                      AtendimentoMessageReceiptRepository receiptRepository,
                                                      UsuarioRepository usuarioRepository,
                                                      LaianeProcuracaoRepository procuracaoRepository,
                                                      ClienteRepository clienteRepository,
                                                      UiHistoryService uiHistoryService,
                                                      ComunicacaoJudicialStateStore stateStore,
                                                      ComunicacaoJudicialMensagemInteligenteService mensagemInteligenteService,
                                                      ComunicacaoJudicialMensagemPrazoVivoService mensagemPrazoVivoService,
                                                      Clock clock) {
        this.threadRepository = Objects.requireNonNull(threadRepository, "threadRepository");
        this.messageRepository = Objects.requireNonNull(messageRepository, "messageRepository");
        this.receiptRepository = Objects.requireNonNull(receiptRepository, "receiptRepository");
        this.usuarioRepository = Objects.requireNonNull(usuarioRepository, "usuarioRepository");
        this.procuracaoRepository = Objects.requireNonNull(procuracaoRepository, "procuracaoRepository");
        this.clienteRepository = Objects.requireNonNull(clienteRepository, "clienteRepository");
        this.uiHistoryService = Objects.requireNonNull(uiHistoryService, "uiHistoryService");
        this.stateStore = Objects.requireNonNull(stateStore, "stateStore");
        this.mensagemInteligenteService = Objects.requireNonNull(mensagemInteligenteService, "mensagemInteligenteService");
        this.mensagemPrazoVivoService = Objects.requireNonNull(mensagemPrazoVivoService, "mensagemPrazoVivoService");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Transactional
    public void propagarAviso(ExpedicaoJudicial expedicao,
                              Processo processo,
                              ComunicacaoJudicialPortalNotificationService.EventoPortal evento) {
        if (expedicao == null || processo == null || evento == null || processo.getId() == null) {
            return;
        }
        String documento = normalizarDocumento(expedicao.getDestinatarioDocumento());
        if (documento == null || documento.length() != 11) {
            return;
        }
        Usuario cidadao = resolverCidadao(documento, processo);
        if (cidadao == null || cidadao.getId() == null || cidadao.getTipoUsuario() != TipoUsuario.CIDADAO || !cidadao.isAtivoESemanticoValido()) {
            return;
        }
        for (Usuario advogado : resolverAdvogadosRepresentantes(processo.getId(), documento)) {
            AtendimentoThread thread = threadRepository.findByProcessoIdAndAdvogadoIdAndCidadaoUsuarioId(processo.getId(), advogado.getId(), cidadao.getId())
                    .orElseGet(() -> criarThread(processo.getId(), advogado.getId(), cidadao, documento));
            if (thread.getId() == null) {
                continue;
            }
            String stateKey = evento.name() + ':' + expedicao.getExpedicaoUuid() + ':' + advogado.getId() + ':' + cidadao.getId();
            if (stateStore.exists(DOMAIN, stateKey)) {
                continue;
            }
            AtendimentoMessage message = registrarMensagem(thread, processo, expedicao, evento);
            stateStore.save(
                    DOMAIN,
                    stateKey,
                    String.valueOf(thread.getId()),
                    new RelaySnapshot(
                            thread.getId(),
                            advogado.getId(),
                            cidadao.getId(),
                            processo.getNumeroUnificado(),
                            evento.name(),
                            expedicao.getExpedicaoUuid(),
                            message.getMsgHash(),
                            message.getCreatedAt()
                    ),
                    processo.getId(),
                    expedicao.getExpedicaoUuid(),
                    null,
                    evento.name()
            );
            registrarInbox(thread, processo, advogado, cidadao, expedicao, evento);
        }
    }


    @Transactional
    public void propagarAlertaPrazo(ExpedicaoJudicial expedicao,
                                    Processo processo,
                                    PrazoRespostaPosEntregaEngine.PrazoResposta prazo,
                                    ComunicacaoJudicialMensagemPrazoVivoService.MarcoPrazo marco) {
        if (expedicao == null || processo == null || prazo == null || marco == null || processo.getId() == null) {
            return;
        }
        String documento = normalizarDocumento(expedicao.getDestinatarioDocumento());
        if (documento == null || documento.length() != 11) {
            return;
        }
        Usuario cidadao = resolverCidadao(documento, processo);
        if (cidadao == null || cidadao.getId() == null || cidadao.getTipoUsuario() != TipoUsuario.CIDADAO || !cidadao.isAtivoESemanticoValido()) {
            return;
        }
        for (Usuario advogado : resolverAdvogadosRepresentantes(processo.getId(), documento)) {
            AtendimentoThread thread = threadRepository.findByProcessoIdAndAdvogadoIdAndCidadaoUsuarioId(processo.getId(), advogado.getId(), cidadao.getId())
                    .orElseGet(() -> criarThread(processo.getId(), advogado.getId(), cidadao, documento));
            if (thread.getId() == null) {
                continue;
            }
            String stateKey = "PRAZO:" + marco.name() + ':' + expedicao.getExpedicaoUuid() + ':' + advogado.getId() + ':' + cidadao.getId();
            if (stateStore.exists(DOMAIN, stateKey)) {
                continue;
            }
            ComunicacaoJudicialMensagemPrazoVivoService.MensagemPrazo mensagem = mensagemPrazoVivoService.construir(expedicao, processo, prazo, marco, true);
            AtendimentoMessage message = registrarMensagemPrazo(thread, mensagem.corpo());
            stateStore.save(
                    DOMAIN,
                    stateKey,
                    String.valueOf(thread.getId()),
                    new RelaySnapshot(
                            thread.getId(),
                            advogado.getId(),
                            cidadao.getId(),
                            processo.getNumeroUnificado(),
                            marco.name(),
                            expedicao.getExpedicaoUuid(),
                            message.getMsgHash(),
                            message.getCreatedAt()
                    ),
                    processo.getId(),
                    expedicao.getExpedicaoUuid(),
                    null,
                    marco.name()
            );
            registrarInboxPrazo(thread, processo, advogado, cidadao, mensagem.resumo());
        }
    }
    private Usuario resolverCidadao(String documento, Processo processo) {
        Usuario vinculado = processo.getUsuario();
        if (vinculado != null && vinculado.getId() != null && vinculado.getTipoUsuario() == TipoUsuario.CIDADAO && Objects.equals(documento, normalizarDocumento(vinculado.getCpf()))) {
            return vinculado;
        }
        return usuarioRepository.findByCpf(documento)
                .filter(usuario -> usuario.getTipoUsuario() == TipoUsuario.CIDADAO)
                .orElse(null);
    }

    private List<Usuario> resolverAdvogadosRepresentantes(Long processoId, String documento) {
        if (processoId == null || documento == null || documento.isBlank()) {
            return List.of();
        }
        String docHash = CriptografiaPJB.hashCpfCnpj(documento);
        if (docHash == null) {
            return List.of();
        }
        Map<Long, Usuario> out = new LinkedHashMap<>();
        procuracaoRepository.findByProcessoIdAndStatusOrderByCreatedAtAsc(processoId, LaianeProcuracaoStatus.ATIVA)
                .stream()
                .map(p -> p.getAdvogado())
                .filter(Objects::nonNull)
                .filter(adv -> adv.getId() != null && adv.getTipoUsuario() == TipoUsuario.ADVOGADO && adv.isAtivoESemanticoValido())
                .filter(adv -> clienteRepository.existsByCpfHashAndAdvogado_Id(docHash, adv.getId()))
                .forEach(adv -> out.putIfAbsent(adv.getId(), adv));
        return List.copyOf(out.values());
    }

    private AtendimentoThread criarThread(Long processoId, Long advogadoId, Usuario cidadao, String documento) {
        Instant now = Instant.now(clock);
        AtendimentoThread thread = AtendimentoThread.builder()
                .processoId(processoId)
                .advogadoId(advogadoId)
                .cidadaoUsuarioId(cidadao.getId())
                .cidadaoCpfHash(CriptografiaPJB.hashCpfCnpj(documento))
                .status(AtendimentoThreadStatus.ATIVO)
                .createdAt(now)
                .updatedAt(now)
                .lastMessageId(null)
                .version(0L)
                .build();
        return threadRepository.save(thread);
    }

    private AtendimentoMessage registrarMensagem(AtendimentoThread thread,
                                                 Processo processo,
                                                 ExpedicaoJudicial expedicao,
                                                 ComunicacaoJudicialPortalNotificationService.EventoPortal evento) {
        Instant now = Instant.now(clock);
        String prevHash = messageRepository.findTopByThreadIdOrderByIdDesc(thread.getId()).map(AtendimentoMessage::getMsgHash).orElse(null);
        ComunicacaoJudicialMensagemInteligenteService.MensagemChat mensagem = mensagemInteligenteService.construirChat(expedicao, processo, evento, true);
        String body = mensagem.corpo();
        String msgHash = Hashes.sha256Hex((prevHash == null ? "" : prevHash)
                + '|'
                + thread.getId()
                + '|'
                + SYSTEM_SENDER_ID
                + '|'
                + SYSTEM_SENDER_TYPE
                + '|'
                + now.toEpochMilli()
                + '|'
                + body);
        AtendimentoMessage message = messageRepository.save(AtendimentoMessage.builder()
                .threadId(thread.getId())
                .senderUsuarioId(SYSTEM_SENDER_ID)
                .senderTipo(SYSTEM_SENDER_TYPE)
                .body(body)
                .status(AtendimentoMessageStatus.DELIVERED)
                .prevHash(prevHash)
                .msgHash(msgHash)
                .createdAt(now)
                .build());
        thread.setUpdatedAt(now);
        thread.setLastMessageId(message.getId());
        threadRepository.save(thread);
        garantirRecibo(message, thread.getAdvogadoId(), now);
        garantirRecibo(message, thread.getCidadaoUsuarioId(), now);
        return message;
    }


    private AtendimentoMessage registrarMensagemPrazo(AtendimentoThread thread, String body) {
        Instant now = Instant.now(clock);
        String prevHash = messageRepository.findTopByThreadIdOrderByIdDesc(thread.getId()).map(AtendimentoMessage::getMsgHash).orElse(null);
        String msgHash = Hashes.sha256Hex((prevHash == null ? "" : prevHash)
                + '|'
                + thread.getId()
                + '|'
                + SYSTEM_SENDER_ID
                + '|'
                + SYSTEM_SENDER_TYPE
                + '|'
                + now.toEpochMilli()
                + '|'
                + body);
        AtendimentoMessage message = messageRepository.save(AtendimentoMessage.builder()
                .threadId(thread.getId())
                .senderUsuarioId(SYSTEM_SENDER_ID)
                .senderTipo(SYSTEM_SENDER_TYPE)
                .body(body)
                .status(AtendimentoMessageStatus.DELIVERED)
                .prevHash(prevHash)
                .msgHash(msgHash)
                .createdAt(now)
                .build());
        thread.setUpdatedAt(now);
        thread.setLastMessageId(message.getId());
        threadRepository.save(thread);
        garantirRecibo(message, thread.getAdvogadoId(), now);
        garantirRecibo(message, thread.getCidadaoUsuarioId(), now);
        return message;
    }

    private void registrarInboxPrazo(AtendimentoThread thread,
                                     Processo processo,
                                     Usuario advogado,
                                     Usuario cidadao,
                                     String message) {
        EnumSet<UiToken> tokens = EnumSet.of(UiToken.NOTIFICADO, UiToken.CITACAO_INTIMACAO, UiToken.URGENTE);
        uiHistoryService.recordInboxEvent(
                "USR:" + advogado.getId(),
                processo.getId(),
                UiHistoryService.EVT_ATENDIMENTO_NEW_MESSAGE,
                tokens,
                null,
                SYSTEM_SENDER_TYPE,
                message
        );
        uiHistoryService.recordInboxEvent(
                "USR:" + cidadao.getId(),
                processo.getId(),
                UiHistoryService.EVT_ATENDIMENTO_NEW_MESSAGE,
                tokens,
                null,
                SYSTEM_SENDER_TYPE,
                message
        );
        if (cidadao.getCpf() != null && !cidadao.getCpf().isBlank()) {
            uiHistoryService.recordInboxEvent(
                    "CIDCPF:" + normalizarDocumento(cidadao.getCpf()),
                    processo.getId(),
                    UiHistoryService.EVT_ATENDIMENTO_NEW_MESSAGE,
                    tokens,
                    null,
                    SYSTEM_SENDER_TYPE,
                    message
            );
        }
    }
    private void garantirRecibo(AtendimentoMessage message, Long usuarioId, Instant at) {
        if (message == null || message.getId() == null || usuarioId == null) {
            return;
        }
        receiptRepository.save(AtendimentoMessageReceipt.builder()
                .messageId(message.getId())
                .threadId(message.getThreadId())
                .usuarioId(usuarioId)
                .createdAt(at)
                .updatedAt(at)
                .build());
    }

    private void registrarInbox(AtendimentoThread thread,
                                Processo processo,
                                Usuario advogado,
                                Usuario cidadao,
                                ExpedicaoJudicial expedicao,
                                ComunicacaoJudicialPortalNotificationService.EventoPortal evento) {
        EnumSet<UiToken> tokens = EnumSet.of(UiToken.NOTIFICADO, UiToken.INFO);
        if (expedicao.getTipoComunicacao() != null && expedicao.getTipoComunicacao().isCitacao()) {
            tokens.add(UiToken.URGENTE);
        }
        String message = mensagemInteligenteService.construirChat(expedicao, processo, evento, true).resumoCurto();
        uiHistoryService.recordInboxEvent(
                "USR:" + advogado.getId(),
                processo.getId(),
                UiHistoryService.EVT_ATENDIMENTO_NEW_MESSAGE,
                tokens,
                null,
                SYSTEM_SENDER_TYPE,
                message
        );
        uiHistoryService.recordInboxEvent(
                "USR:" + cidadao.getId(),
                processo.getId(),
                UiHistoryService.EVT_ATENDIMENTO_NEW_MESSAGE,
                tokens,
                null,
                SYSTEM_SENDER_TYPE,
                message
        );
        if (cidadao.getCpf() != null && !cidadao.getCpf().isBlank()) {
            uiHistoryService.recordInboxEvent(
                    "CIDCPF:" + normalizarDocumento(cidadao.getCpf()),
                    processo.getId(),
                    UiHistoryService.EVT_ATENDIMENTO_NEW_MESSAGE,
                    tokens,
                    null,
                    SYSTEM_SENDER_TYPE,
                    message
            );
        }
    }

    private static String normalizarDocumento(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String digits = value.replaceAll("\\D", "");
        return digits.isBlank() ? null : digits;
    }
}
