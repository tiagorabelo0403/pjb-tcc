package com.tcc.pjb.backend.model.dto.magistratura;

import java.util.List;
import java.util.Set;
import com.tcc.pjb.backend.model.dto.projections.JurisdicaoResumoProjection;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.EsferaJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MagistraturaContextResponse {

    private Long userId;
    private String nome;
    private TipoUsuario tipoUsuario;
    private String personaKey;
    private String displayPerfil;
    private String tratamento;

    private String uf;
    private String comarca;

    private GrauJurisdicao grau;
    private EsferaJurisdicao esfera;

    
    private List<JurisdicaoResumoProjection> jurisdicoesProvaveis;

    
    private Set<String> areasAtuacaoProvaveis;

    
    private Set<String> ritosProvaveis;
}
