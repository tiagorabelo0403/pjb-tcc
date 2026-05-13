package com.tcc.pjb.backend.model.dto.criminal;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PoliceNativeRequestsTest {

    @Test
    void shouldNormalizeDispatchRequest() {
        PoliceNativeCautelarDispatchRequest request = new PoliceNativeCautelarDispatchRequest(null, null, " ", " ", java.util.Arrays.asList(" ev-1 ", null, "ev-1"), null, null, null, null);
        Assertions.assertEquals("MEDIDA_CAUTELAR", request.tipoMedida());
        Assertions.assertEquals(1, request.referenciasEvidencia().size());
        Assertions.assertEquals("TRIBUNAL_PADRAO", request.tribunalAlvo());
        Assertions.assertTrue(request.permitirRemessaParceiraResolvido());
    }

    @Test
    void shouldNormalizeMirrorAndSnapshot() {
        PoliceNativeIntimationMirrorRequest mirror = new PoliceNativeIntimationMirrorRequest(null, null, " ", 0, null, null, null, null);
        Assertions.assertEquals("PJE_MNI", mirror.sistemaParceiro());
        Assertions.assertEquals(24, mirror.janelaHoras());

        PoliceLocalSnapshotRequest snapshot = new PoliceLocalSnapshotRequest(null, null, " ", " ", null, null, null, null, null);
        Assertions.assertEquals("COMPLETO", snapshot.escopoSnapshot());
        Assertions.assertEquals("PJE_MNI", snapshot.sistemaParceiro());
        Assertions.assertTrue(snapshot.reconciliarResolvido());
    }
}
