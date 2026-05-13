package com.tcc.pjb.backend.modules.laiane.dto.roles.lawyer;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaianeHabilitacaoSolicitacaoRequest {

    
    @NotNull
    private Long clienteId;

    
    private String poderesJson;

    
    private String anexosJson;

    
    private String mensagem;

    private String tipoInstrumento;

    private Long audienciaId;

    private String tipoAudiencia;

    private Boolean contextoConsensual;

    private Boolean poderesEspeciaisTransigir;

    private String termoAudienciaReferencia;

    private String ataAudienciaReferencia;
}
