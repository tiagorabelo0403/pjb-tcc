package com.tcc.pjb.backend.service.juiz;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.service.jurisprudencia.search.JurisprudenceSearchEngine;
import com.tcc.pjb.backend.service.jurisprudencia.search.JurisprudenceSearchHit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;

@Service
public class JudicialVoiceDraftIntelligenceService {

    private final JurisprudenceSearchEngine jurisprudenceSearchEngine;

    public JudicialVoiceDraftIntelligenceService(JurisprudenceSearchEngine jurisprudenceSearchEngine) {
        this.jurisprudenceSearchEngine = Objects.requireNonNull(jurisprudenceSearchEngine);
    }

    public DraftProjection project(Processo processo, String modoDocumento, String transcricaoBruta) {
        String normalized = normalizeTranscript(transcricaoBruta);
        List<String> fundamentos = buildFundamentos(processo, modoDocumento, normalized);
        List<Map<String, Object>> precedentes = jurisprudenceSearchEngine.search(
                        buildQuery(processo, normalized),
                        processo == null ? null : processo.getRamoDireito(),
                        processo == null ? null : processo.getRito(),
                        3
                )
                .stream()
                .map(this::toMap)
                .toList();
        return new DraftProjection(
                normalized,
                List.copyOf(fundamentos),
                precedentes,
                precedentes.isEmpty() ? "VOICE_DRAFT_BASE" : "VOICE_DRAFT_JURISPRUDENCIA_ATIVA"
        );
    }

    private String normalizeTranscript(String transcricaoBruta) {
        if (transcricaoBruta == null || transcricaoBruta.isBlank()) {
            return "";
        }
        String normalized = transcricaoBruta
                .replace('\n', ' ')
                .replaceAll("\\s+", " ")
                .replaceAll("(?i)\\bcumpra se\\b", "cumpra-se")
                .replaceAll("(?i)\\bintime se\\b", "intime-se")
                .trim();
        if (normalized.isEmpty()) {
            return "";
        }
        return Character.toUpperCase(normalized.charAt(0)) + normalized.substring(1);
    }

    private List<String> buildFundamentos(Processo processo, String modoDocumento, String normalized) {
        ArrayList<String> fundamentos = new ArrayList<>();
        fundamentos.add("Delimitar o relatório em fatos processualmente relevantes, evitando ruído narrativo na versão final do ato.");
        fundamentos.add("Fechar a fundamentação com conexão explícita entre quadro fático, impulso processual e consequência operacional do dispositivo.");
        if (normalized.contains("urg") || normalized.contains("tutela") || normalized.contains("liminar")) {
            fundamentos.add("Há sinal de urgência no ditado; convém enfrentar imediatamente probabilidade do direito, risco de demora e reversibilidade operacional.");
        }
        if (processo != null && processo.getNivelSigilo() != null && processo.getNivelSigilo().exigeCredencial()) {
            fundamentos.add("O processo exige cuidado redacional com sigilo reforçado, evitando reprodução aberta de dados sensíveis no relatório e no dispositivo.");
        }
        String ramo = processo == null || processo.getRamoDireito() == null ? "CIVEL" : processo.getRamoDireito().name();
        switch (ramo) {
            case "PENAL", "MILITAR", "ELEITORAL" -> fundamentos.add("Na linha penal, a minuta deve enfrentar contraditório, cautelaridade concreta, cadeia de custódia e suficiência de motivação do ato.");
            case "TRABALHISTA" -> fundamentos.add("Na linha trabalhista, a minuta deve fechar coerência entre narrativa fática, distribuição dinâmica do ônus e resultado prático da tutela pretendida.");
            case "ADMINISTRATIVO", "TRIBUTARIO", "PREVIDENCIARIO", "CONSTITUCIONAL" -> fundamentos.add("Na linha fazendária, convém tratar risco fiscal, exigibilidade, interesse público e regime de cumprimento do provimento.");
            default -> fundamentos.add("Na linha cível, a minuta deve enfrentar contraditório, cooperação, utilidade do ato e saneamento do próximo passo processual.");
        }
        if (modoDocumento != null && modoDocumento.equalsIgnoreCase("DESPACHO")) {
            fundamentos.add("Como despacho, a redação deve privilegiar impulso oficial claro, comandos executáveis e redução de ambiguidade operacional para a secretaria.");
        }
        if (modoDocumento != null && modoDocumento.equalsIgnoreCase("DECISAO")) {
            fundamentos.add("Como decisão interlocutória, a fundamentação deve separar premissas de fato, premissas normativas e comando final com carga executiva imediata.");
        }
        return fundamentos;
    }

    private String buildQuery(Processo processo, String normalized) {
        return Stream.of(
                        processo == null ? null : processo.getClasseProcessual(),
                        processo == null ? null : processo.getAssunto(),
                        processo == null ? null : processo.getPedidoPrincipal(),
                        head(normalized)
                )
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .limit(4)
                .reduce((left, right) -> left + ' ' + right)
                .orElse(normalized);
    }

    private String head(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        return normalized.length() <= 220 ? normalized : normalized.substring(0, 220);
    }

    private Map<String, Object> toMap(JurisprudenceSearchHit hit) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("id", hit.id());
        map.put("fonte", hit.fonte() == null ? null : hit.fonte().name());
        map.put("tipo", hit.tipo() == null ? null : hit.tipo().name());
        map.put("identificador", hit.identificador());
        map.put("titulo", hit.titulo());
        map.put("tese", hit.tese());
        map.put("ementaResumo", hit.ementaResumo());
        map.put("urlReferencia", hit.urlReferencia());
        map.put("dataPublicacao", hit.dataPublicacao() == null ? null : hit.dataPublicacao().toString());
        map.put("score", Math.round(hit.score() * 100.0d) / 100.0d);
        map.put("aderencia", resolveAderencia(hit));
        return Map.copyOf(map);
    }

    private String resolveAderencia(JurisprudenceSearchHit hit) {
        if (hit == null) {
            return "MATERIAL";
        }
        boolean rito = hit.ritoSugerido() != null;
        boolean ramo = hit.ramoSugerido() != null;
        if (ramo && rito) {
            return "DIRETA";
        }
        return "MATERIAL";
    }

    public record DraftProjection(String transcricaoNormalizada,
                                  List<String> fundamentosSugeridos,
                                  List<Map<String, Object>> precedentesSugeridos,
                                  String profile) {
    }
}
