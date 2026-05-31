package com.tcc.pjb.backend.quality;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class PjbNoRawObjectControllerContractTest {

    private static final Set<String> KNOWN_VIOLATIONS = Set.of(
            "InqueritoPolicialDigitalController",
            "DelegadoPainelController",
            "OficialJusticaMandadoController"
    );

    private static final int EXPECTED_OCCURRENCE_COUNT = 25;

    private static final Pattern RESPONSE_ENTITY_OBJECT =
            Pattern.compile("ResponseEntity\\s*<\\s*Object\\s*>");

    @Test
    void nenhum_controller_novo_pode_retornar_responseEntityObject() throws IOException {
        Path srcRoot = Path.of("src/main/java/com/tcc/pjb/backend");

        Map<String, Integer> violacoes = new LinkedHashMap<>();

        try (Stream<Path> paths = Files.walk(srcRoot)) {
            paths.filter(p -> p.getFileName().toString().endsWith("Controller.java"))
                 .forEach(p -> {
                     String source = ler(p);
                     String className = p.getFileName().toString().replace(".java", "");
                     Matcher m = RESPONSE_ENTITY_OBJECT.matcher(source);
                     int count = 0;
                     while (m.find()) count++;
                     if (count > 0) violacoes.put(className, count);
                 });
        }

        Set<String> novasViolacoes = new java.util.HashSet<>(violacoes.keySet());
        novasViolacoes.removeAll(KNOWN_VIOLATIONS);

        assertThat(novasViolacoes)
                .as("Novos controllers com ResponseEntity<Object> detectados além da allowlist. " +
                    "Corrija antes de commitar ou adicione em openapi-contract-hardening-allowlist.yml + KNOWN_VIOLATIONS.")
                .isEmpty();

        int total = violacoes.values().stream().mapToInt(i -> i).sum();
        assertThat(total)
                .as("Contagem de ResponseEntity<Object> deve ser exatamente %d. " +
                    "Se diminuiu: remova o controller de KNOWN_VIOLATIONS e atualize EXPECTED_OCCURRENCE_COUNT. " +
                    "Se aumentou: corrija o controller — não adicione na allowlist sem ADR.", EXPECTED_OCCURRENCE_COUNT)
                .isEqualTo(EXPECTED_OCCURRENCE_COUNT);
    }

    private static String ler(Path p) {
        try { return Files.readString(p); } catch (IOException e) { return ""; }
    }
}
