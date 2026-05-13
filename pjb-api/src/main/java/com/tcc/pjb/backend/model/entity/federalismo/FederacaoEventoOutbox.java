package com.tcc.pjb.backend.model.entity.federalismo;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@PjbDataOwnership(module = PjbModuleId.INTEGRACOES_EXTERNAS, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(
        name = "tb_federacao_evento_outbox",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_federacao_outbox_idempotency", columnNames = "idempotency_key")
        },
        indexes = {
                @Index(name = "idx_federacao_outbox_status", columnList = "status, proxima_tentativa_em"),
                @Index(name = "idx_federacao_outbox_tribunal", columnList = "tribunal_codigo, topic_kafka"),
                @Index(name = "idx_federacao_outbox_corr", columnList = "correlation_id")
        }
)
public class FederacaoEventoOutbox {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "tribunal_codigo", nullable = false, length = 20)
    private String tribunalCodigo;

    @Column(name = "topic_kafka", nullable = false, length = 180)
    private String topicKafka;

    @Column(name = "event_type", nullable = false, length = 120)
    private String eventType;

    @Column(name = "payload_json", nullable = false, columnDefinition = "TEXT")
    private String payloadJson;

    @Column(name = "payload_hash", nullable = false, length = 64)
    private String payloadHash;

    @Column(name = "idempotency_key", nullable = false, length = 180)
    private String idempotencyKey;

    @Column(name = "correlation_id", length = 120)
    private String correlationId;

    @Column(name = "schema_version", nullable = false)
    private long schemaVersion;

    @Column(name = "tentativas", nullable = false)
    private int tentativas;

    @Column(name = "prioridade", nullable = false)
    private int prioridade;

    @Column(name = "proxima_tentativa_em", nullable = false)
    private Instant proximaTentativaEm;

    @Column(name = "publicado_em")
    private Instant publicadoEm;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusEventoOutboxFederacao status;

    @Column(name = "ultimo_erro", columnDefinition = "TEXT")
    private String ultimoErro;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected FederacaoEventoOutbox() {
    }

    public FederacaoEventoOutbox(UUID id,
                                 String tribunalCodigo,
                                 String topicKafka,
                                 String eventType,
                                 String payloadJson,
                                 String payloadHash,
                                 String idempotencyKey,
                                 String correlationId,
                                 long schemaVersion,
                                 int prioridade) {
        this.id = Objects.requireNonNull(id);
        this.tribunalCodigo = Objects.requireNonNull(tribunalCodigo);
        this.topicKafka = Objects.requireNonNull(topicKafka);
        this.eventType = Objects.requireNonNull(eventType);
        this.payloadJson = Objects.requireNonNull(payloadJson);
        this.payloadHash = Objects.requireNonNull(payloadHash);
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey);
        this.correlationId = correlationId;
        this.schemaVersion = Math.max(1L, schemaVersion);
        this.prioridade = Math.max(0, prioridade);
        this.proximaTentativaEm = Instant.now();
        this.status = StatusEventoOutboxFederacao.PENDENTE;
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        this.tribunalCodigo = normalize(this.tribunalCodigo);
        this.topicKafka = normalize(this.topicKafka);
        this.eventType = normalize(this.eventType);
        this.idempotencyKey = normalize(this.idempotencyKey);
        this.correlationId = normalizeNullable(this.correlationId);
        if (this.schemaVersion < 1) {
            this.schemaVersion = 1L;
        }
        if (this.prioridade < 0) {
            this.prioridade = 0;
        }
        if (this.tentativas < 0) {
            this.tentativas = 0;
        }
        if (this.proximaTentativaEm == null) {
            this.proximaTentativaEm = Instant.now();
        }
        if (this.status == null) {
            this.status = StatusEventoOutboxFederacao.PENDENTE;
        }
        Instant agora = Instant.now();
        if (this.criadoEm == null) {
            this.criadoEm = agora;
        }
        this.atualizadoEm = agora;
    }

    public void marcarProcessando() {
        this.status = StatusEventoOutboxFederacao.PROCESSANDO;
        this.proximaTentativaEm = Instant.now();
    }

    public void marcarPublicado() {
        this.status = StatusEventoOutboxFederacao.PUBLICADO;
        this.publicadoEm = Instant.now();
        this.ultimoErro = null;
    }

    public void marcarFalha(String erro) {
        this.tentativas = this.tentativas + 1;
        this.ultimoErro = erro;
        long backoff = Math.min(300L, (long) Math.pow(2, Math.min(this.tentativas, 7)) * 5L);
        this.proximaTentativaEm = Instant.now().plusSeconds(backoff);
        this.status = this.tentativas >= 6 ? StatusEventoOutboxFederacao.FALHA_PERMANENTE : StatusEventoOutboxFederacao.PENDENTE;
    }

    private static String normalize(String value) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            throw new IllegalArgumentException("valor obrigatorio");
        }
        return normalized;
    }

    private static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    public UUID getId() {
        return id;
    }

    public String getTribunalCodigo() {
        return tribunalCodigo;
    }

    public String getTopicKafka() {
        return topicKafka;
    }

    public String getEventType() {
        return eventType;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public String getPayloadHash() {
        return payloadHash;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public long getSchemaVersion() {
        return schemaVersion;
    }

    public int getTentativas() {
        return tentativas;
    }

    public int getPrioridade() {
        return prioridade;
    }

    public Instant getProximaTentativaEm() {
        return proximaTentativaEm;
    }

    public Instant getPublicadoEm() {
        return publicadoEm;
    }

    public StatusEventoOutboxFederacao getStatus() {
        return status;
    }

    public String getUltimoErro() {
        return ultimoErro;
    }
}
