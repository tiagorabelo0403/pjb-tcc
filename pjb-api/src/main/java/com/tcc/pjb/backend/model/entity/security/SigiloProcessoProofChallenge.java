package com.tcc.pjb.backend.model.entity.security;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@PjbDataOwnership(module = PjbModuleId.IDENTIDADE_SEGURANCA, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_sigilo_processo_proof_challenge",
        indexes = {
                @Index(name = "idx_sigilo_proof_challenge_id", columnList = "challenge_id", unique = true),
                @Index(name = "idx_sigilo_proof_challenge_processo", columnList = "processo_id,status"),
                @Index(name = "idx_sigilo_proof_challenge_user", columnList = "solicitante_id,status")
        })
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class SigiloProcessoProofChallenge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "challenge_id", nullable = false, unique = true, length = 120)
    private String challengeId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "processo_id", nullable = false)
    private Processo processo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitante_id")
    private Usuario solicitante;

    @Column(name = "escopo", nullable = false, length = 80)
    private String escopo;

    @Column(name = "statement", nullable = false, columnDefinition = "TEXT")
    private String statement;

    @Column(name = "challenge_payload", nullable = false, columnDefinition = "TEXT")
    private String challengePayload;

    @Column(name = "commitment_hash", nullable = false, length = 128)
    private String commitmentHash;

    @Column(name = "response_hash", length = 128)
    private String responseHash;

    @Column(name = "status", nullable = false, length = 40)
    private String status;

    @Column(name = "expira_em")
    private Instant expiraEm;

    @Column(name = "verificado_em")
    private Instant verificadoEm;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        if (status == null || status.isBlank()) {
            status = "EMITIDO";
        }
        if (expiraEm == null) {
            expiraEm = Instant.now().plusSeconds(300);
        }
    }
}
