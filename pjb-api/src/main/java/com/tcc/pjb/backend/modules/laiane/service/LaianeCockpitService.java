package com.tcc.pjb.backend.modules.laiane.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.ai.juridica.v3.core.LegalRitosEngine;
import com.tcc.pjb.backend.core.procedural.CanonicalRitoSelector;
import com.tcc.pjb.backend.core.procedural.CanonicalRitoSelector.SelectedRito;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.modules.laiane.dto.legal.LaianeCockpitRequest;
import com.tcc.pjb.backend.modules.laiane.dto.legal.LaianeCockpitResponse;
import com.tcc.pjb.backend.modules.laiane.repository.LaianeUserPreferenceRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class LaianeCockpitService {

    private static final TypeReference<Map<String, Object>> MAP_REF = new TypeReference<>() {};
    private final LegalRitosEngine legalRitosEngine;
    private final LaianePlaybookService playbookService;
    private final CurrentUserService currentUserService;
    private final CanonicalRitoSelector canonicalRitoSelector;
    private final LaianeUserPreferenceRepository laianeUserPreferenceRepository;
    private final ObjectMapper objectMapper;

    public LaianeCockpitService(LegalRitosEngine legalRitosEngine,
                                LaianePlaybookService playbookService,
                                CurrentUserService currentUserService,
                                CanonicalRitoSelector canonicalRitoSelector,
                                LaianeUserPreferenceRepository laianeUserPreferenceRepository,
                                ObjectMapper objectMapper) {
        this.legalRitosEngine = Objects.requireNonNull(legalRitosEngine);
        this.playbookService = Objects.requireNonNull(playbookService);
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.canonicalRitoSelector = Objects.requireNonNull(canonicalRitoSelector);
        this.laianeUserPreferenceRepository = Objects.requireNonNull(laianeUserPreferenceRepository);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    public LaianeCockpitResponse cockpit(LaianeCockpitRequest req) {
        Usuario u = currentUserService.getOptional().orElse(null);
        Map<String, Object> preferences = loadPreferences(u);
        Map<String, Object> ctx = mergeCtx(req != null ? req.getCtx() : Map.of(), preferences);
        String ritoPreferido = firstNonBlank(req != null ? req.getRito() : null, stringValue(preferences.get("rito")));
        String roleHint = firstNonBlank(req != null ? req.getRole() : null, stringValue(preferences.get("role")));
        Map<String, Object> ritoOut = legalRitosEngine.inferRito(ctx);
        RitoProcessual heuristicRito = RitoProcessual.tryParse(ritoPreferido).orElse(null);
        SelectedRito selectedRito = canonicalRitoSelector.select(buildCanonicalPayload(ctx, ritoPreferido, ritoOut), heuristicRito, "laiane_cockpit_service");
        var rito = selectedRito.rito();
        TipoUsuario role = u != null ? u.getTipoUsuario() : null;
        if (roleHint != null) {
            try {
                role = TipoUsuario.fromPerfil(roleHint);
            } catch (Exception ignored) {
            }
        }
        return LaianeCockpitResponse.builder()
                .ritoInference(enrichInference(ritoOut, selectedRito, preferences))
                .playbook(playbookService.topBenefits(role, rito != null ? rito.name() : null, 12))
                .differentiators(coreDifferentiators(rito != null ? rito.name() : null, preferences))
                .build();
    }

    private Map<String, Object> mergeCtx(Map<String, Object> ctx, Map<String, Object> preferences) {
        LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
        Object preferredCtx = preferences.get("ctx");
        if (preferredCtx instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    merged.put(String.valueOf(entry.getKey()), entry.getValue());
                }
            }
        }
        if (ctx != null) {
            for (Map.Entry<String, Object> entry : ctx.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    merged.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return immutableCopy(merged);
    }

    private Map<String, Object> buildCanonicalPayload(Map<String, Object> ctx, String rito, Map<String, Object> ritoOut) {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>(ctx == null ? Map.of() : ctx);
        if (rito != null && !rito.isBlank()) {
            payload.put("rito", rito);
        } else if (ritoOut != null && ritoOut.get("rito") != null) {
            payload.put("rito", String.valueOf(ritoOut.get("rito")));
        }
        if (ritoOut != null && ritoOut.get("ramoSugerido") != null) {
            payload.putIfAbsent("ramoDireito", String.valueOf(ritoOut.get("ramoSugerido")));
            payload.putIfAbsent("materia", String.valueOf(ritoOut.get("ramoSugerido")));
        }
        return payload;
    }

    private Map<String, Object> enrichInference(Map<String, Object> ritoOut, SelectedRito selectedRito, Map<String, Object> preferences) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>(ritoOut == null ? Map.of() : ritoOut);
        out.put("selectedRito", selectedRito != null && selectedRito.rito() != null ? selectedRito.rito().name() : null);
        out.put("selectionStatus", selectedRito != null ? selectedRito.status() : null);
        out.put("selection", selectedRito != null ? selectedRito.toMap() : Map.of());
        if (!preferences.isEmpty()) {
            out.put("preferencesLoaded", true);
        }
        return immutableCopy(out);
    }

    private Map<String, Object> loadPreferences(Usuario usuario) {
        if (usuario == null || usuario.getId() == null) {
            return Map.of();
        }
        return laianeUserPreferenceRepository.findByUsuario_Id(usuario.getId())
                .map(pref -> pref.getPreferencesJson())
                .filter(json -> !json.isBlank())
                .map(this::parsePreferences)
                .orElse(Map.of());
    }

    private Map<String, Object> parsePreferences(String json) {
        try {
            Map<String, Object> parsed = objectMapper.readValue(json, MAP_REF);
            return immutableCopy(parsed);
        } catch (Exception ex) {
            return Map.of();
        }
    }

    private static Map<String, Object> immutableCopy(Map<String, Object> input) {
        if (input == null || input.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, Object> copy = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : input.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                copy.put(entry.getKey(), entry.getValue());
            }
        }
        return copy.isEmpty() ? Map.of() : Map.copyOf(copy);
    }

    private static List<String> coreDifferentiators(String ritoName, Map<String, Object> preferences) {
        List<String> dif = new ArrayList<>();
        dif.add("Ritos e checklists unificados: o mesmo processo anda por fila de trabalho com SLAs e travas quando necessário.");
        dif.add("Motor determinístico e auditável: cada sugestão vem com base legal e trilha de decisão institucional.");
        dif.add("Modelos inteligentes com validação: minutas geradas com campos obrigatórios, consistência e alertas de riscos.");
        dif.add("Inbox por papel: tarefas aparecem por atribuição e por território quando cabível.");
        dif.add("Provas digitais e cadeia de custódia: anexos com hash e trilha de integridade.");
        dif.add("Protocolos envelopados: cada protocolo recebe payload canonicalizado com hash verificável.");
        dif.add("Observabilidade e anti-queda: fila, reprocessamento, idempotência e eventos auditáveis.");
        if (ritoName != null && ritoName.startsWith("ELEITORAL")) {
            dif.add("Eleitoral: janelas ultracurtas com relógio processual e alertas por marcos críticos.");
            dif.add("Eleitoral: trilha probatória de propaganda digital com preservação e hash.");
        }
        if (ritoName != null && ritoName.startsWith("PENAL")) {
            dif.add("Penal: cadeia de custódia e checklist de diligências com histórico íntegro de atuação.");
        }
        if (ritoName != null && (ritoName.startsWith("FAZENDA") || ritoName.startsWith("TRIBUT"))) {
            dif.add("Fazenda e tributário: cálculo automático com dupla checagem e revisão auditável.");
        }
        if (preferences.containsKey("formalismo") || preferences.containsKey("linguagem")) {
            dif.add("Preferências operacionais carregadas do usuário para ajustar foco e cadência do cockpit.");
        }
        return dif;
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

    private static String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }
}
