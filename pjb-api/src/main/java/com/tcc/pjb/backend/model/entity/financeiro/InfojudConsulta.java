package com.tcc.pjb.backend.model.entity.financeiro;

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
@Table(name = "pjb_infojud_consulta", indexes = {
        @Index(name = "idx_infojud_processo", columnList = "processo_id"),
        @Index(name = "idx_infojud_status", columnList = "status")
})
@PjbDataOwnership(module = PjbModuleId.PROCESSO_LIFECYCLE, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InfojudConsulta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "processo_id", nullable = false)
    private Long processoId;

    @Column(name = "cpf_cnpj_consultado", nullable = false, length = 14)
    private String cpfCnpjConsultado;

    @Column(name = "protocolo_receita", length = 128)
    private String protocoloReceita;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "resumo_retorno", length = 12000)
    private String resumoRetorno;

    @Column(name = "tentativas", nullable = false)
    private int tentativas;

    @Column(name = "proximo_retry_em")
    private Instant proximoRetryEm;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "confirmado_em")
    private Instant confirmadoEm;

    @Column(name = "operador_id")
    private Long operadorId;

    @Column(name = "authz_trail_id", length = 128)
    private String authzTrailId;
}
