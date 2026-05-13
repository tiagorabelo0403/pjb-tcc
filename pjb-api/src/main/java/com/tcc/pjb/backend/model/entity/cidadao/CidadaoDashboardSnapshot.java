package com.tcc.pjb.backend.model.entity.cidadao;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@PjbDataOwnership(module = PjbModuleId.PROCESSO_LIFECYCLE, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_cidadao_dashboard_snapshot")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CidadaoDashboardSnapshot {

    @Id
    @Column(name = "cidadao_user_id", nullable = false)
    private Long cidadaoUserId;

    @Column(name = "cpf_hash", nullable = false, length = 64)
    private String cpfHash;

    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "badges_json", nullable = false, length = 12000)
    private String badgesJson;

    @Column(name = "widgets_json", nullable = false, length = 12000)
    private String widgetsJson;

    @Column(name = "pendencias_json", nullable = false, length = 12000)
    private String pendenciasJson;

    @Column(name = "proximos_eventos_json", nullable = false, length = 12000)
    private String proximosEventosJson;

    @Column(name = "recentes_json", nullable = false, length = 12000)
    private String recentesJson;

    @Column(name = "gov_hub_json", nullable = false, length = 12000)
    private String govHubJson;
}
