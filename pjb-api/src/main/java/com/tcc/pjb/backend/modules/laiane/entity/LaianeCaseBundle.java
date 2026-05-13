package com.tcc.pjb.backend.modules.laiane.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.modules.laiane.model.LaianeCaseBundleStatus;
import lombok.*;

@Entity
@Table(name = "tb_laiane_case_bundle", indexes = {
        @Index(name = "idx_case_bundle_advogado", columnList = "advogado_id"),
        @Index(name = "idx_case_bundle_status", columnList = "status")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaianeCaseBundle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "advogado_id", nullable = false)
    private Usuario advogado;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 40, nullable = false)
    @Builder.Default
    private LaianeCaseBundleStatus status = LaianeCaseBundleStatus.ABERTO;

    @Lob
    @Column(name = "processos_json", nullable = false, length = 12000, columnDefinition = "TEXT")
    private String processosJson;

    @Column(name = "tese_id")
    private Long teseId;

    @Lob
    @Column(name = "descricao", columnDefinition = "TEXT")
    private String descricao;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        if (status == null) status = LaianeCaseBundleStatus.ABERTO;
    }
}
