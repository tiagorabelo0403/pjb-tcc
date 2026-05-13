package com.tcc.pjb.backend.model.entity;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import com.tcc.pjb.backend.model.dto.IaSettings;
import com.tcc.pjb.backend.model.entity.enums.StatusAcordo;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true) 
@PjbDataOwnership(module = PjbModuleId.PROCESSO_LIFECYCLE, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "propostas_acordo", indexes = {
        @Index(name = "idx_proposta_uuid", columnList = "uuid")
})
@EntityListeners(AuditingEntityListener.class)
public class PropostaAcordo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID uuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processo_id", nullable = false)
    private Processo processo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proponente_id", nullable = false)
    private Usuario proponente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipe_id", nullable = false)
    private Equipe equipe;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String termosHtml;

    @Column(name = "valor_acordo", precision = 19, scale = 2)
    private BigDecimal valorAcordo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusAcordo status;

    @Column(name = "hash_assinatura_parte1")
    private String hashAssinaturaParte1;

    private LocalDateTime dataAssinaturaParte1;

    @Column(name = "hash_assinatura_parte2")
    private String hashAssinaturaParte2;

    private LocalDateTime dataAssinaturaParte2;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime dataAtualizacao;

    @Column(name = "ia_run_id")
    private UUID iarunId;

    @Column(name = "aprovado_por_advogado_id")
    private Long aprovadoPor;

    private String justificativaAprovacao;

    private LocalDateTime dataAprovacao;

    @Column(name = "ia_profile_id")
    private Long profileId;

    @Column(name = "ia_settings", length = 1000)
    @Convert(converter = IaSettings.IaSettingsConverter.class)
    private IaSettings settings;

    @PrePersist
    protected void prePersist() {
        if (this.uuid == null) {
            this.uuid = UUID.randomUUID();
        }
        if (this.status == null) {
            this.status = StatusAcordo.EM_NEGOCIACAO;
        }
    }
}
