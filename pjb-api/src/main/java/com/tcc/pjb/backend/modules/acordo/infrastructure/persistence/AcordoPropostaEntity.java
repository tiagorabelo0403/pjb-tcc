package com.tcc.pjb.backend.modules.acordo.infrastructure.persistence;

import com.tcc.pjb.backend.modules.acordo.domain.AcordoPropostaStatus;
import com.tcc.pjb.backend.modules.acordo.domain.AcordoPropostaTipo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "tb_acordo_proposta",
        indexes = {
                @Index(name = "idx_acordo_prop_sessao_status", columnList = "sessao_id, status"),
                @Index(name = "idx_acordo_prop_validade", columnList = "validade_ate")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class AcordoPropostaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sessao_id", nullable = false)
    private Long sessaoId;

    @Column(name = "autor_id", nullable = false)
    private Long autorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 30)
    private AcordoPropostaTipo tipo;

    @Column(name = "valor", precision = 19, scale = 2)
    private BigDecimal valor;

    @Column(name = "termos_json", nullable = false, columnDefinition = "jsonb")
    private String termosJson;

    @Column(name = "validade_ate", nullable = false)
    private Instant validadeAte;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private AcordoPropostaStatus status;

    @Column(name = "criada_por_ia", nullable = false)
    private boolean criadaPorIa;

    @Column(name = "revisada_por_humano", nullable = false)
    private boolean revisadaPorHumano;

    @Column(name = "revisada_por_id")
    private Long revisadaPorId;

    @Column(name = "revisada_em")
    private Instant revisadaEm;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
