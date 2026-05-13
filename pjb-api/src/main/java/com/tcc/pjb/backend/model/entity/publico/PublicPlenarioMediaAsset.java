package com.tcc.pjb.backend.model.entity.publico;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

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
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.julgamento.JulgamentoColegiado;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@PjbDataOwnership(module = PjbModuleId.PROCESSO_LIFECYCLE, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_public_plenario_media_asset",
        indexes = {
                @Index(name = "idx_public_plenario_media_sessao", columnList = "sessao_id"),
                @Index(name = "idx_public_plenario_media_tipo", columnList = "tipo"),
                @Index(name = "idx_public_plenario_media_publico", columnList = "publico")
        })
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class PublicPlenarioMediaAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sessao_id", nullable = false)
    private JulgamentoColegiado sessao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_id")
    private Usuario uploadedBy;

    @Column(name = "tipo", nullable = false, length = 80)
    private String tipo;

    @Column(name = "titulo", nullable = false, length = 180)
    private String titulo;

    @Column(name = "url_publica", nullable = false, length = 320)
    private String urlPublica;

    @Column(name = "hash_integridade", length = 128)
    private String hashIntegridade;

    @Column(name = "publico", nullable = false)
    private boolean publico;

    @Column(name = "ordem_exibicao")
    private Integer ordemExibicao;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (tipo == null || tipo.isBlank()) {
            tipo = "DOCUMENTO_PUBLICO";
        }
        if (ordemExibicao == null || ordemExibicao < 0) {
            ordemExibicao = 0;
        }
    }
}
