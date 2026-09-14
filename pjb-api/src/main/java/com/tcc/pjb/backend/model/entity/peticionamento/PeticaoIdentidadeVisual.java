package com.tcc.pjb.backend.model.entity.peticionamento;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

@PjbDataOwnership(module = PjbModuleId.PROCESSO_LIFECYCLE, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_peticao_identidade_visual",
        indexes = {
                @Index(name = "uk_peticao_identidade_usuario", columnList = "usuario_id", unique = true)
        })
public class PeticaoIdentidadeVisual {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "escopo", nullable = false, length = 20)
    private String escopo;

    @Column(name = "escopo_ref", length = 80)
    private String escopoRef;

    @Column(name = "nome_exibicao", length = 600)
    private String nomeExibicao;

    @Column(name = "nome_instituicao", length = 600)
    private String nomeInstituicao;

    @Column(name = "logo_storage_key", columnDefinition = "TEXT")
    private String logoStorageKey;

    @Column(name = "logo_content_type", length = 80)
    private String logoContentType;

    @Column(name = "logo_size_bytes")
    private Long logoSizeBytes;

    @Column(name = "logo_sha256", length = 64)
    private String logoSha256;

    @Column(name = "cabecalho_livre", length = 1500)
    private String cabecalhoLivre;

    @Column(name = "rodape_livre", length = 1500)
    private String rodapeLivre;

    @Column(name = "paleta_primaria", length = 16)
    private String paletaPrimaria;

    @Column(name = "paleta_secundaria", length = 16)
    private String paletaSecundaria;

    @Column(name = "exibir_registro_profissional", nullable = false)
    private boolean exibirRegistroProfissional;

    @Column(name = "exibir_brasao_logomarca", nullable = false)
    private boolean exibirBrasaoLogomarca;

    @Column(name = "ativo", nullable = false)
    private boolean ativo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PeticaoIdentidadeVisual() {
    }

    public PeticaoIdentidadeVisual(Long usuarioId) {
        this.usuarioId = usuarioId;
        this.escopo = "INDIVIDUAL";
        this.exibirRegistroProfissional = true;
        this.exibirBrasaoLogomarca = true;
        this.ativo = true;
    }

    /** Perfil institucional (papel timbrado do órgão), sem dono individual — curado por admin do órgão. */
    public static PeticaoIdentidadeVisual institucional(String escopoRef) {
        PeticaoIdentidadeVisual e = new PeticaoIdentidadeVisual();
        e.escopo = "INSTITUCIONAL";
        e.escopoRef = escopoRef;
        e.exibirRegistroProfissional = true;
        e.exibirBrasaoLogomarca = true;
        e.ativo = true;
        return e;
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (escopo == null || escopo.isBlank()) {
            escopo = "INDIVIDUAL";
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public void aplicarLogo(String storageKey, String contentType, long sizeBytes, String sha256) {
        this.logoStorageKey = storageKey;
        this.logoContentType = contentType;
        this.logoSizeBytes = sizeBytes;
        this.logoSha256 = sha256;
    }

    public void removerLogo() {
        this.logoStorageKey = null;
        this.logoContentType = null;
        this.logoSizeBytes = null;
        this.logoSha256 = null;
    }

    public boolean temLogo() {
        return logoStorageKey != null && !logoStorageKey.isBlank();
    }

    public Long getId() {
        return id;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public String getEscopo() {
        return escopo;
    }

    public void setEscopo(String escopo) {
        this.escopo = escopo;
    }

    public String getEscopoRef() {
        return escopoRef;
    }

    public void setEscopoRef(String escopoRef) {
        this.escopoRef = escopoRef;
    }

    public String getNomeExibicao() {
        return nomeExibicao;
    }

    public void setNomeExibicao(String nomeExibicao) {
        this.nomeExibicao = nomeExibicao;
    }

    public String getNomeInstituicao() {
        return nomeInstituicao;
    }

    public void setNomeInstituicao(String nomeInstituicao) {
        this.nomeInstituicao = nomeInstituicao;
    }

    public String getLogoStorageKey() {
        return logoStorageKey;
    }

    public String getLogoContentType() {
        return logoContentType;
    }

    public Long getLogoSizeBytes() {
        return logoSizeBytes;
    }

    public String getLogoSha256() {
        return logoSha256;
    }

    public String getCabecalhoLivre() {
        return cabecalhoLivre;
    }

    public void setCabecalhoLivre(String cabecalhoLivre) {
        this.cabecalhoLivre = cabecalhoLivre;
    }

    public String getRodapeLivre() {
        return rodapeLivre;
    }

    public void setRodapeLivre(String rodapeLivre) {
        this.rodapeLivre = rodapeLivre;
    }

    public String getPaletaPrimaria() {
        return paletaPrimaria;
    }

    public void setPaletaPrimaria(String paletaPrimaria) {
        this.paletaPrimaria = paletaPrimaria;
    }

    public String getPaletaSecundaria() {
        return paletaSecundaria;
    }

    public void setPaletaSecundaria(String paletaSecundaria) {
        this.paletaSecundaria = paletaSecundaria;
    }

    public boolean isExibirRegistroProfissional() {
        return exibirRegistroProfissional;
    }

    public void setExibirRegistroProfissional(boolean exibirRegistroProfissional) {
        this.exibirRegistroProfissional = exibirRegistroProfissional;
    }

    public boolean isExibirBrasaoLogomarca() {
        return exibirBrasaoLogomarca;
    }

    public void setExibirBrasaoLogomarca(boolean exibirBrasaoLogomarca) {
        this.exibirBrasaoLogomarca = exibirBrasaoLogomarca;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
