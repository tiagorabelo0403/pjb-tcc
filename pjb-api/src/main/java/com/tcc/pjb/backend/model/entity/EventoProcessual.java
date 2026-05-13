package com.tcc.pjb.backend.model.entity;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.ColumnDefault;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import com.tcc.pjb.backend.model.entity.enums.StatusEvento;
import com.tcc.pjb.backend.model.entity.enums.TipoEvento;
import lombok.*;

@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@PjbDataOwnership(module = PjbModuleId.PROCESSO_LIFECYCLE, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(
        name = "evento_processual",
        indexes = {
                @Index(name = "idx_evento_processo", columnList = "processo_id"),
                @Index(name = "idx_evento_responsavel_status", columnList = "responsavel_id, status"),
                
                @Index(name = "idx_evento_datas", columnList = "data_inicio, data_fim")
        }
)
@EntityListeners(AuditingEntityListener.class)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"processo", "responsavel"})
public class EventoProcessual implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processo_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_evento_processo"))
    private Processo processo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsavel_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_evento_responsavel"))
    private Usuario responsavel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoEvento tipo;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @ColumnDefault("'PENDENTE'")
    private StatusEvento status = StatusEvento.PENDENTE;

    @NotBlank
    @Column(nullable = false, length = 200)
    private String titulo;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String descricao;

    @FutureOrPresent
    @Column(name = "data_inicio", nullable = false)
    private LocalDateTime dataInicio;

    @Future
    @Column(name = "data_fim", nullable = false)
    private LocalDateTime dataFim;

    @CreatedDate
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @LastModifiedDate
    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @Version
    private Long versao;

    @Column(nullable = false)
    @Builder.Default
    private Boolean ativo = true; 

    @PrePersist
    public void prePersist() {
        validarDatas();
    }

    @PreUpdate
    public void preUpdate() {
        validarDatas();
    }

    private void validarDatas() {
        if (this.dataFim != null && this.dataInicio != null && this.dataFim.isBefore(this.dataInicio)) {
            this.dataFim = this.dataInicio.plusHours(1);
        }
    }

    
    
    

    public boolean isEventoEmAndamento() {
        return status == StatusEvento.PENDENTE && LocalDateTime.now().isBefore(dataFim);
    }

    public boolean isEventoFinalizado() {
        return status == StatusEvento.CONCLUIDO || LocalDateTime.now().isAfter(dataFim);
    }

    public long getDuracaoHoras() {
        if (dataInicio != null && dataFim != null) {
            return Duration.between(dataInicio, dataFim).toHours();
        }
        return 0;
    }

    public boolean isAtrasado() {
        return status == StatusEvento.PENDENTE && LocalDateTime.now().isAfter(dataFim);
    }
}