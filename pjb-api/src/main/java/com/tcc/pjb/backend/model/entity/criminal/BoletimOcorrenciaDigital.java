package com.tcc.pjb.backend.model.entity.criminal;

import com.tcc.pjb.backend.core.id.PjbUuidV7Generator;
import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;
import com.tcc.pjb.backend.model.entity.UnidadeInstituicao;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.StatusBoletimOcorrenciaDigital;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@PjbDataOwnership(module = PjbModuleId.PROCESSO_LIFECYCLE, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_boletim_ocorrencia_digital",
        indexes = {
                @Index(name = "idx_boletim_ocorrencia_numero", columnList = "numero_boletim", unique = true),
                @Index(name = "idx_boletim_ocorrencia_uuid", columnList = "uuid", unique = true),
                @Index(name = "idx_boletim_ocorrencia_unidade", columnList = "unidade_registro_id,status"),
                @Index(name = "idx_boletim_ocorrencia_registrado_por_jpa", columnList = "registrado_por_id")
        })
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class BoletimOcorrenciaDigital {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID uuid;

    @Column(name = "numero_boletim", nullable = false, unique = true, length = 80)
    private String numeroBoletim;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private StatusBoletimOcorrenciaDigital status;

    @Column(name = "natureza_fato", nullable = false, length = 180)
    private String naturezaFato;

    @Column(name = "resumo_fatos", nullable = false, columnDefinition = "TEXT")
    private String resumoFatos;

    @Column(name = "local_fato", nullable = false, length = 255)
    private String localFato;

    @Column(name = "ocorrido_em", nullable = false)
    private Instant ocorridoEm;

    @Column(name = "comunicante_resumo", nullable = false, columnDefinition = "TEXT")
    private String comunicanteResumo;

    @Column(name = "envolvidos_resumo", nullable = false, columnDefinition = "TEXT")
    private String envolvidosResumo;

    @Column(name = "providencias_iniciais", nullable = false, columnDefinition = "TEXT")
    private String providenciasIniciais;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unidade_registro_id", nullable = false)
    private UnidadeInstituicao unidadeRegistro;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registrado_por_id", nullable = false)
    private Usuario registradoPor;

    @Column(name = "cadeia_custodia_hash", nullable = false, length = 128)
    private String cadeiaCustodiaHash;

    @CreatedDate
    @Column(name = "registrado_em", nullable = false, updatable = false)
    private Instant registradoEm;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        if (uuid == null) {
            uuid = PjbUuidV7Generator.generate();
        }
        if (status == null) {
            status = StatusBoletimOcorrenciaDigital.REGISTRADO;
        }
        if (envolvidosResumo == null) {
            envolvidosResumo = "";
        }
        Instant now = Instant.now();
        if (registradoEm == null) {
            registradoEm = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }
}
