package com.tcc.pjb.backend.service.prazo;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.dto.publico.PrazoRealPredictionResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.workflow.MovimentacaoProcessual;
import com.tcc.pjb.backend.model.repository.MovimentacaoProcessualRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;

@Service
public class CongestionScoreService {

    private final ProcessoRepository processoRepository;
    private final MovimentacaoProcessualRepository movimentacaoRepository;
    private final WorkItemRepository workItemRepository;
    private final PjbAuthorizationService authorizationService;
    private final PrazoRiskIntelligenceService prazoRiskIntelligenceService;

    public CongestionScoreService(ProcessoRepository processoRepository,
                                  MovimentacaoProcessualRepository movimentacaoRepository,
                                  WorkItemRepository workItemRepository,
                                  PjbAuthorizationService authorizationService,
                                  PrazoRiskIntelligenceService prazoRiskIntelligenceService) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.movimentacaoRepository = Objects.requireNonNull(movimentacaoRepository);
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
        this.authorizationService = Objects.requireNonNull(authorizationService);
        this.prazoRiskIntelligenceService = Objects.requireNonNull(prazoRiskIntelligenceService);
    }

    @Transactional(readOnly = true)
    public PrazoRealPredictionResponse predizer(Long processoId, String tipoAto) {
        Processo processo = processoRepository.findProcessoCompletoById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        authorizationService.requireReadProcesso(processo);
        return predizerInterno(processo, tipoAto);
    }

    @Transactional(readOnly = true)
    public PrazoRealPredictionResponse predizerPublicoPorNumero(String numeroProcesso, String tipoAto) {
        Processo processo = processoRepository.findByNumeroUnificado(numeroProcesso)
                .or(() -> processoRepository.findByNumeroProcesso(numeroProcesso))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", numeroProcesso));
        if (processo.getNivelSigilo() != null && processo.getNivelSigilo().exigeCredencial()) {
            throw new IllegalStateException("Processo sigiloso indisponível para predição pública.");
        }
        return predizerInterno(processo, tipoAto);
    }

    private PrazoRealPredictionResponse predizerInterno(Processo processo, String tipoAto) {
        List<MovimentacaoProcessual> historico = movimentacaoRepository.findTop80ByProcesso_IdOrderByDataMovimentacaoDesc(processo.getId())
                .stream()
                .sorted(Comparator.comparing(MovimentacaoProcessual::getDataMovimentacao))
                .toList();
        List<Long> gaps = new ArrayList<>();
        for (int i = 1; i < historico.size(); i++) {
            var atual = historico.get(i).getDataMovimentacao();
            var anterior = historico.get(i - 1).getDataMovimentacao();
            if (atual != null && anterior != null) {
                long dias = Math.max(1L, ChronoUnit.DAYS.between(anterior, atual));
                gaps.add(dias);
            }
        }
        long mediaHistorica = gaps.isEmpty()
                ? 15L
                : Math.max(1L, Math.round(gaps.stream().mapToLong(Long::longValue).average().orElse(15d)));
        long backlogTerritorial = processoRepository.countByUfAndComarca(processo.getUf(), processo.getComarca());
        long workItemsAbertos = workItemRepository.countOpenByProcesso(processo.getId());
        double congestion = Math.min(2.75d, 1.0d + (backlogTerritorial / 2500.0d) + (workItemsAbertos / 10.0d));
        long prazoNominalDias = inferPrazoNominal(tipoAto, processo);
        long prazoReal = Math.max(1L, Math.round((mediaHistorica + prazoNominalDias) * congestion / 2.0d));
        double desvio = ((double) prazoReal - prazoNominalDias) / Math.max(1d, prazoNominalDias);
        PrazoRiskIntelligenceService.RiskProjection risk = prazoRiskIntelligenceService.project(
                processo,
                tipoAto,
                prazoNominalDias,
                prazoReal,
                congestion,
                backlogTerritorial,
                gaps.size()
        );
        return new PrazoRealPredictionResponse(
                processo.getId(),
                processo.getNumeroProcesso(),
                tipoAto == null || tipoAto.isBlank() ? "ATO_PROCESSUAL" : tipoAto.trim(),
                prazoNominalDias,
                prazoReal,
                desvio,
                LocalDate.now(ZoneId.systemDefault()).plusDays(prazoReal),
                congestion,
                backlogTerritorial,
                gaps.size(),
                risk.modelVersion(),
                risk.riskProbability(),
                risk.riskLevel(),
                risk.uiBand(),
                risk.workloadScore(),
                risk.userPressureScore(),
                risk.complexityScore(),
                risk.openItems(),
                risk.dueSoonItems(),
                risk.overdueItems(),
                risk.fundamentos()
        );
    }

    private long inferPrazoNominal(String tipoAto, Processo processo) {
        String token = tipoAto == null ? "" : tipoAto.trim().toUpperCase();
        if (token.contains("AUDI")) return 30L;
        if (token.contains("SENTEN")) return 30L;
        if (token.contains("PERIC")) return 20L;
        if (token.contains("INTIMA")) return 5L;
        if (processo.getRamoDireito() != null && processo.getRamoDireito().name().contains("PENAL")) return 10L;
        if (processo.getRamoDireito() != null && processo.getRamoDireito().name().contains("TRABALHO")) return 8L;
        return 15L;
    }
}
