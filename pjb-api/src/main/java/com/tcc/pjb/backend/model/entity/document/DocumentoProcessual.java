package com.tcc.pjb.backend.model.entity.document;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.DocumentoCategoria;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

@Entity
@PjbDataOwnership(module = PjbModuleId.DOCUMENTOS, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Table(name = "tb_documento_processual")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DocumentoProcessual {

    @Id
    @GeneratedValue
    private UUID id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "processo_id")
    private Processo processo;

    private String titulo;

    @Column(name = "nome_original")
    private String nomeOriginal;

    private String sha256;
    private String sha384;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "tamanho_bytes")
    private Long tamanhoBytes;

    @Column(name = "storage_backend")
    private String storageBackend;

    @Column(name = "storage_uri")
    private String storageUri;

    @Column(name = "externalized_at")
    private Instant externalizedAt;

    @JsonIgnore
    @Column(name = "pdf", columnDefinition = "bytea")
    private byte[] pdf;

    @Column(name = "origem_sistema")
    private String origemSistema;

    @Enumerated(EnumType.STRING)
    private DocumentoCategoria categoria;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_sigilo")
    private NivelSigilo nivelSigilo;

    @Column(name = "visibility_scope", length = 80)
    private String visibilityScope;

    @Column(name = "criado_por")
    private Long criadoPor;

    @Column(name = "criado_em")
    private LocalDateTime criadoEm;

    public void setCriadoEm(Instant criadoEm) {
        this.criadoEm = criadoEm == null ? null : LocalDateTime.ofInstant(criadoEm, ZoneOffset.UTC);
    }

    public String getDocumentoId() {
        return id == null ? null : id.toString();
    }

    public String getDocumentoSha256() {
        return sha256;
    }

    public String getDocumentoTitulo() {
        return titulo == null || titulo.isBlank() ? nomeOriginal : titulo;
    }

    public Long getTamanhoBytes() {
        return tamanhoBytes;
    }

    public String getStorageBackend() {
        return storageBackend;
    }

    public String getStorageUri() {
        return storageUri;
    }

    public String getNomeOriginal() {
        return nomeOriginal;
    }

    public static DocumentoProcessualBuilder builder() {
        return new DocumentoProcessualBuilder();
    }

    public static final class DocumentoProcessualBuilder {
        private final DocumentoProcessual target = new DocumentoProcessual();

        public DocumentoProcessualBuilder id(UUID id) { target.id = id; return this; }
        public DocumentoProcessualBuilder processo(Processo processo) { target.processo = processo; return this; }
        public DocumentoProcessualBuilder titulo(String titulo) { target.titulo = titulo; return this; }
        public DocumentoProcessualBuilder nomeOriginal(String nomeOriginal) { target.nomeOriginal = nomeOriginal; return this; }
        public DocumentoProcessualBuilder sha256(String sha256) { target.sha256 = sha256; return this; }
        public DocumentoProcessualBuilder sha384(String sha384) { target.sha384 = sha384; return this; }
        public DocumentoProcessualBuilder contentType(String contentType) { target.contentType = contentType; return this; }
        public DocumentoProcessualBuilder tamanhoBytes(Long tamanhoBytes) { target.tamanhoBytes = tamanhoBytes; return this; }
        public DocumentoProcessualBuilder storageBackend(String storageBackend) { target.storageBackend = storageBackend; return this; }
        public DocumentoProcessualBuilder storageUri(String storageUri) { target.storageUri = storageUri; return this; }
        public DocumentoProcessualBuilder externalizedAt(Instant externalizedAt) { target.externalizedAt = externalizedAt; return this; }
        public DocumentoProcessualBuilder pdf(byte[] pdf) { target.pdf = pdf; return this; }
        public DocumentoProcessualBuilder origemSistema(String origemSistema) { target.origemSistema = origemSistema; return this; }
        public DocumentoProcessualBuilder categoria(DocumentoCategoria categoria) { target.categoria = categoria; return this; }
        public DocumentoProcessualBuilder nivelSigilo(NivelSigilo nivelSigilo) { target.nivelSigilo = nivelSigilo; return this; }
        public DocumentoProcessualBuilder visibilityScope(String visibilityScope) { target.visibilityScope = visibilityScope; return this; }
        public DocumentoProcessualBuilder criadoPor(Long criadoPor) { target.criadoPor = criadoPor; return this; }
        public DocumentoProcessualBuilder criadoEm(LocalDateTime criadoEm) { target.criadoEm = criadoEm; return this; }
        public DocumentoProcessual build() { return target; }
    }



    public void setTamanhoBytes(Long tamanhoBytes) {
        this.tamanhoBytes = tamanhoBytes;
    }

    public void setStorageBackend(String storageBackend) {
        this.storageBackend = storageBackend;
    }

    public void setStorageUri(String storageUri) {
        this.storageUri = storageUri;
    }

    public void setCriadoPor(Long criadoPor) {
        this.criadoPor = criadoPor;
    }

    public void setCriadoPor(Usuario usuario) {
        this.criadoPor = usuario == null ? null : usuario.getId();
    }

    public String getVisibilityScope() {
        return visibilityScope;
    }

    public void setVisibilityScope(String visibilityScope) {
        this.visibilityScope = visibilityScope;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }

}
