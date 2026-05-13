package com.tcc.pjb.backend.core.quality.apisurface;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PjbUploadSurfaceDisciplineTest {

    @Test
    void uploadBatchControllerNaoDeveTerRecordInlineNemRespostaDeServiceAninhado() throws IOException {
        String source = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/controller/UploadBatchController.java"));
        assertFalse(source.contains("record "));
        assertFalse(source.contains("BulkUploadIngressService."));
        assertFalse(source.contains("JobsController."));
    }
}
