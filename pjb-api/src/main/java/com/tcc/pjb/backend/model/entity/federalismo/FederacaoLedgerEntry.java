package com.tcc.pjb.backend.model.entity.federalismo;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.Instant;
import java.util.Objects;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@PjbDataOwnership(module = PjbModuleId.INTEGRACOES_EXTERNAS, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(
        name = "tb_federacao_ledger_entry",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_federacao_ledger_hash", columnNames = "hash_entrada"),
                @UniqueConstraint(name = "uk_federacao_ledger_idempotency", columnNames = "idempotency_key")
        },
        indexes = {
                @Index(name = "idx_federacao_ledger_seq_global", columnList = "sequencia_global"),
                @Index(name = "idx_federacao_ledger_nupn", columnList = "nupn"),
                @Index(name = "idx_federacao_ledger_tribunal_seq", columnList = "tribunal_codigo, sequencia_tribunal"),
                @Index(name = "idx_federacao_ledger_evento", columnList = "tipo_evento, topic_kafka, ocorrido_em")
        }
)
public class FederacaoLedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sequencia_global", nullable = false)
    private long sequenciaGlobal;

    @Column(name = "sequencia_tribunal", nullable = false)
    private long sequenciaTribunal;

    @Column(name = "hash_entrada", nullable = false, length = 64)
    private String hashEntrada;

    @Column(name = "hash_anterior", nullable = false, length = 64)
    private String hashAnterior;

    @Column(name = "tribunal_codigo", nullable = false, length = 20)
    private String tribunalCodigo;

    @Column(name = "tipo_evento", nullable = false, length = 120)
    private String tipoEvento;

    @Column(name = "topic_kafka", nullable = false, length = 180)
    private String topicKafka;

    @Column(name = "nupn", length = 50)
    private String nupn;

    @Column(name = "payload_hash", nullable = false, length = 64)
    private String payloadHash;

    @Column(name = "operador_id", length = 120)
    private String operadorId;

    @Column(name = "schema_version", nullable = false)
    private long schemaVersion;

    @Column(name = "correlation_id", length = 120)
    private String correlationId;

    @Column(name = "idempotency_key", nullable = false, length = 180)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_assinatura", nullable = false, length = 20)
    private StatusAssinaturaFederacao statusAssinatura;

    @Enumerated(EnumType.STRING)
    @Column(name = "classificacao_conflito", nullable = false, length = 20)
    private ClassificacaoConflitoFederacao classificacaoConflito;

    @Column(name = "tamanho_payload_bytes", nullable = false)
    private int tamanhoPayloadBytes;

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;

    @Column(name = "ocorrido_em", nullable = false)
    private Instant ocorridoEm;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected FederacaoLedgerEntry() {
    }

    public FederacaoLedgerEntry(long sequenciaGlobal,
                                long sequenciaTribunal,
                                String hashEntrada,
                                String hashAnterior,
                                String tribunalCodigo,
                                String tipoEvento,
                                String topicKafka,
                                String nupn,
                                String payloadHash,
                                String operadorId,
                                long schemaVersion,
                                String correlationId,
                                String idempotencyKey,
                                StatusAssinaturaFederacao statusAssinatura,
                                ClassificacaoConflitoFederacao classificacaoConflito,
                                int tamanhoPayloadBytes,
                                String metadataJson,
                                Instant ocorridoEm) {
        this.sequenciaGlobal = sequenciaGlobal;
        this.sequenciaTribunal = sequenciaTribunal;
        this.hashEntrada = Objects.requireNonNull(hashEntrada);
        this.hashAnterior = Objects.requireNonNull(hashAnterior);
        this.tribunalCodigo = Objects.requireNonNull(tribunalCodigo);
        this.tipoEvento = Objects.requireNonNull(tipoEvento);
        this.topicKafka = Objects.requireNonNull(topicKafka);
        this.nupn = nupn;
        this.payloadHash = Objects.requireNonNull(payloadHash);
        this.operadorId = operadorId;
        this.schemaVersion = Math.max(1L, schemaVersion);
        this.correlationId = correlationId;
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey);
        this.statusAssinatura = Objects.requireNonNull(statusAssinatura);
        this.classificacaoConflito = Objects.requireNonNull(classificacaoConflito);
        this.tamanhoPayloadBytes = Math.max(0, tamanhoPayloadBytes);
        this.metadataJson = metadataJson;
        this.ocorridoEm = Objects.requireNonNull(ocorridoEm);
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        this.tribunalCodigo = normalize(this.tribunalCodigo);
        this.tipoEvento = normalize(this.tipoEvento);
        this.topicKafka = normalize(this.topicKafka);
        this.nupn = normalizeNullable(this.nupn);
        this.operadorId = normalizeNullable(this.operadorId);
        this.correlationId = normalizeNullable(this.correlationId);
        this.idempotencyKey = normalize(this.idempotencyKey);
        if (this.schemaVersion < 1) {
            this.schemaVersion = 1L;
        }
        if (this.statusAssinatura == null) {
            this.statusAssinatura = StatusAssinaturaFederacao.PENDENTE;
        }
        if (this.classificacaoConflito == null) {
            this.classificacaoConflito = ClassificacaoConflitoFederacao.NENHUM;
        }
        Instant agora = Instant.now();
        if (this.criadoEm == null) {
            this.criadoEm = agora;
        }
        this.atualizadoEm = agora;
    }

    public boolean consistenteComAnterior(FederacaoLedgerEntry anterior) {
        if (anterior == null) {
            return this.sequenciaGlobal == 0L && "0".repeat(64).equals(this.hashAnterior);
        }
        return Objects.equals(this.hashAnterior, anterior.getHashEntrada())
                && this.sequenciaGlobal == anterior.getSequenciaGlobal() + 1L;
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

    public Long getId() {
        return id;
    }

    public long getSequenciaGlobal() {
        return sequenciaGlobal;
    }

    public long getSequenciaTribunal() {
        return sequenciaTribunal;
    }

    public String getHashEntrada() {
        return hashEntrada;
    }

    public String getHashAnterior() {
        return hashAnterior;
    }

    public String getTribunalCodigo() {
        return tribunalCodigo;
    }

    public String getTipoEvento() {
        return tipoEvento;
    }

    public String getTopicKafka() {
        return topicKafka;
    }

    public String getNupn() {
        return nupn;
    }

    public String getPayloadHash() {
        return payloadHash;
    }

    public String getOperadorId() {
        return operadorId;
    }

    public long getSchemaVersion() {
        return schemaVersion;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public StatusAssinaturaFederacao getStatusAssinatura() {
        return statusAssinatura;
    }

    public ClassificacaoConflitoFederacao getClassificacaoConflito() {
        return classificacaoConflito;
    }

    public int getTamanhoPayloadBytes() {
        return tamanhoPayloadBytes;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public Instant getOcorridoEm() {
        return ocorridoEm;
    }
}
