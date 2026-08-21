package com.tcc.pjb.backend.core.quality.apisurface;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class PjbPermitAllExposureDisciplineTest {

    private static final Set<String> APPROVED_PERMIT_ALL_FILES = Set.of(
            "src/main/java/com/tcc/pjb/backend/controller/IdeaCatalogController.java",
            "src/main/java/com/tcc/pjb/backend/controller/OrgaoJudiciarioController.java",
            "src/main/java/com/tcc/pjb/backend/controller/auth/CertificadoAuthController.java",
            "src/main/java/com/tcc/pjb/backend/controller/auth/GovBrLoginController.java",
            "src/main/java/com/tcc/pjb/backend/controller/auth/PasskeyAuthController.java",
            "src/main/java/com/tcc/pjb/backend/controller/auth/PasskeyStepUpController.java",
            "src/main/java/com/tcc/pjb/backend/controller/auth/TermosAceiteController.java",
            "src/main/java/com/tcc/pjb/backend/controller/consulta/ConsultasRapidasController.java",
            "src/main/java/com/tcc/pjb/backend/controller/integration/N8nIntegrationController.java",
            "src/main/java/com/tcc/pjb/backend/controller/magistratura/MagistradoAtivacaoController.java",
            "src/main/java/com/tcc/pjb/backend/controller/notification/NotificationTrackingController.java",
            "src/main/java/com/tcc/pjb/backend/controller/publico/AtlasAcessoJusticaController.java",
            "src/main/java/com/tcc/pjb/backend/controller/publico/ConsultasPublicasController.java",
            "src/main/java/com/tcc/pjb/backend/controller/publico/PainelNacionalJusticaController.java",
            "src/main/java/com/tcc/pjb/backend/controller/publico/PublicJulgamentoController.java",
            "src/main/java/com/tcc/pjb/backend/controller/publico/PublicPlenarioController.java",
            "src/main/java/com/tcc/pjb/backend/controller/publico/PublicProcessoGemeoDigitalController.java",
            "src/main/java/com/tcc/pjb/backend/controller/publico/TribunalPerfilController.java",
            "src/main/java/com/tcc/pjb/backend/controller/security/AdvogadoBaptismController.java",
            "src/main/java/com/tcc/pjb/backend/controller/security/BodyHashController.java",
            "src/main/java/com/tcc/pjb/backend/controller/security/RequestHashController.java",
            "src/main/java/com/tcc/pjb/backend/controller/system/PjbProbeController.java",
            "src/main/java/com/tcc/pjb/backend/controller/ui/UiAssuntoCatalogController.java",
            "src/main/java/com/tcc/pjb/backend/controller/ui/UiLegendController.java",
            "src/main/java/com/tcc/pjb/backend/modules/advocacia/controller/LegacyAdvocaciaClienteForwardController.java"
    );

    private static final Set<String> APPROVED_PUBLIC_WRITE_FILES = Set.of(
            "src/main/java/com/tcc/pjb/backend/controller/auth/CertificadoAuthController.java",
            "src/main/java/com/tcc/pjb/backend/controller/auth/GovBrLoginController.java",
            "src/main/java/com/tcc/pjb/backend/controller/auth/PasskeyAuthController.java",
            "src/main/java/com/tcc/pjb/backend/controller/auth/PasskeyStepUpController.java",
            "src/main/java/com/tcc/pjb/backend/controller/auth/TermosAceiteController.java",
            "src/main/java/com/tcc/pjb/backend/controller/integration/N8nIntegrationController.java",
            "src/main/java/com/tcc/pjb/backend/controller/magistratura/MagistradoAtivacaoController.java",
            "src/main/java/com/tcc/pjb/backend/controller/notification/NotificationTrackingController.java",
            "src/main/java/com/tcc/pjb/backend/controller/security/AdvogadoBaptismController.java",
            "src/main/java/com/tcc/pjb/backend/controller/security/BodyHashController.java",
            "src/main/java/com/tcc/pjb/backend/controller/security/RequestHashController.java",
            "src/main/java/com/tcc/pjb/backend/modules/advocacia/controller/LegacyAdvocaciaClienteForwardController.java"
    );

    private static final Pattern CLASS_LEVEL_PERMIT_ALL = Pattern.compile("@PreAuthorize\\(\\\"permitAll\\(\\)\\\"\\)\\s*public class");
    private static final Pattern WRITE_MAPPING = Pattern.compile("@(PostMapping|PutMapping|PatchMapping|DeleteMapping)|RequestMethod\\.(POST|PUT|PATCH|DELETE)");

    @Test
    void permitAllExposureMustStayOnCuratedAllowlist() throws Exception {
        Set<String> actual = ApiSurfaceTestSupport.controllerFiles().stream()
                .filter(this::containsPermitAll)
                .map(PjbPermitAllExposureDisciplineTest::normalizePath)
                .collect(java.util.stream.Collectors.toCollection(java.util.TreeSet::new));
        assertEquals(new java.util.TreeSet<>(APPROVED_PERMIT_ALL_FILES), actual,
                "Novas exposicoes publicas com permitAll exigem revisao deliberada.");
    }

    @Test
    void publicWriteExposureMustStayOnCuratedAllowlist() throws Exception {
        Set<String> actual = ApiSurfaceTestSupport.controllerFiles().stream()
                .filter(this::hasPublicWriteExposure)
                .map(PjbPermitAllExposureDisciplineTest::normalizePath)
                .collect(java.util.stream.Collectors.toCollection(java.util.TreeSet::new));
        assertEquals(new java.util.TreeSet<>(APPROVED_PUBLIC_WRITE_FILES), actual,
                "Endpoints publicos com escrita exigem allowlist explicita e revisao cuidadosa.");
    }

    @Test
    void administrativeControllersMustCarryAdministrativeAuthorityMarkers() throws Exception {
        List<String> weak = ApiSurfaceTestSupport.controllerFiles().stream()
                .filter(path -> path.toString().contains("/controller/admin/"))
                .filter(path -> !hasAdministrativeAuthorityMarker(path))
                .map(PjbPermitAllExposureDisciplineTest::normalizePath)
                .sorted()
                .toList();
        assertTrue(weak.isEmpty(), "Controllers administrativos sem marcador explicito de autoridade administrativa: " + weak);
    }

    private static String normalizePath(Path path) {
        return path.toString().replace('\\', '/');
    }

    private boolean containsPermitAll(Path path) {
        return ApiSurfaceTestSupport.read(path).contains("@PreAuthorize(\"permitAll()\")");
    }

    private boolean hasPublicWriteExposure(Path path) {
        String content = ApiSurfaceTestSupport.read(path);
        if (!content.contains("@PreAuthorize(\"permitAll()\")")) {
            return false;
        }
        if (CLASS_LEVEL_PERMIT_ALL.matcher(content).find()) {
            return WRITE_MAPPING.matcher(content).find();
        }
        boolean permitAllInAnnotationBlock = false;
        boolean writeInAnnotationBlock = false;
        for (String rawLine : content.split("\\R")) {
            String line = rawLine.strip();
            if (line.isEmpty()) {
                continue;
            }
            if (line.startsWith("@")) {
                if (line.contains("@PreAuthorize(\"permitAll()\")")) {
                    permitAllInAnnotationBlock = true;
                }
                if (WRITE_MAPPING.matcher(line).find()) {
                    writeInAnnotationBlock = true;
                }
                continue;
            }
            if (line.contains("RequestMethod.POST")
                    || line.contains("RequestMethod.PUT")
                    || line.contains("RequestMethod.PATCH")
                    || line.contains("RequestMethod.DELETE")) {
                writeInAnnotationBlock = true;
                continue;
            }
            if (line.startsWith("public ")) {
                if (permitAllInAnnotationBlock && writeInAnnotationBlock) {
                    return true;
                }
                permitAllInAnnotationBlock = false;
                writeInAnnotationBlock = false;
                continue;
            }
            permitAllInAnnotationBlock = false;
            writeInAnnotationBlock = false;
        }
        return false;
    }

    private boolean hasAdministrativeAuthorityMarker(Path path) {
        String content = ApiSurfaceTestSupport.read(path);
        return content.contains("hasRole('ADMIN")
                || content.contains("hasRole('ADMINISTRADOR")
                || content.contains("hasAnyRole('ADMIN")
                || content.contains("hasAnyRole('ADMINISTRADOR")
                || content.contains("hasAuthority('ROLE_ADMIN")
                || content.contains("hasAuthority('ROLE_ADMINISTRADOR")
                || content.contains("hasAnyAuthority('ROLE_ADMIN")
                || content.contains("hasAnyAuthority('ROLE_ADMINISTRADOR");
    }
}
