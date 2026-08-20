package com.tcc.pjb.backend.model.dto.security;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public final class CertificadoAuthDtos {

    private CertificadoAuthDtos() {
    }

    public enum Status {
        DESAFIO_EMITIDO,
        AUTENTICADO,
        PENDENTE_SELECAO,
        NEGADO,
        INDISPONIVEL
    }

    public static final class DesafioRequest {
        @NotBlank
        private String certificado;

        public String getCertificado() {
            return certificado;
        }

        public void setCertificado(String certificado) {
            this.certificado = certificado;
        }
    }

    public static final class RespostaRequest {
        @NotBlank
        private String certificado;

        @NotBlank
        private String nonce;

        @NotBlank
        private String assinatura;

        private String algoritmoAssinatura;

        public String getCertificado() {
            return certificado;
        }

        public void setCertificado(String certificado) {
            this.certificado = certificado;
        }

        public String getNonce() {
            return nonce;
        }

        public void setNonce(String nonce) {
            this.nonce = nonce;
        }

        public String getAssinatura() {
            return assinatura;
        }

        public void setAssinatura(String assinatura) {
            this.assinatura = assinatura;
        }

        public String getAlgoritmoAssinatura() {
            return algoritmoAssinatura;
        }

        public void setAlgoritmoAssinatura(String algoritmoAssinatura) {
            this.algoritmoAssinatura = algoritmoAssinatura;
        }
    }

    public record DesafioResponse(
            Status status,
            String nonce,
            String algoritmoAssinatura,
            String motivo
    ) {
        public DesafioResponse {
            Objects.requireNonNull(status);
            nonce = nonce == null ? "" : nonce;
            algoritmoAssinatura = algoritmoAssinatura == null ? "" : algoritmoAssinatura;
            motivo = motivo == null ? "" : motivo;
        }
    }

    public sealed interface Resposta permits AutenticadoResponse, PendenteSelecaoResponse, NegadoResponse {
        Status status();
    }

    public record AutenticadoResponse(
            Status status,
            String token,
            LocalDateTime expiresAt,
            ContextoResponse contexto,
            boolean termosPendentes
    ) implements Resposta {
        public AutenticadoResponse {
            Objects.requireNonNull(status);
            token = token == null ? "" : token;
            Objects.requireNonNull(contexto);
        }
    }

    public record PendenteSelecaoResponse(
            Status status,
            List<LotacaoResponse> lotacoes
    ) implements Resposta {
        public PendenteSelecaoResponse {
            Objects.requireNonNull(status);
            lotacoes = List.copyOf(lotacoes == null ? List.of() : lotacoes);
        }
    }

    public record NegadoResponse(
            Status status,
            String motivo
    ) implements Resposta {
        public NegadoResponse {
            Objects.requireNonNull(status);
            motivo = motivo == null ? "" : motivo;
        }
    }

    public record ContextoResponse(
            Long unidadeId,
            String unidadeNome,
            String unidadeTipo,
            String papelNaUnidade
    ) {
        public ContextoResponse {
            unidadeNome = unidadeNome == null ? "" : unidadeNome;
            unidadeTipo = unidadeTipo == null ? "" : unidadeTipo;
            papelNaUnidade = papelNaUnidade == null ? "" : papelNaUnidade;
        }
    }

    public record LotacaoResponse(
            Long lotacaoId,
            Long unidadeId,
            String unidadeNome,
            String unidadeTipo,
            String papelNaUnidade
    ) {
        public LotacaoResponse {
            unidadeNome = unidadeNome == null ? "" : unidadeNome;
            unidadeTipo = unidadeTipo == null ? "" : unidadeTipo;
            papelNaUnidade = papelNaUnidade == null ? "" : papelNaUnidade;
        }
    }
}
