package com.tcc.pjb.backend.core.comunicacao.institucional.panel.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalProcessProfile;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOperationalProfileProjection;
import com.tcc.pjb.backend.core.comunicacao.institucional.processual.domain.InstitutionalProcessWorkspace;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

class InstitutionalHearingSchedulingScopeSupport {
    String buildSchedulingScopeKey(InstitutionalOperationalProfileProjection profile,
                                           InstitutionalProcessWorkspace workspace,
                                           String scope,
                                           InstitutionalProcessProfile processProfile) {
        String tribunal = firstNonBlank(profile == null ? null : profile.responsibleTribunalCode(), profile == null ? null : profile.orgaoSigla(), "SEM_TRIBUNAL");
        String comarca = resolveComarcaScope(profile);
        String unidade = firstNonBlank(profile == null ? null : profile.unidadeCodigo(), profile == null ? null : profile.responsibleUnitCode(), "SEM_UNIDADE");
        String caixa = firstNonBlank(profile == null ? null : profile.caixaCodigo(), "SEM_CAIXA");
        String axis = resolveJurisdictionAxis(workspace, scope);
        String unitCluster = resolveUnitCluster(profile, workspace, scope);
        String profileCode = processProfile == null ? "SEM_PERFIL" : processProfile.name();
        return normalize(tribunal)
                + '|' + normalize(comarca)
                + '|' + normalize(unidade)
                + '|' + normalize(unitCluster)
                + '|' + normalize(caixa)
                + '|' + normalize(axis)
                + '|' + normalize(profileCode);
    }

    String buildRiteQueueScopeKey(InstitutionalOperationalProfileProjection profile,
                                          InstitutionalProcessWorkspace workspace,
                                          String schedulingScopeKey,
                                          String riteCode,
                                          String specializationAxis) {
        String territorial = resolveComarcaScope(profile);
        String ramo = resolveJurisdictionAxis(workspace, null);
        String unitCluster = resolveUnitCluster(profile, workspace, specializationAxis);
        return normalize(schedulingScopeKey)
                + '|' + normalize(territorial)
                + '|' + normalize(ramo)
                + '|' + normalize(unitCluster)
                + '|' + normalize(riteCode)
                + '|' + normalize(specializationAxis);
    }

    List<String> resolveOperationalQueues(InstitutionalOperationalProfileProjection profile,
                                                  InstitutionalProcessWorkspace workspace,
                                                  String scope,
                                                  InstitutionalProcessProfile processProfile,
                                                  boolean secretariat,
                                                  boolean scheduler,
                                                  boolean management,
                                                  boolean prisonFlow) {
        LinkedHashSet<String> queues = new LinkedHashSet<>();
        String unidade = firstNonBlank(profile == null ? null : profile.unidadeCodigo(), profile == null ? null : profile.responsibleUnitCode(), "SEM_UNIDADE");
        String caixa = firstNonBlank(profile == null ? null : profile.caixaCodigo(), "SEM_CAIXA");
        String comarca = resolveComarcaScope(profile);
        String unitCluster = resolveUnitCluster(profile, workspace, scope);
        String logicalUnit = resolveLogicalUnitKey(profile);
        if (secretariat) {
            queues.add("PAUTA:" + normalize(unidade) + ':' + normalize(caixa));
            queues.add("INTIMACOES_AUDIENCIA:" + normalize(unidade));
            queues.add("PREPARO_CONCLUSAO_SECRETARIA:" + normalize(logicalUnit));
        }
        if (scheduler) {
            queues.add("AGENDA_AUDIENCIA:" + normalize(unidade));
            queues.add("SALAS_AUDIENCIA:" + normalize(unidade));
            queues.add("BLOCO_PAUTA_VARA:" + normalize(unitCluster));
        }
        if (management) {
            queues.add("SUPERVISAO_PAUTA:" + normalize(unidade));
            queues.add("SUPERVISAO_ISOLAMENTO_UNIDADE:" + normalize(logicalUnit));
        }
        if (prisonFlow) {
            queues.add("CUSTODIA_APRESENTACAO:" + normalize(unidade));
            queues.add("ESCOLTA_E_UNIDADE_PRISIONAL:" + normalize(unidade));
        }
        String branch = resolveJurisdictionAxis(workspace, scope);
        queues.add("FILTRO_COMPETENCIA:" + normalize(branch));
        queues.add("FILTRO_COMARCA:" + normalize(comarca));
        queues.add("FILTRO_UNIDADE_LOGICA:" + normalize(logicalUnit));
        queues.add("FILTRO_VARA_CLUSTER:" + normalize(unitCluster));
        return List.copyOf(queues);
    }

