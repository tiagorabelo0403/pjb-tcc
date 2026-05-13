package com.tcc.pjb.backend.core.comunicacao.judicial;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.model.dto.ui.UiToken;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.NotificationHistoryRepository;
import com.tcc.pjb.backend.modules.advocacia.entity.util.CriptografiaPJB;
import com.tcc.pjb.backend.modules.advocacia.repository.ClienteRepository;
import com.tcc.pjb.backend.modules.laiane.model.LaianeProcuracaoStatus;
import com.tcc.pjb.backend.modules.laiane.repository.LaianeProcuracaoRepository;
import com.tcc.pjb.backend.service.cidadao.dashboard.CidadaoDashboardSnapshotWriteService;
import com.tcc.pjb.backend.service.notification.NotificationService;
import com.tcc.pjb.backend.service.ui.UiHistoryService;

@Service
public class ComunicacaoJudicialPortalNotificationService {

    public enum EventoPortal {
        EXPEDIDA,
        ENTREGUE_CONFIRMADA,
        LIDA_CONFIRMADA,
        PRESUMIDA_ENTREGUE,
        PUBLICADA_EDITAL
    }

    private final NotificationService notificationService;
    private final NotificationHistoryRepository notificationHistoryRepository;
    private final UiHistoryService uiHistoryService;
    private final CidadaoDashboardSnapshotWriteService cidadaoDashboardSnapshotWriteService;
    private final ComunicacaoJudicialAudienceResolver audienceResolver;
    private final LaianeProcuracaoRepository procuracaoRepository;
    private final ClienteRepository clienteRepository;
    private final ComunicacaoJudicialMensagemInteligenteService mensagemInteligenteService;
    private final ComunicacaoJudicialCalendarioProcessualService calendarioProcessualService;

    public ComunicacaoJudicialPortalNotificationService(NotificationService notificationService,
                                                        NotificationHistoryRepository notificationHistoryRepository,
                                                        UiHistoryService uiHistoryService,
                                                        CidadaoDashboardSnapshotWriteService cidadaoDashboardSnapshotWriteService,
                                                        ComunicacaoJudicialAudienceResolver audienceResolver,
                                                        LaianeProcuracaoRepository procuracaoRepository,
                                                        ClienteRepository clienteRepository,
                                                        ComunicacaoJudicialMensagemInteligenteService mensagemInteligenteService,
                                                        ComunicacaoJudicialCalendarioProcessualService calendarioProcessualService) {
        this.notificationService = Objects.requireNonNull(notificationService, "notificationService");
        this.notificationHistoryRepository = Objects.requireNonNull(notificationHistoryRepository, "notificationHistoryRepository");
        this.uiHistoryService = Objects.requireNonNull(uiHistoryService, "uiHistoryService");
        this.cidadaoDashboardSnapshotWriteService = Objects.requireNonNull(cidadaoDashboardSnapshotWriteService, "cidadaoDashboardSnapshotWriteService");
        this.audienceResolver = Objects.requireNonNull(audienceResolver, "audienceResolver");
        this.procuracaoRepository = Objects.requireNonNull(procuracaoRepository, "procuracaoRepository");
        this.clienteRepository = Objects.requireNonNull(clienteRepository, "clienteRepository");
        this.mensagemInteligenteService = Objects.requireNonNull(mensagemInteligenteService, "mensagemInteligenteService");
        this.calendarioProcessualService = Objects.requireNonNull(calendarioProcessualService, "calendarioProcessualService");
    }

    public void notificar(ExpedicaoJudicial expedicao, Processo processo, EventoPortal evento) {
        Objects.requireNonNull(expedicao, "expedicao");
        Objects.requireNonNull(evento, "evento");
        List<ComunicacaoJudicialAudienceResolver.AudienceTarget> destinatarios = audienceResolver.resolver(expedicao, processo);
        if (destinatarios.isEmpty()) {
            return;
        }
        calendarioProcessualService.sincronizarCiencia(expedicao, processo, evento);
        for (ComunicacaoJudicialAudienceResolver.AudienceTarget contexto : destinatarios) {
            Usuario usuario = contexto.usuario();
            if (usuario == null || usuario.getId() == null || !usuario.isAtivoESemanticoValido()) {
                continue;
            }
            boolean representante = contexto.representante() || possuiRepresentacaoOuAtendimentoAtivo(usuario, processo, expedicao);
            ComunicacaoJudicialMensagemInteligenteService.MensagemPortal mensagem = mensagemInteligenteService.construirPortal(expedicao, processo, evento, representante);
            if (duplicada(usuario, processo, mensagem.titulo())) {
                continue;
            }
            notificationService.notifyUser(usuario, processo, mensagem.titulo(), mensagem.corpo(), deepLink(expedicao, processo, representante));
            registrarInbox(usuario, expedicao, evento, mensagem.titulo(), representante);
            if (!representante && usuario.getCpf() != null && !usuario.getCpf().isBlank()) {
                cidadaoDashboardSnapshotWriteService.refreshForCpf(normalizarDocumento(usuario.getCpf()));
            }
        }
    }


