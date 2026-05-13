package com.tcc.pjb.backend.service.oficial_justica;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class OficialJusticaOrganizationalScopeService {

    private static final Pattern PREFIX_PATTERN = Pattern.compile("^(VARA|UNIDADE|UJ|LOTACAO|ABRANGENCIA|ESCOPO|TRIBUNAL)\\s*[:=\\-]\\s*(.+)$");
    private static final Pattern VARA_PATTERN = Pattern.compile("\\b(VARA\\s*[A-Z0-9 -]+)\\b");
    private static final Set<String> GLOBAL_SCOPE_TOKENS = Set.of(
            "TODAS_AS_VARAS",
            "TODA_COMARCA",
            "COMARCA_COMPLETA",
            "GLOBAL_COMARCA",
            "VARA_TODA",
            "VARA_COMPLETA",
            "TODAS_VARAS_COMARCA"
    );

    public Scope resolve(Usuario usuario, List<WorkItem> universe) {
        LinkedHashSet<String> allowedVaras = new LinkedHashSet<>();
        LinkedHashSet<String> allowedUnidades = new LinkedHashSet<>();
        LinkedHashSet<String> rawTokens = new LinkedHashSet<>();
        if (usuario != null) {
            collectTokens(rawTokens, usuario.getEspecialidades());
            collectTokens(rawTokens, splitLoose(usuario.getRegistroProfissional()));
            collectTokens(rawTokens, splitLoose(usuario.getPerfil()));
        }
        boolean coversAllVaras = false;
        for (String token : rawTokens) {
            if (token == null || token.isBlank()) {
                continue;
            }
            String normalized = normalizeKey(token);
            Matcher matcher = PREFIX_PATTERN.matcher(normalized);
            if (matcher.matches()) {
                String prefix = matcher.group(1);
                String value = matcher.group(2);
                if (isGlobalValue(value)) {
                    coversAllVaras = true;
                    continue;
                }
                switch (prefix) {
                    case "VARA" -> addNormalizedVariant(allowedVaras, value);
                    case "UNIDADE", "UJ" -> addNormalizedVariant(allowedUnidades, value);
                    case "LOTACAO", "ABRANGENCIA", "ESCOPO" -> {
                        if (isGlobalValue(value)) {
                            coversAllVaras = true;
                        } else if (value.contains("VARA")) {
                            addNormalizedVariant(allowedVaras, value);
                        }
                    }
                    default -> {
                    }
                }
                continue;
            }
            if (isGlobalValue(normalized)) {
                coversAllVaras = true;
                continue;
            }
            if (normalized.contains("VARA")) {
                addNormalizedVariant(allowedVaras, normalized);
            }
            if (normalized.startsWith("UJ") || normalized.startsWith("UNIDADE")) {
                addNormalizedVariant(allowedUnidades, normalized);
            }
        }
        if (allowedVaras.isEmpty() && !coversAllVaras && universe != null && !universe.isEmpty()) {
            List<String> explicitVaras = availableVaras(universe);
            if (explicitVaras.size() == 1) {
                addNormalizedVariant(allowedVaras, explicitVaras.getFirst());
            }
        }
        boolean institutionManaged = coversAllVaras || !allowedVaras.isEmpty() || !allowedUnidades.isEmpty();
        String mode = institutionManaged
                ? coversAllVaras ? "LOTAÇÃO_COMARCA_INSTITUCIONAL" : "VARA_DIRECIONADA_INSTITUCIONAL"
                : "NOMEAÇÃO_DIRETA_CONTROLADA";
        String label = coversAllVaras
                ? "Cobertura institucional da comarca/estrutura integral"
                : !allowedVaras.isEmpty()
                ? "Oficial direcionado por vara/unidade institucional"
                : "Oficial operando apenas no conjunto materializado de nomeações/vínculos";
        return new Scope(
                mode,
                label,
                institutionManaged,
                coversAllVaras,
                List.copyOf(allowedVaras),
                List.copyOf(allowedUnidades),
                usuario != null ? normalizeNullable(usuario.getUf()) : null,
                usuario != null ? normalizeNullable(usuario.getComarca()) : null
        );
    }

    public List<WorkItem> filterByScope(Usuario usuario, List<WorkItem> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        Scope scope = resolve(usuario, items);
        return items.stream().filter(item -> allows(scope, item.getProcesso(), item)).toList();
    }

    public boolean allows(Usuario usuario, Processo processo, WorkItem item) {
        return allows(resolve(usuario, item == null ? List.of() : List.of(item)), processo, item);
    }

    public boolean allows(Scope scope, Processo processo, WorkItem item) {
        if (scope == null) {
            return true;
        }
        if (!territoryMatches(scope, processo, item)) {
            return false;
        }
        if (!scope.institutionManaged()) {
            return true;
        }
        if (scope.cobreTodasAsVaras()) {
            return true;
        }
        String vara = normalizeNullable(resolveVaraDisplay(processo, item));
        String unidade = normalizeNullable(resolveUnidadeJudiciaria(processo));
        if (!scope.unidades().isEmpty() && unidade != null && scope.unidades().contains(unidade)) {
            return true;
        }
        if (scope.varas().isEmpty()) {
            return true;
        }
        return matchesConfiguredVara(scope.varas(), vara);
    }

    public String resolveVaraDisplay(Processo processo, WorkItem item) {
        String vara = firstNonBlank(processo != null ? processo.getVara() : null, item != null ? item.getQueueCode() : null, item != null ? item.getInboxKey() : null);
        if (vara == null) {
            return "VARA_NAO_IDENTIFICADA";
        }
        Matcher matcher = VARA_PATTERN.matcher(normalizeLabel(vara));
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return normalizeLabel(vara);
    }

    public String resolveLotacaoLabel(Scope scope, Processo processo, WorkItem item) {
        if (scope == null) {
            return "NOMEAÇÃO_DIRETA";
        }
        if (scope.cobreTodasAsVaras()) {
            return "LOTAÇÃO COMARCA / TODAS AS VARAS";
        }
        String vara = resolveVaraDisplay(processo, item);
        if (!scope.varas().isEmpty() && matchesConfiguredVara(scope.varas(), normalizeNullable(vara))) {
            return "LOTAÇÃO " + vara;
        }
        if (!scope.unidades().isEmpty()) {
            return "LOTAÇÃO UNIDADE " + firstNonBlank(resolveUnidadeJudiciaria(processo), scope.unidades().getFirst());
        }
        return "NOMEAÇÃO DIRETA CONTROLADA";
    }

    public List<String> availableVaras(List<WorkItem> items) {
        if (items == null) {
            return List.of();
        }
        return items.stream()
                .map(item -> resolveVaraDisplay(item.getProcesso(), item))
                .filter(Objects::nonNull)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    public List<String> availableRitos(List<WorkItem> items) {
        if (items == null) {
            return List.of();
        }
        return items.stream()
                .map(item -> item.getProcesso() != null && item.getProcesso().getRito() != null ? item.getProcesso().getRito().name() : "COMUM_ORDINARIO")
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    public List<String> availableLotacoes(Usuario usuario, List<WorkItem> items) {
        Scope scope = resolve(usuario, items);
        if (scope.cobreTodasAsVaras()) {
            return List.of("LOTAÇÃO COMARCA / TODAS AS VARAS");
        }
        if (!scope.varas().isEmpty()) {
            return scope.varas().stream().map(this::normalizeLabel).toList();
        }
        return items == null ? List.of() : items.stream()
                .map(item -> resolveLotacaoLabel(scope, item.getProcesso(), item))
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    public Map<String, List<String>> processNumbersByRito(List<WorkItem> items) {
        if (items == null || items.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, LinkedHashSet<String>> out = new LinkedHashMap<>();
        items.stream()
                .sorted(Comparator.comparing(item -> item.getProcesso() != null && item.getProcesso().getRito() != null ? item.getProcesso().getRito().name() : "COMUM_ORDINARIO"))
                .forEach(item -> {
                    String rito = item.getProcesso() != null && item.getProcesso().getRito() != null ? item.getProcesso().getRito().name() : "COMUM_ORDINARIO";
                    String numero = item.getProcesso() != null ? firstNonBlank(item.getProcesso().getNumeroProcesso(), item.getProcesso().getNumero()) : null;
                    if (numero != null) {
                        out.computeIfAbsent(rito, ignored -> new LinkedHashSet<>()).add(numero);
                    }
                });
        LinkedHashMap<String, List<String>> view = new LinkedHashMap<>();
        out.forEach((key, value) -> view.put(key, List.copyOf(value)));
        return Map.copyOf(view);
    }

    public Map<String, Object> toMap(Scope scope) {
        if (scope == null) {
            return Map.of();
        }
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("mode", scope.mode());
        out.put("label", scope.label());
        out.put("institutionManaged", scope.institutionManaged());
        out.put("cobreTodasAsVaras", scope.cobreTodasAsVaras());
        out.put("varas", scope.varas());
        out.put("unidades", scope.unidades());
        out.put("territorioBase", firstNonBlank(scope.comarcaBase(), "COMARCA") + "/" + firstNonBlank(scope.ufBase(), "UF"));
        return Collections.unmodifiableMap(out);
    }

    private boolean territoryMatches(Scope scope, Processo processo, WorkItem item) {
        String uf = normalizeNullable(firstNonBlank(processo != null ? processo.getUf() : null, item != null ? item.getUf() : null));
        String comarca = normalizeNullable(firstNonBlank(processo != null ? processo.getComarca() : null, item != null ? item.getComarca() : null));
        boolean ufMatches = scope.ufBase() == null || uf == null || scope.ufBase().equals(uf);
        boolean comarcaMatches = scope.comarcaBase() == null || comarca == null || scope.comarcaBase().equals(comarca);
        return ufMatches && comarcaMatches;
    }

    private boolean matchesConfiguredVara(List<String> configuredVaras, String actualVara) {
        if (configuredVaras == null || configuredVaras.isEmpty()) {
            return true;
        }
        if (actualVara == null || actualVara.isBlank()) {
            return false;
        }
        for (String configured : configuredVaras) {
            if (configured == null || configured.isBlank()) {
                continue;
            }
            if (actualVara.equals(configured) || actualVara.contains(configured) || configured.contains(actualVara)) {
                return true;
            }
            String actualDigits = digitsOnly(actualVara);
            String configDigits = digitsOnly(configured);
            if (!actualDigits.isBlank() && actualDigits.equals(configDigits)) {
                return true;
            }
        }
        return false;
    }

    private String resolveUnidadeJudiciaria(Processo processo) {
        return processo == null ? null : normalizeNullable(processo.getUnidadeJudiciariaCodigo());
    }

    private void collectTokens(LinkedHashSet<String> target, Collection<String> values) {
        if (values == null) {
            return;
        }
        values.forEach(value -> {
            if (value != null) {
                String trimmed = value.trim();
                if (!trimmed.isBlank()) {
                    target.add(trimmed);
                }
            }
        });
    }

    private List<String> splitLoose(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split("[,;|]"))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }

    private boolean isGlobalValue(String value) {
        String normalized = normalizeNullable(value);
        return normalized != null && GLOBAL_SCOPE_TOKENS.contains(normalized.replace(' ', '_'));
    }

    private void addNormalizedVariant(LinkedHashSet<String> target, String value) {
        String normalized = normalizeNullable(value);
        if (normalized != null) {
            target.add(normalized);
        }
    }

    private String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return normalizeKey(value);
    }

    private String normalizeKey(String value) {
        String noAccent = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return noAccent.toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String normalizeLabel(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }

    private String digitsOnly(String value) {
        return value == null ? "" : value.replaceAll("\\D+", "");
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    public record Scope(
            String mode,
            String label,
            boolean institutionManaged,
            boolean cobreTodasAsVaras,
            List<String> varas,
            List<String> unidades,
            String ufBase,
            String comarcaBase
    ) {
        public Scope {
            varas = varas == null ? List.of() : List.copyOf(varas);
            unidades = unidades == null ? List.of() : List.copyOf(unidades);
        }
    }
}
