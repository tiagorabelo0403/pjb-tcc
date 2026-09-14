package com.tcc.pjb.backend.core.plataforma.substituicao.application;

import static com.tcc.pjb.backend.core.plataforma.substituicao.application.PjbArquiteturaSubstituicaoPilarSupport.available;
import static com.tcc.pjb.backend.core.plataforma.substituicao.application.PjbArquiteturaSubstituicaoPilarSupport.capacidade;
import static com.tcc.pjb.backend.core.plataforma.substituicao.application.PjbArquiteturaSubstituicaoPilarSupport.format;
import static com.tcc.pjb.backend.core.plataforma.substituicao.application.PjbArquiteturaSubstituicaoPilarSupport.pilar;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.explainability.DecisionTraceService;
import com.tcc.pjb.backend.core.governance.idempotency.RequestIdempotencyService;
import com.tcc.pjb.backend.core.idempotency.ActionIdempotencyService;
import com.tcc.pjb.backend.core.jobs.runtime.JobAdminService;
import com.tcc.pjb.backend.core.jobs.runtime.JobCircuitBreaker;
import com.tcc.pjb.backend.core.jobs.runtime.JobExecutionService;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbArquiteturaSubstituicaoCapacidade;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbArquiteturaSubstituicaoPilar;
import com.tcc.pjb.backend.core.processo.producao.application.ProcessoOperacaoTransversalApplicationService;
import com.tcc.pjb.backend.core.resilience.LocalCircuitBreaker;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.service.admin.AdministradorNacionalGovernanceService;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Avalia o pilar "confiabilidade institucional" (Fatia F6 -- extraído de
 * PjbArquiteturaSubstituicaoNacionalApplicationService).
 */
@Component
public class PjbArquiteturaConfiabilidadePilarEvaluator {

    private final AdministradorNacionalGovernanceService administradorNacionalGovernanceService;
    private final ObjectProvider<ProcessoOperacaoTransversalApplicationService> operacaoTransversalProvider;
    private final ObjectProvider<ActionIdempotencyService> actionIdempotencyServiceProvider;
    private final ObjectProvider<RequestIdempotencyService> requestIdempotencyServiceProvider;
    private final ObjectProvider<JobExecutionService> jobExecutionServiceProvider;
    private final ObjectProvider<JobAdminService> jobAdminServiceProvider;
    private final ObjectProvider<LocalCircuitBreaker> localCircuitBreakerProvider;
    private final ObjectProvider<JobCircuitBreaker> jobCircuitBreakerProvider;
    private final ObjectProvider<AuditLedgerService> auditLedgerServiceProvider;
    private final ObjectProvider<DecisionTraceService> decisionTraceServiceProvider;
    private final ObjectProvider<PjbAuthorizationService> pjbAuthorizationServiceProvider;

    public PjbArquiteturaConfiabilidadePilarEvaluator(
            AdministradorNacionalGovernanceService administradorNacionalGovernanceService,
            ObjectProvider<ProcessoOperacaoTransversalApplicationService> operacaoTransversalProvider,
            ObjectProvider<ActionIdempotencyService> actionIdempotencyServiceProvider,
            ObjectProvider<RequestIdempotencyService> requestIdempotencyServiceProvider,
            ObjectProvider<JobExecutionService> jobExecutionServiceProvider,
            ObjectProvider<JobAdminService> jobAdminServiceProvider,
            ObjectProvider<LocalCircuitBreaker> localCircuitBreakerProvider,
            ObjectProvider<JobCircuitBreaker> jobCircuitBreakerProvider,
            ObjectProvider<AuditLedgerService> auditLedgerServiceProvider,
            ObjectProvider<DecisionTraceService> decisionTraceServiceProvider,
            ObjectProvider<PjbAuthorizationService> pjbAuthorizationServiceProvider) {
        this.administradorNacionalGovernanceService = Objects.requireNonNull(administradorNacionalGovernanceService);
        this.operacaoTransversalProvider = Objects.requireNonNull(operacaoTransversalProvider);
        this.actionIdempotencyServiceProvider = Objects.requireNonNull(actionIdempotencyServiceProvider);
        this.requestIdempotencyServiceProvider = Objects.requireNonNull(requestIdempotencyServiceProvider);
        this.jobExecutionServiceProvider = Objects.requireNonNull(jobExecutionServiceProvider);
        this.jobAdminServiceProvider = Objects.requireNonNull(jobAdminServiceProvider);
        this.localCircuitBreakerProvider = Objects.requireNonNull(localCircuitBreakerProvider);
        this.jobCircuitBreakerProvider = Objects.requireNonNull(jobCircuitBreakerProvider);
        this.auditLedgerServiceProvider = Objects.requireNonNull(auditLedgerServiceProvider);
        this.decisionTraceServiceProvider = Objects.requireNonNull(decisionTraceServiceProvider);
        this.pjbAuthorizationServiceProvider = Objects.requireNonNull(pjbAuthorizationServiceProvider);
    }

    public PjbArquiteturaSubstituicaoPilar avaliar(long pendentes, long expirados, boolean buildGateAprovado) {
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
}
