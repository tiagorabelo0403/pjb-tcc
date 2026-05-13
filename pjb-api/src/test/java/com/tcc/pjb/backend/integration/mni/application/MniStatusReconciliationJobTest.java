package com.tcc.pjb.backend.integration.mni.application;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.tcc.pjb.backend.integration.mni.infra.MniRemessaProperties;
import org.junit.jupiter.api.Test;

class MniStatusReconciliationJobTest {

    @Test
    void naoDeveExecutarQuandoDesabilitado() {
        MniRemessaService service = mock(MniRemessaService.class);
        MniStatusReconciliationJob job = new MniStatusReconciliationJob(service, new MniRemessaProperties(false, 5, 300_000, 10));

        job.run();

        verify(service, never()).reprocessarPendentes();
    }

    @Test
    void deveExecutarQuandoHabilitado() {
        MniRemessaService service = mock(MniRemessaService.class);
        MniStatusReconciliationJob job = new MniStatusReconciliationJob(service, new MniRemessaProperties(true, 5, 300_000, 10));

        job.run();

        verify(service, times(1)).reprocessarPendentes();
    }
}
