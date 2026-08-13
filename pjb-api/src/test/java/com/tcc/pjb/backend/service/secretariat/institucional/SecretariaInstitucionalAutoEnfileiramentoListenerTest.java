package com.tcc.pjb.backend.service.secretariat.institucional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.model.entity.enums.MotivoEnfileiramentoInstitucional;
import com.tcc.pjb.backend.model.entity.enums.TipoUnidadeInstitucional;
import org.junit.jupiter.api.Test;

class SecretariaInstitucionalAutoEnfileiramentoListenerTest {

    private final SecretariaInstitucionalEnfileiramentoService enfileiramentoService = mock(SecretariaInstitucionalEnfileiramentoService.class);
    private final AuditLedgerService auditService = mock(AuditLedgerService.class);
    private final SecretariaInstitucionalAutoEnfileiramentoListener listener =
            new SecretariaInstitucionalAutoEnfileiramentoListener(enfileiramentoService, auditService);

    @Test
    void eventoDePoloNucleoDefensoriaDisparaEnfileiramentoAutomatico() {
        listener.aoComporPoloInstitucional(new PoloInstitucionalComposicaoEvent(42L, "Fortaleza", TipoUnidadeInstitucional.NUCLEO_DEFENSORIA));

        verify(enfileiramentoService).enfileirar(eq(42L), eq("Fortaleza"), eq(TipoUnidadeInstitucional.NUCLEO_DEFENSORIA),
                eq(MotivoEnfileiramentoInstitucional.PARTE_AUTOMATICA), eq(15));
    }

    @Test
    void excecaoDentroDoListenerNaoPropagaParaOChamadorPosCommit() {
        when(enfileiramentoService.enfileirar(any(), any(), any(), any(), org.mockito.ArgumentMatchers.anyInt()))
                .thenThrow(new IllegalStateException("falha inesperada de roteamento institucional"));

        assertThatCode(() -> listener.aoComporPoloInstitucional(
                new PoloInstitucionalComposicaoEvent(43L, "Sobral", TipoUnidadeInstitucional.PROMOTORIA)))
                .doesNotThrowAnyException();

        verify(auditService).appendSafely(eq("SECRETARIA_INSTITUCIONAL_ENFILEIRAMENTO_FALHA_POS_COMMIT"), any());
    }
}
