package com.tcc.pjb.backend.model.dto.consultapublica;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ConsultaPublicaHitDTO {
    @JsonIgnore
    Long processoId;
    String numeroUnificado;
    String tipoJustica;
    String ramoDireito;
    String classeProcessual;
    String assunto;
    LocalDateTime dataUltimaMovimentacao;

    UUID documentoId;
    String documentoTitulo;
    String origemSistema;

    String pageId;
    Integer pageNumber;
    String snippet;
    Double score;
}
