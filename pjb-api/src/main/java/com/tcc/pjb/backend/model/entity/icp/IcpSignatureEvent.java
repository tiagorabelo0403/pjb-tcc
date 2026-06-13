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
@Table(name = "pjb_icp_signature_event", indexes = {
        @Index(name = "idx_icp_sig_evento_processo", columnList = "processo_id")
})
@PjbDataOwnership(module = PjbModuleId.PROCESSO_LIFECYCLE, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IcpSignatureEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "processo_id")
    private Long processoId;

    @Column(name = "doc_hash", nullable = false, length = 128)
    private String docHash;

    @Column(name = "cert_serial_hex", length = 128)
    private String certSerialHex;

    @Column(name = "profile_candidate", nullable = false, length = 8)
    private String profileCandidate;

    @Column(name = "profile_achieved", length = 8)
    private String profileAchieved;

    @Column(name = "ts_rfc3161_embedded", nullable = false)
    private boolean tsRfc3161Embedded;

    @Column(name = "dss_dict_present", nullable = false)
    private boolean dssDictPresent;

    @Column(name = "vri_present", nullable = false)
    private boolean vriPresent;

    @Column(name = "signed_at", nullable = false)
    private Instant signedAt;

    @Column(name = "validation_ok")
    private Boolean validationOk;

    @Column(name = "failure_reason", length = 4000)
    private String failureReason;

    public static IcpSignatureEvent failure(Long processoId, String docHash, String failureReason) {
        return IcpSignatureEvent.builder()
                .processoId(processoId)
                .docHash(docHash)
                .profileCandidate("LTA")
                .signedAt(Instant.now())
                .validationOk(false)
                .failureReason(failureReason)
                .build();
    }
}
