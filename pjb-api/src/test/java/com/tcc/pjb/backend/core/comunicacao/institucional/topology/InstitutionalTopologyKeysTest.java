package com.tcc.pjb.backend.core.comunicacao.institucional.topology;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.core.comunicacao.institucional.topology.domain.InstitutionalTopologyKeys;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import org.junit.jupiter.api.Test;

class InstitutionalTopologyKeysTest {

    @Test
    void mustCanonicalizeQueueAndRecipientKeys() {
        assertEquals("UNI-1|CX-TRIAGEM", InstitutionalTopologyKeys.queueKey(" UNI-1 ", " CX-TRIAGEM "));
        assertEquals("UNI-1|MINISTERIO_PUBLICO", InstitutionalTopologyKeys.unitRecipientKey(" UNI-1 ", DestinatarioInstitucionalKind.MINISTERIO_PUBLICO));
        assertTrue(InstitutionalTopologyKeys.matchesQueue("UNI-1", "CX-TRIAGEM", " UNI-1 ", " CX-TRIAGEM "));
    }
}
