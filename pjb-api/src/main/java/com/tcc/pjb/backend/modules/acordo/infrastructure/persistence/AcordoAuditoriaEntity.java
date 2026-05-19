package com.tcc.pjb.backend.modules.acordo.infrastructure.persistence;

import com.tcc.pjb.backend.modules.acordo.domain.AcordoAuditoriaEvento;
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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "tb_acordo_auditoria",
        indexes = {
                @Index(name = "idx_acordo_audit_sessao_created", columnList = "sessao_id, created_at"),
                @Index(name = "idx_acordo_audit_evento", columnList = "evento, created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class AcordoAuditoriaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sessao_id", nullable = false)
    private Long sessaoId;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(name = "evento", nullable = false, length = 40)
    private AcordoAuditoriaEvento evento;

    @Column(name = "detalhes_json", nullable = false, columnDefinition = "jsonb")
    private String detalhesJson;

    @Column(name = "ip_hash", length = 64)
    private String ipHash;

    @Column(name = "user_agent_hash", length = 64)
    private String userAgentHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
