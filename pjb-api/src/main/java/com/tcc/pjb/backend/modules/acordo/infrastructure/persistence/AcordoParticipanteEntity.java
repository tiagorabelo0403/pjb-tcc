package com.tcc.pjb.backend.modules.acordo.infrastructure.persistence;

import com.tcc.pjb.backend.modules.acordo.domain.AcordoPapelParticipante;
import com.tcc.pjb.backend.modules.acordo.domain.AcordoParticipanteStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "tb_acordo_participante",
        indexes = {
                @Index(name = "idx_acordo_part_sessao_status", columnList = "sessao_id, status"),
                @Index(name = "idx_acordo_part_usuario", columnList = "usuario_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_acordo_part_sessao_usuario", columnNames = {"sessao_id", "usuario_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
public class AcordoParticipanteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sessao_id", nullable = false)
    private Long sessaoId;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(name = "papel", nullable = false, length = 40)
    private AcordoPapelParticipante papel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private AcordoParticipanteStatus status;

    @Column(name = "aceitou_em")
    private Instant aceitouEm;

    @Column(name = "recusou_em")
    private Instant recusouEm;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
