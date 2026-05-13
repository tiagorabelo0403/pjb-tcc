package com.tcc.pjb.backend.service.prazo;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.repository.MovimentacaoProcessualRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class PrazoRiskIntelligenceService {

    private final CurrentUserService currentUserService;
    private final WorkItemRepository workItemRepository;
    private final DocumentoProcessualRepository documentoProcessualRepository;
    private final MovimentacaoProcessualRepository movimentacaoProcessualRepository;

    public PrazoRiskIntelligenceService(CurrentUserService currentUserService,
                                        WorkItemRepository workItemRepository,
                                        DocumentoProcessualRepository documentoProcessualRepository,
                                        MovimentacaoProcessualRepository movimentacaoProcessualRepository) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
        this.documentoProcessualRepository = Objects.requireNonNull(documentoProcessualRepository);
        this.movimentacaoProcessualRepository = Objects.requireNonNull(movimentacaoProcessualRepository);
    }

    public RiskProjection project(Processo processo,
                                  String tipoAto,
                                  long prazoNominalDias,
                                  long prazoRealEstimadoDias,
                                  double congestionScore,
                                  long backlogTerritorial,
                                  long referenciasHistoricas) {
        Usuario usuario = currentUserService.getOrNull();
        WorkloadProjection workload = resolveWorkload(usuario);
        ComplexityProjection complexity = resolveComplexity(processo);
        double desvioScore = clamp01(((double) prazoRealEstimadoDias - prazoNominalDias) / Math.max(1d, prazoNominalDias));
        double backlogScore = clamp01(backlogTerritorial / 5000.0d);
        double congestionSignal = clamp01((congestionScore - 1.0d) / 2.0d);
        double historicalStability = referenciasHistoricas <= 0L ? 0.35d : clamp01(referenciasHistoricas / 18.0d);
        double probability = clamp01(
                desvioScore * 0.30d
                        + workload.pressureScore() * 0.23d
                        + complexity.score() * 0.22d
                        + backlogScore * 0.10d
                        + congestionSignal * 0.10d
                        + (1.0d - historicalStability) * 0.05d
        );
        RiskBand band = resolveBand(probability);
        ArrayList<String> fundamentos = new ArrayList<>();
        fundamentos.add("A projeção cruza desvio entre prazo nominal e prazo real estimado com pressão operacional do usuário corrente.");
        fundamentos.add("A complexidade considera score do processo, densidade documental e cadência recente de movimentações.");
        if (workload.openItems() > 0L) {
            fundamentos.add("A fila do usuário corrente soma " + workload.openItems() + " itens abertos, com " + workload.overdueItems() + " vencidos e " + workload.dueSoonItems() + " vencendo em até 72 horas.");
        }
        if (tipoAto != null && !tipoAto.isBlank()) {
            fundamentos.add("O ato analisado foi consolidado como " + tipoAto.trim().toUpperCase(Locale.ROOT) + ".");
        }
        return new RiskProjection(
                "PRAZO_PREDICTIVE_V2",
                round2(probability),
                band.name(),
                band.uiBand,
                round2(workload.score()),
                round2(workload.pressureScore()),
                round2(complexity.score()),
                workload.openItems(),
                workload.dueSoonItems(),
                workload.overdueItems(),
                List.copyOf(fundamentos)
        );
    }

    private WorkloadProjection resolveWorkload(Usuario usuario) {
        if (usuario == null || usuario.getId() == null) {
            return new WorkloadProjection(0.18d, 0.18d, 0L, 0L, 0L);
        }
        long openItems = workItemRepository.inboxByUser(usuario.getId(), PageRequest.of(0, 1)).getTotalElements();
        Instant now = Instant.now();
        long dueSoonItems = workItemRepository.countDueByAssignedUser(usuario.getId(), now.plus(72, ChronoUnit.HOURS));
        long overdueItems = workItemRepository.countOverdueByAssignedUser(usuario.getId(), now);
        double score = clamp01(openItems / 40.0d);
        double pressure = clamp01(score * 0.45d + (dueSoonItems / 12.0d) * 0.25d + (overdueItems / 8.0d) * 0.30d);
        return new WorkloadProjection(score, pressure, openItems, dueSoonItems, overdueItems);
    }

    private ComplexityProjection resolveComplexity(Processo processo) {
        if (processo == null || processo.getId() == null) {
            return new ComplexityProjection(0.25d);
        }
        double structural = processo.getScoreComplexidade() == null
                ? 0.0d
                : clamp01(processo.getScoreComplexidade() / 100.0d);
        long documentos = documentoProcessualRepository.countByProcesso_Id(processo.getId());
        long movimentacoes = movimentacaoProcessualRepository.findTop80ByProcesso_IdOrderByDataMovimentacaoDesc(processo.getId()).size();
        double documental = clamp01(documentos / 30.0d);
        double cadence = clamp01(movimentacoes / 45.0d);
        double sigilo = processo.getNivelSigilo() != null && processo.getNivelSigilo().exigeCredencial() ? 0.15d : 0.0d;
        return new ComplexityProjection(clamp01(structural * 0.55d + documental * 0.25d + cadence * 0.20d + sigilo));
    }

    private RiskBand resolveBand(double probability) {
        if (probability >= 0.85d) {
            return RiskBand.CRITICO;
        }
        if (probability >= 0.65d) {
            return RiskBand.ALTO;
        }
        if (probability >= 0.40d) {
            return RiskBand.MEDIO;
        }
        return RiskBand.BAIXO;
    }

    private double clamp01(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0d;
        }
        return Math.max(0.0d, Math.min(1.0d, value));
    }

    private double round2(double value) {
        return Math.round(value * 100.0d) / 100.0d;
    }

    private enum RiskBand {
        BAIXO("VERDE"),
        MEDIO("AMARELO"),
        ALTO("VERMELHO"),
        CRITICO("CRITICO");

        private final String uiBand;

        RiskBand(String uiBand) {
            this.uiBand = uiBand;
        }
    }

    private record WorkloadProjection(double score,
                                      double pressureScore,
                                      long openItems,
                                      long dueSoonItems,
                                      long overdueItems) {
    }

    private record ComplexityProjection(double score) {
    }

    public record RiskProjection(String modelVersion,
                                 double riskProbability,
                                 String riskLevel,
                                 String uiBand,
                                 double workloadScore,
                                 double userPressureScore,
                                 double complexityScore,
                                 long openItems,
                                 long dueSoonItems,
                                 long overdueItems,
                                 List<String> fundamentos) {
    }
}
