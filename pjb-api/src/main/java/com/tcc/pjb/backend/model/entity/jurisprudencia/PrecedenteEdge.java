package com.tcc.pjb.backend.model.entity.jurisprudencia;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;

@PjbDataOwnership(module = PjbModuleId.INTELIGENCIA_APLICADA, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tb_precedente_edge")
public class PrecedenteEdge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_precedente_id", nullable = false)
    private Precedente fromPrecedente;

    @Column(name = "relation", nullable = false, length = 30)
    private String relation;

    @Column(name = "target_type", nullable = false, length = 40)
    private String targetType;

    @Column(name = "target_ref", nullable = false, length = 220)
    private String targetRef;

    @Column(name = "raw", length = 260)
    private String raw;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
