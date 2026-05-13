package com.tcc.pjb.backend.core.processo.hardening.application;

import com.tcc.pjb.backend.core.processo.encaixe.application.ProcessoEncaixeFinalApplicationService;
import com.tcc.pjb.backend.core.processo.encaixe.domain.ProcessoEncaixeFinalAggregate;
import com.tcc.pjb.backend.core.processo.encaixe.domain.ProcessoEncaixeFinding;
import com.tcc.pjb.backend.core.processo.hardening.domain.ProcessoHardeningAggregate;
import com.tcc.pjb.backend.core.processo.hardening.domain.ProcessoHardeningFinding;
import com.tcc.pjb.backend.core.processo.operacao.application.ProcessoOperacaoApplicationService;
import com.tcc.pjb.backend.core.processo.operacao.domain.ProcessoOperacaoAggregate;
import com.tcc.pjb.backend.core.processo.policy.application.ProcessoPolicyVigenciaApplicationService;
import com.tcc.pjb.backend.core.processo.policy.domain.ProcessoPolicyAggregate;
import com.tcc.pjb.backend.core.processo.sigilo.application.ProcessoSigiloApplicationService;
import com.tcc.pjb.backend.core.processo.sigilo.domain.ProcessoSigiloAggregate;
import com.tcc.pjb.backend.core.processo.sigilo.domain.ProcessoSigiloFinding;
import com.tcc.pjb.backend.core.processo.timeline.application.ProcessoTimelineApplicationService;
import com.tcc.pjb.backend.core.processo.timeline.domain.ProcessoTimelineAggregate;
import com.tcc.pjb.backend.core.processo.unificado.application.ProcessoUnificadoApplicationService;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoAggregate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ProcessoHardeningFinalApplicationService {

    private final ProcessoUnificadoApplicationService processoUnificadoApplicationService;
    private final ProcessoEncaixeFinalApplicationService processoEncaixeFinalApplicationService;
    private final ProcessoSigiloApplicationService processoSigiloApplicationService;
    private final ProcessoPolicyVigenciaApplicationService processoPolicyVigenciaApplicationService;
    private final ProcessoTimelineApplicationService processoTimelineApplicationService;
    private final ProcessoOperacaoApplicationService processoOperacaoApplicationService;

    public ProcessoHardeningFinalApplicationService(ProcessoUnificadoApplicationService processoUnificadoApplicationService,
                                                    ProcessoEncaixeFinalApplicationService processoEncaixeFinalApplicationService,
                                                    ProcessoSigiloApplicationService processoSigiloApplicationService,
                                                    ProcessoPolicyVigenciaApplicationService processoPolicyVigenciaApplicationService,
                                                    ProcessoTimelineApplicationService processoTimelineApplicationService,
                                                    ProcessoOperacaoApplicationService processoOperacaoApplicationService) {
        this.processoUnificadoApplicationService = Objects.requireNonNull(processoUnificadoApplicationService);
        this.processoEncaixeFinalApplicationService = Objects.requireNonNull(processoEncaixeFinalApplicationService);
        this.processoSigiloApplicationService = Objects.requireNonNull(processoSigiloApplicationService);
        this.processoPolicyVigenciaApplicationService = Objects.requireNonNull(processoPolicyVigenciaApplicationService);
        this.processoTimelineApplicationService = Objects.requireNonNull(processoTimelineApplicationService);
        this.processoOperacaoApplicationService = Objects.requireNonNull(processoOperacaoApplicationService);
    }

    public ProcessoHardeningAggregate detalhar(Long processoId) {
        ProcessoUnificadoAggregate unificado = processoUnificadoApplicationService.detalhar(processoId);
        ProcessoEncaixeFinalAggregate encaixe = processoEncaixeFinalApplicationService.detalhar(processoId);
        ProcessoSigiloAggregate sigilo = processoSigiloApplicationService.detalhar(processoId);
        ProcessoPolicyAggregate policy = processoPolicyVigenciaApplicationService.avaliar(processoId);
        ProcessoTimelineAggregate timeline = processoTimelineApplicationService.detalhar(processoId);
        ProcessoOperacaoAggregate operacao = processoOperacaoApplicationService.detalhar(processoId);

        ArrayList<ProcessoHardeningFinding> findings = new ArrayList<>();
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>(unificado.competencia().fundamentos());
        fundamentos.addAll(sigilo.fundamentos());
        fundamentos.addAll(policy.invariants());
        fundamentos.add("O hardening final do processo precisa cruzar coerência, sigilo, operação, vigência e timeline antes do corte de produção.");

        for (ProcessoEncaixeFinding finding : encaixe.findings()) {
            findings.add(new ProcessoHardeningFinding(
                    finding.codigo(),
                    finding.eixo(),
                    finding.severidade(),
                    finding.bloqueante(),
                    finding.detalhe(),
                    blank(finding.remediacao()) ? List.of() : List.of(finding.remediacao())
            ));
        }
        for (ProcessoSigiloFinding finding : sigilo.findings()) {
            findings.add(new ProcessoHardeningFinding(
                    finding.code(),
                    "SIGILO",
                    finding.severity(),
                    finding.blocking(),
                    finding.detail(),
                    finding.correctiveActions()
            ));
        }
        if (policy.blockingPolicies() > 0) {
            findings.add(new ProcessoHardeningFinding(
                    "POLICY_ENGINE_CRITICO",
                    "VIGENCIA",
                    "CRITICAL",
                    true,
                    "O policy engine ainda mantém políticas bloqueantes ativas para o processo.",
                    List.of("SANEAR_POLICY_ENGINE", "REVALIDAR_JANELAS_NORMATIVAS")
            ));
        }
        if (timeline.totalBloqueantes() > 0) {
            findings.add(new ProcessoHardeningFinding(
                    "TIMELINE_COM_BLOQUEANTES",
                    "TIMELINE",
                    "ALTA",
                    true,
                    "A linha do tempo viva mantém pendências bloqueantes que travam a próxima onda operacional.",
                    List.of("DESBLOQUEAR_PENDENCIAS", "REALINHAR_NEXT_BEST_FLOW")
            ));
        }
        if (!"READY".equalsIgnoreCase(operacao.readiness()) && !"PRONTA".equalsIgnoreCase(operacao.readiness())) {
            findings.add(new ProcessoHardeningFinding(
                    "OPERACAO_AINDA_NAO_PRONTA",
                    "OPERACAO",
                    "ALTA",
                    true,
                    "A operação do processo ainda não atingiu readiness suficiente para corte amplo.",
                    operacao.acoesImediatas().isEmpty() ? List.of("ENDURECER_RESILIENCIA") : operacao.acoesImediatas()
            ));
        }
        if (sigilo.nivelSigilo().getNivel() >= 1 && !sigilo.exigeStepUp()) {
            findings.add(new ProcessoHardeningFinding(
                    "SIGILO_SEM_STEP_UP",
                    "SIGILO",
                    "CRITICAL",
                    true,
                    "Processo sigiloso sem exigência de step-up é incompatível com o hardening final.",
                    List.of("ATIVAR_STEP_UP", "RECALCULAR_GUARDAS_DE_SIGILO")
            ));
        }
        if (sigilo.nivelSigilo().getNivel() >= 1 && sigilo.exigeCredencial() && sigilo.approvedCredentials() == 0) {
            findings.add(new ProcessoHardeningFinding(
                    "SIGILO_SEM_CREDENCIAL_APROVADA",
                    "SIGILO",
                    "ALTA",
                    true,
                    "O processo sigiloso exige credencial reforçada, mas nenhuma credencial aprovada foi materializada.",
                    List.of("MATERIALIZAR_CREDENCIAL_REFORCADA", "REVALIDAR_GUARDAS_DE_SIGILO")
            ));
        }
        if (sigilo.nivelSigilo().getNivel() >= 2 && sigilo.totalFindings() > 0 && sigilo.findings().isEmpty()) {
            findings.add(new ProcessoHardeningFinding(
                    "SIGILO_COM_ACHADOS_PENDENTES",
                    "SIGILO",
                    "ALTA",
                    true,
                    "O agregado de sigilo materializou achados pendentes sem finding detalhado consolidado no hardening final.",
                    List.of("CONSOLIDAR_FINDINGS_DE_SIGILO", "REVISAR_MODO_DE_DIVULGACAO")
            ));
        }

        findings.sort(Comparator.comparing(ProcessoHardeningFinding::blocking).reversed()
                .thenComparing(this::severityRank)
                .thenComparing(ProcessoHardeningFinding::code));

        LinkedHashSet<String> axes = new LinkedHashSet<>();
        LinkedHashSet<String> correctivePlan = new LinkedHashSet<>();
        for (ProcessoHardeningFinding finding : findings) {
            axes.add(finding.axis());
            correctivePlan.addAll(finding.correctiveActions());
        }
        long blocking = findings.stream().filter(ProcessoHardeningFinding::blocking).count();
        long score = score(encaixe.score(), sigilo, policy, timeline, operacao, findings);
        return new ProcessoHardeningAggregate(
                unificado.identity(),
                readiness(score, blocking),
                score,
                blocking,
                findings.size(),
                List.copyOf(axes),
                findings,
                List.copyOf(correctivePlan),
                List.copyOf(fundamentos),
                Instant.now()
        );
    }

    private long score(long encaixeScore,
                       ProcessoSigiloAggregate sigilo,
                       ProcessoPolicyAggregate policy,
                       ProcessoTimelineAggregate timeline,
                       ProcessoOperacaoAggregate operacao,
                       List<ProcessoHardeningFinding> findings) {
        long score = Math.max(0L, Math.min(100L, encaixeScore));
        score -= findings.stream().filter(ProcessoHardeningFinding::blocking).count() * 7L;
        score -= policy.blockingPolicies() * 5L;
        score -= timeline.totalBloqueantes() * 4L;
        score -= operacao.totalBloqueios() * 3L;
        if (sigilo.nivelSigilo().getNivel() >= 2 && sigilo.approvedCredentials() == 0) {
            score -= 4L;
        }
        return Math.max(0L, Math.min(100L, score));
    }

    private String readiness(long score, long blocking) {
        if (blocking > 0 || score < 60) {
            return "NOT_READY";
        }
        if (score < 80) {
            return "HARDENING_EM_ANDAMENTO";
        }
        return "READY_FOR_PILOT";
    }

    private int severityRank(ProcessoHardeningFinding finding) {
        return switch (finding.severity()) {
            case "CRITICAL" -> 0;
            case "ALTA", "ELEVADA" -> 1;
            case "MEDIA" -> 2;
            default -> 3;
        };
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
