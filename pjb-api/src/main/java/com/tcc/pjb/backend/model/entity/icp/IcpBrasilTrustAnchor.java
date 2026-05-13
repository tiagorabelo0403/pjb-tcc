package com.tcc.pjb.backend.model.entity.icp;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "pjb_icp_trust_anchor", indexes = {
        @Index(name = "idx_icp_anchor_ativo", columnList = "ativo"),
        @Index(name = "idx_icp_anchor_sigla", columnList = "ac_sigla")
})
@PjbDataOwnership(module = PjbModuleId.PROCESSO_LIFECYCLE, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IcpBrasilTrustAnchor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "subject_dn", nullable = false, length = 4000)
    private String subjectDn;

    @Column(name = "issuer_dn", nullable = false, length = 4000)
    private String issuerDn;

    @Column(name = "serial_hex", nullable = false, length = 128)
    private String serialHex;

    @Column(name = "ac_sigla", nullable = false, length = 64)
    private String acSigla;

    @Column(name = "ativo", nullable = false)
    private boolean ativo;

    @Column(name = "certificado_der", nullable = false, columnDefinition = "bytea")
    private byte[] certificadoDer;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
