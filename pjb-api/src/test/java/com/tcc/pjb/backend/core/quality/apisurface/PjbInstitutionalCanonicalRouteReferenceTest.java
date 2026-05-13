package com.tcc.pjb.backend.core.quality.apisurface;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class PjbInstitutionalCanonicalRouteReferenceTest {

    @Test
    void legacyInstitutionalLiteralMustDisappearFromRuntimeCodebase() throws Exception {
        try (Stream<Path> stream = Files.walk(Path.of("src/main/java"))) {
            List<String> files = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> ApiSurfaceTestSupport.read(path).contains("/api/v1/processual/comunicacoes/institucional"))
                    .map(Path::toString)
                    .sorted()
                    .toList();
            assertEquals(List.of(), files, "Rota institucional legada nao deve permanecer acessivel no runtime.");
        }
    }

    @Test
    void canonicalInstitutionalLiteralMustBeUsedAcrossRuntimeHelpers() {
        String routes = ApiSurfaceTestSupport.read(Path.of("src/main/java/com/tcc/pjb/backend/core/comunicacao/institucional/InstitutionalApiRoutes.java"));
        assertTrue(routes.contains("public static final String CANONICAL_BASE = \"/api/v1/institucional\";"));
        assertFalse(routes.contains("LEGACY_BASE"));
        assertTrue(routes.contains("public static String painelExecutivo()"));
        assertTrue(routes.contains("public static String afiliacoes()"));
        assertTrue(routes.contains("public static String homologarAfiliacao()"));
        assertTrue(routes.contains("public static String homologarAfiliacao(String affiliationId)"));
        assertTrue(routes.contains("public static String atestacaoFontesOficiaisAfiliacao()"));
        assertTrue(routes.contains("public static String revalidarFontesOficiaisAfiliacao()"));
        assertTrue(routes.contains("public static String atestacaoFontesOficiaisAfiliacao(String affiliationId)"));
        assertTrue(routes.contains("public static String revalidarFontesOficiaisAfiliacao(String affiliationId)"));
        assertTrue(routes.contains("public static String nomeacoes()"));
        assertTrue(routes.contains("public static String entradaSegura()"));
        assertTrue(routes.contains("public static String catalogoAcessos()"));
        assertTrue(routes.contains("public static String blueprints()"));
        assertTrue(routes.contains("public static String recertificacoes()"));
        assertTrue(routes.contains("public static String recertificarAfiliacao()"));
        assertTrue(routes.contains("public static String recertificarAfiliacao(String affiliationId)"));
        assertTrue(routes.contains("public static String revogarAcessosAfiliacao()"));
        assertTrue(routes.contains("public static String revogarAcessosAfiliacao(String affiliationId)"));
        assertTrue(routes.contains("public static String integracoesGovernanca()"));
        assertTrue(routes.contains("public static String quatroNiveis()"));
        assertTrue(routes.contains("public static String casosOperacionaisAfiliacao()"));
        assertTrue(routes.contains("public static String casosOperacionaisAfiliacao(String affiliationId)"));
        assertTrue(routes.contains("public static String diagnosticoEstrutural()"));
        assertTrue(routes.contains("public static String modeloOperacional()"));
        assertTrue(routes.contains("public static String entradaInteligente()"));
        assertTrue(routes.contains("public static String contextosEntrada()"));
        assertTrue(routes.contains("public static String painelPessoal()"));
        assertTrue(routes.contains("public static String atestacaoFontesOficiaisAdesaoDelegada()"));
        assertTrue(routes.contains("public static String revalidarFontesOficiaisAdesaoDelegada()"));
        assertTrue(routes.contains("public static String atestacaoFontesOficiaisAdesaoDelegada(String requestId)"));
        assertTrue(routes.contains("public static String revalidarFontesOficiaisAdesaoDelegada(String requestId)"));
        assertTrue(routes.contains("public static String dimensionamentoUsuariosInternos()"));
        assertTrue(routes.contains("public static String governancaConfianca()"));
        assertTrue(routes.contains("public static String governancaConfianca(String nominationId)"));
        assertTrue(routes.contains("public static String governancaConfiancaDecisoes()"));
        assertTrue(routes.contains("public static String governancaConfiancaDecisoes(String nominationId)"));
        assertTrue(routes.contains("public static String planoDadosHorizontal()"));
        assertTrue(routes.contains("public static String planoDadosHorizontal(String nominationId)"));
        assertTrue(routes.contains("public static String perfilOperacional()"));
        assertTrue(routes.contains("public static String perfilOperacional(String nominationId)"));
        assertTrue(routes.contains("private static String encodeQueryComponent(String value)"));
        assertTrue(routes.contains("public static boolean isInstitutionalPath(String path)"));
        assertTrue(routes.contains("public static String extractNominationId(String path)"));
        assertTrue(routes.contains("public static String extractAffiliationId(String path)"));
    }
}
