package com.tcc.pjb.backend.modules.acordo.infrastructure.persistence;

import com.tcc.pjb.backend.modules.acordo.domain.AcordoConfidencialidadeNivel;
import com.tcc.pjb.backend.modules.acordo.domain.AcordoSessaoStatus;
import com.tcc.pjb.backend.modules.acordo.domain.AcordoTipoSala;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "tb_sessao_acordo_processual",
        indexes = {
                @Index(name = "idx_sap_processo", columnList = "processo_id"),
                @Index(name = "idx_sap_status_expira", columnList = "status, expira_em")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class AcordoSessaoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "processo_id", nullable = false)
    private Long processoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_sala", nullable = false, length = 40)
    private AcordoTipoSala tipoSala;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private AcordoSessaoStatus status;

    @Column(name = "aberta_por_id", nullable = false)
    private Long abertaPorId;

    @Column(name = "aberta_em", nullable = false)
    private Instant abertaEm;

    @Column(name = "expira_em", nullable = false)
    private Instant expiraEm;

    @Column(name = "motivo_abertura", nullable = false, length = 1000)
    private String motivoAbertura;

    @Column(name = "segredo_justica", nullable = false)
    private boolean segredoJustica;

    @Enumerated(EnumType.STRING)
    @Column(name = "confidencialidade_nivel", nullable = false, length = 40)
    private AcordoConfidencialidadeNivel confidencialidadeNivel;

    @Column(name = "cejusc_referenciado", nullable = false)
    private boolean cejuscReferenciado;

    @Column(name = "homologado_em")
    private Instant homologadoEm;

    @Column(name = "homologado_por_id")
    private Long homologadoPorId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
