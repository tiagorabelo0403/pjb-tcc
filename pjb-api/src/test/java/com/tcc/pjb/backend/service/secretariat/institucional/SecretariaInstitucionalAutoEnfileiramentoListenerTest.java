package com.tcc.pjb.backend.service.secretariat.institucional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.tcc.pjb.backend.model.entity.enums.MotivoEnfileiramentoInstitucional;
import com.tcc.pjb.backend.model.entity.enums.TipoUnidadeInstitucional;
import org.junit.jupiter.api.Test;

class SecretariaInstitucionalAutoEnfileiramentoListenerTest {

    private final SecretariaInstitucionalEnfileiramentoService enfileiramentoService = mock(SecretariaInstitucionalEnfileiramentoService.class);
    private final SecretariaInstitucionalAutoEnfileiramentoListener listener =
            new SecretariaInstitucionalAutoEnfileiramentoListener(enfileiramentoService);

    @Test
    void eventoDePoloNucleoDefensoriaDisparaEnfileiramentoAutomatico() {
        listener.aoComporPoloInstitucional(new PoloInstitucionalComposicaoEvent(42L, "Fortaleza", TipoUnidadeInstitucional.NUCLEO_DEFENSORIA));

        verify(enfileiramentoService).enfileirar(eq(42L), eq("Fortaleza"), eq(TipoUnidadeInstitucional.NUCLEO_DEFENSORIA),
                eq(MotivoEnfileiramentoInstitucional.PARTE_AUTOMATICA), eq(15));
    }
}
