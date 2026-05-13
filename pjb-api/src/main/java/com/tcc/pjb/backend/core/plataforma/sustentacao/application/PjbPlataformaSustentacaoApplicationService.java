package com.tcc.pjb.backend.core.plataforma.sustentacao.application;

import com.tcc.pjb.backend.configs.PjbFeatureFlagsProperties;
import com.tcc.pjb.backend.core.competence.NationalCompetenceMatrix;
import com.tcc.pjb.backend.core.idempotency.ActionIdempotencyService;
import com.tcc.pjb.backend.core.plataforma.sustentacao.domain.PjbPlataformaSustentacaoAggregate;
import com.tcc.pjb.backend.core.plataforma.sustentacao.domain.PjbPlataformaSustentacaoCenario;
import com.tcc.pjb.backend.core.plataforma.sustentacao.domain.PjbPlataformaSustentacaoEixo;
import com.tcc.pjb.backend.core.plataforma.sustentacao.domain.PjbPlataformaSustentacaoModulo;
import com.tcc.pjb.backend.core.processo.migracao.application.ProcessoMigracaoApplicationService;
import com.tcc.pjb.backend.core.processo.migracao.application.ProcessoMigracaoFactoryApplicationService;
import com.tcc.pjb.backend.core.processo.migracao.domain.ProcessoMigracaoAggregate;
import com.tcc.pjb.backend.core.processo.migracao.domain.ProcessoMigracaoFabricaAggregate;
import com.tcc.pjb.backend.core.quality.apisurface.application.PjbApiSurfaceSanityApplicationService;
import com.tcc.pjb.backend.core.quality.apisurface.domain.PjbApiSurfaceIssue;
import com.tcc.pjb.backend.core.quality.apisurface.domain.PjbApiSurfaceSanityAggregate;
import com.tcc.pjb.backend.core.quality.codebase.application.PjbCodebaseSanityApplicationService;
import com.tcc.pjb.backend.core.quality.codebase.domain.PjbCodebaseSanityAggregate;
import com.tcc.pjb.backend.core.quality.codebase.domain.PjbCodebaseSanityIssue;
import com.tcc.pjb.backend.core.procedural.NationalProceduralRoutingService;
import com.tcc.pjb.backend.core.procedural.ProceduralCanonicalResolver;
import com.tcc.pjb.backend.core.procedural.ProceduralRoutingReport;
import com.tcc.pjb.backend.core.security.sigilo.SigiloAccessStatus;
import com.tcc.pjb.backend.core.security.sigilo.repository.SigiloAccessRequestRepository;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorCommandCenterReport;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorCommandCenterService;
import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
import com.tcc.pjb.backend.model.dto.governance.BuildGateEvaluationResponse;
import com.tcc.pjb.backend.model.dto.processual.rollout.NationalFeatureRolloutRequest;
import com.tcc.pjb.backend.model.dto.processual.rollout.NationalFeatureRolloutResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.outbox.OutboxStatus;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.institucional.InstitutionalInboxItemSnapshotRepository;
import com.tcc.pjb.backend.repository.outbox.OutboxEventRepository;
import com.tcc.pjb.backend.service.SigiloService;
import com.tcc.pjb.backend.service.SigiloService.SigiloDecision;
import com.tcc.pjb.backend.service.governance.BuildGateGovernanceService;
import com.tcc.pjb.backend.service.procedural.ProceduralArchitectureSanityService;
import com.tcc.pjb.backend.service.procedural.ProceduralLegacyBoundaryAuditService;
import com.tcc.pjb.backend.service.processual.rollout.NationalFeatureRolloutService;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class PjbPlataformaSustentacaoApplicationService {

    private final PjbCodebaseSanityApplicationService codebaseSanityApplicationService;
    private final PjbApiSurfaceSanityApplicationService apiSurfaceSanityApplicationService;
    private final ProceduralArchitectureSanityService proceduralArchitectureSanityService;
    private final ProceduralLegacyBoundaryAuditService proceduralLegacyBoundaryAuditService;
    private final BuildGateGovernanceService buildGateGovernanceService;
    private final NationalFeatureRolloutService nationalFeatureRolloutService;
    private final PjbFeatureFlagsProperties featureFlagsProperties;
    private final JudicialConnectorCommandCenterService judicialConnectorCommandCenterService;
    private final ActionIdempotencyService actionIdempotencyService;
    private final SigiloService sigiloService;
    private final ProceduralCanonicalResolver proceduralCanonicalResolver;
    private final NationalProceduralRoutingService nationalProceduralRoutingService;
    private final ProcessoMigracaoFactoryApplicationService processoMigracaoFactoryApplicationService;
    private final ProcessoMigracaoApplicationService processoMigracaoApplicationService;
    private final ProcessoRepository processoRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final InstitutionalInboxItemSnapshotRepository institutionalInboxItemSnapshotRepository;
    private final SigiloAccessRequestRepository sigiloAccessRequestRepository;
    private final ApplicationContext applicationContext;

    public PjbPlataformaSustentacaoApplicationService(PjbCodebaseSanityApplicationService codebaseSanityApplicationService,
                                                      PjbApiSurfaceSanityApplicationService apiSurfaceSanityApplicationService,
                                                      ProceduralArchitectureSanityService proceduralArchitectureSanityService,
                                                      ProceduralLegacyBoundaryAuditService proceduralLegacyBoundaryAuditService,
                                                      BuildGateGovernanceService buildGateGovernanceService,
                                                      NationalFeatureRolloutService nationalFeatureRolloutService,
                                                      PjbFeatureFlagsProperties featureFlagsProperties,
                                                      JudicialConnectorCommandCenterService judicialConnectorCommandCenterService,
                                                      ActionIdempotencyService actionIdempotencyService,
                                                      SigiloService sigiloService,
                                                      ProceduralCanonicalResolver proceduralCanonicalResolver,
                                                      NationalProceduralRoutingService nationalProceduralRoutingService,
                                                      ProcessoMigracaoFactoryApplicationService processoMigracaoFactoryApplicationService,
                                                      ProcessoMigracaoApplicationService processoMigracaoApplicationService,
                                                      ProcessoRepository processoRepository,
                                                      ObjectProvider<OutboxEventRepository> outboxEventRepositoryProvider,
                                                      ObjectProvider<InstitutionalInboxItemSnapshotRepository> institutionalInboxItemSnapshotRepositoryProvider,
                                                      ObjectProvider<SigiloAccessRequestRepository> sigiloAccessRequestRepositoryProvider,
                                                      ApplicationContext applicationContext) {
        this.codebaseSanityApplicationService = Objects.requireNonNull(codebaseSanityApplicationService);
        this.apiSurfaceSanityApplicationService = Objects.requireNonNull(apiSurfaceSanityApplicationService);
        this.proceduralArchitectureSanityService = Objects.requireNonNull(proceduralArchitectureSanityService);
        this.proceduralLegacyBoundaryAuditService = Objects.requireNonNull(proceduralLegacyBoundaryAuditService);
        this.buildGateGovernanceService = Objects.requireNonNull(buildGateGovernanceService);
        this.nationalFeatureRolloutService = Objects.requireNonNull(nationalFeatureRolloutService);
        this.featureFlagsProperties = Objects.requireNonNull(featureFlagsProperties);
        this.judicialConnectorCommandCenterService = Objects.requireNonNull(judicialConnectorCommandCenterService);
        this.actionIdempotencyService = Objects.requireNonNull(actionIdempotencyService);
        this.sigiloService = Objects.requireNonNull(sigiloService);
        this.proceduralCanonicalResolver = Objects.requireNonNull(proceduralCanonicalResolver);
        this.nationalProceduralRoutingService = Objects.requireNonNull(nationalProceduralRoutingService);
        this.processoMigracaoFactoryApplicationService = Objects.requireNonNull(processoMigracaoFactoryApplicationService);
        this.processoMigracaoApplicationService = Objects.requireNonNull(processoMigracaoApplicationService);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.outboxEventRepository = outboxEventRepositoryProvider.getIfAvailable();
        this.institutionalInboxItemSnapshotRepository = institutionalInboxItemSnapshotRepositoryProvider.getIfAvailable();
        this.sigiloAccessRequestRepository = sigiloAccessRequestRepositoryProvider.getIfAvailable();
        this.applicationContext = Objects.requireNonNull(applicationContext);
    }

    public PjbPlataformaSustentacaoAggregate avaliar() {
        PjbPlataformaSustentacaoEixo gateArquitetural = avaliarGateArquitetural();
        ManifestoBundle manifestoBundle = avaliarManifestoModular();
        PjbPlataformaSustentacaoEixo featureFlags = avaliarFeatureFlagsFederativas();
        PjbPlataformaSustentacaoEixo confiabilidade = avaliarConfiabilidadeInstitucional();
        PjbPlataformaSustentacaoEixo sigiloCentral = avaliarMotorSigiloCentral();
        PjbPlataformaSustentacaoEixo normalizador = avaliarNormalizadorNacional();
        PjbPlataformaSustentacaoEixo shadowCompare = avaliarShadowCompareMigracao();
        GoldenBundle goldenBundle = avaliarCenariosDourados();

        List<PjbPlataformaSustentacaoEixo> eixos = List.of(
                gateArquitetural,
                manifestoBundle.eixo(),
                featureFlags,
                confiabilidade,
                sigiloCentral,
                normalizador,
                shadowCompare,
                goldenBundle.eixo()
        );
        int scoreGeral = average(eixos.stream().mapToInt(PjbPlataformaSustentacaoEixo::score).toArray());
        int eixosProntos = (int) eixos.stream().filter(PjbPlataformaSustentacaoEixo::pronto).count();
        boolean aptoPreBuild = gateArquitetural.pronto()
                && confiabilidade.pronto()
                && normalizador.pronto()
                && goldenBundle.eixo().score() >= 70
                && shadowCompare.score() >= 60;

        LinkedHashSet<String> bloqueadores = new LinkedHashSet<>();
        LinkedHashSet<String> proximasAcoes = new LinkedHashSet<>();
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        fundamentos.add("sustentacao.eixos=" + eixos.size());
        fundamentos.add("sustentacao.scoreGeral=" + scoreGeral);
        fundamentos.add("sustentacao.aptoPreBuild=" + aptoPreBuild);
        for (PjbPlataformaSustentacaoEixo eixo : eixos) {
            fundamentos.add(eixo.codigo() + "=" + eixo.status());
            bloqueadores.addAll(eixo.bloqueadores());
            proximasAcoes.addAll(eixo.proximasAcoes());
        }
        if (aptoPreBuild) {
            proximasAcoes.add("INICIAR_BUILD_LOCAL_COM_SUITE_DE_CENARIOS_DOURADOS");
        } else {
            proximasAcoes.add("ESTABILIZAR_EIXOS_CRITICOS_ANTES_DA_BUILD_INTEGRAL");
        }

        return new PjbPlataformaSustentacaoAggregate(
                scoreGeral,
                aptoPreBuild,
                eixosProntos,
                eixos.size(),
                eixos,
                manifestoBundle.modulos(),
                goldenBundle.cenarios(),
                List.copyOf(bloqueadores),
                List.copyOf(proximasAcoes),
                List.copyOf(fundamentos),
                Instant.now()
        );
    }

    private PjbPlataformaSustentacaoEixo avaliarGateArquitetural() {
        PjbCodebaseSanityAggregate codebase = codebaseSanityApplicationService.auditar();
        PjbApiSurfaceSanityAggregate apiSurface = apiSurfaceSanityApplicationService.auditar();
        ProceduralArchitectureSanityService.SanityReport architecture = proceduralArchitectureSanityService.report();
        ProceduralLegacyBoundaryAuditService.BoundaryReport boundary = proceduralLegacyBoundaryAuditService.report();
        BuildGateEvaluationResponse buildGate = buildGateGovernanceService.evaluate();
        int score = average(
                codebase.score(),
                apiSurface.score(),
                architecture.healthy() ? 95 : 58,
                boundary.clean() ? 92 : 52,
                buildGate.approved() ? 96 : 54
        );
        LinkedHashSet<String> sinais = new LinkedHashSet<>();
        sinais.add(codebase.resumo());
        sinais.add("apiSurfaceScore=" + apiSurface.score());
        sinais.add("architectureHealthy=" + architecture.healthy());
        sinais.add("legacyBoundaryClean=" + boundary.clean());
        sinais.add("buildGateApproved=" + buildGate.approved());
        sinais.add("routeGateApproved=" + buildGate.routeGateApproved());
        sinais.add("validationGateApproved=" + buildGate.validationGateApproved());
        LinkedHashSet<String> bloqueadores = new LinkedHashSet<>(buildGate.outstandingIssues());
        bloqueadores.addAll(architecture.issues());
        bloqueadores.addAll(boundary.violations().stream().map(ProceduralLegacyBoundaryAuditService.BoundaryViolation::reason).toList());
        bloqueadores.addAll(codebase.issues().stream().map(PjbCodebaseSanityIssue::codigo).toList());
        bloqueadores.addAll(apiSurface.issues().stream().map(PjbApiSurfaceIssue::codigo).toList());
        LinkedHashSet<String> proximasAcoes = new LinkedHashSet<>(buildGate.nextActions());
        if (!architecture.healthy()) {
            proximasAcoes.add("NORMALIZAR_CATALOGO_PROCEDURAL_E_CONNECTORES_PREFERIDOS");
        }
        if (!boundary.clean()) {
            proximasAcoes.add("EXPULSAR_REFERENCIAS_DIRETAS_A_ENUMS_LEGADOS_FORA_DA_CAMADA_CANONICA");
        }
        LinkedHashMap<String, Object> evidencias = new LinkedHashMap<>();
        evidencias.put("codebaseScore", codebase.score());
        evidencias.put("codebaseIssues", codebase.issues().size());
        evidencias.put("apiSurfaceScore", apiSurface.score());
        evidencias.put("apiSurfaceIssues", apiSurface.issues().size());
        evidencias.put("architectureIssues", architecture.issues().size());
        evidencias.put("legacyBoundaryViolations", boundary.violations().size());
        evidencias.put("buildGateOutstandingIssues", buildGate.totalOutstandingIssues());
        return eixo(
                "gate.arquitetural",
                "Gate arquitetural, surface e build",
                score,
                codebase.limpo() && apiSurface.limpo() && architecture.healthy() && boundary.clean() && buildGate.approved(),
                sinais,
                bloqueadores,
                proximasAcoes,
                evidencias
        );
    }

    private ManifestoBundle avaliarManifestoModular() {
        JudicialConnectorCommandCenterReport commandCenter = judicialConnectorCommandCenterService.nationalReport(Duration.ofHours(6));
        List<PjbPlataformaSustentacaoModulo> modulos = List.of(
                modulo("mod.nucleo-processual", "Núcleo processual e roteamento", "CORE", countBeansContaining(".core.procedural") + countBeansContaining(".service.rito"), average(beanScore(".core.procedural"), beanScore(".service.rito"), 88), List.of("canonical", "routing", "rito-pack"), List.of()),
                modulo("mod.integracao", "Integração, outbox e inbox", "INTEGRACAO", scorePresence(outboxEventRepository, institutionalInboxItemSnapshotRepository, actionIdempotencyService), average(commandCenter.governance().operationalConnectorCount() > 0 ? 90 : 58, outboxEventRepository != null ? 84 : 42, institutionalInboxItemSnapshotRepository != null ? 82 : 38), List.of("connectors", "outbox", "inbox", "idempotencia"), commandCenter.alerts()),
                modulo("mod.sigilo", "Sigilo central e step-up", "SEGURANCA", countBeansContaining(".core.security.sigilo") + countBeansContaining(".core.processo.sigilo"), average(beanScore(".core.security.sigilo"), beanScore(".core.processo.sigilo"), 90), List.of("sigilo", "credencial", "step-up"), List.of()),
                modulo("mod.migracao", "Migração, shadow compare e rollback", "MIGRACAO", countBeansContaining(".core.processo.migracao") + countBeansContaining(".integration.judicial"), average(beanScore(".core.processo.migracao"), commandCenter.observability().healthySystems() > 0 ? 84 : 60), List.of("shadow", "factory", "cutover"), commandCenter.alerts()),
                modulo("mod.coletivo", "Coletivo, precedentes e pós-coletiva", "JULGAMENTO", countBeansContaining(".core.plataforma.substituicao") + countBeansContaining(".core.processo.execucao"), average(beanScore(".core.plataforma.substituicao"), beanScore(".core.processo.execucao"), 82), List.of("precedentes", "tutela-coletiva", "pos-coletiva"), List.of()),
                modulo("mod.governanca", "Governança, observabilidade e readiness", "GOVERNANCA", countBeansContaining(".service.governance") + countBeansContaining(".controller.admin"), average(beanScore(".service.governance"), beanScore(".controller.admin"), 86), List.of("build-gate", "observability", "governance-surface"), List.of())
        );
        int score = average(modulos.stream().mapToInt(PjbPlataformaSustentacaoModulo::score).toArray());
        LinkedHashSet<String> sinais = new LinkedHashSet<>();
        sinais.add("modulosMapeados=" + modulos.size());
        sinais.add("connectorsOperacionais=" + commandCenter.governance().operationalConnectorCount());
        sinais.add("contingencyConnectorPresent=" + commandCenter.governance().contingencyConnectorPresent());
        LinkedHashSet<String> bloqueadores = new LinkedHashSet<>(commandCenter.alerts());
        modulos.stream().filter(modulo -> modulo.score() < 65).map(PjbPlataformaSustentacaoModulo::codigo).map(codigo -> "manifesto_incompleto:" + codigo).forEach(bloqueadores::add);
        LinkedHashSet<String> proximasAcoes = new LinkedHashSet<>();
        proximasAcoes.add("CONGELAR_MAPA_DE_MODULOS_CRITICOS_ANTES_DA_BUILD");
        proximasAcoes.add("DECLARAR_MODO_INTERNO_OU_SURFACE_EXPLICITA_PARA_SERVICOS_PROCESSUAIS_SOLTOS");
        LinkedHashMap<String, Object> evidencias = new LinkedHashMap<>();
        evidencias.put("modulos", modulos.size());
        evidencias.put("healthySystems", commandCenter.observability().healthySystems());
        evidencias.put("degradedSystems", commandCenter.observability().degradedSystems());
        evidencias.put("blockedSystems", commandCenter.observability().blockedSystems());
        return new ManifestoBundle(
                eixo(
                        "manifesto.modular",
                        "Manifesto nacional de módulos e conexões",
                        score,
                        score >= 75 && commandCenter.observability().blockedSystems() == 0,
                        sinais,
                        bloqueadores,
                        proximasAcoes,
                        evidencias
                ),
                modulos
        );
    }

    private PjbPlataformaSustentacaoEixo avaliarFeatureFlagsFederativas() {
        List<NationalFeatureRolloutResponse> amostras = List.of(
                resolveFeature("WORKFLOW", "TJCE", JudicialSystem.PJE, "SERVIDOR"),
                resolveFeature("SEARCH", "TJSP", JudicialSystem.ESAJ, "MAGISTRADO"),
                resolveFeature("KAFKA", "TJRS", JudicialSystem.EPROC, "ADMINISTRADOR"),
                resolveFeature("GOV_VITAL_MONITOR", "TRF4", JudicialSystem.EPROC, "ADMINISTRADOR")
        );
        long enabled = amostras.stream().filter(NationalFeatureRolloutResponse::enabled).count();
        long warnings = amostras.stream().mapToLong(response -> response.warnings().size()).sum();
        int score = Math.max(0, Math.min(100, 52 + (int) (enabled * 12) - (int) (warnings * 2)
                + (featureFlagsProperties.getWorkflow().isEnabled() ? 8 : 0)
                + (featureFlagsProperties.getSearch().isEnabled() ? 8 : 0)
                + (featureFlagsProperties.getKafka().isEnabled() ? 8 : 0)));
        LinkedHashSet<String> sinais = new LinkedHashSet<>();
        sinais.add("workflow=" + featureFlagsProperties.getWorkflow().isEnabled());
        sinais.add("search=" + featureFlagsProperties.getSearch().isEnabled());
        sinais.add("kafka=" + featureFlagsProperties.getKafka().isEnabled());
        sinais.add("govVitalMonitor=" + featureFlagsProperties.getGov().getVitalMonitor().isEnabled());
        sinais.add("rolloutsAmostrados=" + amostras.size());
        LinkedHashSet<String> bloqueadores = new LinkedHashSet<>();
        if (enabled == 0) {
            bloqueadores.add("nenhuma_feature_federativa_ativa_na_amostra");
        }
        amostras.stream().flatMap(response -> response.warnings().stream()).forEach(bloqueadores::add);
        LinkedHashSet<String> proximasAcoes = new LinkedHashSet<>();
        proximasAcoes.add("CENTRALIZAR_TOGGLES_POR_TRIBUNAL_RAMO_RITO_E_UNIDADE");
        proximasAcoes.add("AMARRAR_ROLLOUT_FEDERATIVO_A_GUARDRAILS_DE_CUTOVER_E_ROLLBACK");
        LinkedHashMap<String, Object> evidencias = new LinkedHashMap<>();
        evidencias.put("amostras", amostras.stream().map(this::toMap).toList());
        evidencias.put("tribunaisAvaliados", amostras.stream().map(NationalFeatureRolloutResponse::tribunalCodigo).distinct().toList());
        return eixo(
                "rollout.feature-flags",
                "Feature flags federativas e rollout controlado",
                score,
                enabled >= 2 && warnings <= 4,
                sinais,
                bloqueadores,
                proximasAcoes,
                evidencias
        );
    }

    private PjbPlataformaSustentacaoEixo avaliarConfiabilidadeInstitucional() {
        long pendingOutbox = outboxEventRepository != null ? outboxEventRepository.countByStatus(OutboxStatus.PENDING) : -1L;
        long inflightOutbox = outboxEventRepository != null ? outboxEventRepository.countByStatus(OutboxStatus.INFLIGHT) : -1L;
        long failedOutbox = outboxEventRepository != null ? outboxEventRepository.countByStatus(OutboxStatus.FAILED) : -1L;
        long inboxSnapshots = institutionalInboxItemSnapshotRepository != null ? institutionalInboxItemSnapshotRepository.count() : -1L;
        int score = average(
                outboxEventRepository != null ? (failedOutbox == 0 ? 88 : failedOutbox < 5 ? 70 : 45) : 42,
                institutionalInboxItemSnapshotRepository != null ? 84 : 38,
                actionIdempotencyService != null ? 86 : 40,
                judicialConnectorCommandCenterService.nationalReport(Duration.ofHours(24)).observability().blockedSystems() == 0 ? 82 : 54
        );
        LinkedHashSet<String> sinais = new LinkedHashSet<>();
        sinais.add("outboxRepository=" + (outboxEventRepository != null));
        sinais.add("inboxRepository=" + (institutionalInboxItemSnapshotRepository != null));
        sinais.add("idempotencyService=" + (actionIdempotencyService != null));
        if (outboxEventRepository != null) {
            sinais.add("outboxPending=" + pendingOutbox);
            sinais.add("outboxInflight=" + inflightOutbox);
            sinais.add("outboxFailed=" + failedOutbox);
        }
        if (institutionalInboxItemSnapshotRepository != null) {
            sinais.add("inboxSnapshots=" + inboxSnapshots);
        }
        LinkedHashSet<String> bloqueadores = new LinkedHashSet<>();
        if (outboxEventRepository == null) {
            bloqueadores.add("outbox_repository_indisponivel");
        }
        if (institutionalInboxItemSnapshotRepository == null) {
            bloqueadores.add("inbox_repository_indisponivel");
        }
        if (failedOutbox > 0) {
            bloqueadores.add("outbox_failed=" + failedOutbox);
        }
        LinkedHashSet<String> proximasAcoes = new LinkedHashSet<>();
        proximasAcoes.add("AMPLIAR_LEDGER_DE_IDEMPOTENCIA_E_REPLAY_SEGURO_PARA_OPERACOES_CRITICAS");
        proximasAcoes.add("TRAVAR_PROMOCAO_DE_BUILD_COM_OUTBOX_FALHADO_OU_INBOX_SEM_RECONCILIACAO");
        LinkedHashMap<String, Object> evidencias = new LinkedHashMap<>();
        evidencias.put("outboxPending", pendingOutbox);
        evidencias.put("outboxInflight", inflightOutbox);
        evidencias.put("outboxFailed", failedOutbox);
        evidencias.put("inboxSnapshots", inboxSnapshots);
        return eixo(
                "confiabilidade.institucional",
                "Outbox, inbox, idempotência e replay",
                score,
                outboxEventRepository != null && institutionalInboxItemSnapshotRepository != null && failedOutbox <= 0,
                sinais,
                bloqueadores,
                proximasAcoes,
                evidencias
        );
    }

    private PjbPlataformaSustentacaoEixo avaliarMotorSigiloCentral() {
        SigiloDecision violenciaDomestica = sigiloService.avaliarCorpus("violencia domestica com medida protetiva, menor e prontuario medico sensivel");
        SigiloDecision penalSensivel = sigiloService.avaliarCorpus("prisao em flagrante com dado bancario, fiscal e investigacao penal sensivel");
        long pendentes = sigiloAccessRequestRepository != null ? sigiloAccessRequestRepository.findByStatusAndExpiresAtBefore(SigiloAccessStatus.APROVADA, java.time.LocalDateTime.now()).size() : 0L;
        int score = average(
                violenciaDomestica.score() >= 70 ? 92 : 58,
                penalSensivel.score() >= 70 ? 92 : 58,
                sigiloAccessRequestRepository != null ? 84 : 42,
                beanAvailable("processoMalhaSigiloAuthorizationService") ? 88 : 46
        );
        LinkedHashSet<String> sinais = new LinkedHashSet<>();
        sinais.add("violenciaDomesticaNivel=" + violenciaDomestica.nivel().name());
        sinais.add("penalSensivelNivel=" + penalSensivel.nivel().name());
        sinais.add("sigiloAccessRepository=" + (sigiloAccessRequestRepository != null));
        sinais.add("malhaSigiloAuthorization=" + beanAvailable("processoMalhaSigiloAuthorizationService"));
        LinkedHashSet<String> bloqueadores = new LinkedHashSet<>();
        if (sigiloAccessRequestRepository == null) {
            bloqueadores.add("sigilo_access_repository_indisponivel");
        }
        if (violenciaDomestica.score() < 70 || penalSensivel.score() < 70) {
            bloqueadores.add("motor_sigilo_nao_reagiu_com_forca_suficiente_a_corpus_sensivel");
        }
        if (pendentes > 0) {
            bloqueadores.add("credenciais_sigilo_expiradas_ou_pendentes=" + pendentes);
        }
        LinkedHashSet<String> proximasAcoes = new LinkedHashSet<>();
        proximasAcoes.add("UNIFICAR_NASCIMENTO_DE_SIGILO_COM_STEP_UP_MASCARAMENTO_E_AUDITORIA");
        proximasAcoes.add("TRAVAR_COMUNICACAO_EXTERNA_QUANDO_NIVEL_DE_SIGILO_EXIGIR_ENVELOPE_REFORCADO");
        LinkedHashMap<String, Object> evidencias = new LinkedHashMap<>();
        evidencias.put("violenciaDomesticaSignals", violenciaDomestica.signals().stream().map(Enum::name).toList());
        evidencias.put("penalSensivelSignals", penalSensivel.signals().stream().map(Enum::name).toList());
        evidencias.put("violenciaDomesticaRecomendacoes", violenciaDomestica.recomendacoes());
        evidencias.put("penalSensivelRecomendacoes", penalSensivel.recomendacoes());
        evidencias.put("credenciaisExpiradas", pendentes);
        return eixo(
                "sigilo.central",
                "Motor central de sigilo, step-up e credencial",
                score,
                violenciaDomestica.score() >= 70 && penalSensivel.score() >= 70 && sigiloAccessRequestRepository != null,
                sinais,
                bloqueadores,
                proximasAcoes,
                evidencias
        );
    }

    private PjbPlataformaSustentacaoEixo avaliarNormalizadorNacional() {
        List<Map<String, Object>> amostras = List.of(
                samplePayload("TJCE", "CIVEL", "cumprimento de contrato bancario", "procedimento comum civel", "ACAO_DE_CONHECIMENTO"),
                samplePayload("TRT7", "TRABALHISTA", "verbas rescisorias e horas extras", "rito sumarissimo trabalhista", "RECLAMACAO_TRABALHISTA"),
                samplePayload("TRE-CE", "ELEITORAL", "registro de candidatura", "acao eleitoral", "REGISTRO_CANDIDATURA"),
                samplePayload("TRF4", "FEDERAL", "medicamento de alto custo contra uniao", "procedimento comum", "OBRIGACAO_DE_FAZER")
        );
        ArrayList<Map<String, Object>> resolucoes = new ArrayList<>();
        int scoreAcumulado = 0;
        LinkedHashSet<String> bloqueadores = new LinkedHashSet<>();
        for (Map<String, Object> amostra : amostras) {
            ProceduralCanonicalResolver.CanonicalContext canonical = proceduralCanonicalResolver.resolve(amostra);
            ProceduralRoutingReport routing = nationalProceduralRoutingService.analyzeContext(amostra);
            int score = 40;
            if (canonical.rito() != null) {
                score += 20;
            } else {
                bloqueadores.add("normalizador_sem_rito:" + Objects.toString(amostra.get("tribunalCodigo"), "SEM_TRIBUNAL"));
            }
            if (canonical.tribunalCodigo() != null && !canonical.tribunalCodigo().isBlank()) {
                score += 20;
            } else {
                bloqueadores.add("normalizador_sem_tribunal:" + Objects.toString(amostra.get("tribunalCodigo"), "SEM_TRIBUNAL"));
            }
            if (canonical.classeTpuCodigo() != null && !canonical.classeTpuCodigo().isBlank()) {
                score += 10;
            }
            if (routing != null && routing.tribunalCodigo() != null && !routing.tribunalCodigo().isBlank()) {
                score += 10;
            }
            scoreAcumulado += score;
            LinkedHashMap<String, Object> linha = new LinkedHashMap<>();
            linha.put("tribunalCodigo", amostra.get("tribunalCodigo"));
            linha.put("ramo", canonical.ramoDireito());
            linha.put("rito", canonical.rito() != null ? canonical.rito().name() : null);
            linha.put("classeTpuCodigo", canonical.classeTpuCodigo());
            linha.put("tribunalNormalizado", canonical.tribunalCodigo());
            linha.put("judicialSystemPreferido", canonical.judicialSystemPreferido());
            linha.put("score", score);
            resolucoes.add(cleanMap(linha));
        }
        int score = amostras.isEmpty() ? 0 : Math.max(0, Math.min(100, scoreAcumulado / amostras.size()));
        LinkedHashSet<String> sinais = new LinkedHashSet<>();
        sinais.add("amostrasNormalizadas=" + amostras.size());
        sinais.add("tribunaisComMatriz=" + resolucoes.stream().filter(item -> item.get("tribunalNormalizado") != null).count());
        sinais.add("classesNormalizadas=" + resolucoes.stream().filter(item -> item.get("classeTpuCodigo") != null).count());
        LinkedHashSet<String> proximasAcoes = new LinkedHashSet<>();
        proximasAcoes.add("CENTRALIZAR_COMPETENCIA_CLASSE_E_RITO_NO_CANONICAL_RESOLVER");
        proximasAcoes.add("IMPEDIR_DESVIOS_LOCAIS_DE_COMPETENCIA_FORA_DO_NORMALIZADOR_NACIONAL");
        LinkedHashMap<String, Object> evidencias = new LinkedHashMap<>();
        evidencias.put("amostras", resolucoes);
        evidencias.put("tribunaisDaMatriz", List.of(NationalCompetenceMatrix.TJCE.codigo(), NationalCompetenceMatrix.TJSP.codigo(), NationalCompetenceMatrix.TRF4.codigo()));
        return eixo(
                "normalizador.nacional",
                "Normalizador nacional de competência, classe e rito",
                score,
                score >= 75 && bloqueadores.isEmpty(),
                sinais,
                bloqueadores,
                proximasAcoes,
                evidencias
        );
    }

    private PjbPlataformaSustentacaoEixo avaliarShadowCompareMigracao() {
        List<Processo> processos = processoRepository.findAll(PageRequest.of(0, 6)).getContent();
        ArrayList<Map<String, Object>> amostras = new ArrayList<>();
        LinkedHashSet<String> bloqueadores = new LinkedHashSet<>();
        int scoreTotal = 0;
        if (processos.isEmpty()) {
            bloqueadores.add("sem_amostra_de_processo_para_shadow_compare");
        }
        for (Processo processo : processos) {
            try {
                ProcessoMigracaoFabricaAggregate fabrica = processoMigracaoFactoryApplicationService.planejar(processo.getId());
                ProcessoMigracaoAggregate migracao = processoMigracaoApplicationService.detalhar(processo.getId());
                int score = average(fabrica.scoreGeral(), "READY_FOR_CUTOVER".equalsIgnoreCase(migracao.readiness()) ? 94 : "READY_FOR_SHADOW".equalsIgnoreCase(migracao.readiness()) ? 76 : 52, migracao.canCutOver() ? 92 : 60);
                scoreTotal += score;
                LinkedHashMap<String, Object> linha = new LinkedHashMap<>();
                linha.put("processoId", processo.getId());
                linha.put("scoreFactory", fabrica.scoreGeral());
                linha.put("factoryStatus", fabrica.statusGeral().name());
                linha.put("migrationReadiness", migracao.readiness());
                linha.put("canCutOver", migracao.canCutOver());
                linha.put("comparacoes", migracao.comparacoes().size());
                linha.put("bloqueiosFactory", fabrica.bloqueios());
                amostras.add(cleanMap(linha));
                if (!migracao.canCutOver()) {
                    bloqueadores.add("shadow_compare_bloqueado:processo=" + processo.getId());
                }
            } catch (RuntimeException ex) {
                bloqueadores.add("shadow_compare_falhou:processo=" + processo.getId());
            }
        }
        int score = processos.isEmpty() ? 35 : Math.max(0, Math.min(100, scoreTotal / Math.max(1, amostras.size())));
        LinkedHashSet<String> sinais = new LinkedHashSet<>();
        sinais.add("processosAmostrados=" + processos.size());
        sinais.add("processosComShadowCompare=" + amostras.size());
        sinais.add("bloqueadoresShadow=" + bloqueadores.size());
        LinkedHashSet<String> proximasAcoes = new LinkedHashSet<>();
        proximasAcoes.add("EXECUTAR_RECONCILIACAO_AUTOMATICA_DE_METADADOS_ANTES_DO_CUTOVER");
        proximasAcoes.add("FORMALIZAR_RELATORIO_DE_DIVERGENCIA_LEGADO_VS_PJB_POR_LOTE_DE_MIGRACAO");
        LinkedHashMap<String, Object> evidencias = new LinkedHashMap<>();
        evidencias.put("amostras", amostras);
        evidencias.put("totalProcessosPersistidos", processoRepository.count());
        return eixo(
                "migracao.shadow-compare",
                "Shadow compare, reconciliação e migração",
                score,
                !processos.isEmpty() && bloqueadores.isEmpty(),
                sinais,
                bloqueadores,
                proximasAcoes,
                evidencias
        );
    }

    private GoldenBundle avaliarCenariosDourados() {
        List<PjbPlataformaSustentacaoCenario> cenarios = List.of(
                cenario("civil-comum", "Cível comum com competência estadual", "TJCE", samplePayload("TJCE", "CIVEL", "cumprimento de contrato e danos morais", "procedimento comum civel", "ACAO_DE_CONHECIMENTO")),
                cenario("penal-sigilo", "Penal sensível com reforço de sigilo", "TJSP", samplePayload("TJSP", "PENAL", "prisao em flagrante com quebra de sigilo bancario", "procedimento penal comum", "ACAO_PENAL")),
                cenario("trabalhista-calculo", "Trabalhista com cálculo e rito sumaríssimo", "TRT7", samplePayload("TRT7", "TRABALHISTA", "horas extras e verbas rescisorias", "rito sumarissimo trabalhista", "RECLAMACAO_TRABALHISTA")),
                cenario("fazenda-publica", "Fazenda pública federal", "TRF4", samplePayload("TRF4", "FAZENDA_PUBLICA", "medicamento de alto custo e tutela de urgencia", "procedimento comum fazenda", "OBRIGACAO_DE_FAZER")),
                cenario("coletivo", "Tutela coletiva com execução em massa", "TJRS", samplePayload("TJRS", "COLETIVO", "acao civil publica por dano ambiental com liquidacao coletiva", "coletivo estruturante", "ACAO_CIVIL_PUBLICA")),
                cenario("precedente-repetitivo", "Repetitivo com sobrestamento", "STJ", samplePayload("STJ", "CIVEL", "tema repetitivo com sobrestamento de demandas", "recurso repetitivo", "RECURSO_ESPECIAL_REPETITIVO"))
        );
        int score = average(cenarios.stream().mapToInt(PjbPlataformaSustentacaoCenario::score).toArray());
        LinkedHashSet<String> sinais = new LinkedHashSet<>();
        sinais.add("cenariosDourados=" + cenarios.size());
        sinais.add("cenariosAptos=" + cenarios.stream().filter(PjbPlataformaSustentacaoCenario::apto).count());
        LinkedHashSet<String> bloqueadores = new LinkedHashSet<>();
        cenarios.stream().filter(cenario -> !cenario.apto()).map(PjbPlataformaSustentacaoCenario::codigo).map(codigo -> "cenario_nao_apto:" + codigo).forEach(bloqueadores::add);
        LinkedHashSet<String> proximasAcoes = new LinkedHashSet<>();
        proximasAcoes.add("MATERIALIZAR_SUITE_DE_CASOS_DOURADOS_END_TO_END_POR_RAMO_E_RITO");
        proximasAcoes.add("ANEXAR_SHADOW_COMPARE_E_SIGILO_AOS_CENARIOS_CRITICOS_DE_BUILD");
        LinkedHashMap<String, Object> evidencias = new LinkedHashMap<>();
        evidencias.put("cenarios", cenarios.stream().map(this::toMap).toList());
        return new GoldenBundle(
                eixo(
                        "cenarios.dourados",
                        "Casos dourados processuais e prova de integração",
                        score,
                        score >= 70 && bloqueadores.isEmpty(),
                        sinais,
                        bloqueadores,
                        proximasAcoes,
                        evidencias
                ),
                cenarios
        );
    }

    private PjbPlataformaSustentacaoCenario cenario(String codigo,
                                                    String titulo,
                                                    String tribunalCodigo,
                                                    Map<String, Object> payload) {
        ProceduralCanonicalResolver.CanonicalContext canonical = proceduralCanonicalResolver.resolve(payload);
        ProceduralRoutingReport routing = nationalProceduralRoutingService.analyzeContext(payload);
        SigiloDecision sigilo = sigiloService.avaliarCorpus(Objects.toString(payload.get("resumo"), ""));
        LinkedHashSet<String> alertas = new LinkedHashSet<>();
        int score = 35;
        if (canonical.tribunalCodigo() != null && !canonical.tribunalCodigo().isBlank()) {
            score += 20;
        } else {
            alertas.add("tribunal_nao_normalizado");
        }
        if (canonical.rito() != null) {
            score += 20;
        } else {
            alertas.add("rito_nao_resolvido");
        }
        if (canonical.classeTpuCodigo() != null) {
            score += 10;
        }
        if (routing.tribunalCodigo() != null && !routing.tribunalCodigo().isBlank()) {
            score += 10;
        } else {
            alertas.add("routing_sem_tribunal");
        }
        if (sigilo.nivel().nivel() >= 1 && Objects.toString(payload.get("resumo"), "").toLowerCase(Locale.ROOT).contains("sigilo")) {
            score += 5;
        }
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        fundamentos.add("tribunal=" + canonical.tribunalCodigo());
        fundamentos.add("rito=" + (canonical.rito() != null ? canonical.rito().name() : "NAO_RESOLVIDO"));
        fundamentos.add("classeTpu=" + Objects.toString(canonical.classeTpuCodigo(), "SEM_CLASSE"));
        fundamentos.add("sigiloNivel=" + sigilo.nivel().name());
        return new PjbPlataformaSustentacaoCenario(
                codigo,
                titulo,
                tribunalCodigo,
                Objects.toString(canonical.ramoDireito(), Objects.toString(payload.get("ramoDireito"), null)),
                canonical.rito() != null ? canonical.rito().name() : null,
                Math.max(0, Math.min(100, score)),
                alertas.isEmpty(),
                List.copyOf(alertas),
                List.copyOf(fundamentos)
        );
    }

    private NationalFeatureRolloutResponse resolveFeature(String featureCode,
                                                          String tribunalCodigo,
                                                          JudicialSystem judicialSystem,
                                                          String profile) {
        return nationalFeatureRolloutService.resolve(new NationalFeatureRolloutRequest(
                featureCode,
                null,
                tribunalCodigo,
                judicialSystem,
                profile,
                null,
                null,
                null
        ));
    }

    private PjbPlataformaSustentacaoEixo eixo(String codigo,
                                              String titulo,
                                              int score,
                                              boolean pronto,
                                              LinkedHashSet<String> sinais,
                                              LinkedHashSet<String> bloqueadores,
                                              LinkedHashSet<String> proximasAcoes,
                                              Map<String, Object> evidencias) {
        return new PjbPlataformaSustentacaoEixo(
                codigo,
                titulo,
                Math.max(0, Math.min(100, score)),
                pronto ? "PRONTO" : score >= 70 ? "PARCIAL" : "BLOQUEADO",
                pronto,
                List.copyOf(sinais),
                List.copyOf(bloqueadores),
                List.copyOf(proximasAcoes),
                cleanMap(evidencias)
        );
    }

    private PjbPlataformaSustentacaoModulo modulo(String codigo,
                                                  String titulo,
                                                  String camada,
                                                  int beansConectados,
                                                  int score,
                                                  List<String> conexoes,
                                                  List<String> riscos) {
        return new PjbPlataformaSustentacaoModulo(
                codigo,
                titulo,
                camada,
                Math.max(0, beansConectados),
                Math.max(0, Math.min(100, score)),
                score >= 80 ? "CONECTADO" : score >= 65 ? "PARCIAL" : "FRAGIL",
                conexoes,
                riscos.stream().distinct().toList()
        );
    }

    private Map<String, Object> samplePayload(String tribunalCodigo,
                                              String ramo,
                                              String resumo,
                                              String rito,
                                              String classe) {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("tribunalCodigo", tribunalCodigo);
        payload.put("ramoDireito", ramo);
        payload.put("resumo", resumo);
        payload.put("assunto", resumo);
        payload.put("rito", rito);
        payload.put("classe", classe);
        payload.put("classeProcessual", classe);
        payload.put("tipoAcao", classe);
        payload.put("ufAutor", tribunalCodigo.contains("TJ") || tribunalCodigo.contains("TRT") || tribunalCodigo.contains("TRE") ? tribunalCodigo.substring(tribunalCodigo.length() - 2).replace("-", "") : "BR");
        payload.put("valorCausa", java.math.BigDecimal.valueOf(25000));
        payload.put("pedidoPrincipal", resumo);
        return payload;
    }

    private int average(int... values) {
        if (values == null || values.length == 0) {
            return 0;
        }
        int total = 0;
        for (int value : values) {
            total += value;
        }
        return Math.max(0, Math.min(100, total / values.length));
    }

    private int scorePresence(Object... items) {
        int count = 0;
        if (items != null) {
            for (Object item : items) {
                if (item != null) {
                    count++;
                }
            }
        }
        return count;
    }

    private int beanScore(String packageFragment) {
        int count = countBeansContaining(packageFragment);
        return count >= 12 ? 92 : count >= 6 ? 82 : count >= 3 ? 68 : count > 0 ? 55 : 30;
    }

    private int countBeansContaining(String packageFragment) {
        int count = 0;
        for (Object bean : applicationContext.getBeansWithAnnotation(Service.class).values()) {
            Class<?> targetClass = AopUtils.getTargetClass(bean);
            if (targetClass != null && targetClass.getPackage() != null && targetClass.getPackage().getName().contains(packageFragment)) {
                count++;
            }
        }
        return count;
    }

    private boolean beanAvailable(String beanName) {
        return applicationContext.containsBean(beanName);
    }

    private Map<String, Object> cleanMap(Map<String, Object> source) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        if (source != null) {
            for (Map.Entry<String, Object> entry : source.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    out.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return Collections.unmodifiableMap(out);
    }

    private Map<String, Object> toMap(NationalFeatureRolloutResponse response) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("featureCode", response.featureCode());
        out.put("enabled", response.enabled());
        out.put("rolloutMode", response.rolloutMode());
        out.put("thresholdPercent", response.thresholdPercent());
        out.put("tribunalCodigo", response.tribunalCodigo());
        out.put("perfilAlvo", response.perfilAlvo());
        out.put("warnings", response.warnings());
        return cleanMap(out);
    }

    private Map<String, Object> toMap(PjbPlataformaSustentacaoCenario cenario) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("codigo", cenario.codigo());
        out.put("tribunalCodigo", cenario.tribunalCodigo());
        out.put("ramo", cenario.ramo());
        out.put("rito", cenario.rito());
        out.put("score", cenario.score());
        out.put("apto", cenario.apto());
        out.put("alertas", cenario.alertas());
        return cleanMap(out);
    }

    private record ManifestoBundle(PjbPlataformaSustentacaoEixo eixo,
                                   List<PjbPlataformaSustentacaoModulo> modulos) {
    }

    private record GoldenBundle(PjbPlataformaSustentacaoEixo eixo,
                                List<PjbPlataformaSustentacaoCenario> cenarios) {
    }
}
