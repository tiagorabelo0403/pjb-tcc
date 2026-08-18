package com.tcc.pjb.backend.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tb_unidade_institucional_abrangencia")
public class UnidadeInstitucionalAbrangencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "unidade_institucional_id", nullable = false)
    private Long unidadeInstitucionalId;

    @Column(name = "comarca_atendida", nullable = false, length = 120)
    private String comarcaAtendida;

    public Long getId() { return id; }
    public Long getUnidadeInstitucionalId() { return unidadeInstitucionalId; }
    public void setUnidadeInstitucionalId(Long unidadeInstitucionalId) { this.unidadeInstitucionalId = unidadeInstitucionalId; }
    public String getComarcaAtendida() { return comarcaAtendida; }
    public void setComarcaAtendida(String comarcaAtendida) { this.comarcaAtendida = comarcaAtendida; }
}
