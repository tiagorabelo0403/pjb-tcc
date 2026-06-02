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

// ESCOPO INTENCIONAL: modules/ (5), ai/ (3), leitura/ (14), peticionamento/ (9), calculo/ (13) — BLOCO-28.B/C.
// Outros pacotes (oficial_justica/, magistratura/, financial/) ficam para BLOCO-28.D+.
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

    // --- model/dto/leitura/ — BLOCO-28.B ---
    // 14 classes com Map<String,Object> documentados como Categoria D (payload polimórfico de leitura processual).
    // EcosystemResponse.frontend/integrity e FlowResponse.metadata foram tipados como records (Categoria A).
    // WorkspaceResponse.integrity tipado como record. WorkspaceResponse.frontend e PresetCatalogResponse.frontend ficam D.

    private static final Set<String> KNOWN_VIOLATIONS_LEITURA = Set.of(
            "ProcessReadingActionResponse",
            "ProcessReadingContentBlockResponse",
            "ProcessReadingContentResponse",
            "ProcessReadingDocumentResponse",
            "ProcessReadingLaneResponse",
            "ProcessReadingNavigationNodeResponse",
            "ProcessReadingNavigationResponse",
            "ProcessReadingPresetCatalogResponse",
            "ProcessReadingProceduralContextResponse",
            "ProcessReadingProcessEntryResponse",
            "ProcessReadingSearchHitResponse",
            "ProcessReadingSpecializationResponse",
            "ProcessReadingSurfaceResponse",
            "ProcessReadingWorkspaceResponse"
    );

    private static final int EXPECTED_LEITURA_COUNT = 14;

    @Test
    void nenhum_novo_dto_publico_de_leitura_pode_ter_campo_mapStringObject() throws IOException {
        Path leituraDto = Path.of("src/main/java/com/tcc/pjb/backend/model/dto/leitura");

        Set<String> violacoes = new LinkedHashSet<>();

        try (Stream<Path> paths = Files.walk(leituraDto)) {
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
        novasViolacoes.removeAll(KNOWN_VIOLATIONS_LEITURA);

        assertThat(novasViolacoes)
                .as("Novos DTOs em leitura/ com Map<String,Object> não documentados. Use record tipado ou @Schema+@Size.")
                .isEmpty();

        assertThat(violacoes)
                .as("Contagem deve ser exatamente %d. Se diminuiu: remova da KNOWN_VIOLATIONS_LEITURA.", EXPECTED_LEITURA_COUNT)
                .hasSize(EXPECTED_LEITURA_COUNT);
    }

    // --- model/dto/processual/peticionamento/ — BLOCO-28.B ---
    // 9 classes com Map<String,Object> documentados como Categoria D.
    // Studio Maps (procedure, dossier, riskMatrix, etc.) passados de projection sem tipagem — cascata fora de escopo.

    private static final Set<String> KNOWN_VIOLATIONS_PETICIONAMENTO = Set.of(
            "PeticionamentoAutomacaoResponse",
            "PeticionamentoGuardrailResponse",
            "PeticionamentoJourneyIntelligenceResponse",
            "PeticionamentoSimpleProtocolWizardResponse",
            "PeticionamentoSessaoResponse",
            "PeticionamentoStudioDraftDiffResponse",
            "PeticionamentoStudioGovernedReviewResponse",
            "PeticionamentoStudioQuickDraftResponse",
            "PeticionamentoStudioWorkspaceResponse"
    );

    private static final int EXPECTED_PETICIONAMENTO_COUNT = 9;

    @Test
    void nenhum_novo_dto_publico_de_peticionamento_pode_ter_campo_mapStringObject() throws IOException {
        Path peticionamentoDto = Path.of("src/main/java/com/tcc/pjb/backend/model/dto/processual/peticionamento");

        Set<String> violacoes = new LinkedHashSet<>();

        try (Stream<Path> paths = Files.walk(peticionamentoDto)) {
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
        novasViolacoes.removeAll(KNOWN_VIOLATIONS_PETICIONAMENTO);

        assertThat(novasViolacoes)
                .as("Novos DTOs em peticionamento/ com Map<String,Object> não documentados. Use record tipado ou @Schema+@Size.")
                .isEmpty();

        assertThat(violacoes)
                .as("Contagem deve ser exatamente %d. Se diminuiu: remova da KNOWN_VIOLATIONS_PETICIONAMENTO.", EXPECTED_PETICIONAMENTO_COUNT)
                .hasSize(EXPECTED_PETICIONAMENTO_COUNT);
    }

    // --- model/dto/processual/calculo/ — BLOCO-28.C ---
    // 13 classes com Map<String,Object> documentados como Categoria D.
    // EconomicReferenceResponse: salarioMinimoNacional e inss tipados como records (Categoria A);
    // fontesOficiais convertido a Map<String,String> (Categoria C); metadata fica D.

    private static final Set<String> KNOWN_VIOLATIONS_CALCULO = Set.of(
            "CalculoJudicialAjuizamentoSignalResponse",
            "CalculoJudicialAssistenciaResponse",
            "CalculoJudicialEconomicReferenceResponse",
            "CalculoJudicialExperiencePreferenceResponse",
            "CalculoJudicialFrontendBootstrapResponse",
            "CalculoJudicialFrontendCatalogResponse",
            "CalculoJudicialFrontendDomainResponse",
            "CalculoJudicialIaFinanceiraResponse",
            "CalculoJudicialResumoResponse",
            "CalculoJudicialTabelaOficialItemResponse",
            "CalculoJudicialTabelaOficialResponse",
            "CalculoJudicialWorkspaceCardResponse",
            "CalculoJudicialWorkspaceResponse"
    );

    private static final int EXPECTED_CALCULO_COUNT = 13;

    @Test
    void nenhum_novo_dto_publico_de_calculo_pode_ter_campo_mapStringObject() throws IOException {
        Path calculoDto = Path.of("src/main/java/com/tcc/pjb/backend/model/dto/processual/calculo");

        Set<String> violacoes = new LinkedHashSet<>();

        try (Stream<Path> paths = Files.walk(calculoDto)) {
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
        novasViolacoes.removeAll(KNOWN_VIOLATIONS_CALCULO);

        assertThat(novasViolacoes)
                .as("Novos DTOs em calculo/ com Map<String,Object> nao documentados. Use record tipado ou @Schema+@Size.")
                .isEmpty();

        assertThat(violacoes)
                .as("Contagem deve ser exatamente %d. Se diminuiu: remova da KNOWN_VIOLATIONS_CALCULO.", EXPECTED_CALCULO_COUNT)
                .hasSize(EXPECTED_CALCULO_COUNT);
    }

    private static String ler(Path p) {
        try { return Files.readString(p); } catch (IOException e) { return ""; }
    }
}
