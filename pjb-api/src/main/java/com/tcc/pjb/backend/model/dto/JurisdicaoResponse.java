package com.tcc.pjb.backend.model.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.tcc.pjb.backend.model.entity.*;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import lombok.Getter;
import lombok.Setter;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.EsferaJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.MateriaJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.NaturezaJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.TipoJurisdicao;

@Getter
@Setter
public class JurisdicaoResponse {
    private Long id;
    private String codigo;
    private String sigla;
    private String nome;
    private TipoJurisdicao tipo;
    private NaturezaJurisdicao natureza;
    private GrauJurisdicao grau;
    private EsferaJurisdicao esfera;
    private MateriaJurisdicao materia;
    private String comarca;
    private String estado;
    private BigDecimal tetoValorCausa;
    private boolean ativo;
    private LocalDateTime dataCriacao;
}
