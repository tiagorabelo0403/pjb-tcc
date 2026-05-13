package com.tcc.pjb.backend.modules.advocacia.office.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.tcc.pjb.backend.model.entity.Equipe;
import com.tcc.pjb.backend.model.entity.Usuario;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "adv_office_delegacao_usage",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_adv_office_usage", columnNames = {"equipe_id", "usuario_id", "dia"})
        },
        indexes = {
                @Index(name = "ix_adv_office_usage_equipe_dia", columnList = "equipe_id, dia"),
                @Index(name = "ix_adv_office_usage_usuario_dia", columnList = "usuario_id, dia")
        }
)
@Getter
@Setter
public class EquipeOfficeDelegacaoUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipe_id", nullable = false, foreignKey = @ForeignKey(name = "fk_adv_office_usage_equipe"))
    private Equipe equipe;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, foreignKey = @ForeignKey(name = "fk_adv_office_usage_usuario"))
    private Usuario usuario;

    @Column(name = "dia", nullable = false)
    private LocalDate dia;

    @Column(name = "auto_usado", nullable = false)
    private int autoUsado = 0;

    @Column(name = "queue_criado", nullable = false)
    private int queueCriado = 0;

    @Column(name = "ultimo_evento_em")
    private LocalDateTime ultimoEventoEm;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
