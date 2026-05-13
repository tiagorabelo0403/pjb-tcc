package com.tcc.pjb.backend.service.intelligence;

import com.tcc.pjb.backend.ai.jurimetria.JurimetriaService;
import com.tcc.pjb.backend.ai.jurimetria.model.JurimetriaReport;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.dto.JudgeProfile;
import com.tcc.pjb.backend.model.dto.intelligence.ProcessOutcomePredictionResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.JudgeInsightEngine;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcessOutcomePredictionService {

    private final ProcessoRepository processoRepository;
    private final PjbAuthorizationService authorizationService;
    private final JurimetriaService jurimetriaService;
    private final JudgeInsightEngine judgeInsightEngine;

    public ProcessOutcomePredictionService(ProcessoRepository processoRepository,
                                           PjbAuthorizationService authorizationService,
                                           JurimetriaService jurimetriaService,
                                           JudgeInsightEngine judgeInsightEngine) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.authorizationService = Objects.requireNonNull(authorizationService);
        this.jurimetriaService = Objects.requireNonNull(jurimetriaService);
        this.judgeInsightEngine = Objects.requireNonNull(judgeInsightEngine);
    }

    @Transactional(readOnly = true)
    public ProcessOutcomePredictionResponse analyze(Long processoId) {
        Processo processo = processoRepository.findProcessoCompletoById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        authorizationService.requireReadProcesso(processo);
        return analyze(processo);
    }

    @Transactional(readOnly = true)
    public ProcessOutcomePredictionResponse analyze(Processo processo) {
        Objects.requireNonNull(processo, "processo");
        JurimetriaReport jurimetria = jurimetriaService.gerarRelatorio(
                processo.getPedidoPrincipal(),
                firstNonBlank(processo.getTribunal(), processo.getJurisdicao() != null ? processo.getJurisdicao().getSigla() : null),
                processo.getClasseProcessual(),
                processo.getAssunto(),
                Map.of("ramoDireito", processo.getRamoDireito() != null ? processo.getRamoDireito().name() : "CIVIL")
        );
        Map<String, Double> indicadores = indicadores(jurimetria);
        List<Processo> comparaveis = processoRepository.findComparableCases(
                processo.getId(),
                processo.getTribunal(),
                processo.getClasseProcessual(),
                processo.getAssunto(),
                PageRequest.of(0, 120)
        );
        double positivos = 0d;
        double parciais = 0d;
        double negativos = 0d;
        int amostra = 0;
        for (Processo caso : comparaveis) {
            if (caso == null || caso.getId() == null || Objects.equals(caso.getId(), processo.getId())) {
                continue;
            }
            String resultado = upper(caso.getResultadoFinal());
            if (resultado.isBlank()) {
                continue;
            }
            amostra++;
            if (containsAny(resultado, "PARCIAL")) {
                parciais += 1d;
            } else if (containsAny(resultado, "PROCEDENTE", "CONDENACAO", "ACORDO_HOMOLOGADO", "GANHO") && !containsAny(resultado, "IMPROCEDENTE")) {
                positivos += 1d;
            } else if (containsAny(resultado, "IMPROCEDENTE", "EXTINCAO", "NEGADO")) {
                negativos += 1d;
            }
        }
        double samplePositive = amostra == 0 ? 0.44d : clamp(positivos / (double) amostra);
        double samplePartial = amostra == 0 ? 0.24d : clamp(parciais / (double) amostra);
        double sampleNegative = amostra == 0 ? 0.32d : clamp(negativos / (double) amostra);
        double baseSuccess = indicadores.getOrDefault("taxa_sucesso_estimada", 0.50d);
        double baseSettlement = indicadores.getOrDefault("prob_acordo_estimada", 0.35d);
        double complexity = clamp(normalizeScore(processo.getScoreComplexidade(), 55));
        double material = clamp(normalizeScore(processo.getMaterialProbatorioScore(), 58));
        double acordoScore = clamp(normalizeScore(processo.getPotencialAcordoScore(), 45));
        JudgeProfile judgeProfile = judgeInsightEngine.analyze(processo);
        double procedencia = clamp((baseSuccess * 0.40d) + (samplePositive * 0.26d) + (material * 0.14d) + (judgeProfile.getTaxaHomologacao() * 0.10d) + ((1.0d - complexity) * 0.10d));
        double parcial = clamp((samplePartial * 0.42d) + (acordoScore * 0.18d) + (complexity * 0.18d) + ((1.0d - material) * 0.10d) + (baseSettlement * 0.12d));
        double improcedencia = clamp((sampleNegative * 0.45d) + ((1.0d - material) * 0.25d) + (complexity * 0.15d) + ((1.0d - baseSuccess) * 0.15d));
        double total = procedencia + parcial + improcedencia;
        if (total <= 0d) {
            procedencia = 0.45d;
            parcial = 0.25d;
            improcedencia = 0.30d;
            total = 1d;
        }
        procedencia = round(procedencia / total);
        parcial = round(parcial / total);
        improcedencia = round(Math.max(0d, 1.0d - procedencia - parcial));
        double acordo = round(clamp((baseSettlement * 0.45d) + (acordoScore * 0.25d) + (judgeProfile.getTaxaHomologacao() * 0.15d) + (samplePartial * 0.15d)));
        String predictedDisposition = procedencia >= parcial && procedencia >= improcedencia ? "PROCEDENCIA_PROVAVEL" : parcial >= improcedencia ? "PROCEDENCIA_PARCIAL_PROVAVEL" : "IMPROCEDENCIA_PROVAVEL";
        String recommendationBand = acordo >= 0.58d || parcial >= 0.34d ? "TENTAR_ACORDO_PRE_SENTENCA" : procedencia >= 0.60d ? "FORTE_PARA_SENTENCA" : "REVISAR_MATERIAL_E_PROVAS";
        double confidence = round(clamp((Math.min(1.0d, amostra / 40.0d) * 0.35d) + (material * 0.25d) + ((1.0d - Math.abs(procedencia - improcedencia)) * 0.10d) + (baseSuccess * 0.15d) + (judgeProfile.getTaxaHomologacao() * 0.15d)));
        ArrayList<String> fundamentos = new ArrayList<>();
        fundamentos.add("Sinal jurimétrico do ramo e do tribunal combinado com amostra comparável do acervo local.");
        fundamentos.add("Material probatório e complexidade atual modulam o eixo entre procedência, parcialidade e improcedência.");
        fundamentos.add("Perfil decisório contextual ajusta a agressividade do acordo e a expectativa de homologação.");
        if (amostra > 0) {
            fundamentos.add("Amostra comparável considerada: " + amostra + " casos do mesmo recorte processual.");
        }
        ArrayList<String> conciliacaoPrompts = new ArrayList<>();
        conciliacaoPrompts.add("Processos similares neste recorte apresentaram cerca de " + pct(parcial) + " de procedência parcial. Deseja tentar acordo antes da sentença?");
        if (acordo >= 0.55d) {
            conciliacaoPrompts.add("Há janela negocial com aderência estimada de " + pct(acordo) + ". Recomenda-se proposta calibrada com cláusulas executáveis.");
        }
        return new ProcessOutcomePredictionResponse(
                processo.getId(),
                predictedDisposition,
                recommendationBand,
                procedencia,
                parcial,
                improcedencia,
                acordo,
                confidence,
                judgeProfile.getTendenciaFormalidade(),
                round(judgeProfile.getTaxaHomologacao()),
                judgeProfile.getClausulasPreferidas(),
                List.copyOf(fundamentos),
                List.copyOf(conciliacaoPrompts)
        );
    }

    private Map<String, Double> indicadores(JurimetriaReport report) {
        LinkedHashMap<String, Double> out = new LinkedHashMap<>();
        if (report == null || report.getIndicadores() == null) {
            return out;
        }
        report.getIndicadores().forEach(item -> {
            if (item != null && item.getNome() != null && item.getValor() != null) {
                out.put(item.getNome(), item.getValor());
            }
        });
        return out;
    }

    private boolean containsAny(String value, String... tokens) {
        if (value == null || value.isBlank() || tokens == null) {
            return false;
        }
        for (String token : tokens) {
            if (token != null && !token.isBlank() && value.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private String upper(String value) {
        return value == null ? "" : value.toUpperCase(Locale.ROOT);
    }

    private double normalizeScore(Integer value, int fallback) {
        int effective = value == null ? fallback : Math.max(0, Math.min(100, value));
        return effective / 100.0d;
    }

    private double clamp(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0d;
        }
        return Math.max(0d, Math.min(1d, value));
    }

    private double round(double value) {
        return Math.round(value * 10000.0d) / 10000.0d;
    }

    private String pct(double value) {
        return Math.round(clamp(value) * 100d) + "%";
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
