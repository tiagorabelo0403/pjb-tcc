package com.tcc.pjb.backend.service.intelligence;

import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.dto.intelligence.ExecutionRecoveryRiskResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExecutionRecoveryRiskService {

    private final ProcessoRepository processoRepository;
    private final PjbAuthorizationService authorizationService;

    public ExecutionRecoveryRiskService(ProcessoRepository processoRepository,
                                        PjbAuthorizationService authorizationService) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.authorizationService = Objects.requireNonNull(authorizationService);
    }

    @Transactional(readOnly = true)
    public ExecutionRecoveryRiskResponse analyze(Long processoId) {
        Processo processo = processoRepository.findProcessoCompletoById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        authorizationService.requireReadProcesso(processo);
        return analyze(processo);
    }

    @Transactional(readOnly = true)
    public ExecutionRecoveryRiskResponse analyze(Processo processo) {
        Objects.requireNonNull(processo, "processo");
        List<Processo> relacionados = processo.getParteReuCpf() == null || processo.getParteReuCpf().isBlank()
                ? List.of()
                : processoRepository.findAllByPartesCpf(processo.getParteReuCpf().replaceAll("\\D+", ""));
        int active = 0;
        int archived = 0;
        for (Processo caso : relacionados) {
            if (caso == null || caso.getId() == null || Objects.equals(caso.getId(), processo.getId())) {
                continue;
            }
            if (caso.getStatusProcesso() == StatusProcesso.ARQUIVADO || caso.getStatusProcesso() == StatusProcesso.TRANSITO_EM_JULGADO) {
                archived++;
            } else {
                active++;
            }
        }
        List<String> assetSignals = extractAssetSignals(processo);
        double assetWeight = Math.min(1.0d, assetSignals.size() / 4.0d);
        double debtPressure = relacionados.isEmpty() ? 0.35d : Math.min(1.0d, active / (double) Math.max(1, relatedCount(relacionados, processo)));
        double valuePressure = processo.getValorCausa() == null ? 0.45d : Math.min(1.0d, processo.getValorCausa().doubleValue() / 250000.0d);
        double recoveryProbability = clamp((assetWeight * 0.48d) + ((1.0d - debtPressure) * 0.22d) + ((1.0d - valuePressure) * 0.12d) + (archived > 0 ? 0.08d : 0.0d) + (processo.getParteReuCpf() != null && !processo.getParteReuCpf().isBlank() ? 0.10d : 0.0d));
        double confidence = clamp((processo.getParteReuCpf() != null && !processo.getParteReuCpf().isBlank() ? 0.35d : 0.12d) + Math.min(1.0d, relatedCount(relacionados, processo) / 8.0d) * 0.25d + assetWeight * 0.25d + (processo.getValorCausa() != null ? 0.15d : 0.05d));
        String band = recoveryProbability >= 0.70d ? "ALTA_RECUPERABILIDADE" : recoveryProbability >= 0.45d ? "RECUPERABILIDADE_MODERADA" : "RECUPERABILIDADE_BAIXA";
        ArrayList<String> fundamentos = new ArrayList<>();
        fundamentos.add("Probabilidade estimada a partir de sinais patrimoniais textuais, histórico relacional do polo passivo e pressão de passivo correlato.");
        if (processo.getParteReuCpf() == null || processo.getParteReuCpf().isBlank()) {
            fundamentos.add("Ausência de identificador forte do executado reduz a confiança da inferência.");
        }
        if (!assetSignals.isEmpty()) {
            fundamentos.add("Sinais patrimoniais localizados no processo: " + String.join(", ", assetSignals));
        }
        if (active > 0) {
            fundamentos.add("Há " + active + " processo(s) ativo(s) relacionados ao mesmo polo passivo, sugerindo pressão concorrencial sobre recuperação.");
        }
        ArrayList<String> estrategias = new ArrayList<>();
        estrategias.add("Priorizar pesquisa patrimonial e constrição cedo quando houver baixa recuperabilidade.");
        estrategias.add("Acoplar a execução a cláusulas objetivas de vencimento e gatilhos de multa quando houver acordo ou título negociado.");
        if (assetSignals.stream().anyMatch(signal -> signal.contains("IMOVEL") || signal.contains("VEICUL"))) {
            estrategias.add("Avaliar desde logo estratégia de expropriação focada nos bens já sinalizados no material do processo.");
        }
        return new ExecutionRecoveryRiskResponse(
                processo.getId(),
                round(recoveryProbability),
                band,
                round(confidence),
                relatedCount(relacionados, processo),
                assetSignals,
                List.copyOf(fundamentos),
                List.copyOf(estrategias)
        );
    }

    private int relatedCount(List<Processo> relacionados, Processo processo) {
        int count = 0;
        for (Processo caso : relacionados) {
            if (caso != null && caso.getId() != null && !Objects.equals(caso.getId(), processo.getId())) {
                count++;
            }
        }
        return count;
    }

    private List<String> extractAssetSignals(Processo processo) {
        String corpus = upper(join(processo.getResumoIA(), processo.getMaterialProbatorioResumo(), processo.getPedidoPrincipal(), processo.getPedidosConsolidados()));
        ArrayList<String> out = new ArrayList<>();
        if (containsAny(corpus, "IMOVEL", "APARTAMENTO", "TERRENO")) {
            out.add("IMOVEL_REFERENCIADO");
        }
        if (containsAny(corpus, "VEICUL", "AUTOMOVEL", "MOTO", "CAMINHAO")) {
            out.add("VEICULO_REFERENCIADO");
        }
        if (containsAny(corpus, "CONTA", "PIX", "BANCO", "FATURAMENTO", "RECEBIVEIS")) {
            out.add("FLUXO_FINANCEIRO_REFERENCIADO");
        }
        if (containsAny(corpus, "EMPRESA", "SOCIEDADE", "CNPJ", "ESTABELECIMENTO")) {
            out.add("ATIVIDADE_EMPRESARIAL_REFERENCIADA");
        }
        return out.stream().distinct().toList();
    }

    private String join(String... values) {
        StringBuilder sb = new StringBuilder();
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                if (sb.length() > 0) {
                    sb.append(' ');
                }
                sb.append(value.trim());
            }
        }
        return sb.toString();
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

    private double clamp(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0d;
        }
        return Math.max(0d, Math.min(1d, value));
    }

    private double round(double value) {
        return Math.round(value * 10000.0d) / 10000.0d;
    }
}
