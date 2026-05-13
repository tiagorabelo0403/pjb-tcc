package com.tcc.pjb.backend.model.entity.jurisprudencia;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.TipoPrecedente;
import com.tcc.pjb.backend.model.entity.enums.TribunalFonte;
import lombok.*;

@PjbDataOwnership(module = PjbModuleId.INTELIGENCIA_APLICADA, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_precedente",
        indexes = {
                @Index(name = "idx_prec_fonte_tipo", columnList = "fonte,tipo"),
                @Index(name = "idx_prec_ident", columnList = "identificador"),
                @Index(name = "idx_prec_data", columnList = "data_publicacao")
        })
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Precedente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "fonte", length = 30, nullable = false)
    private TribunalFonte fonte;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", length = 60, nullable = false)
    private TipoPrecedente tipo;

    
    @Column(name = "identificador", length = 80)
    private String identificador;

    @Column(name = "titulo", length = 260)
    private String titulo;

    @Column(name = "tese", columnDefinition = "TEXT")
    private String tese;

    @Column(name = "ementa_resumo", columnDefinition = "TEXT")
    private String ementaResumo;

    @Column(name = "url_referencia", length = 600)
    private String urlReferencia;

    @Column(name = "data_publicacao")
    private LocalDate dataPublicacao;

    
    @Enumerated(EnumType.STRING)
    @Column(name = "ramo_sugerido", length = 60)
    private RamoDireito ramoSugerido;

    @Enumerated(EnumType.STRING)
    @Column(name = "rito_sugerido", length = 60)
    private RitoProcessual ritoSugerido;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    @PreUpdate
    private void normalizeBeforeSave() {
        
        identificador = normalize(identificador);
        titulo = limit(normalize(titulo), 260);
        urlReferencia = limit(normalize(urlReferencia), 600);
        tese = normalizeSpaces(tese);
        ementaResumo = normalizeSpaces(ementaResumo);
    }

    private static String normalize(String v) {
        if (v == null) return null;
        String s = v.trim().replaceAll("\\s+", " ");
        return s.isEmpty() ? null : s;
    }

    private static String normalizeSpaces(String v) {
        if (v == null) return null;
        String s = v.trim().replaceAll("\\s+", " ");
        return s.isEmpty() ? null : s;
    }



    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public TribunalFonte getFonte() { return fonte; }
    public void setFonte(TribunalFonte fonte) { this.fonte = fonte; }
    public TipoPrecedente getTipo() { return tipo; }
    public void setTipo(TipoPrecedente tipo) { this.tipo = tipo; }
    public String getIdentificador() { return identificador; }
    public void setIdentificador(String identificador) { this.identificador = identificador; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getTese() { return tese; }
    public void setTese(String tese) { this.tese = tese; }
    public String getEmentaResumo() { return ementaResumo; }
    public void setEmentaResumo(String ementaResumo) { this.ementaResumo = ementaResumo; }
    public String getUrlReferencia() { return urlReferencia; }
    public void setUrlReferencia(String urlReferencia) { this.urlReferencia = urlReferencia; }
    public LocalDate getDataPublicacao() { return dataPublicacao; }
    public void setDataPublicacao(LocalDate dataPublicacao) { this.dataPublicacao = dataPublicacao; }
    public RamoDireito getRamoSugerido() { return ramoSugerido; }
    public void setRamoSugerido(RamoDireito ramoSugerido) { this.ramoSugerido = ramoSugerido; }
    public RitoProcessual getRitoSugerido() { return ritoSugerido; }
    public void setRitoSugerido(RitoProcessual ritoSugerido) { this.ritoSugerido = ritoSugerido; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    private static String limit(String v, int max) {
        if (v == null) return null;
        if (v.length() <= max) return v;
        return v.substring(0, max);
    }
}
