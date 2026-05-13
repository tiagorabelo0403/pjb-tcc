package com.tcc.pjb.backend.service.processual.participacao;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.tcc.pjb.backend.service.processual.participacao.workspace.RepresentationGuardView;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProcessualParticipacaoAtivaFacadeCompatibilityTest {

    @Test
    void shouldExposeLegacyAndCanonicalRepresentationInstrumentAccessors() {
        RepresentationGuardView view = new RepresentationGuardView(
                "REGULAR",
                true,
                false,
                true,
                "PROCURACAO_PUBLICA",
                "POSTULACAO_TECNICA",
                List.of("procuração"),
                List.of("validar_vinculo"),
                List.of("alerta")
        );

        assertEquals("PROCURACAO_PUBLICA", view.instrumentoResolvido());
        assertEquals(view.instrumentoResolvido(), view.resolvedInstrument());
    }
}
