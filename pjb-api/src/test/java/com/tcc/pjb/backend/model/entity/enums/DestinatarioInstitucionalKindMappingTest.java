package com.tcc.pjb.backend.model.entity.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class DestinatarioInstitucionalKindMappingTest {

    @Test
    void shouldMapLegacyExpandedKinds() {
        assertEquals(DestinatarioInstitucionalKind.CEJUSC,
                DestinatarioInstitucionalKind.fromNationalCommunicationRecipientKind(NationalCommunicationRecipientKind.CEJUSC).orElseThrow());
        assertEquals(DestinatarioInstitucionalKind.CARTORIO_EXTRAJUDICIAL,
                DestinatarioInstitucionalKind.fromNationalCommunicationRecipientKind(NationalCommunicationRecipientKind.CARTORIO_EXTRAJUDICIAL).orElseThrow());
        assertEquals(OrganizacaoExtraJudicialKind.APOIO_TECNICO_JUDICIAL,
                DestinatarioInstitucionalKind.PERICIA_JUDICIAL.toOrganizacaoExtraJudicialKind());
        assertTrue(NationalCommunicationRecipientKind.ORGAO_TECNICO_CONVENIADO.isInstitucional());
    }
}
