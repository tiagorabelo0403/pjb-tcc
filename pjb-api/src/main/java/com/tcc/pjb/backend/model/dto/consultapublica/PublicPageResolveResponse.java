package com.tcc.pjb.backend.model.dto.consultapublica;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PublicPageResolveResponse {
    String pageId;
    UUID documentoId;
    String documentoTitulo;
    String publicActKind;
    @JsonIgnore
    Long processoId;
    String numeroUnificado;
    Integer pageNumber;
    String fingerprint;
    String texto;
}
