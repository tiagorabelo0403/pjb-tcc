package com.tcc.pjb.backend.model.entity.identity;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;

@PjbDataOwnership(module = PjbModuleId.IDENTIDADE_SEGURANCA, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(
        name = "tb_prontuario_nacional_entrada",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_prontuario_nacional_natural",
                        columnNames = {"documento_hash", "nupn", "polo", "qualificacao", "tribunal_codigo"}
                )
        }
)
public class ProntuarioNacionalEntrada {

    public enum PoloProcessual {
        ATIVO,
        PASSIVO,
        INTERESSADO,
        TERCEIRO,
        REPRESENTANTE,
        AUXILIAR_JUSTICA
    }

    public enum QualificacaoProcessual {
        AUTOR,
        REU,
        ADVOGADO,
        REPRESENTANTE_LEGAL,
        MEMBRO_MP,
        DEFENSOR_PUBLICO,
        PROCURADOR_PUBLICO,
        PERITO,
        TESTEMUNHA,
        ASSISTENTE,
        IMPETRANTE,
        IMPETRADO,
        EXECUTANTE,
        EXECUTADO,
        INTERESSADO,
        AUTORIDADE,
        VITIMA,
        INVESTIGADO
    }

    public enum OrigemRegistro {
        AJUIZAMENTO_LOCAL,
        SINCRONIZACAO_NACIONAL,
        MIGRACAO_HOMOLOGADA,
        RETIFICACAO_OPERADOR,
        MATERIALIZACAO
    }

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "identidade_id")
    private IdentidadeJuridicaNacional identidade;

    @Column(name = "documento", nullable = false, length = 14)
    private String documento;

    @Column(name = "documento_hash", nullable = false, length = 64)
    private String documentoHash;

    @Column(name = "nome_sujeito", nullable = false, length = 180)
    private String nomeSujeito;

    @Column(name = "nome_sujeito_chave", nullable = false, length = 180)
    private String nomeSujeitoChave;

    @Column(name = "nupn", nullable = false, length = 50)
    private String nupn;

    @Column(name = "processo_local_id")
    private Long processoLocalId;

    @Column(name = "tribunal_codigo", nullable = false, length = 20)
    private String tribunalCodigo;

    @Column(name = "tribunal_origem_uri", length = 240)
    private String tribunalOrigemUri;

    @Enumerated(EnumType.STRING)
    @Column(name = "polo", nullable = false, length = 20)
    private PoloProcessual polo;

    @Enumerated(EnumType.STRING)
    @Column(name = "qualificacao", nullable = false, length = 30)
    private QualificacaoProcessual qualificacao;

    @Enumerated(EnumType.STRING)
    @Column(name = "ramo_direito", length = 60)
    private RamoDireito ramoDireito;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_processo", nullable = false, length = 60)
    private StatusProcesso statusProcesso;

    @Column(name = "classe_processual", length = 120)
    private String classeProcessual;

    @Column(name = "assunto", length = 180)
    private String assunto;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_sigilo", length = 40)
    private NivelSigilo nivelSigilo;

    @Enumerated(EnumType.STRING)
    @Column(name = "origem_registro", nullable = false, length = 30)
    private OrigemRegistro origemRegistro;

    @Column(name = "fonte_evento_id", length = 80)
    private String fonteEventoId;

    @Column(name = "ocorrido_em", nullable = false)
    private Instant ocorridoEm;

    @Column(name = "registrado_em", nullable = false)
    private Instant registradoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected ProntuarioNacionalEntrada() {
    }

    public ProntuarioNacionalEntrada(UUID id,
                                     IdentidadeJuridicaNacional identidade,
                                     String documento,
                                     String documentoHash,
                                     String nomeSujeito,
                                     String nomeSujeitoChave,
                                     String nupn,
                                     Long processoLocalId,
                                     String tribunalCodigo,
                                     String tribunalOrigemUri,
                                     PoloProcessual polo,
                                     QualificacaoProcessual qualificacao,
                                     RamoDireito ramoDireito,
                                     StatusProcesso statusProcesso,
                                     String classeProcessual,
                                     String assunto,
                                     NivelSigilo nivelSigilo,
                                     OrigemRegistro origemRegistro,
                                     String fonteEventoId,
                                     Instant ocorridoEm) {
        this.id = Objects.requireNonNull(id);
        this.identidade = identidade;
        this.documento = Objects.requireNonNull(documento);
        this.documentoHash = Objects.requireNonNull(documentoHash);
        this.nomeSujeito = Objects.requireNonNull(nomeSujeito);
        this.nomeSujeitoChave = Objects.requireNonNull(nomeSujeitoChave);
        this.nupn = Objects.requireNonNull(nupn);
        this.processoLocalId = processoLocalId;
        this.tribunalCodigo = Objects.requireNonNull(tribunalCodigo);
        this.tribunalOrigemUri = tribunalOrigemUri;
        this.polo = Objects.requireNonNull(polo);
        this.qualificacao = Objects.requireNonNull(qualificacao);
        this.ramoDireito = ramoDireito;
        this.statusProcesso = Objects.requireNonNull(statusProcesso);
        this.classeProcessual = classeProcessual;
        this.assunto = assunto;
        this.nivelSigilo = nivelSigilo;
        this.origemRegistro = Objects.requireNonNull(origemRegistro);
        this.fonteEventoId = fonteEventoId;
        this.ocorridoEm = Objects.requireNonNull(ocorridoEm);
    }

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (registradoEm == null) {
            registradoEm = now;
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

    public void atualizarStatus(StatusProcesso novoStatus) {
        this.statusProcesso = Objects.requireNonNull(novoStatus);
        this.atualizadoEm = Instant.now();
    }

    public void atualizarProcesso(String nomeSujeito,
                                  String nomeSujeitoChave,
                                  RamoDireito ramoDireito,
                                  StatusProcesso statusProcesso,
                                  String classeProcessual,
                                  String assunto,
                                  NivelSigilo nivelSigilo,
                                  String tribunalOrigemUri,
                                  String fonteEventoId,
                                  Instant ocorridoEm) {
        this.nomeSujeito = Objects.requireNonNull(nomeSujeito);
        this.nomeSujeitoChave = Objects.requireNonNull(nomeSujeitoChave);
        this.ramoDireito = ramoDireito;
        this.statusProcesso = Objects.requireNonNull(statusProcesso);
        this.classeProcessual = classeProcessual;
        this.assunto = assunto;
        this.nivelSigilo = nivelSigilo;
        this.tribunalOrigemUri = tribunalOrigemUri;
        this.fonteEventoId = fonteEventoId;
        this.ocorridoEm = Objects.requireNonNull(ocorridoEm);
        this.atualizadoEm = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public IdentidadeJuridicaNacional getIdentidade() {
        return identidade;
    }

    public void setIdentidade(IdentidadeJuridicaNacional identidade) {
        this.identidade = identidade;
    }

    public String getDocumento() {
        return documento;
    }

    public String getDocumentoHash() {
        return documentoHash;
    }

    public String getNomeSujeito() {
        return nomeSujeito;
    }

    public String getNomeSujeitoChave() {
        return nomeSujeitoChave;
    }

    public String getNupn() {
        return nupn;
    }

    public Long getProcessoLocalId() {
        return processoLocalId;
    }

    public String getTribunalCodigo() {
        return tribunalCodigo;
    }

    public String getTribunalOrigemUri() {
        return tribunalOrigemUri;
    }

    public PoloProcessual getPolo() {
        return polo;
    }

    public QualificacaoProcessual getQualificacao() {
        return qualificacao;
    }

    public RamoDireito getRamoDireito() {
        return ramoDireito;
    }

    public StatusProcesso getStatusProcesso() {
        return statusProcesso;
    }

    public String getClasseProcessual() {
        return classeProcessual;
    }

    public String getAssunto() {
        return assunto;
    }

    public NivelSigilo getNivelSigilo() {
        return nivelSigilo;
    }

    public OrigemRegistro getOrigemRegistro() {
        return origemRegistro;
    }

    public String getFonteEventoId() {
        return fonteEventoId;
    }

    public Instant getOcorridoEm() {
        return ocorridoEm;
    }

    public Instant getRegistradoEm() {
        return registradoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }
}
