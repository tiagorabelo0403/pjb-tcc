package com.tcc.pjb.backend.integration.judicial.financeiro.domain;

public record InfojudConsultaRequest(Long processoId,
                                     String cpfCnpjConsultado,
                                     boolean delegatedOperation) {
}
