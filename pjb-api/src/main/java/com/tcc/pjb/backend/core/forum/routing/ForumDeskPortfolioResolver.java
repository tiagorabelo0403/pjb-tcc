package com.tcc.pjb.backend.core.forum.routing;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ForumDeskPortfolioResolver {

    public ForumDeskPortfolioProfile resolve(ForumDeskKey key) {
        String laneToken = key.lane().token();
        String instance = key.instance().name();
        String organ = key.organ().code();
        String triageDesk = "TRIAGEM_" + organ + '_' + instance + '_' + laneToken;
        String gabineteDesk = key.isSecondInstance() ? "GABINETE_" + organ + '_' + laneToken : "SECRETARIA_UNIDADE_" + organ + '_' + laneToken;
        String hearingDesk = key.lane().requiresAudienceDesk() ? "AUDIENCIA_" + organ + '_' + laneToken : "AUDIENCIA_NAO_PRIORITARIA_" + laneToken;
        String complianceDesk = key.lane().isSpecialized() ? "CUMPRIMENTO_ESPECIALIZADO_" + laneToken : "CUMPRIMENTO_PADRAO_" + laneToken;
        String escalationDesk = key.isSecondInstance() ? "ESCALACAO_RELATOR_" + organ : "ESCALACAO_DIRETORIA_" + organ;
        String assistantDesk = resolveAssistantDesk(key, organ, laneToken);
        String coordinationDesk = resolveCoordinationDesk(key, organ, laneToken);
        String redistributionDesk = resolveRedistributionDesk(key, organ, laneToken);
        String dashboardBucket = key.lane().dashboardBucket() + '_' + key.instance().name();

        LinkedHashSet<String> labels = new LinkedHashSet<>();
        labels.add(organ);
        labels.add(instance);
        labels.add(laneToken);
        labels.add(key.uf());
        labels.add(key.comarca().isBlank() ? "SEM_COMARCA" : key.comarca());
        if (key.lane().requiresAudienceDesk()) {
            labels.add("AUDIENCE_REQUIRED");
        }
        if (key.isSpecializedDesk()) {
            labels.add("SPECIALIZED");
        }
        if (key.isSecondInstance()) {
            labels.add("COLEGIADO");
        }

        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("descriptor", key.descriptor());
        metadata.put("specializedDesk", key.isSpecializedDesk());
        metadata.put("territory", key.territorialLabel());
        metadata.put("coordinationDescriptor", coordinationDesk + ':' + assistantDesk);
        metadata.put("redistributionMode", key.isSecondInstance() ? "GABINETE_SUPPORT" : "SECRETARIA_INTERNAL");
        return new ForumDeskPortfolioProfile(
                triageDesk,
                gabineteDesk,
                hearingDesk,
                complianceDesk,
                escalationDesk,
                assistantDesk,
                coordinationDesk,
                redistributionDesk,
                dashboardBucket,
                List.copyOf(labels),
                metadata
        );
    }

    private String resolveAssistantDesk(ForumDeskKey key, String organ, String laneToken) {
        if (key.isSecondInstance()) {
            return "ASSESSORIA_" + organ + '_' + laneToken;
        }
        if (key.lane().requiresAudienceDesk()) {
            return "ASSIST_AUDIENCIA_" + organ + '_' + laneToken;
        }
        if (key.isSpecializedDesk()) {
            return "ASSIST_ESPECIALIZADA_" + organ + '_' + laneToken;
        }
        return "ASSIST_SECRETARIA_" + organ + '_' + laneToken;
    }

    private String resolveCoordinationDesk(ForumDeskKey key, String organ, String laneToken) {
        if (key.isSecondInstance()) {
            return "COORD_COLEGIADO_" + organ + '_' + laneToken;
        }
        if (key.lane().requiresAudienceDesk()) {
            return "COORD_AUDIENCIA_" + organ + '_' + laneToken;
        }
        return "COORD_SECRETARIA_" + organ + '_' + laneToken;
    }

    private String resolveRedistributionDesk(ForumDeskKey key, String organ, String laneToken) {
        if (key.lane().requiresAudienceDesk()) {
            return "REDIST_AUDIENCIA_" + organ + '_' + laneToken;
        }
        if (key.isSecondInstance()) {
            return "REDIST_GABINETE_" + organ + '_' + laneToken;
        }
        if (key.isSpecializedDesk()) {
            return "REDIST_ESPECIALIZADA_" + organ + '_' + laneToken;
        }
        return "REDIST_SECRETARIA_" + organ + '_' + laneToken;
    }
}
