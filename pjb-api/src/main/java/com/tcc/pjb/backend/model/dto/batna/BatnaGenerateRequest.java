package com.tcc.pjb.backend.model.dto.batna;

import java.math.BigDecimal;

public record BatnaGenerateRequest(
        Long processoId,
        Long propostaAcordoId,
        String nupn,
        String tribunalCodigo,
        String ramoDireito,
        String classeTpu,
        BigDecimal valorCausa,
        BigDecimal valorPedidoPrincipal,
        String faseAtual,
        Integer diasEmAndamento,
        Boolean temRecursoProvavel,
        Boolean autorAssistidoPorAdvogado,
        Boolean reuAssistidoPorAdvogado,
        Boolean autorBeneficiarioJg,
        Boolean reuBeneficiarioJg,
        Boolean autorPessoaJuridica,
        Boolean reuPessoaJuridica,
        String uf,
        BigDecimal valorAcordoEmDiscussao,
        Boolean modoEstritoTeto
) {
}
