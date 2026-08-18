package com.tcc.pjb.backend.model.entity.protocolo;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;
import com.tcc.pjb.backend.model.entity.enums.processual.completude.OrigemValidacao;
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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
@Table(name = "tb_protocolo_validacao_historico",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_validacao_historico_uuid", columnNames = {"uuid"})
        },
        indexes = {
                @Index(name = "idx_validacao_historico_protocolo", columnList = "protocolo_id,executado_em")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProtocoloValidacaoHistorico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid", nullable = false, updatable = false)
    private UUID uuid;

    @Column(name = "protocolo_id", nullable = false)
    private Long protocoloId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_resultante", nullable = false, length = 40)
    private ProtocoloCompletudeStatus statusResultante;

    @Column(name = "versao_regra_aplicada", nullable = false, length = 40)
    private String versaoRegraAplicada;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "violacoes_json", nullable = false, columnDefinition = "jsonb")
    private String violacoesJson;

    @Column(name = "documentos_hash", nullable = false, length = 64)
    private String documentosHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "origem_validacao", nullable = false, length = 20)
    private OrigemValidacao origemValidacao;

    @Column(name = "executado_por")
    private Long executadoPor;

    @Column(name = "executado_em", nullable = false, updatable = false)
    private OffsetDateTime executadoEm;

    @PrePersist
    void prePersist() {
        if (executadoEm == null) executadoEm = OffsetDateTime.now();
    }
}
