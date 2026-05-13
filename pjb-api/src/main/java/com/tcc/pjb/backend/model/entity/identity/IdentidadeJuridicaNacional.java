package com.tcc.pjb.backend.model.entity.identity;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@PjbDataOwnership(module = PjbModuleId.IDENTIDADE_SEGURANCA, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(
        name = "tb_identidade_juridica_nacional",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_identidade_juridica_documento", columnNames = {"tipo_documento", "documento"}),
                @UniqueConstraint(name = "uk_identidade_juridica_documento_hash", columnNames = {"documento_hash"})
        }
)
public class IdentidadeJuridicaNacional {

    public enum ReceitaStatus {
        PENDENTE,
        VALIDADO,
        IRREGULAR,
        SUSPENSO,
        CANCELADO,
        ERRO_SERVICO
    }

    public enum OabStatus {
        NAO_APLICAVEL,
        PENDENTE,
        ATIVO,
        SUSPENSO,
        CANCELADO
    }

    public enum PapelNacional {
        SUJEITO_PROCESSUAL,
        PESSOA_FISICA,
        PESSOA_JURIDICA,
        ADVOGADO,
        PARTE,
        REPRESENTANTE_LEGAL,
        MEMBRO_MP,
        DEFENSOR_PUBLICO,
        MAGISTRADO,
        SERVENTUARIO,
        PERITO,
        TESTEMUNHA,
        ORGAO_PUBLICO,
        AUTORIDADE_POLICIAL,
        PROCURADOR_PUBLICO
    }

    public enum NivelConfiancaIdentidade {
        AUTODECLARADA,
        DOCUMENTAL,
        FEDERADA,
        PROFISSIONAL_VALIDADA,
        MULTIFONTE,
        INSTITUCIONAL
    }

    public enum GovBrNivel {
        NAO_VINCULADO,
        BRONZE,
        PRATA,
        OURO
    }

    public enum OrigemCadastro {
        AUTOCADASTRO,
        GOV_BR,
        RECEITA_FEDERAL,
        OAB_NACIONAL,
        CNJ,
        TRIBUNAL,
        API_INSTITUCIONAL,
        MIGRACAO_HOMOLOGADA,
        USUARIO_SERVICE
    }

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", nullable = false, length = 10)
    private com.tcc.pjb.backend.core.validation.document.DocumentoNacionalValidator.TipoDocumento tipoDocumento;

    @Column(name = "documento", nullable = false, length = 14)
    private String documento;

    @Column(name = "documento_hash", nullable = false, length = 64)
    private String documentoHash;

    @Column(name = "documento_formatado", nullable = false, length = 20)
    private String documentoFormatado;

    @Column(name = "nome_canonico", nullable = false, length = 180)
    private String nomeCanonico;

    @Column(name = "chave_pesquisa", nullable = false, length = 180)
    private String chavePesquisa;

    @Column(name = "prontuario_nacional_uri", nullable = false, length = 240)
    private String prontuarioNacionalUri;

