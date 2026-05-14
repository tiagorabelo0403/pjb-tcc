package com.tcc.pjb.backend.service.assinatura;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AssinaturaDigitalVerificadorService {

    public record AssinaturaInput(
            UUID documentoId,
            String cpfCnpjAssinante,
            String certifcadoSerial,
            String autoridade,
            LocalDateTime dataAssinatura,
            LocalDateTime validadeCertificado,
            boolean certificadoIcpBrasil,
            boolean cadeiaCertificacaoValida,
            boolean hashDocumentoValido,
            boolean naoRepudiado,
            boolean timestampAutorizado
    ) {}

    public record VerificacaoResult(
            boolean valida,
            List<String> falhas,
            String nivel,
            String fundamentacao
    ) {}

    public VerificacaoResult verificar(AssinaturaInput input) {
        List<String> falhas = new ArrayList<>();

        if (!input.certificadoIcpBrasil()) {
            falhas.add("Certificado não pertence à cadeia ICP-Brasil (MP 2.200-2/2001).");
        }
        if (!input.cadeiaCertificacaoValida()) {
            falhas.add("Cadeia de certificação inválida ou revogada.");
        }
        if (input.validadeCertificado().isBefore(input.dataAssinatura())) {
            falhas.add("Certificado expirado no momento da assinatura.");
        }
        if (!input.hashDocumentoValido()) {
            falhas.add("Hash do documento não confere — possível adulteração.");
        }
        if (!input.naoRepudiado()) {
            falhas.add("Assinatura sem atributo de não repúdio.");
        }
        if (!input.timestampAutorizado()) {
            falhas.add("Carimbo de tempo não autorizado por AC-Tempo credenciada.");
        }

        boolean valida = falhas.isEmpty();
        String nivel = determinarNivel(input, valida);

        return new VerificacaoResult(valida, List.copyOf(falhas), nivel,
                valida ? "Assinatura ICP-Brasil válida — nível " + nivel + " (MP 2.200-2/2001)."
                       : "Assinatura inválida: " + falhas.size() + " falha(s) crítica(s).");
    }

    private String determinarNivel(AssinaturaInput input, boolean valida) {
        if (!valida) return "INVALIDO";
        if (input.timestampAutorizado() && input.naoRepudiado()) return "A3";
        if (input.certificadoIcpBrasil()) return "A1";
        return "DESCONHECIDO";
    }
}
