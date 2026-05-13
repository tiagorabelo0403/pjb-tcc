package com.tcc.pjb.backend.modules.advocacia.office.entity;

import com.tcc.pjb.backend.model.entity.Equipe;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "adv_office_workspace_presence", uniqueConstraints = {
        @UniqueConstraint(name = "uk_adv_office_presence_equipe_user", columnNames = {"equipe_id", "user_id"})
})
@Getter
@Setter
public class AdvOfficeWorkspacePresence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipe_id", nullable = false, foreignKey = @ForeignKey(name = "fk_adv_office_presence_equipe"))
    private Equipe equipe;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "membro_equipe_id")
    private Long membroEquipeId;

    @Column(name = "office_mode", nullable = false, length = 20)
    private String officeMode;

    @Column(name = "source_path", length = 255)
    private String sourcePath;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
