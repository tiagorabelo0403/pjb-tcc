package com.tcc.pjb.backend.core.processual.routing;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class PreventionConstraintResolver {

    public PreventionConstraintProfile resolve(NationalProcessRoutingService.RoutingCommand command,
                                               TerritorialRoutingProfile territorial) {
        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        LinkedHashSet<String> reviewChecklist = new LinkedHashSet<>();

        String reference = firstNonBlank(command.preventionReference(), command.processoReferencia());
        String relationMode;
        String bindingStrength;
        String triageOverride;
        boolean strictLock;

        if (command.redistribuicaoImpedimento()) {
            relationMode = "IMPEDIMENTO_SUSPEICAO";
            bindingStrength = "ESTRITA";
            triageOverride = "GATE_IMPEDIMENTO";
            strictLock = true;
            warnings.add("Redistribuição por impedimento exige bloqueio do órgão anterior e nova trilha de sorteio controlado.");
        } else if (notBlank(command.preventionReference())) {
            relationMode = "PREVENCAO_REFERENCIADA";
            bindingStrength = "ESTRITA";
            triageOverride = "GATE_PREVENCAO_REFERENCIADA";
            strictLock = true;
            fundamentos.add("Prevenção expressa impõe vinculação prioritária ao órgão e à unidade já referenciados.");
        } else if (command.dependenciaDeclarada()) {
            relationMode = "DEPENDENCIA_PROCESSUAL";
            bindingStrength = "ALTA";
            triageOverride = "GATE_DEPENDENCIA_REFERENCIADA";
            strictLock = true;
            fundamentos.add("Dependência processual requer preservação de acervo e de identidade funcional da tramitação.");
        } else if (command.conexaoDeclarada() && command.continenciaDeclarada()) {
            relationMode = "CONEXAO_CONTINENCIA";
            bindingStrength = "ALTA";
            triageOverride = "GATE_RELACAO_COMPLEXA";
            strictLock = true;
            warnings.add("Conexão e continência simultâneas exigem saneamento conjunto da prevenção e do processo líder.");
        } else if (command.conexaoDeclarada()) {
            relationMode = "CONEXAO_DECLARADA";
            bindingStrength = "MODERADA";
            triageOverride = "GATE_CONEXAO_DECLARADA";
            strictLock = false;
        } else if (command.continenciaDeclarada()) {
            relationMode = "CONTINENCIA_DECLARADA";
            bindingStrength = "ALTA";
            triageOverride = "GATE_CONTINENCIA_DECLARADA";
            strictLock = true;
        } else {
            relationMode = "AUTONOMA";
            bindingStrength = "BAIXA";
            triageOverride = "GATE_AUTONOMO";
            strictLock = false;
        }

        boolean autoAttachAllowed = "AUTONOMA".equals(relationMode) || notBlank(reference);
        String normalizedReference = normalizeReference(reference);
        if (!"AUTONOMA".equals(relationMode) && !autoAttachAllowed) {
            warnings.add("Vínculo processual sem referência explícita; distribuição automática deve permanecer bloqueada até saneamento.");
            reviewChecklist.add("Informar número CNJ ou referência interna do processo prevento/principal.");
        }
        if (command.grau() != null && command.grau() != com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao.PRIMEIRO_GRAU && !"AUTONOMA".equals(relationMode)) {
            fundamentos.add("Relação processual em instância colegiada exige convergência entre relator, órgão fracionário e prevenção registral.");
            reviewChecklist.add("Validar relator prevento, órgão fracionário e histórico de distribuição associado ao vínculo.");
            strictLock = true;
        }
        if (territorial != null && territorial.territorialLabel() != null) {
            fundamentos.add("Fingerprint relacional ancorada em território: " + territorial.territorialLabel() + '.');
        }

        String preventionFingerprint = buildFingerprint(relationMode, normalizedReference, territorial, command.grau() == null ? null : command.grau().name(), "PREV");
        String dependencyFingerprint = buildFingerprint(relationMode, normalizedReference, territorial, command.grau() == null ? null : command.grau().name(), "DEP");

        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("referenceSource", notBlank(command.preventionReference()) ? "PREVENTION_REFERENCE" : notBlank(command.processoReferencia()) ? "PROCESSO_REFERENCIA" : "SEM_REFERENCIA");
        metadata.put("territoryToken", territorial != null ? territorial.territoryToken() : null);
        metadata.put("grau", command.grau() != null ? command.grau().name() : null);
        metadata.put("strictRegistryLock", strictLock);
        metadata.put("autoAttachAllowed", autoAttachAllowed);
        metadata.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);

        return new PreventionConstraintProfile(
                relationMode,
                reference,
                normalizedReference,
                preventionFingerprint,
                dependencyFingerprint,
                bindingStrength,
                triageOverride,
                strictLock,
                autoAttachAllowed,
                List.copyOf(warnings),
                List.copyOf(fundamentos),
                List.copyOf(reviewChecklist),
                metadata
        );
    }

    private String buildFingerprint(String relationMode,
                                    String normalizedReference,
                                    TerritorialRoutingProfile territorial,
                                    String grau,
                                    String prefix) {
        return String.join("|",
                prefix,
                firstNonBlank(relationMode, "AUTONOMA"),
                firstNonBlank(normalizedReference, "SEM_REFERENCIA"),
                territorial == null ? "SEM_TERRITORIO" : firstNonBlank(territorial.territoryToken(), "SEM_TERRITORIO"),
                firstNonBlank(grau, "SEM_GRAU"));
    }

    private String normalizeReference(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return raw.trim()
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("(^_|_$)", "");
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (notBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
