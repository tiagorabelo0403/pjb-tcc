package com.tcc.pjb.backend.core.comunicacao.judicial;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Objects;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import com.tcc.pjb.backend.platform.cluster.PjbClusterSingletonTask;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.comunicacao.judicial.state.ComunicacaoJudicialStateStore;
import com.tcc.pjb.backend.model.dto.ui.UiToken;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.notification.NotificationService;
import com.tcc.pjb.backend.service.ui.UiHistoryService;

@Service
public class ComunicacaoJudicialPrazoReminderService {

    private static final String DOMAIN = "COM_JUD_PRAZO_REMINDER";

    private final PrazoRespostaPosEntregaEngine prazoEngine;
    private final ExpedicaoJudicialRepository expedicaoRepository;
    private final ProcessoRepository processoRepository;
    private final ComunicacaoJudicialAudienceResolver audienceResolver;
    private final ComunicacaoJudicialMensagemPrazoVivoService mensagemPrazoVivoService;
    private final NotificationService notificationService;
    private final UiHistoryService uiHistoryService;
    private final ComunicacaoJudicialCalendarioProcessualService calendarioProcessualService;
    private final ComunicacaoJudicialAtendimentoRelayService atendimentoRelayService;
    private final ComunicacaoJudicialStateStore stateStore;

    public ComunicacaoJudicialPrazoReminderService(PrazoRespostaPosEntregaEngine prazoEngine,
                                                   ExpedicaoJudicialRepository expedicaoRepository,
                                                   ProcessoRepository processoRepository,
                                                   ComunicacaoJudicialAudienceResolver audienceResolver,
                                                   ComunicacaoJudicialMensagemPrazoVivoService mensagemPrazoVivoService,
                                                   NotificationService notificationService,
                                                   UiHistoryService uiHistoryService,
                                                   ComunicacaoJudicialCalendarioProcessualService calendarioProcessualService,
                                                   ComunicacaoJudicialAtendimentoRelayService atendimentoRelayService,
                                                   ComunicacaoJudicialStateStore stateStore) {
        this.prazoEngine = Objects.requireNonNull(prazoEngine, "prazoEngine");
        this.expedicaoRepository = Objects.requireNonNull(expedicaoRepository, "expedicaoRepository");
        this.processoRepository = Objects.requireNonNull(processoRepository, "processoRepository");
        this.audienceResolver = Objects.requireNonNull(audienceResolver, "audienceResolver");
        this.mensagemPrazoVivoService = Objects.requireNonNull(mensagemPrazoVivoService, "mensagemPrazoVivoService");
        this.notificationService = Objects.requireNonNull(notificationService, "notificationService");
        this.uiHistoryService = Objects.requireNonNull(uiHistoryService, "uiHistoryService");
        this.calendarioProcessualService = Objects.requireNonNull(calendarioProcessualService, "calendarioProcessualService");
        this.atendimentoRelayService = Objects.requireNonNull(atendimentoRelayService, "atendimentoRelayService");
        this.stateStore = Objects.requireNonNull(stateStore, "stateStore");
    }

    @PjbClusterSingletonTask(key = "comjud-prazo-reminder", ttl = "PT2M")
    @Scheduled(fixedDelay = 300_000)
    @Transactional
    public void monitorarPrazosCriticos() {
        LocalDate hoje = LocalDate.now();
        for (PrazoRespostaPosEntregaEngine.PrazoResposta prazo : prazoEngine.listarPrazos()) {
            if (prazo == null || prazo.vencimentoEm() == null) {
                continue;
            }
            if (prazo.status() != PrazoRespostaPosEntregaEngine.StatusPrazoResposta.PRAZO_INICIADO
                    && prazo.status() != PrazoRespostaPosEntregaEngine.StatusPrazoResposta.PRAZO_RETOMADO
                    && prazo.status() != PrazoRespostaPosEntregaEngine.StatusPrazoResposta.PRAZO_VENCIDO) {
                continue;
            }
            ComunicacaoJudicialMensagemPrazoVivoService.MarcoPrazo marco = resolverMarco(hoje, prazo);
            if (marco == null) {
                continue;
            }
            ExpedicaoJudicial expedicao = expedicaoRepository.findByExpedicaoUuid(prazo.expedicaoUuid()).orElse(null);
            if (expedicao == null) {
                continue;
            }
            Processo processo = processoRepository.findProcessoCompletoById(prazo.processoId()).orElse(null);
            if (processo == null) {
                continue;
            }
            for (ComunicacaoJudicialAudienceResolver.AudienceTarget target : audienceResolver.resolver(expedicao, processo)) {
                Usuario usuario = target.usuario();
                if (usuario == null || usuario.getId() == null || !usuario.isAtivoESemanticoValido()) {
                    continue;
                }
                String stateKey = prazo.expedicaoUuid() + ':' + usuario.getId() + ':' + marco.name();
                if (stateStore.exists(DOMAIN, stateKey)) {
                    continue;
                }
                ComunicacaoJudicialMensagemPrazoVivoService.MensagemPrazo mensagem = mensagemPrazoVivoService.construir(expedicao, processo, prazo, marco, target.representante());
                notificationService.notifyUser(usuario, processo, mensagem.titulo(), mensagem.corpo(), detailsUrl(processo, target.representante()));
                registrarInbox(usuario, processo, expedicao, mensagem, marco);
                calendarioProcessualService.sincronizarLembrete(usuario, processo, expedicao, prazo, target.representante(), mensagem, marco);
                stateStore.save(DOMAIN, stateKey, prazo.prazoUuid(), mensagem, processo.getId(), expedicao.getExpedicaoUuid(), null, marco.name());
            }
            atendimentoRelayService.propagarAlertaPrazo(expedicao, processo, prazo, marco);
        }
    }

