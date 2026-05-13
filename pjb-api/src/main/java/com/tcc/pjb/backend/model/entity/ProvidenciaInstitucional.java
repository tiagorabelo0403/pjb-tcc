package com.tcc.pjb.backend.model.entity;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import com.tcc.pjb.backend.model.entity.enums.TipoProvidenciaInstitucional;
import lombok.*;

@PjbDataOwnership(module = PjbModuleId.PROCESSO_LIFECYCLE, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "providencia_institucional",
        indexes = {
                @Index(name = "idx_providencia_evento", columnList = "evento_id")
        })
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProvidenciaInstitucional {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "evento_id", nullable = false)
    private EventoInstitucional evento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TipoProvidenciaInstitucional tipo;

    @Column(nullable = false, length = 240)
    private String titulo;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Column(name = "criado_por_usuario_id")
    private Long criadoPorUsuarioId;

    @Column(name = "criado_por", length = 160)
    private String criadoPor;

    @CreatedDate
    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;
}
