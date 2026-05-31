package com.tcc.pjb.backend.quality;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("contract")
class PjbOpenApiContractWeaknessDetectorTest {

    private static final Pattern ADDITIONAL_PROPERTIES_TRUE =
            Pattern.compile("additionalProperties\\s*=\\s*(?:true|SchemaType\\.BOOLEAN_TRUE|SchemaType\\.Boolean\\.TRUE)");

    private static final Pattern SCHEMA_EXAMPLE_ZERO =
            Pattern.compile("@Schema[^)]*example\\s*=\\s*\"0\"[^)]*\\)");

    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    void laiane_nao_deve_ter_additionalProperties_true_em_schema() throws IOException {
        Path laianeDto = Path.of("src/main/java/com/tcc/pjb/backend/modules/laiane/dto");

        List<String> violacoes = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(laianeDto)) {
            paths.filter(p -> p.toString().endsWith(".java"))
                 .forEach(p -> {
                     String source = ler(p);
                     if (ADDITIONAL_PROPERTIES_TRUE.matcher(source).find()) {
                         violacoes.add(p.getFileName().toString());
                     }
                 });
        }

        assertThat(violacoes)
                .as("DTOs do módulo laiane não devem ter @Schema com additionalProperties=true. " +
                    "Módulo laiane foi declarado limpo no BLOCO-26 — qualquer nova ocorrência é regressão.")
                .isEmpty();
    }

    @Test
    void laiane_nao_deve_ter_example_zero_em_campo_id() throws IOException {
        Path laianeDto = Path.of("src/main/java/com/tcc/pjb/backend/modules/laiane/dto");

        List<String> violacoes = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(laianeDto)) {
            paths.filter(p -> p.toString().endsWith(".java"))
                 .forEach(p -> {
                     String source = ler(p);
                     if (SCHEMA_EXAMPLE_ZERO.matcher(source).find()) {
                         violacoes.add(p.getFileName().toString());
                     }
                 });
        }

        assertThat(violacoes)
                .as("DTOs do módulo laiane não devem ter @Schema(example=\"0\") em campo id. " +
                    "Use UUID ou valor descritivo como exemplo.")
                .isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void laianeMpOficioResponse_nao_contem_campos_proibidos_pelo_contrato_baseline() throws IOException {
        Path contrato = Path.of("../docs/api/laiane-mp-openapi-v1.json");
        assertThat(contrato).as("Arquivo de contrato baseline deve existir").exists();

        Map<String, Object> baseline = JSON.readValue(contrato.toFile(), Map.class);
        List<String> proibidos = (List<String>) baseline.get("prohibited");
        assertThat(proibidos).as("Lista 'prohibited' deve estar definida no contrato").isNotEmpty();

        Path responseFile = Path.of("src/main/java/com/tcc/pjb/backend/modules/laiane/dto/roles/mp/LaianeMpOficioResponse.java");
        assertThat(responseFile).as("LaianeMpOficioResponse.java deve existir").exists();
        String source = Files.readString(responseFile);

        List<String> camposProibidosPresentes = proibidos.stream()
                .filter(campo -> {
                    Pattern fieldDecl = Pattern.compile(
                            "(?:private|protected|public)\\s+\\S+\\s+" + Pattern.quote(campo) + "\\s*[;,=]");
                    return fieldDecl.matcher(source).find();
                })
                .toList();

        assertThat(camposProibidosPresentes)
                .as("LaianeMpOficioResponse não deve ter campos proibidos pelo contrato baseline " +
                    "(docs/api/laiane-mp-openapi-v1.json). Campos proibidos: %s", proibidos)
                .isEmpty();
    }

    private static String ler(Path p) {
        try { return Files.readString(p); } catch (IOException e) { return ""; }
    }
}
