package com.tcc.pjb.backend.modules.advocacia.dto;

import java.time.Duration;
import java.time.LocalDateTime;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tcc.pjb.backend.modules.advocacia.enums.StatusCliente;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ClienteDTO {

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "Requisição para cadastro ou atualização de cliente no sistema jurídico")
    public static class ClienteRequest {

        @NotNull(message = "O ID do advogado é obrigatório.")
        @Schema(example = "123")
        private Long advogadoId;

        @NotBlank(message = "O nome completo é obrigatório.")
        @Size(min = 3, max = 150, message = "O nome deve ter entre 3 e 150 caracteres.")
        @Schema(example = "Ocinária Rabelo Lima")
        private String nomeCompleto;

        @Size(max = 32, message = "CPF/CNPJ muito longo.")
        @Schema(example = "123.456.789-00")
        private String cpfCnpj;

        






        @AssertTrue(message = "O CPF/CNPJ deve conter 11 (CPF) ou 14 (CNPJ) dígitos.")
        public boolean isCpfCnpjCom11ou14Digitos() {
            if (cpfCnpj == null || cpfCnpj.isBlank()) return true;
            String digits = cpfCnpj.replaceAll("\\D+", "");
            return digits.length() == 11 || digits.length() == 14;
        }

        @Email(message = "E-mail inválido.")
        @Schema(example = "ocinaria.lima@exemplo.com")
        private String email;

        @Pattern(
                regexp = "^(\\+?\\d{0,3})?\\s?\\(?\\d{2}\\)?\\s?\\d{4,5}-?\\d{4}$",
                message = "Telefone inválido. Use o formato (DDD) 99999-9999."
        )
        @Schema(example = "(85) 99999-9999")
        private String telefone;

        @Schema(example = "Rua Maria de Lourdes Terceiro Chagas, nº 100, Bairro 02 de Agosto, Morada Nova/CE")
        private String endereco;

        @Size(max = 500, message = "As observações devem conter no máximo 500 caracteres.")
        @Schema(example = "Cliente envolvido em ação de direito de vizinhança.")
        private String observacoes;
    }
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "Dados completos do cliente retornados pelo sistema jurídico")
    public static class ClienteResponse {

        private Long id;
        private String nomeCompleto;

        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        private String cpfCnpj;

        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        private String email;
        private String telefone;
        private String endereco;
        private String observacoes;
        private StatusCliente status;
        private LocalDateTime dataCriacao;
        private LocalDateTime dataAtualizacao;
        private Double nivelConfiabilidade;
        private LocalDateTime ultimaAnaliseIa;
        private Long advogadoId;
        private String advogadoNome;

        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        public String getCpfCnpj() {
            return cpfCnpj;
        }

        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        public String getEmail() {
            return email;
        }

        @Schema(description = "Descrição textual do status (ATIVO → Cliente ativo no sistema)")
        public String getStatusDescricao() {
            if (status == null) return "Não definido";
            return switch (status) {
                case ATIVO -> "Cliente ativo no sistema";
                case INATIVO -> "Cliente inativo";
                case SUSPENSO -> "Cliente com restrição temporária";
                case EM_ANALISE -> "Cliente em processo de verificação";
                case ARQUIVADO -> "Cliente arquivado";
                default -> "Status desconhecido";
            };
        }

        @Schema(description = "Tempo total desde o cadastro (em dias)")
        public Long getTempoDeCadastroDias() {
            if (dataCriacao == null) return null;
            return Duration.between(dataCriacao, LocalDateTime.now()).toDays();
        }

        @Schema(description = "CPF ou CNPJ parcialmente mascarado para segurança")
        public String getCpfCnpjMascarado() {
            if (cpfCnpj == null) return null;
            return switch (cpfCnpj.length()) {
                case 11 -> cpfCnpj.replaceAll("(\\d{3})\\d{5}(\\d{3})", "$1*****$2");
                case 14 -> cpfCnpj.replaceAll("(\\d{3})\\d{8}(\\d{3})", "$1********$2");
                default -> cpfCnpj;
            };
        }

        @Schema(description = "E-mail parcialmente mascarado para privacidade")
        public String getEmailMascarado() {
            if (email == null || !email.contains("@")) return email;
            String[] partes = email.split("@");
            return partes[0].substring(0, Math.min(2, partes[0].length())) + "*****@" + partes[1];
        }
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "Filtros de pesquisa avançada de clientes")
    public static class ClienteQuery {

        @Schema(description = "ID do advogado responsável pelo cliente")
        private Long advogadoId;

        @Schema(description = "Nome completo ou parte do nome do cliente")
        private String nome;

        @Schema(description = "CPF ou CNPJ do cliente (somente números)")
        private String cpfCnpj;

        @Schema(description = "Status atual do cliente no sistema")
        private StatusCliente status;

        @Schema(description = "Data mínima de cadastro para o filtro de busca")
        private LocalDateTime cadastradoDepois;
    }
}
