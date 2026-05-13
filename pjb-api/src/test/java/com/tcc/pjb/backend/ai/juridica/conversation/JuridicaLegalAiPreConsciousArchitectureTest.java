package com.tcc.pjb.backend.ai.juridica.conversation;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class JuridicaLegalAiPreConsciousArchitectureTest {

    @Test
    void mustNotCreatePreConsciousHttpSurface() throws IOException {
        Path root = Path.of("src/main/java");
        if (!Files.exists(root)) {
            return;
        }

        var offenders = Files.walk(root)
                .filter(path -> path.toString().endsWith(".java"))
                .filter(path -> path.getFileName().toString().contains("Controller"))
                .filter(this::containsForbiddenSurface)
                .map(Path::toString)
                .toList();

        assertTrue(offenders.isEmpty(), "A pré-consciência jurídica deve permanecer no orchestrator, sem controller próprio: " + offenders);
    }

    @Test
    void mustKeepPreConsciousClassesBelowTwoHundredLines() throws IOException {
        List<Path> files = List.of(
                Path.of("src/main/java/com/tcc/pjb/backend/ai/juridica/conversation/LegalAiJuridicalLineageRegistry.java"),
                Path.of("src/main/java/com/tcc/pjb/backend/ai/juridica/conversation/LegalAiPreConsciousSignalExtractor.java"),
                Path.of("src/main/java/com/tcc/pjb/backend/ai/juridica/conversation/LegalAiPreConsciousFrameService.java"),
                Path.of("src/main/java/com/tcc/pjb/backend/ai/juridica/conversation/LegalAiPreConsciousToolScopeEnricher.java"),
                Path.of("src/main/java/com/tcc/pjb/backend/model/dto/ai/legal/conversation/LegalAiJuridicalLineageDescriptor.java"),
                Path.of("src/main/java/com/tcc/pjb/backend/model/dto/ai/legal/conversation/LegalAiPreConsciousSignal.java"),
                Path.of("src/main/java/com/tcc/pjb/backend/model/dto/ai/legal/conversation/LegalAiPreConsciousFrameSnapshot.java")
        );

        var offenders = files.stream()
                .filter(Files::exists)
                .filter(path -> countLines(path) > 200)
                .map(Path::toString)
                .toList();

        assertTrue(offenders.isEmpty(), "Classes da pré-consciência jurídica não podem passar de 200 linhas: " + offenders);
    }

    @Test
    void orchestratorMustWirePreConsciousFrameBeforeFinalApproval() throws IOException {
        Path orchestrator = Path.of("src/main/java/com/tcc/pjb/backend/ai/juridica/conversation/LegalAiConversationOrchestrator.java");
        if (!Files.exists(orchestrator)) {
            return;
        }
        String source = Files.readString(orchestrator);
        int frame = source.indexOf("preConsciousFrameService.inspect");
        int approval = source.indexOf("approvalService.evaluate", frame);

        assertTrue(frame > 0, "Orchestrator deve montar a moldura pré-consciente.");
        assertTrue(approval > frame, "Approval final deve ocorrer depois da moldura pré-consciente.");
    }

    private boolean containsForbiddenSurface(Path path) {
        try {
            String source = Files.readString(path).toLowerCase();
            return source.contains("subconsciente")
                    || source.contains("subconscious")
                    || source.contains("preconscious")
                    || source.contains("pre-conscious")
                    || source.contains("metacognition")
                    || source.contains("metacognicao");
        } catch (IOException ex) {
            return true;
        }
    }

    private static long countLines(Path path) {
        try (var lines = Files.lines(path)) {
            return lines.count();
        } catch (IOException ex) {
            return Long.MAX_VALUE;
        }
    }
}