    private ComunicacaoJudicialMensagemPrazoVivoService.MarcoPrazo resolverMarco(LocalDate hoje,
                                                                                  PrazoRespostaPosEntregaEngine.PrazoResposta prazo) {
        long dias = java.time.temporal.ChronoUnit.DAYS.between(hoje, prazo.vencimentoEm());
        if (dias == 3) {
            return ComunicacaoJudicialMensagemPrazoVivoService.MarcoPrazo.FALTAM_TRES_DIAS;
        }
        if (dias == 1) {
            return ComunicacaoJudicialMensagemPrazoVivoService.MarcoPrazo.FALTA_UM_DIA;
        }
        if (dias == 0) {
            return prazo.status() == PrazoRespostaPosEntregaEngine.StatusPrazoResposta.PRAZO_VENCIDO
                    ? ComunicacaoJudicialMensagemPrazoVivoService.MarcoPrazo.VENCIDO
                    : ComunicacaoJudicialMensagemPrazoVivoService.MarcoPrazo.VENCE_HOJE;
        }
        if (dias < 0) {
            return ComunicacaoJudicialMensagemPrazoVivoService.MarcoPrazo.VENCIDO;
        }
        return null;
    }

    private void registrarInbox(Usuario usuario,
                                Processo processo,
                                ExpedicaoJudicial expedicao,
                                ComunicacaoJudicialMensagemPrazoVivoService.MensagemPrazo mensagem,
                                ComunicacaoJudicialMensagemPrazoVivoService.MarcoPrazo marco) {
        EnumSet<UiToken> tokens = EnumSet.of(UiToken.NOTIFICADO, UiToken.CITACAO_INTIMACAO);
        if (expedicao.getTipoComunicacao() != null && expedicao.getTipoComunicacao().isCitacao()) {
            tokens.add(UiToken.URGENTE);
        }
        if (marco == ComunicacaoJudicialMensagemPrazoVivoService.MarcoPrazo.FALTA_UM_DIA
                || marco == ComunicacaoJudicialMensagemPrazoVivoService.MarcoPrazo.VENCE_HOJE
                || marco == ComunicacaoJudicialMensagemPrazoVivoService.MarcoPrazo.VENCIDO) {
            tokens.add(UiToken.ATRASADO);
        } else {
            tokens.add(UiToken.INFO);
        }
        String actorRole = usuario.getTipoUsuario() != null ? usuario.getTipoUsuario().name() : "USUARIO";
        uiHistoryService.recordInboxEvent(
                "USR:" + usuario.getId(),
                processo.getId(),
                "COMUNICACAO_JUDICIAL_PRAZO_" + marco.name(),
                tokens,
                usuario.getId(),
                actorRole,
                mensagem.resumo()
        );
        if (usuario.getCpf() != null && !usuario.getCpf().isBlank()) {
            uiHistoryService.recordInboxEvent(
                    "CIDCPF:" + usuario.getCpf().replaceAll("\\D", ""),
                    processo.getId(),
                    "COMUNICACAO_JUDICIAL_PRAZO_" + marco.name(),
                    tokens,
                    usuario.getId(),
                    actorRole,
                    mensagem.resumo()
            );
        }
    }

    private String detailsUrl(Processo processo, boolean representante) {
        return "/api/v1/comunicacoes/judiciais/minhas?processoId=" + processo.getId() + (representante ? "&scope=representado" : "");
    }
}
