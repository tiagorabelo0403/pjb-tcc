package com.tcc.pjb.backend.core.plataforma.substituicao.persistence.entity;

import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoComunicacaoSyncSituacao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "tb_pjb_subst_com_sync_item",
        indexes = {
                @Index(name = "ix_pjb_subst_com_item_cursor", columnList = "cursor_id, created_at"),
                @Index(name = "ix_pjb_subst_com_item_corr", columnList = "correlation_key, situacao")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_pjb_subst_com_item_cursor_dedupe", columnNames = {"cursor_id", "dedupe_hash"})
        })
public class PjbSubstituicaoComunicacaoSyncItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "ver", nullable = false)
    private long version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cursor_id", nullable = false)
    private PjbSubstituicaoComunicacaoSyncCursorEntity cursor;

    @Column(name = "dedupe_hash", nullable = false, length = 128)
    private String dedupeHash;

    @Column(name = "external_message_id", length = 180)
    private String externalMessageId;

    @Column(name = "correlation_key", nullable = false, length = 180)
    private String correlationKey;

    @Column(name = "processo_numero", length = 64)
    private String processoNumero;

    @Enumerated(EnumType.STRING)
    @Column(name = "situacao", nullable = false, length = 32)
    private PjbSubstituicaoComunicacaoSyncSituacao situacao;

    @Column(name = "reprocessavel", nullable = false)
    private boolean reprocessavel;

    @Column(name = "payload_json", nullable = false, columnDefinition = "text")
    private String payloadJson;

    @Column(name = "resultado_json", nullable = false, columnDefinition = "text")
    private String resultadoJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PjbSubstituicaoComunicacaoSyncItemEntity() {
    }

    public PjbSubstituicaoComunicacaoSyncItemEntity(PjbSubstituicaoComunicacaoSyncCursorEntity cursor,
                                                    String dedupeHash,
                                                    String externalMessageId,
                                                    String correlationKey,
                                                    String processoNumero,
                                                    PjbSubstituicaoComunicacaoSyncSituacao situacao,
                                                    boolean reprocessavel,
                                                    String payloadJson,
                                                    String resultadoJson,
                                                    Instant createdAt,
                                                    Instant updatedAt) {
        this.cursor = Objects.requireNonNull(cursor, "cursor");
        this.dedupeHash = require(dedupeHash, "dedupeHash");
        this.externalMessageId = normalize(externalMessageId);
        this.correlationKey = require(correlationKey, "correlationKey");
        this.processoNumero = normalize(processoNumero);
        this.situacao = Objects.requireNonNull(situacao, "situacao");
        this.reprocessavel = reprocessavel;
        this.payloadJson = requireJson(payloadJson, "payloadJson");
        this.resultadoJson = requireJson(resultadoJson, "resultadoJson");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public Long getId() { return id; }
    public PjbSubstituicaoComunicacaoSyncCursorEntity getCursor() { return cursor; }
    public String getDedupeHash() { return dedupeHash; }
    public String getExternalMessageId() { return externalMessageId; }
    public String getCorrelationKey() { return correlationKey; }
    public String getProcessoNumero() { return processoNumero; }
    public PjbSubstituicaoComunicacaoSyncSituacao getSituacao() { return situacao; }
    public boolean isReprocessavel() { return reprocessavel; }
    public String getPayloadJson() { return payloadJson; }
    public String getResultadoJson() { return resultadoJson; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void refresh(PjbSubstituicaoComunicacaoSyncSituacao situacao,
                        boolean reprocessavel,
                        String resultadoJson,
                        Instant updatedAt) {
        this.situacao = Objects.requireNonNull(situacao, "situacao");
        this.reprocessavel = reprocessavel;
        this.resultadoJson = requireJson(resultadoJson, "resultadoJson");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    private static String require(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " é obrigatório");
        }
        return value.trim();
    }

    private static String requireJson(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " é obrigatório");
        }
        return value;
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
