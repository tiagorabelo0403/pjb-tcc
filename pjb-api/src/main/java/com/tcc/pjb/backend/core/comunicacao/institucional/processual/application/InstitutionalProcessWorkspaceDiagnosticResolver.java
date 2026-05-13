package com.tcc.pjb.backend.core.comunicacao.institucional.processual.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAccessProfileCatalogEntry;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalProcessProfile;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalRiskSeverity;
import com.tcc.pjb.backend.core.comunicacao.institucional.processual.domain.InstitutionalProcessActionSpec;
import com.tcc.pjb.backend.core.comunicacao.institucional.processual.domain.InstitutionalProcessDiagnosticFinding;
import com.tcc.pjb.backend.core.comunicacao.institucional.processual.domain.InstitutionalProcessDiagnosticReport;
import com.tcc.pjb.backend.core.comunicacao.institucional.processual.domain.InstitutionalProcessWorkspace;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.CapacidadeCaixaInstitucional;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class InstitutionalProcessWorkspaceDiagnosticResolver {

    InstitutionalProcessDiagnosticReport diagnosticar(List<InstitutionalAccessProfileCatalogEntry> entries,
                                                      InstitutionalProcessWorkspaceAssembler assembler,
                                                      InstitutionalProcessWorkspaceSnapshot snapshot) {
        ArrayList<InstitutionalProcessDiagnosticFinding> findings = new ArrayList<>();
        entries.stream()
                .filter(this::isInstitutionalAffiliatedProfile)
                .forEach(entry -> inspectWorkspace(findings, assembler.toWorkspace(entry, snapshot), entry, snapshot));
        long blocking = findings.stream().filter(InstitutionalProcessDiagnosticFinding::blocking).count();
        return new InstitutionalProcessDiagnosticReport(
                findings.isEmpty(),
                findings.size(),
                blocking,
                findings.stream().sorted(Comparator.comparing((InstitutionalProcessDiagnosticFinding finding) -> finding.severity().weight()).reversed()
                        .thenComparing(InstitutionalProcessDiagnosticFinding::profileCode)
                        .thenComparing(InstitutionalProcessDiagnosticFinding::code)).toList(),
                List.of(
                        "Perfis institucionais precisam preservar coerência entre poderes, separadores, painéis e atos sensíveis.",
                        "A identidade pessoal permanece raiz; a legitimidade processual nasce do vínculo institucional homologado.",
                        "Recursos, embargos, execução, urgência e custódia não podem ficar misturados em filas genéricas."
                ),
                Instant.now()
        );
    }

    private void inspectWorkspace(List<InstitutionalProcessDiagnosticFinding> findings,
                                  InstitutionalProcessWorkspace workspace,
                                  InstitutionalAccessProfileCatalogEntry entry,
                                  InstitutionalProcessWorkspaceSnapshot snapshot) {
        if (workspace.actions().isEmpty()) {
            findings.add(finding(
                    "WORKSPACE_SEM_ACAO",
                    InstitutionalRiskSeverity.CRITICA,
                    workspace.profileCode(),
                    "Perfil institucional sem atos processuais habilitados.",
                    List.of("panel=" + workspace.panel(), "processProfile=" + workspace.processProfile())
            ));
        }
        if (workspace.sections().isEmpty()) {
            findings.add(finding(
                    "WORKSPACE_SEM_SECAO",
                    InstitutionalRiskSeverity.CRITICA,
                    workspace.profileCode(),
                    "Perfil institucional sem seções operacionais no processo.",
                    List.of("tabs=" + workspace.tabs().size())
            ));
        }
        if (workspace.authorityBands().isEmpty()) {
            findings.add(finding(
                    "WORKSPACE_SEM_FAIXA_AUTORIDADE",
                    InstitutionalRiskSeverity.ALTA,
                    workspace.profileCode(),
                    "Perfil sem matriz de poderes processuais consolidada.",
                    List.of("actions=" + workspace.actions().size())
            ));
        }
        if (workspace.separators().isEmpty()) {
            findings.add(finding(
                    "WORKSPACE_SEM_SEPARADOR",
                    InstitutionalRiskSeverity.ALTA,
                    workspace.profileCode(),
                    "Perfil sem separadores processuais por rito, recurso, embargo ou urgência.",
                    List.of("fase=" + workspace.faseProcessual())
            ));
        }
        if (allowsPetitioning(entry) && workspace.tabs().stream().noneMatch(tab -> tab.equals("recursos") || tab.equals("peticoes_e_respostas"))) {
            findings.add(finding(
                    "WORKSPACE_PETICAO_SEM_ABA",
                    InstitutionalRiskSeverity.ALTA,
                    workspace.profileCode(),
                    "Perfil com petição habilitada, mas sem aba recursal ou de petições.",
                    List.of("tabs=" + String.join(",", workspace.tabs()))
            ));
        }
        if (snapshot.embargos() && workspace.embargosHabilitados().isEmpty()) {
            findings.add(finding(
                    "WORKSPACE_EMBARGOS_AUSENTES",
                    InstitutionalRiskSeverity.ALTA,
                    workspace.profileCode(),
                    "Processo em embargos sem trilha de embargos habilitada para o perfil.",
                    List.of("status=" + workspace.statusProcessual())
            ));
        }
        if (isTechnical(entry.processProfile()) && workspace.visualLanes().stream().noneMatch(lane -> lane.code().equals("TRILHA_TECNICA"))) {
            findings.add(finding(
                    "WORKSPACE_TECNICO_SEM_TRILHA",
                    InstitutionalRiskSeverity.ALTA,
                    workspace.profileCode(),
                    "Perfil técnico sem trilha visual técnica.",
                    List.of("profile=" + workspace.processProfile())
            ));
        }
        if (entry.nominationRole() != null && entry.nominationRole().isGestaoMestre() && workspace.tabs().stream().noneMatch(tab -> tab.contains("govern") || tab.contains("administr"))) {
            findings.add(finding(
                    "WORKSPACE_GOVERNANCA_SEM_ABA",
                    InstitutionalRiskSeverity.MEDIA,
                    workspace.profileCode(),
                    "Perfil gestor sem aba dedicada à governança ou administração.",
                    List.of("tabs=" + String.join(",", workspace.tabs()))
            ));
        }
        if ((entry.processProfile() == InstitutionalProcessProfile.POLICIAL_PENAL
                || entry.processProfile() == InstitutionalProcessProfile.GESTOR_UNIDADE_PRISIONAL
                || entry.processProfile() == InstitutionalProcessProfile.OPERADOR_CUSTODIA_PRISIONAL)
                && workspace.sections().stream().noneMatch(section -> section.code().equals("SECAO_CUSTODIA"))) {
            findings.add(finding(
                    "WORKSPACE_CUSTODIA_SEM_SECAO",
                    InstitutionalRiskSeverity.CRITICA,
                    workspace.profileCode(),
                    "Perfil prisional sem seção de custódia e apresentação.",
                    List.of("processProfile=" + workspace.processProfile())
            ));
        }
        if ((entry.processProfile() == InstitutionalProcessProfile.PROMOTOR
                || entry.processProfile() == InstitutionalProcessProfile.DEFENSOR
                || entry.processProfile() == InstitutionalProcessProfile.PROCURADOR)
                && workspace.authorityBands().stream().noneMatch(band -> band.code().equals("AUTORIDADE_MANIFESTACAO") && band.enabled())) {
            findings.add(finding(
                    "WORKSPACE_SEM_MANIFESTACAO_TITULAR",
                    InstitutionalRiskSeverity.CRITICA,
                    workspace.profileCode(),
                    "Perfil titular sem faixa de manifestação/assinatura habilitada.",
                    List.of("processProfile=" + workspace.processProfile())
            ));
        }
        if (entry.processProfile() == InstitutionalProcessProfile.SERVIDOR_TRIAGEM
                && workspace.actions().stream().anyMatch(action -> action.requiresCertificate() || action.requiresTitularApproval())) {
            findings.add(finding(
                    "WORKSPACE_TRIAGEM_COM_SENSIVEL",
                    InstitutionalRiskSeverity.MEDIA,
                    workspace.profileCode(),
                    "Perfil de triagem com ato sensível acima do necessário.",
                    workspace.actions().stream()
                            .filter(action -> action.requiresCertificate() || action.requiresTitularApproval())
                            .map(InstitutionalProcessActionSpec::code)
                            .toList()
            ));
        }
    }

    private boolean allowsPetitioning(InstitutionalAccessProfileCatalogEntry entry) {
        return entry.capacidadesPadrao().contains(CapacidadeCaixaInstitucional.PETICIONAR_EM_NOME_DO_ORGAO)
                || entry.capacidadesPadrao().contains(CapacidadeCaixaInstitucional.ASSINAR_MANIFESTACAO)
                || entry.processProfile() == InstitutionalProcessProfile.PROMOTOR
                || entry.processProfile() == InstitutionalProcessProfile.DEFENSOR
                || entry.processProfile() == InstitutionalProcessProfile.PROCURADOR;
    }

    private boolean isTechnical(InstitutionalProcessProfile profile) {
        return profile == InstitutionalProcessProfile.PERITO_JUDICIAL
                || profile == InstitutionalProcessProfile.PSICOLOGO_JUDICIAL
                || profile == InstitutionalProcessProfile.ASSISTENTE_SOCIAL_JUDICIAL
                || profile == InstitutionalProcessProfile.CONTADOR_JUDICIAL
                || profile == InstitutionalProcessProfile.ORGAO_TECNICO_CONVENIADO;
    }

    private InstitutionalProcessDiagnosticFinding finding(String code,
                                                         InstitutionalRiskSeverity severity,
                                                         String profileCode,
                                                         String message,
                                                         List<String> evidences) {
        return new InstitutionalProcessDiagnosticFinding(code, severity, severity.isBlocking(), profileCode, message, evidences);
    }

    private boolean isInstitutionalAffiliatedProfile(InstitutionalAccessProfileCatalogEntry entry) {
        return entry.nominationRole() != null || entry.codigo().contains("__") || entry.codigo().startsWith("OAB_SECCIONAL");
    }
}
