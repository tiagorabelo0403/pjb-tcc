package com.tcc.pjb.backend.modules.laiane.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import com.tcc.pjb.backend.model.entity.Usuario;
import lombok.*;

@Entity
@Table(name = "tb_laiane_tese", indexes = {
        @Index(name = "idx_laiane_tese_adv", columnList = "advogado_id")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaianeTese {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "advogado_id", nullable = false)
    private Usuario advogado;

    @Column(name = "area", length = 64)
    private String area;

    @Column(name = "titulo", nullable = false)
    private String titulo;

    @Column(name = "corpo", nullable = false, columnDefinition = "TEXT")
    private String corpo;

    @Column(name = "tags_json", columnDefinition = "TEXT")
    private String tagsJson;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
