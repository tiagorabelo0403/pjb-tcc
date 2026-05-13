package com.tcc.pjb.backend.core.comunicacao.institucional.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import com.tcc.pjb.backend.core.comunicacao.institucional.integration.domain.InstitutionalExternalDispatchResult;
import com.tcc.pjb.backend.model.entity.enums.StatusIntegracaoInstitucionalExterna;

class InstitutionalExternalDispatchResultTest {

    @Test
    void shouldCreateAcceptedResult() {
        InstitutionalExternalDispatchResult result = InstitutionalExternalDispatchResult.accepted("ref-1", "OUTBOX_ACCEPTED", "{}");
        assertEquals(StatusIntegracaoInstitucionalExterna.ACEITA, result.status());
        assertEquals("ref-1", result.providerReference());
        assertTrue(result.failureReason() == null);
    }
}
