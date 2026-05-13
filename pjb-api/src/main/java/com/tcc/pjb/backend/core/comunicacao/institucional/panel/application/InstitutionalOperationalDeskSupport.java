package com.tcc.pjb.backend.core.comunicacao.institucional.panel.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAccessProfileCatalogEntry;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalProcessProfile;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOperationalProfileProjection;
import com.tcc.pjb.backend.core.comunicacao.institucional.processual.domain.InstitutionalProcessActionSpec;
import com.tcc.pjb.backend.core.comunicacao.institucional.processual.domain.InstitutionalProcessWorkspace;
import com.tcc.pjb.backend.model.entity.enums.CapacidadeCaixaInstitucional;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalNominationRole;
import java.text.Normalizer;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class InstitutionalOperationalDeskSupport {

    private static final Pattern ORDINAL_VARA_PATTERN = Pattern.compile("(?:^|[_\\s])(\\d{1,2})(?:A|ª|O|º)?[_\\s]*VARA");
    private static final Pattern SUFFIX_VARA_PATTERN = Pattern.compile("VARA[_\\s]*(\\d{1,2})");

    InstitutionalOperationalDeskUnitFingerprint resolveUnitFingerprint(String tribunal,
                                                   String comarca,
                                                   String unidadeCodigo,
                                                   String unidadeNome,
                                                   String caixa,
                                                   String branchAxis) {
        String normalizedUnitName = normalize(unidadeNome);
        String varaCluster = resolveVaraCluster(normalizedUnitName);
        String specialization = resolveSpecializationCluster(normalizedUnitName, branchAxis);
        String groupingKey = normalize(tribunal)
                + '|' + normalize(comarca)
                + '|' + normalize(unidadeCodigo)
                + '|' + normalize(varaCluster)
                + '|' + normalize(specialization)
                + '|' + normalize(caixa);
        String isolationMode = normalizedUnitName.contains("UPJ")
                ? "UNIDADE_COMPARTILHADA_COM_FILAS_SEPARADAS"
                : normalizedUnitName.contains("CEJUSC")
                ? "CENTRAL_ESPECIALIZADA_COM_BLOCO_PROPRIO"
                : varaCluster.startsWith("VARA_")
                ? "VARA_E_ESPECIALIZACAO"
                : "UNIDADE_E_CAIXA";
        LinkedHashSet<String> topology = new LinkedHashSet<>();
        topology.add("tribunal=" + normalize(tribunal));
        topology.add("comarca=" + normalize(comarca));
        topology.add("unidade=" + normalize(unidadeCodigo));
        topology.add("unidade_nome=" + normalize(unidadeNome));
        topology.add("caixa=" + normalize(caixa));
        topology.add("vara_cluster=" + normalize(varaCluster));
        topology.add("especializacao=" + normalize(specialization));
        topology.add("modo_isolamento=" + normalize(isolationMode));
        topology.add("bloco_operacional=" + normalize(groupingKey));
        return new InstitutionalOperationalDeskUnitFingerprint(varaCluster, specialization, groupingKey, isolationMode, List.copyOf(topology));
    }

    String resolveVaraCluster(String normalizedUnitName) {
        String ordinal = capture(ORDINAL_VARA_PATTERN, normalizedUnitName);
        if (ordinal != null) {
            return "VARA_" + ordinal;
        }
        String suffix = capture(SUFFIX_VARA_PATTERN, normalizedUnitName);
        if (suffix != null) {
            return "VARA_" + suffix;
        }
        if (normalizedUnitName != null && normalizedUnitName.contains("UPJ")) {
            return "UPJ";
        }
        if (normalizedUnitName != null && normalizedUnitName.contains("CEJUSC")) {
            return "CEJUSC";
        }
        if (containsToken(normalizedUnitName, "TURMA RECURSAL", "CAMARA", "CÂMARA", "SECAO", "SEÇÃO", "COLEGIADO", "PLENARIO", "PLENÁRIO", "GABINETE DESEMBARGADOR", "GABINETE MINISTRO")) {
            return "SEGUNDO_GRAU";
        }
        if (normalizedUnitName != null && normalizedUnitName.contains("JUIZADO")) {
            return "JUIZADO";
        }
        return "UNIDADE_GERAL";
    }

    String resolveSpecializationCluster(String normalizedUnitName, String branchAxis) {
        if (containsToken(normalizedUnitName, "JECRIM")) {
            return "JECRIM";
        }
        if (containsToken(normalizedUnitName, "JUIZADO", "JEC")) {
            return "JUIZADOS";
        }
        if (containsToken(normalizedUnitName, "FAZENDA", "TRIBUT", "EXECUCAO FISCAL", "EXECUÇÃO FISCAL")) {
            return "FAZENDA";
        }
        if (containsToken(normalizedUnitName, "INFANCIA", "ADOLESC")) {
            return "INFANCIA";
        }
        if (containsToken(normalizedUnitName, "FAMILIA", "SUCESSOES", "VIOLENCIA DOMESTICA", "VIOLÊNCIA DOMÉSTICA", "MULHER")) {
            return "FAMILIA";
        }
        if (containsToken(normalizedUnitName, "CUSTODIA")) {
            return "CUSTODIA";
        }
        if (containsToken(normalizedUnitName, "PENAL", "CRIMINAL", "JURI")) {
            return "PENAL";
        }
        if (containsToken(normalizedUnitName, "TRABALHO", "TRABALHISTA")) {
            return "TRABALHISTA";
        }
        if (containsToken(normalizedUnitName, "ELEITOR")) {
            return "ELEITORAL";
        }
        if (containsToken(normalizedUnitName, "MILITAR")) {
            return "MILITAR";
        }
        if (containsToken(normalizedUnitName, "FEDERAL")) {
            return "FEDERAL";
        }
        if (containsToken(branchAxis, "PENAL", "CRIMINAL")) {
            return "PENAL";
        }
        if (containsToken(branchAxis, "TRABALHISTA")) {
            return "TRABALHISTA";
        }
        if (containsToken(branchAxis, "ELEITORAL")) {
            return "ELEITORAL";
        }
        if (containsToken(branchAxis, "MILITAR")) {
            return "MILITAR";
        }
        if (containsToken(branchAxis, "FAZENDA", "TRIBUT")) {
            return "FAZENDA";
        }
        return "CIVEL";
    }

    String resolveJudicialAxis(String unidadeNome,
                                       String scope,
                                       String branchAxis,
                                       String tribunal) {
        if (containsToken(unidadeNome, "TURMA RECURSAL", "CAMARA", "CÂMARA", "SECAO", "SEÇÃO", "COLEGIADO", "PLENARIO", "PLENÁRIO", "GABINETE DESEMBARGADOR", "GABINETE MINISTRO")) {
            if (containsToken(branchAxis, "PENAL", "CRIMINAL", "CUSTODIA", "JURI")) {
                return "SEGUNDO_GRAU_PENAL";
            }
            if (containsToken(branchAxis, "TRABALHISTA", "TRABALHO")) {
                return "SEGUNDO_GRAU_TRABALHISTA";
            }
            if (containsToken(branchAxis, "ELEITORAL")) {
                return "SEGUNDO_GRAU_ELEITORAL";
            }
            return "SEGUNDO_GRAU_CIVEL";
        }
        if (containsToken(unidadeNome, "FEDERAL") || containsToken(scope, "JUSTICA_FEDERAL", "TRF") || containsToken(tribunal, "TRF", "JF")) {
            if (containsToken(branchAxis, "PENAL", "CRIMINAL", "CUSTODIA")) {
                return "FEDERAL_PENAL";
            }
            return containsToken(branchAxis, "FAZENDA", "TRIBUT") ? "FEDERAL_FAZENDA" : "FEDERAL_CIVEL";
        }
        if (containsToken(branchAxis, "TRABALHISTA", "TRABALHO") || containsToken(scope, "TRT", "TRABALHO")) {
            return "TRABALHISTA";
        }
        if (containsToken(branchAxis, "ELEITORAL") || containsToken(scope, "TRE", "ELEITORAL")) {
            return "ELEITORAL";
        }
        if (containsToken(branchAxis, "MILITAR") || containsToken(scope, "MILITAR")) {
            return "MILITAR";
        }
        if (containsToken(unidadeNome, "JUIZADO", "JECRIM", "JEC") || containsToken(branchAxis, "JUIZADO", "JECRIM", "JEC")) {
            return "JUIZADOS_ESPECIAIS";
        }
        if (containsToken(unidadeNome, "PENAL", "CRIMINAL", "CUSTODIA", "JURI") || containsToken(branchAxis, "PENAL", "CRIMINAL", "CUSTODIA", "JURI")) {
            return "ESTADUAL_PENAL";
        }
        if (containsToken(unidadeNome, "FAZENDA", "TRIBUT", "EXECUCAO FISCAL", "EXECUÇÃO FISCAL") || containsToken(branchAxis, "FAZENDA", "TRIBUT")) {
            return "ESTADUAL_FAZENDA";
        }
        if (containsToken(unidadeNome, "INFANCIA", "ADOLESC") || containsToken(branchAxis, "INFANCIA", "ADOLESC")) {
            return "ESTADUAL_INFANCIA";
        }
        return "ESTADUAL_CIVEL";
    }

    String resolveUnitKind(String unidadeNome,
                                   String scope,
                                   InstitutionalProcessProfile processProfile,
                                   InstitutionalOperationalDeskUnitFingerprint fingerprint) {
        if (containsToken(unidadeNome, "GABINETE DESEMBARGADOR", "GABINETE MINISTRO", "CAMARA", "CÂMARA", "TURMA RECURSAL", "SECAO", "SEÇÃO", "COLEGIADO", "PLENARIO", "PLENÁRIO")) {
            return "SECRETARIA_SEGUNDO_GRAU";
        }
        if (containsToken(unidadeNome, "GABINETE")) {
            return "GABINETE";
        }
        if (containsToken(unidadeNome, "PROTOCOLO", "DISTRIBUICAO", "AUTUACAO")) {
            return "PROTOCOLO_DISTRIBUICAO";
        }
        if (containsToken(unidadeNome, "AUDIENCIA", "PAUTA") || containsToken(scope, "CENTRAL_AUDIENCIAS")) {
            return "CENTRAL_AUDIENCIAS";
        }
        if (containsToken(unidadeNome, "UPJ")) {
            return "UPJ";
        }
        if (containsToken(unidadeNome, "CEJUSC") || containsToken(scope, "CEJUSC")) {
            return "CEJUSC";
        }
        if (containsToken(unidadeNome, "CENTRAL", "MANDADOS") || containsToken(scope, "MANDADOS")) {
            return "CENTRAL_MANDADOS";
        }
        if (containsToken(unidadeNome, "SECRETARIA", "CARTORIO") || processProfile == InstitutionalProcessProfile.SECRETARIA_FORUM || processProfile == InstitutionalProcessProfile.DIRETOR_FORUM) {
            return "SECRETARIA";
        }
        if (containsToken(unidadeNome, "PROMOTORIA") || containsToken(scope, "PROMOTORIA", "MINISTERIO_PUBLICO") || processProfile == InstitutionalProcessProfile.PROMOTOR) {
            return "PROMOTORIA";
        }
        if (containsToken(unidadeNome, "DEFENSORIA") || containsToken(scope, "DEFENSORIA") || processProfile == InstitutionalProcessProfile.DEFENSOR) {
            return "DEFENSORIA";
        }
        if (containsToken(unidadeNome, "PROCURADORIA", "AGU", "FAZENDA") || containsToken(scope, "PROCURADORIA", "AGU", "FAZENDA") || processProfile == InstitutionalProcessProfile.PROCURADOR) {
            return "PROCURADORIA_PUBLICA";
        }
        if (containsToken(unidadeNome, "DELEGACIA", "POLICIA") || containsToken(scope, "DELEGACIA", "POLICIA") || processProfile == InstitutionalProcessProfile.DELEGADO) {
            return "DELEGACIA";
        }
        if (containsToken(unidadeNome, "PRISIONAL", "PENITENCIARIA") || processProfile == InstitutionalProcessProfile.POLICIAL_PENAL || processProfile == InstitutionalProcessProfile.GESTOR_UNIDADE_PRISIONAL || processProfile == InstitutionalProcessProfile.OPERADOR_CUSTODIA_PRISIONAL) {
            return "UNIDADE_PRISIONAL";
        }
        if (containsToken(unidadeNome, "CONTADORIA", "CALCULO") || processProfile == InstitutionalProcessProfile.CONTADOR_JUDICIAL) {
            return "CONTADORIA";
        }
        if (containsToken(unidadeNome, "PSICO", "SOCIAL") || processProfile == InstitutionalProcessProfile.PSICOLOGO_JUDICIAL || processProfile == InstitutionalProcessProfile.ASSISTENTE_SOCIAL_JUDICIAL) {
            return "EQUIPE_PSICOSSOCIAL";
        }
        if (fingerprint.varaCluster().startsWith("VARA_")) {
            return "VARA";
        }
        if ("JUIZADO".equals(fingerprint.varaCluster())) {
            return "JUIZADO";
        }
        return "UNIDADE_INSTITUCIONAL";
    }

    String capture(Pattern pattern, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        Matcher matcher = pattern.matcher(value);
        return matcher.find() ? matcher.group(1) : null;
    }

    Set<CapacidadeCaixaInstitucional> resolveCapacities(InstitutionalOperationalProfileProjection profile,
                                                                InstitutionalAccessProfileCatalogEntry catalogEntry) {
        EnumSet<CapacidadeCaixaInstitucional> capacities = EnumSet.noneOf(CapacidadeCaixaInstitucional.class);
        if (catalogEntry != null && catalogEntry.capacidadesPadrao() != null) {
            capacities.addAll(catalogEntry.capacidadesPadrao());
        }
        if (profile != null && profile.capacidades() != null) {
            for (String item : profile.capacidades()) {
                if (item == null || item.isBlank()) {
                    continue;
                }
                try {
                    capacities.add(CapacidadeCaixaInstitucional.valueOf(item.trim().toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        return Set.copyOf(capacities);
    }

    InstitutionalProcessProfile resolveProcessProfile(InstitutionalOperationalProfileProjection profile,
                                                              InstitutionalAccessProfileCatalogEntry catalogEntry) {
        if (catalogEntry != null && catalogEntry.processProfile() != null) {
            return catalogEntry.processProfile();
        }
        if (profile == null || profile.processProfile() == null || profile.processProfile().isBlank()) {
            return null;
        }
        try {
            return InstitutionalProcessProfile.valueOf(profile.processProfile().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    InstitutionalNominationRole resolveNominationRole(InstitutionalOperationalProfileProjection profile,
                                                              InstitutionalAccessProfileCatalogEntry catalogEntry) {
        if (catalogEntry != null && catalogEntry.nominationRole() != null) {
            return catalogEntry.nominationRole();
        }
        if (profile == null || profile.nominationRole() == null || profile.nominationRole().isBlank()) {
            return null;
        }
        try {
            return InstitutionalNominationRole.valueOf(profile.nominationRole().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    boolean isLegalInstitutionProfile(InstitutionalProcessProfile processProfile) {
        return processProfile == InstitutionalProcessProfile.PROMOTOR
                || processProfile == InstitutionalProcessProfile.DEFENSOR
                || processProfile == InstitutionalProcessProfile.PROCURADOR
                || processProfile == InstitutionalProcessProfile.DELEGADO;
    }

    boolean containsWorkspaceSignals(InstitutionalProcessWorkspace workspace, String... signals) {
        if (workspace == null) {
            return false;
        }
        if (containsToken(workspace.ramoDireito(), signals) || containsToken(workspace.ritoProcessual(), signals) || containsToken(workspace.faseProcessual(), signals)) {
            return true;
        }
        for (String item : workspace.tabs()) {
            if (containsToken(item, signals)) {
                return true;
            }
        }
        for (String item : workspace.quickFilters()) {
            if (containsToken(item, signals)) {
                return true;
            }
        }
        for (InstitutionalProcessActionSpec action : workspace.actions()) {
            if (containsToken(action.code(), signals) || containsToken(action.title(), signals) || containsToken(action.description(), signals)) {
                return true;
            }
        }
        return false;
    }

    boolean containsToken(String value, String... signals) {
        if (value == null || value.isBlank() || signals == null || signals.length == 0) {
            return false;
        }
        String normalized = normalize(value);
        for (String signal : signals) {
            if (signal != null && !signal.isBlank() && normalized.contains(normalize(signal))) {
                return true;
            }
        }
        return false;
    }

    boolean hasText(String value) {
        return value != null && !value.isBlank() && !Objects.equals(normalize(value), "SEM_UNIDADE") && !Objects.equals(normalize(value), "SEM_COMARCA") && !Objects.equals(normalize(value), "SEM_TRIBUNAL");
    }

    String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String item : values) {
            if (item != null && !item.isBlank()) {
                return item.trim();
            }
        }
        return null;
    }

    String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "NAO_INFORMADO";
        }
        String ascii = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return ascii
                .replaceAll("[^A-Za-z0-9]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "")
                .toUpperCase(Locale.ROOT);
    }
}
