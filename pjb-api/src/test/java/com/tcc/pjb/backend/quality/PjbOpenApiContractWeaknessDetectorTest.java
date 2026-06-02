package com.tcc.pjb.backend.quality;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("contract")
class PjbOpenApiContractWeaknessDetectorTest {

    private static final Path SNAPSHOT = Path.of("target/openapi-snapshot.json");
    private static final Path ALLOWLIST = Path.of("../docs/architecture/openapi-contract-hardening-allowlist.yml");

    private static final Pattern ADDITIONAL_PROPERTIES_TRUE =
            Pattern.compile("additionalProperties\\s*=\\s*(?:true|SchemaType\\.BOOLEAN_TRUE|SchemaType\\.Boolean\\.TRUE)");

    private static final Pattern SCHEMA_EXAMPLE_ZERO =
            Pattern.compile("@Schema[^)]*example\\s*=\\s*\"0\"[^)]*\\)");

    private static final Pattern ALLOWLIST_SCHEMA =
            Pattern.compile("schema:\\s*(\\S+)");

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

    @Test
    @SuppressWarnings("unchecked")
    void snapshot_nao_deve_ter_additionalProperties_aberto_fora_da_allowlist() throws IOException {
        Assumptions.assumeTrue(Files.exists(SNAPSHOT),
            "Snapshot OpenAPI ausente — gere com: curl -s http://localhost:8080/v3/api-docs > target/openapi-snapshot.json");

        Set<String> allowlisted = carregarAllowlistSchemas();

        Map<String, Object> doc = JSON.readValue(SNAPSHOT.toFile(), Map.class);
        Map<String, Object> components = (Map<String, Object>) doc.getOrDefault("components", Map.of());
        Map<String, Object> schemas = (Map<String, Object>) components.getOrDefault("schemas", Map.of());

        List<String> violacoes = new ArrayList<>();
        for (Map.Entry<String, Object> entry : schemas.entrySet()) {
            String sname = entry.getKey();
            if (allowlisted.contains(sname)) continue;
            Map<String, Object> sdef = (Map<String, Object>) entry.getValue();
            Map<String, Object> props = (Map<String, Object>) sdef.getOrDefault("properties", Map.of());
            for (Map.Entry<String, Object> prop : props.entrySet()) {
                Map<String, Object> pdef = (Map<String, Object>) prop.getValue();
                Object ap = pdef.get("additionalProperties");
                if (ap instanceof Map && ((Map<?, ?>) ap).isEmpty()) {
                    violacoes.add(sname + "." + prop.getKey());
                }
            }
        }

        assertThat(violacoes)
            .as("Campos Map<String,Object> com additionalProperties:{} fora da allowlist. " +
                "Adicione o schema ao allowlist ou tipe o Map. " +
                "SpringDoc 2.8+ gera {} (nao true) para Map<String,Object>.")
            .isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void snapshot_nao_deve_ter_boolean_com_example_zero() throws IOException {
        Assumptions.assumeTrue(Files.exists(SNAPSHOT),
            "Snapshot OpenAPI ausente — gere com: curl -s http://localhost:8080/v3/api-docs > target/openapi-snapshot.json");

        Map<String, Object> doc = JSON.readValue(SNAPSHOT.toFile(), Map.class);
        Map<String, Object> components = (Map<String, Object>) doc.getOrDefault("components", Map.of());
        Map<String, Object> schemas = (Map<String, Object>) components.getOrDefault("schemas", Map.of());

        List<String> violacoes = new ArrayList<>();
        for (Map.Entry<String, Object> entry : schemas.entrySet()) {
            Map<String, Object> sdef = (Map<String, Object>) entry.getValue();
            Map<String, Object> props = (Map<String, Object>) sdef.getOrDefault("properties", Map.of());
            for (Map.Entry<String, Object> prop : props.entrySet()) {
                Map<String, Object> pdef = (Map<String, Object>) prop.getValue();
                Object type = pdef.get("type");
                Object example = pdef.get("example");
                if ("boolean".equals(type) && Integer.valueOf(0).equals(example)) {
                    violacoes.add(entry.getKey() + "." + prop.getKey());
                }
            }
        }

        assertThat(violacoes)
            .as("Campos boolean com example:0 detectados no snapshot OpenAPI. " +
                "Use @Schema(example=\"false\") ou @Schema(example=\"true\") conforme o default real.")
            .isEmpty();
    }

    @SuppressWarnings("unchecked")
    private Set<String> carregarAllowlistSchemas() throws IOException {
        if (!Files.exists(ALLOWLIST)) return Set.of();
        String content = Files.readString(ALLOWLIST);
        Set<String> result = new HashSet<>();
        Matcher m = ALLOWLIST_SCHEMA.matcher(content);
        while (m.find()) result.add(m.group(1));
        return result;
    }

    private static String ler(Path p) {
        try { return Files.readString(p); } catch (IOException e) { return ""; }
    }
}