    @ElementCollection(targetClass = PapelNacional.class)
    @CollectionTable(name = "tb_identidade_juridica_papel", joinColumns = @JoinColumn(name = "identidade_id"))
    @Column(name = "papel", nullable = false, length = 40)
    @Enumerated(EnumType.STRING)
    private Set<PapelNacional> papeis = new LinkedHashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "receita_status", nullable = false, length = 20)
    private ReceitaStatus receitaStatus = ReceitaStatus.PENDENTE;

    @Column(name = "receita_verificado_em")
    private Instant receitaVerificadoEm;

    @Column(name = "receita_protocolo", length = 120)
    private String receitaProtocolo;

    @Column(name = "oab_numero", length = 20)
    private String oabNumero;

    @Column(name = "oab_uf", length = 2)
    private String oabUf;

    @Enumerated(EnumType.STRING)
    @Column(name = "oab_status", nullable = false, length = 20)
    private OabStatus oabStatus = OabStatus.NAO_APLICAVEL;

    @Column(name = "govbr_sub_hash", length = 64)
    private String govBrSubHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "govbr_nivel", nullable = false, length = 20)
    private GovBrNivel govBrNivel = GovBrNivel.NAO_VINCULADO;

    @Column(name = "govbr_vinculado_em")
    private Instant govBrVinculadoEm;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_confianca", nullable = false, length = 30)
    private NivelConfiancaIdentidade nivelConfianca = NivelConfiancaIdentidade.AUTODECLARADA;

    @Enumerated(EnumType.STRING)
    @Column(name = "origem_cadastro", nullable = false, length = 30)
    private OrigemCadastro origemCadastro = OrigemCadastro.AUTOCADASTRO;

    @Column(name = "bloqueio_unificacao", nullable = false)
    private boolean bloqueioUnificacao;

    @Column(name = "versao_unificacao", nullable = false)
    private long versaoUnificacao = 1L;

    @Column(name = "ultima_sincronizacao_em")
    private Instant ultimaSincronizacaoEm;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected IdentidadeJuridicaNacional() {
    }

    public IdentidadeJuridicaNacional(UUID id,
                                      com.tcc.pjb.backend.core.validation.document.DocumentoNacionalValidator.TipoDocumento tipoDocumento,
                                      String documento,
                                      String documentoHash,
                                      String documentoFormatado,
                                      String nomeCanonico,
                                      String chavePesquisa,
                                      String prontuarioNacionalUri,
                                      OrigemCadastro origemCadastro) {
        this.id = Objects.requireNonNull(id);
        this.tipoDocumento = Objects.requireNonNull(tipoDocumento);
        this.documento = Objects.requireNonNull(documento);
        this.documentoHash = Objects.requireNonNull(documentoHash);
        this.documentoFormatado = Objects.requireNonNull(documentoFormatado);
        this.nomeCanonico = Objects.requireNonNull(nomeCanonico);
        this.chavePesquisa = Objects.requireNonNull(chavePesquisa);
        this.prontuarioNacionalUri = Objects.requireNonNull(prontuarioNacionalUri);
        this.origemCadastro = Objects.requireNonNull(origemCadastro);
    }

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (criadoEm == null) {
            criadoEm = now;
        }
        atualizadoEm = now;
        if (ultimaSincronizacaoEm == null) {
            ultimaSincronizacaoEm = now;
        }
        if (tipoDocumento != null) {
            if (tipoDocumento == com.tcc.pjb.backend.core.validation.document.DocumentoNacionalValidator.TipoDocumento.CPF) {
                papeis.add(PapelNacional.SUJEITO_PROCESSUAL);
                papeis.add(PapelNacional.PESSOA_FISICA);
            } else {
                papeis.add(PapelNacional.SUJEITO_PROCESSUAL);
                papeis.add(PapelNacional.PESSOA_JURIDICA);
            }
        }
    }

    @PreUpdate
    public void preUpdate() {
        atualizadoEm = Instant.now();
    }

    public void atualizarNomeCanonico(String nomeCanonico, String chavePesquisa) {
        this.nomeCanonico = Objects.requireNonNull(nomeCanonico);
        this.chavePesquisa = Objects.requireNonNull(chavePesquisa);
        this.versaoUnificacao++;
        this.ultimaSincronizacaoEm = Instant.now();
    }

    public void registrarReceita(ReceitaStatus status, String protocolo) {
        this.receitaStatus = Objects.requireNonNull(status);
        this.receitaProtocolo = protocolo;
        this.receitaVerificadoEm = Instant.now();
        this.ultimaSincronizacaoEm = Instant.now();
    }

    public void registrarOab(String numero, String uf, OabStatus status) {
        this.oabNumero = numero;
        this.oabUf = uf;
        this.oabStatus = Objects.requireNonNull(status);
        this.ultimaSincronizacaoEm = Instant.now();
        if (numero != null && !numero.isBlank()) {
            this.papeis.add(PapelNacional.ADVOGADO);
        }
    }

    public void removerOabPendente() {
        if (this.oabStatus == OabStatus.PENDENTE || this.oabStatus == OabStatus.NAO_APLICAVEL) {
            this.oabNumero = null;
            this.oabUf = null;
            this.oabStatus = OabStatus.NAO_APLICAVEL;
            this.papeis.remove(PapelNacional.ADVOGADO);
            this.ultimaSincronizacaoEm = Instant.now();
        }
    }

    public void vincularGovBr(String govBrSubHash, GovBrNivel govBrNivel) {
        this.govBrSubHash = Objects.requireNonNull(govBrSubHash);
        this.govBrNivel = Objects.requireNonNull(govBrNivel);
        this.govBrVinculadoEm = Instant.now();
        this.ultimaSincronizacaoEm = Instant.now();
    }

    public void adicionarPapel(PapelNacional papel) {
        this.papeis.add(Objects.requireNonNull(papel));
        this.ultimaSincronizacaoEm = Instant.now();
    }

    public void atualizarConfianca(NivelConfiancaIdentidade nivelConfianca) {
        this.nivelConfianca = Objects.requireNonNull(nivelConfianca);
        this.ultimaSincronizacaoEm = Instant.now();
    }

    public void atualizarOrigem(OrigemCadastro origemCadastro) {
        this.origemCadastro = Objects.requireNonNull(origemCadastro);
        this.ultimaSincronizacaoEm = Instant.now();
    }

    public void bloquearUnificacao(boolean bloqueioUnificacao) {
        this.bloqueioUnificacao = bloqueioUnificacao;
        this.ultimaSincronizacaoEm = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public com.tcc.pjb.backend.core.validation.document.DocumentoNacionalValidator.TipoDocumento getTipoDocumento() {
        return tipoDocumento;
    }

    public String getDocumento() {
        return documento;
    }

    public String getDocumentoHash() {
        return documentoHash;
    }

    public String getDocumentoFormatado() {
        return documentoFormatado;
    }

    public String getNomeCanonico() {
        return nomeCanonico;
    }

    public String getChavePesquisa() {
        return chavePesquisa;
    }

    public String getProntuarioNacionalUri() {
        return prontuarioNacionalUri;
    }

    public Set<PapelNacional> getPapeis() {
        return papeis;
    }

    public ReceitaStatus getReceitaStatus() {
        return receitaStatus;
    }

    public Instant getReceitaVerificadoEm() {
        return receitaVerificadoEm;
    }

    public String getReceitaProtocolo() {
        return receitaProtocolo;
    }

    public String getOabNumero() {
        return oabNumero;
    }

    public String getOabUf() {
        return oabUf;
    }

    public OabStatus getOabStatus() {
        return oabStatus;
    }

    public String getGovBrSubHash() {
        return govBrSubHash;
    }

    public GovBrNivel getGovBrNivel() {
        return govBrNivel;
    }

    public Instant getGovBrVinculadoEm() {
        return govBrVinculadoEm;
    }

    public NivelConfiancaIdentidade getNivelConfianca() {
        return nivelConfianca;
    }

    public OrigemCadastro getOrigemCadastro() {
        return origemCadastro;
    }

    public boolean isBloqueioUnificacao() {
        return bloqueioUnificacao;
    }

    public long getVersaoUnificacao() {
        return versaoUnificacao;
    }

    public Instant getUltimaSincronizacaoEm() {
        return ultimaSincronizacaoEm;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }
}
