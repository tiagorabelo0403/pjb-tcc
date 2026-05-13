package com.tcc.pjb.backend.integration.judicial.routing;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class JudicialConnectorSystemResolver {

    public JudicialConnectorSystemProfile resolve(String connectorSystem,
                                                  String tribunalCodigo,
                                                  String competenceHint,
                                                  boolean segredo,
                                                  boolean urgente,
                                                  boolean recursal) {
        String system = normalize(connectorSystem);
        String tribunal = normalize(tribunalCodigo);
        String competence = normalize(competenceHint);

        String protocolNamespace;
        String receiptPattern;
        String replayStrategy;
        String evidenceStore;
        String contingencySystem;
        String manualChannel;
        String publicationBridge;
        boolean sessionAware;

        switch (system) {
            case "PJE", "PJE_RECURSAL_BRIDGE", "PJB_DISTRIBUICAO_BRIDGE" -> {
                protocolNamespace = "PDPJ_PROTOCOLO";
                receiptPattern = segredo ? "ACK_EVENTO_MINIMIZADO_PJE" : "ACK_EVENTO_PJE";
                replayStrategy = urgente ? "REPLAY_PRIORITARIO_IDEMPOTENTE" : "REPLAY_EVENT_SNAPSHOT";
                evidenceStore = recursal ? "DOSSIE_PROTOCOLO_RECURSAL_PJE" : "DOSSIE_PROTOCOLO_PJE";
                contingencySystem = "PDPJ_ASSISTIDO";
                manualChannel = "BALCAO_PROTOCOLO_PJE";
                publicationBridge = "DJE_PJE_BRIDGE";
                sessionAware = true;
            }
            case "EPROC" -> {
                protocolNamespace = "EPROC_GATEWAY";
                receiptPattern = "ACK_TICKET_EPROC";
                replayStrategy = urgente ? "REPLAY_COM_TICKET_PRIORITARIO" : "REPLAY_POR_TICKET";
                evidenceStore = "DOSSIE_EPROC_ASSINADO";
                contingencySystem = "EPROC_ASSISTIDO";
                manualChannel = "CENTRAL_PROTOCOLO_EPROC";
                publicationBridge = "DJE_EPROC_BRIDGE";
                sessionAware = true;
            }
            case "ESAJ" -> {
                protocolNamespace = "ESAJ_PROTOCOLO_DIGITAL";
                receiptPattern = "ACK_COMPROVANTE_ESAJ";
                replayStrategy = "REPLAY_COM_COMPROVANTE";
                evidenceStore = "DOSSIE_COMPROVANTE_ESAJ";
                contingencySystem = "ESAJ_CONTINGENCIA_OPERACIONAL";
                manualChannel = "PORTAL_PETICIONAMENTO_ESAJ";
                publicationBridge = "DJE_ESAJ_BRIDGE";
                sessionAware = true;
            }
            case "PROJUDI" -> {
                protocolNamespace = "PROJUDI_PROTOCOLO";
                receiptPattern = "ACK_EVENTO_PROJUDI";
                replayStrategy = "REPLAY_COM_HASH_PROJUDI";
                evidenceStore = "DOSSIE_HASH_PROJUDI";
                contingencySystem = "PROJUDI_ASSISTIDO";
                manualChannel = "PROTOCOLO_PROJUDI_WEB";
                publicationBridge = "DJE_PROJUDI_BRIDGE";
                sessionAware = false;
            }
            case "CRETA" -> {
                protocolNamespace = "CRETA_PROTOCOLO";
                receiptPattern = "ACK_COMPROVANTE_CRETA";
                replayStrategy = "REPLAY_COM_COMPROVANTE_CRETA";
                evidenceStore = "DOSSIE_COMPROVANTE_CRETA";
                contingencySystem = "CRETA_CONTINGENCIA_ASSISTIDA";
                manualChannel = "PROTOCOLO_CRETA_ASSISTIDO";
                publicationBridge = "DJE_CRETA_BRIDGE";
                sessionAware = false;
            }
            case "PJB_INTERNAL", "OUTRO", "" -> {
                protocolNamespace = "PJB_INTERNAL";
                receiptPattern = "ACK_INTERNO";
                replayStrategy = urgente ? "REPLAY_PRIORITARIO_INTERNO" : "REPLAY_INTERNO";
                evidenceStore = recursal ? "DOSSIE_RECURSAL_INTERNO" : "DOSSIE_INTERNO";
                contingencySystem = "PJB_CONTINGENCIA_INTERNA";
                manualChannel = "MESA_PROTOCOLO_PJB";
                publicationBridge = "DJE_PJB";
                sessionAware = competence.contains("SUPERIOR") || tribunal.startsWith("ST") || tribunal.startsWith("TR");
            }
            default -> {
                protocolNamespace = system + "_PROTOCOLO";
                receiptPattern = "ACK_GENERICO";
                replayStrategy = "REPLAY_CONTROLADO";
                evidenceStore = "DOSSIE_AUDITAVEL_GENERICO";
                contingencySystem = system + "_CONTINGENCIA";
                manualChannel = "PROTOCOLO_ASSISTIDO_" + system;
                publicationBridge = "DJE_" + system;
                sessionAware = competence.contains("SUPERIOR") || tribunal.startsWith("TR") || tribunal.startsWith("ST");
            }
        }

        LinkedHashSet<String> labels = new LinkedHashSet<>();
        labels.add(firstNonBlank(system, "OUTRO"));
        labels.add(protocolNamespace);
        labels.add(replayStrategy);
        labels.add(evidenceStore);
        if (sessionAware) {
            labels.add("SESSION_AWARE");
        }
        if (segredo) {
            labels.add("SIGILO_AUDITAVEL");
        }
        if (urgente) {
            labels.add("PROTOCOLO_PRIORITARIO");
        }
        if (recursal) {
            labels.add("RECURSAL_ENABLED");
        }

        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("tribunalCodigo", tribunal);
        metadata.put("competenceHint", competence);
        metadata.put("descriptor", firstNonBlank(system, "OUTRO") + ':' + protocolNamespace + ':' + receiptPattern);
        metadata.put("supportsSessionPublishing", sessionAware);

        return new JudicialConnectorSystemProfile(
                firstNonBlank(system, "OUTRO"),
                protocolNamespace,
                receiptPattern,
                replayStrategy,
                evidenceStore,
                contingencySystem,
                manualChannel,
                publicationBridge,
                sessionAware,
                List.copyOf(labels),
                metadata
        );
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
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
