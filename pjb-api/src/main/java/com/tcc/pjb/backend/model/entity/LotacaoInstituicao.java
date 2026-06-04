package com.tcc.pjb.backend.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "tb_lotacao_institucional")
public class LotacaoInstituicao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unidade_id", nullable = false)
    private UnidadeInstituicao unidade;

    @Column(nullable = false)
    private LocalDate inicio;

    @Column
    private LocalDate fim;

    @Column(name = "papel_na_unidade", length = 60)
    private String papelNaUnidade;

    public boolean isAtiva() {
        return fim == null;
    }

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public UnidadeInstituicao getUnidade() { return unidade; }
    public void setUnidade(UnidadeInstituicao unidade) { this.unidade = unidade; }
    public LocalDate getInicio() { return inicio; }
    public void setInicio(LocalDate inicio) { this.inicio = inicio; }
    public LocalDate getFim() { return fim; }
    public void setFim(LocalDate fim) { this.fim = fim; }
    public String getPapelNaUnidade() { return papelNaUnidade; }
    public void setPapelNaUnidade(String papelNaUnidade) { this.papelNaUnidade = papelNaUnidade; }
}
