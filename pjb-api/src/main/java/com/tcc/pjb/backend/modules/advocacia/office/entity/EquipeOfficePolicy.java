package com.tcc.pjb.backend.modules.advocacia.office.entity;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;
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
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.tcc.pjb.backend.model.entity.Equipe;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeActionType;
import com.tcc.pjb.backend.modules.advocacia.office.util.OfficeActionSetConverter;
import com.tcc.pjb.backend.modules.advocacia.office.util.RamoDireitoSetConverter;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "adv_office_policy",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_adv_office_policy_equipe", columnNames = {"equipe_id"})
        },
        indexes = {
                @Index(name = "ix_adv_office_policy_enabled", columnList = "enabled")
        }
)
@Getter
@Setter
public class EquipeOfficePolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipe_id", nullable = false, foreignKey = @ForeignKey(name = "fk_adv_office_policy_equipe"))
    private Equipe equipe;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = false;

    @Column(name = "signer_user_id")
    private Long signerUserId;

    @Column(name = "bloqueia_causas_proprias", nullable = false)
    private boolean bloqueiaCausasProprias = false;


    @Column(name = "force_patrono_certificate", nullable = false)
    private boolean forcePatronoCertificate = true;

    @Column(name = "min_trust_auto", nullable = false)
    private int minTrustAuto = 10;

    @Column(name = "max_auto_por_dia", nullable = false)
    private int maxAutoPorDia = 200;


    @Convert(converter = RamoDireitoSetConverter.class)
    @Column(name = "allowed_ramos", nullable = false, length = 1200)
    private Set<RamoDireito> allowedRamos = EnumSet.noneOf(RamoDireito.class);

    @Convert(converter = OfficeActionSetConverter.class)
    @Column(name = "auto_actions", nullable = false, length = 1200)
    private Set<OfficeActionType> autoActions = EnumSet.noneOf(OfficeActionType.class);

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
