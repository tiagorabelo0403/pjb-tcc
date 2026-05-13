package com.tcc.pjb.backend.service.juiz;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.service.juiz.session.GabineteOperationalProfile;

@Component
public class CollegiatePublicationReviewResolver {

    public CollegiatePublicationReviewProfile resolve(CollegiateSessionGovernanceProfile governanceProfile,
                                                      GabineteOperationalProfile operationalProfile,
                                                      boolean colegiadoActive,
                                                      int recursalDrafts,
                                                      int sessionSensitiveItems) {
        String publicationDesk = governanceProfile == null || governanceProfile.publicationDesk() == null
                ? colegiadoActive ? "PUBLICACAO_COLEGIADA_GABINETE" : "PUBLICACAO_DECISORIA_GABINETE"
                : governanceProfile.publicationDesk();
        String reviewDesk = colegiadoActive
                ? "REVISAO_ACORDAO_E_PUBLICACAO"
                : operationalProfile != null && operationalProfile.blockingItems() > 0
                ? "REVISAO_COM_BLOQUEIO"
                : "REVISAO_MINUTA_E_PUBLICACAO";
        String publicationQueue = publicationDesk + "_QUEUE";
        String publicationDeadlineMode = sessionSensitiveItems >= 5
                ? "PUBLICACAO_DEADLINE_CURTO"
                : recursalDrafts >= 3
                ? "PUBLICACAO_DEADLINE_RECURSAL"
                : "PUBLICACAO_DEADLINE_PADRAO";
        String reviewMode = colegiadoActive
                ? "REVISAO_COLEGIADA_OBRIGATORIA"
                : "REVISAO_PADRAO_GABINETE";

        LinkedHashSet<String> labels = new LinkedHashSet<>();
        labels.add(publicationDeadlineMode);
        labels.add(reviewMode);
        if (colegiadoActive) {
            labels.add("PUBLICACAO_COLEGIADA");
        }
        if (recursalDrafts >= 3) {
            labels.add("PUBLICACAO_RECURSAL");
        }

        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("governanceDescriptor", governanceProfile == null ? null : governanceProfile.descriptor());
        metadata.put("recursalDrafts", recursalDrafts);
        metadata.put("sessionSensitiveItems", sessionSensitiveItems);
        metadata.put("descriptor", publicationDesk + ':' + reviewDesk + ':' + publicationDeadlineMode);
        metadata.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);

        return new CollegiatePublicationReviewProfile(
                publicationDesk,
                reviewDesk,
                publicationQueue,
                publicationDeadlineMode,
                reviewMode,
                List.copyOf(labels),
                metadata
        );
    }
}
