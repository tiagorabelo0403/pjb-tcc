package com.tcc.pjb.backend.core.comunicacao.judicial;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class CitacaoWebhookNotifierService {

    private final ObjectProvider<WebhookOutboundService> webhookProvider;

    public CitacaoWebhookNotifierService(ObjectProvider<WebhookOutboundService> webhookProvider) {
        this.webhookProvider = webhookProvider;
    }

    public void publicar(ExpedicaoJudicial expedicao, WebhookOutboundService.EventoWebhook evento, Map<?, ?> metadados) {
        WebhookOutboundService webhook = webhookProvider.getIfAvailable();
        if (webhook == null || expedicao == null || evento == null) {
            return;
        }
        LinkedHashMap<String, String> payload = new LinkedHashMap<>();
        if (metadados != null) {
            metadados.forEach((k, v) -> {
                if (k != null && v != null) payload.put(String.valueOf(k), String.valueOf(v));
            });
        }
        webhook.publicarEventoExpedicao(expedicao, evento, payload);
    }
}
