package com.tcc.pjb.backend.model.entity.judicial;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "pjb_mni_recepcao", indexes = {
        @Index(name = "idx_mni_recepcao_hash", columnList = "mni_payload_hash"),
        @Index(name = "idx_mni_recepcao_processo", columnList = "processo_id_local")
})
@PjbDataOwnership(module = PjbModuleId.INTEGRACOES_EXTERNAS, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MniRecepcao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tribunal_origem", nullable = false, length = 32)
    private String tribunalOrigem;

    @Column(name = "numero_unificado", nullable = false, length = 64)
    private String numeroUnificado;

    @Column(name = "processo_id_local")
    private Long processoIdLocal;

    @Column(name = "motivo", length = 64)
    private String motivo;

    @Column(name = "mni_payload_hash", nullable = false, length = 128)
    private String mniPayloadHash;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "status", nullable = false, length = 32)
    private String status;
}
