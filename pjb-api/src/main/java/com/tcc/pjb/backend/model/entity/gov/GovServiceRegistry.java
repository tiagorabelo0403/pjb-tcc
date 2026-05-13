package com.tcc.pjb.backend.model.entity.gov;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.UpdateTimestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@PjbDataOwnership(module = PjbModuleId.INTEGRACOES_EXTERNAS, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_gov_service_registry")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GovServiceRegistry {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "uf", nullable = false, length = 2)
    private String uf;

    @Column(name = "category", nullable = false, length = 32)
    private String category;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", nullable = false, length = 16)
    private GovServiceType serviceType;

    @Column(name = "url", nullable = false, columnDefinition = "TEXT")
    private String url;

    @Column(name = "requirements_json", nullable = false, columnDefinition = "TEXT")
    private String requirementsJson;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
