package com.tcc.pjb.backend.modules.laiane.entity;

import java.time.LocalDateTime;
import java.util.UUID;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import com.tcc.pjb.backend.model.entity.Equipe;
import com.tcc.pjb.backend.model.entity.Usuario;
import lombok.*;

@Entity
@Table(name = "tb_laiane_protocol_package")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaianeProtocolPackage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "title", length = 220)
    private String title;

    
    @Column(name = "payload_json", columnDefinition = "TEXT", nullable = false)
    private String payloadJson;

    
    @Column(name = "integrity_hash", length = 64, nullable = false)
    private String integrityHash;

    
    @Column(name = "status", length = 30, nullable = false)
    @Builder.Default
    private String status = "DRAFT";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipe_id")
    private Equipe equipe;

    @Column(name = "executor_user_id")
    private Long executorUserId;

    @Column(name = "signer_user_id")
    private Long signerUserId;

    @Column(name = "office_queue_item_id")
    private Long officeQueueItemId;

    @Column(name = "submission_job_id")
    private UUID submissionJobId;

    @Column(name = "external_protocol_ref", length = 120)
    private String externalProtocolRef;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Lob
    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
