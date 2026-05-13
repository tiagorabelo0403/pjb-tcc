package com.tcc.pjb.backend.model.entity.enums;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Optional;

public enum DestinatarioInstitucionalKind {
    MINISTERIO_PUBLICO,
    DEFENSORIA_PUBLICA,
    ADVOCACIA_PUBLICA,
    PROCURADORIA_ESTADO,
    PROCURADORIA_MUNICIPIO,
    AGU,
    FAZENDA_PUBLICA,
    DELEGACIA_POLICIA,
    DELEGACIA_POLICIA_CIVIL,
    DELEGACIA_POLICIA_FEDERAL,
    POLICIA_PENAL,
    UNIDADE_PRISIONAL,
    CONSELHO_TUTELAR,
    PERICIA_JUDICIAL,
    PERITO_JUDICIAL,
    CONTADORIA_JUDICIAL,
    EQUIPE_PSICOSSOCIAL,
    ASSISTENTE_SOCIAL_JUDICIAL,
    CEJUSC,
    CARTORIO_EXTRAJUDICIAL,
    ORGAO_TECNICO_CONVENIADO,
    JUIZO_DEPRECADO,
    ORGAO_JUDICIAL_EXTERNO;

    public boolean isInstituicaoEssencialJustica() {
        return switch (this) {
            case MINISTERIO_PUBLICO,
                    DEFENSORIA_PUBLICA,
                    ADVOCACIA_PUBLICA,
                    PROCURADORIA_ESTADO,
                    PROCURADORIA_MUNICIPIO,
                    AGU,
                    FAZENDA_PUBLICA -> true;
            default -> false;
        };
    }

    public boolean isApoioTecnicoOuAuxiliar() {
        return switch (this) {
            case PERICIA_JUDICIAL,
                    PERITO_JUDICIAL,
                    CONTADORIA_JUDICIAL,
                    EQUIPE_PSICOSSOCIAL,
                    ASSISTENTE_SOCIAL_JUDICIAL,
                    CEJUSC,
                    CARTORIO_EXTRAJUDICIAL,
                    ORGAO_TECNICO_CONVENIADO -> true;
            default -> false;
        };
    }

    public boolean admiteCanalNacionalPessoal() {
        return switch (this) {
            case MINISTERIO_PUBLICO,
                    DEFENSORIA_PUBLICA,
                    ADVOCACIA_PUBLICA,
                    PROCURADORIA_ESTADO,
                    PROCURADORIA_MUNICIPIO,
                    AGU,
                    FAZENDA_PUBLICA,
                    JUIZO_DEPRECADO,
                    ORGAO_JUDICIAL_EXTERNO -> true;
            default -> false;
        };
    }

    public OrganizacaoExtraJudicialKind toOrganizacaoExtraJudicialKind() {
        return OrganizacaoExtraJudicialKind.fromDestinatario(this);
    }

    public Optional<NationalCommunicationRecipientKind> toNationalCommunicationRecipientKind() {
        return switch (this) {
            case MINISTERIO_PUBLICO -> Optional.of(NationalCommunicationRecipientKind.MINISTERIO_PUBLICO);
            case DEFENSORIA_PUBLICA -> Optional.of(NationalCommunicationRecipientKind.DEFENSOR_PUBLICO);
            case ADVOCACIA_PUBLICA, PROCURADORIA_ESTADO, PROCURADORIA_MUNICIPIO, AGU, FAZENDA_PUBLICA -> Optional.of(NationalCommunicationRecipientKind.FAZENDA_PUBLICA);
            case DELEGACIA_POLICIA, DELEGACIA_POLICIA_CIVIL, DELEGACIA_POLICIA_FEDERAL -> Optional.of(NationalCommunicationRecipientKind.DELEGACIA_POLICIA);
            case POLICIA_PENAL -> Optional.of(NationalCommunicationRecipientKind.POLICIA_PENAL);
            case UNIDADE_PRISIONAL -> Optional.of(NationalCommunicationRecipientKind.UNIDADE_PRISIONAL);
            case CONSELHO_TUTELAR -> Optional.of(NationalCommunicationRecipientKind.CONSELHO_TUTELAR);
            case PERICIA_JUDICIAL, PERITO_JUDICIAL -> Optional.of(NationalCommunicationRecipientKind.PERITO_JUDICIAL);
            case CONTADORIA_JUDICIAL -> Optional.of(NationalCommunicationRecipientKind.CONTADORIA_JUDICIAL);
            case EQUIPE_PSICOSSOCIAL, ASSISTENTE_SOCIAL_JUDICIAL -> Optional.of(NationalCommunicationRecipientKind.EQUIPE_PSICOSSOCIAL);
            case CEJUSC -> Optional.of(NationalCommunicationRecipientKind.CEJUSC);
            case CARTORIO_EXTRAJUDICIAL -> Optional.of(NationalCommunicationRecipientKind.CARTORIO_EXTRAJUDICIAL);
            case ORGAO_TECNICO_CONVENIADO -> Optional.of(NationalCommunicationRecipientKind.ORGAO_TECNICO_CONVENIADO);
            case JUIZO_DEPRECADO -> Optional.of(NationalCommunicationRecipientKind.JUIZO_DEPRECADO);
            case ORGAO_JUDICIAL_EXTERNO -> Optional.of(NationalCommunicationRecipientKind.ORGAO_JUDICIAL_EXTERNO);
        };
    }

