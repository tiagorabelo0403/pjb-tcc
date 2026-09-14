package com.tcc.pjb.backend.core.comunicacao.judicial;

import com.tcc.pjb.backend.model.entity.Processo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class CitacaoPortalRelayNotificationService {

    private static final Logger log = LoggerFactory.getLogger(CitacaoPortalRelayNotificationService.class);

    private final ObjectProvider<ComunicacaoJudicialPortalNotificationService> portalNotificationProvider;
    private final ObjectProvider<ComunicacaoJudicialAtendimentoRelayService> atendimentoRelayProvider;

    public CitacaoPortalRelayNotificationService(ObjectProvider<ComunicacaoJudicialPortalNotificationService> portalNotificationProvider,
                                                 ObjectProvider<ComunicacaoJudicialAtendimentoRelayService> atendimentoRelayProvider) {
        this.portalNotificationProvider = portalNotificationProvider;
        this.atendimentoRelayProvider = atendimentoRelayProvider;
    }

    public void notificarPortalDestinatario(ExpedicaoJudicial expedicao,
                                           Processo processo,
                                           ComunicacaoJudicialPortalNotificationService.EventoPortal evento) {
        ComunicacaoJudicialPortalNotificationService service = portalNotificationProvider.getIfAvailable();
        if (service == null) {
            return;
        }
        try {
            service.notificar(expedicao, processo, evento);
        } catch (Exception ex) {
            log.warn("[CitacaoEngine] Falha ao notificar portal destinatário uuid={}: {}", expedicao.getExpedicaoUuid(), ex.getMessage());
        }
    }

    public void propagarAvisoAtendimento(ExpedicaoJudicial expedicao,
                                        Processo processo,
                                        ComunicacaoJudicialPortalNotificationService.EventoPortal evento) {
        ComunicacaoJudicialAtendimentoRelayService service = atendimentoRelayProvider.getIfAvailable();
        if (service == null) {
            return;
        }
        try {
            service.propagarAviso(expedicao, processo, evento);
        } catch (Exception ex) {
            log.warn("[CitacaoEngine] Falha ao propagar aviso de atendimento uuid={}: {}", expedicao.getExpedicaoUuid(), ex.getMessage());
        }
    }
}
