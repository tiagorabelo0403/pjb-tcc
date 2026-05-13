package com.tcc.pjb.backend.model.entity.infra;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@PjbDataOwnership(module = PjbModuleId.AUDITORIA_OBSERVABILIDADE, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_partition_plan",
        indexes = {
                @Index(name = "idx_partition_plan_table_name", columnList = "table_name", unique = true),
                @Index(name = "idx_partition_plan_status", columnList = "status")
        })
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class PartitionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "table_name", nullable = false, unique = true, length = 120)
    private String tableName;

    @Column(name = "partition_column", nullable = false, length = 120)
    private String partitionColumn;

    @Column(name = "partition_prefix", nullable = false, length = 120)
    private String partitionPrefix;

    @Column(name = "start_year", nullable = false)
    private Integer startYear;

    @Column(name = "years_ahead", nullable = false)
    private Integer yearsAhead;

    @Column(name = "status", nullable = false, length = 40)
    private String status;

    @Column(name = "last_materialized_year")
    private Integer lastMaterializedYear;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        if (status == null || status.isBlank()) {
            status = "ATIVO";
        }
        if (yearsAhead == null || yearsAhead <= 0) {
            yearsAhead = 2;
        }
    }
}
