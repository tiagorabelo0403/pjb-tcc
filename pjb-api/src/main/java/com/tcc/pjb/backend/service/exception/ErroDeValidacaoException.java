package com.tcc.pjb.backend.service.exception;

import java.io.Serial;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tcc.pjb.backend.service.exception.enums.TipoErroValidacao;
import lombok.Getter;

@Getter
@ResponseStatus(HttpStatus.BAD_REQUEST)
@JsonInclude(JsonInclude.Include.NON_NULL) 
@JsonIgnoreProperties({"stackTrace", "cause", "suppressed", "localizedMessage"}) 
public class ErroDeValidacaoException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonProperty("id_incidente") 
    private final String idErro;

    @JsonProperty("codigo_erro")
    private final String codigoErro; 

    @JsonProperty("titulo")
    private final String titulo;

    @JsonProperty("momento")
    private final LocalDateTime timestamp;

    @JsonProperty("detalhes_tecnicos")
    private final Map<String, Object> metadados;

    
    public ErroDeValidacaoException(TipoErroValidacao tipo, String detalheAdicional) {
        super(tipo.getDescricaoPadrao() + ". " + detalheAdicional);
        this.idErro = UUID.randomUUID().toString();
        this.codigoErro = tipo.getCodigo();
        this.titulo = tipo.getDescricaoPadrao();
        this.timestamp = LocalDateTime.now();
        this.metadados = new HashMap<>();

        
        if (detalheAdicional != null && !detalheAdicional.isBlank()) {
            this.metadados.put("mensagem_usuario", detalheAdicional);
        }
    }

    
    public ErroDeValidacaoException(TipoErroValidacao tipo, String detalheAdicional, Throwable cause) {
        super(tipo.getDescricaoPadrao() + ". " + detalheAdicional, cause);
        this.idErro = UUID.randomUUID().toString();
        this.codigoErro = tipo.getCodigo();
        this.titulo = tipo.getDescricaoPadrao();
        this.timestamp = LocalDateTime.now();
        this.metadados = new HashMap<>();

        this.metadados.put("mensagem_usuario", detalheAdicional);
        
        this.metadados.put("causa_raiz", cause.getClass().getSimpleName());
    }

    
    public ErroDeValidacaoException addMetadado(String chave, Object valor) {
        this.metadados.put(chave, valor);
        return this;
    }

    
    @Override
    public String toString() {
        return String.format("[VALIDACAO-ERROR] ID=%s | Code=%s | Msg=%s | Meta=%s",
                idErro, codigoErro, getMessage(), metadados);
    }
}