package com.tcc.pjb.backend.modules.laiane.event;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.tcc.pjb.backend.modules.auditoria.AuditoriaInteligenteService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class LaianeOficioAuditPostCommitServiceTest {

    @Test
    void on_eventoValido_delegaParaAuditoria() {
        AuditoriaInteligenteService auditoria = Mockito.mock(AuditoriaInteligenteService.class);
        LaianeOficioAuditPostCommitService handler = new LaianeOficioAuditPostCommitService(auditoria);

        var event = new LaianeOficioAuditEvent(
                "MP_OFICIO_CRIADO",
                "01963c1a-7e3f-7000-8000-000000000001",
                "tipo=OFICIO_REQUISITORIO;destinoId=null",
                "Fluxo institucional"
        );

        handler.on(event);

        verify(auditoria).registrarEventoImutavelJustificado(
                "MP_OFICIO_CRIADO",
                "01963c1a-7e3f-7000-8000-000000000001",
                "tipo=OFICIO_REQUISITORIO;destinoId=null",
                "Fluxo institucional"
        );
    }

    @Test
    void on_eventoNulo_naoChama() {
        AuditoriaInteligenteService auditoria = Mockito.mock(AuditoriaInteligenteService.class);
        LaianeOficioAuditPostCommitService handler = new LaianeOficioAuditPostCommitService(auditoria);

        handler.on(null);

        verifyNoInteractions(auditoria);
    }

    @Test
    void on_acaoNula_naoChama() {
        AuditoriaInteligenteService auditoria = Mockito.mock(AuditoriaInteligenteService.class);
        LaianeOficioAuditPostCommitService handler = new LaianeOficioAuditPostCommitService(auditoria);

        handler.on(new LaianeOficioAuditEvent(null, "ref", "det", "just"));

        verifyNoInteractions(auditoria);
    }

    @Test
    void on_referenciaIdNula_naoChama() {
        AuditoriaInteligenteService auditoria = Mockito.mock(AuditoriaInteligenteService.class);
        LaianeOficioAuditPostCommitService handler = new LaianeOficioAuditPostCommitService(auditoria);

        handler.on(new LaianeOficioAuditEvent("MP_OFICIO_CRIADO", null, "det", "just"));

        verifyNoInteractions(auditoria);
    }
}
