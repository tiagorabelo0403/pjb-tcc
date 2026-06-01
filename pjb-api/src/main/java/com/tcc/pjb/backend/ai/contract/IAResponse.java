package com.tcc.pjb.backend.ai.contract;

import java.util.Collections;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import com.tcc.pjb.backend.ai.provenance.EvidenceItem;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

@Getter
@Builder(toBuilder = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class IAResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public enum StatusIA {
        SUCESSO,
        ALERTA,
        INDETERMINADO,
        ERRO
    }

    private final String origem;
    private final String texto;
    private final StatusIA status;
    private final Double confianca;
    private final Instant dataGeracao;

    @Singular("alerta")
    private final List<String> alertasCriticos;

    @Schema(description = "Metadados técnicos do pipeline de IA — estrutura varia por versão (v1/v2/v3) e domínio (jurídica/financeira)")
    @Size(max = 50)
    @Singular("metadado")
    private final Map<String, Object> metadados;

    @Schema(hidden = true)
    @Builder.Default
    private final Map<String, Object> essence = Collections.emptyMap();

    
    @Singular("evidencia")
    private final List<EvidenceItem> evidencias;

    

    public IAResponse adicionarTexto(String adicional) {
        if (adicional == null || adicional.isBlank()) {
            return this;
        }
        String novoTexto = (this.texto == null ? "" : this.texto) + adicional;
        return this.toBuilder().texto(novoTexto).build();
    }

    public IAResponse adicionarAlerta(String alerta) {
        if (alerta == null || alerta.isBlank()) {
            return this;
        }

        List<String> novos = new ArrayList<>(
                Optional.ofNullable(this.alertasCriticos)
                        .orElse(Collections.emptyList())
        );
        novos.add(alerta);

        return this.toBuilder()
                .alertasCriticos(Collections.unmodifiableList(novos))
                .build();
    }

    public IAResponse adicionarMetadados(Map<String, Object> novos) {
        if (novos == null || novos.isEmpty()) {
            return this;
        }

        Map<String, Object> mapa = new HashMap<>(
                Optional.ofNullable(this.metadados)
                        .orElse(Collections.emptyMap())
        );
        mapa.putAll(novos);

        return this.toBuilder()
                .metadados(Collections.unmodifiableMap(mapa))
                .build();
    }


    public List<String> getAlertas() {
        return alertasCriticos;
    }

    public static class IAResponseBuilder {
        public IAResponseBuilder adicionarAlerta(String alerta) {
            return alerta(alerta);
        }
    }

    public IAResponse adicionarEvidencias(List<EvidenceItem> novas) {
        if (novas == null || novas.isEmpty()) {
            return this;
        }
        List<EvidenceItem> lista = new ArrayList<>(
                Optional.ofNullable(this.evidencias)
                        .orElse(Collections.emptyList())
        );
        lista.addAll(novas);
        return this.toBuilder()
                .evidencias(Collections.unmodifiableList(lista))
                .build();
    }
}
