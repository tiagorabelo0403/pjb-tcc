package com.tcc.pjb.backend.model.dto.pastadigital;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PastaDigitalResponse {
    Long processoId;
    List<DocumentoResumoDTO> documentos;
}
