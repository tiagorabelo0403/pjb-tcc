package com.tcc.pjb.backend.service.processual.participacao;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.enums.DocumentoCategoria;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.service.exception.ErroDeValidacaoException;
import com.tcc.pjb.backend.service.processual.participacao.submission.SubmissionRequest;
import com.tcc.pjb.backend.service.exception.enums.TipoErroValidacao;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public final class ProcessualParticipacaoAtivaSupportUtils {

    public static final String ORIGEM_SISTEMA = "PJB:PARTICIPACAO_ATIVA_V1";

    private ProcessualParticipacaoAtivaSupportUtils() {
    }

    public static NivelSigilo resolveSigilo(Processo processo) {
        return processo.getNivelSigilo() == null ? NivelSigilo.PUBLICO : processo.getNivelSigilo();
    }

    public static NivelSigilo resolveRequestedSigilo(SubmissionRequest request, Processo processo, ActionProfile action) {
        NivelSigilo base = resolveSigilo(processo);
        if (action.highSensitivity()) {
            base = maxSigilo(base, NivelSigilo.SIGILO_N2);
        }
        String requested = trimToNull(request.nivelSigilo());
        return requested == null ? base : maxSigilo(base, NivelSigilo.fromString(requested));
    }

    public static DocumentoCategoria resolveRequestedCategoria(SubmissionRequest request, NivelSigilo sigilo) {
        String requested = trimToNull(request.categoriaDocumentoPrincipal());
        if (requested == null) {
            return sigilo.getNivel() >= NivelSigilo.SIGILO_N2.getNivel() ? DocumentoCategoria.PESSOAL : DocumentoCategoria.PUBLICO;
        }
        return DocumentoCategoria.fromString(requested);
    }

    public static byte[] decodeContent(String base64, String filename) {
        try {
            String normalized = requireText(base64, filename + ".base64Content");
            return Base64.getDecoder().decode(normalized);
        } catch (IllegalArgumentException ex) {
            throw new ErroDeValidacaoException(TipoErroValidacao.FORMATO_INVALIDO, "Base64 inválido no anexo " + filename + '.');
        }
    }

    public static String shaHex(String algorithm, byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            return HexFormat.of().formatHex(md.digest(bytes));
        } catch (Exception ex) {
            throw new IllegalStateException("Falha ao calcular hash", ex);
        }
    }

    public static String normalizeContentType(String contentType) {
        String normalized = trimToNull(contentType);
        return normalized == null ? "application/pdf" : normalized.toLowerCase(Locale.ROOT);
    }

    public static Instant resolveDueAt(boolean urgent, FaseProcessual fase) {
        if (urgent) {
            return Instant.now().plusSeconds(4 * 60 * 60);
        }
        if (fase == null) {
            return Instant.now().plusSeconds(72 * 60 * 60);
        }
        return switch (fase) {
            case RECURSAL -> Instant.now().plusSeconds(24 * 60 * 60);
            case PERICIA_TECNICA, AUDIENCIA_CUSTODIA -> Instant.now().plusSeconds(12 * 60 * 60);
            case EXECUCAO, CUMPRIMENTO_SENTENCA -> Instant.now().plusSeconds(36 * 60 * 60);
            default -> Instant.now().plusSeconds(72 * 60 * 60);
        };
    }

    public static Optional<ActionProfile> findByCode(List<ActionProfile> actions, String code) {
        String normalized = normalizeToken(code);
        return actions.stream().filter(item -> item.code().equals(normalized)).findFirst();
    }

    public static void add(List<ActionProfile> target, Persona persona, ActionProfile action) {
        if (persona != Persona.NAO_SUPORTADA) {
            target.add(action);
        }
    }

    public static ActionProfile action(String code,
                                String label,
                                WorkItemType workItemType,
                                int priority,
                                boolean blocking,
                                boolean highSensitivity,
                                List<FaseProcessual> phases,
                                List<String> tags,
                                List<String> innovations,
                                List<String> checklist) {
        return new ActionProfile(
                normalizeToken(code),
                label,
                workItemType,
                priority,
                blocking,
                highSensitivity,
                List.copyOf(phases),
                List.copyOf(tags),
                List.copyOf(innovations),
                List.copyOf(checklist),
                null
        );
    }

    public static List<TipoUsuario> resolveActorRoles(TipoUsuario tipoUsuario) {
        if (tipoUsuario == null) {
            return List.of();
        }
        LinkedHashSet<TipoUsuario> out = new LinkedHashSet<>();
        out.add(tipoUsuario);
        if (tipoUsuario.isDefensoriaPublica()) {
            out.add(TipoUsuario.DEFENSOR_PUBLICO);
        }
        if (tipoUsuario.isProcuradoria()) {
            out.add(TipoUsuario.PROCURADOR);
        }
        if (tipoUsuario.isMinisterioPublico()) {
            out.add(TipoUsuario.MEMBRO_MINISTERIO_PUBLICO);
        }
        if (tipoUsuario.isPerito()) {
            out.add(TipoUsuario.PERITO);
        }
        if (tipoUsuario.isAdvocacia()) {
            out.add(TipoUsuario.ADVOGADO);
        }
        return List.copyOf(out);
    }

    public static String receptionTemplateCode(Persona persona, ActionProfile action, DocumentoProcessual primary) {
        String suffix = firstNonBlank(primary.getSha256(), primary.getId() == null ? null : primary.getId().toString(), UUID.randomUUID().toString());
        return truncate("PARTICIPACAO_ATIVA:" + persona.name() + ':' + action.code() + ':' + suffix.substring(0, Math.min(16, suffix.length())), 120);
    }

    public static String inlineStorageUri(Long processoId, String sha256) {
        return "inline://processos/" + processoId + "/participacao-ativa/" + sha256;
    }

    public static ErroDeValidacaoException validation(String message) {
        return new ErroDeValidacaoException(TipoErroValidacao.REGRA_NEGOCIO, message);
    }

    public static ErroDeValidacaoException duplicateValidation(String message) {
        return new ErroDeValidacaoException(TipoErroValidacao.DUPLICIDADE_DETECTADA, message);
    }

    public static ErroDeValidacaoException sizeValidation(String message) {
        return new ErroDeValidacaoException(TipoErroValidacao.TAMANHO_EXCEDIDO, message);
    }

    public static ErroDeValidacaoException formatValidation(String message) {
        return new ErroDeValidacaoException(TipoErroValidacao.FORMATO_INVALIDO, message);
    }

    public static String requireText(String value, String field) {
        String out = trimToNull(value);
        if (out == null) {
            throw new ErroDeValidacaoException(TipoErroValidacao.CAMPO_OBRIGATORIO, "Campo obrigatório ausente: " + field);
        }
        return out;
    }

    public static String redactCertificate(String value) {
        String token = trimToNull(value);
        if (token == null || token.length() <= 8) {
            return token;
        }
        return token.substring(0, 4) + "..." + token.substring(token.length() - 4);
    }

    public static NivelSigilo maxSigilo(NivelSigilo first, NivelSigilo second) {
        NivelSigilo a = first == null ? NivelSigilo.PUBLICO : first;
        NivelSigilo b = second == null ? NivelSigilo.PUBLICO : second;
        return a.getNivel() >= b.getNivel() ? a : b;
    }

    public static String safeName(Enum<?> value) {
        return value == null ? "NAO_INFORMADO" : value.name();
    }

    public static String normalizeUf(String value) {
        String out = trimToNull(value);
        if (out == null) {
            return null;
        }
        return out.toUpperCase(Locale.ROOT);
    }

    public static String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    public static String normalizeToken(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT)
                .replace('Á', 'A')
                .replace('À', 'A')
                .replace('Ã', 'A')
                .replace('Â', 'A')
                .replace('É', 'E')
                .replace('Ê', 'E')
                .replace('Í', 'I')
                .replace('Ó', 'O')
                .replace('Ô', 'O')
                .replace('Õ', 'O')
                .replace('Ú', 'U')
                .replace('Ç', 'C')
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("_{2,}", "_")
                .replaceAll("^_+|_+$", "");
    }

    public static String slugFilename(String value, String extension) {
        String token = normalizeToken(value).toLowerCase(Locale.ROOT);
        return (token.isBlank() ? "documento" : token) + extension;
    }

    public static String truncate(String value, int maxLen) {
        if (value == null || value.length() <= maxLen) {
            return value;
        }
        return value.substring(0, maxLen);
    }

    public static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    public static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
