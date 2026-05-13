package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.core.catalog.TpuClasseCnj;
import com.tcc.pjb.backend.core.catalog.TpuClasseCnj.RamoJustica;
import com.tcc.pjb.backend.core.competence.NationalCompetenceMatrix;
import com.tcc.pjb.backend.core.competence.NationalCompetenceMatrix.RamoJusticaNacional;
import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.service.procedural.ProceduralCatalogService;
import com.tcc.pjb.backend.core.util.PayloadMaps;
import java.text.Normalizer;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class ProceduralCanonicalResolver {

    public record CanonicalContext(
            Instant resolvedAt,
            RitoProcessual rito,
            String ramoDireito,
            String classeTpuCodigo,
            String classeTpuNome,
            String ramoJusticaNacional,
            String tribunalCodigo,
            String tribunalNome,
            String judicialSystemPreferido,
            List<String> requiredPartyRoles,
            List<String> requiredDocuments,
            List<String> competenceHints,
            Map<String, Object> metadata
    ) {
        public CanonicalContext() {
            this(Instant.now(), null, null, null, null, null, null, null, null, List.of(), List.of(), List.of(), Map.of());
        }

        public CanonicalContext {
            requiredPartyRoles = PayloadMaps.copyTrimmedStrings(requiredPartyRoles);
            requiredDocuments = PayloadMaps.copyTrimmedStrings(requiredDocuments);
            competenceHints = PayloadMaps.copyTrimmedStrings(competenceHints);
            metadata = PayloadMaps.deepCopyWithoutNulls(metadata);
        }

        public Map<String, Object> toMap() {
            LinkedHashMap<String, Object> out = new LinkedHashMap<>();
            out.put("resolvedAt", resolvedAt != null ? resolvedAt.toString() : null);
            out.put("rito", rito != null ? rito.name() : null);
            out.put("ramoDireito", ramoDireito);
            out.put("classeTpuCodigo", classeTpuCodigo);
            out.put("classeTpuNome", classeTpuNome);
            out.put("ramoJusticaNacional", ramoJusticaNacional);
            out.put("tribunalCodigo", tribunalCodigo);
            out.put("tribunalNome", tribunalNome);
            out.put("judicialSystemPreferido", judicialSystemPreferido);
            out.put("requiredPartyRoles", requiredPartyRoles);
            out.put("requiredDocuments", requiredDocuments);
            out.put("competenceHints", competenceHints);
            out.put("metadata", metadata);
            return PayloadMaps.deepCopyWithoutNulls(out);
        }
    }

    private final ProceduralCatalogService proceduralCatalogService;

    public ProceduralCanonicalResolver(ProceduralCatalogService proceduralCatalogService) {
        this.proceduralCatalogService = Objects.requireNonNull(proceduralCatalogService);
    }

    public CanonicalContext resolve(Map<String, Object> payload) {
        Map<String, Object> safe = payload == null ? Map.of() : payload;
        String ritoRaw = firstNonBlank(text(safe.get("rito")), text(safe.get("rito_processual")), text(safe.get("ritoProcessual")));
        String ramoRaw = firstNonBlank(text(safe.get("ramo_direito")), text(safe.get("ramoDireito")), text(safe.get("materia")), text(safe.get("ambito_direito")));
        String classeRaw = firstNonBlank(text(safe.get("classeTpu")), text(safe.get("classe_tpu")), text(safe.get("classeProcessual")), text(safe.get("classe")), text(safe.get("tipoAcao")));
        String tribunalInput = firstNonBlank(text(safe.get("tribunalCodigo")), text(safe.get("tribunal_codigo")), text(safe.get("tribunal")), text(safe.get("siglaTribunal")));
        String uf = firstNonBlank(text(safe.get("uf")), text(safe.get("ufAutor")), text(safe.get("estado")));
        String competencia = firstNonBlank(text(safe.get("competencia")), text(safe.get("tipoJustica")), text(safe.get("esfera")));

        boolean hasProceduralSignal = hasText(ritoRaw) || hasText(ramoRaw) || hasText(classeRaw);
        RitoProcessual rito = hasProceduralSignal ? proceduralCatalogService.resolveRito(ritoRaw, ramoRaw, classeRaw) : null;
        Optional<TpuClasseCnj> classe = hasText(classeRaw) || rito != null
                ? proceduralCatalogService.resolveClasseTpu(classeRaw, rito)
                : Optional.empty();
        RamoDireito ramoDireito = resolveRamoDireito(ramoRaw, rito, classe.orElse(null));
        RamoJusticaNacional ramoJustica = resolveRamoJustica(competencia, rito, classe.orElse(null));

        Optional<NationalCompetenceMatrix> tribunal = resolveTribunal(tribunalInput, uf, ramoJustica, classe.orElse(null));
        String tribunalCodigo = tribunal.map(NationalCompetenceMatrix::codigo)
                .orElseGet(() -> tribunalInput == null || tribunalInput.isBlank() ? null : tribunalInput.trim().toUpperCase(Locale.ROOT));
        String tribunalNome = tribunal.map(NationalCompetenceMatrix::nome).orElse(tribunalCodigo);
        JudicialSystem judicialSystem = tribunal.map(NationalCompetenceMatrix::connectorPreferido)
                .orElseGet(() -> preferredSystem(rito, ramoJustica));

        LinkedHashSet<String> requiredPartyRoles = new LinkedHashSet<>(rito != null
                ? proceduralCatalogService.requiredParties(rito).stream().map(p -> p.code()).toList()
                : List.of());
        classe.map(TpuClasseCnj::parteCanonica)
                .map(Enum::name)
                .stream()
                .flatMap(name -> Arrays.stream(name.split("_")))
                .map(this::normalizeRole)
                .filter(s -> !s.isBlank())
                .forEach(requiredPartyRoles::add);

        List<String> competenceHints = classe.map(c -> PayloadMaps.copyTrimmedStrings(List.of(c.ramoJustica().name(), c.faixaProcedimental().name())))
                .orElseGet(() -> {
                    if (ramoJustica != null) {
                        return List.of(ramoJustica.name());
                    }
                    if (rito != null) {
                        return List.of(rito.group());
                    }
                    return List.of();
                });

        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("inputRito", ritoRaw);
        metadata.put("inputRamo", ramoRaw);
        metadata.put("inputClasseTpu", classeRaw);
        metadata.put("inputTribunal", tribunalInput);
        metadata.put("uf", uf);
        metadata.put("competenciaInput", competencia);
        metadata.put("resolvedByClasseTpu", classe.isPresent());
        metadata.put("resolvedByMatrix", tribunal.isPresent());
        metadata.put("canonicalSignalPresent", hasProceduralSignal);
        if (rito != null) {
            metadata.put("catalog", proceduralCatalogService.describe(rito));
        }
        classe.ifPresent(value -> metadata.put("classeTpu", TpuClasseCnj.toMap(value)));
        tribunal.ifPresent(value -> metadata.put("tribunalMatrix", value.toMap()));

        return new CanonicalContext(
                Instant.now(),
                rito,
                ramoDireito != null ? ramoDireito.name() : null,
                classe.map(c -> String.valueOf(c.codigoTpu())).orElse(null),
                classe.map(TpuClasseCnj::descricao).orElse(classeRaw),
                ramoJustica != null ? ramoJustica.name() : null,
                tribunalCodigo,
                tribunalNome,
                judicialSystem != null ? judicialSystem.name() : null,
                PayloadMaps.copyTrimmedStrings(requiredPartyRoles),
                PayloadMaps.copyTrimmedStrings(rito != null ? proceduralCatalogService.requiredDocuments(rito) : List.of()),
                PayloadMaps.copyTrimmedStrings(competenceHints),
                PayloadMaps.deepCopyWithoutNulls(metadata)
        );
    }

    private Optional<NationalCompetenceMatrix> resolveTribunal(String tribunalInput,
                                                               String uf,
                                                               RamoJusticaNacional ramoJustica,
                                                               TpuClasseCnj classe) {
        if (tribunalInput != null && !tribunalInput.isBlank()) {
            Optional<NationalCompetenceMatrix> explicit = NationalCompetenceMatrix.porCodigo(tribunalInput);
            if (explicit.isPresent()) {
                return explicit;
            }
        }
        if (uf != null && !uf.isBlank() && ramoJustica != null) {
            Optional<NationalCompetenceMatrix> byUf = NationalCompetenceMatrix.resolver(uf.trim().toUpperCase(Locale.ROOT), ramoJustica);
            if (byUf.isPresent()) {
                return byUf;
            }
        }
        if (classe != null) {
            if (classe.tribunaisHabilitados().contains(TpuClasseCnj.TribunalAlcada.STF)) {
                return NationalCompetenceMatrix.porCodigo("STF");
            }
            if (classe.tribunaisHabilitados().contains(TpuClasseCnj.TribunalAlcada.STJ)) {
                return NationalCompetenceMatrix.porCodigo("STJ");
            }
            if (classe.tribunaisHabilitados().contains(TpuClasseCnj.TribunalAlcada.TSE)) {
                return NationalCompetenceMatrix.porCodigo("TSE");
            }
            if (classe.tribunaisHabilitados().contains(TpuClasseCnj.TribunalAlcada.TST)) {
                return NationalCompetenceMatrix.porCodigo("TST");
            }
            if (classe.tribunaisHabilitados().contains(TpuClasseCnj.TribunalAlcada.STM)) {
                return NationalCompetenceMatrix.porCodigo("STM");
            }
        }
        return Optional.empty();
    }

    private RamoDireito resolveRamoDireito(String ramoRaw, RitoProcessual rito, TpuClasseCnj classe) {
        RamoDireito explicit = ramoRaw == null || ramoRaw.isBlank() ? null : RamoDireito.fromString(ramoRaw);
        if (explicit != null) {
            return explicit;
        }
        if (classe != null) {
            return classe.ramoDireito();
        }
        return rito != null ? rito.suggestedRamo() : null;
    }

    private RamoJusticaNacional resolveRamoJustica(String competencia, RitoProcessual rito, TpuClasseCnj classe) {
        if (classe != null) {
            return switch (classe.ramoJustica()) {
                case ELEITORAL -> RamoJusticaNacional.ELEITORAL;
                case TRABALHO -> RamoJusticaNacional.TRABALHO;
                case MILITAR -> RamoJusticaNacional.MILITAR_ESTADUAL;
                case FEDERAL -> RamoJusticaNacional.FEDERAL;
                case SUPERIOR -> RamoJusticaNacional.SUPERIOR;
                case ESTADUAL_FEDERAL, ESTADUAL -> inferByCompetenciaOuRito(competencia, rito, RamoJusticaNacional.ESTADUAL);
                case ESTADUAL_FEDERAL_SUPERIOR -> inferByCompetenciaOuRito(competencia, rito, RamoJusticaNacional.SUPERIOR);
            };
        }
        return inferByCompetenciaOuRito(competencia, rito, null);
    }

    private RamoJusticaNacional inferByCompetenciaOuRito(String competencia, RitoProcessual rito, RamoJusticaNacional fallback) {
        String seed = firstNonBlank(competencia, rito != null ? rito.group() : null);
        if (seed == null) {
            return fallback;
        }
        String normalized = normalize(seed);
        if (normalized.contains("ELEITOR")) {
            return RamoJusticaNacional.ELEITORAL;
        }
        if (normalized.contains("TRABALH")) {
            return RamoJusticaNacional.TRABALHO;
        }
        if (normalized.contains("MILITAR")) {
            return rito != null && rito.name().contains("UNIAO") ? RamoJusticaNacional.MILITAR_SUPERIOR : RamoJusticaNacional.MILITAR_ESTADUAL;
        }
        if (normalized.contains("FEDERAL") || normalized.contains("PREVIDENCI")) {
            return RamoJusticaNacional.FEDERAL;
        }
        if (normalized.contains("SUPERIOR") || normalized.contains("STJ") || normalized.contains("STF") || normalized.contains("TSE") || normalized.contains("STM") || normalized.contains("TST")) {
            return RamoJusticaNacional.SUPERIOR;
        }
        return fallback;
    }

    private JudicialSystem preferredSystem(RitoProcessual rito, RamoJusticaNacional ramoJustica) {
        if (ramoJustica == RamoJusticaNacional.ELEITORAL || rito != null && rito.isEleitoral()) {
            return JudicialSystem.PJE;
        }
        if (ramoJustica == RamoJusticaNacional.TRABALHO || rito != null && rito.isTrabalhista()) {
            return JudicialSystem.PJE;
        }
        if (ramoJustica == RamoJusticaNacional.FEDERAL || rito != null && rito.isPrevidenciario()) {
            return JudicialSystem.EPROC;
        }
        if (ramoJustica == RamoJusticaNacional.MILITAR_ESTADUAL || ramoJustica == RamoJusticaNacional.MILITAR_SUPERIOR || rito != null && rito.isMilitar()) {
            return JudicialSystem.MNI;
        }
        return null;
    }

    private String normalizeRole(String value) {
        return normalize(value).replace(' ', '_').toUpperCase(Locale.ROOT);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String text(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static Map<String, Object> copyNonNull(Map<String, Object> values) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        if (values != null) {
            for (Map.Entry<String, Object> entry : values.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    out.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return PayloadMaps.deepCopyWithoutNulls(out);
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("[^A-Za-z0-9 ]", " ")
                .replaceAll(" +", " ")
                .toUpperCase(Locale.ROOT)
                .trim();
    }
}
