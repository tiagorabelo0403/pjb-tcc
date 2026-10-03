package com.tcc.pjb.backend.core.plataforma.sustentacao.application;

import com.tcc.pjb.backend.core.plataforma.sustentacao.domain.PjbPlataformaSustentacaoCenario;
import com.tcc.pjb.backend.core.plataforma.sustentacao.domain.PjbPlataformaSustentacaoEixo;
import com.tcc.pjb.backend.core.plataforma.sustentacao.domain.PjbPlataformaSustentacaoModulo;
import com.tcc.pjb.backend.model.dto.processual.rollout.NationalFeatureRolloutResponse;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

final class PjbPlataformaSustentacaoDiagnosticoSupport {

    private PjbPlataformaSustentacaoDiagnosticoSupport() {
    }

    static PjbPlataformaSustentacaoEixo eixo(String codigo,
                                             String titulo,
                                             int score,
                                             boolean pronto,
                                             LinkedHashSet<String> sinais,
                                             LinkedHashSet<String> bloqueadores,
                                             LinkedHashSet<String> proximasAcoes,
                                             Map<String, Object> evidencias) {
        return new PjbPlataformaSustentacaoEixo(
                codigo,
                titulo,
                Math.max(0, Math.min(100, score)),
                pronto ? "PRONTO" : score >= 70 ? "PARCIAL" : "BLOQUEADO",
                pronto,
                List.copyOf(sinais),
                List.copyOf(bloqueadores),
                List.copyOf(proximasAcoes),
                cleanMap(evidencias)
        );
    }

    static PjbPlataformaSustentacaoModulo modulo(String codigo,
                                                 String titulo,
                                                 String camada,
                                                 int beansConectados,
                                                 int score,
                                                 List<String> conexoes,
                                                 List<String> riscos) {
        return new PjbPlataformaSustentacaoModulo(
                codigo,
                titulo,
                camada,
                Math.max(0, beansConectados),
                Math.max(0, Math.min(100, score)),
                score >= 80 ? "CONECTADO" : score >= 65 ? "PARCIAL" : "FRAGIL",
                conexoes,
                riscos.stream().distinct().toList()
        );
    }

    static Map<String, Object> samplePayload(String tribunalCodigo,
                                             String ramo,
                                             String resumo,
                                             String rito,
                                             String classe) {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("tribunalCodigo", tribunalCodigo);
        payload.put("ramoDireito", ramo);
        payload.put("resumo", resumo);
        payload.put("assunto", resumo);
        payload.put("rito", rito);
        payload.put("classe", classe);
        payload.put("classeProcessual", classe);
        payload.put("tipoAcao", classe);
        payload.put("ufAutor", tribunalCodigo.contains("TJ") || tribunalCodigo.contains("TRT") || tribunalCodigo.contains("TRE") ? tribunalCodigo.substring(tribunalCodigo.length() - 2).replace("-", "") : "BR");
        payload.put("valorCausa", java.math.BigDecimal.valueOf(25000));
        payload.put("pedidoPrincipal", resumo);
        return payload;
    }

    static int average(int... values) {
        if (values == null || values.length == 0) {
            return 0;
        }
        int total = 0;
        for (int value : values) {
            total += value;
        }
        return Math.max(0, Math.min(100, total / values.length));
    }

    static int scorePresence(Object... items) {
        int count = 0;
        if (items != null) {
            for (Object item : items) {
                if (item != null) {
                    count++;
                }
            }
        }
        return count;
    }

    static Map<String, Object> cleanMap(Map<String, Object> source) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        if (source != null) {
            for (Map.Entry<String, Object> entry : source.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    out.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return Collections.unmodifiableMap(out);
    }

    static Map<String, Object> toMap(NationalFeatureRolloutResponse response) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("featureCode", response.featureCode());
        out.put("enabled", response.enabled());
        out.put("rolloutMode", response.rolloutMode());
        out.put("thresholdPercent", response.thresholdPercent());
        out.put("tribunalCodigo", response.tribunalCodigo());
        out.put("perfilAlvo", response.perfilAlvo());
        out.put("warnings", response.warnings());
        return cleanMap(out);
    }

    static Map<String, Object> toMap(PjbPlataformaSustentacaoCenario cenario) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("codigo", cenario.codigo());
        out.put("tribunalCodigo", cenario.tribunalCodigo());
        out.put("ramo", cenario.ramo());
        out.put("rito", cenario.rito());
        out.put("score", cenario.score());
        out.put("apto", cenario.apto());
        out.put("alertas", cenario.alertas());
        return cleanMap(out);
    }
}