    private boolean possuiRepresentacaoOuAtendimentoAtivo(Usuario usuario, Processo processo, ExpedicaoJudicial expedicao) {
        if (usuario == null || usuario.getId() == null || processo == null || processo.getId() == null) {
            return false;
        }
        if (!usuario.isAdvogado()) {
            return false;
        }
        boolean possuiProcuracaoAtiva = procuracaoRepository
                .findByProcessoIdAndStatusOrderByCreatedAtAsc(processo.getId(), LaianeProcuracaoStatus.ATIVA)
                .stream()
                .map(p -> p.getAdvogado())
                .filter(Objects::nonNull)
                .anyMatch(advogado -> Objects.equals(advogado.getId(), usuario.getId()));
        if (possuiProcuracaoAtiva) {
            return true;
        }
        String documento = normalizarDocumento(expedicao.getDestinatarioDocumento());
        if (documento == null) {
            return false;
        }
        String docHash = CriptografiaPJB.hashCpfCnpj(documento);
        return clienteRepository.existsByCpfHashAndAdvogado_Id(docHash, usuario.getId());
    }

    private boolean duplicada(Usuario usuario, Processo processo, String titulo) {
        return notificationHistoryRepository.existsByUsuarioIdAndProcessoIdAndTituloAndStatusAndEnviadoEmAfter(
                usuario.getId(),
                processo != null ? processo.getId() : null,
                titulo,
                "ENVIADO",
                LocalDateTime.now().minusHours(6)
        );
    }

    private void registrarInbox(Usuario usuario,
                                ExpedicaoJudicial expedicao,
                                EventoPortal evento,
                                String titulo,
                                boolean representante) {
        Set<UiToken> base = new LinkedHashSet<>();
        base.add(UiToken.NOTIFICADO);
        base.add(UiToken.INFO);
        if (expedicao.getTipoComunicacao() != null && expedicao.getTipoComunicacao().isCitacao()) {
            base.add(UiToken.URGENTE);
        }
        if (evento == EventoPortal.PRESUMIDA_ENTREGUE || evento == EventoPortal.PUBLICADA_EDITAL) {
            base.add(UiToken.ATRASADO);
        }
        if (representante) {
            base.add(UiToken.PENDENTE);
        }
        java.util.EnumSet<UiToken> tokens = java.util.EnumSet.copyOf(base);
        String actorRole = usuario.getTipoUsuario() != null ? usuario.getTipoUsuario().name() : "USUARIO";
        uiHistoryService.recordInboxEvent(
                "notificacao:" + usuario.getId(),
                expedicao.getProcessoId(),
                evento.name(),
                tokens,
                usuario.getId(),
                actorRole,
                titulo
        );
        uiHistoryService.recordInboxEvent(
                "USR:" + usuario.getId(),
                expedicao.getProcessoId(),
                evento.name(),
                tokens,
                usuario.getId(),
                actorRole,
                titulo
        );
        if (!representante && usuario.getTipoUsuario() == TipoUsuario.CIDADAO && usuario.getCpf() != null && !usuario.getCpf().isBlank()) {
            uiHistoryService.recordInboxEvent(
                    "CIDCPF:" + normalizarDocumento(usuario.getCpf()),
                    expedicao.getProcessoId(),
                    evento.name(),
                    tokens,
                    usuario.getId(),
                    actorRole,
                    titulo
            );
        }
    }

    private String deepLink(ExpedicaoJudicial expedicao, Processo processo, boolean representante) {
        Long processoId = processo != null ? processo.getId() : expedicao.getProcessoId();
        return "/api/v1/comunicacoes/judiciais/minhas?processoId=" + (processoId == null ? "" : processoId) + (representante ? "&scope=representado" : "");
    }

    private static String normalizeNullable(String value) {
        return value == null ? null : value.trim();
    }

    private static String normalizarDocumento(String value) {
        String trimmed = normalizeNullable(value);
        if (trimmed == null || trimmed.isBlank()) {
            return null;
        }
        String digits = trimmed.replaceAll("\\D", "");
        return digits.isBlank() ? null : digits;
    }
}
