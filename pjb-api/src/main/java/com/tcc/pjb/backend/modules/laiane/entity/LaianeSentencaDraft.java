package com.tcc.pjb.backend.modules.laiane.entity;

import java.time.LocalDateTime;
import java.util.UUID;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import com.tcc.pjb.backend.core.id.PjbUuidV7Generator;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.modules.laiane.model.LaianeSentencaStatus;
import lombok.*;

@Entity
@Table(name = "tb_laiane_sentenca_draft", indexes = {
        @Index(name = "idx_laiane_sentenca_processo_ts", columnList = "processo_id, created_at"),
        @Index(name = "idx_laiane_sentenca_processo_status", columnList = "processo_id, status"),
        @Index(name = "idx_laiane_sentenca_hash", columnList = "input_hash")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaianeSentencaDraft {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid", nullable = false, unique = true)
    @Builder.Default
    private UUID uuid = PjbUuidV7Generator.generate();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "processo_id", nullable = false)
    private Processo processo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "criado_por_usuario_id")
    private Usuario criadoPor;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 24, nullable = false)
    @Builder.Default
    private LaianeSentencaStatus status = LaianeSentencaStatus.DRAFT;

    @Column(name = "input_hash", length = 64, nullable = false)
    private String inputHash;

    @Column(name = "draft_markdown", nullable = false, columnDefinition = "TEXT")
    private String draftMarkdown;

    @Column(name = "context_json", columnDefinition = "TEXT")
    private String contextJson;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;
}
