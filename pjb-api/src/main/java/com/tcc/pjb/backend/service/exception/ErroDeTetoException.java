package com.tcc.pjb.backend.service.exception;

import java.io.Serial;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import com.tcc.pjb.backend.service.exception.enums.TipoViolacaoTeto;
import lombok.Getter;

@Getter
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class ErroDeTetoException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String idIncidente;
    private final TipoViolacaoTeto tipo;
    private final String fundamentacaoLegal;
    private final Map<String, Object> calculoDetalhado;
    private final String sugestaoCorrecao;
    private final LocalDateTime timestamp;
    private final String provaIntegridade;
    private final Integer anoReferencia;
    private final BigDecimal salarioMinimoReferencia;
    private final BigDecimal limiteLegal;
    private final BigDecimal valorInformado;
    private final BigDecimal excedente;
    private final BigDecimal percentualExcedente;
    private final String competenciaSugerida;
    private final String ritoSugerido;
    private final boolean bloqueante;

    public ErroDeTetoException(Builder builder) {
        super(montarMensagem(builder));
        this.idIncidente = UUID.randomUUID().toString();
        this.tipo = builder.tipo;
        this.fundamentacaoLegal = builder.fundamentacaoLegal;
        this.calculoDetalhado = Map.copyOf(builder.dadosMatematicos);
        this.sugestaoCorrecao = builder.sugestao;
        this.timestamp = LocalDateTime.now();
        this.anoReferencia = builder.anoReferencia;
        this.salarioMinimoReferencia = normalize(builder.salarioMinimoReferencia);
        this.limiteLegal = normalize(builder.limiteLegal);
        this.valorInformado = normalize(builder.valorInformado);
        this.excedente = normalize(builder.excedente);
        this.percentualExcedente = normalize(builder.percentualExcedente);
        this.competenciaSugerida = builder.competenciaSugerida;
        this.ritoSugerido = builder.ritoSugerido;
        this.bloqueante = builder.bloqueante;
        this.provaIntegridade = gerarHashLogico();
    }

    public ErroDeTetoException(String message) {
        super(message);
        this.idIncidente = UUID.randomUUID().toString();
        this.tipo = TipoViolacaoTeto.RITO_INCOMPATIVEL;
        this.fundamentacaoLegal = message == null ? "Validacao economica processual" : message;
        this.calculoDetalhado = Map.of("mensagem", this.fundamentacaoLegal);
        this.sugestaoCorrecao = "Revise valor da causa, competencia e rito antes de prosseguir.";
        this.timestamp = LocalDateTime.now();
        this.anoReferencia = null;
        this.salarioMinimoReferencia = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.limiteLegal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.valorInformado = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.excedente = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.percentualExcedente = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.competenciaSugerida = null;
        this.ritoSugerido = null;
        this.bloqueante = true;
        this.provaIntegridade = gerarHashLogico();
    }

    public String getCodigoTipo() {
        return tipo != null ? tipo.getCodigo() : null;
    }

    private String gerarHashLogico() {
        return Integer.toHexString((
                idIncidente +
                        (tipo != null ? tipo.name() : "") +
                        fundamentacaoLegal +
                        calculoDetalhado +
                        sugestaoCorrecao +
                        timestamp +
                        anoReferencia +
                        salarioMinimoReferencia +
                        limiteLegal +
                        valorInformado +
                        excedente +
                        percentualExcedente +
                        competenciaSugerida +
                        ritoSugerido +
                        bloqueante
        ).hashCode());
    }

    private static String montarMensagem(Builder builder) {
        StringBuilder sb = new StringBuilder();
        sb.append(builder.tipo.getTituloJuridico()).append(". ");
        sb.append(builder.fundamentacaoLegal);
        if (builder.limiteLegal != null && builder.valorInformado != null && builder.limiteLegal.compareTo(BigDecimal.ZERO) > 0) {
            NumberFormat nf = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"));
            sb.append(" Valor informado: ").append(nf.format(builder.valorInformado));
            sb.append(". Limite aplicavel: ").append(nf.format(builder.limiteLegal)).append('.');
        }
        if (builder.competenciaSugerida != null && !builder.competenciaSugerida.isBlank()) {
            sb.append(" Competencia sugerida: ").append(builder.competenciaSugerida).append('.');
        }
        if (builder.ritoSugerido != null && !builder.ritoSugerido.isBlank()) {
            sb.append(" Rito sugerido: ").append(builder.ritoSugerido).append('.');
        }
        sb.append(' ').append(builder.sugestao);
        return sb.toString().trim();
    }

    private static BigDecimal normalize(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : value.setScale(2, RoundingMode.HALF_UP);
    }

    public static class Builder {

        private final TipoViolacaoTeto tipo;
        private String fundamentacaoLegal = "Nao especificada";
        private String sugestao = "Revise o enquadramento economico processual.";
        private final Map<String, Object> dadosMatematicos = new LinkedHashMap<>();
        private Integer anoReferencia;
        private BigDecimal salarioMinimoReferencia;
        private BigDecimal limiteLegal;
        private BigDecimal valorInformado;
        private BigDecimal excedente;
        private BigDecimal percentualExcedente;
        private String competenciaSugerida;
        private String ritoSugerido;
        private boolean bloqueante = true;

        public Builder(TipoViolacaoTeto tipo) {
            if (tipo == null) {
                throw new IllegalStateException("Tipo de violacao de teto obrigatorio.");
            }
            this.tipo = tipo;
        }

        public Builder fundamento(String fundamento) {
            if (fundamento == null || fundamento.isBlank()) {
                throw new IllegalArgumentException("Fundamentacao legal invalida.");
            }
            this.fundamentacaoLegal = fundamento.strip();
            return this;
        }

        public Builder anoReferencia(Integer anoReferencia) {
            this.anoReferencia = anoReferencia;
            return this;
        }

        public Builder salarioMinimoReferencia(BigDecimal salarioMinimoReferencia) {
            this.salarioMinimoReferencia = salarioMinimoReferencia;
            return this;
        }

        public Builder competenciaSugerida(String competenciaSugerida) {
            this.competenciaSugerida = competenciaSugerida;
            return this;
        }

        public Builder ritoSugerido(String ritoSugerido) {
            this.ritoSugerido = ritoSugerido;
            return this;
        }

        public Builder bloqueante(boolean bloqueante) {
            this.bloqueante = bloqueante;
            return this;
        }

        public Builder matematica(String chave, Object valor) {
            if (chave != null && !chave.isBlank()) {
                this.dadosMatematicos.put(chave, valor);
            }
            return this;
        }

        public Builder calculoFinanceiro(BigDecimal limite, BigDecimal informado) {
            if (limite == null || informado == null) {
                throw new IllegalArgumentException("Limite e valor informado sao obrigatorios.");
            }
            if (limite.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalStateException("Limite legal deve ser maior que zero.");
            }
            this.limiteLegal = limite.abs().setScale(2, RoundingMode.HALF_UP);
            this.valorInformado = informado.abs().setScale(2, RoundingMode.HALF_UP);
            this.excedente = valorInformado.compareTo(limiteLegal) > 0
                    ? valorInformado.subtract(limiteLegal).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            this.percentualExcedente = excedente.compareTo(BigDecimal.ZERO) > 0
                    ? excedente.divide(limiteLegal, 6, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            NumberFormat nf = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"));
            dadosMatematicos.put("limiteLegal", nf.format(this.limiteLegal));
            dadosMatematicos.put("valorInformado", nf.format(this.valorInformado));
            dadosMatematicos.put("excedente", nf.format(this.excedente));
            dadosMatematicos.put("percentualExcedente", this.percentualExcedente + "%");
            return this;
        }

        public Builder sugestao(String sugestao) {
            if (sugestao != null && !sugestao.isBlank()) {
                this.sugestao = sugestao.strip();
            }
            return this;
        }

        public ErroDeTetoException build() {
            if (fundamentacaoLegal.equalsIgnoreCase("Nao especificada")) {
                throw new IllegalStateException("Fundamentacao legal obrigatoria.");
            }
            if (limiteLegal != null && valorInformado != null && dadosMatematicos.isEmpty()) {
                calculoFinanceiro(limiteLegal, valorInformado);
            }
            return new ErroDeTetoException(this);
        }
    }
}
