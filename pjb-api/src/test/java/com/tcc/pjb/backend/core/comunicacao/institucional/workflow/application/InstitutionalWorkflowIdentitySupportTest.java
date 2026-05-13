package com.tcc.pjb.backend.core.comunicacao.institucional.workflow.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.domain.InstitutionalDraftManifestation;
import com.tcc.pjb.backend.model.entity.enums.StatusMinutaInstitucional;
import com.tcc.pjb.backend.model.entity.enums.TipoFluxoDelegacaoInstitucional;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class InstitutionalWorkflowIdentitySupportTest {

    @Test
    void nextDraftIdMustAdvanceWithHistorySize() {
        Instant now = Instant.now();
        InstitutionalDraftManifestation first = new InstitutionalDraftManifestation(
                "draft-1", "exp-1", 77L, "UNI", "CX", 10L, null,
                StatusMinutaInstitucional.RASCUNHO,
                "Titulo", "Conteudo", null,
                now.minusSeconds(120), null, null, now.minusSeconds(120), "hash-1"
        );
        InstitutionalDraftManifestation second = new InstitutionalDraftManifestation(
                "draft-2", "exp-1", 77L, "UNI", "CX", 10L, 20L,
                StatusMinutaInstitucional.REJEITADA,
                "Titulo 2", "Conteudo 2", null,
                now.minusSeconds(60), now.minusSeconds(30), now.minusSeconds(20), now.minusSeconds(20), "hash-2"
        );

        String next = InstitutionalWorkflowIdentitySupport.nextDraftId("exp-1", List.of(first, second));

        assertNotEquals("draft-1", next);
        assertNotEquals("draft-2", next);
        assertEquals(next, InstitutionalWorkflowIdentitySupport.nextDraftId("exp-1", List.of(first, second)));
    }

    @Test
    void outboxDedupKeyMustBeStableRegardlessOfMapOrder() {
        String first = InstitutionalWorkflowIdentitySupport.outboxDedupKey(
                "institutional_workflow",
                "EVENT",
                "exp-1",
                Map.of("b", 2, "a", 1)
        );
        String second = InstitutionalWorkflowIdentitySupport.outboxDedupKey(
                "institutional_workflow",
                "EVENT",
                "exp-1",
                Map.of("a", 1, "b", 2)
        );

        assertEquals(first, second);
        assertTrue(first.contains("a=1"));
        assertTrue(first.contains("b=2"));
    }

    @Test
    void assignmentIdMustChangeAcrossDistinctCreationInstants() {
        String first = InstitutionalWorkflowIdentitySupport.assignmentId("exp-1", TipoFluxoDelegacaoInstitucional.DELEGACAO, 30L, Instant.ofEpochMilli(10));
        String second = InstitutionalWorkflowIdentitySupport.assignmentId("exp-1", TipoFluxoDelegacaoInstitucional.DELEGACAO, 30L, Instant.ofEpochMilli(20));

        assertNotEquals(first, second);
    }
}
