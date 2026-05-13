package com.tcc.pjb.backend.core.comunicacao.institucional.topology.application;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;

class InstitutionalRecipientTopologyApplicationServiceTest {

    @Test
    void shouldExposeExpandedInstitutionalTopology() {
        InstitutionalRecipientTopologyApplicationService service = new InstitutionalRecipientTopologyApplicationService();
        var cejusc = service.list().stream()
                .filter(entry -> entry.destinatarioInstitucionalKind() == DestinatarioInstitucionalKind.CEJUSC)
                .findFirst()
                .orElseThrow();
        assertFalse(cejusc.legadosCompativeis().isEmpty());
        assertTrue(service.list().stream().anyMatch(entry -> entry.destinatarioInstitucionalKind() == DestinatarioInstitucionalKind.MINISTERIO_PUBLICO));
    }
}
