package com.tcc.pjb.backend.service.admin.surface;

import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalCatalogGovernanceApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalCatalogCoverageSummary;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalCatalogGovernanceEntry;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalCatalogGovernanceSummary;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalCompetenceRule;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.application.InstitutionalOperationalCoverageApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.domain.InstitutionalOperationalCoverageRule;
import com.tcc.pjb.backend.model.dto.admin.AdminInstitutionalCatalogCoverageItemResponse;
import com.tcc.pjb.backend.model.dto.admin.AdminInstitutionalCatalogCoverageSummaryResponse;
import com.tcc.pjb.backend.model.dto.admin.AdminInstitutionalCatalogGovernanceResponse;
import com.tcc.pjb.backend.model.dto.admin.AdminInstitutionalCatalogGovernanceSummaryResponse;
import com.tcc.pjb.backend.model.dto.admin.AdminInstitutionalCatalogGovernanceUpsertRequest;
import com.tcc.pjb.backend.model.dto.admin.AdminInstitutionalCompetenceRuleResponse;
import com.tcc.pjb.backend.model.dto.admin.AdminInstitutionalCompetenceRuleUpsertRequest;
import com.tcc.pjb.backend.model.dto.admin.AdminInstitutionalRegionalBaselineExpansionRequest;
import com.tcc.pjb.backend.model.dto.admin.AdminInstitutionalRegionalBaselineExpansionResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalCoverageCreateRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalCoverageResponse;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class AdminInstitutionalGovernanceFacadeService {

    private final InstitutionalCatalogGovernanceApplicationService catalogService;
    private final InstitutionalOperationalCoverageApplicationService coverageService;

    public AdminInstitutionalGovernanceFacadeService(InstitutionalCatalogGovernanceApplicationService catalogService,
                                                     InstitutionalOperationalCoverageApplicationService coverageService) {
        this.catalogService = Objects.requireNonNull(catalogService);
        this.coverageService = Objects.requireNonNull(coverageService);
    }

    public List<AdminInstitutionalCatalogGovernanceResponse> listGovernances(DestinatarioInstitucionalKind destinatarioKind,
                                                                             String uf) {
        return catalogService.listGovernances(destinatarioKind, uf).stream().map(this::toGovernanceResponse).toList();
    }

    public AdminInstitutionalCatalogGovernanceResponse upsertGovernance(AdminInstitutionalCatalogGovernanceUpsertRequest request) {
        return toGovernanceResponse(catalogService.saveGovernance(
                request.governanceId(),
                request.unidadeCodigo(),
                request.destinatarioKind(),
                request.uf(),
                request.comarca(),
                request.foro(),
                request.ramoDireito(),
                request.grauJurisdicao(),
                request.abrangencia(),
                request.vigenciaInicio(),
                request.vigenciaFim(),
                request.ativa() == null || request.ativa(),
                request.suspendeEntregaExterna() != null && request.suspendeEntregaExterna(),
                request.exigeHomologacaoAdministrativa() != null && request.exigeHomologacaoAdministrativa(),
                request.canaisPreferenciais(),
                request.unidadeSubstitutaCodigo(),
                request.fundamentoAdministrativo(),
                request.origem()
        ));
    }

    public List<AdminInstitutionalCompetenceRuleResponse> listCompetenceRules(DestinatarioInstitucionalKind destinatarioKind,
                                                                              String uf) {
        return catalogService.listCompetenceRules(destinatarioKind, uf).stream().map(this::toCompetenceRuleResponse).toList();
    }

    public AdminInstitutionalCompetenceRuleResponse upsertCompetenceRule(AdminInstitutionalCompetenceRuleUpsertRequest request) {
        return toCompetenceRuleResponse(catalogService.saveCompetenceRule(
                request.ruleId(),
                request.destinatarioKind(),
                request.papelProcessual(),
                request.uf(),
                request.comarca(),
                request.foro(),
                request.ramoDireito(),
                request.grauJurisdicao(),
                request.unidadeCodigo(),
                request.prioridade() == null ? 100 : request.prioridade(),
                request.vigenciaInicio(),
                request.vigenciaFim(),
                request.ativa() == null || request.ativa(),
                request.origem(),
                request.fundamentoAdministrativo()
        ));
    }

    public AdminInstitutionalCatalogGovernanceSummaryResponse summary() {
        InstitutionalCatalogGovernanceSummary summary = catalogService.summarize();
        return new AdminInstitutionalCatalogGovernanceSummaryResponse(
                summary.totalUnidadesCatalogadas(),
                summary.totalGovernancasAtivas(),
                summary.totalRegrasCompetenciaAtivas(),
                summary.totalUnidadesSuspensas(),
                summary.totalComSubstituicao(),
                summary.totalExpirandoEm30Dias(),
                summary.catalogVersion()
        );
    }

    public AdminInstitutionalCatalogCoverageSummaryResponse nationalCoverage() {
        InstitutionalCatalogCoverageSummary summary = catalogService.coverageSummary();
        return new AdminInstitutionalCatalogCoverageSummaryResponse(
                summary.itens().stream().map(item -> new AdminInstitutionalCatalogCoverageItemResponse(
                        item.destinatarioKind().name(),
                        item.totalUfsCobertas(),
                        item.totalUnidadesAtivas(),
                        item.ufsCobertas(),
                        item.ufsFaltantes()
                )).toList(),
                summary.catalogVersion(),
                summary.geradoEm()
        );
    }

    public AdminInstitutionalRegionalBaselineExpansionResponse expandRegionalBaseline(AdminInstitutionalRegionalBaselineExpansionRequest request) {
        var result = catalogService.seedRegionalSpecializedBaseline(request.uf(), request.comarca(), request.foro());
        return new AdminInstitutionalRegionalBaselineExpansionResponse(
                result.uf(),
                result.comarca(),
                result.foro(),
                result.regrasCriadas(),
                result.regrasAtualizadas(),
                result.catalogVersion(),
                result.generatedAt(),
                result.notas()
        );
    }

    public NationalCommunicationInstitutionalCoverageResponse criarCobertura(NationalCommunicationInstitutionalCoverageCreateRequest request) {
        return toCoverageResponse(coverageService.criar(
                request.unidadeCodigo(),
                request.caixaCodigo(),
                request.titularUsuarioId(),
                request.coberturaUsuarioId(),
                request.tipoCobertura(),
                request.capacidades(),
                request.inicioVigencia(),
                request.fimVigencia(),
                request.motivo(),
                request.observacoes()
        ));
    }

    public List<NationalCommunicationInstitutionalCoverageResponse> listarCoberturas(String unidadeCodigo) {
        return coverageService.listar(unidadeCodigo).stream().map(this::toCoverageResponse).toList();
    }

    private AdminInstitutionalCatalogGovernanceResponse toGovernanceResponse(InstitutionalCatalogGovernanceEntry entry) {
        return new AdminInstitutionalCatalogGovernanceResponse(
                entry.governanceId(),
                entry.unidadeCodigo(),
                entry.destinatarioKind().name(),
                entry.uf(),
                entry.comarca(),
                entry.foro(),
                entry.ramoDireito() == null ? null : entry.ramoDireito().name(),
                entry.grauJurisdicao() == null ? null : entry.grauJurisdicao().name(),
                entry.abrangencia().name(),
                entry.vigenciaInicio(),
                entry.vigenciaFim(),
                entry.ativa(),
                entry.suspendeEntregaExterna(),
                entry.exigeHomologacaoAdministrativa(),
                entry.canaisPreferenciais().stream().map(Enum::name).collect(Collectors.toUnmodifiableSet()),
                entry.unidadeSubstitutaCodigo(),
                entry.fundamentoAdministrativo(),
                entry.origem(),
                entry.updatedAt()
        );
    }

    private AdminInstitutionalCompetenceRuleResponse toCompetenceRuleResponse(InstitutionalCompetenceRule rule) {
        return new AdminInstitutionalCompetenceRuleResponse(
                rule.ruleId(),
                rule.destinatarioKind().name(),
                rule.papelProcessual().name(),
                rule.uf(),
                rule.comarca(),
                rule.foro(),
                rule.ramoDireito() == null ? null : rule.ramoDireito().name(),
                rule.grauJurisdicao() == null ? null : rule.grauJurisdicao().name(),
                rule.unidadeCodigo(),
                rule.prioridade(),
                rule.vigenciaInicio(),
                rule.vigenciaFim(),
                rule.ativa(),
                rule.origem(),
                rule.fundamentoAdministrativo(),
                rule.updatedAt()
        );
    }

    private NationalCommunicationInstitutionalCoverageResponse toCoverageResponse(InstitutionalOperationalCoverageRule item) {
        return new NationalCommunicationInstitutionalCoverageResponse(
                item.ruleId(),
                item.unidadeCodigo(),
                item.caixaCodigo(),
                item.titularUsuarioId(),
                item.coberturaUsuarioId(),
                item.tipoCobertura().name(),
                item.capacidades().stream().map(Enum::name).toList(),
                item.status().name(),
                item.inicioVigencia(),
                item.fimVigencia(),
                item.motivo(),
                item.observacoes(),
                item.createdAt(),
                item.updatedAt()
        );
    }
}
