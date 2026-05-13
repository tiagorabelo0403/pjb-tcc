package com.tcc.pjb.backend.model.entity;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import com.tcc.pjb.backend.model.entity.enums.StatusEventoInstitucional;
import com.tcc.pjb.backend.model.entity.enums.TipoEventoInstitucional;
import lombok.*;

@PjbDataOwnership(module = PjbModuleId.COMUNICACOES, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "evento_institucional",
        indexes = {
                @Index(name = "idx_evento_institucional_uf", columnList = "uf"),
                @Index(name = "idx_evento_institucional_status", columnList = "status"),
                @Index(name = "idx_evento_institucional_processo", columnList = "processo_id"),
                @Index(name = "idx_evento_institucional_orgao", columnList = "orgao"),
                @Index(name = "idx_evento_institucional_numproc", columnList = "numero_processo")
        })
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
public class EventoInstitucional {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 60)
    private TipoEventoInstitucional tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    @Builder.Default
    private StatusEventoInstitucional status = StatusEventoInstitucional.ABERTO;

    
    @Column(nullable = false, length = 2)
    private String uf;

    
    @Column(length = 40)
    private String tribunal;

    
    @Column(length = 120)
    private String orgao;

    
    @Column(name = "processo_id")
    private Long processoId;

    
    @Column(length = 40)
    private String numeroProcesso;

    
    @Column(nullable = false)
    @Builder.Default
    private int severidade = 3;

    @Column(nullable = false, length = 240)
    private String resumo;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String detalhes;

    
    @Column(name = "criado_por_usuario_id")
    private Long criadoPorUsuarioId;

    
    @Column(name = "criado_por", length = 160)
    private String criadoPor;

    @CreatedDate
    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;

    @LastModifiedDate
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    @OneToMany(mappedBy = "evento", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProvidenciaInstitucional> providencias = new ArrayList<>();

    public void addProvidencia(ProvidenciaInstitucional p) {
        if (p == null) return;
        p.setEvento(this);
        this.providencias.add(p);
    }
}
