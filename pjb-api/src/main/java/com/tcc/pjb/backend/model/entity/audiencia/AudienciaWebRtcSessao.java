package com.tcc.pjb.backend.model.entity.audiencia;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import com.tcc.pjb.backend.model.entity.Processo;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@PjbDataOwnership(module = PjbModuleId.PROCESSO_LIFECYCLE, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_audiencia_webrtc_sessao",
        indexes = {
                @Index(name = "idx_audiencia_webrtc_token", columnList = "sessao_token", unique = true),
                @Index(name = "idx_audiencia_webrtc_participante", columnList = "participante_usuario_id,status"),
                @Index(name = "idx_audiencia_webrtc_processo", columnList = "processo_id")
        })
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class AudienciaWebRtcSessao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "audiencia_id", nullable = false)
    private Long audienciaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processo_id")
    private Processo processo;

    @Column(name = "sessao_token", nullable = false, unique = true, length = 120)
    private String sessaoToken;

    @Column(name = "participante_identificador", nullable = false, length = 180)
    private String participanteIdentificador;

    @Column(name = "participante_usuario_id")
    private Long participanteUsuarioId;

    @Column(name = "status", nullable = false, length = 40)
    private String status;

    @Column(name = "ice_servers_csv", columnDefinition = "TEXT")
    private String iceServersCsv;

    @Column(name = "offer_hash", length = 128)
    private String offerHash;

    @Column(name = "answer_hash", length = 128)
    private String answerHash;

    @Column(name = "exigir_biometria", nullable = false)
    private boolean exigirBiometria;

    @Column(name = "biometria_status", length = 40)
    private String biometriaStatus;

    @Column(name = "transcricao_integral", columnDefinition = "TEXT")
    private String transcricaoIntegral;

    @Column(name = "gravacao_hash", length = 128)
    private String gravacaoHash;

    @Column(name = "metricas_json", columnDefinition = "TEXT")
    private String metricasJson;

    @Column(name = "aberta_em")
    private Instant abertaEm;

    @Column(name = "encerrada_em")
    private Instant encerradaEm;

    @Column(name = "expira_em")
    private Instant expiraEm;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        if (status == null || status.isBlank()) {
            status = "ABERTA";
        }
        if (biometriaStatus == null || biometriaStatus.isBlank()) {
            biometriaStatus = exigirBiometria ? "PENDENTE" : "DISPENSADA";
        }
        if (abertaEm == null) {
            abertaEm = Instant.now();
        }
        if (expiraEm == null) {
            expiraEm = abertaEm.plusSeconds(60L * 60L * 2L);
        }
    }
}
