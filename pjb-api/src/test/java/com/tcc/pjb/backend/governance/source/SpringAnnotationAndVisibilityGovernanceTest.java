package com.tcc.pjb.backend.governance.source;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class SpringAnnotationAndVisibilityGovernanceTest {

    @Test
    void naoDeveHaverAtalhosInvalidosDeAnotacaoSpringWeb() {
        List<String> offenders = SourceGovernanceScanner.forbiddenSpringShortcutAnnotations();
        assertTrue(offenders.isEmpty(), "Atalhos inválidos de anotação Spring Web detectados: " + offenders);
    }

    @Test
    void controllersComMappingsDevemImportarSpringWebBindAnotations() {
        List<String> offenders = SourceGovernanceScanner.controllerMappingImportViolations();
        assertTrue(offenders.isEmpty(), "Controller com mapping sem import confiável de Spring Web annotation: " + offenders);
    }

    @Test
    void importsEntrePacotesNaoDevemApontarParaTiposTopLevelNaoPublicos() {
        List<String> offenders = SourceGovernanceScanner.nonPublicCrossPackageImports();
        assertTrue(offenders.isEmpty(), "Import de tipo top-level não público entre pacotes: " + offenders);
    }
}
