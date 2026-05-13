package com.tcc.pjb.backend.model.entity.intelligence;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@PjbDataOwnership(module = PjbModuleId.INTELIGENCIA_APLICADA, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_diligencia_operador_certidao_documento", indexes = {
        @Index(name = "idx_diligencia_certidao_documento_certidao", columnList = "certidao_id, created_at"),
        @Index(name = "idx_diligencia_certidao_documento_doc", columnList = "documento_id"),
        @Index(name = "idx_diligencia_certidao_documento_unique", columnList = "certidao_id, documento_id", unique = true)
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiligenciaOperadorCertidaoDocumento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "certidao_id", nullable = false)
    private Long certidaoId;

    @Column(name = "processo_id", nullable = false)
    private Long processoId;

    @Column(name = "documento_id", nullable = false)
    private UUID documentoId;

    @Column(name = "documento_titulo", length = 255)
    private String documentoTitulo;

    @Column(name = "documento_sha256", length = 64)
    private String documentoSha256;

    @Column(name = "origem", nullable = false, length = 20)
    private String origem;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        createdAt = createdAt == null ? Instant.now() : createdAt;
        origem = normalize(origem);
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "REQUEST";
        }
        String normalized = value.trim().toUpperCase();
        return normalized.length() <= 20 ? normalized : normalized.substring(0, 20);
    }
}
