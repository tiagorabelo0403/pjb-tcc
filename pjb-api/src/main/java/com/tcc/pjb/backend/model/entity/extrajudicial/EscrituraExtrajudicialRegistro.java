package com.tcc.pjb.backend.model.entity.extrajudicial;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.math.BigDecimal;
import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@PjbDataOwnership(module = PjbModuleId.PROCESSO_LIFECYCLE, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_escritura_extrajudicial_registro",
        indexes = {
                @Index(name = "idx_escritura_protocolo", columnList = "protocolo", unique = true),
                @Index(name = "idx_escritura_tipo_status", columnList = "tipo,status"),
                @Index(name = "idx_escritura_processo", columnList = "processo_vinculado_id")
        })
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class EscrituraExtrajudicialRegistro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "protocolo", nullable = false, length = 80, unique = true)
    private String protocolo;

    @Column(name = "tipo", nullable = false, length = 40)
    private String tipo;

    @Column(name = "status", nullable = false, length = 40)
    private String status;

    @Column(name = "ato_resumo", columnDefinition = "TEXT", nullable = false)
    private String atoResumo;

    @Column(name = "partes_resumo", columnDefinition = "TEXT", nullable = false)
    private String partesResumo;

    @Column(name = "bens_resumo", columnDefinition = "TEXT")
    private String bensResumo;

    @Column(name = "valor_declarado", precision = 19, scale = 2)
    private BigDecimal valorDeclarado;

    @Column(name = "comarca", length = 120)
    private String comarca;

    @Column(name = "uf", length = 2)
    private String uf;

    @Column(name = "assinatura_hash", length = 128)
    private String assinaturaHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cartorio_responsavel_id")
    private Usuario cartorioResponsavel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processo_vinculado_id")
    private Processo processoVinculado;

    @Column(name = "lavrada_em")
    private Instant lavradaEm;

    @Column(name = "vinculada_em")
    private Instant vinculadaEm;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        if (status == null || status.isBlank()) {
            status = "LAVRADA";
        }
        if (lavradaEm == null) {
            lavradaEm = Instant.now();
        }
    }
}
