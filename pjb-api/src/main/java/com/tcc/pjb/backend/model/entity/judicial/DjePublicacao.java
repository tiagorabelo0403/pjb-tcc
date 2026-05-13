package com.tcc.pjb.backend.model.entity.judicial;

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
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "pjb_dje_publicacao", indexes = {
        @Index(name = "idx_dje_processo", columnList = "processo_id"),
        @Index(name = "idx_dje_status", columnList = "status,data_disponibilizacao")
})
@PjbDataOwnership(module = PjbModuleId.PROCESSO_LIFECYCLE, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DjePublicacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "processo_id", nullable = false)
    private Long processoId;

    @Column(name = "tipo_ato", nullable = false, length = 64)
    private String tipoAto;

    @Column(name = "conteudo_hash", nullable = false, length = 128)
    private String conteudoHash;

    @Column(name = "edicao_dje", length = 32)
    private String edicaoDje;

    @Column(name = "data_disponibilizacao")
    private LocalDate dataDisponibilizacao;

    @Column(name = "data_publicacao")
    private LocalDate dataPublicacao;

    @Column(name = "prazo_comeca_em")
    private LocalDate prazoComecaEm;

    @Column(name = "tribunal_codigo", length = 32)
    private String tribunalCodigo;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "enviado_em")
    private Instant enviadoEm;

    @Column(name = "publicado_em")
    private Instant publicadoEm;

    @Column(name = "failure_reason", length = 4000)
    private String failureReason;

    @Column(name = "partes_notificadas", nullable = false)
    private boolean partesNotificadas;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
