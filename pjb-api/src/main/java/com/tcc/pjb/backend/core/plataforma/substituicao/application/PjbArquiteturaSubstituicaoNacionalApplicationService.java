package com.tcc.pjb.backend.core.plataforma.substituicao.application;

import com.tcc.pjb.backend.core.competence.NationalCompetenceMatrix;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbArquiteturaSubstituicaoNacionalAggregate;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbArquiteturaSubstituicaoPilar;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.governance.BuildGateGovernanceService;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fatia F6: este bean tinha 40 dependências de construtor (34 ObjectProvider usados
 * apenas via presença no contexto Spring). A avaliação de cada um dos 4 pilares foi
 * extraída para um avaliador dedicado (seam real: os 4 pilares já eram métodos privados
 * distintos, cada um com seu próprio subconjunto de dependências). Este orquestrador
 * agora só monta o agregado final a partir dos 4 pilares.
 */
@Service
public class PjbArquiteturaSubstituicaoNacionalApplicationService {

    private final ProcessoRepository processoRepository;
    private final WorkItemRepository workItemRepository;
    private final BuildGateGovernanceService buildGateGovernanceService;
    private final PjbArquiteturaMotorProcessualPilarEvaluator motorProcessualEvaluator;
    private final PjbArquiteturaInteroperabilidadePilarEvaluator interoperabilidadeEvaluator;
    private final PjbArquiteturaConfiabilidadePilarEvaluator confiabilidadeEvaluator;
    private final PjbArquiteturaGovernancaPilarEvaluator governancaEvaluator;

    public PjbArquiteturaSubstituicaoNacionalApplicationService(
            ProcessoRepository processoRepository,
            WorkItemRepository workItemRepository,
            BuildGateGovernanceService buildGateGovernanceService,
            PjbArquiteturaMotorProcessualPilarEvaluator motorProcessualEvaluator,
            PjbArquiteturaInteroperabilidadePilarEvaluator interoperabilidadeEvaluator,
            PjbArquiteturaConfiabilidadePilarEvaluator confiabilidadeEvaluator,
            PjbArquiteturaGovernancaPilarEvaluator governancaEvaluator) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
        this.buildGateGovernanceService = Objects.requireNonNull(buildGateGovernanceService);
        this.motorProcessualEvaluator = Objects.requireNonNull(motorProcessualEvaluator);
        this.interoperabilidadeEvaluator = Objects.requireNonNull(interoperabilidadeEvaluator);
        this.confiabilidadeEvaluator = Objects.requireNonNull(confiabilidadeEvaluator);
        this.governancaEvaluator = Objects.requireNonNull(governancaEvaluator);
    }

    @Transactional(readOnly = true)
    public PjbArquiteturaSubstituicaoNacionalAggregate avaliar() {
        long totalProcessos = processoRepository.count();
        long totalPendentes = workItemRepository.countByStatus(WorkItemStatus.PENDENTE);
        long totalExpirados = workItemRepository.countByStatus(WorkItemStatus.EXPIRADO);
        boolean buildGateAprovado = resolveBuildGateAprovado();

        PjbArquiteturaSubstituicaoPilar motor = motorProcessualEvaluator.avaliar();
        PjbArquiteturaSubstituicaoPilar interoperabilidade = interoperabilidadeEvaluator.avaliar();
        PjbArquiteturaSubstituicaoPilar confiabilidade = confiabilidadeEvaluator.avaliar(totalPendentes, totalExpirados, buildGateAprovado);
        PjbArquiteturaSubstituicaoPilar governanca = governancaEvaluator.avaliar(buildGateAprovado);
        List<PjbArquiteturaSubstituicaoPilar> pilares = List.of(motor, interoperabilidade, confiabilidade, governanca);
        int scoreGeral = PjbArquiteturaSubstituicaoPilarSupport.score(pilares.stream().mapToInt(PjbArquiteturaSubstituicaoPilar::score).average().orElse(0));
        boolean pronto = buildGateAprovado
                && pilares.stream().allMatch(PjbArquiteturaSubstituicaoPilar::pronto)
                && totalExpirados <= Math.max(250, totalPendentes / 8);
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        fundamentos.add("totalProcessos=" + totalProcessos);
        fundamentos.add("workItemsPendentes=" + totalPendentes);
        fundamentos.add("workItemsExpirados=" + totalExpirados);
        fundamentos.add("tribunaisCatalogados=" + NationalCompetenceMatrix.values().length);
        fundamentos.add("ramosCatalogados=" + com.tcc.pjb.backend.model.entity.enums.RamoDireito.values().length);
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

    private boolean resolveBuildGateAprovado() {
        try {
            return buildGateGovernanceService.evaluate().approved();
        } catch (RuntimeException ex) {
            return false;
        }
    }
}
