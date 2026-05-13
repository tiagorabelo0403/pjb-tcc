package com.tcc.pjb.backend.modules.advocacia.office.entity;

import com.tcc.pjb.backend.model.entity.Equipe;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeActionType;
import com.tcc.pjb.backend.modules.advocacia.office.util.OfficeActionSetConverter;
import com.tcc.pjb.backend.modules.advocacia.office.util.RamoDireitoSetConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(
        name = "adv_office_delegacao_regra",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_adv_office_delegacao_regra", columnNames = {"equipe_id", "usuario_id"})
        },
        indexes = {
                @Index(name = "ix_adv_office_regra_equipe", columnList = "equipe_id"),
                @Index(name = "ix_adv_office_regra_usuario", columnList = "usuario_id")
        }
)
@Getter
@Setter
public class EquipeOfficeDelegacaoRegra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipe_id", nullable = false, foreignKey = @ForeignKey(name = "fk_adv_office_regra_equipe"))
    private Equipe equipe;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, foreignKey = @ForeignKey(name = "fk_adv_office_regra_usuario"))
    private Usuario usuario;

    @Column(name = "ativo", nullable = false)
    private boolean ativo = true;

    @Column(name = "bloqueia_pessoal", nullable = false)
    private boolean bloqueiaPessoal;

    @Column(name = "min_trust_auto_override")
    private Integer minTrustAutoOverride;

    @Column(name = "max_auto_por_dia_override")
    private Integer maxAutoPorDiaOverride;

    @Convert(converter = RamoDireitoSetConverter.class)
    @Column(name = "allowed_ramos_override", nullable = false, length = 1200)
    private Set<RamoDireito> allowedRamosOverride = EnumSet.noneOf(RamoDireito.class);

    @Convert(converter = OfficeActionSetConverter.class)
    @Column(name = "auto_actions_override", nullable = false, length = 1200)
    private Set<OfficeActionType> autoActionsOverride = EnumSet.noneOf(OfficeActionType.class);

    @Column(name = "workspace_priority", nullable = false)
    private int workspacePriority = 100;

    @Column(name = "auto_activate_workspace", nullable = false)
    private boolean autoActivateWorkspace;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
