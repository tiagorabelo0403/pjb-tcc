package com.tcc.pjb.backend.core.comunicacao.institucional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.NationalCommunicationRecipientKind;

class DestinatarioInstitucionalKindTest {

    @Test
    void shouldMapLegacyRecipientKinds() {
        assertEquals(DestinatarioInstitucionalKind.MINISTERIO_PUBLICO,
                DestinatarioInstitucionalKind.fromNationalCommunicationRecipientKind(NationalCommunicationRecipientKind.MINISTERIO_PUBLICO).orElseThrow());
        assertEquals(NationalCommunicationRecipientKind.FAZENDA_PUBLICA,
                DestinatarioInstitucionalKind.ADVOCACIA_PUBLICA.toNationalCommunicationRecipientKind().orElseThrow());
    }

    @Test
    void shouldRecognizeEssentialJusticeInstitutions() {
        assertTrue(DestinatarioInstitucionalKind.DEFENSORIA_PUBLICA.isInstituicaoEssencialJustica());
        assertTrue(DestinatarioInstitucionalKind.MINISTERIO_PUBLICO.admiteCanalNacionalPessoal());
    }
}
