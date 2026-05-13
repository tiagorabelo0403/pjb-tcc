package com.tcc.pjb.backend.service.consultapublica;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public final class ConsultaPublicaDocumentPolicy {

    private static final List<String> TOKENS_DESPACHO = List.of("DESPACHO", "ATO ORDINATORIO", "ATO ORDINATÓRIO", "DECISÃO DE EXPEDIENTE", "DECISAO DE EXPEDIENTE");
    private static final List<String> TOKENS_DECISAO = List.of("DECISAO", "DECISÃO", "DECISAO INTERLOCUTORIA", "DECISÃO INTERLOCUTÓRIA", "DECISAO MONOCRATICA", "DECISÃO MONOCRÁTICA");
    private static final List<String> TOKENS_SENTENCA = List.of("SENTENCA", "SENTENÇA");
    private static final List<String> TOKENS_ACORDAO = List.of("ACORDAO", "ACÓRDÃO");

    private ConsultaPublicaDocumentPolicy() {
    }

    public static boolean canExposePublicAct(Processo processo, DocumentoProcessual documento) {
        if (processo == null || documento == null) {
            return false;
        }
        NivelSigilo processSigilo = processo.getNivelSigilo() == null ? NivelSigilo.PUBLICO : processo.getNivelSigilo();
        NivelSigilo documentSigilo = documento.getNivelSigilo() == null ? NivelSigilo.PUBLICO : documento.getNivelSigilo();
        if (processSigilo.exigeCredencial() || documentSigilo.exigeCredencial()) {
            return false;
        }
        return resolvePublicActKind(documento) != null;
    }

    public static String resolvePublicActKind(DocumentoProcessual documento) {
        String source = normalize(Optional.ofNullable(documento)
                .map(item -> Optional.ofNullable(item.getTitulo()).filter(value -> !value.isBlank()).orElse(item.getNomeOriginal()))
                .orElse(null));
        if (source == null) {
            return null;
        }
        if (containsAny(source, TOKENS_SENTENCA)) {
            return "SENTENCA_PUBLICA";
        }
        if (containsAny(source, TOKENS_ACORDAO)) {
            return "ACORDAO_PUBLICO";
        }
        if (containsAny(source, TOKENS_DECISAO)) {
            return "DECISAO_PUBLICA";
        }
        if (containsAny(source, TOKENS_DESPACHO)) {
            return "DESPACHO_PUBLICO";
        }
        return null;
    }

    private static boolean containsAny(String source, List<String> tokens) {
        return tokens.stream()
                .filter(Objects::nonNull)
                .map(ConsultaPublicaDocumentPolicy::normalize)
                .anyMatch(token -> token != null && source.contains(token));
    }

    private static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return raw.trim().toUpperCase(Locale.ROOT);
    }
}
