package com.tcc.pjb.backend.governance.source;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class RepresentationPolicyJsonUsageGuardTest {

    @Test
    void pontosCriticosDevemReusarExtractorCanonicoDaRepresentationPolicy() {
        List<Path> files = List.of(
                Path.of("src/main/java/com/tcc/pjb/backend/controller/forum/ForumHabilitacaoController.java"),
                Path.of("src/main/java/com/tcc/pjb/backend/modules/laiane/api/LaianeLawyerController.java"),
                Path.of("src/main/java/com/tcc/pjb/backend/service/forum/ForumHabilitacaoService.java")
        );

        List<String> missing = files.stream()
                .filter(path -> !SourceGovernanceScanner.read(path).contains("RepresentacaoProcessualPolicyJsonExtractor"))
                .map(Path::toString)
                .toList();

        assertTrue(missing.isEmpty(), "Pontos críticos ainda não usam o extractor canônico da representationPolicy: " + missing);
    }
}
