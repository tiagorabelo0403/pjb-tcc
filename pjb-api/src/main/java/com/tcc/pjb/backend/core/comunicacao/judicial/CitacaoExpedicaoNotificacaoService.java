package com.tcc.pjb.backend.core.comunicacao.judicial;

import com.tcc.pjb.backend.model.entity.Processo;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class CitacaoExpedicaoNotificacaoService {

    private final CitacaoWebhookNotifierService webhook;
    private final CitacaoPortalRelayNotificationService portal;

    public CitacaoExpedicaoNotificacaoService(CitacaoWebhookNotifierService webhook,
                                              CitacaoPortalRelayNotificationService portal) {
        this.webhook = Objects.requireNonNull(webhook, "webhook");
        this.portal = Objects.requireNonNull(portal, "portal");
    }

    public void notificarExpedida(ExpedicaoJudicial expedicao, Processo processo) {
        portal.notificarPortalDestinatario(expedicao, processo, ComunicacaoJudicialPortalNotificationService.EventoPortal.EXPEDIDA);
        portal.propagarAvisoAtendimento(expedicao, processo, ComunicacaoJudicialPortalNotificationService.EventoPortal.EXPEDIDA);
        webhook.publicar(expedicao, WebhookOutboundService.EventoWebhook.EXPEDICAO_EXPEDIDA, Map.of("fundamento", expedicao.getFundamentacaoLegal()));
    }

    public void notificarEntregaConfirmada(ExpedicaoJudicial expedicao, Processo processo, String canal) {
        portal.notificarPortalDestinatario(expedicao, processo, ComunicacaoJudicialPortalNotificationService.EventoPortal.ENTREGUE_CONFIRMADA);
        portal.propagarAvisoAtendimento(expedicao, processo, ComunicacaoJudicialPortalNotificationService.EventoPortal.ENTREGUE_CONFIRMADA);
        webhook.publicar(expedicao, WebhookOutboundService.EventoWebhook.EXPEDICAO_ENTREGUE_CONFIRMADA, Map.of("canal", canal));
    }

    public void notificarLeituraConfirmada(ExpedicaoJudicial expedicao, Processo processo, String acuseHash) {
        portal.notificarPortalDestinatario(expedicao, processo, ComunicacaoJudicialPortalNotificationService.EventoPortal.LIDA_CONFIRMADA);
        portal.propagarAvisoAtendimento(expedicao, processo, ComunicacaoJudicialPortalNotificationService.EventoPortal.LIDA_CONFIRMADA);
        webhook.publicar(expedicao, WebhookOutboundService.EventoWebhook.EXPEDICAO_LIDA_CONFIRMADA, Map.of("acuseHash", String.valueOf(acuseHash)));
    }

    public void notificarPresumidaEntregue(ExpedicaoJudicial expedicao, Processo processo) {
        portal.notificarPortalDestinatario(expedicao, processo, ComunicacaoJudicialPortalNotificationService.EventoPortal.PRESUMIDA_ENTREGUE);
        portal.propagarAvisoAtendimento(expedicao, processo, ComunicacaoJudicialPortalNotificationService.EventoPortal.PRESUMIDA_ENTREGUE);
        webhook.publicar(expedicao, WebhookOutboundService.EventoWebhook.EXPEDICAO_PRESUMIDA_ENTREGUE, Map.of());
    }

    public void notificarFrustracao(ExpedicaoJudicial expedicao, String motivo, String fallback) {
        webhook.publicar(expedicao, WebhookOutboundService.EventoWebhook.EXPEDICAO_FRUSTRADA, Map.of("motivo", motivo, "fallback", fallback));
    }

    public void notificarEditalPublicado(ExpedicaoJudicial expedicao, Processo processo) {
        portal.notificarPortalDestinatario(expedicao, processo, ComunicacaoJudicialPortalNotificationService.EventoPortal.PUBLICADA_EDITAL);
        portal.propagarAvisoAtendimento(expedicao, processo, ComunicacaoJudicialPortalNotificationService.EventoPortal.PUBLICADA_EDITAL);
    }
}
