package com.tcc.pjb.backend.quality;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

class PjbHardeningAllowlistSchemaTest {

    private static final Path ALLOWLIST = Path.of("../docs/architecture/openapi-contract-hardening-allowlist.yml");

    private static final Set<String> CAMPOS_OBRIGATORIOS = Set.of(
            "id", "tipo", "schema", "violacao", "motivo", "risco",
            "bloco_correcao", "teste_negativo", "criado_em", "auditoria_ref"
    );

    private static final Set<String> RISCOS_VALIDOS = Set.of("critico", "alto", "medio", "baixo");

    @Test
    @SuppressWarnings("unchecked")
    void allowlist_deve_ter_schema_valido() throws IOException {
        assertThat(ALLOWLIST).as("Allowlist deve existir em docs/architecture/").exists();

        Yaml yaml = new Yaml();
        Map<String, Object> doc;
        try (FileInputStream fis = new FileInputStream(ALLOWLIST.toFile())) {
            doc = yaml.load(fis);
        }

        List<Map<String, Object>> entradas = (List<Map<String, Object>>) doc.get("allowlist");
        assertThat(entradas).as("allowlist não deve ser nula ou vazia").isNotEmpty();

        List<String> erros = new java.util.ArrayList<>();

        for (Map<String, Object> entrada : entradas) {
            String id = String.valueOf(entrada.getOrDefault("id", "?"));

            for (String campo : CAMPOS_OBRIGATORIOS) {
                if (!entrada.containsKey(campo) || entrada.get(campo) == null) {
                    erros.add(id + ": campo obrigatório ausente: " + campo);
                }
            }

            Object risco = entrada.get("risco");
            if (risco != null && !RISCOS_VALIDOS.contains(String.valueOf(risco))) {
                erros.add(id + ": risco inválido '" + risco + "' — valores válidos: " + RISCOS_VALIDOS);
            }

            Object sla = entrada.get("sla_remocao");
            if (sla != null) {
                try {
                    LocalDate slaDate = LocalDate.parse(String.valueOf(sla));
                    if (slaDate.isBefore(LocalDate.now())) {
                        erros.add(id + ": sla_remocao no passado (" + sla + ") — corrija a violação ou atualize o SLA");
                    }

                    Object criadoEm = entrada.get("criado_em");
                    if ("critico".equals(String.valueOf(risco)) && criadoEm != null) {
                        LocalDate criado = LocalDate.parse(String.valueOf(criadoEm));
                        if (slaDate.isAfter(criado.plusDays(30))) {
                            erros.add(id + ": risco=critico mas sla_remocao (" + sla + ") excede 30 dias após criado_em (" + criadoEm + ")");
                        }
                    }
                } catch (DateTimeParseException e) {
                    erros.add(id + ": sla_remocao com formato inválido (esperado yyyy-MM-dd): " + sla);
                }
            } else if ("alto".equals(String.valueOf(risco)) || "critico".equals(String.valueOf(risco))) {
                erros.add(id + ": risco=" + risco + " requer sla_remocao definido");
            }
        }

        assertThat(erros)
                .as("Erros de schema encontrados na allowlist openapi-contract-hardening-allowlist.yml")
                .isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void allowlist_nao_deve_ter_sla_no_passado() throws IOException {
        Yaml yaml = new Yaml();
        Map<String, Object> doc;
        try (FileInputStream fis = new FileInputStream(ALLOWLIST.toFile())) {
            doc = yaml.load(fis);
        }

        List<Map<String, Object>> entradas = (List<Map<String, Object>>) doc.get("allowlist");
        LocalDate hoje = LocalDate.now();

        List<String> vencidos = entradas.stream()
                .filter(e -> e.get("sla_remocao") != null)
                .filter(e -> {
                    try {
                        return LocalDate.parse(String.valueOf(e.get("sla_remocao"))).isBefore(hoje);
                    } catch (DateTimeParseException ex) { return false; }
                })
                .map(e -> e.get("id") + " (sla_remocao=" + e.get("sla_remocao") + ")")
                .toList();

        assertThat(vencidos)
                .as("Entradas com sla_remocao no passado — a violação deveria ter sido corrigida")
                .isEmpty();
    }
}
