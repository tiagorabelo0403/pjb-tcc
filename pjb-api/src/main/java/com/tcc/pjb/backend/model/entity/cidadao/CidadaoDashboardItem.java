package com.tcc.pjb.backend.model.entity.cidadao;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@PjbDataOwnership(module = PjbModuleId.PROCESSO_LIFECYCLE, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_cidadao_dashboard_item")
@IdClass(CidadaoDashboardItemId.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CidadaoDashboardItem {

    @Id
    @Column(name = "cidadao_user_id", nullable = false)
    private Long cidadaoUserId;

    @Id
    @Column(name = "processo_id", nullable = false)
    private Long processoId;

    @Column(name = "last_update_at", nullable = false)
    private Instant lastUpdateAt;

    @Column(name = "sort_key", nullable = false)
    private long sortKey;

    @Column(name = "card_json", nullable = false, length = 12000)
    private String cardJson;

    @Column(name = "flags_json", nullable = false, length = 12000)
    private String flagsJson;
}
