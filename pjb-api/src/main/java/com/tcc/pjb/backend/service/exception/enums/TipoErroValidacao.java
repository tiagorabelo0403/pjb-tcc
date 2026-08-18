package com.tcc.pjb.backend.service.exception.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TipoErroValidacao {

    
    ARQUIVO_CORROMPIDO("VAL-101", "O arquivo PDF está corrompido ou ilegível."),
    FORMATO_INVALIDO("VAL-102", "Formato não suportado. Apenas PDF/A é aceito."),
    TAMANHO_EXCEDIDO("VAL-103", "O arquivo excede o limite de tamanho permitido (5MB)."),
    ARQUIVO_PROTEGIDO("VAL-104", "O arquivo possui senha ou restrições de DRM."),
    NOME_ARQUIVO_AUSENTE("VAL-105", "Nome do arquivo ausente ou vazio."),
    NOME_ARQUIVO_DUPLICADO("VAL-106", "Nomes de arquivo duplicados detectados."),
    CORRELACAO_ARQUIVO_TIPO("VAL-107", "Incompatibilidade entre arquivos enviados e tipos declarados."),


    CPF_INVALIDO("VAL-201", "O CPF informado não passa na validação da Receita Federal."),
    EMAIL_INVALIDO("VAL-202", "Formato de e-mail incorreto."),
    DATA_FUTURA("VAL-203", "A data informada não pode ser futura."),
    CAMPO_OBRIGATORIO("VAL-204", "Campo obrigatório ausente ou inválido."),
    CNPJ_INVALIDO("VAL-205", "O CNPJ informado não passa na validação dos dígitos verificadores."),
    DOCUMENTO_INVALIDO("VAL-206", "O documento deve ter 11 dígitos (CPF) ou 14 dígitos (CNPJ)."),


    DUPLICIDADE_DETECTADA("VAL-301", "Registro duplicado detectado no banco de dados."),
    VINCULO_INVALIDO("VAL-302", "A entidade referenciada não pertence ao contexto atual."),

    
    REGRA_NEGOCIO("VAL-399", "Regra de negócio violada.");

    private final String codigo;
    private final String descricaoPadrao;
}