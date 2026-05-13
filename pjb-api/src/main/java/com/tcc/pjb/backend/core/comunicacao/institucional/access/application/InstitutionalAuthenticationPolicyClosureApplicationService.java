package com.tcc.pjb.backend.core.comunicacao.institucional.access.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.application.InstitutionalOrganizationBlueprintCatalogApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAccessLaneBlueprint;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAffiliation;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalNomination;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalOrganizationBlueprint;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalAffiliationStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalNominationStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.domain.InstitutionalAuthenticationLanePolicy;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.domain.InstitutionalAuthenticationPolicyClosure;
import com.tcc.pjb.backend.model.entity.enums.CapacidadeCaixaInstitucional;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalTrustLevel;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class InstitutionalAuthenticationPolicyClosureApplicationService {

    private final InstitutionalAffiliationStateRepository affiliationRepository;
    private final InstitutionalNominationStateRepository nominationRepository;
    private final InstitutionalOrganizationBlueprintCatalogApplicationService blueprintCatalogApplicationService;

    public InstitutionalAuthenticationPolicyClosureApplicationService(InstitutionalAffiliationStateRepository affiliationRepository,
                                                                     InstitutionalNominationStateRepository nominationRepository,
                                                                     InstitutionalOrganizationBlueprintCatalogApplicationService blueprintCatalogApplicationService) {
        this.affiliationRepository = Objects.requireNonNull(affiliationRepository);
        this.nominationRepository = Objects.requireNonNull(nominationRepository);
        this.blueprintCatalogApplicationService = Objects.requireNonNull(blueprintCatalogApplicationService);
    }

    public InstitutionalAuthenticationPolicyClosure consolidar(String affiliationId) {
        InstitutionalAffiliation affiliation = affiliationRepository.findByAffiliationId(Objects.requireNonNull(affiliationId))
                .orElseThrow(() -> new IllegalArgumentException("afiliacao_institucional_nao_encontrada"));
        Instant now = Instant.now();
        List<InstitutionalNomination> nominations = nominationRepository.findByAffiliationId(affiliation.affiliationId()).stream()
                .filter(item -> item.ativaEm(now))
                .toList();
        InstitutionalOrganizationBlueprint blueprint = blueprintCatalogApplicationService.resolve(
                affiliation.organizationScope(), affiliation.destinatarioKind()).orElse(null);
        List<InstitutionalAuthenticationLanePolicy> lanePolicies = resolveLanePolicies(affiliation, blueprint, nominations);
        LinkedHashSet<String> findings = new LinkedHashSet<>();
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        fundamentos.add("identidade_pessoal_raiz_via_govbr_obrigatoria_para_toda_atuacao_institucional");
        fundamentos.add("usuarios_operacionais_nao_assinantes_podem_ter_login_gerenciado_pela_instituicao_sem_substituir_a_identidade_govbr");
        fundamentos.add("atos_sensiveis_exigem_dose_dupla_de_prova_com_govbr_e_certificado_quando_houver_assinatura_ou_envio_em_nome_do_orgao");
        if (affiliation.requerCertificadoICP()) {
            fundamentos.add("afiliacao_requer_certificado_icp_para_perfis_assinantes");
        }
        if (affiliation.restringeCertificadoRedeInstitucional()) {
            fundamentos.add("certificado_restrito_a_rede_institucional_ou_autorizacao_remota_valida");
        }
        if (affiliation.requerDuplaAprovacaoAdministrador()) {
            fundamentos.add("administracao_mestra_exige_dupla_aprovacao_para_ativacao");
        }
        if (lanePolicies.stream().noneMatch(InstitutionalAuthenticationLanePolicy::allowsInstitutionManagedLogin)) {
            findings.add("catalogo_atual_sem_faixa_operacional_para_login_gerenciado_interno");
        }
        if (lanePolicies.stream().noneMatch(InstitutionalAuthenticationLanePolicy::signsOrSubmitsSensitiveActs)) {
            findings.add("catalogo_atual_sem_faixa_assinante_ou_peticionante_materializada");
        }
        if (blueprint == null) {
            findings.add("blueprint_institucional_nao_encontrado_para_a_afiliacao");
            fundamentos.add("fallback_para_politica_minima_de_entrada_institucional");
        } else {
            fundamentos.add("blueprint=" + blueprint.codigo());
            fundamentos.addAll(blueprint.fundamentos());
        }
        return new InstitutionalAuthenticationPolicyClosure(
                affiliation.affiliationId(),
                affiliation.orgaoSigla(),
                affiliation.orgaoNome(),
                affiliation.unidadeCodigo(),
                affiliation.organizationScope() == null ? null : affiliation.organizationScope().name(),
                affiliation.blueprintCode() == null && blueprint != null ? blueprint.codigo() : affiliation.blueprintCode(),
                true,
                true,
                true,
                true,
                affiliation.requerCertificadoICP() || lanePolicies.stream().anyMatch(InstitutionalAuthenticationLanePolicy::requiresQualifiedCertificateForSensitiveActs),
                affiliation.restringeCertificadoRedeInstitucional() || lanePolicies.stream().anyMatch(InstitutionalAuthenticationLanePolicy::requiresInstitutionalNetwork),
                lanePolicies,
                List.copyOf(findings),
                List.copyOf(fundamentos),
                Instant.now());
    }

    private List<InstitutionalAuthenticationLanePolicy> resolveLanePolicies(InstitutionalAffiliation affiliation,
                                                                            InstitutionalOrganizationBlueprint blueprint,
                                                                            List<InstitutionalNomination> nominations) {
        LinkedHashSet<String> covered = new LinkedHashSet<>();
        ArrayList<InstitutionalAuthenticationLanePolicy> out = new ArrayList<>();
        List<InstitutionalAccessLaneBlueprint> lanes = blueprint == null ? List.of() : blueprint.lanes();
        for (InstitutionalAccessLaneBlueprint lane : lanes) {
            out.add(toPolicy(affiliation, blueprint, lane, nominations));
            covered.add(resolveKey(lane.codigo(), lane.laneKind() == null ? null : lane.laneKind().name(), lane.nominationRole() == null ? null : lane.nominationRole().name()));
        }
        nominations.stream()
                .sorted(Comparator.comparing(InstitutionalNomination::updatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .forEach(nomination -> {
                    String key = resolveKey(nomination.accessLaneKind() == null ? null : nomination.accessLaneKind().name(),
                            nomination.nominationRole() == null ? null : nomination.nominationRole().name(),
                            nomination.processProfile() == null ? null : nomination.processProfile().name());
                    if (!covered.contains(key)) {
                        out.add(toPolicy(affiliation, blueprint, nomination));
                        covered.add(key);
                    }
                });
        return List.copyOf(out);
    }

    private InstitutionalAuthenticationLanePolicy toPolicy(InstitutionalAffiliation affiliation,
                                                           InstitutionalOrganizationBlueprint blueprint,
                                                           InstitutionalAccessLaneBlueprint lane,
                                                           List<InstitutionalNomination> nominations) {
        boolean signsOrSubmits = signsOrSubmits(lane.capacidadesPadrao());
        boolean signerBand = signsOrSubmits || (lane.funcaoOperacional() != null && lane.funcaoOperacional().isFuncaoAssinantePreferencial());
        boolean adminOrLeadership = (lane.nominationRole() != null && lane.nominationRole().isGestaoMestre())
                || (lane.funcaoOperacional() != null && lane.funcaoOperacional().isLideranca());
        String minimumGovBrLevel = resolveMinimumGovBrLevel(lane.trustFloor(), signerBand || adminOrLeadership, lane.requerStepUpMfa());
        boolean managedLoginAllowed = !signerBand;
        boolean managedLoginRequired = managedLoginAllowed && !adminOrLeadership;
        boolean certForEntry = lane.requerCertificadoICP() && signerBand;
        boolean certForSensitiveActs = lane.requerCertificadoICP() || signerBand || affiliation.requerCertificadoICP();
        boolean networkRequired = lane.requerRedeInstitucional() || (blueprint != null && blueprint.restringeCertificadoRedeInstitucional()) || affiliation.restringeCertificadoRedeInstitucional();
        boolean remoteAuthorized = lane.permiteUsoRemotoAutorizado() || (blueprint != null && blueprint.permiteUsoRemotoComAutorizacao()) || affiliation.permiteUsoRemotoComAutorizacao();
        ArrayList<String> fundamentos = new ArrayList<>();
        fundamentos.add("governo_identidade_raiz_govbr");
        fundamentos.add("minimo_govbr=" + minimumGovBrLevel);
        if (managedLoginAllowed) {
            fundamentos.add("login_gerenciado_institucional_permitido_com_vinculo_a_identidade_pessoal");
        }
        if (managedLoginRequired) {
            fundamentos.add("login_gerenciado_institucional_esperado_para_rotina_operacional_da_faixa");
        }
        if (certForSensitiveActs) {
            fundamentos.add("assinar_ou_enviar_em_nome_do_orgao_depende_de_certificado_e_govbr");
        }
        if (networkRequired) {
            fundamentos.add("uso_de_certificado_exige_rede_institucional_ou_autorizacao_remota_especifica");
        }
        if (remoteAuthorized) {
            fundamentos.add("remoto_autorizado_depende_de_autorizacao_temporal_e_dispositivo_homologado");
        }
        if (nominations.stream().noneMatch(item -> item.accessLaneKind() == lane.laneKind() || item.nominationRole() == lane.nominationRole())) {
            fundamentos.add("faixa_ainda_sem_nomeacao_ativa_materializada");
        }
        fundamentos.addAll(lane.fundamentos());
        return new InstitutionalAuthenticationLanePolicy(
                lane.codigo(),
                lane.laneKind() == null ? null : lane.laneKind().name(),
                lane.nominationRole() == null ? null : lane.nominationRole().name(),
                lane.funcaoOperacional() == null ? null : lane.funcaoOperacional().name(),
                lane.processProfile() == null ? null : lane.processProfile().name(),
                lane.nomeExibicao(),
                true,
                minimumGovBrLevel,
                managedLoginAllowed,
                managedLoginRequired,
                lane.requerStepUpMfa() || adminOrLeadership,
                certForEntry,
                certForSensitiveActs,
                networkRequired,
                remoteAuthorized,
                signsOrSubmits,
                lane.capacidadesPadrao().stream().map(Enum::name).sorted().toList(),
                List.copyOf(fundamentos.stream().filter(Objects::nonNull).map(String::trim).filter(item -> !item.isBlank()).distinct().toList()));
    }

    private InstitutionalAuthenticationLanePolicy toPolicy(InstitutionalAffiliation affiliation,
                                                           InstitutionalOrganizationBlueprint blueprint,
                                                           InstitutionalNomination nomination) {
        boolean signsOrSubmits = signsOrSubmits(nomination.capacidades());
        boolean signerBand = signsOrSubmits || (nomination.funcaoOperacional() != null && nomination.funcaoOperacional().isFuncaoAssinantePreferencial());
        boolean adminOrLeadership = (nomination.nominationRole() != null && nomination.nominationRole().isGestaoMestre())
                || (nomination.funcaoOperacional() != null && nomination.funcaoOperacional().isLideranca());
        String minimumGovBrLevel = resolveMinimumGovBrLevel(nomination.trustFloor(), signerBand || adminOrLeadership, nomination.requerStepUpMfa());
        boolean managedLoginAllowed = !signerBand;
        boolean managedLoginRequired = managedLoginAllowed && !adminOrLeadership;
        ArrayList<String> fundamentos = new ArrayList<>();
        fundamentos.add("faixa_materializada_por_nomeacao_ativa_sem_blueprint_dedicado");
        fundamentos.add("minimo_govbr=" + minimumGovBrLevel);
        if (managedLoginAllowed) {
            fundamentos.add("login_gerenciado_institucional_permitido_com_confirmacao_govbr");
        }
        if (signerBand || nomination.requerCertificadoICP()) {
            fundamentos.add("assinar_ou_enviar_em_nome_do_orgao_depende_de_certificado_e_govbr");
        }
        if (nomination.requerRedeInstitucional() || (blueprint != null && blueprint.restringeCertificadoRedeInstitucional()) || affiliation.restringeCertificadoRedeInstitucional()) {
            fundamentos.add("certificado_restrito_a_rede_institucional_ou_autorizacao_remota");
        }
        return new InstitutionalAuthenticationLanePolicy(
                nomination.accessLaneKind() == null ? nomination.nominationId() : nomination.accessLaneKind().name(),
                nomination.accessLaneKind() == null ? null : nomination.accessLaneKind().name(),
                nomination.nominationRole() == null ? null : nomination.nominationRole().name(),
                nomination.funcaoOperacional() == null ? null : nomination.funcaoOperacional().name(),
                nomination.processProfile() == null ? null : nomination.processProfile().name(),
                nomination.nominatedUserName() == null || nomination.nominatedUserName().isBlank() ? nomination.nominationId() : nomination.nominatedUserName(),
                true,
                minimumGovBrLevel,
                managedLoginAllowed,
                managedLoginRequired,
                nomination.requerStepUpMfa() || adminOrLeadership,
                nomination.requerCertificadoICP() && signerBand,
                nomination.requerCertificadoICP() || signerBand || affiliation.requerCertificadoICP(),
                nomination.requerRedeInstitucional() || (blueprint != null && blueprint.restringeCertificadoRedeInstitucional()) || affiliation.restringeCertificadoRedeInstitucional(),
                nomination.permiteUsoRemotoAutorizado() || (blueprint != null && blueprint.permiteUsoRemotoComAutorizacao()) || affiliation.permiteUsoRemotoComAutorizacao(),
                signsOrSubmits,
                nomination.capacidades().stream().map(Enum::name).sorted().toList(),
                List.copyOf(fundamentos));
    }

    private boolean signsOrSubmits(java.util.Set<CapacidadeCaixaInstitucional> capacidades) {
        if (capacidades == null || capacidades.isEmpty()) {
            return false;
        }
        return capacidades.contains(CapacidadeCaixaInstitucional.ASSINAR_MANIFESTACAO)
                || capacidades.contains(CapacidadeCaixaInstitucional.PETICIONAR_EM_NOME_DO_ORGAO);
    }

    private String resolveMinimumGovBrLevel(InstitutionalTrustLevel trustFloor,
                                            boolean highSensitivity,
                                            boolean stepUp) {
        if (trustFloor == InstitutionalTrustLevel.NIVEL_4_AMBIENTE_INSTITUCIONAL_RESTRITO || highSensitivity) {
            return "OURO";
        }
        if (stepUp || trustFloor == InstitutionalTrustLevel.NIVEL_3_CERTIFICADO_QUALIFICADO || trustFloor == InstitutionalTrustLevel.NIVEL_2_NOMEACAO_ATIVA) {
            return "PRATA";
        }
        return "PRATA";
    }

    private String resolveKey(String... values) {
        return java.util.Arrays.stream(values)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .findFirst()
                .orElse("GENERICA");
    }
}
