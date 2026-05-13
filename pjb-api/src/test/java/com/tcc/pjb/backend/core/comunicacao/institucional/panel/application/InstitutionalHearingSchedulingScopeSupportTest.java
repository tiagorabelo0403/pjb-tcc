package com.tcc.pjb.backend.core.comunicacao.institucional.panel.application;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalProcessProfile;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOperationalProfileProjection;
import com.tcc.pjb.backend.core.comunicacao.institucional.processual.domain.InstitutionalProcessWorkspace;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class InstitutionalHearingSchedulingScopeSupportTest {

    @Test
    void mustResolveSchedulingScopeKeyAndQueuesWithExpandedFallbackChain() {
        InstitutionalHearingSchedulingScopeSupport support = new InstitutionalHearingSchedulingScopeSupport();
        InstitutionalOperationalProfileProjection profile = new InstitutionalOperationalProfileProjection(
                "PROFILE-1",
                "ATIVO",
                true,
                "AFF-1",
                "NOM-1",
                10L,
                "Servidor 1",
                "SERVIDOR",
                "TJCE",
                "TJCE",
                "TJCE",
                "Tribunal de Justiça do Ceará",
                "UNI-1",
                "2ª Vara Criminal de Fortaleza",
                "CX-1",
                "SECRETARIA",
                "SECRETARIA_FORUM",
                "GESTOR_CAIXA",
                "SECRETARIA_FORUM",
                "PAINEL_AUDIENCIAS",
                "/api/v1/institucional",
                "#2563eb",
                "AREA-1",
                "PLENO",
                true,
                true,
                true,
                false,
                false,
                true,
                "LOCAL",
                "TJCE",
                "UNI-1",
                "2ª Vara Criminal de Fortaleza",
                "Fortaleza",
                "CE|ORG-1|UNI-1|CX-1|0",
                "WRITE-1",
                "READ-1",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                Instant.now()
        );
        InstitutionalProcessWorkspace workspace = new InstitutionalProcessWorkspace(
                "PROFILE-1",
                "Secretaria",
                "PANEL",
                "SECRETARIA_FORUM",
                "PLENO",
                "#2563eb",
                "RITO_PENAL",
                "INSTRUCAO",
                "ATIVO",
                "PENAL",
                List.of("AUDIENCIAS"),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );

        String scopeKey = support.buildSchedulingScopeKey(profile, workspace, "PENAL", InstitutionalProcessProfile.SECRETARIA_FORUM);
        List<String> queues = support.resolveOperationalQueues(profile, workspace, "PENAL", InstitutionalProcessProfile.SECRETARIA_FORUM, true, false, false, false);

        assertTrue(scopeKey.contains("VARA_2_PENAL"));
        assertTrue(scopeKey.endsWith("|SECRETARIA_FORUM"));
        assertTrue(queues.contains("PAUTA:UNI-1:CX-1"));
        assertTrue(queues.contains("FILTRO_VARA_CLUSTER:VARA_2_PENAL"));
    }
}
