package com.tcc.pjb.backend.ai.skills.v1;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.ai.contract.IARequest;
import com.tcc.pjb.backend.ai.contract.IAResponse;
import com.tcc.pjb.backend.ai.skills.IASkill;

@Component
public class SkillAnaliseRiscoJuridicoV1 implements IASkill {

    private static final Pattern CONTEXTO_NEGACAO = Pattern.compile("(?iu)\\b(n[aã]o|sem|aus[eê]ncia de|inexist[eê]ncia de)\\b");

    @Override
    public boolean suporta(IARequest request) {
        return suportaAcao(request, "ANALISE_RISCO_JURIDICO") || suportaAcao(request, getNome());
    }

    @Override
    public IAResponse executar(IARequest request, Map<String, Object> contexto) {
        String textoOriginal = request != null ? request.getSafeString("texto") : null;
        if (textoOriginal == null || textoOriginal.isBlank()) {
            return IAResponse.builder()
                    .origem(getNome())
                    .status(IAResponse.StatusIA.INDETERMINADO)
                    .texto("Texto ausente para análise de risco.")
                    .confianca(0.0)
                    .dataGeracao(Instant.now())
                    .build();
        }

        RiscoDetectado riscoDetectado = analisar(textoOriginal);
        return IAResponse.builder()
                .origem(getNome())
                .status(riscoDetectado.status())
                .texto(riscoDetectado.resumo())
                .confianca(riscoDetectado.confianca())
                .dataGeracao(Instant.now())
                .metadados(riscoDetectado.metadados())
                .build();
    }

    @Override
    public String getNome() {
        return "SKILL_ANALISE_RISCO_JURIDICO_V1";
    }

    private RiscoDetectado analisar(String textoOriginal) {
        String texto = normalizar(textoOriginal);
        LinkedHashSet<IndicadorRisco> indicadores = new LinkedHashSet<>();
        LinkedHashSet<IndicadorRisco> indicadoresNegados = new LinkedHashSet<>();
        double score = 0.0;

        for (IndicadorRisco indicador : IndicadorRisco.values()) {
            if (!indicador.detecta(texto)) {
                continue;
            }
            if (indicador.negado(texto)) {
                indicadoresNegados.add(indicador);
                continue;
            }
            indicadores.add(indicador);
            score += indicador.peso();
        }

        LinkedHashSet<String> categorias = new LinkedHashSet<>();
        indicadores.forEach(indicador -> categorias.add(indicador.categoria()));
        double confianca = calcularConfianca(score, indicadores.size(), indicadoresNegados.size());
        IAResponse.StatusIA status = score >= 1.15 ? IAResponse.StatusIA.ALERTA : IAResponse.StatusIA.SUCESSO;

        Map<String, Object> metadados = new LinkedHashMap<>();
        metadados.put("risco_score", arredondar(score));
        metadados.put("categorias_risco", List.copyOf(categorias));
        metadados.put("indicadores_ativos", indicadores.stream().map(Enum::name).toList());
        metadados.put("indicadores_negados", indicadoresNegados.stream().map(Enum::name).toList());
        metadados.put("quantidade_indicadores", indicadores.size());
        metadados.put("quantidade_indicadores_negados", indicadoresNegados.size());

        return new RiscoDetectado(
                status,
                construirResumo(score, indicadores, indicadoresNegados),
                confianca,
                Map.copyOf(metadados)
        );
    }

    private String construirResumo(double score,
                                   Set<IndicadorRisco> indicadores,
                                   Set<IndicadorRisco> indicadoresNegados) {
        if (indicadores.isEmpty()) {
            if (indicadoresNegados.isEmpty()) {
                return "Nenhum risco jurídico relevante foi identificado pelos sinais materiais monitorados.";
            }
            return "Foram encontrados sinais jurídicos com contexto de negação, sem formação de alerta material de risco.";
        }
        List<String> grupos = indicadores.stream()
                .map(IndicadorRisco::descricao)
                .map(s -> s.toLowerCase(Locale.ROOT))
                .toList();
        return "Risco jurídico material identificado com score " + arredondar(score)
                + " a partir de sinais de " + String.join(", ", grupos) + ".";
    }

    private double calcularConfianca(double score, int quantidadeIndicadores, int quantidadeNegados) {
        double confianca = 0.42 + Math.min(0.46, score * 0.18) + Math.min(0.1, quantidadeIndicadores * 0.03);
        if (quantidadeIndicadores == 0 && quantidadeNegados > 0) {
            confianca = Math.max(0.51, confianca - 0.08);
        }
        return Math.min(0.98, confianca);
    }

    private double arredondar(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    private String normalizar(String texto) {
        return Objects.toString(texto, "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
    }

    private enum IndicadorRisco {
        PRESCRICAO("prescrição", "tempo", 0.62, List.of("prescrição", "prescricional", "prescrito")),
        DECADENCIA("decadência", "tempo", 0.54, List.of("decadência", "decadencial")),
        INTEMPESTIVIDADE("intempestividade", "prazo", 0.66, List.of("intempestivo", "intempestiva", "fora do prazo")),
        PRECLUSAO("preclusão", "prazo", 0.48, List.of("preclusão", "precluso", "preclusa")),
        NULIDADE("nulidade", "validade", 0.58, List.of("nulidade", "nulo", "anulável", "cerceamento")),
        ILEGITIMIDADE("ilegitimidade", "pressupostos", 0.44, List.of("ilegitimidade", "parte ilegítima", "ilegitimidade passiva", "ilegitimidade ativa")),
        INCOMPETENCIA("incompetência", "competência", 0.41, List.of("incompetência", "foro incompetente", "juízo incompetente"));

        private final String descricao;
        private final String categoria;
        private final double peso;
        private final List<String> gatilhos;

        IndicadorRisco(String descricao, String categoria, double peso, List<String> gatilhos) {
            this.descricao = descricao;
            this.categoria = categoria;
            this.peso = peso;
            this.gatilhos = List.copyOf(gatilhos);
        }

        String descricao() {
            return descricao;
        }

        String categoria() {
            return categoria;
        }

        double peso() {
            return peso;
        }

        boolean detecta(String texto) {
            return gatilhos.stream().anyMatch(texto::contains);
        }

        boolean negado(String texto) {
            for (String gatilho : gatilhos) {
                int idx = texto.indexOf(gatilho);
                if (idx < 0) {
                    continue;
                }
                int inicioJanela = Math.max(0, idx - 40);
                String prefixo = texto.substring(inicioJanela, idx).trim();
                if (CONTEXTO_NEGACAO.matcher(prefixo).find()) {
                    return true;
                }
            }
            return false;
        }
    }

    private record RiscoDetectado(IAResponse.StatusIA status,
                                  String resumo,
                                  double confianca,
                                  Map<String, Object> metadados) {
    }
}
