package com.tcc.pjb.backend.core.comunicacao.institucional.workflow.domain;

import com.tcc.pjb.backend.model.entity.enums.StatusMinutaInstitucional;
import java.time.Instant;
import java.util.Objects;

public record InstitutionalDraftManifestation(
        String draftId,
        String expedicaoUuid,
        Long processoId,
        String unidadeCodigo,
        String caixaCodigo,
        Long autorUsuarioId,
        Long aprovadorUsuarioId,
        StatusMinutaInstitucional status,
        String titulo,
        String conteudo,
        String observacoes,
        Instant createdAt,
        Instant submittedAt,
        Instant reviewedAt,
        Instant updatedAt,
        String hashIntegridade
) {
    public InstitutionalDraftManifestation {
        draftId = require(draftId, "draftId");
        expedicaoUuid = require(expedicaoUuid, "expedicaoUuid");
        Objects.requireNonNull(processoId, "processoId");
        unidadeCodigo = require(unidadeCodigo, "unidadeCodigo");
        caixaCodigo = require(caixaCodigo, "caixaCodigo");
        Objects.requireNonNull(autorUsuarioId, "autorUsuarioId");
        Objects.requireNonNull(status, "status");
        titulo = require(titulo, "titulo");
        conteudo = require(conteudo, "conteudo");
        observacoes = normalize(observacoes);
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
        hashIntegridade = require(hashIntegridade, "hashIntegridade");
    }

    public InstitutionalDraftManifestation withConteudo(String titulo, String conteudo, String observacoes, Instant at, String hash) {
        if (status == StatusMinutaInstitucional.ENVIADA) {
            throw new IllegalStateException("minuta_enviada_nao_pode_ser_editada");
        }
        Instant now = at == null ? Instant.now() : at;
        return new InstitutionalDraftManifestation(
                draftId, expedicaoUuid, processoId, unidadeCodigo, caixaCodigo, autorUsuarioId, aprovadorUsuarioId,
                StatusMinutaInstitucional.RASCUNHO, require(titulo, "titulo"), require(conteudo, "conteudo"), normalize(observacoes),
                createdAt, null, null, now, hash
        );
    }

    public InstitutionalDraftManifestation withSubmissao(Long aprovadorUsuarioId, String observacoes, Instant at, String hash) {
        if (status != StatusMinutaInstitucional.RASCUNHO) {
            throw new IllegalStateException("minuta_somente_pode_ser_submetida_a_partir_de_rascunho");
        }
        Instant now = at == null ? Instant.now() : at;
        return new InstitutionalDraftManifestation(
                draftId, expedicaoUuid, processoId, unidadeCodigo, caixaCodigo, autorUsuarioId, aprovadorUsuarioId,
                StatusMinutaInstitucional.EM_APROVACAO, titulo, conteudo, normalize(observacoes),
                createdAt, now, null, now, hash
        );
    }

    public InstitutionalDraftManifestation withAprovacao(String observacoes, Instant at, String hash) {
        if (status != StatusMinutaInstitucional.EM_APROVACAO) {
            throw new IllegalStateException("minuta_somente_pode_ser_aprovada_quando_estiver_em_aprovacao");
        }
        Instant now = at == null ? Instant.now() : at;
        return new InstitutionalDraftManifestation(
                draftId, expedicaoUuid, processoId, unidadeCodigo, caixaCodigo, autorUsuarioId, aprovadorUsuarioId,
                StatusMinutaInstitucional.APROVADA, titulo, conteudo, normalize(observacoes),
                createdAt, submittedAt, now, now, hash
        );
    }

    public InstitutionalDraftManifestation withRejeicao(String observacoes, Instant at, String hash) {
        if (status != StatusMinutaInstitucional.EM_APROVACAO) {
            throw new IllegalStateException("minuta_somente_pode_ser_rejeitada_quando_estiver_em_aprovacao");
        }
        Instant now = at == null ? Instant.now() : at;
        return new InstitutionalDraftManifestation(
                draftId, expedicaoUuid, processoId, unidadeCodigo, caixaCodigo, autorUsuarioId, aprovadorUsuarioId,
                StatusMinutaInstitucional.REJEITADA, titulo, conteudo, normalize(observacoes),
                createdAt, submittedAt, now, now, hash
        );
    }

    public InstitutionalDraftManifestation withEnvio(Instant at, String hash) {
        if (status != StatusMinutaInstitucional.APROVADA) {
            throw new IllegalStateException("minuta_somente_pode_ser_enviada_quando_estiver_aprovada");
        }
        Instant now = at == null ? Instant.now() : at;
        return new InstitutionalDraftManifestation(
                draftId, expedicaoUuid, processoId, unidadeCodigo, caixaCodigo, autorUsuarioId, aprovadorUsuarioId,
                StatusMinutaInstitucional.ENVIADA, titulo, conteudo, observacoes,
                createdAt, submittedAt, reviewedAt, now, hash
        );
    }

    private static String require(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " é obrigatório");
        }
        return value.trim();
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