    List<String> resolveSegregationGuards(InstitutionalOperationalProfileProjection profile,
                                                  InstitutionalProcessWorkspace workspace,
                                                  String scope,
                                                  boolean requiresUnitIsolation,
                                                  boolean secretariat,
                                                  boolean scheduler,
                                                  boolean management,
                                                  boolean prisonFlow) {
        LinkedHashSet<String> guards = new LinkedHashSet<>();
        String unidade = firstNonBlank(profile == null ? null : profile.unidadeCodigo(), profile == null ? null : profile.responsibleUnitCode(), "SEM_UNIDADE");
        String caixa = firstNonBlank(profile == null ? null : profile.caixaCodigo(), "SEM_CAIXA");
        String comarca = resolveComarcaScope(profile);
        String logicalUnit = resolveLogicalUnitKey(profile);
        String unitCluster = resolveUnitCluster(profile, workspace, scope);
        if (requiresUnitIsolation) {
            guards.add("unidade=" + normalize(unidade));
            guards.add("caixa=" + normalize(caixa));
            guards.add("foro_comarca=" + normalize(comarca));
            guards.add("unidade_logica=" + normalize(logicalUnit));
            guards.add("vara_cluster=" + normalize(unitCluster));
        }
        guards.add("escopo=" + normalize(scope));
        guards.add("ramo=" + normalize(resolveJurisdictionAxis(workspace, scope)));
        if (secretariat) {
            guards.add("segregacao_secretaria_vs_gabinete");
            guards.add("secretaria_opera_apenas_unidade_e_vara_da_lotacao");
            if (matchesAny(scope, "CENTRAL_AUDIENCIA", "CENTRAL_AUDIENCIAS", "CEJUSC", "PAUTA")) {
                guards.add("segregacao_central_de_audiencias");
                guards.add("central_de_audiencias_respeita_bloco_de_vara_e_especializacao");
            }
        }
        if (scheduler) {
            guards.add("segregacao_central_de_audiencias");
            guards.add("central_de_audiencias_respeita_bloco_de_vara_e_especializacao");
        }
        if (management) {
            guards.add("gestao_nao_substitui_ato_jurisdicional");
            guards.add("gestao_nao_rompe_isolamento_entre_varas_homologadas");
        }
        if (prisonFlow) {
            guards.add("custodia_requer_confirmacao_de_apresentacao");
        }
        return List.copyOf(guards);
    }

    List<String> mergeSegregationGuards(List<String> base, String... extra) {
        LinkedHashSet<String> guards = new LinkedHashSet<>(base == null ? List.of() : base);
        if (extra != null) {
            for (String item : extra) {
                if (item != null && !item.isBlank()) {
                    guards.add(normalize(item));
                }
            }
        }
        return List.copyOf(guards);
    }

    String resolveComarcaScope(InstitutionalOperationalProfileProjection profile) {
        return firstNonBlank(profile == null ? null : profile.responsibleComarca(), profile == null ? null : profile.unidadeNome(), "SEM_COMARCA");
    }

    String resolveLogicalUnitKey(InstitutionalOperationalProfileProjection profile) {
        String tribunal = firstNonBlank(profile == null ? null : profile.responsibleTribunalCode(), profile == null ? null : profile.orgaoSigla(), "SEM_TRIBUNAL");
        String comarca = resolveComarcaScope(profile);
        String unidade = firstNonBlank(profile == null ? null : profile.unidadeCodigo(), profile == null ? null : profile.responsibleUnitCode(), "SEM_UNIDADE");
        return normalize(tribunal) + '_' + normalize(comarca) + '_' + normalize(unidade);
    }

