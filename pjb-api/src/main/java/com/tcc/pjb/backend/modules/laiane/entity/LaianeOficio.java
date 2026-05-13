package com.tcc.pjb.backend.modules.laiane.entity;

import java.time.LocalDateTime;
import java.util.UUID;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.modules.laiane.model.LaianeOficioStatus;
import lombok.*;

@Entity
@Table(name = "tb_laiane_oficio", indexes = {
        @Index(name = "idx_laiane_oficio_origem", columnList = "origem_id"),
        @Index(name = "idx_laiane_oficio_status", columnList = "status")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaianeOficio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tracking_code", nullable = false, unique = true)
    private UUID trackingCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "origem_id", nullable = false)
    private Usuario origem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destino_id")
    private Usuario destino;

    @Column(name = "tipo", length = 40, nullable = false)
    private String tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 40, nullable = false)
    @Builder.Default
    private LaianeOficioStatus status = LaianeOficioStatus.CRIADO;

    @Column(name = "protocolo", length = 64)
    private String protocolo;

    @Column(name = "assunto", length = 200)
    private String assunto;

    @Lob
    @Column(name = "conteudo", columnDefinition = "TEXT")
    private String conteudo;

    @Column(name = "enviado_em")
    private LocalDateTime enviadoEm;

    @Column(name = "entregue_em")
    private LocalDateTime entregueEm;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        if (trackingCode == null) trackingCode = UUID.randomUUID();
        if (status == null) status = LaianeOficioStatus.CRIADO;
    }
}
