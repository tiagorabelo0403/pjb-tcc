package com.tcc.pjb.backend.core.comunicacao.institucional.panel.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAccessProfileCatalogEntry;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOperationalProfileProjection;
import com.tcc.pjb.backend.core.comunicacao.institucional.processual.domain.InstitutionalProcessWorkspace;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

final class InstitutionalPanelProvisioningSupport {

    boolean requiresOpinionFlow(InstitutionalOperationalProfileProjection profile,
                                InstitutionalAccessProfileCatalogEntry catalogEntry,
                                InstitutionalProcessWorkspace workspace) {
        String processProfile = normalize(profile == null ? null : profile.processProfile());
        String catalogProfile = catalogEntry == null || catalogEntry.processProfile() == null ? null : catalogEntry.processProfile().name();
        return containsToken(processProfile, "PROMOTOR", "DEFENSOR", "PROCURADOR", "ASSESSOR", "SECRETARIA_FORUM", "CONTADOR_JUDICIAL", "PSICOLOGO_JUDICIAL", "ASSISTENTE_SOCIAL_JUDICIAL")
                || containsToken(catalogProfile, "PROMOTOR", "DEFENSOR", "PROCURADOR", "ASSESSOR", "SECRETARIA_FORUM", "CONTADOR_JUDICIAL", "PSICOLOGO_JUDICIAL", "ASSISTENTE_SOCIAL_JUDICIAL")
                || hasWorkspaceAction(workspace, "PARECER", "MANIFESTACAO", "MINUTA", "DEFESA", "INFORMACOES");
    }

    boolean requiresCalculator(InstitutionalOperationalProfileProjection profile,
                               InstitutionalAccessProfileCatalogEntry catalogEntry,
                               InstitutionalProcessWorkspace workspace) {
        String processProfile = normalize(profile == null ? null : profile.processProfile());
        String catalogProfile = catalogEntry == null || catalogEntry.processProfile() == null ? null : catalogEntry.processProfile().name();
        return containsToken(processProfile, "PROMOTOR", "DEFENSOR", "PROCURADOR", "ASSESSOR", "SECRETARIA_FORUM", "CONTADOR_JUDICIAL", "TECNICO_INSTITUCIONAL")
                || containsToken(catalogProfile, "PROMOTOR", "DEFENSOR", "PROCURADOR", "ASSESSOR", "SECRETARIA_FORUM", "CONTADOR_JUDICIAL", "TECNICO_INSTITUCIONAL")
                || hasWorkspaceAction(workspace, "CALCULO", "CALCULADORA", "LIQUIDACAO");
    }

    boolean hasWorkspaceAction(InstitutionalProcessWorkspace workspace, String... signals) {
        if (workspace == null || workspace.actions().isEmpty()) {
            return false;
        }
        return workspace.actions().stream().anyMatch(item -> containsAny(item.code(), item.title(), item.description(), signals));
    }

    boolean containsDomainSignals(Set<String> sections,
                                  Set<String> actions,
                                  Set<String> tabs,
                                  String... signals) {
        LinkedHashSet<String> tokens = new LinkedHashSet<>();
        tokens.addAll(sections);
        tokens.addAll(actions);
        tokens.addAll(tabs);
        if (tokens.isEmpty() || signals == null || signals.length == 0) {
            return false;
        }
        for (String token : tokens) {
            String normalizedToken = normalize(token);
            for (String signal : signals) {
                if (normalizedToken.contains(normalize(signal))) {
                    return true;
                }
            }
        }
        return false;
    }

    int score(InstitutionalAccessProfileCatalogEntry entry, InstitutionalOperationalProfileProjection profile) {
        if (entry.panel() == null || !entry.panel().name().equalsIgnoreCase(profile.panelCode())) {
            return -1;
        }
        int score = 100;
        if (equalsNormalized(entry.processProfile() == null ? null : entry.processProfile().name(), profile.processProfile())) {
            score += 40;
        }
        if (equalsNormalized(entry.nominationRole() == null ? null : entry.nominationRole().name(), profile.nominationRole())) {
            score += 25;
        }
        if (equalsNormalized(entry.trustFloor() == null ? null : entry.trustFloor().name(), profile.trustFloor())) {
            score += 10;
        }
        if (containsNormalized(entry.codigo(), profile.organizationScope())) {
            score += 8;
        }
        if (containsNormalized(entry.codigo(), profile.accessLaneKind())) {
            score += 5;
        }
        if (containsNormalized(entry.nomeExibicao(), profile.funcaoOperacional())) {
            score += 4;
        }
        return score;
    }

    String inferScopeFromProfileCode(String profileCode) {
        if (isBlank(profileCode) || !profileCode.contains("__")) {
            return null;
        }
        return profileCode.substring(0, profileCode.indexOf("__")).trim();
    }

    boolean containsAny(String left, String middle, String right, String... signals) {
        return containsToken(left, signals) || containsToken(middle, signals) || containsToken(right, signals);
    }


    boolean containsToken(String value, String... signals) {
        if (value == null || value.isBlank() || signals == null || signals.length == 0) {
            return false;
        }
        String normalized = normalize(value);
        for (String signal : signals) {
            if (normalized.contains(normalize(signal))) {
                return true;
            }
        }
        return false;
    }

    boolean equalsNormalized(String left, String right) {
        return normalize(left).equals(normalize(right)) && !normalize(left).isEmpty();
    }

    boolean containsNormalized(String container, String candidate) {
        String normalizedContainer = normalize(container);
        String normalizedCandidate = normalize(candidate);
        return !normalizedContainer.isEmpty() && !normalizedCandidate.isEmpty() && normalizedContainer.contains(normalizedCandidate);
    }

    String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }

    String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (!isBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    String nullSafe(String value) {
        return isBlank(value) ? "NA" : value.trim();
    }

    boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
