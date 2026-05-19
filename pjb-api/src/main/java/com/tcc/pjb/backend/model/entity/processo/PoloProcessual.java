package com.tcc.pjb.backend.model.entity.processo;

import com.tcc.pjb.backend.model.entity.enums.TipoParte;
import com.tcc.pjb.backend.model.entity.enums.TipoPolo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;

@Entity
@Table(name = "tb_polo_processual")
@Getter
public class PoloProcessual {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "processo_id", nullable = false)
    private Long processoId;

    @Column(name = "identidade_id")
    private UUID identidadeId;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_polo", nullable = false)
    private TipoPolo tipoPolo;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_parte", nullable = false)
    private TipoParte tipoParte;

    @Column(name = "nome_completo", nullable = false)
    private String nomeCompleto;

    @Column(name = "documento")
    private String documento;

    @Column(name = "documento_tipo")
    private String documentoTipo;

    @Column(name = "oab_numero")
    private String oabNumero;

    @Column(name = "oab_uf")
    private String oabUf;

    @Column(name = "representado_por_id")
    private Long representadoPorId;

    @Column(name = "ativo", nullable = false)
    private boolean ativo;

    @Column(name = "ordem_polo", nullable = false)
    private int ordemPolo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PoloProcessual() {
    }

    public PoloProcessual(Long processoId, TipoPolo tipoPolo, TipoParte tipoParte,
                          String nomeCompleto, String documento, String documentoTipo,
                          String oabNumero, String oabUf, Long usuarioId,
                          UUID identidadeId, Long representadoPorId, int ordemPolo) {
        this.processoId = processoId;
        this.tipoPolo = tipoPolo;
        this.tipoParte = tipoParte;
        this.nomeCompleto = nomeCompleto;
        this.documento = documento;
        this.documentoTipo = documentoTipo;
        this.oabNumero = oabNumero;
        this.oabUf = oabUf;
        this.usuarioId = usuarioId;
        this.identidadeId = identidadeId;
        this.representadoPorId = representadoPorId;
        this.ordemPolo = ordemPolo;
        this.ativo = true;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void desativar() {
        this.ativo = false;
        this.updatedAt = Instant.now();
    }

    public boolean isAdvogado() {
        return oabNumero != null && !oabNumero.isBlank();
    }

    public boolean isPessoaFisica() {
        return "CPF".equals(documentoTipo);
    }

    public boolean isPessoaJuridica() {
        return "CNPJ".equals(documentoTipo);
    }
}
