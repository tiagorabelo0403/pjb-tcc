package com.tcc.pjb.backend.core.comunicacao.institucional.registry.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.CatalogoInstitucionalUnificadoService;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.EstruturaCaixaInstitucionalService;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.application.InstitutionalManagedCredentialApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.application.InstitutionalRootAdministratorApprovalApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.domain.InstitutionalManagedCredential;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.application.InstitutionalOrganizationBlueprintCatalogApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAffiliation;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalNomination;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalOrganizationBlueprint;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalAffiliationStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalNominationStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.model.CaixaInstitucional;
import com.tcc.pjb.backend.core.comunicacao.institucional.model.UnidadeInstitucional;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain.InstitutionalOperationalProvisioningSnapshot;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain.InstitutionalProvisionedDirectoryEntry;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.infrastructure.InstitutionalOperationalProvisioningStateRepository;
import com.tcc.pjb.backend.model.entity.enums.CanalComunicacaoInstitucional;
import com.tcc.pjb.backend.model.entity.enums.PapelProcessualInstitucional;
import com.tcc.pjb.backend.model.entity.enums.TipoCaixaInstitucional;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class InstitutionalOperationalProvisioningApplicationService {

    private final InstitutionalOperationalProvisioningStateRepository repository;
    private final InstitutionalAffiliationStateRepository affiliationRepository;
    private final InstitutionalNominationStateRepository nominationRepository;
    private final InstitutionalOrganizationBlueprintCatalogApplicationService blueprintCatalogApplicationService;
    private final EstruturaCaixaInstitucionalService estruturaCaixaInstitucionalService;
    private final InstitutionalManagedCredentialApplicationService managedCredentialApplicationService;
    private final InstitutionalRootAdministratorApprovalApplicationService rootAdministratorApprovalApplicationService;
    private final CatalogoInstitucionalUnificadoService catalogoInstitucionalUnificadoService;

    public InstitutionalOperationalProvisioningApplicationService(InstitutionalOperationalProvisioningStateRepository repository,
                                                                 InstitutionalAffiliationStateRepository affiliationRepository,
                                                                 InstitutionalNominationStateRepository nominationRepository,
                                                                 InstitutionalOrganizationBlueprintCatalogApplicationService blueprintCatalogApplicationService,
                                                                 EstruturaCaixaInstitucionalService estruturaCaixaInstitucionalService,
                                                                 InstitutionalManagedCredentialApplicationService managedCredentialApplicationService,
                                                                 InstitutionalRootAdministratorApprovalApplicationService rootAdministratorApprovalApplicationService,
                                                                 CatalogoInstitucionalUnificadoService catalogoInstitucionalUnificadoService) {
        this.repository = Objects.requireNonNull(repository);
        this.affiliationRepository = Objects.requireNonNull(affiliationRepository);
        this.nominationRepository = Objects.requireNonNull(nominationRepository);
        this.blueprintCatalogApplicationService = Objects.requireNonNull(blueprintCatalogApplicationService);
        this.estruturaCaixaInstitucionalService = Objects.requireNonNull(estruturaCaixaInstitucionalService);
        this.managedCredentialApplicationService = Objects.requireNonNull(managedCredentialApplicationService);
        this.rootAdministratorApprovalApplicationService = Objects.requireNonNull(rootAdministratorApprovalApplicationService);
        this.catalogoInstitucionalUnificadoService = Objects.requireNonNull(catalogoInstitucionalUnificadoService);
    }

    public InstitutionalOperationalProvisioningSnapshot consolidar(String affiliationId) {
        return repository.findLatestByAffiliationId(affiliationId).orElseGet(() -> buildSnapshot(loadAffiliation(affiliationId), List.of(), false, List.of()));
    }

    public InstitutionalOperationalProvisioningSnapshot provisionar(String affiliationId,
                                                                   boolean persistExpandedBoxes,
                                                                   List<String> fundamentos) {
        InstitutionalAffiliation affiliation = loadAffiliation(affiliationId);
        if (affiliation == null) {
            throw new IllegalArgumentException("Afiliação institucional não localizada para provisionamento.");
        }
        return repository.save(buildSnapshot(affiliation, nominationRepository.findByAffiliationId(affiliationId), persistExpandedBoxes, fundamentos));
    }

    private InstitutionalOperationalProvisioningSnapshot buildSnapshot(InstitutionalAffiliation affiliation,
                                                                      List<InstitutionalNomination> nominations,
                                                                      boolean persistExpandedBoxes,
                                                                      List<String> extraFundamentos) {
        List<InstitutionalNomination> orderedNominations = nominations.stream()
                .sorted(Comparator.comparing(InstitutionalNomination::updatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        InstitutionalOrganizationBlueprint blueprint = affiliation == null ? null : blueprintCatalogApplicationService.resolve(affiliation.organizationScope(), affiliation.destinatarioKind()).orElse(null);
        UnidadeInstitucional unidade = affiliation == null ? null : toUnit(affiliation);
        List<CaixaInstitucional> caixas = unidade == null ? List.of() : estruturaCaixaInstitucionalService.expandir(unidade);
        List<InstitutionalManagedCredential> managedCredentials = affiliation == null ? List.of() : managedCredentialApplicationService.listar(affiliation.affiliationId());
        boolean rootApprovalRequired = affiliation != null && affiliation.requerDuplaAprovacaoAdministrador();
        boolean rootApprovalSatisfied = affiliation == null || rootAdministratorApprovalApplicationService.isSatisfied(affiliation.affiliationId());
        ArrayList<InstitutionalProvisionedDirectoryEntry> entries = new ArrayList<>();
        ArrayList<String> findings = new ArrayList<>();
        if (affiliation == null) {
            findings.add("afiliacao_inexistente");
        }
        if (affiliation != null && !affiliation.ativa()) {
            findings.add("afiliacao_ainda_nao_homologada");
        }
        if (caixas.isEmpty()) {
            findings.add("estrutura_caixas_nao_materializada");
        }
        if (orderedNominations.isEmpty()) {
            findings.add("nenhuma_nomeacao_ativa_ou_modelada_para_unidade");
        }
        if (rootApprovalRequired && !rootApprovalSatisfied) {
            findings.add("aprovacao_admin_raiz_pendente");
        }
        if (unidade != null) {
            String unitPartition = partitionKey(affiliation);
            entries.add(new InstitutionalProvisionedDirectoryEntry(
                    "UNIT:" + unidade.codigo(),
                    "UNIDADE",
                    null,
                    unidade.codigo(),
                    unidade.nomeOficial(),
                    affiliation.organizationScope() == null ? null : affiliation.organizationScope().name(),
                    territorialScope(affiliation),
                    unidade.caixaPrincipal().codigo(),
                    null,
                    null,
                    unitPartition + "|UNIT",
                    unitPartition,
                    readReplicaCode(affiliation),
                    affiliation.ativa(),
                    List.of(),
                    List.of("catalogo=" + catalogoInstitucionalUnificadoService.version(), "persistExpandedBoxes=" + persistExpandedBoxes)));
            for (CaixaInstitucional caixa : caixas) {
                entries.add(new InstitutionalProvisionedDirectoryEntry(
                        "BOX:" + caixa.codigo(),
                        "CAIXA",
                        "UNIT:" + unidade.codigo(),
                        caixa.codigo(),
                        caixa.nomeExibicao(),
                        affiliation.organizationScope() == null ? null : affiliation.organizationScope().name(),
                        territorialScope(affiliation),
                        caixa.codigo(),
                        null,
                        null,
                        unitPartition + "|" + caixa.codigo(),
                        unitPartition,
                        readReplicaCode(affiliation),
                        true,
                        List.of(caixa.tipo().name()),
                        List.of("tipo_caixa=" + caixa.tipo().name(), "permite_triagem=" + caixa.permiteTriagem())));
            }
            for (InstitutionalNomination nomination : orderedNominations) {
                entries.add(new InstitutionalProvisionedDirectoryEntry(
                        "LOT:" + nomination.nominationId(),
                        "LOTACAO",
                        "UNIT:" + unidade.codigo(),
                        nomination.nominationId(),
                        nomination.nominatedUserName() == null ? "Usuário institucional" : nomination.nominatedUserName(),
                        affiliation.organizationScope() == null ? null : affiliation.organizationScope().name(),
                        territorialScope(affiliation),
                        nomination.caixaCodigo(),
                        nomination.nominatedUserId(),
                        nomination.nominatedUserName(),
                        unitPartition + "|" + nomination.caixaCodigo(),
                        unitPartition,
                        readReplicaCode(affiliation),
                        nomination.ativaEm(Instant.now()),
                        List.of(nomination.accessLaneKind() == null ? "SEM_LANE" : nomination.accessLaneKind().name(), nomination.nominationRole().name()),
                        List.of("perfil=" + nomination.processProfile().name(), "funcao=" + nomination.funcaoOperacional().name())));
            }
        }
        boolean managedCredentialLaneSupported = blueprint != null && blueprint.lanes().stream().anyMatch(lane -> !lane.requerCertificadoICP());
        boolean managedCredentialLaneReady = managedCredentialLaneSupported && managedCredentials.stream().anyMatch(InstitutionalManagedCredential::ativa);
        boolean trustedSignerLanePresent = orderedNominations.stream().anyMatch(this::signerLane);
        String status = affiliation == null
                ? "INCONSISTENTE"
                : !affiliation.ativa()
                    ? "PENDENTE_HOMOLOGACAO"
                    : rootApprovalRequired && !rootApprovalSatisfied
                        ? "PENDENTE_APROVACAO_ADMIN_RAIZ"
                        : entries.isEmpty()
                            ? "PENDENTE_ESTRUTURA"
                            : "PRONTO";
        return new InstitutionalOperationalProvisioningSnapshot(
                UUID.randomUUID().toString(),
                affiliation == null ? null : affiliation.affiliationId(),
                affiliation == null ? null : affiliation.orgaoSigla(),
                affiliation == null ? null : affiliation.orgaoNome(),
                affiliation == null ? null : affiliation.unidadeCodigo(),
                affiliation == null ? null : affiliation.unidadeNome(),
                affiliation == null || affiliation.organizationScope() == null ? null : affiliation.organizationScope().name(),
                affiliation == null ? null : affiliation.blueprintCode(),
                status,
                affiliation != null && affiliation.ativa(),
                rootApprovalRequired,
                rootApprovalSatisfied,
                managedCredentialLaneSupported,
                managedCredentialLaneReady,
                trustedSignerLanePresent,
                entries.size(),
                (int) entries.stream().filter(item -> "CAIXA".equals(item.entryType())).count(),
                (int) entries.stream().filter(item -> "LOTACAO".equals(item.entryType())).count(),
                managedCredentials.size(),
                List.copyOf(entries),
                List.copyOf(findings),
                buildFundamentos(affiliation, persistExpandedBoxes, extraFundamentos),
                Instant.now()
        );
    }

    private UnidadeInstitucional toUnit(InstitutionalAffiliation affiliation) {
        String tribunalCodigo = affiliation.orgaoSigla() == null ? null : affiliation.orgaoSigla().trim().toUpperCase(Locale.ROOT);
        CaixaInstitucional principal = new CaixaInstitucional(
                affiliation.unidadeCodigo() + ":PRINCIPAL",
                (affiliation.unidadeNome() == null ? affiliation.orgaoNome() : affiliation.unidadeNome()) + " — Principal",
                TipoCaixaInstitucional.CAIXA_UNIDADE,
                affiliation.unidadeCodigo(),
                affiliation.destinatarioKind(),
                true,
                true);
        return new UnidadeInstitucional(
                affiliation.unidadeCodigo(),
                affiliation.destinatarioKind(),
                affiliation.orgaoSigla(),
                affiliation.orgaoNome(),
                affiliation.uf(),
                affiliation.comarca(),
                affiliation.comarca(),
                affiliation.unidadeNome(),
                affiliation.unidadeNome(),
                null,
                null,
                PapelProcessualInstitucional.DESTINATARIO_OFICIO,
                principal,
                List.of(new com.tcc.pjb.backend.core.comunicacao.institucional.model.CanalEntregaInstitucional(CanalComunicacaoInstitucional.PJB_INBOX, true, false, 48, 120, null, null)),
                tribunalCodigo,
                true,
                "provisionado_pelo_onboarding_institucional");
    }

    private boolean signerLane(InstitutionalNomination nomination) {
        return nomination.requerCertificadoICP()
                || nomination.funcaoOperacional().isFuncaoAssinantePreferencial()
                || nomination.capacidades().stream().anyMatch(cap -> cap.isAtoDeAssinaturaOuManifestacao());
    }

    private String partitionKey(InstitutionalAffiliation affiliation) {
        String uf = affiliation == null || affiliation.uf() == null ? "NACIONAL" : affiliation.uf().toUpperCase(Locale.ROOT);
        String orgao = affiliation == null || affiliation.orgaoSigla() == null ? "PJB" : affiliation.orgaoSigla().replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
        String unidade = affiliation == null || affiliation.unidadeCodigo() == null ? "UNIDADE" : affiliation.unidadeCodigo().replaceAll("[^A-Za-z0-9:_-]", "").toUpperCase(Locale.ROOT);
        return uf + '|' + orgao + '|' + unidade;
    }

    private String territorialScope(InstitutionalAffiliation affiliation) {
        if (affiliation == null) {
            return null;
        }
        return (affiliation.uf() == null ? "NACIONAL" : affiliation.uf()) + ':' + (affiliation.comarca() == null ? "GERAL" : affiliation.comarca());
    }

    private String readReplicaCode(InstitutionalAffiliation affiliation) {
        if (affiliation == null || affiliation.uf() == null || affiliation.uf().isBlank()) {
            return "replica-national";
        }
        return "replica-" + affiliation.uf().trim().toLowerCase(Locale.ROOT);
    }

    private InstitutionalAffiliation loadAffiliation(String affiliationId) {
        return affiliationId == null || affiliationId.isBlank() ? null : affiliationRepository.findByAffiliationId(affiliationId).orElse(null);
    }

    private List<String> buildFundamentos(InstitutionalAffiliation affiliation,
                                          boolean persistExpandedBoxes,
                                          List<String> extraFundamentos) {
        ArrayList<String> out = new ArrayList<>();
        out.add("provisionamento_institucional_persistido_em_state_store_para_nao_pesar_filas_quentes_do_processo");
        out.add("estrutura_materializada_em_unidade_caixas_lotacoes_e_chaves_horizontais");
        out.add("credenciais_gerenciadas_ficam_restritas_a_faixas_nao_assinantes");
        out.add("persistExpandedBoxes=" + persistExpandedBoxes);
        out.add("catalogo_unificado=" + catalogoInstitucionalUnificadoService.version());
        if (affiliation != null) {
            out.add("afiliacao=" + affiliation.affiliationId());
            out.add("blueprint=" + affiliation.blueprintCode());
            out.add("scope=" + (affiliation.organizationScope() == null ? "null" : affiliation.organizationScope().name()));
        }
        if (extraFundamentos != null && !extraFundamentos.isEmpty()) {
            out.addAll(extraFundamentos);
        }
        return List.copyOf(out);
    }
}
