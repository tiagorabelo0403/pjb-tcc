package com.tcc.pjb.backend.service.processual.peticionamento.journey;

import com.tcc.pjb.backend.model.dto.processual.peticionamento.session.PeticionamentoSessaoRequest;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

final class PeticionamentoJourneyPayloadSupport {

    private PeticionamentoJourneyPayloadSupport() {
    }

    static Map<String, Object> buildPayload(PeticionamentoSessaoRequest request) {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        if (request == null) {
            return payload;
        }
        put(payload, "processoId", request.getProcessoId());
        put(payload, "tituloCaso", request.getTituloCaso());
        put(payload, "parteAutoraNome", request.getParteAutora());
        put(payload, "parteReuNome", request.getParteRe());
        put(payload, "ramoDireito", request.getRamoDireito());
        put(payload, "rito", request.getRitoProcessual());
        put(payload, "classe", request.getClasseProcessual());
        put(payload, "classeProcessual", request.getClasseProcessual());
        put(payload, "assunto", request.getAssuntoTpu());
        put(payload, "objetoProcessual", request.getMateriaPrincipal());
        put(payload, "tipoJustica", request.getTipoJustica());
        put(payload, "valorCausa", request.getValorCausa());
        put(payload, "pedidoPrincipal", firstNonBlank(firstItem(safeList(request.getPedidos())), request.getTextoPeticaoLivre()));
        put(payload, "pedidos", safeList(request.getPedidos()));
        put(payload, "provas", mergeDistinct(safeList(request.getProvasIndicadas()), safeList(request.getProvasDocumentais())));
        put(payload, "foro", request.getCidadeProtocolo());
        put(payload, "comarcaAutor", request.getCidadeFato());
        put(payload, "ufAutor", request.getUfFato());
        if (request.getCtx() != null) {
            Object tribunalCodigo = request.getCtx().get("tribunalCodigo");
            Object varaPretendida = request.getCtx().get("unidadeJudiciariaCodigo");
            put(payload, "tribunalCodigo", tribunalCodigo);
            put(payload, "varaPretendida", varaPretendida);
        }
        return payload;
    }

    static List<String> safeList(List<?> value) {
        return value == null ? List.of() : value.stream().filter(Objects::nonNull).map(String::valueOf).filter(PeticionamentoJourneyPayloadSupport::filled).toList();
    }

    static List<String> mergeDistinct(List<String> first, List<String> second) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        out.addAll(first == null ? List.of() : first);
        out.addAll(second == null ? List.of() : second);
        return List.copyOf(out);
    }

    static void put(Map<String, Object> payload, String key, Object value) {
        if (key != null && value != null) {
            payload.put(key, value);
        }
    }

    static String firstItem(List<String> values) {
        return values == null || values.isEmpty() ? null : values.getFirst();
    }

    static String firstNonBlank(String first, String second) {
        return filled(first) ? first.trim() : filled(second) ? second.trim() : null;
    }

    static String normalizeUpper(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    static boolean filled(String value) {
        return value != null && !value.isBlank();
    }
}
