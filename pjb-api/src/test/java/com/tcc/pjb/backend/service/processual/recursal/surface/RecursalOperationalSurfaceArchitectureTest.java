package com.tcc.pjb.backend.service.processual.recursal.surface;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class RecursalOperationalSurfaceArchitectureTest {

    @Test
    void deveManterSurfaceOperacionalRecursalNosPacotesCorretos() {
        assertThat(Files.exists(Path.of("src/main/java/com/tcc/pjb/backend/model/dto/processual/recursal/surface/RecursalOperationalSurfaceResponse.java"))).isTrue();
        assertThat(Files.exists(Path.of("src/main/java/com/tcc/pjb/backend/model/dto/processual/recursal/surface/RecursalOperationalSurfaceSectionView.java"))).isTrue();
        assertThat(Files.exists(Path.of("src/main/java/com/tcc/pjb/backend/model/dto/processual/recursal/surface/RecursalOperationalSurfaceGapView.java"))).isTrue();
        assertThat(Files.exists(Path.of("src/main/java/com/tcc/pjb/backend/service/processual/recursal/surface/RecursalOperationalSurfaceService.java"))).isTrue();
        assertThat(Files.exists(Path.of("src/main/java/com/tcc/pjb/backend/controller/processual/recursal/surface/RecursalOperationalSurfaceController.java"))).isTrue();
    }
}
