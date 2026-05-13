package com.tcc.pjb.backend.service.upload;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.PushbackInputStream;
import org.junit.jupiter.api.Test;

class UploadContentPolicyServiceTest {

    private final UploadContentPolicyService service = new UploadContentPolicyService();

    @Test
    void deveResolverReservaParaVideoMp4() {
        var policy = service.resolveReservation("prova.mp4", "video/mp4", 1024L);
        assertEquals("video/mp4", policy.normalizedContentType());
        assertEquals(".mp4", policy.storageExtension());
    }

    @Test
    void deveRejeitarTipoNaoPermitido() {
        assertThrows(IllegalArgumentException.class, () -> service.resolveReservation("script.js", "application/javascript", 512L));
    }

    @Test
    void deveValidarAssinaturaPng() throws Exception {
        byte[] png = new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00};
        service.assertMagicBytes("image/png", new PushbackInputStream(new ByteArrayInputStream(png), 32));
    }
}
