package com.tcc.pjb.backend.modules.advocacia.office.entity;

import com.tcc.pjb.backend.model.entity.Processo;
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
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "adv_office_process_transfer_item",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_adv_office_process_transfer_item", columnNames = {"transfer_id", "processo_id"})
        },
        indexes = {
                @Index(name = "ix_adv_office_process_transfer_item_transfer", columnList = "transfer_id"),
                @Index(name = "ix_adv_office_process_transfer_item_processo", columnList = "processo_id")
        })
@Getter
@Setter
public class AdvOfficeProcessTransferItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transfer_id", nullable = false, foreignKey = @ForeignKey(name = "fk_adv_office_process_transfer_item_transfer"))
    private AdvOfficeProcessTransfer transfer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "processo_id", nullable = false, foreignKey = @ForeignKey(name = "fk_adv_office_process_transfer_item_processo"))
    private Processo processo;

    @Column(name = "source_equipe_id")
    private Long sourceEquipeId;

    @Column(name = "source_usuario_id")
    private Long sourceUsuarioId;

    @Column(name = "target_equipe_id", nullable = false)
    private Long targetEquipeId;

    @Column(name = "target_usuario_id", nullable = false)
    private Long targetUsuarioId;

    @Column(name = "ramo_direito", length = 64)
    private String ramoDireito;

    @Column(name = "nivel_sigilo", length = 64)
    private String nivelSigilo;

    @Column(name = "numero_processo_snapshot", length = 64)
    private String numeroProcessoSnapshot;
}