    String resolveUnitCluster(InstitutionalOperationalProfileProjection profile,
                                      InstitutionalProcessWorkspace workspace,
                                      String fallbackAxis) {
        String unitName = firstNonBlank(
                profile == null ? null : profile.unidadeNome(),
                profile == null ? null : profile.responsibleUnitName(),
                profile == null ? null : profile.responsibleUnitCode(),
                profile == null ? null : profile.unidadeCodigo(),
                "UNIDADE"
        );
        String normalized = normalize(unitName);
        String branch = resolveJurisdictionAxis(workspace, fallbackAxis);
        String specialization = resolveSpecializationCluster(normalized, branch);
        String varaNumber = resolveVaraNumber(normalized);
        if (normalized.contains("UPJ")) {
            return "UPJ_" + specialization;
        }
        if (varaNumber != null) {
            return "VARA_" + varaNumber + '_' + specialization;
        }
        if (normalized.contains("JUIZADO")) {
            return "JUIZADO_" + specialization;
        }
        if (normalized.contains("CEJUSC")) {
            return "CEJUSC_" + specialization;
        }
        return specialization + "_UNIDADE";
    }

    private String resolveVaraNumber(String normalizedUnitName) {
        if (normalizedUnitName == null || normalizedUnitName.isBlank()) {
            return null;
        }
        java.util.regex.Matcher ordinal = java.util.regex.Pattern.compile("(?:^|\\s)(\\d{1,2})(?:A|ª|O|º)?\\s*VARA").matcher(normalizedUnitName);
        if (ordinal.find()) {
            return ordinal.group(1);
        }
        java.util.regex.Matcher suffix = java.util.regex.Pattern.compile("VARA\\s*(\\d{1,2})").matcher(normalizedUnitName);
        if (suffix.find()) {
            return suffix.group(1);
        }
        return null;
    }

    private String resolveSpecializationCluster(String normalizedUnitName, String branch) {
        if (matchesAny(normalizedUnitName, "JECRIM")) {
            return "JECRIM";
        }
        if (matchesAny(normalizedUnitName, "JUIZADO", "JEC")) {
            return "JUIZADOS";
        }
        if (matchesAny(normalizedUnitName, "CUSTODIA")) {
            return "CUSTODIA";
        }
        if (matchesAny(normalizedUnitName, "FAMILIA", "SUCESSOES")) {
            return "FAMILIA";
        }
        if (matchesAny(normalizedUnitName, "INFANCIA")) {
            return "INFANCIA";
        }
        if (matchesAny(normalizedUnitName, "FAZENDA", "TRIBUT")) {
            return "FAZENDA";
        }
        if (matchesAny(normalizedUnitName, "EXECUCAO FISCAL", "EXECUCAO_FISCAL")) {
            return "EXECUCAO_FISCAL";
        }
        if (matchesAny(normalizedUnitName, "PENAL", "CRIMINAL", "JURI")) {
            return "PENAL";
        }
        if (matchesAny(normalizedUnitName, "TRABALHO", "TRABALHISTA")) {
            return "TRABALHISTA";
        }
        if (matchesAny(normalizedUnitName, "ELEITOR")) {
            return "ELEITORAL";
        }
        if (matchesAny(normalizedUnitName, "MILITAR")) {
            return "MILITAR";
        }
        if (matchesAny(normalizedUnitName, "FEDERAL")) {
            return "FEDERAL";
        }
        if (matchesAny(normalizedUnitName, "CIVEL", "CÍVEL")) {
            return "CIVEL";
        }
        return matchesAny(branch, "PENAL", "CRIMINAL") ? "PENAL" : matchesAny(branch, "TRABALHISTA") ? "TRABALHISTA" : matchesAny(branch, "ELEITORAL") ? "ELEITORAL" : matchesAny(branch, "MILITAR") ? "MILITAR" : matchesAny(branch, "FAZENDA", "TRIBUT") ? "FAZENDA" : "CIVEL";
    }

