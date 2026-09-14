package com.tcc.pjb.backend.integration.mni.migration;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Um item de uma fila de migração em lote via MNI: um payload XML de um processo de outro
 * tribunal, aguardando (ou já submetido a) {@code MniRecepcaoService.receberAutos()}. A ordenação
 * por id serve de cursor resumível para o job de migração (ver {@code MniMigrationBatchService}).
 */
@Entity
@Table(name = "mni_migration_batch_item", indexes = {
        @Index(name = "ix_mni_migration_status_id", columnList = "status, id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MniMigrationBatchItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tribunal_origem", length = 40)
    private String tribunalOrigem;

    @Column(name = "motivo", length = 80)
    private String motivo;

    @Column(name = "xml", columnDefinition = "text", nullable = false)
    private String xml;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MniMigrationItemStatus status;

    @Column(name = "processo_id_local")
    private Long processoIdLocal;

    @Column(name = "erro", columnDefinition = "text")
    private String erro;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "processado_em")
    private Instant processadoEm;

    @PrePersist
    void prePersist() {
        if (status == null) {
            status = MniMigrationItemStatus.PENDENTE;
        }
        if (criadoEm == null) {
            criadoEm = Instant.now();
        }
    }
}
