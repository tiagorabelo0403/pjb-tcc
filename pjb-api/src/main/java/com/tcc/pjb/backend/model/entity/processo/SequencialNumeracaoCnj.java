package com.tcc.pjb.backend.model.entity.processo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;

@Entity
@Table(name = "pjb_sequencial_numeracao_cnj")
@Getter
public class SequencialNumeracaoCnj {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "segmento", nullable = false)
    private int segmento;

    @Column(name = "tribunal", nullable = false)
    private int tribunal;

    @Column(name = "unidade", nullable = false)
    private int unidade;

    @Column(name = "ano", nullable = false)
    private int ano;

    @Column(name = "ultimo_seq", nullable = false)
    private long ultimoSeq;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SequencialNumeracaoCnj() {
    }

    public SequencialNumeracaoCnj(int segmento, int tribunal, int unidade, int ano) {
        this.segmento = segmento;
        this.tribunal = tribunal;
        this.unidade = unidade;
        this.ano = ano;
        this.ultimoSeq = 0;
        this.updatedAt = Instant.now();
    }

    public long incrementar() {
        this.ultimoSeq++;
        this.updatedAt = Instant.now();
        return this.ultimoSeq;
    }
}
