package com.tcc.pjb.backend.model.dto.prazo;

import java.util.List;

public record PrazoCartorioPainelResponse(
        String vara,
        long totalPendentes,
        long vencidos,
        long vencendoEm7Dias,
        long vencendoEm15Dias,
        List<PrazoCartorioItemResponse> itens
) {}
