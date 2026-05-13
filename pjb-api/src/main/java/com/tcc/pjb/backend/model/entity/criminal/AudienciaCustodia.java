package com.tcc.pjb.backend.model.entity.criminal;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "pjb_audiencia_custodia", indexes = {
        @Index(name = "idx_custodia_status", columnList = "status,prazo_limite_24h"),
        @Index(name = "idx_custodia_processo", columnList = "processo_id")
})
@PjbDataOwnership(module = PjbModuleId.PROCESSO_LIFECYCLE, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AudienciaCustodia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "processo_id", nullable = false)
    private Long processoId;

    @Column(name = "preso_nome", nullable = false, length = 255)
    private String presoNome;

    @Column(name = "preso_cpf", length = 11)
    private String presoCpf;

    @Column(name = "data_prisao", nullable = false)
    private Instant dataPrisao;

    @Column(name = "prazo_limite_24h", nullable = false)
    private Instant prazoLimite24h;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "magistrado_id")
    private Long magistradoId;

    @Column(name = "realizada_em")
    private Instant realizadaEm;

    @Column(name = "resultado", length = 64)
    private String resultado;

    @Column(name = "medidas_cautelares", length = 4000)
    private String medidasCautelares;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