    public static Optional<DestinatarioInstitucionalKind> fromNationalCommunicationRecipientKind(NationalCommunicationRecipientKind kind) {
        if (kind == null) {
            return Optional.empty();
        }
        return switch (kind) {
            case MINISTERIO_PUBLICO -> Optional.of(MINISTERIO_PUBLICO);
            case DEFENSOR_PUBLICO -> Optional.of(DEFENSORIA_PUBLICA);
            case ADVOCACIA_PUBLICA -> Optional.of(ADVOCACIA_PUBLICA);
            case FAZENDA_PUBLICA -> Optional.of(FAZENDA_PUBLICA);
            case DELEGACIA_POLICIA -> Optional.of(DELEGACIA_POLICIA);
            case POLICIA_PENAL -> Optional.of(POLICIA_PENAL);
            case UNIDADE_PRISIONAL -> Optional.of(UNIDADE_PRISIONAL);
            case CONSELHO_TUTELAR -> Optional.of(CONSELHO_TUTELAR);
            case PERITO_JUDICIAL -> Optional.of(PERITO_JUDICIAL);
            case CONTADORIA_JUDICIAL -> Optional.of(CONTADORIA_JUDICIAL);
            case EQUIPE_PSICOSSOCIAL -> Optional.of(EQUIPE_PSICOSSOCIAL);
            case CEJUSC -> Optional.of(CEJUSC);
            case CARTORIO_EXTRAJUDICIAL -> Optional.of(CARTORIO_EXTRAJUDICIAL);
            case ORGAO_TECNICO_CONVENIADO -> Optional.of(ORGAO_TECNICO_CONVENIADO);
            case JUIZO_DEPRECADO -> Optional.of(JUIZO_DEPRECADO);
            case ORGAO_JUDICIAL_EXTERNO -> Optional.of(ORGAO_JUDICIAL_EXTERNO);
            default -> Optional.empty();
        };
    }

    public static DestinatarioInstitucionalKind fromTexto(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String token = Normalizer.normalize(raw.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(Locale.ROOT);
        token = switch (token) {
            case "MP", "MINISTERIO_PUBLICO_ESTADUAL", "MINISTERIO_PUBLICO_FEDERAL" -> "MINISTERIO_PUBLICO";
            case "DEFENSORIA", "DEFENSORIA_PUBLICA_ESTADUAL", "DEFENSORIA_PUBLICA_UNIAO" -> "DEFENSORIA_PUBLICA";
            case "PROCURADORIA", "PROCURADORIA_GERAL", "ADVOCACIA_PUBLICA" -> "ADVOCACIA_PUBLICA";
            case "PROCURADORIA_ESTADO", "PGE", "PROCURADORIA_GERAL_DO_ESTADO" -> "PROCURADORIA_ESTADO";
            case "PROCURADORIA_MUNICIPIO", "PGM", "PROCURADORIA_GERAL_DO_MUNICIPIO" -> "PROCURADORIA_MUNICIPIO";
            case "AGU" -> "AGU";
            case "FAZENDA_PUBLICA", "FAZENDA" -> "FAZENDA_PUBLICA";
            case "DELEGACIA" -> "DELEGACIA_POLICIA";
            case "POLICIA_CIVIL", "DELEGACIA_POLICIA_CIVIL" -> "DELEGACIA_POLICIA_CIVIL";
            case "POLICIA_FEDERAL", "DELEGACIA_POLICIA_FEDERAL" -> "DELEGACIA_POLICIA_FEDERAL";
            case "CARTORIO", "CARTORIO_EXTRAJUDICIAL_INTEGRADO" -> "CARTORIO_EXTRAJUDICIAL";
            case "PERITO" -> "PERITO_JUDICIAL";
            case "PERITO_JUDICIAL" -> "PERITO_JUDICIAL";
            case "ASSISTENTE_SOCIAL", "ASSISTENTE_SOCIAL_JUDICIAL" -> "ASSISTENTE_SOCIAL_JUDICIAL";
            default -> token;
        };
        try {
            return DestinatarioInstitucionalKind.valueOf(token);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
