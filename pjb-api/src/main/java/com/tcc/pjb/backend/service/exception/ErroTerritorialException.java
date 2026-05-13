package com.tcc.pjb.backend.service.exception;

import java.io.Serial;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import com.tcc.pjb.backend.service.exception.enums.TipoViolacaoTerritorial;
import lombok.Getter;

@Getter
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class ErroTerritorialException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String idIncidente;
    private final TipoViolacaoTerritorial tipo;
    private final String fundamentacaoLegal;
    private final String sugestaoCorrecao;
    private final String territorialMode;
    private final String tribunalCodigoSugerido;
    private final String unidadeJudiciariaSugerida;
    private final String comarcaInformada;
    private final String comarcaSugerida;
    private final String ufInformada;
    private final String ufSugerida;
    private final String varaInformada;
    private final String varaSugerida;
    private final String foroInformado;
    private final String foroSugerido;
    private final List<String> alertas;
    private final List<String> checklist;
    private final LocalDateTime timestamp;
    private final String provaIntegridade;
    private final boolean bloqueante;

    public ErroTerritorialException(Builder builder) {
        super(buildMessage(builder));
        this.idIncidente = UUID.randomUUID().toString();
        this.tipo = builder.tipo;
        this.fundamentacaoLegal = builder.fundamentacaoLegal;
        this.sugestaoCorrecao = builder.sugestaoCorrecao;
        this.territorialMode = builder.territorialMode;
        this.tribunalCodigoSugerido = builder.tribunalCodigoSugerido;
        this.unidadeJudiciariaSugerida = builder.unidadeJudiciariaSugerida;
        this.comarcaInformada = builder.comarcaInformada;
        this.comarcaSugerida = builder.comarcaSugerida;
        this.ufInformada = builder.ufInformada;
        this.ufSugerida = builder.ufSugerida;
        this.varaInformada = builder.varaInformada;
        this.varaSugerida = builder.varaSugerida;
        this.foroInformado = builder.foroInformado;
        this.foroSugerido = builder.foroSugerido;
        this.alertas = builder.alertas == null ? List.of() : List.copyOf(builder.alertas);
        this.checklist = builder.checklist == null ? List.of() : List.copyOf(builder.checklist);
        this.timestamp = LocalDateTime.now();
        this.bloqueante = builder.bloqueante;
        this.provaIntegridade = Integer.toHexString((
                idIncidente +
                        (tipo != null ? tipo.name() : "") +
                        fundamentacaoLegal +
                        sugestaoCorrecao +
                        territorialMode +
                        tribunalCodigoSugerido +
                        unidadeJudiciariaSugerida +
                        comarcaInformada +
                        comarcaSugerida +
                        ufInformada +
                        ufSugerida +
                        varaInformada +
                        varaSugerida +
                        foroInformado +
                        foroSugerido +
                        alertas +
                        checklist +
                        timestamp +
                        bloqueante
        ).hashCode());
    }

    public String getCodigoTipo() {
        return tipo != null ? tipo.getCodigo() : null;
    }

    private static String buildMessage(Builder builder) {
        StringBuilder sb = new StringBuilder();
        sb.append(builder.tipo != null ? builder.tipo.getTituloJuridico() : "Inconsistência territorial").append('.');
        if (builder.fundamentacaoLegal != null && !builder.fundamentacaoLegal.isBlank()) {
            sb.append(' ').append(builder.fundamentacaoLegal.trim());
        }
        if (builder.comarcaInformada != null && builder.comarcaSugerida != null && !builder.comarcaInformada.equalsIgnoreCase(builder.comarcaSugerida)) {
            sb.append(" Comarca informada: ").append(builder.comarcaInformada).append('.');
            sb.append(" Comarca sugerida: ").append(builder.comarcaSugerida).append('.');
        }
        if (builder.ufInformada != null && builder.ufSugerida != null && !builder.ufInformada.equalsIgnoreCase(builder.ufSugerida)) {
            sb.append(" UF informada: ").append(builder.ufInformada).append('.');
            sb.append(" UF sugerida: ").append(builder.ufSugerida).append('.');
        }
        if (builder.sugestaoCorrecao != null && !builder.sugestaoCorrecao.isBlank()) {
            sb.append(' ').append(builder.sugestaoCorrecao.trim());
        }
        return sb.toString().trim();
    }

    public static class Builder {
        private final TipoViolacaoTerritorial tipo;
        private String fundamentacaoLegal;
        private String sugestaoCorrecao = "Revise os elementos territoriais e a unidade competente antes de prosseguir.";
        private String territorialMode;
        private String tribunalCodigoSugerido;
        private String unidadeJudiciariaSugerida;
        private String comarcaInformada;
        private String comarcaSugerida;
        private String ufInformada;
        private String ufSugerida;
        private String varaInformada;
        private String varaSugerida;
        private String foroInformado;
        private String foroSugerido;
        private List<String> alertas = List.of();
        private List<String> checklist = List.of();
        private boolean bloqueante = true;

        public Builder(TipoViolacaoTerritorial tipo) {
            this.tipo = tipo;
        }

        public Builder fundamento(String value) {
            this.fundamentacaoLegal = value;
            return this;
        }

        public Builder sugestao(String value) {
            if (value != null && !value.isBlank()) {
                this.sugestaoCorrecao = value.trim();
            }
            return this;
        }

        public Builder territorialMode(String value) {
            this.territorialMode = value;
            return this;
        }

        public Builder tribunalCodigoSugerido(String value) {
            this.tribunalCodigoSugerido = value;
            return this;
        }

        public Builder unidadeJudiciariaSugerida(String value) {
            this.unidadeJudiciariaSugerida = value;
            return this;
        }

        public Builder comarcaInformada(String value) {
            this.comarcaInformada = value;
            return this;
        }

        public Builder comarcaSugerida(String value) {
            this.comarcaSugerida = value;
            return this;
        }

        public Builder ufInformada(String value) {
            this.ufInformada = value;
            return this;
        }

        public Builder ufSugerida(String value) {
            this.ufSugerida = value;
            return this;
        }

        public Builder varaInformada(String value) {
            this.varaInformada = value;
            return this;
        }

        public Builder varaSugerida(String value) {
            this.varaSugerida = value;
            return this;
        }

        public Builder foroInformado(String value) {
            this.foroInformado = value;
            return this;
        }

        public Builder foroSugerido(String value) {
            this.foroSugerido = value;
            return this;
        }

        public Builder alertas(List<String> value) {
            this.alertas = value == null ? List.of() : List.copyOf(value);
            return this;
        }

        public Builder checklist(List<String> value) {
            this.checklist = value == null ? List.of() : List.copyOf(value);
            return this;
        }

        public Builder bloqueante(boolean value) {
            this.bloqueante = value;
            return this;
        }

        public ErroTerritorialException build() {
            return new ErroTerritorialException(this);
        }
    }
}
