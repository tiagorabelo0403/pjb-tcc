package com.tcc.pjb.backend.core.procedural;

import static com.tcc.pjb.backend.core.procedural.NationalProceduralRoutingSupport.bool;
import static com.tcc.pjb.backend.core.procedural.NationalProceduralRoutingSupport.buildCorpus;
import static com.tcc.pjb.backend.core.procedural.NationalProceduralRoutingSupport.containsAny;
import static com.tcc.pjb.backend.core.procedural.NationalProceduralRoutingSupport.decimal;
import static com.tcc.pjb.backend.core.procedural.NationalProceduralRoutingSupport.firstNonBlank;
import static com.tcc.pjb.backend.core.procedural.NationalProceduralRoutingSupport.isBlank;
import static com.tcc.pjb.backend.core.procedural.NationalProceduralRoutingSupport.normalize;
import static com.tcc.pjb.backend.core.procedural.NationalProceduralRoutingSupport.raw;
import static com.tcc.pjb.backend.core.procedural.NationalProceduralRoutingSupport.splitTextualItems;
import static com.tcc.pjb.backend.core.procedural.NationalProceduralRoutingSupport.text;

import com.tcc.pjb.backend.core.catalog.TpuClasseCnj;
import com.tcc.pjb.backend.core.preflight.ProceduralPreflightEngine;
import com.tcc.pjb.backend.integration.judicial.routing.TribunalProtocolRoutingService;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class NationalProceduralPreflightPayloadFactory {

    public Map<String, Object> buildProtocolRoutingPayload(Map<String, Object> payload,
                                                           TpuClasseCnj classeTpu,
                                                           String tribunalCodigo,
                                                           String comarca,
                                                           String uf,
                                                           String unidadeCodigo,
                                                           String varaSugerida) {
        Map<String, Object> source = payload;
        LinkedHashMap<String, Object> out = new LinkedHashMap<>(source);
        String varaPretendida = text(source.get("varaPretendida"));
        out.put("tribunalCodigo", tribunalCodigo);
        out.put("comarca", comarca);
        out.put("uf", uf);
        out.put("vara", firstNonBlank(unidadeCodigo, varaSugerida, varaPretendida));
        if (classeTpu != null) {
            out.put("classeTpu", String.valueOf(classeTpu.codigoTpu()));
            out.put("classeTpuNome", classeTpu.descricao());
        }
        return out;
    }

    public Map<String, Object> buildPreflightPayload(Map<String, Object> payload,
                                                     TpuClasseCnj classeTpu,
                                                     String ritoSugerido,
                                                     ProceduralCanonicalResolver.CanonicalContext canonical,
                                                     String tribunalCodigo,
                                                     String comarca,
                                                     String uf,
                                                     String unidadeCodigo,
                                                     String varaSugerida,
                                                     TribunalProtocolRoutingService.RoutingDecision routingDecision) {
        Map<String, Object> source = payload;
        LinkedHashMap<String, Object> out = new LinkedHashMap<>(source);
        String classe = text(source.get("classe"));
        String ramoDireito = text(source.get("ramoDireito"));
        String materia = text(source.get("materia"));
        String varaPretendida = text(source.get("varaPretendida"));
        BigDecimal valorCausa = decimal(source.get("valorCausa"));
        String tipoAcao = text(source.get("tipoAcao"));
        Object assinadoDigitalmente = raw(source, "assinadoDigitalmente");
        Object assinaturaDigital = raw(source, "assinaturaDigital");
        Object certificadoDigital = raw(source, "certificadoDigital");
        Object segredoJustica = raw(source, "segredoJustica");
        String corpus = buildCorpus(source);
        out.put("rito", ritoSugerido);
        out.put("classeTpu", classeTpu != null ? String.valueOf(classeTpu.codigoTpu()) : firstNonBlank(canonical.classeTpuCodigo(), classe));
        out.put("ramoDireito", firstNonBlank(canonical.ramoDireito(), ramoDireito, materia));
        out.put("tribunalCodigo", tribunalCodigo);
        out.put("uf", uf);
        out.put("comarca", comarca);
        out.put("vara", firstNonBlank(unidadeCodigo, varaSugerida, varaPretendida));
        out.put("valorCausa", valorCausa);
        out.put("partesPresentes", extractPresentParties(source));
        out.put("documentosPresentes", extractPresentDocuments(source, canonical));
        out.put("formatosArquivo", extractAttachmentFormats(source));
        out.put("assinadoDigitalmente", bool(assinadoDigitalmente) || bool(assinaturaDigital) || bool(certificadoDigital));
        out.put("possuiProcuracao", hasProcuracao(payload));
        out.put("recurso", containsAny(tipoAcao, "RECURSO", "APELACAO") || containsAny(corpus, "AGRAVO", "EMBARGOS", "RECURSO"));
        out.put("tutela", containsAny(corpus, "TUTELA", "LIMINAR", "URGENTE"));
        out.put("segredoJustica", bool(segredoJustica) || containsAny(corpus, "SEGREDO DE JUSTICA", "SIGILO"));
        if (routingDecision != null && routingDecision.judicialSystem() != null) {
            out.put("connectorSystem", routingDecision.judicialSystem().name());
        }
        return out;
    }

    public boolean isStructuralPreflightBlocker(ProceduralPreflightEngine.PreflightIssue issue) {
        if (issue == null) {
            return false;
        }
        String field = normalize(firstNonBlank(issue.field(), issue.code()));
        return containsAny(field, "WORKFLOW", "TRIBUNALCODIGO", "VARA", "COMARCA", "DOCUMENTOS", "PARTES", "RITO", "CLASSETPU", "CONNECTOR")
                || containsAny(normalize(issue.message()),
                "WORKFLOW",
                "TRIBUNAL",
                "CONECTOR",
                "DOCUMENTO OBRIGATORIO AUSENTE",
                "PARTE OBRIGATORIA AUSENTE",
                "RITO AUSENTE",
                "RITO NAO RECONHECIDO",
                "CLASSE TPU NAO RESOLVIDA",
                "SANITY GATE BLOQUEANTE",
                "STEP UP REQUERIDO",
                "ASSINATURA DIGITAL AUSENTE",
                "PROCURACAO AUSENTE");
    }

    private List<String> extractPresentParties(Map<String, Object> payload) {
        Map<String, Object> source = payload;
        LinkedHashSet<String> out = new LinkedHashSet<>();
        String parteAutoraNome = text(source.get("parteAutoraNome"));
        String parteAutoraCpf = text(source.get("parteAutoraCpf"));
        String autor = text(source.get("autor"));
        String parteReuNome = text(source.get("parteReuNome"));
        String parteReuCpf = text(source.get("parteReuCpf"));
        String reu = text(source.get("reu"));
        if (!isBlank(firstNonBlank(parteAutoraNome, parteAutoraCpf, autor))) {
            out.add("AUTOR");
        }
        if (!isBlank(firstNonBlank(parteReuNome, parteReuCpf, reu))) {
            out.add("REU");
        }
        String corpus = buildCorpus(payload);
        if (containsAny(corpus, "IMPETRANTE")) out.add("IMPETRANTE");
        if (containsAny(corpus, "AUTORIDADE COATORA")) out.add("AUTORIDADE_COATORA");
        if (containsAny(corpus, "MINISTERIO PUBLICO", "MP ")) out.add("MP");
        if (containsAny(corpus, "INSS")) out.add("INSS");
        if (containsAny(corpus, "EXEQUENTE")) out.add("EXEQUENTE");
        if (containsAny(corpus, "EXECUTADO")) out.add("EXECUTADO");
        return List.copyOf(out);
    }

    private List<String> extractPresentDocuments(Map<String, Object> payload,
                                                 ProceduralCanonicalResolver.CanonicalContext canonical) {
        if (payload.containsKey("documentosTipados")) {
            Object raw = payload.get("documentosTipados");
            if (raw instanceof List<?> lista) {
                return lista.stream()
                        .filter(item -> item instanceof String)
                        .map(item -> (String) item)
                        .toList();
            }
            return List.of();
        }
        Map<String, Object> source = payload;
        String provas = text(source.get("provas"));
        String materialProbatorioResumo = text(source.get("materialProbatorioResumo"));
        String documentos = text(source.get("documentos"));
        String anexos = text(source.get("anexos"));
        Object assinaturaDigital = source.get("assinaturaDigital");
        Object assinadoDigitalmente = source.get("assinadoDigitalmente");
        LinkedHashSet<String> out = new LinkedHashSet<>(splitTextualItems(provas, materialProbatorioResumo, documentos, anexos));
        if (hasProcuracao(payload)) {
            out.add("PROCURACAO");
        }
        if (bool(assinaturaDigital) || bool(assinadoDigitalmente)) {
            out.add("ASSINATURA_DIGITAL");
        }
        if (canonical != null && canonical.requiredDocuments() != null && !out.isEmpty()) {
            canonical.requiredDocuments().stream()
                    .filter(doc -> out.stream().anyMatch(item -> normalize(item).contains(normalize(doc)) || normalize(doc).contains(normalize(item))))
                    .forEach(out::add);
        }
        return List.copyOf(out);
    }

    private List<String> extractAttachmentFormats(Map<String, Object> payload) {
        Map<String, Object> source = payload;
        LinkedHashSet<String> out = new LinkedHashSet<>();
        Object preferenciaDigital = source.get("preferenciaDigital");
        String provas = text(source.get("provas"));
        if (bool(preferenciaDigital) || !isBlank(provas)) {
            out.add("application/pdf");
        }
        String format = firstNonBlank(text(source.get("mimeType")), text(source.get("contentType")));
        if (!isBlank(format)) {
            out.add(format);
        }
        return List.copyOf(out);
    }

    private boolean hasProcuracao(Map<String, Object> payload) {
        Map<String, Object> source = payload;
        Object possuiProcuracao = source.get("possuiProcuracao");
        String provas = text(source.get("provas"));
        String materialProbatorioResumo = text(source.get("materialProbatorioResumo"));
        String documentos = text(source.get("documentos"));
        return bool(possuiProcuracao)
                || containsAny(firstNonBlank(provas, materialProbatorioResumo, documentos), "PROCURACAO", "PROCURAÇÃO", "MANDATO");
    }
}
