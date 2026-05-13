package com.tcc.pjb.backend.ai.juridica.v3.core;

import com.tcc.pjb.backend.core.procedural.CanonicalRitoSelector;
import com.tcc.pjb.backend.service.rito.RitoPackService;
import com.tcc.pjb.backend.service.rito.model.RitoDefinition;
import com.tcc.pjb.backend.service.rito.model.RitoStage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class LegalRitosEngine {

    private final RitoPackService ritoPackService;
    private final CanonicalRitoSelector canonicalRitoSelector;

    public LegalRitosEngine(RitoPackService ritoPackService,
                            CanonicalRitoSelector canonicalRitoSelector) {
        this.ritoPackService = Objects.requireNonNull(ritoPackService);
        this.canonicalRitoSelector = Objects.requireNonNull(canonicalRitoSelector);
    }

    public Map<String, Object> inferRito(Map<String, Object> ctx) {
        Map<String, Object> safe = sanitize(ctx);
        var selectedRito = canonicalRitoSelector.select(safe, extractHeuristicRito(safe), "legal_ritos_engine");
        var rito = selectedRito.rito();
        var defOpt = ritoPackService.get(rito);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("rito", rito != null ? rito.name() : null);
        out.put("selection", selectedRito.toMap());
        out.put("canonicalContext", selectedRito.canonicalContext() != null ? selectedRito.canonicalContext().toMap() : Map.of());
        out.put("sanityGate", selectedRito.sanityGate() != null ? selectedRito.sanityGate().toMap() : Map.of());

        if (defOpt.isEmpty()) {
            out.put("status", selectedRito.status() != null ? selectedRito.status() : "rito_pack_missing");
            out.put("observacoes", "RitoPack não possui definição para o rito efetivo selecionado.");
            out.put("passos", List.of(
                    "Confirmar competência, classe TPU e rito antes do protocolo",
                    "Validar documentos obrigatórios e representação processual",
                    "Montar cronologia e mapa de prova/ônus",
                    "Planejar prazos, atos subsequentes e contingências"
            ));
            out.put("requiredInputFields", recommendedFieldsFor(rito != null ? rito.name() : null));
            return Collections.unmodifiableMap(out);
        }

        RitoDefinition def = defOpt.get();
        out.put("status", "ok");
        out.put("title", def.getTitle());
        out.put("ramoSugerido", def.getRamoSugerido());

        List<Map<String, Object>> stages = new ArrayList<>();
        if (def.getStages() != null) {
            for (RitoStage stage : def.getStages()) {
                if (stage == null) {
                    continue;
                }
                Map<String, Object> st = new LinkedHashMap<>();
                st.put("fase", stage.getFase());
                st.put("allowedNext", stage.getAllowedNext() == null ? List.of() : stage.getAllowedNext());
                st.put("work", stage.getWork() == null ? List.of() : stage.getWork());
                stages.add(st);
            }
        }
        out.put("stages", List.copyOf(stages));
        out.put("passos", computeNextSteps(def, 5));
        out.put("requiredInputFields", recommendedFieldsFor(rito != null ? rito.name() : null));
        return Collections.unmodifiableMap(out);
    }

    private List<String> computeNextSteps(RitoDefinition def, int limit) {
        if (def == null || def.getStages() == null || def.getStages().isEmpty()) {
            return List.of();
        }
        RitoStage first = def.getStages().get(0);
        if (first == null || first.getWork() == null) {
            return List.of();
        }
        return first.getWork().stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(work -> work.getPriority() == null ? 99 : work.getPriority()))
                .limit(limit)
                .map(work -> work.getTitle() == null ? String.valueOf(work.getCode()) : work.getTitle())
                .toList();
    }

    private List<String> recommendedFieldsFor(String ritoName) {
        List<String> base = new ArrayList<>(List.of(
                "tribunal",
                "comarca",
                "vara_ou_unidade",
                "classe",
                "partes",
                "resumo",
                "fundamentos",
                "pedidos",
                "provas"
        ));
        if (ritoName == null || ritoName.isBlank()) {
            return base;
        }
        if (ritoName.startsWith("ELEITORAL")) {
            base.addAll(List.of("pleito_ano", "municipio", "cargo", "data_fato", "provas_digitais"));
        } else if (ritoName.startsWith("MILITAR")) {
            base.addAll(List.of("organizacao_militar", "posto_graduacao", "fato", "testemunhas", "laudos"));
        } else if (ritoName.startsWith("PREVIDENCIARIO")) {
            base.addAll(List.of("nb_beneficio", "cnis", "carencia", "incapacidade", "laudos_medicos"));
        } else if (ritoName.startsWith("TRIBUTARIO") || ritoName.startsWith("FAZENDA")) {
            base.addAll(List.of("cda", "auto_infracao", "tributo", "periodo", "valores", "selic"));
        } else if (ritoName.startsWith("PENAL") || ritoName.startsWith("TRIBUNAL_JURI") || ritoName.startsWith("PROCEDIMENTO_PENAL")) {
            base.addAll(List.of("fato", "tipificacao", "provas", "testemunhas", "medidas_cautelares"));
        }
        return List.copyOf(base);
    }

    private String extractHeuristicRito(Map<String, Object> ctx) {
        return firstNonBlank(
                asString(ctx.get("rito")),
                asString(ctx.get("rito_processual")),
                asString(ctx.get("procedimento")),
                asString(ctx.get("classe_rito"))
        );
    }

    private static Map<String, Object> sanitize(Map<String, Object> ctx) {
        Map<String, Object> safe = new LinkedHashMap<>();
        if (ctx == null) {
            return safe;
        }
        for (Map.Entry<String, Object> entry : ctx.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            safe.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return safe;
    }

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
