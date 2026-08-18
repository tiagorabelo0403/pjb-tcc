package com.tcc.pjb.backend.modules.acordo.infrastructure.persistence;

import com.tcc.pjb.backend.modules.acordo.domain.AcordoTermoStatus;
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
        name = "tb_acordo_termo",
        indexes = {
                @Index(name = "idx_acordo_termo_sessao", columnList = "sessao_id"),
                @Index(name = "idx_acordo_termo_proposta", columnList = "proposta_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class AcordoTermoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sessao_id", nullable = false)
    private Long sessaoId;

    @Column(name = "proposta_id", nullable = false)
    private Long propostaId;

    @Column(name = "conteudo_termo", nullable = false, columnDefinition = "TEXT")
    private String conteudoTermo;

    @Column(name = "hash_termo", nullable = false, length = 64)
    private String hashTermo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private AcordoTermoStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
