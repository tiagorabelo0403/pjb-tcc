package com.tcc.pjb.backend.core.comunicacao.judicial;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.calendar.UserCalendarSystemEvent;
import com.tcc.pjb.backend.model.repository.calendar.UserCalendarSystemEventRepository;

@Service
public class ComunicacaoJudicialCalendarioProcessualService {

    private static final String EVENT_TYPE = "COMUNICACAO_JUDICIAL";
    private static final ZoneId ZONE = ZoneId.of("America/Fortaleza");

    private final UserCalendarSystemEventRepository repository;
    private final ComunicacaoJudicialAudienceResolver audienceResolver;
    private final ComunicacaoJudicialMensagemPrazoVivoService mensagemPrazoVivoService;
    private final ObjectProvider<PrazoRespostaPosEntregaEngine> prazoRespostaProvider;

    public ComunicacaoJudicialCalendarioProcessualService(UserCalendarSystemEventRepository repository,
                                                          ComunicacaoJudicialAudienceResolver audienceResolver,
                                                          ComunicacaoJudicialMensagemPrazoVivoService mensagemPrazoVivoService,
                                                          ObjectProvider<PrazoRespostaPosEntregaEngine> prazoRespostaProvider) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.audienceResolver = Objects.requireNonNull(audienceResolver, "audienceResolver");
        this.mensagemPrazoVivoService = Objects.requireNonNull(mensagemPrazoVivoService, "mensagemPrazoVivoService");
        this.prazoRespostaProvider = Objects.requireNonNull(prazoRespostaProvider, "prazoRespostaProvider");
    }

    @Transactional
    public void sincronizarCiencia(ExpedicaoJudicial expedicao,
                                   Processo processo,
                                   ComunicacaoJudicialPortalNotificationService.EventoPortal evento) {
        if (expedicao == null) {
            return;
        }
        PrazoRespostaPosEntregaEngine prazoEngine = prazoRespostaProvider.getIfAvailable();
        PrazoRespostaPosEntregaEngine.PrazoResposta prazo = prazoEngine == null ? null : prazoEngine.consultarPorExpedicao(expedicao.getExpedicaoUuid()).orElse(null);
        if (prazo == null && evento != ComunicacaoJudicialPortalNotificationService.EventoPortal.EXPEDIDA) {
            return;
        }
        for (ComunicacaoJudicialAudienceResolver.AudienceTarget target : audienceResolver.resolver(expedicao, processo)) {
            if (target.usuario() == null || target.usuario().getId() == null) {
                continue;
            }
            if (prazo == null) {
                upsert(target.usuario(), processo, expedicao, domainKey(expedicao, target.representante(), "CIENCIA"),
                        titleCiencia(expedicao, processo, target.representante()),
                        bodyCiencia(expedicao, processo, target.representante()),
                        LocalDateTime.ofInstant(expedicao.getExpedidaEm() != null ? expedicao.getExpedidaEm() : Instant.now(), ZONE),
                        colorCiencia(expedicao),
                        detailsUrl(expedicao, processo, target.representante()));
                continue;
            }
            ComunicacaoJudicialMensagemPrazoVivoService.MensagemPrazo mensagem = mensagemPrazoVivoService.construir(
                    expedicao,
                    processo,
                    prazo,
                    ComunicacaoJudicialMensagemPrazoVivoService.MarcoPrazo.ABERTO,
                    target.representante()
            );
            upsert(target.usuario(), processo, expedicao, domainKey(expedicao, target.representante(), "PRAZO"),
                    mensagem.titulo(),
                    mensagem.corpo(),
                    LocalDateTime.of(prazo.vencimentoEm(), LocalTime.of(10, 0)),
                    mensagem.cor(),
                    detailsUrl(expedicao, processo, target.representante()));
        }
    }

    @Transactional
    public void sincronizarLembrete(Usuario usuario,
                                    Processo processo,
                                    ExpedicaoJudicial expedicao,
                                    PrazoRespostaPosEntregaEngine.PrazoResposta prazo,
                                    boolean representante,
                                    ComunicacaoJudicialMensagemPrazoVivoService.MensagemPrazo mensagem,
                                    ComunicacaoJudicialMensagemPrazoVivoService.MarcoPrazo marco) {
        if (usuario == null || usuario.getId() == null || processo == null || expedicao == null || prazo == null || mensagem == null || marco == null) {
            return;
        }
        upsert(usuario, processo, expedicao, domainKey(expedicao, representante, "PRAZO"),
                mensagem.titulo(),
                mensagem.corpo(),
                LocalDateTime.of(prazo.vencimentoEm(), LocalTime.of(10, 0)),
                mensagem.cor(),
                detailsUrl(expedicao, processo, representante));
        upsert(usuario, processo, expedicao, domainKey(expedicao, representante, "LEMBRETE:" + marco.name()),
                mensagem.titulo(),
                mensagem.corpo(),
                LocalDateTime.of(LocalDateTime.now(ZONE).toLocalDate(), LocalTime.of(9, 0)),
                mensagem.cor(),
                detailsUrl(expedicao, processo, representante));
    }

    private void upsert(Usuario usuario,
                        Processo processo,
                        ExpedicaoJudicial expedicao,
                        String domainKey,
                        String title,
                        String body,
                        LocalDateTime at,
                        String color,
                        String detailsUrl) {
        Instant now = Instant.now();
        UserCalendarSystemEvent event = repository.findByUsuarioIdAndDomainKey(usuario.getId(), domainKey).orElseGet(UserCalendarSystemEvent::new);
        event.setUsuarioId(usuario.getId());
        event.setProcessoId(processo != null ? processo.getId() : expedicao.getProcessoId());
        event.setDomainKey(domainKey);
        event.setEventType(EVENT_TYPE);
        event.setTitle(normalizeTitle(title));
        event.setBody(body == null ? null : body.trim());
        event.setAt(at);
        event.setColor(normalizeColor(color));
        event.setDetailsUrl(detailsUrl);
        if (event.getCreatedAt() == null) {
            event.setCreatedAt(now);
        }
        event.setUpdatedAt(now);
        repository.save(event);
    }

    private String titleCiencia(ExpedicaoJudicial expedicao, Processo processo, boolean representante) {
        String numero = processo != null && processo.getNumeroUnificado() != null && !processo.getNumeroUnificado().isBlank()
                ? processo.getNumeroUnificado()
                : expedicao.getNumeroUnificado();
        String tipo = expedicao.getTipoComunicacao() != null && expedicao.getTipoComunicacao().isCitacao() ? "citação" : "intimação";
        if (numero == null || numero.isBlank()) {
            numero = String.valueOf(expedicao.getProcessoId());
        }
        return representante
                ? "Nova " + tipo + " do cliente no processo " + numero
                : "Nova " + tipo + " no processo " + numero;
    }

    private String bodyCiencia(ExpedicaoJudicial expedicao, Processo processo, boolean representante) {
        String tipo = expedicao.getTipoComunicacao() != null && expedicao.getTipoComunicacao().isCitacao() ? "citação" : "intimação";
        String numero = processo != null && processo.getNumeroUnificado() != null && !processo.getNumeroUnificado().isBlank()
                ? processo.getNumeroUnificado()
                : expedicao.getNumeroUnificado();
        if (numero == null || numero.isBlank()) {
            numero = String.valueOf(expedicao.getProcessoId());
        }
        return representante
                ? "O PJB registrou nova " + tipo + " para cliente representado. O prazo será lançado automaticamente no calendário após a ciência válida e a consolidação do marco inicial."
                : "O PJB registrou nova " + tipo + " no processo " + numero + ". O prazo será lançado automaticamente no calendário após a ciência válida e a consolidação do marco inicial.";
    }

    private String colorCiencia(ExpedicaoJudicial expedicao) {
        return expedicao.getTipoComunicacao() != null && expedicao.getTipoComunicacao().isCitacao() ? "AMBER" : "BLUE";
    }

    private String detailsUrl(ExpedicaoJudicial expedicao, Processo processo, boolean representante) {
        Long processoId = processo != null ? processo.getId() : expedicao.getProcessoId();
        return "/api/v1/comunicacoes/judiciais/minhas?processoId=" + (processoId == null ? "" : processoId) + (representante ? "&scope=representado" : "");
    }

    private String domainKey(ExpedicaoJudicial expedicao, boolean representante, String suffix) {
        return EVENT_TYPE + ':' + expedicao.getExpedicaoUuid() + ':' + (representante ? "REP" : "DIR") + ':' + suffix;
    }

    private String normalizeTitle(String title) {
        if (title == null || title.isBlank()) {
            return "Agenda processual PJB";
        }
        String normalized = title.trim();
        return normalized.length() > 180 ? normalized.substring(0, 180) : normalized;
    }

    private String normalizeColor(String color) {
        if (color == null || color.isBlank()) {
            return "BLUE";
        }
        String normalized = color.trim().toUpperCase();
        return normalized.length() > 16 ? normalized.substring(0, 16) : normalized;
    }
}
