package com.tcc.pjb.backend.core.plataforma.substituicao.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.competence.NationalCompetenceMatrix;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalSensitiveActAuthorizationApplicationService;
import com.tcc.pjb.backend.core.comunicacao.judicial.CitacaoIntimacaoEngine;
import com.tcc.pjb.backend.core.explainability.DecisionTraceService;
import com.tcc.pjb.backend.core.governance.idempotency.RequestIdempotencyService;
import com.tcc.pjb.backend.core.idempotency.ActionIdempotencyService;
import com.tcc.pjb.backend.core.jobs.runtime.JobAdminService;
import com.tcc.pjb.backend.core.jobs.runtime.JobCircuitBreaker;
import com.tcc.pjb.backend.core.jobs.runtime.JobExecutionService;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.NationalRecursalMeshEngine;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.rules.TribunalRegionalEleitoralRuleProfile;
import com.tcc.pjb.backend.core.kernel.recursal.template.impl.JuizadoRecursalTemplate;
import com.tcc.pjb.backend.core.kernel.recursal.template.impl.TrabalhistaRecursalTemplate;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbArquiteturaSubstituicaoNacionalAggregate;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbArquiteturaSubstituicaoPilar;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbFechamentoStatus;
import com.tcc.pjb.backend.core.processo.lifecycle.eleitoral.EleitoralLifecyclePack;
import com.tcc.pjb.backend.core.processo.lifecycle.civel.JuizadoLifecyclePack;
import com.tcc.pjb.backend.core.processo.lifecycle.militar.MilitarLifecyclePack;
import com.tcc.pjb.backend.core.processo.migracao.application.ProcessoMigracaoApplicationService;
import com.tcc.pjb.backend.core.processo.migracao.application.ProcessoMigracaoFactoryApplicationService;
import com.tcc.pjb.backend.core.processo.producao.application.ProcessoOperacaoTransversalApplicationService;
import com.tcc.pjb.backend.core.processo.recursal.application.ProcessoRecursalApplicationService;
import com.tcc.pjb.backend.core.processo.sigilo.application.ProcessoSigiloApplicationService;
import com.tcc.pjb.backend.core.processo.sigilo.application.ProcessoSigiloInteligenteApplicationService;
import com.tcc.pjb.backend.core.processo.sigilo.application.ProcessoSigiloNotificacaoApplicationService;
import com.tcc.pjb.backend.core.processo.transicao.application.ProcessoConvivenciaTransicaoApplicationService;
import com.tcc.pjb.backend.core.processo.trabalho.application.ProcessoTrabalhoApplicationService;
import com.tcc.pjb.backend.core.processo.vertical.estadual.civel.application.ProcessoVerticalCivelPrimeiroGrauApplicationService;
import com.tcc.pjb.backend.core.processo.vertical.estadual.fazenda.application.ProcessoVerticalExecucaoFiscalFazendariaApplicationService;
import com.tcc.pjb.backend.core.processo.vertical.estadual.penal.application.ProcessoVerticalPenalCustodiaApplicationService;
import com.tcc.pjb.backend.core.processual.routing.RecursalCollegiateResolver;
import com.tcc.pjb.backend.core.resilience.LocalCircuitBreaker;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.dto.governance.BuildGateEvaluationResponse;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.admin.AdministradorNacionalGovernanceService;
import com.tcc.pjb.backend.service.competencia.CompetenceResolverService;
import com.tcc.pjb.backend.service.governance.BuildGateGovernanceService;
import com.tcc.pjb.backend.service.profile.PerfilCapabilityMatrixService;
import com.tcc.pjb.backend.service.rito.RitoResolutionService;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Teste de caracterização (baseline) da fatia F6: prova que, após extrair os 4
 * avaliadores de pilar do antigo bean de 40 dependências, o AGREGADO FINAL produzido
 * por {@link PjbArquiteturaSubstituicaoNacionalApplicationService#avaliar()} continua
 * idêntico -- os 4 avaliadores usados aqui são instâncias REAIS (não mocks), só as
 * dependências-folha são mockadas, provando o comportamento ponta a ponta preservado.
 */
class PjbArquiteturaSubstituicaoNacionalApplicationServiceTest {

    private static <T> ObjectProvider<T> providerOf(T value) {
        @SuppressWarnings("unchecked")
        ObjectProvider<T> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(value);
        return provider;
    }

    private PjbArquiteturaSubstituicaoNacionalApplicationService build(
            ProcessoRepository processoRepository,
            WorkItemRepository workItemRepository,
            BuildGateGovernanceService buildGateGovernanceService,
            AdministradorNacionalGovernanceService administradorNacionalGovernanceService,
            boolean tudoDisponivel) {

        Map<Class<?>, Object> beans = tudoDisponivel ? mocksDisponiveis() : Map.of();

        PjbArquiteturaMotorProcessualPilarEvaluator motorEvaluator = new PjbArquiteturaMotorProcessualPilarEvaluator(
                providerOf((ProcessoVerticalCivelPrimeiroGrauApplicationService) beans.get(ProcessoVerticalCivelPrimeiroGrauApplicationService.class)),
                providerOf((ProcessoVerticalPenalCustodiaApplicationService) beans.get(ProcessoVerticalPenalCustodiaApplicationService.class)),
                providerOf((ProcessoVerticalExecucaoFiscalFazendariaApplicationService) beans.get(ProcessoVerticalExecucaoFiscalFazendariaApplicationService.class)),
                providerOf((ProcessoTrabalhoApplicationService) beans.get(ProcessoTrabalhoApplicationService.class)),
                providerOf((TrabalhistaRecursalTemplate) beans.get(TrabalhistaRecursalTemplate.class)),
                providerOf((JuizadoLifecyclePack) beans.get(JuizadoLifecyclePack.class)),
                providerOf((JuizadoRecursalTemplate) beans.get(JuizadoRecursalTemplate.class)),
                providerOf((EleitoralLifecyclePack) beans.get(EleitoralLifecyclePack.class)),
                providerOf((TribunalRegionalEleitoralRuleProfile) beans.get(TribunalRegionalEleitoralRuleProfile.class)),
                providerOf((MilitarLifecyclePack) beans.get(MilitarLifecyclePack.class)),
                providerOf((ProcessoRecursalApplicationService) beans.get(ProcessoRecursalApplicationService.class)),
                providerOf((NationalRecursalMeshEngine) beans.get(NationalRecursalMeshEngine.class)),
                providerOf((ProcessoSigiloApplicationService) beans.get(ProcessoSigiloApplicationService.class)),
                providerOf((ProcessoSigiloInteligenteApplicationService) beans.get(ProcessoSigiloInteligenteApplicationService.class)),
                providerOf((ProcessoSigiloNotificacaoApplicationService) beans.get(ProcessoSigiloNotificacaoApplicationService.class)),
                providerOf((CitacaoIntimacaoEngine) beans.get(CitacaoIntimacaoEngine.class)),
                providerOf((RecursalCollegiateResolver) beans.get(RecursalCollegiateResolver.class))
        );

        PjbArquiteturaInteroperabilidadePilarEvaluator interoperabilidadeEvaluator = new PjbArquiteturaInteroperabilidadePilarEvaluator(
                providerOf((PjbSubstituicaoLegadosApplicationService) beans.get(PjbSubstituicaoLegadosApplicationService.class)),
                providerOf((ProcessoMigracaoFactoryApplicationService) beans.get(ProcessoMigracaoFactoryApplicationService.class)),
                providerOf((ProcessoConvivenciaTransicaoApplicationService) beans.get(ProcessoConvivenciaTransicaoApplicationService.class)),
                providerOf((ProcessoMigracaoApplicationService) beans.get(ProcessoMigracaoApplicationService.class)),
                providerOf((AuditLedgerService) beans.get(AuditLedgerService.class)),
                providerOf((ActionIdempotencyService) beans.get(ActionIdempotencyService.class)),
                providerOf((RequestIdempotencyService) beans.get(RequestIdempotencyService.class))
        );

        PjbArquiteturaConfiabilidadePilarEvaluator confiabilidadeEvaluator = new PjbArquiteturaConfiabilidadePilarEvaluator(
                administradorNacionalGovernanceService,
                providerOf((ProcessoOperacaoTransversalApplicationService) beans.get(ProcessoOperacaoTransversalApplicationService.class)),
                providerOf((ActionIdempotencyService) beans.get(ActionIdempotencyService.class)),
                providerOf((RequestIdempotencyService) beans.get(RequestIdempotencyService.class)),
                providerOf((JobExecutionService) beans.get(JobExecutionService.class)),
                providerOf((JobAdminService) beans.get(JobAdminService.class)),
                providerOf((LocalCircuitBreaker) beans.get(LocalCircuitBreaker.class)),
                providerOf((JobCircuitBreaker) beans.get(JobCircuitBreaker.class)),
                providerOf((AuditLedgerService) beans.get(AuditLedgerService.class)),
                providerOf((DecisionTraceService) beans.get(DecisionTraceService.class)),
                providerOf((PjbAuthorizationService) beans.get(PjbAuthorizationService.class))
        );

        PjbArquiteturaGovernancaPilarEvaluator governancaEvaluator = new PjbArquiteturaGovernancaPilarEvaluator(
                administradorNacionalGovernanceService,
                providerOf((CompetenceResolverService) beans.get(CompetenceResolverService.class)),
                providerOf((RitoResolutionService) beans.get(RitoResolutionService.class)),
                providerOf((PerfilCapabilityMatrixService) beans.get(PerfilCapabilityMatrixService.class)),
                providerOf((InstitutionalSensitiveActAuthorizationApplicationService) beans.get(InstitutionalSensitiveActAuthorizationApplicationService.class)),
                providerOf((CapabilityRateLimiter) beans.get(CapabilityRateLimiter.class))
        );

        return new PjbArquiteturaSubstituicaoNacionalApplicationService(
                processoRepository,
                workItemRepository,
                buildGateGovernanceService,
                motorEvaluator,
                interoperabilidadeEvaluator,
                confiabilidadeEvaluator,
                governancaEvaluator
        );
    }

    private Map<Class<?>, Object> mocksDisponiveis() {
        return Map.ofEntries(
                Map.entry(ProcessoVerticalCivelPrimeiroGrauApplicationService.class, mock(ProcessoVerticalCivelPrimeiroGrauApplicationService.class)),
                Map.entry(ProcessoVerticalPenalCustodiaApplicationService.class, mock(ProcessoVerticalPenalCustodiaApplicationService.class)),
                Map.entry(ProcessoVerticalExecucaoFiscalFazendariaApplicationService.class, mock(ProcessoVerticalExecucaoFiscalFazendariaApplicationService.class)),
                Map.entry(ProcessoTrabalhoApplicationService.class, mock(ProcessoTrabalhoApplicationService.class)),
                Map.entry(TrabalhistaRecursalTemplate.class, mock(TrabalhistaRecursalTemplate.class)),
                Map.entry(JuizadoLifecyclePack.class, mock(JuizadoLifecyclePack.class)),
                Map.entry(JuizadoRecursalTemplate.class, mock(JuizadoRecursalTemplate.class)),
                Map.entry(EleitoralLifecyclePack.class, mock(EleitoralLifecyclePack.class)),
                Map.entry(TribunalRegionalEleitoralRuleProfile.class, mock(TribunalRegionalEleitoralRuleProfile.class)),
                Map.entry(MilitarLifecyclePack.class, mock(MilitarLifecyclePack.class)),
                Map.entry(ProcessoRecursalApplicationService.class, mock(ProcessoRecursalApplicationService.class)),
                Map.entry(NationalRecursalMeshEngine.class, mock(NationalRecursalMeshEngine.class)),
                Map.entry(ProcessoSigiloApplicationService.class, mock(ProcessoSigiloApplicationService.class)),
                Map.entry(ProcessoSigiloInteligenteApplicationService.class, mock(ProcessoSigiloInteligenteApplicationService.class)),
                Map.entry(ProcessoSigiloNotificacaoApplicationService.class, mock(ProcessoSigiloNotificacaoApplicationService.class)),
                Map.entry(CitacaoIntimacaoEngine.class, mock(CitacaoIntimacaoEngine.class)),
                Map.entry(RecursalCollegiateResolver.class, mock(RecursalCollegiateResolver.class)),
                Map.entry(ProcessoMigracaoApplicationService.class, mock(ProcessoMigracaoApplicationService.class)),
                Map.entry(ProcessoMigracaoFactoryApplicationService.class, mock(ProcessoMigracaoFactoryApplicationService.class)),
                Map.entry(ProcessoConvivenciaTransicaoApplicationService.class, mock(ProcessoConvivenciaTransicaoApplicationService.class)),
                Map.entry(PjbSubstituicaoLegadosApplicationService.class, mock(PjbSubstituicaoLegadosApplicationService.class)),
                Map.entry(ProcessoOperacaoTransversalApplicationService.class, mock(ProcessoOperacaoTransversalApplicationService.class)),
                Map.entry(ActionIdempotencyService.class, mock(ActionIdempotencyService.class)),
                Map.entry(RequestIdempotencyService.class, mock(RequestIdempotencyService.class)),
                Map.entry(JobExecutionService.class, mock(JobExecutionService.class)),
                Map.entry(JobAdminService.class, mock(JobAdminService.class)),
                Map.entry(JobCircuitBreaker.class, mock(JobCircuitBreaker.class)),
                Map.entry(LocalCircuitBreaker.class, mock(LocalCircuitBreaker.class)),
                Map.entry(AuditLedgerService.class, mock(AuditLedgerService.class)),
                Map.entry(DecisionTraceService.class, mock(DecisionTraceService.class)),
                Map.entry(PjbAuthorizationService.class, mock(PjbAuthorizationService.class)),
                Map.entry(CompetenceResolverService.class, mock(CompetenceResolverService.class)),
                Map.entry(RitoResolutionService.class, mock(RitoResolutionService.class)),
                Map.entry(PerfilCapabilityMatrixService.class, mock(PerfilCapabilityMatrixService.class)),
                Map.entry(InstitutionalSensitiveActAuthorizationApplicationService.class, mock(InstitutionalSensitiveActAuthorizationApplicationService.class)),
                Map.entry(CapabilityRateLimiter.class, mock(CapabilityRateLimiter.class))
        );
    }

    @Test
    void tudoDisponivelEBuildGateAprovado_produzArquiteturaProntaComQuatroPilaresConcluidos() {
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        WorkItemRepository workItemRepository = mock(WorkItemRepository.class);
        BuildGateGovernanceService buildGateGovernanceService = mock(BuildGateGovernanceService.class);
        AdministradorNacionalGovernanceService administradorNacionalGovernanceService = mock(AdministradorNacionalGovernanceService.class);

        when(processoRepository.count()).thenReturn(100L);
        when(workItemRepository.countByStatus(WorkItemStatus.PENDENTE)).thenReturn(100L);
        when(workItemRepository.countByStatus(WorkItemStatus.EXPIRADO)).thenReturn(2L);
        when(buildGateGovernanceService.evaluate()).thenReturn(new BuildGateEvaluationResponse(
                true, true, true, true, true, true, true, 0, List.of(), List.of()));

        PjbArquiteturaSubstituicaoNacionalApplicationService service = build(
                processoRepository, workItemRepository, buildGateGovernanceService,
                administradorNacionalGovernanceService, true);

        PjbArquiteturaSubstituicaoNacionalAggregate resultado = service.avaliar();

        assertThat(resultado.totalProcessos()).isEqualTo(100L);
        assertThat(resultado.totalWorkItemsPendentes()).isEqualTo(100L);
        assertThat(resultado.totalWorkItemsExpirados()).isEqualTo(2L);
        assertThat(resultado.buildGateAprovado()).isTrue();
        assertThat(resultado.totalTribunaisCatalogados()).isEqualTo(NationalCompetenceMatrix.values().length);
        assertThat(resultado.pilares()).hasSize(4);
        assertThat(resultado.pilares().stream().map(PjbArquiteturaSubstituicaoPilar::codigo))
                .containsExactly("motor-processual-nacional", "interoperabilidade-migracao",
                        "confiabilidade-institucional", "governanca-nacional");
        for (PjbArquiteturaSubstituicaoPilar pilar : resultado.pilares()) {
            assertThat(pilar.status())
                    .as("pilar %s deveria estar concluído com tudo disponível", pilar.codigo())
                    .isEqualTo(PjbFechamentoStatus.CONCLUIDA);
            assertThat(pilar.pronto()).isTrue();
            assertThat(pilar.capacidades()).allSatisfy(cap ->
                    assertThat(cap.status()).isEqualTo(PjbFechamentoStatus.CONCLUIDA));
        }
        assertThat(resultado.prontoParaSubstituicaoImediata()).isTrue();
        assertThat(resultado.scoreGeral()).isGreaterThanOrEqualTo(85);
        assertThat(resultado.fundamentos()).isNotEmpty();
        assertThat(resultado.conclusaoTecnica()).contains("rollout de substituição nacional em ondas controladas");
    }

    @Test
    void nadaDisponivelEBuildGateReprovado_produzArquiteturaNaoProntaComPilaresNaoConcluidos() {
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        WorkItemRepository workItemRepository = mock(WorkItemRepository.class);
        BuildGateGovernanceService buildGateGovernanceService = mock(BuildGateGovernanceService.class);
        AdministradorNacionalGovernanceService administradorNacionalGovernanceService = mock(AdministradorNacionalGovernanceService.class);

        when(processoRepository.count()).thenReturn(0L);
        when(workItemRepository.countByStatus(WorkItemStatus.PENDENTE)).thenReturn(0L);
        when(workItemRepository.countByStatus(WorkItemStatus.EXPIRADO)).thenReturn(0L);
        when(buildGateGovernanceService.evaluate()).thenThrow(new RuntimeException("gate indisponível"));

        PjbArquiteturaSubstituicaoNacionalApplicationService service = build(
                processoRepository, workItemRepository, buildGateGovernanceService,
                administradorNacionalGovernanceService, false);

        PjbArquiteturaSubstituicaoNacionalAggregate resultado = service.avaliar();

        assertThat(resultado.buildGateAprovado()).isFalse();
        assertThat(resultado.prontoParaSubstituicaoImediata()).isFalse();
        assertThat(resultado.pilares()).hasSize(4);
        for (PjbArquiteturaSubstituicaoPilar pilar : resultado.pilares()) {
            assertThat(pilar.pronto())
                    .as("pilar %s não deveria estar pronto sem nenhum colaborador disponível", pilar.codigo())
                    .isFalse();
            assertThat(pilar.status()).isNotEqualTo(PjbFechamentoStatus.CONCLUIDA);
        }
        assertThat(resultado.conclusaoTecnica()).contains("ainda depende de fechar as pendências");
    }
}
