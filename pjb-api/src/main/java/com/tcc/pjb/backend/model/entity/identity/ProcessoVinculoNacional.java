package com.tcc.pjb.backend.model.entity.identity;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import com.tcc.pjb.backend.model.entity.enums.GrauConfiancaVinculoProcessual;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.PapelProcessualNacional;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@PjbDataOwnership(module = PjbModuleId.IDENTIDADE_SEGURANCA, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_processo_vinculo_nacional", uniqueConstraints = {
        @UniqueConstraint(name = "uk_processo_vinculo_nacional", columnNames = {"identidade_id", "nupn", "papel_processual"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessoVinculoNacional {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "identidade_id", nullable = false)
    private UUID identidadeId;

    @Column(name = "documento_hash", nullable = false, length = 64)
    private String documentoHash;

    @Column(name = "nupn", nullable = false, length = 50)
    private String nupn;

    @Column(name = "processo_local_id")
    private Long processoLocalId;

    @Column(name = "tribunal_codigo", nullable = false, length = 20)
    private String tribunalCodigo;

    @Column(name = "tribunal_origem_uri", length = 240)
    private String tribunalOrigemUri;

    @Column(name = "sistema_origem", length = 20)
    private String sistemaOrigem;

    @Enumerated(EnumType.STRING)
    @Column(name = "papel_processual", nullable = false, length = 40)
    private PapelProcessualNacional papelProcessual;

    @Column(name = "polo_processual", nullable = false, length = 20)
    private String poloProcessual;

    @Column(name = "qualificacao_original", nullable = false, length = 40)
    private String qualificacaoOriginal;

    @Enumerated(EnumType.STRING)
    @Column(name = "ramo_direito", length = 60)
    private RamoDireito ramoDireito;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_processo", nullable = false, length = 60)
    private StatusProcesso statusProcesso;

    @Column(name = "classe_processual", length = 160)
    private String classeProcessual;

    @Column(name = "assunto", length = 240)
    private String assunto;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_sigilo", length = 40)
    private NivelSigilo nivelSigilo;

    @Enumerated(EnumType.STRING)
    @Column(name = "grau_confianca", nullable = false, length = 40)
    private GrauConfiancaVinculoProcessual grauConfianca;

    @Column(name = "score_confianca", nullable = false)
    private Integer scoreConfianca;

    @Column(name = "origem_vinculo", nullable = false, length = 40)
    private String origemVinculo;

    @Column(name = "visivel_painel_pessoal", nullable = false)
    private boolean visivelPainelPessoal;

    @Column(name = "exige_step_up", nullable = false)
    private boolean exigeStepUp;

    @Column(name = "contestado", nullable = false)
    private boolean contestado;

    @Column(name = "ocorrido_em", nullable = false)
    private Instant ocorridoEm;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (criadoEm == null) {
            criadoEm = now;
        }
        if (ocorridoEm == null) {
            ocorridoEm = now;
        }
        atualizadoEm = now;
    }

    @PreUpdate
    public void preUpdate() {
        atualizadoEm = Instant.now();
    }

    public void atualizarContexto(ProcessoVinculoSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        this.processoLocalId = snapshot.processoLocalId();
        this.tribunalCodigo = snapshot.tribunalCodigo();
        this.tribunalOrigemUri = snapshot.tribunalOrigemUri();
        this.sistemaOrigem = snapshot.sistemaOrigem();
        this.ramoDireito = snapshot.ramoDireito();
        this.statusProcesso = snapshot.statusProcesso();
        this.classeProcessual = snapshot.classeProcessual();
        this.assunto = snapshot.assunto();
        this.nivelSigilo = snapshot.nivelSigilo();
        this.grauConfianca = snapshot.grauConfianca();
        this.scoreConfianca = snapshot.scoreConfianca();
        this.origemVinculo = snapshot.origemVinculo();
        this.visivelPainelPessoal = snapshot.visivelPainelPessoal();
        this.exigeStepUp = snapshot.exigeStepUp();
        this.contestado = snapshot.contestado();
        this.ocorridoEm = snapshot.ocorridoEm();
        this.atualizadoEm = Instant.now();
    }

    public record ProcessoVinculoSnapshot(
            Long processoLocalId,
            String tribunalCodigo,
            String tribunalOrigemUri,
            String sistemaOrigem,
            RamoDireito ramoDireito,
            StatusProcesso statusProcesso,
            String classeProcessual,
            String assunto,
            NivelSigilo nivelSigilo,
            GrauConfiancaVinculoProcessual grauConfianca,
            Integer scoreConfianca,
            String origemVinculo,
            boolean visivelPainelPessoal,
            boolean exigeStepUp,
            boolean contestado,
            Instant ocorridoEm
    ) {
    }
}
