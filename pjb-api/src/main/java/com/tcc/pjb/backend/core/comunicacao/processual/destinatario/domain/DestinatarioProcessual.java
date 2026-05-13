package com.tcc.pjb.backend.core.comunicacao.processual.destinatario.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import com.tcc.pjb.backend.core.util.PayloadMaps;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioProcessualKind;
import com.tcc.pjb.backend.model.entity.enums.NationalCommunicationRecipientKind;
import com.tcc.pjb.backend.model.entity.enums.PapelProcessualInstitucional;
import com.tcc.pjb.backend.model.entity.enums.TrilhoComunicacaoProcessual;

public record DestinatarioProcessual(
        DestinatarioProcessualKind kind,
        TrilhoComunicacaoProcessual trilho,
        NationalCommunicationRecipientKind legacyKind,
        String documentoPrincipal,
        String nomeExibicao,
        String email,
        String telefone,
        String oabNumero,
        String govbrAccountId,
        String uf,
        String comarca,
        String foro,
        DestinatarioInstitucionalKind destinatarioInstitucionalKind,
        PapelProcessualInstitucional papelProcessualInstitucional,
        String unidadeInstitucionalCodigo,
        boolean exigeCaixaInstitucional,
        boolean exigeIntimacaoPessoal,
        boolean admiteCitacao,
        boolean admiteIntimacao,
        List<String> justificativas,
        String hashResolucao) {

    public DestinatarioProcessual {
        kind = Objects.requireNonNull(kind);
        trilho = Objects.requireNonNull(trilho);
        documentoPrincipal = normalizarDocumento(documentoPrincipal);
        nomeExibicao = nomeExibicao == null || nomeExibicao.isBlank() ? "DESTINATARIO_PROCESSUAL" : nomeExibicao.trim();
        justificativas = PayloadMaps.copyTrimmedStrings(justificativas);
        hashResolucao = hashResolucao == null || hashResolucao.isBlank()
                ? hash(kind, trilho, legacyKind, documentoPrincipal, nomeExibicao, destinatarioInstitucionalKind, papelProcessualInstitucional, unidadeInstitucionalCodigo, justificativas)
                : hashResolucao;
    }

    public boolean isInstitucional() {
        return trilho == TrilhoComunicacaoProcessual.INSTITUCIONAL_CAIXA;
    }

    public boolean isPessoal() {
        return trilho == TrilhoComunicacaoProcessual.PESSOAL_DIRETO;
    }

    public boolean isRepresentacional() {
        return trilho == TrilhoComunicacaoProcessual.REPRESENTACAO_PROCESSUAL;
    }

    private static String normalizarDocumento(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String sanitized = raw.replaceAll("\\D", "");
        return sanitized.isBlank() ? raw.trim().toUpperCase(Locale.ROOT) : sanitized;
    }

    private static String hash(Object... parts) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (Object part : parts) {
                if (part == null) {
                    digest.update((byte) 0x00);
                } else {
                    digest.update(String.valueOf(part).getBytes(StandardCharsets.UTF_8));
                    digest.update((byte) 0x7c);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception ex) {
            throw new IllegalStateException("Falha ao gerar hash do destinatário processual", ex);
        }
    }
}
