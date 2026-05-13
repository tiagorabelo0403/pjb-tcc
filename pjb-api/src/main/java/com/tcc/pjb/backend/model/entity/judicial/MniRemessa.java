package com.tcc.pjb.backend.model.entity.judicial;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;
import com.tcc.pjb.backend.integration.mni.domain.MniStatusRemessa;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "pjb_mni_remessa", indexes = {
        @Index(name = "idx_mni_remessa_status", columnList = "status,proximo_retry_em"),
        @Index(name = "idx_mni_remessa_processo", columnList = "processo_id")
})
@PjbDataOwnership(module = PjbModuleId.INTEGRACOES_EXTERNAS, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MniRemessa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "processo_id", nullable = false)
    private Long processoId;

    @Column(name = "tribunal_destino", nullable = false, length = 32)
    private String tribunalDestino;

    @Column(name = "motivo", nullable = false, length = 64)
    private String motivo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private MniStatusRemessa status;

    @Column(name = "protocolo_destino", length = 128)
    private String protocoloDestino;

    @Column(name = "mni_payload_hash", length = 128)
    private String mniPayloadHash;

    @Column(name = "tentativas", nullable = false)
    private Integer tentativas;

    @Column(name = "max_tentativas", nullable = false)
    private Integer maxTentativas;

    @Column(name = "proximo_retry_em")
    private Instant proximoRetryEm;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "failure_reason", length = 4000)
    private String failureReason;
}
