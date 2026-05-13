package com.tcc.pjb.backend.model.dto.consultapublica;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ConsultaPublicaSearchResponse {
    String query;
    int page;
    int size;
    long total;
    List<ConsultaPublicaHitDTO> hits;
}
