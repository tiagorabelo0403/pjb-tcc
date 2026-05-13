package com.tcc.pjb.backend.ai.jurimetria;

import com.tcc.pjb.backend.ai.jurimetria.model.JurimetriaReport;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class JurimetriaService {

    private final ProcessoRepository processoRepository;

    public JurimetriaService(ProcessoRepository processoRepository) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
    }

    public JurimetriaReport gerarRelatorio(String tese,
                                           String tribunal,
                                           String classe,
                                           String assunto,
                                           Map<String, Object> filtros) {
        String ramo = resolveRamo(filtros, classe, assunto, tese);
        List<Object[]> agregadosRamo = processoRepository.agregadosPorRamo(ramo);
        List<Object[]> agregadosTribunal = isBlank(tribunal) ? List.of() : processoRepository.agregadosPorRamoETribunal(ramo, tribunal.trim().toUpperCase(Locale.ROOT));
        LinkedHashMap<String, Double> indicadores = new LinkedHashMap<>();
        ArrayList<String> observacoes = new ArrayList<>();
        if (!agregadosRamo.isEmpty()) {
            Object[] row = agregadosRamo.getFirst();
            double total = number(row, 1);
            double julgados = number(row, 2);
            double recursais = number(row, 3);
            double ativos = number(row, 4);
            double tempoMedioDias = number(row, 5);
            indicadores.put("acervo_total_ramo", total);
            indicadores.put("taxa_julgamento_ramo", ratio(julgados, total));
            indicadores.put("taxa_recursal_ramo", ratio(recursais, total));
            indicadores.put("taxa_acervo_ativo_ramo", ratio(ativos, total));
            indicadores.put("tempo_medio_dias_ramo", tempoMedioDias);
            observacoes.add("Base amostral do ramo inferido: " + ramo + ".");
        } else {
            observacoes.add("Sem amostra histórica suficiente para o ramo inferido.");
        }
        if (!agregadosTribunal.isEmpty()) {
            Object[] row = agregadosTribunal.getFirst();
            double totalTribunal = number(row, 0);
            double ativosTribunal = number(row, 1);
            double comAcordo = number(row, 2);
            double comTutela = number(row, 3);
            indicadores.put("acervo_total_tribunal", totalTribunal);
            indicadores.put("taxa_acervo_ativo_tribunal", ratio(ativosTribunal, totalTribunal));
            indicadores.put("taxa_acordo_tribunal", ratio(comAcordo, totalTribunal));
            indicadores.put("taxa_tutela_urgencia_tribunal", ratio(comTutela, totalTribunal));
            observacoes.add("Indicadores do tribunal calculados a partir da base local indexada para " + tribunal.trim().toUpperCase(Locale.ROOT) + '.');
        } else if (!isBlank(tribunal)) {
            observacoes.add("Sem massa crítica suficiente para o tribunal informado na base local indexada.");
        }
        double taxaJulgamento = indicadores.getOrDefault("taxa_julgamento_ramo", 0.0);
        double taxaAcordo = indicadores.getOrDefault("taxa_acordo_tribunal", 0.0);
        double taxaTutela = indicadores.getOrDefault("taxa_tutela_urgencia_tribunal", 0.0);
        double tempoMedio = indicadores.getOrDefault("tempo_medio_dias_ramo", 0.0);
        double taxaRecursal = indicadores.getOrDefault("taxa_recursal_ramo", 0.0);
        double probSucesso = clamp((taxaJulgamento * 0.55) + ((1.0 - taxaRecursal) * 0.25) + (taxaAcordo * 0.20));
        double propensaoAcordo = clamp((taxaAcordo * 0.65) + ((1.0 - taxaRecursal) * 0.15) + (taxaTutela * 0.20));
        double probTutela = clamp((taxaTutela * 0.70) + (taxaJulgamento * 0.15) + ((tempoMedio > 0 ? Math.min(1.0, 180.0 / tempoMedio) : 0.0) * 0.15));
        indicadores.putIfAbsent("taxa_sucesso_estimada", probSucesso);
        indicadores.putIfAbsent("prob_acordo_estimada", propensaoAcordo);
        indicadores.putIfAbsent("prob_tutela_urgencia", probTutela);
        if (!indicadores.containsKey("tempo_medio_dias_ramo")) {
            indicadores.put("tempo_medio_dias_ramo", tempoMedio);
        }
        observacoes.add("Indicadores derivados por heurística supervisionada da base local do PJB, sujeitos a recalibração por tribunal, classe e período.");
        observacoes.add("Amostra local deve ser combinada com datasets institucionais externos para governança nacional completa.");
        List<JurimetriaReport.Indicador> itens = indicadores.entrySet().stream()
                .map(entry -> JurimetriaReport.Indicador.builder()
                        .nome(entry.getKey())
                        .valor(round(entry.getValue()))
                        .unidade(unit(entry.getKey()))
                        .build())
                .toList();
        return JurimetriaReport.builder()
                .tese(tese)
                .tribunal(tribunal)
                .classe(classe)
                .assunto(assunto)
                .geradoEm(Instant.now())
                .indicadores(itens)
                .observacoes(observacoes)
                .explicacao(buildExplanation(ramo, tribunal, filtros, indicadores))
                .build();
    }

    private String buildExplanation(String ramo,
                                    String tribunal,
                                    Map<String, Object> filtros,
                                    Map<String, Double> indicadores) {
        StringBuilder sb = new StringBuilder();
        sb.append("Relatório jurimétrico calculado sobre a base processual consolidada do PJB");
        if (!isBlank(ramo)) {
            sb.append(" com recorte principal no ramo ").append(ramo);
        }
        if (!isBlank(tribunal)) {
            sb.append(" e janela complementar no tribunal ").append(tribunal.trim().toUpperCase(Locale.ROOT));
        }
        sb.append(". Foram considerados acervo, taxa de julgamento, pressão recursal, sinal de acordo e incidência de tutela de urgência.");
        if (filtros != null && !filtros.isEmpty()) {
            sb.append(" Filtros adicionais: ").append(filtros.keySet().stream().sorted().toList()).append('.');
        }
        if (indicadores.isEmpty()) {
            sb.append(" A amostra atual ainda é insuficiente para inferências fortes.");
        }
        return sb.toString();
    }

    private String resolveRamo(Map<String, Object> filtros,
                               String classe,
                               String assunto,
                               String tese) {
        String explicit = text(filtros != null ? filtros.get("ramo") : null);
        if (explicit == null) {
            explicit = text(filtros != null ? filtros.get("ramoDireito") : null);
        }
        if (explicit != null) {
            return explicit.trim().toUpperCase(Locale.ROOT);
        }
        String combined = String.join(" ", List.of(nullSafe(classe), nullSafe(assunto), nullSafe(tese))).toUpperCase(Locale.ROOT);
        if (combined.contains("TRABALH")) {
            return "TRABALHISTA";
        }
        if (combined.contains("PREVIDENCI")) {
            return "PREVIDENCIARIO";
        }
        if (combined.contains("TRIBUT")) {
            return "TRIBUTARIO";
        }
        if (combined.contains("FAZENDA")) {
            return "FAZENDA_PUBLICA";
        }
        if (combined.contains("PENAL") || combined.contains("CRIM")) {
            return "PENAL";
        }
        if (combined.contains("ELEITORAL")) {
            return "ELEITORAL";
        }
        if (combined.contains("MILITAR")) {
            return "MILITAR";
        }
        return "CIVIL";
    }

    private double number(Object[] row, int index) {
        if (row == null || index < 0 || index >= row.length || row[index] == null) {
            return 0.0;
        }
        Object value = row[index];
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return 0.0;
        }
    }

    private double ratio(double value, double total) {
        if (total <= 0.0) {
            return 0.0;
        }
        return clamp(value / total);
    }

    private double clamp(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, value));
    }

    private double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }

    private String unit(String key) {
        if (key == null) {
            return "ratio";
        }
        return key.contains("dias") ? "days" : key.startsWith("acervo_") ? "count" : "ratio";
    }

    private String text(Object value) {
        if (value == null) {
            return null;
        }
        String out = String.valueOf(value).trim();
        return out.isBlank() ? null : out;
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
