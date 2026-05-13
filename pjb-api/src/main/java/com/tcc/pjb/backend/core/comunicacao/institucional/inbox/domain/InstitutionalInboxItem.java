package com.tcc.pjb.backend.core.comunicacao.institucional.inbox.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.core.util.PayloadMaps;
import com.tcc.pjb.backend.core.comunicacao.judicial.TipoComunicacaoJudicial;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.PapelProcessualInstitucional;
import com.tcc.pjb.backend.model.entity.enums.StatusComunicacaoInstitucional;

public record InstitutionalInboxItem(
        String inboxItemId,
        String expedicaoUuid,
        Long processoId,
        String processoNumero,
        String unidadeCodigo,
        String unidadeSigla,
        DestinatarioInstitucionalKind destinatarioKind,
        PapelProcessualInstitucional papelProcessual,
        TipoComunicacaoJudicial tipoComunicacao,
        String caixaCodigoOrigem,
        String caixaCodigoAtual,
        String canalPrincipal,
        StatusComunicacaoInstitucional status,
        String gateCode,
        boolean bloqueiaFluxo,
        Long atribuidoUsuarioId,
        Long ultimoOperadorUsuarioId,
        Instant disponibilizadaEm,
        Instant recebidaEm,
        Instant cientificadaEm,
        Instant cumpridaEm,
        Instant prazoCienciaEm,
        Instant prazoRespostaEm,
        Instant updatedAt,
        List<String> justificativas,
        String hashIntegridade
) {
    public InstitutionalInboxItem {
        inboxItemId = require(inboxItemId, "inboxItemId");
        expedicaoUuid = require(expedicaoUuid, "expedicaoUuid");
        Objects.requireNonNull(processoId, "processoId");
        processoNumero = normalizeOptional(processoNumero);
        unidadeCodigo = require(unidadeCodigo, "unidadeCodigo");
        unidadeSigla = require(unidadeSigla, "unidadeSigla");
        Objects.requireNonNull(destinatarioKind, "destinatarioKind");
        Objects.requireNonNull(papelProcessual, "papelProcessual");
        Objects.requireNonNull(tipoComunicacao, "tipoComunicacao");
        caixaCodigoOrigem = require(caixaCodigoOrigem, "caixaCodigoOrigem");
        caixaCodigoAtual = require(caixaCodigoAtual, "caixaCodigoAtual");
        canalPrincipal = require(canalPrincipal, "canalPrincipal");
        Objects.requireNonNull(status, "status");
        gateCode = normalizeOptional(gateCode);
        Objects.requireNonNull(disponibilizadaEm, "disponibilizadaEm");
        Objects.requireNonNull(prazoCienciaEm, "prazoCienciaEm");
        Objects.requireNonNull(prazoRespostaEm, "prazoRespostaEm");
        Objects.requireNonNull(updatedAt, "updatedAt");
        justificativas = normalizeJustificativas(justificativas);
        hashIntegridade = normalizeHash(hashIntegridade, inboxItemId, expedicaoUuid, caixaCodigoAtual, status, updatedAt);
    }

    public InstitutionalInboxItem withRecebimento(Long usuarioId, Instant at, String hash, List<String> justificativas) {
        return new InstitutionalInboxItem(
                inboxItemId,
                expedicaoUuid,
                processoId,
                processoNumero,
                unidadeCodigo,
                unidadeSigla,
                destinatarioKind,
                papelProcessual,
                tipoComunicacao,
                caixaCodigoOrigem,
                caixaCodigoAtual,
                canalPrincipal,
                StatusComunicacaoInstitucional.RECEBIDA,
                gateCode,
                bloqueiaFluxo,
                atribuidoUsuarioId,
                usuarioId,
                disponibilizadaEm,
                at,
                cientificadaEm,
                cumpridaEm,
                prazoCienciaEm,
                prazoRespostaEm,
                at,
                mergeJustificativas(this.justificativas, justificativas),
                hash
        );
    }

    public InstitutionalInboxItem withRedistribuicao(String novaCaixaCodigo,
                                                     Long usuarioId,
                                                     Instant at,
                                                     String hash,
                                                     List<String> justificativas) {
        return new InstitutionalInboxItem(
                inboxItemId,
                expedicaoUuid,
                processoId,
                processoNumero,
                unidadeCodigo,
                unidadeSigla,
                destinatarioKind,
                papelProcessual,
                tipoComunicacao,
                caixaCodigoOrigem,
                require(novaCaixaCodigo, "novaCaixaCodigo"),
                canalPrincipal,
                StatusComunicacaoInstitucional.DISPONIBILIZADA,
                gateCode,
                bloqueiaFluxo,
                atribuidoUsuarioId,
                usuarioId,
                disponibilizadaEm,
                recebidaEm,
                cientificadaEm,
                cumpridaEm,
                prazoCienciaEm,
                prazoRespostaEm,
                at,
                mergeJustificativas(this.justificativas, justificativas),
                hash
        );
    }

    public InstitutionalInboxItem withCiencia(Long usuarioId, Instant at, String hash, List<String> justificativas) {
        return new InstitutionalInboxItem(
                inboxItemId,
                expedicaoUuid,
                processoId,
                processoNumero,
                unidadeCodigo,
                unidadeSigla,
                destinatarioKind,
                papelProcessual,
                tipoComunicacao,
                caixaCodigoOrigem,
                caixaCodigoAtual,
                canalPrincipal,
                StatusComunicacaoInstitucional.CIENTIFICADA,
                gateCode,
                bloqueiaFluxo,
                atribuidoUsuarioId,
                usuarioId,
                disponibilizadaEm,
                recebidaEm,
                at,
                cumpridaEm,
                prazoCienciaEm,
                prazoRespostaEm,
                at,
                mergeJustificativas(this.justificativas, justificativas),
                hash
        );
    }

    public InstitutionalInboxItem withCumprimento(Long usuarioId, Instant at, String hash, List<String> justificativas) {
        return new InstitutionalInboxItem(
                inboxItemId,
                expedicaoUuid,
                processoId,
                processoNumero,
                unidadeCodigo,
                unidadeSigla,
                destinatarioKind,
                papelProcessual,
                tipoComunicacao,
                caixaCodigoOrigem,
                caixaCodigoAtual,
                canalPrincipal,
                StatusComunicacaoInstitucional.CUMPRIDA,
                gateCode,
                bloqueiaFluxo,
                atribuidoUsuarioId,
                usuarioId,
                disponibilizadaEm,
                recebidaEm,
                cientificadaEm,
                at,
                prazoCienciaEm,
                prazoRespostaEm,
                at,
                mergeJustificativas(this.justificativas, justificativas),
                hash
        );
    }

    public InstitutionalInboxItem withAtribuicao(Long atribuidoUsuarioId,
                                                 Long usuarioOperadorId,
                                                 Instant at,
                                                 String hash,
                                                 List<String> justificativas) {
        return new InstitutionalInboxItem(
                inboxItemId,
                expedicaoUuid,
                processoId,
                processoNumero,
                unidadeCodigo,
                unidadeSigla,
                destinatarioKind,
                papelProcessual,
                tipoComunicacao,
                caixaCodigoOrigem,
                caixaCodigoAtual,
                canalPrincipal,
                status,
                gateCode,
                bloqueiaFluxo,
                atribuidoUsuarioId,
                usuarioOperadorId,
                disponibilizadaEm,
                recebidaEm,
                cientificadaEm,
                cumpridaEm,
                prazoCienciaEm,
                prazoRespostaEm,
                at,
                mergeJustificativas(this.justificativas, justificativas),
                hash
        );
    }



    private static List<String> mergeJustificativas(List<String> atuais, List<String> novas) {
        java.util.LinkedHashSet<String> merged = new java.util.LinkedHashSet<>();
        normalizeJustificativas(atuais).forEach(merged::add);
        normalizeJustificativas(novas).forEach(merged::add);
        return merged.isEmpty() ? List.of() : List.copyOf(merged);
    }

    private static List<String> normalizeJustificativas(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return PayloadMaps.copyTrimmedStrings(values);
    }

    private static String normalizeHash(String hashIntegridade,
                                        String inboxItemId,
                                        String expedicaoUuid,
                                        String caixaCodigoAtual,
                                        StatusComunicacaoInstitucional status,
                                        Instant updatedAt) {
        if (hashIntegridade != null && !hashIntegridade.isBlank()) {
            return hashIntegridade.trim();
        }
        return Hashes.sha256Hex(inboxItemId + "|" + expedicaoUuid + "|" + caixaCodigoAtual + "|" + status.name() + "|" + updatedAt);
    }

    private static String require(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " é obrigatório");
        }
        return value.trim();
    }

    private static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
