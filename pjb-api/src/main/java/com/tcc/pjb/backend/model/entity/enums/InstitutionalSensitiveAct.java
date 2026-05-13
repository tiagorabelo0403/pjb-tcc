package com.tcc.pjb.backend.model.entity.enums;

import java.util.Locale;

public enum InstitutionalSensitiveAct {
    DAR_CIENCIA_INSTITUCIONAL(
            CapacidadeCaixaInstitucional.DAR_CIENCIA,
            InstitutionalTrustLevel.NIVEL_2_NOMEACAO_ATIVA,
            false,
            false,
            false,
            false),
    APROVAR_MINUTA_FINAL(
            CapacidadeCaixaInstitucional.PREPARAR_MINUTA,
            InstitutionalTrustLevel.NIVEL_3_CERTIFICADO_QUALIFICADO,
            true,
            false,
            false,
            true),
    ASSINAR_MANIFESTACAO(
            CapacidadeCaixaInstitucional.ASSINAR_MANIFESTACAO,
            InstitutionalTrustLevel.NIVEL_3_CERTIFICADO_QUALIFICADO,
            true,
            true,
            true,
            true),
    PETICIONAR_EM_NOME_DO_ORGAO(
            CapacidadeCaixaInstitucional.PETICIONAR_EM_NOME_DO_ORGAO,
            InstitutionalTrustLevel.NIVEL_3_CERTIFICADO_QUALIFICADO,
            true,
            true,
            true,
            true),
    REDISTRIBUICAO_SENSIVEL(
            CapacidadeCaixaInstitucional.REDISTRIBUIR_INTERNAMENTE,
            InstitutionalTrustLevel.NIVEL_3_CERTIFICADO_QUALIFICADO,
            true,
            false,
            false,
            false),
    GERAR_CERTIDAO_DE_CIENCIA(
            CapacidadeCaixaInstitucional.GERAR_CERTIDAO_CIENCIA,
            InstitutionalTrustLevel.NIVEL_2_NOMEACAO_ATIVA,
            false,
            false,
            false,
            false);

    private final CapacidadeCaixaInstitucional requiredCapability;
    private final InstitutionalTrustLevel minimumTrust;
    private final boolean requireMfa;
    private final boolean requireCertificate;
    private final boolean requireNetworkOrRemoteAuthorization;
    private final boolean requireTitularAuthority;

    InstitutionalSensitiveAct(CapacidadeCaixaInstitucional requiredCapability,
                              InstitutionalTrustLevel minimumTrust,
                              boolean requireMfa,
                              boolean requireCertificate,
                              boolean requireNetworkOrRemoteAuthorization,
                              boolean requireTitularAuthority) {
        this.requiredCapability = requiredCapability;
        this.minimumTrust = minimumTrust;
        this.requireMfa = requireMfa;
        this.requireCertificate = requireCertificate;
        this.requireNetworkOrRemoteAuthorization = requireNetworkOrRemoteAuthorization;
        this.requireTitularAuthority = requireTitularAuthority;
    }

    public CapacidadeCaixaInstitucional requiredCapability() {
        return requiredCapability;
    }

    public InstitutionalTrustLevel minimumTrust() {
        return minimumTrust;
    }

    public boolean requireMfa() {
        return requireMfa;
    }

    public boolean requireCertificate() {
        return requireCertificate;
    }

    public boolean requireNetworkOrRemoteAuthorization() {
        return requireNetworkOrRemoteAuthorization;
    }

    public boolean requireTitularAuthority() {
        return requireTitularAuthority;
    }

    public static InstitutionalSensitiveAct fromTexto(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String token = raw.trim().replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT);
        token = switch (token) {
            case "DAR_CIENCIA", "CIENCIA", "DAR_CIENCIA_ORGAO" -> "DAR_CIENCIA_INSTITUCIONAL";
            case "APROVAR_MINUTA", "MINUTA_FINAL" -> "APROVAR_MINUTA_FINAL";
            case "ASSINATURA", "ASSINAR" -> "ASSINAR_MANIFESTACAO";
            case "PETICIONAR", "PETICIONAMENTO" -> "PETICIONAR_EM_NOME_DO_ORGAO";
            case "REDISTRIBUICAO", "REDISTRIBUICAO_CRITICA" -> "REDISTRIBUICAO_SENSIVEL";
            case "CERTIDAO_CIENCIA" -> "GERAR_CERTIDAO_DE_CIENCIA";
            default -> token;
        };
        try {
            return InstitutionalSensitiveAct.valueOf(token);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
