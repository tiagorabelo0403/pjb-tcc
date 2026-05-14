package com.tcc.pjb.backend.service.secretariat.ingest;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ProcessoExternalNormalizationService {

    public record ProcessoExternoSnapshot(
            UUID processoId,
            String numeroNacionalUnificado,
            SistemaProcessualOrigem origem,
            String classeOriginal,
            String classeNormalizada,
            RitoProcessual ritoDetectado,
            Map<String, String> camposNormalizados,
            boolean normalizacaoConcluida,
            String aviso
    ) {}

    public record NormalizacaoInput(
            String numeroOrigem,
            SistemaProcessualOrigem origem,
            String classeOriginal,
            Map<String, String> camposRaw
    ) {}

    public ProcessoExternoSnapshot normalizar(NormalizacaoInput input) {
        String npu = normalizarNpu(input.numeroOrigem(), input.origem());
        String classeNorm = normalizarClasse(input.classeOriginal());
        RitoProcessual rito = inferirRito(classeNorm);

        Map<String, String> normalizados = new java.util.LinkedHashMap<>();
        if (input.camposRaw() != null) {
            input.camposRaw().forEach((k, v) -> normalizados.put(k, v != null ? v.trim() : ""));
        }
        normalizados.put("classeNormalizada", classeNorm);
        normalizados.put("numeroNacionalUnificado", npu);

        String aviso = input.origem().requerNormalizacao()
                ? "Processo normalizado de " + input.origem().descricao() + " — conferir antes de distribuir."
                : null;

        return new ProcessoExternoSnapshot(
                UUID.randomUUID(), npu, input.origem(),
                input.classeOriginal(), classeNorm, rito, normalizados, true, aviso);
    }

    private String normalizarNpu(String numero, SistemaProcessualOrigem origem) {
        if (numero == null) return null;
        return numero.replaceAll("[^0-9]", "")
                .replaceFirst("(\\d{7})(\\d{2})(\\d{4})(\\d{1})(\\d{2})(\\d{4})", "$1-$2.$3.$4.$5.$6");
    }

    private String normalizarClasse(String classe) {
        if (classe == null) return "NAO_INFORMADO";
        return classe.trim().toUpperCase().replaceAll("[^A-Z0-9_]", "_");
    }

    private RitoProcessual inferirRito(String classeNorm) {
        if (classeNorm.contains("PENAL") || classeNorm.contains("CRIMINAL")) return RitoProcessual.PROCEDIMENTO_PENAL_COMUM;
        if (classeNorm.contains("TRABALHIST")) return RitoProcessual.TRABALHISTA_ORDINARIO;
        if (classeNorm.contains("JUIZADO")) return RitoProcessual.JUIZADO_ESPECIAL_CIVEL;
        return RitoProcessual.PROCEDIMENTO_COMUM;
    }
}
