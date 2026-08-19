package com.tcc.pjb.backend.modules.laiane.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.modules.laiane.model.LaianeProcuracaoStatus;
import lombok.*;

@Entity
@Table(name = "tb_laiane_procuracao", indexes = {
        @Index(name = "idx_laiane_procuracao_adv", columnList = "advogado_id"),
        @Index(name = "idx_laiane_procuracao_proc", columnList = "processo_id"),
        @Index(name = "idx_laiane_procuracao_status", columnList = "status")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaianeProcuracao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "advogado_id", nullable = false)
    private Usuario advogado;

    @Column(name = "cliente_id")
    private Long clienteId;

    @Column(name = "processo_id")
    private Long processoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private LaianeProcuracaoStatus status;

    @Column(name = "inicio_vigencia")
    private LocalDate inicioVigencia;

    @Column(name = "fim_vigencia")
    private LocalDate fimVigencia;

    @Column(name = "poderes", columnDefinition = "TEXT")
    private String poderes;

    @Column(name = "anexos_json", columnDefinition = "TEXT")
    private String anexosJson;

    
    @Column(name = "aprovado_por_id")
    private Long aprovadoPorId;

    @Column(name = "aprovado_em")
    private LocalDateTime aprovadoEm;

    @Column(name = "decisao_motivo", columnDefinition = "TEXT")
    private String decisaoMotivo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "substabelecido_de_id")
    private LaianeProcuracao substabelecidoDe;

    @Column(name = "com_reserva_de_poderes", nullable = false)
    @Builder.Default
    private boolean comReservaDePoderes = false;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
