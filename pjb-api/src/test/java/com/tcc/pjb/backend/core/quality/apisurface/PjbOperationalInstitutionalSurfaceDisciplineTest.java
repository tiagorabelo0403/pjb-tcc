package com.tcc.pjb.backend.core.quality.apisurface;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PjbOperationalInstitutionalSurfaceDisciplineTest {

    private static final List<String> TARGETS = List.of(
            "src/main/java/com/tcc/pjb/backend/controller/transito/TransitoJulgadoArquivamentoController.java",
            "src/main/java/com/tcc/pjb/backend/controller/conciliacao/ConciliadorMediadorEnhancedController.java",
            "src/main/java/com/tcc/pjb/backend/controller/secretariat/operational/ServidorSecretariaOperacionalController.java",
            "src/main/java/com/tcc/pjb/backend/controller/secretariat/operational/SecretariaEspecializadaController.java",
            "src/main/java/com/tcc/pjb/backend/controller/secretariat/operational/SecretariatMinutaJuntadaController.java",
            "src/main/java/com/tcc/pjb/backend/controller/defensor/DefensoriaPublicaOperacionalController.java",
            "src/main/java/com/tcc/pjb/backend/controller/defensor/DefensoriaVulnerabilidadeController.java",
            "src/main/java/com/tcc/pjb/backend/controller/desembargador/DesembargadorColegialdoPainelController.java",
            "src/main/java/com/tcc/pjb/backend/controller/procuradoria/ProcuradoriaOperacionalController.java",
            "src/main/java/com/tcc/pjb/backend/controller/procuradoria/PrecatorioRpvController.java"
    );

    @Test
    void controllersNaoDevemExporMapCruOuTiposAninhadosDeService() throws IOException {
        for (String target : TARGETS) {
            String source = Files.readString(Path.of(target));
            assertFalse(source.contains("Map<String, Object>"), target);
            assertFalse(source.contains(" record ") || source.contains("\n    record "), target);
            assertFalse(source.matches("(?s).*ResponseEntity\\s*<\\s*[A-Za-z0-9_$.]+Service\\.[A-Za-z0-9_$.]+.*"), target);
            assertFalse(source.matches("(?s).*@RequestBody\\s+[A-Za-z0-9_$.]+Service\\.[A-Za-z0-9_$.]+.*"), target);
        }
    }
}
