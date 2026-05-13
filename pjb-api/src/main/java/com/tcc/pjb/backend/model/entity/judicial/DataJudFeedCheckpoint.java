package com.tcc.pjb.backend.model.entity.judicial;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "pjb_datajud_feed_checkpoint")
@PjbDataOwnership(module = PjbModuleId.INTEGRACOES_EXTERNAS, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataJudFeedCheckpoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tribunal_codigo", nullable = false, unique = true, length = 32)
    private String tribunalCodigo;

    @Column(name = "last_processo_id", nullable = false)
    private Long lastProcessoId;

    @Column(name = "last_sent_at")
    private Instant lastSentAt;

    @Column(name = "total_sent", nullable = false)
    private Long totalSent;

    @Column(name = "last_error", length = 4000)
    private String lastError;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static DataJudFeedCheckpoint init(String tribunalCodigo) {
        return DataJudFeedCheckpoint.builder()
                .tribunalCodigo(tribunalCodigo)
                .lastProcessoId(0L)
                .totalSent(0L)
                .updatedAt(Instant.now())
                .build();
    }
}
