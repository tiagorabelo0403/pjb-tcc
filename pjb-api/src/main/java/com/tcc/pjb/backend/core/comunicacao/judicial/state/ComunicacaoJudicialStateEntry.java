package com.tcc.pjb.backend.core.comunicacao.judicial.state;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(
        name = "tb_comunicacao_judicial_state",
        indexes = {
                @Index(name = "idx_com_jud_state_domain_key", columnList = "domain_name,state_key", unique = true),
                @Index(name = "idx_com_jud_state_domain_secondary", columnList = "domain_name,secondary_key"),
                @Index(name = "idx_com_jud_state_processo", columnList = "processo_id"),
                @Index(name = "idx_com_jud_state_expedicao", columnList = "expedicao_uuid"),
                @Index(name = "idx_com_jud_state_mandado", columnList = "mandado_id")
        }
)
public class ComunicacaoJudicialStateEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "domain_name", nullable = false, length = 80)
    private String domainName;

    @Column(name = "state_key", nullable = false, length = 180)
    private String stateKey;

    @Column(name = "secondary_key", length = 180)
    private String secondaryKey;

    @Column(name = "processo_id")
    private Long processoId;

    @Column(name = "expedicao_uuid", length = 36)
    private String expedicaoUuid;

    @Column(name = "mandado_id", length = 120)
    private String mandadoId;

    @Column(name = "status_code", length = 80)
    private String statusCode;

    @Column(name = "payload_json", nullable = false, columnDefinition = "TEXT")
    private String payloadJson;

    @Column(name = "hash_integridade", nullable = false, length = 64)
    private String hashIntegridade;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    protected ComunicacaoJudicialStateEntry() {
    }

    public Long getId() {
        return id;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = trim(domainName);
    }

    public String getStateKey() {
        return stateKey;
    }

    public void setStateKey(String stateKey) {
        this.stateKey = trim(stateKey);
    }

    public String getSecondaryKey() {
        return secondaryKey;
    }

    public void setSecondaryKey(String secondaryKey) {
        this.secondaryKey = trim(secondaryKey);
    }

    public Long getProcessoId() {
        return processoId;
    }

    public void setProcessoId(Long processoId) {
        this.processoId = processoId;
    }

    public String getExpedicaoUuid() {
        return expedicaoUuid;
    }

    public void setExpedicaoUuid(String expedicaoUuid) {
        this.expedicaoUuid = trim(expedicaoUuid);
    }

    public String getMandadoId() {
        return mandadoId;
    }

    public void setMandadoId(String mandadoId) {
        this.mandadoId = trim(mandadoId);
    }

    public String getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(String statusCode) {
        this.statusCode = trim(statusCode);
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public void setPayloadJson(String payloadJson) {
        this.payloadJson = trim(payloadJson);
    }

    public String getHashIntegridade() {
        return hashIntegridade;
    }

    public void setHashIntegridade(String hashIntegridade) {
        this.hashIntegridade = trim(hashIntegridade);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        domainName = trim(domainName);
        stateKey = trim(stateKey);
        secondaryKey = trim(secondaryKey);
        expedicaoUuid = trim(expedicaoUuid);
        mandadoId = trim(mandadoId);
        statusCode = trim(statusCode);
        payloadJson = trim(payloadJson);
        hashIntegridade = trim(hashIntegridade);
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
        domainName = trim(domainName);
        stateKey = trim(stateKey);
        secondaryKey = trim(secondaryKey);
        expedicaoUuid = trim(expedicaoUuid);
        mandadoId = trim(mandadoId);
        statusCode = trim(statusCode);
        payloadJson = trim(payloadJson);
        hashIntegridade = trim(hashIntegridade);
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
