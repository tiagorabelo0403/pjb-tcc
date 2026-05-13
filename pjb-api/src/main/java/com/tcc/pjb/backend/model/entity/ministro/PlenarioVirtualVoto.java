package com.tcc.pjb.backend.model.entity.ministro;

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
import jakarta.persistence.UniqueConstraint;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import com.tcc.pjb.backend.model.entity.Usuario;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@PjbDataOwnership(module = PjbModuleId.PROCESSO_LIFECYCLE, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_plenario_virtual_voto",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_plenario_virtual_voto_sessao_ministro", columnNames = {"sessao_id", "ministro_id"})
        },
        indexes = {
                @Index(name = "idx_plenario_virtual_voto_sessao", columnList = "sessao_id"),
                @Index(name = "idx_plenario_virtual_voto_ministro", columnList = "ministro_id")
        })
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class PlenarioVirtualVoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sessao_id", nullable = false)
    private PlenarioVirtualSessao sessao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ministro_id", nullable = false)
    private Usuario ministro;

    @Column(name = "commitment_hash", nullable = false, length = 128)
    private String commitmentHash;

    @Column(name = "receipt_hash", nullable = false, length = 128)
    private String receiptHash;

    @Column(name = "envelope_base64", nullable = false, columnDefinition = "TEXT")
    private String envelopeBase64;

    @Column(name = "prova_integridade", nullable = false, columnDefinition = "TEXT")
    private String provaIntegridade;

    @Column(name = "status", nullable = false, length = 40)
    private String status;

    @Column(name = "fundamentacao_resumo", columnDefinition = "TEXT")
    private String fundamentacaoResumo;

    @Column(name = "ressalva", columnDefinition = "TEXT")
    private String ressalva;

    @Column(name = "homomorphic_commitment", length = 128)
    private String homomorphicCommitment;

    @Column(name = "homomorphic_tally_blob", columnDefinition = "TEXT")
    private String homomorphicTallyBlob;

    @Column(name = "zk_proof_hash", length = 128)
    private String zkProofHash;

    @Column(name = "sigilo_ate_proclamacao", nullable = false)
    private boolean sigiloAteProclamacao;

    @Column(name = "revelado_em")
    private Instant reveladoEm;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        if (status == null || status.isBlank()) {
            status = "RECEBIDO";
        }
    }
}
