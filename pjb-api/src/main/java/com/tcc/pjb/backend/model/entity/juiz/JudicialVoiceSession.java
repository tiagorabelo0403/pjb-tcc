package com.tcc.pjb.backend.model.entity.juiz;

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
import com.tcc.pjb.backend.model.entity.Usuario;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@PjbDataOwnership(module = PjbModuleId.PROCESSO_LIFECYCLE, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_judicial_voice_session",
        indexes = {
                @Index(name = "idx_judicial_voice_session_magistrado", columnList = "magistrado_id,status"),
                @Index(name = "idx_judicial_voice_session_processo", columnList = "processo_id")
        })
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class JudicialVoiceSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "processo_id", nullable = false)
    private Processo processo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "magistrado_id", nullable = false)
    private Usuario magistrado;

    @Column(name = "modo_documento", nullable = false, length = 40)
    private String modoDocumento;

    @Column(name = "status", nullable = false, length = 40)
    private String status;

    @Column(name = "transcricao_integral", columnDefinition = "TEXT")
    private String transcricaoIntegral;

    @Column(name = "relatorio_draft", columnDefinition = "TEXT")
    private String relatorioDraft;

    @Column(name = "fundamentacao_draft", columnDefinition = "TEXT")
    private String fundamentacaoDraft;

    @Column(name = "dispositivo_draft", columnDefinition = "TEXT")
    private String dispositivoDraft;

    @Column(name = "comando_resumo", columnDefinition = "TEXT")
    private String comandoResumo;

    @Column(name = "audio_preview_text", columnDefinition = "TEXT")
    private String audioPreviewText;

    @Column(name = "aberta_em")
    private Instant abertaEm;

    @Column(name = "finalizada_em")
    private Instant finalizadaEm;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        if (modoDocumento == null || modoDocumento.isBlank()) {
            modoDocumento = "SENTENCA";
        }
        if (status == null || status.isBlank()) {
            status = "ABERTA";
        }
        if (abertaEm == null) {
            abertaEm = Instant.now();
        }
    }
}
