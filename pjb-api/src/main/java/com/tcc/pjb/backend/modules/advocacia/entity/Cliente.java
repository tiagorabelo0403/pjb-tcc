package com.tcc.pjb.backend.modules.advocacia.entity;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import com.tcc.pjb.backend.model.converter.SensitiveDataConverter;
import com.tcc.pjb.backend.model.entity.Equipe;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.modules.advocacia.enums.StatusCliente;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "adv_clientes",
        indexes = {
                @Index(name = "idx_cliente_advogado_status", columnList = "advogado_id, status"),
                @Index(name = "idx_cliente_advogado_cpf_hash", columnList = "advogado_id, cpf_hash", unique = true),
                @Index(name = "idx_cliente_cpf_hash", columnList = "cpf_hash")
        }
)
@FilterDef(
        name = "filtroEquipe",
        parameters = {
                @ParamDef(name = "usuarioIdParam", type = Long.class),
                @ParamDef(name = "equipeIdParam", type = Long.class)
        }
)
@Filter(
        name = "filtroEquipe",
        condition = "((:equipeIdParam = -1 and equipe_id is null and advogado_id = :usuarioIdParam) " +
                "or (:equipeIdParam <> -1 and equipe_id = :equipeIdParam))"
)
@EntityListeners(AuditingEntityListener.class)
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "advogado_id", nullable = false)
    private Usuario advogado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipe_id")
    private Equipe equipe;

    @Column(nullable = false, length = 150)
    private String nome;

    public String getNomeCompleto() { return nome; }
    public void setNomeCompleto(String nomeCompleto) { this.nome = nomeCompleto; }

    @Column(nullable = false, length = 150)
    private String nomeNormalizado;

    @Convert(converter = SensitiveDataConverter.class)
    @Column(name = "cpf_criptografado", length = 1000)
    private String cpfCriptografado;

    @Column(name = "cpf_hash", length = 64)
    private String cpfHash;

    @Convert(converter = SensitiveDataConverter.class)
    @Column(name = "email_criptografado", length = 1000)
    private String emailCriptografado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusCliente status;

    @Column(length = 20)
    private String telefone;

    @Column(length = 255)
    private String endereco;

    @Lob
    @Column(columnDefinition = "TEXT")
private String observacoes;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime dataAtualizacao;

    @Version
    private Long versao;

    @Column(name = "nivel_confiabilidade")
    private Double nivelConfiabilidade = 1.0;

    @Column(name = "ultima_analise_ia")
    private LocalDateTime ultimaAnaliseIa;

    @PrePersist
    public void prePersist() {
        this.nomeNormalizado = normalizar(this.nome);
        if (this.status == null) {
            this.status = StatusCliente.ATIVO;
        }
        if (this.dataCriacao == null) {
            this.dataCriacao = LocalDateTime.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.nomeNormalizado = normalizar(this.nome);
    }

    private String normalizar(String texto) {
        if (texto == null) return null;
        return java.text.Normalizer.normalize(texto, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .trim();
    }
}
