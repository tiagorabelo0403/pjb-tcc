package com.tcc.pjb.backend.service.communication.domicilio;

import java.util.ArrayList;
import java.util.List;

public final class DomicilioJudicialPayloadValidator {

    private DomicilioJudicialPayloadValidator() {}

    public record PayloadInput(
            String cpfCnpjDestinatario,
            String conteudoHash,
            String tipoEnvio,
            boolean destinatarioCadastrado,
            boolean assinaturaDigitalValida
    ) {}

    public record PayloadValidacao(
            boolean valido,
            List<String> erros
    ) {}

    public PayloadValidacao validar(PayloadInput input) {
        List<String> erros = new ArrayList<>();
        if (input.cpfCnpjDestinatario() == null || input.cpfCnpjDestinatario().isBlank()) {
            erros.add("CPF/CNPJ do destinatário obrigatório.");
        }
        if (!input.destinatarioCadastrado()) {
            erros.add("Destinatário não cadastrado no domicílio judicial eletrônico.");
        }
        if (input.conteudoHash() == null || input.conteudoHash().isBlank()) {
            erros.add("Hash do conteúdo obrigatório para integridade.");
        }
        if (!input.assinaturaDigitalValida()) {
            erros.add("Assinatura digital inválida ou ausente.");
        }
        return new PayloadValidacao(erros.isEmpty(), erros);
    }
}
