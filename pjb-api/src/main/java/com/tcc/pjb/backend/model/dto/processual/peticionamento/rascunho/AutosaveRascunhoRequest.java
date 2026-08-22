package com.tcc.pjb.backend.model.dto.processual.peticionamento.rascunho;

import jakarta.validation.constraints.Size;
import java.util.List;

public record AutosaveRascunhoRequest(
        @Size(max = 180) String tituloCaso,
        @Size(max = 400_000) String minutaHtml,
        @Size(max = 200) List<@Size(max = 8000) String> fatos,
        @Size(max = 200) List<@Size(max = 8000) String> pedidos,
        @Size(max = 200) List<@Size(max = 8000) String> fundamentos,
        @Size(max = 200) List<@Size(max = 8000) String> provas
) {
}