    String resolveJurisdictionAxis(InstitutionalProcessWorkspace workspace, String fallbackAxis) {
        return firstNonBlank(workspace == null ? null : workspace.ramoDireito(), workspace == null ? null : workspace.ritoProcessual(), fallbackAxis, "SEM_RAMO");
    }

    boolean hasOperationalUnitContext(InstitutionalOperationalProfileProjection profile) {
        return profile != null && ((profile.unidadeCodigo() != null && !profile.unidadeCodigo().isBlank())
                || (profile.caixaCodigo() != null && !profile.caixaCodigo().isBlank())
                || (profile.responsibleUnitCode() != null && !profile.responsibleUnitCode().isBlank()));
    }

    boolean isFederalScope(String scope,
                                   InstitutionalOperationalProfileProjection profile,
                                   InstitutionalProcessWorkspace workspace,
                                   InstitutionalProcessProfile processProfile) {
        return matchesAny(scope, "FEDERAL", "TRF", "JF", "AGU", "MPF", "DPU", "PFN")
                || matchesAny(profile == null ? null : profile.orgaoSigla(), "TRF", "JF")
                || matchesAny(profile == null ? null : profile.destinatarioKind(), "FEDERAL", "AGU")
                || matchesAny(workspace == null ? null : workspace.ramoDireito(), "FEDERAL")
                || processProfile == InstitutionalProcessProfile.PROCURADOR && matchesAny(scope, "AGU");
    }

    boolean isLaborScope(String scope,
                                 InstitutionalOperationalProfileProjection profile,
                                 InstitutionalProcessWorkspace workspace) {
        return matchesAny(scope, "TRAB", "TRT", "TST", "JUSTICA_DO_TRABALHO")
                || matchesAny(profile == null ? null : profile.orgaoSigla(), "TRT", "TST")
                || matchesAny(workspace == null ? null : workspace.ramoDireito(), "TRABALHISTA")
                || matchesAny(workspace == null ? null : workspace.ritoProcessual(), "TRAB");
    }

    boolean isElectoralScope(String scope,
                                     InstitutionalOperationalProfileProjection profile,
                                     InstitutionalProcessWorkspace workspace) {
        return matchesAny(scope, "ELEITOR", "TRE", "TSE")
                || matchesAny(profile == null ? null : profile.orgaoSigla(), "TRE", "TSE")
                || matchesAny(workspace == null ? null : workspace.ramoDireito(), "ELEITORAL")
                || matchesAny(workspace == null ? null : workspace.ritoProcessual(), "ELEITORAL");
    }

    boolean isMilitaryScope(String scope,
                                    InstitutionalOperationalProfileProjection profile,
                                    InstitutionalProcessWorkspace workspace) {
        return matchesAny(scope, "MILITAR", "STM", "CJM", "AUDITORIA_MILITAR")
                || matchesAny(profile == null ? null : profile.orgaoSigla(), "STM", "CJM")
                || matchesAny(workspace == null ? null : workspace.ramoDireito(), "MILITAR")
                || matchesAny(workspace == null ? null : workspace.ritoProcessual(), "MILITAR");
    }


    boolean scopeMatches(String scope, String token) {
        return scope != null && token != null && normalize(scope).contains(token.trim().toUpperCase(Locale.ROOT));
    }

    private boolean matchesAny(String value, String... tokens) {
        if (value == null || tokens == null || tokens.length == 0) {
            return false;
        }
        String normalized = normalize(value);
        for (String token : tokens) {
            if (token != null && !token.isBlank() && normalized.contains(token.trim().toUpperCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second;
    }

    private String firstNonBlank(String first, String second, String third) {
        return firstNonBlank(firstNonBlank(first, second), third);
    }

    private String firstNonBlank(String first, String second, String third, String fourth) {
        return firstNonBlank(firstNonBlank(first, second, third), fourth);
    }

    private String firstNonBlank(String first, String second, String third, String fourth, String fifth) {
        return firstNonBlank(firstNonBlank(first, second, third, fourth), fifth);
    }

    private String normalize(String value) {
        return value == null ? "NAO_INFORMADO" : value.trim().toUpperCase(Locale.ROOT);
    }
}
