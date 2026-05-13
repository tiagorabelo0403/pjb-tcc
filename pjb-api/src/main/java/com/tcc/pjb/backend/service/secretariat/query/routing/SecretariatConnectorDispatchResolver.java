package com.tcc.pjb.backend.service.secretariat.query.routing;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.core.forum.routing.ForumDeskPortfolioProfile;

@Component
public class SecretariatConnectorDispatchResolver {

    public SecretariatConnectorDispatchProfile resolve(String targetSystem,
                                                       String title,
                                                       Collection<String> tags,
                                                       ForumDeskPortfolioProfile portfolio,
                                                       SecretariatFlowBridgeProfile bridgeProfile) {
        String normalizedTarget = targetSystem == null || targetSystem.isBlank() ? "PJB_INTERNAL" : targetSystem.trim().toUpperCase(Locale.ROOT);
        String source = ((title == null ? "" : title) + ' ' + (tags == null ? "" : String.join(" ", tags))).toUpperCase(Locale.ROOT);
        boolean urgent = containsAny(source, "URGENTE", "LIMINAR", "HC", "UTI", "MEDICAMENTO", "PLANTAO")
                || bridgeProfile != null && "RECURSAL".equals(bridgeProfile.downstreamAxis());
        boolean sensitive = containsAny(source, "SIGILO", "SEGREDO", "PROTETIVA", "INFANCIA", "VIOLENCIA")
                || bridgeProfile != null && bridgeProfile.requiresGabineteSync();

        String connectorId = normalizedTarget + (normalizedTarget.equals("PJB_INTERNAL") ? "_LOCAL" : "_CONNECTOR");
        String ackChannel = normalizedTarget.equals("PJB_INTERNAL") ? "ACK_INTERNO"
                : sensitive ? "ACK_ASSINADO_RESTRITO"
                : "ACK_TRIBUNAL_ASSINADO";
        String replayDesk = sensitive ? "REPLAY_SIGILOSO_" + normalizedTarget
                : urgent ? "REPLAY_PRIORITARIO_" + normalizedTarget
                : firstNonBlank(portfolio == null ? null : portfolio.escalationDesk(), "REPLAY_PADRAO_" + normalizedTarget);
        String retryMode = sensitive ? "RETRY_MANUAL_CONTROLADO"
                : urgent ? "RETRY_CONFIRMADO_CURTO"
                : "RETRY_EXPONENCIAL";
        String evidencePolicy = sensitive ? "HASH_MINIMO_EVIDENCIA"
                : normalizedTarget.equals("PJB_INTERNAL") ? "LOG_INTERNO_ASSINADO"
                : "RECIBO_EVIDENCIA_COMPLETA";
        String dispatchWindow = urgent ? "JANELA_IMEDIATA" : sensitive ? "JANELA_CONTROLADA" : "JANELA_OPERACIONAL";

        LinkedHashSet<String> labels = new LinkedHashSet<>();
        labels.add(connectorId);
        labels.add(ackChannel);
        labels.add(retryMode);
        labels.add(evidencePolicy);
        labels.add(dispatchWindow);
        if (urgent) {
            labels.add("URGENT");
        }
        if (sensitive) {
            labels.add("SENSITIVE");
        }

        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("targetSystem", normalizedTarget);
        metadata.put("urgent", urgent);
        metadata.put("sensitive", sensitive);
        metadata.put("descriptor", connectorId + ':' + ackChannel + ':' + dispatchWindow);

        return new SecretariatConnectorDispatchProfile(
                connectorId,
                ackChannel,
                replayDesk,
                retryMode,
                evidencePolicy,
                dispatchWindow,
                List.copyOf(labels),
                metadata
        );
    }

    private static boolean containsAny(String source, String... tokens) {
        if (source == null || source.isBlank() || tokens == null) {
            return false;
        }
        for (String token : tokens) {
            if (token != null && !token.isBlank() && source.contains(token.trim().toUpperCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
