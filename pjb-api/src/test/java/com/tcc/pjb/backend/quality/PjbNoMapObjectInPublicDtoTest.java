package com.tcc.pjb.backend.quality;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

// ESCOPO INTENCIONAL: este teste cobre modules/ (5 classes) e ai/ (3 classes — BLOCO-28.A).
// Outros pacotes (model/dto/, financial/) ficam para BLOCO-28.B/C.
class PjbNoMapObjectInPublicDtoTest {

    private static final Pattern MAP_STRING_OBJECT =
            Pattern.compile("Map\\s*<\\s*String\\s*,\\s*Object\\s*>");

    // --- modules/ ---

    private static final Set<String> KNOWN_VIOLATIONS_MODULES = Set.of(
            "LaianeCockpitResponse",
            "LaianeJudicialDecisionAdvisoryResponse",
            "LaianeLawyerProcuracaoResponse",
            "LaianePeticaoAssistResponse",
            "LaianePeticaoProtocolPackageResponse"
    );

    private static final int EXPECTED_MODULES_COUNT = 5;

    @Test
    void nenhum_novo_dto_publico_de_modules_pode_ter_campo_mapStringObject() throws IOException {
        Path modulesDto = Path.of("src/main/java/com/tcc/pjb/backend/modules");

        Set<String> violacoes = new LinkedHashSet<>();

        try (Stream<Path> paths = Files.walk(modulesDto)) {
            paths.filter(p -> {
                     String name = p.getFileName().toString();
                     return name.endsWith("Response.java") || name.endsWith("View.java");
                 })
                 .forEach(p -> {
                     String source = ler(p);
                     String className = p.getFileName().toString().replace(".java", "");
                     if (MAP_STRING_OBJECT.matcher(source).find()) {
                         violacoes.add(className);
                     }
                 });
        }

        Set<String> novasViolacoes = new HashSet<>(violacoes);
        novasViolacoes.removeAll(KNOWN_VIOLATIONS_MODULES);

        assertThat(novasViolacoes)
                .as("Novos DTOs em modules/ com Map<String,Object> detectados além da allowlist. " +
                    "Use um record ou classe concreta em vez de Map.")
                .isEmpty();

        assertThat(violacoes)
                .as("Contagem de DTOs com Map<String,Object> deve ser exatamente %d. " +
                    "Se diminuiu: remova da KNOWN_VIOLATIONS e atualize EXPECTED_CLASS_COUNT. " +
                    "Se aumentou: corrija o DTO — não adicione na allowlist sem ADR.", EXPECTED_MODULES_COUNT)
                .hasSize(EXPECTED_MODULES_COUNT);
    }

    // --- ai/ — BLOCO-28.A ---
    // AgentResult.data, AgenticRunResponse.output (@JsonIgnore — legado), IAResponse.metadados/essence:
    // todos Categoria D (payload polimórfico de IA). Documentados na allowlist AL-0028 a AL-0030.

    private static final Set<String> KNOWN_VIOLATIONS_AI = Set.of(
            "AgentResult",
            "AgenticRunResponse",
            "IAResponse"
    );

    private static final int EXPECTED_AI_COUNT = 3;

    @Test
    void nenhum_novo_dto_publico_de_ai_pode_ter_campo_mapStringObject() throws IOException {
        Path aiDto = Path.of("src/main/java/com/tcc/pjb/backend/ai");

        Set<String> violacoes = new LinkedHashSet<>();

        try (Stream<Path> paths = Files.walk(aiDto)) {
            paths.filter(p -> {
                     String name = p.getFileName().toString();
                     return name.endsWith("Response.java") || name.endsWith("View.java")
                             || name.endsWith("Result.java");
                 })
                 .forEach(p -> {
                     String source = ler(p);
                     String className = p.getFileName().toString().replace(".java", "");
                     if (MAP_STRING_OBJECT.matcher(source).find()) {
                         violacoes.add(className);
                     }
                 });
        }

        Set<String> novasViolacoes = new HashSet<>(violacoes);
        novasViolacoes.removeAll(KNOWN_VIOLATIONS_AI);

        assertThat(novasViolacoes)
                .as("Novos DTOs em ai/ com Map<String,Object> detectados além da allowlist. " +
                    "Use um record ou classe concreta em vez de Map.")
                .isEmpty();

        assertThat(violacoes)
                .as("Contagem de DTOs em ai/ com Map<String,Object> deve ser exatamente %d. " +
                    "Se diminuiu: remova da KNOWN_VIOLATIONS_AI. " +
                    "Se aumentou: corrija o DTO — não adicione na allowlist sem ADR.", EXPECTED_AI_COUNT)
                .hasSize(EXPECTED_AI_COUNT);
    }

    private static String ler(Path p) {
        try { return Files.readString(p); } catch (IOException e) { return ""; }
    }
}
