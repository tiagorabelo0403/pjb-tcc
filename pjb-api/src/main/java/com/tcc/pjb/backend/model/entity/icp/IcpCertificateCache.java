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
@Table(name = "pjb_icp_certificate_cache", indexes = {
        @Index(name = "idx_icp_cert_cpf", columnList = "cpf_titular"),
        @Index(name = "idx_icp_cert_cnpj", columnList = "cnpj_titular"),
        @Index(name = "idx_icp_cert_valid", columnList = "valid_until")
})
@PjbDataOwnership(module = PjbModuleId.PROCESSO_LIFECYCLE, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IcpCertificateCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "subject_dn", nullable = false, length = 4000)
    private String subjectDn;

    @Column(name = "issuer_dn", nullable = false, length = 4000)
    private String issuerDn;

    @Column(name = "serial_hex", nullable = false, length = 128)
    private String serialHex;

    @Column(name = "cpf_titular", length = 11)
    private String cpfTitular;

    @Column(name = "cnpj_titular", length = 14)
    private String cnpjTitular;

    @Column(name = "cert_type", nullable = false, length = 8)
    private String certType;

    @Column(name = "ac_sigla", length = 64)
    private String acSigla;

    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;

    @Column(name = "valid_until", nullable = false)
    private Instant validUntil;

    @Column(name = "revoked", nullable = false)
    private boolean revoked;

    @Column(name = "revocation_checked")
    private Instant revocationChecked;

    @Column(name = "raw_der", columnDefinition = "bytea")
    private byte[] rawDer;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
