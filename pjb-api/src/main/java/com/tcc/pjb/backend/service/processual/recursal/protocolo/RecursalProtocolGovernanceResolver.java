package com.tcc.pjb.backend.service.processual.recursal.protocolo;

import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalPlanningResult;
import com.tcc.pjb.backend.service.processual.recursal.admissibilidade.RecursalAdmissibilityProfile;
import com.tcc.pjb.backend.service.processual.recursal.admissibilidade.RecursalAdmissibilityService;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class RecursalProtocolGovernanceResolver {

    public RecursalProtocolGovernanceProfile resolve(RecursalAdmissibilityService.RecursalAdmissibilityCommand command,
                                                     RecursalPlanningResult planning,
                                                     RecursalAdmissibilityProfile admissibilityProfile,
                                                     String destinationCourt) {
        String target = destinationCourt == null || destinationCourt.isBlank() ? "DEFAULT" : destinationCourt.trim().toUpperCase(Locale.ROOT);
        boolean urgent = command.pedidoEfeitoSuspensivo() || command.tutelaUrgenciaRecursal();
        boolean secret = command.segredoJustica();
        boolean superior = planning != null && planning.routePlan() != null
                && planning.routePlan().instanciaDestino() != null
                && switch (planning.routePlan().instanciaDestino()) {
                    case SUPERIOR, EXTRAORDINARY -> true;
                    default -> false;
                };

        String ackDesk = urgent ? "ACK_RECURSAL_PRIORITARIO_" + target : "ACK_RECURSAL_" + target;
        String receiptChannel = superior ? "RECIBO_SUPERIOR_ASSINADO" : secret ? "RECIBO_SIGILOSO_CONTROLADO" : "RECIBO_PROTOCOLO_PADRAO";
        String retryMode = secret ? "RETRY_CONTROLADO_MANUAL" : urgent ? "RETRY_RAPIDO_CONFIRMADO" : "RETRY_EXPONENCIAL";
        String evidencePolicy = secret ? "TRILHA_MINIMIZADA_HASHADA"
                : superior ? "TRILHA_COMPLETA_COM_RECIBO"
                : "TRILHA_PADRAO_ASSINADA";
        String complianceDesk = secret ? "COMPLIANCE_SIGILO_RECURSAL_" + target
                : superior ? "COMPLIANCE_RECURSAL_SUPERIOR_" + target
                : firstNonBlank(admissibilityProfile.reviewDesk(), "COMPLIANCE_RECURSAL_" + target);
        String protocolWindow = urgent ? "JANELA_IMEDIATA"
                : command.priorizaIdosoOuSaude() ? "JANELA_PRIORITARIA"
                : "JANELA_REGULAR";

        LinkedHashSet<String> labels = new LinkedHashSet<>();
        labels.add(target);
        labels.add(receiptChannel);
        labels.add(retryMode);
        labels.add(evidencePolicy);
        labels.add(protocolWindow);
        if (urgent) {
            labels.add("URGENT");
        }
        if (secret) {
            labels.add("SECRET");
        }
        if (superior) {
            labels.add("SUPERIOR");
        }

        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("targetCourt", target);
        metadata.put("urgent", urgent);
        metadata.put("secret", secret);
        metadata.put("superior", superior);
        metadata.put("descriptor", ackDesk + ':' + receiptChannel + ':' + protocolWindow);

        return new RecursalProtocolGovernanceProfile(
                ackDesk,
                receiptChannel,
                retryMode,
                evidencePolicy,
                complianceDesk,
                protocolWindow,
                List.copyOf(labels),
                metadata
        );
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
