package com.tcc.pjb.backend.core.comunicacao.judicial;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.tcc.pjb.backend.model.entity.Processo;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CitacaoExpedicaoNotificacaoServiceTest {

    private final CitacaoWebhookNotifierService webhook = mock(CitacaoWebhookNotifierService.class);
    private final CitacaoPortalRelayNotificationService portal = mock(CitacaoPortalRelayNotificationService.class);
    private final CitacaoExpedicaoNotificacaoService service = new CitacaoExpedicaoNotificacaoService(webhook, portal);

    private ExpedicaoJudicial expedicao() {
        return new ExpedicaoJudicial(80L, "PROC-80", TipoComunicacaoJudicial.CITACAO_INICIAL,
                ModalidadeExpedicaoJudicial.DIGITAL_GOVBR_PUSH, ExpedicaoJudicial.TipoDestinatario.PESSOA_FISICA,
                "Fulano", "12345678900", "CIVIL", "PRIMEIRO_GRAU", null, "hash-anterior", "fundamento");
    }

    private Processo processo() {
        Processo processo = new Processo();
        processo.setId(80L);
        return processo;
    }

    @Test
    void expedidaNotificaPortalEWebhookComFundamento() {
        ExpedicaoJudicial exp = expedicao();
        Processo proc = processo();

        service.notificarExpedida(exp, proc);

        verify(portal).notificarPortalDestinatario(exp, proc, ComunicacaoJudicialPortalNotificationService.EventoPortal.EXPEDIDA);
        verify(portal).propagarAvisoAtendimento(exp, proc, ComunicacaoJudicialPortalNotificationService.EventoPortal.EXPEDIDA);
        verify(webhook).publicar(exp, WebhookOutboundService.EventoWebhook.EXPEDICAO_EXPEDIDA, Map.of("fundamento", "fundamento"));
    }

    @Test
    void entregaConfirmadaPropagaOCanalRecebido() {
        ExpedicaoJudicial exp = expedicao();
        Processo proc = processo();

        service.notificarEntregaConfirmada(exp, proc, "AR_DIGITAL");

        verify(portal).notificarPortalDestinatario(exp, proc, ComunicacaoJudicialPortalNotificationService.EventoPortal.ENTREGUE_CONFIRMADA);
        verify(portal).propagarAvisoAtendimento(exp, proc, ComunicacaoJudicialPortalNotificationService.EventoPortal.ENTREGUE_CONFIRMADA);
        verify(webhook).publicar(exp, WebhookOutboundService.EventoWebhook.EXPEDICAO_ENTREGUE_CONFIRMADA, Map.of("canal", "AR_DIGITAL"));
    }

    @Test
    void leituraConfirmadaPropagaOAcuseHash() {
        ExpedicaoJudicial exp = expedicao();
        Processo proc = processo();

        service.notificarLeituraConfirmada(exp, proc, "acuse-123");

        verify(portal).notificarPortalDestinatario(exp, proc, ComunicacaoJudicialPortalNotificationService.EventoPortal.LIDA_CONFIRMADA);
        verify(portal).propagarAvisoAtendimento(exp, proc, ComunicacaoJudicialPortalNotificationService.EventoPortal.LIDA_CONFIRMADA);
        verify(webhook).publicar(exp, WebhookOutboundService.EventoWebhook.EXPEDICAO_LIDA_CONFIRMADA, Map.of("acuseHash", "acuse-123"));
    }

    @Test
    void presumidaEntregueNotificaSemPayload() {
        ExpedicaoJudicial exp = expedicao();
        Processo proc = processo();

        service.notificarPresumidaEntregue(exp, proc);

        verify(portal).notificarPortalDestinatario(exp, proc, ComunicacaoJudicialPortalNotificationService.EventoPortal.PRESUMIDA_ENTREGUE);
        verify(portal).propagarAvisoAtendimento(exp, proc, ComunicacaoJudicialPortalNotificationService.EventoPortal.PRESUMIDA_ENTREGUE);
        verify(webhook).publicar(exp, WebhookOutboundService.EventoWebhook.EXPEDICAO_PRESUMIDA_ENTREGUE, Map.of());
    }

    @Test
    void frustracaoNotificaSomenteWebhookComMotivoEFallback() {
        ExpedicaoJudicial exp = expedicao();

        service.notificarFrustracao(exp, "SEM_CANAL_ATIVO", "EDITAL");

        verify(webhook).publicar(exp, WebhookOutboundService.EventoWebhook.EXPEDICAO_FRUSTRADA,
                Map.of("motivo", "SEM_CANAL_ATIVO", "fallback", "EDITAL"));
        verifyNoInteractions(portal);
    }

    @Test
    void editalPublicadoNotificaSomentePortal() {
        ExpedicaoJudicial exp = expedicao();
        Processo proc = processo();

        service.notificarEditalPublicado(exp, proc);

        verify(portal).notificarPortalDestinatario(exp, proc, ComunicacaoJudicialPortalNotificationService.EventoPortal.PUBLICADA_EDITAL);
        verify(portal).propagarAvisoAtendimento(exp, proc, ComunicacaoJudicialPortalNotificationService.EventoPortal.PUBLICADA_EDITAL);
        verifyNoInteractions(webhook);
    }
}
