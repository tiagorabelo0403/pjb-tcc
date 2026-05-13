package com.tcc.pjb.backend.tribunal.regras;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import com.tcc.pjb.backend.tribunal.regras.spec.PluginManifest;

class PluginResolucaoTribunalServiceRefinementArchitectureTest {

    private static final Path SERVICE_PATH = Path.of("src/main/java/com/tcc/pjb/backend/tribunal/regras/PluginResolucaoTribunalService.java");
    private static final Path SUPPORT_PATH = Path.of("src/main/java/com/tcc/pjb/backend/tribunal/regras/PluginResolucaoTribunalManifestSupport.java");

    @Test
    void pluginServiceAndManifestSupportMustStayBelowHotspotThresholds() throws IOException {
        assertThat(Files.lines(SERVICE_PATH, StandardCharsets.UTF_8).count()).isLessThan(1100);
        assertThat(Files.lines(SUPPORT_PATH, StandardCharsets.UTF_8).count()).isLessThan(500);
    }

    @Test
    void pluginServiceMustKeepManifestParsingAndProfileMergingOutsideHotspot() throws IOException {
        String service = Files.readString(SERVICE_PATH, StandardCharsets.UTF_8);
        String support = Files.readString(SUPPORT_PATH, StandardCharsets.UTF_8);

        assertThat(service).contains("PluginResolucaoTribunalManifestSupport");
        assertThat(service).doesNotContain("private PluginManifest readManifest(String json)");
        assertThat(service).doesNotContain("private PerfilInstanciaTribunalService.PerfilInstancia converterPerfil(");
        assertThat(service).doesNotContain("private Set<LocalDate> mergeFeriadosPrazo(");

        assertThat(support).contains("PluginManifest readManifest(String json)");
        assertThat(support).contains("PerfilInstanciaTribunalService.PerfilInstancia converterPerfil(");
        assertThat(support).contains("Set<LocalDate> mergeFeriadosPrazo(");
    }
}
