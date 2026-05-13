package com.tcc.pjb.backend.core.processo.policy.application;

import com.tcc.pjb.backend.core.processo.dsl.application.ProcessoDslApplicationService;
import com.tcc.pjb.backend.core.processo.dsl.domain.ProcessoDslAggregate;
import com.tcc.pjb.backend.core.processo.dsl.domain.ProcessoDslBlock;
import com.tcc.pjb.backend.core.processo.dsl.domain.ProcessoDslRule;
import com.tcc.pjb.backend.core.processo.policy.domain.ProcessoPolicyAggregate;
import com.tcc.pjb.backend.core.processo.policy.domain.ProcessoPolicyDecision;
import com.tcc.pjb.backend.core.processo.policy.domain.ProcessoPolicyWindow;
import com.tcc.pjb.backend.core.processo.prazo.application.ProcessoPrazoApplicationService;
import com.tcc.pjb.backend.core.processo.prazo.domain.ProcessoPrazoAggregate;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ProcessoPolicyVigenciaApplicationService {

    private final ProcessoDslApplicationService processoDslApplicationService;
    private final ProcessoPrazoApplicationService processoPrazoApplicationService;

    public ProcessoPolicyVigenciaApplicationService(ProcessoDslApplicationService processoDslApplicationService,
                                                    ProcessoPrazoApplicationService processoPrazoApplicationService) {
        this.processoDslApplicationService = Objects.requireNonNull(processoDslApplicationService);
        this.processoPrazoApplicationService = Objects.requireNonNull(processoPrazoApplicationService);
    }

    public ProcessoPolicyAggregate avaliar(Long processoId) {
        return avaliar(processoId, LocalDate.now());
    }

    public ProcessoPolicyAggregate avaliar(Long processoId, LocalDate referenceDate) {
        ProcessoDslAggregate dsl = processoDslApplicationService.detalhar(processoId);
        ProcessoPrazoAggregate prazo = processoPrazoApplicationService.detalhar(processoId);
        List<ProcessoPolicyWindow> windows = dsl.blocks().stream()
                .map(block -> toWindow(block, referenceDate))
                .toList();
        List<ProcessoPolicyDecision> decisions = dsl.blocks().stream()
                .map(block -> toDecision(block, referenceDate, prazo))
                .toList();
        long blockingPolicies = decisions.stream().filter(decision -> decision.active() && !decision.deferredRules().isEmpty()).count();
        return new ProcessoPolicyAggregate(
                dsl.identity(),
                referenceDate,
                windows.size(),
                windows.stream().filter(ProcessoPolicyWindow::active).count(),
                blockingPolicies,
                windows,
                decisions,
                invariants(dsl, prazo),
                Instant.now()
        );
    }

    private ProcessoPolicyWindow toWindow(ProcessoDslBlock block, LocalDate referenceDate) {
        LocalDate from = block.rules().stream().map(ProcessoDslRule::effectiveFrom).filter(Objects::nonNull).min(LocalDate::compareTo).orElse(LocalDate.of(2026, 1, 1));
        LocalDate until = block.rules().stream().map(ProcessoDslRule::effectiveUntil).filter(Objects::nonNull).max(LocalDate::compareTo).orElse(null);
        long activeRules = block.rules().stream().filter(rule -> isActive(rule, referenceDate)).count();
        return new ProcessoPolicyWindow(
                block.code(),
                block.title(),
                from,
                until,
                activeRules > 0,
                activeRules,
                block.sources()
        );
    }

    private ProcessoPolicyDecision toDecision(ProcessoDslBlock block, LocalDate referenceDate, ProcessoPrazoAggregate prazo) {
        List<String> activeRules = block.rules().stream().filter(rule -> isActive(rule, referenceDate)).map(ProcessoDslRule::code).toList();
        List<String> deferredRules = block.rules().stream().filter(rule -> !isActive(rule, referenceDate) || rule.blocking()).map(ProcessoDslRule::code).toList();
        String severity = prazo.marcosCriticos() > 0 && activeRules.size() < block.rules().size() ? "CRITICA" : deferredRules.isEmpty() ? "CONTROLADA" : "ELEVADA";
        String rationale = prazo.marcosCriticos() > 0
                ? "Janela de prazo crítica exige política vigente explícita e sem lacunas de regra."
                : "A política versionada preserva a rastreabilidade entre regra ativa e regra diferida.";
        return new ProcessoPolicyDecision(
                "POLICY_" + block.code(),
                block.code(),
                !activeRules.isEmpty(),
                severity,
                resumo(block, activeRules, deferredRules),
                rationale,
                activeRules,
                deferredRules
        );
    }

    private String resumo(ProcessoDslBlock block, List<String> activeRules, List<String> deferredRules) {
        if (activeRules.isEmpty()) {
            return "Bloco " + block.code() + " sem regra vigente ativa na data consultada.";
        }
        if (deferredRules.isEmpty()) {
            return "Bloco " + block.code() + " totalmente coberto pela vigência ativa.";
        }
        return "Bloco " + block.code() + " com cobertura parcial: regras ativas e regras diferidas coexistem.";
    }

    private boolean isActive(ProcessoDslRule rule, LocalDate referenceDate) {
        boolean afterStart = !referenceDate.isBefore(rule.effectiveFrom());
        boolean beforeEnd = rule.effectiveUntil() == null || !referenceDate.isAfter(rule.effectiveUntil());
        return afterStart && beforeEnd;
    }

    private List<String> invariants(ProcessoDslAggregate dsl, ProcessoPrazoAggregate prazo) {
        LinkedHashSet<String> invariants = new LinkedHashSet<>(dsl.invariants());
        invariants.add("POLITICA_PRECISA_SABER_QUAL_REGRA_VALIA_NA_DATA_DA_CONSULTA");
        invariants.add("PRAZO_CRITICO_NAO_CONVIVE_COM_BLOCO_SEM_REGRA_ATIVA");
        if (prazo.marcosComCienciaObrigatoria() > 0) {
            invariants.add("CIENCIA_OBRIGATORIA_EXIGE_RASTRO_DE_VIGENCIA_E_DE_FECHAMENTO");
        }
        return List.copyOf(invariants);
    }
}
