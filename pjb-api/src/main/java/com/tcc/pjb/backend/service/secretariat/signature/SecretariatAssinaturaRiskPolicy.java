package com.tcc.pjb.backend.service.secretariat.signature;

import java.util.ArrayList;
import java.util.List;

public final class SecretariatAssinaturaRiskPolicy {

    private SecretariatAssinaturaRiskPolicy() {}

    public record AssinaturaRiskInput(
            String hashSha256,
            boolean documentoRenderizado,
            boolean signatarioHabilitado,
            boolean papelOuLocalizacaoValido,
            boolean sigiloCompativel,
            boolean assinaturaDigitalValida
    ) {}

    public record AssinaturaRiskResultado(
            boolean aprovado,
            List<String> riscos,
            String mensagem
    ) {}

    public AssinaturaRiskResultado avaliar(AssinaturaRiskInput input) {
        List<String> riscos = new ArrayList<>();
        if (input.hashSha256() == null || input.hashSha256().isBlank()) {
            riscos.add("Hash SHA-256 ausente — integridade do documento não verificada.");
        }
        if (!input.documentoRenderizado()) {
            riscos.add("Documento não renderizado — assinar sem visualizar é proibido.");
        }
        if (!input.signatarioHabilitado()) {
            riscos.add("Signatário sem habilitação para este tipo de documento.");
        }
        if (!input.papelOuLocalizacaoValido()) {
            riscos.add("Papel ou lotação do signatário incompatível com este ato.");
        }
        if (!input.sigiloCompativel()) {
            riscos.add("Documento com nível de sigilo incompatível com o perfil do signatário.");
        }
        if (!input.assinaturaDigitalValida()) {
            riscos.add("Certificado digital inválido ou expirado.");
        }
        boolean aprovado = riscos.isEmpty();
        String mensagem = aprovado
                ? "Documento apto para assinatura."
                : "Assinatura bloqueada: " + riscos.size() + " risco(s) identificado(s).";
        return new AssinaturaRiskResultado(aprovado, riscos, mensagem);
    }
}
