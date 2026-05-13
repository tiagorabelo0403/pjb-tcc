package com.tcc.pjb.backend.model.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DocumentoEnriquecidoDTO {
    String nomeOriginal;
    String conteudoPublico;
    String tipoProvaDetectado;
    String forcaProbatoria;
    boolean contemDadosSensiveis;
    String sugestaoSistema;
    Integer urgenciaCalculada;
    String classeInferida;
    String ramoInferido;
}
