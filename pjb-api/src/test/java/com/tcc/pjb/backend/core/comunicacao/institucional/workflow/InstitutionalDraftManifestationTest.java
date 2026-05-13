package com.tcc.pjb.backend.core.comunicacao.institucional.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.domain.InstitutionalDraftManifestation;
import com.tcc.pjb.backend.model.entity.enums.StatusMinutaInstitucional;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class InstitutionalDraftManifestationTest {

    @Test
    void shouldRejectApprovalOutsideApprovalState() {
        Instant now = Instant.now();
        InstitutionalDraftManifestation draft = new InstitutionalDraftManifestation(
                "draft-1", "exp-1", 77L, "UNI", "CX", 10L, null,
                StatusMinutaInstitucional.RASCUNHO,
                "Titulo", "Conteudo", null,
                now.minusSeconds(60), null, null, now.minusSeconds(60), "hash"
        );

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> draft.withAprovacao("ok", now, "hash-2"));

        assertEquals("minuta_somente_pode_ser_aprovada_quando_estiver_em_aprovacao", ex.getMessage());
    }

    @Test
    void shouldRejectSendingBeforeApproval() {
        Instant now = Instant.now();
        InstitutionalDraftManifestation draft = new InstitutionalDraftManifestation(
                "draft-1", "exp-1", 77L, "UNI", "CX", 10L, 20L,
                StatusMinutaInstitucional.EM_APROVACAO,
                "Titulo", "Conteudo", null,
                now.minusSeconds(60), now.minusSeconds(30), null, now.minusSeconds(30), "hash"
        );

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> draft.withEnvio(now, "hash-2"));

        assertEquals("minuta_somente_pode_ser_enviada_quando_estiver_aprovada", ex.getMessage());
    }
}
