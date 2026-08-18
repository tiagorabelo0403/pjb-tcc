package com.tcc.pjb.backend.model.entity.protocolo;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;
import com.tcc.pjb.backend.model.entity.enums.processual.completude.ProtocoloCompletudeStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@PjbDataOwnership(module = PjbModuleId.AJUIZAMENTO, mode = PjbOwnershipMode.OWNER_ONLY)
@Entity
@Table(name = "tb_protocolo_pendencia",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_protocolo_pendencia_uuid", columnNames = {"uuid"})
        },
        indexes = {
                @Index(name = "idx_pendencia_protocolo_status", columnList = "protocolo_id,status")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProtocoloPendencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid", nullable = false, updatable = false)
    private UUID uuid;

    @Column(name = "protocolo_id", nullable = false)
    private Long protocoloId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private ProtocoloCompletudeStatus status;

    @Column(name = "prazo_regularizacao")
    private LocalDate prazoRegularizacao;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "violacoes_json", nullable = false, columnDefinition = "jsonb")
    private String violacoesJson;

    @Column(name = "requisitos_versao", nullable = false, length = 40)
    private String requisitosVersao;

    @Column(name = "documentos_hash", nullable = false, length = 64)
    private String documentosHash;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "dispensado_por")
    private Long dispensadoPor;

    @Column(name = "dispensa_justificativa", length = 1000)
    private String dispensaJustificativa;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;

    @PrePersist
    void prePersist() {
        OffsetDateTime agora = OffsetDateTime.now();
        if (criadoEm == null) criadoEm = agora;
        if (atualizadoEm == null) atualizadoEm = agora;
    }

    @PreUpdate
    void preUpdate() {
        atualizadoEm = OffsetDateTime.now();
    }
}
