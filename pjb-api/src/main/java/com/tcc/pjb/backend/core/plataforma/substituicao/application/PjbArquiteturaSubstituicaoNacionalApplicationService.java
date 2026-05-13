package com.tcc.pjb.backend.core.plataforma.substituicao.application;

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
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbArquiteturaSubstituicaoCapacidade;
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
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.admin.AdministradorNacionalGovernanceService;
import com.tcc.pjb.backend.service.competencia.CompetenceResolverService;
import com.tcc.pjb.backend.service.governance.BuildGateGovernanceService;
import com.tcc.pjb.backend.service.profile.PerfilCapabilityMatrixService;
import com.tcc.pjb.backend.service.rito.RitoResolutionService;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PjbArquiteturaSubstituicaoNacionalApplicationService {

    private final ProcessoRepository processoRepository;
    private final WorkItemRepository workItemRepository;
    private final BuildGateGovernanceService buildGateGovernanceService;
    private final AdministradorNacionalGovernanceService administradorNacionalGovernanceService;
    private final ObjectProvider<ProcessoVerticalCivelPrimeiroGrauApplicationService> verticalCivelProvider;
    private final ObjectProvider<ProcessoVerticalPenalCustodiaApplicationService> verticalPenalProvider;
    private final ObjectProvider<ProcessoVerticalExecucaoFiscalFazendariaApplicationService> verticalFazendaProvider;
    private final ObjectProvider<ProcessoTrabalhoApplicationService> trabalhoProvider;
    private final ObjectProvider<TrabalhistaRecursalTemplate> trabalhistaRecursalTemplateProvider;
    private final ObjectProvider<JuizadoLifecyclePack> juizadoLifecyclePackProvider;
    private final ObjectProvider<JuizadoRecursalTemplate> juizadoRecursalTemplateProvider;
    private final ObjectProvider<EleitoralLifecyclePack> eleitoralLifecyclePackProvider;
    private final ObjectProvider<TribunalRegionalEleitoralRuleProfile> eleitoralRuleProfileProvider;
    private final ObjectProvider<MilitarLifecyclePack> militarLifecyclePackProvider;
    private final ObjectProvider<ProcessoRecursalApplicationService> recursalProvider;
    private final ObjectProvider<NationalRecursalMeshEngine> recursalMeshProvider;
    private final ObjectProvider<ProcessoSigiloApplicationService> sigiloProvider;
    private final ObjectProvider<ProcessoSigiloInteligenteApplicationService> sigiloInteligenteProvider;
    private final ObjectProvider<ProcessoSigiloNotificacaoApplicationService> sigiloNotificacaoProvider;
    private final ObjectProvider<CitacaoIntimacaoEngine> citacaoIntimacaoEngineProvider;
    private final ObjectProvider<RecursalCollegiateResolver> recursalCollegiateResolverProvider;
    private final ObjectProvider<ProcessoMigracaoApplicationService> migracaoProvider;
    private final ObjectProvider<ProcessoMigracaoFactoryApplicationService> migracaoFactoryProvider;
    private final ObjectProvider<ProcessoConvivenciaTransicaoApplicationService> transicaoProvider;
    private final ObjectProvider<PjbSubstituicaoLegadosApplicationService> substituicaoLegadosProvider;
    private final ObjectProvider<ProcessoOperacaoTransversalApplicationService> operacaoTransversalProvider;
    private final ObjectProvider<ActionIdempotencyService> actionIdempotencyServiceProvider;
    private final ObjectProvider<RequestIdempotencyService> requestIdempotencyServiceProvider;
    private final ObjectProvider<JobExecutionService> jobExecutionServiceProvider;
    private final ObjectProvider<JobAdminService> jobAdminServiceProvider;
    private final ObjectProvider<JobCircuitBreaker> jobCircuitBreakerProvider;
    private final ObjectProvider<LocalCircuitBreaker> localCircuitBreakerProvider;
    private final ObjectProvider<com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService> auditLedgerServiceProvider;
    private final ObjectProvider<DecisionTraceService> decisionTraceServiceProvider;
    private final ObjectProvider<PjbAuthorizationService> pjbAuthorizationServiceProvider;
    private final ObjectProvider<CompetenceResolverService> competenceResolverServiceProvider;
    private final ObjectProvider<RitoResolutionService> ritoResolutionServiceProvider;
    private final ObjectProvider<PerfilCapabilityMatrixService> perfilCapabilityMatrixServiceProvider;
    private final ObjectProvider<InstitutionalSensitiveActAuthorizationApplicationService> institutionalSensitiveActAuthorizationApplicationServiceProvider;
    private final ObjectProvider<CapabilityRateLimiter> capabilityRateLimiterProvider;

    public PjbArquiteturaSubstituicaoNacionalApplicationService(
            ProcessoRepository processoRepository,
            WorkItemRepository workItemRepository,
            BuildGateGovernanceService buildGateGovernanceService,
            AdministradorNacionalGovernanceService administradorNacionalGovernanceService,
            ObjectProvider<ProcessoVerticalCivelPrimeiroGrauApplicationService> verticalCivelProvider,
            ObjectProvider<ProcessoVerticalPenalCustodiaApplicationService> verticalPenalProvider,
            ObjectProvider<ProcessoVerticalExecucaoFiscalFazendariaApplicationService> verticalFazendaProvider,
            ObjectProvider<ProcessoTrabalhoApplicationService> trabalhoProvider,
            ObjectProvider<TrabalhistaRecursalTemplate> trabalhistaRecursalTemplateProvider,
            ObjectProvider<JuizadoLifecyclePack> juizadoLifecyclePackProvider,
            ObjectProvider<JuizadoRecursalTemplate> juizadoRecursalTemplateProvider,
            ObjectProvider<EleitoralLifecyclePack> eleitoralLifecyclePackProvider,
            ObjectProvider<TribunalRegionalEleitoralRuleProfile> eleitoralRuleProfileProvider,
            ObjectProvider<MilitarLifecyclePack> militarLifecyclePackProvider,
            ObjectProvider<ProcessoRecursalApplicationService> recursalProvider,
            ObjectProvider<NationalRecursalMeshEngine> recursalMeshProvider,
            ObjectProvider<ProcessoSigiloApplicationService> sigiloProvider,
            ObjectProvider<ProcessoSigiloInteligenteApplicationService> sigiloInteligenteProvider,
            ObjectProvider<ProcessoSigiloNotificacaoApplicationService> sigiloNotificacaoProvider,
            ObjectProvider<CitacaoIntimacaoEngine> citacaoIntimacaoEngineProvider,
            ObjectProvider<RecursalCollegiateResolver> recursalCollegiateResolverProvider,
            ObjectProvider<ProcessoMigracaoApplicationService> migracaoProvider,
            ObjectProvider<ProcessoMigracaoFactoryApplicationService> migracaoFactoryProvider,
            ObjectProvider<ProcessoConvivenciaTransicaoApplicationService> transicaoProvider,
            ObjectProvider<PjbSubstituicaoLegadosApplicationService> substituicaoLegadosProvider,
            ObjectProvider<ProcessoOperacaoTransversalApplicationService> operacaoTransversalProvider,
            ObjectProvider<ActionIdempotencyService> actionIdempotencyServiceProvider,
            ObjectProvider<RequestIdempotencyService> requestIdempotencyServiceProvider,
            ObjectProvider<JobExecutionService> jobExecutionServiceProvider,
            ObjectProvider<JobAdminService> jobAdminServiceProvider,
            ObjectProvider<JobCircuitBreaker> jobCircuitBreakerProvider,
            ObjectProvider<LocalCircuitBreaker> localCircuitBreakerProvider,
            ObjectProvider<com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService> auditLedgerServiceProvider,
            ObjectProvider<DecisionTraceService> decisionTraceServiceProvider,
            ObjectProvider<PjbAuthorizationService> pjbAuthorizationServiceProvider,
            ObjectProvider<CompetenceResolverService> competenceResolverServiceProvider,
            ObjectProvider<RitoResolutionService> ritoResolutionServiceProvider,
            ObjectProvider<PerfilCapabilityMatrixService> perfilCapabilityMatrixServiceProvider,
            ObjectProvider<InstitutionalSensitiveActAuthorizationApplicationService> institutionalSensitiveActAuthorizationApplicationServiceProvider,
            ObjectProvider<CapabilityRateLimiter> capabilityRateLimiterProvider) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
        this.buildGateGovernanceService = Objects.requireNonNull(buildGateGovernanceService);
        this.administradorNacionalGovernanceService = Objects.requireNonNull(administradorNacionalGovernanceService);
        this.verticalCivelProvider = Objects.requireNonNull(verticalCivelProvider);
        this.verticalPenalProvider = Objects.requireNonNull(verticalPenalProvider);
        this.verticalFazendaProvider = Objects.requireNonNull(verticalFazendaProvider);
        this.trabalhoProvider = Objects.requireNonNull(trabalhoProvider);
        this.trabalhistaRecursalTemplateProvider = Objects.requireNonNull(trabalhistaRecursalTemplateProvider);
        this.juizadoLifecyclePackProvider = Objects.requireNonNull(juizadoLifecyclePackProvider);
        this.juizadoRecursalTemplateProvider = Objects.requireNonNull(juizadoRecursalTemplateProvider);
        this.eleitoralLifecyclePackProvider = Objects.requireNonNull(eleitoralLifecyclePackProvider);
        this.eleitoralRuleProfileProvider = Objects.requireNonNull(eleitoralRuleProfileProvider);
        this.militarLifecyclePackProvider = Objects.requireNonNull(militarLifecyclePackProvider);
        this.recursalProvider = Objects.requireNonNull(recursalProvider);
        this.recursalMeshProvider = Objects.requireNonNull(recursalMeshProvider);
        this.sigiloProvider = Objects.requireNonNull(sigiloProvider);
        this.sigiloInteligenteProvider = Objects.requireNonNull(sigiloInteligenteProvider);
        this.sigiloNotificacaoProvider = Objects.requireNonNull(sigiloNotificacaoProvider);
        this.citacaoIntimacaoEngineProvider = Objects.requireNonNull(citacaoIntimacaoEngineProvider);
        this.recursalCollegiateResolverProvider = Objects.requireNonNull(recursalCollegiateResolverProvider);
        this.migracaoProvider = Objects.requireNonNull(migracaoProvider);
        this.migracaoFactoryProvider = Objects.requireNonNull(migracaoFactoryProvider);
        this.transicaoProvider = Objects.requireNonNull(transicaoProvider);
        this.substituicaoLegadosProvider = Objects.requireNonNull(substituicaoLegadosProvider);
        this.operacaoTransversalProvider = Objects.requireNonNull(operacaoTransversalProvider);
        this.actionIdempotencyServiceProvider = Objects.requireNonNull(actionIdempotencyServiceProvider);
        this.requestIdempotencyServiceProvider = Objects.requireNonNull(requestIdempotencyServiceProvider);
        this.jobExecutionServiceProvider = Objects.requireNonNull(jobExecutionServiceProvider);
        this.jobAdminServiceProvider = Objects.requireNonNull(jobAdminServiceProvider);
        this.jobCircuitBreakerProvider = Objects.requireNonNull(jobCircuitBreakerProvider);
        this.localCircuitBreakerProvider = Objects.requireNonNull(localCircuitBreakerProvider);
        this.auditLedgerServiceProvider = Objects.requireNonNull(auditLedgerServiceProvider);
        this.decisionTraceServiceProvider = Objects.requireNonNull(decisionTraceServiceProvider);
        this.pjbAuthorizationServiceProvider = Objects.requireNonNull(pjbAuthorizationServiceProvider);
        this.competenceResolverServiceProvider = Objects.requireNonNull(competenceResolverServiceProvider);
        this.ritoResolutionServiceProvider = Objects.requireNonNull(ritoResolutionServiceProvider);
        this.perfilCapabilityMatrixServiceProvider = Objects.requireNonNull(perfilCapabilityMatrixServiceProvider);
        this.institutionalSensitiveActAuthorizationApplicationServiceProvider = Objects.requireNonNull(institutionalSensitiveActAuthorizationApplicationServiceProvider);
        this.capabilityRateLimiterProvider = Objects.requireNonNull(capabilityRateLimiterProvider);
    }

    @Transactional(readOnly = true)
    public PjbArquiteturaSubstituicaoNacionalAggregate avaliar() {
        long totalProcessos = processoRepository.count();
        long totalPendentes = workItemRepository.countByStatus(WorkItemStatus.PENDENTE);
        long totalExpirados = workItemRepository.countByStatus(WorkItemStatus.EXPIRADO);
        boolean buildGateAprovado = resolveBuildGateAprovado();

        PjbArquiteturaSubstituicaoPilar motor = avaliarMotorProcessualTransversal();
        PjbArquiteturaSubstituicaoPilar interoperabilidade = avaliarInteroperabilidadeEMigracao();
        PjbArquiteturaSubstituicaoPilar confiabilidade = avaliarConfiabilidadeInstitucional(totalPendentes, totalExpirados, buildGateAprovado);
        PjbArquiteturaSubstituicaoPilar governanca = avaliarGovernancaNacional(buildGateAprovado);
        List<PjbArquiteturaSubstituicaoPilar> pilares = List.of(motor, interoperabilidade, confiabilidade, governanca);
        int scoreGeral = score(pilares.stream().mapToInt(PjbArquiteturaSubstituicaoPilar::score).average().orElse(0));
        boolean pronto = buildGateAprovado
                && pilares.stream().allMatch(PjbArquiteturaSubstituicaoPilar::pronto)
                && totalExpirados <= Math.max(250, totalPendentes / 8);
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        fundamentos.add("totalProcessos=" + totalProcessos);
        fundamentos.add("workItemsPendentes=" + totalPendentes);
        fundamentos.add("workItemsExpirados=" + totalExpirados);
        fundamentos.add("tribunaisCatalogados=" + NationalCompetenceMatrix.values().length);
        fundamentos.add("ramosCatalogados=" + RamoDireito.values().length);
        fundamentos.add("ritosCatalogados=" + RitoProcessual.values().length);
        fundamentos.add("buildGateAprovado=" + buildGateAprovado);
        pilares.forEach(pilar -> fundamentos.add(pilar.codigo() + "=" + pilar.status().name() + ":" + pilar.score()));
        String conclusao = pronto
                ? "A arquitetura nacional já reúne motor transversal, convivência com legado, confiabilidade institucional e governança suficientes para rollout de substituição nacional em ondas controladas."
                : "A arquitetura avançou de forma real, mas a substituição nacional imediata ainda depende de fechar as pendências dos quatro pilares estruturais e consolidar prova operacional pesada.";
        return new PjbArquiteturaSubstituicaoNacionalAggregate(
                scoreGeral,
                pronto,
                buildGateAprovado,
                totalProcessos,
                totalPendentes,
                totalExpirados,
                NationalCompetenceMatrix.values().length,
                RitoProcessual.values().length,
                pilares,
                conclusao,
                List.copyOf(fundamentos),
                Instant.now()
        );
    }

    private PjbArquiteturaSubstituicaoPilar avaliarMotorProcessualTransversal() {
        ArrayList<PjbArquiteturaSubstituicaoCapacidade> capacidades = new ArrayList<>();
        capacidades.add(capacidade(
                "motor.civel",
                "Civil comum e família com fatia vertical explícita",
                available(verticalCivelProvider),
                96,
                List.of("ProcessoVerticalCivelPrimeiroGrauApplicationService", "Ritos de família, tutela e conhecimento mapeados no catálogo"),
                List.of("Continuar materialização fina de família e sucessões em ondas próprias")
        ));
        capacidades.add(capacidade(
                "motor.penal",
                "Penal, custódia, execução e recursal criminal",
                available(verticalPenalProvider) && available(recursalProvider),
                94,
                List.of("ProcessoVerticalPenalCustodiaApplicationService", "ProcessoRecursalApplicationService"),
                List.of("Ampliar cobertura fina de júri, execução penal e incidentes especiais")
        ));
        capacidades.add(capacidade(
                "motor.fazenda",
                "Fazenda pública e execução fiscal",
                available(verticalFazendaProvider),
                93,
                List.of("ProcessoVerticalExecucaoFiscalFazendariaApplicationService", "Ritos tributários e previdenciários catalogados"),
                List.of("Fechar trilhas específicas de fazenda não executiva e RPPS em profundidade")
        ));
        capacidades.add(capacidade(
                "motor.trabalhista",
                "Trabalhista transversal com prazo, recursal e trilha de trabalho",
                available(trabalhoProvider) && available(trabalhistaRecursalTemplateProvider),
                86,
                List.of("ProcessoTrabalhoApplicationService", "TrabalhistaRecursalTemplate", "Ritos trabalhistas no catálogo"),
                List.of("Materializar fatia vertical trabalhista dedicada de ponta a ponta")
        ));
        capacidades.add(capacidade(
                "motor.juizados",
                "Juizados e turma recursal",
                available(juizadoLifecyclePackProvider) && available(juizadoRecursalTemplateProvider),
                84,
                List.of("JuizadoLifecyclePack", "JuizadoRecursalTemplate", "Ritos de juizado no catálogo"),
                List.of("Fechar diferenças finas entre JEC, JECRIM, JEF e Juizado da Fazenda Pública")
        ));
        capacidades.add(capacidade(
                "motor.eleitoral",
                "Eleitoral e colegiado TRE/TSE",
                available(eleitoralLifecyclePackProvider) && available(eleitoralRuleProfileProvider),
                82,
                List.of("EleitoralLifecyclePack", "TribunalRegionalEleitoralRuleProfile", "Ritos eleitorais catalogados"),
                List.of("Aprofundar fatia vertical eleitoral com registro, AIJE, AIME e contas")
        ));
        capacidades.add(capacidade(
                "motor.militar",
                "Militar comum e especial",
                available(militarLifecyclePackProvider),
                80,
                List.of("MilitarLifecyclePack", "Ritos militares catalogados"),
                List.of("Fechar fatia vertical militar própria com conselho e execução disciplinar")
        ));
        capacidades.add(capacidade(
                "motor.execucao-recursal-sigilo",
                "Execução, recursos, prevenção, redistribuição e sigilo",
                available(recursalProvider) && available(recursalMeshProvider) && available(sigiloProvider) && available(sigiloInteligenteProvider) && available(sigiloNotificacaoProvider),
                92,
                List.of("ProcessoRecursalApplicationService", "NationalRecursalMeshEngine", "ProcessoSigiloApplicationService", "sigilo inteligente e notificações"),
                List.of("Seguir endurecendo prevenção, redistribuição e incidentes altamente especializados")
        ));
        capacidades.add(capacidade(
                "motor.comunicacao-colegiado",
                "Comunicação judicial, intimação/citação e colegiados",
                available(citacaoIntimacaoEngineProvider) && available(recursalCollegiateResolverProvider),
                90,
                List.of("CitacaoIntimacaoEngine", "RecursalCollegiateResolver"),
                List.of("Fechar microrregras locais por tribunal e colegiado para rollout nacional")
        ));
        return pilar(
                "motor-processual-nacional",
                "Motor processual nacional realmente transversal",
                capacidades,
                List.of(
                        "Materializar fatias verticais explícitas para trabalhista, eleitoral e militar em nível equivalente ao cível/penal/fazenda.",
                        "Fechar catálogo operacional fino de juizados e colegiados locais sem quebrar o núcleo nacional."
                )
        );
    }

    private PjbArquiteturaSubstituicaoPilar avaliarInteroperabilidadeEMigracao() {
        ArrayList<PjbArquiteturaSubstituicaoCapacidade> capacidades = new ArrayList<>();
        capacidades.add(capacidade(
                "interop.integracao",
                "Conectores nacionais, submissão e sincronização",
                available(substituicaoLegadosProvider),
                91,
                List.of("PjbSubstituicaoLegadosApplicationService", "AdministradorNacionalGovernanceService"),
                List.of("Ampliar matriz de conector por tribunal e trilha de corte gradual")
        ));
        capacidades.add(capacidade(
                "interop.importacao-legado",
                "Importação e fábrica de migração com mapeamento canônico",
                available(migracaoFactoryProvider),
                93,
                List.of("ProcessoMigracaoFactoryApplicationService", "Acervo, movimentos, documentos, filas e sigilos modelados"),
                List.of("Concluir lacres finais de assinatura e revalidação em lote nacional")
        ));
        capacidades.add(capacidade(
                "interop.shadow-e-convivencia",
                "Shadow mode, convivência com legado e reversibilidade",
                available(transicaoProvider) && available(migracaoProvider),
                90,
                List.of("ProcessoConvivenciaTransicaoApplicationService", "ProcessoMigracaoApplicationService"),
                List.of("Endurecer dual-write governado, rollback e checkpoints por onda de tribunal")
        ));
        capacidades.add(capacidade(
                "interop.reconciliacao-metadata",
                "Reconciliação de metadados e trilha comparativa",
                available(migracaoProvider),
                88,
                List.of("ProcessoMigracaoApplicationService", "Comparação de sombra, readiness e divergência controlada"),
                List.of("Amarrar reconciliação intertribunal e saneamento automático de divergências canônicas")
        ));
        capacidades.add(capacidade(
                "interop.auditoria-rollback",
                "Auditoria, rollback seguro e replay controlado",
                available(auditLedgerServiceProvider) && available(actionIdempotencyServiceProvider) && available(requestIdempotencyServiceProvider),
                87,
                List.of("AuditLedgerService", "ActionIdempotencyService", "RequestIdempotencyService"),
                List.of("Fechar rollback transacional por corte de tribunal e rito de alta criticidade")
        ));
        return pilar(
                "interoperabilidade-migracao",
                "Camada pesada de interoperabilidade e migração",
                capacidades,
                List.of(
                        "Fechar runbook de corte, reversão e replay por tribunal, ramo e rito sensível.",
                        "Endurecer reconciliação de assinatura, documento e metadado em migração massiva."
                )
        );
    }

    private PjbArquiteturaSubstituicaoPilar avaliarConfiabilidadeInstitucional(long pendentes, long expirados, boolean buildGateAprovado) {
        ArrayList<PjbArquiteturaSubstituicaoCapacidade> capacidades = new ArrayList<>();
        double taxaExpiracao = pendentes + expirados == 0 ? 0d : (expirados * 100d) / (pendentes + expirados);
        capacidades.add(capacidade(
                "conf.observabilidade",
                "Observabilidade nacional, backlog e saúde operacional",
                available(administradorNacionalGovernanceService) && available(operacaoTransversalProvider),
                taxaExpiracao < 5d ? 92 : taxaExpiracao < 12d ? 81 : 65,
                List.of("AdministradorNacionalGovernanceService", "ProcessoOperacaoTransversalApplicationService", "taxaExpiracao=" + format(taxaExpiracao)),
                List.of("Continuar fechando alertas críticos, filas quentes e visibilidade ponta a ponta")
        ));
        capacidades.add(capacidade(
                "conf.idempotencia-e-mensageria",
                "Idempotência, jobs, mensageria e retry",
                available(actionIdempotencyServiceProvider) && available(requestIdempotencyServiceProvider) && available(jobExecutionServiceProvider) && available(jobAdminServiceProvider),
                90,
                List.of("ActionIdempotencyService", "RequestIdempotencyService", "JobExecutionService", "JobAdminService"),
                List.of("Padronizar retry/backoff por eixo crítico e consolidar outbox de ponta a ponta")
        ));
        capacidades.add(capacidade(
                "conf.resiliencia-e-failover",
                "Circuit breaker, degradação graciosa e failover",
                available(localCircuitBreakerProvider) && available(jobCircuitBreakerProvider),
                86,
                List.of("LocalCircuitBreaker", "JobCircuitBreaker"),
                List.of("Formalizar failover multi-canal e recuperação orquestrada por tribunal")
        ));
        capacidades.add(capacidade(
                "conf.trilha-probatoria",
                "Trilha probatória, auditoria e explicabilidade",
                available(auditLedgerServiceProvider) && available(decisionTraceServiceProvider),
                92,
                List.of("AuditLedgerService", "DecisionTraceService"),
                List.of("Seguir endurecendo preservação de hash e cadeia probatória em todas as bordas")
        ));
        capacidades.add(capacidade(
                "conf.build-e-seguranca",
                "Build gate, segurança e endurecimento institucional",
                buildGateAprovado && available(pjbAuthorizationServiceProvider),
                buildGateAprovado ? 89 : 68,
                List.of("BuildGateGovernanceService", "PjbAuthorizationService"),
                List.of("Executar build integral e consolidar prova pesada de carga, soak e segurança")
        ));
        return pilar(
                "confiabilidade-institucional",
                "Confiabilidade institucional",
                capacidades,
                List.of(
                        "Concluir prova pesada nacional de carga, concorrência, resiliência e segurança.",
                        "Fechar disaster recovery, failover multi-canal e rollback operacional governado."
                )
        );
    }

    private PjbArquiteturaSubstituicaoPilar avaliarGovernancaNacional(boolean buildGateAprovado) {
        ArrayList<PjbArquiteturaSubstituicaoCapacidade> capacidades = new ArrayList<>();
        capacidades.add(capacidade(
                "gov.tribunais-e-ramos",
                "Matriz nacional de tribunal, ramo e conectores",
                NationalCompetenceMatrix.values().length >= 60,
                94,
                List.of("NationalCompetenceMatrix=" + NationalCompetenceMatrix.values().length, "RamoDireito=" + RamoDireito.values().length, "RitoProcessual=" + RitoProcessual.values().length),
                List.of("Seguir refinando diferenças locais por unidade e colegiado sem quebrar o núcleo")
        ));
        capacidades.add(capacidade(
                "gov.competencia-e-rito",
                "Resolução de competência e rito sem duplicar núcleo",
                available(competenceResolverServiceProvider) && available(ritoResolutionServiceProvider),
                91,
                List.of("CompetenceResolverService", "RitoResolutionService"),
                List.of("Endurecer override local parametrizado e rastreável por tribunal")
        ));
        capacidades.add(capacidade(
                "gov.perfis-e-capabilidades",
                "Perfis, capabilidades e autorização sensível",
                available(perfilCapabilityMatrixServiceProvider) && available(institutionalSensitiveActAuthorizationApplicationServiceProvider) && available(capabilityRateLimiterProvider),
                88,
                List.of("PerfilCapabilityMatrixService", "InstitutionalSensitiveActAuthorizationApplicationService", "CapabilityRateLimiter"),
                List.of("Consolidar matriz nacional de perfis especiais e atos sensíveis por microssistema")
        ));
        capacidades.add(capacidade(
                "gov.governanca-operacional",
                "Governança operacional nacional e health checks",
                available(administradorNacionalGovernanceService),
                87,
                List.of("AdministradorNacionalGovernanceService", "metricas por UF/comarca e reconciliação global"),
                List.of("Fechar rollout governado com pilotos, ondas de adesão e comitê executivo nacional")
        ));
        capacidades.add(capacidade(
                "gov.build-gates",
                "Governança estrutural de build e disciplina de superfície",
                buildGateAprovado,
                buildGateAprovado ? 90 : 70,
                List.of("BuildGateGovernanceService", "Disciplina estrutural já incorporada ao herdeiro"),
                List.of("Continuar zerando regressão estrutural e mantendo gate de rollout nacional")
        ));
        return pilar(
                "governanca-nacional",
                "Governança nacional",
                capacidades,
                List.of(
                        "Fechar política nacional de implantação por onda, tribunal e microssistema.",
                        "Preservar parametrização local com trilha auditável e núcleo canônico único."
                )
        );
    }

    private PjbArquiteturaSubstituicaoPilar pilar(String codigo,
                                                  String titulo,
                                                  List<PjbArquiteturaSubstituicaoCapacidade> capacidades,
                                                  List<String> proximasAcoes) {
        int score = score(capacidades.stream().mapToInt(PjbArquiteturaSubstituicaoCapacidade::score).average().orElse(0));
        long concluidas = capacidades.stream().filter(PjbArquiteturaSubstituicaoCapacidade::concluida).count();
        boolean pronto = concluidas == capacidades.size() && score >= 85;
        PjbFechamentoStatus status = pronto ? PjbFechamentoStatus.CONCLUIDA : score >= 75 ? PjbFechamentoStatus.PARCIAL : PjbFechamentoStatus.BLOQUEADA;
        return new PjbArquiteturaSubstituicaoPilar(codigo, titulo, status, score, pronto, capacidades, proximasAcoes);
    }

    private PjbArquiteturaSubstituicaoCapacidade capacidade(String codigo,
                                                            String titulo,
                                                            boolean concluida,
                                                            int scoreConcluida,
                                                            List<String> evidencias,
                                                            List<String> pendencias) {
        int score = concluida ? Math.max(scoreConcluida, 85) : Math.max(45, Math.min(84, scoreConcluida - 12));
        PjbFechamentoStatus status = concluida ? PjbFechamentoStatus.CONCLUIDA : score >= 70 ? PjbFechamentoStatus.PARCIAL : PjbFechamentoStatus.PENDENTE;
        String conclusao = concluida
                ? "Capacidade materializada com base concreta no herdeiro atual."
                : "Capacidade parcialmente presente, ainda exigindo fechamento fino para substituição nacional imediata.";
        return new PjbArquiteturaSubstituicaoCapacidade(codigo, titulo, status, score, conclusao, evidencias, pendencias);
    }

    private boolean resolveBuildGateAprovado() {
        try {
            return buildGateGovernanceService.evaluate().approved();
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private boolean available(ObjectProvider<?> provider) {
        return provider.getIfAvailable() != null;
    }

    private boolean available(Object value) {
        return value != null;
    }

    private int score(double value) {
        return Math.max(0, Math.min(100, (int) Math.round(value)));
    }

    private String format(double value) {
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }
}
