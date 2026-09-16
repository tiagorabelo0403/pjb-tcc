package com.tcc.pjb.backend.ai.contract;

import java.util.ArrayList;
import java.util.Collections;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import com.tcc.pjb.backend.ai.provenance.EvidenceItem;

/**
 * Builder/getters escritos a mao (nao Lombok) -- @Builder(toBuilder=true) + @Singular nesta classe
 * parava de gerar toBuilder()/alerta()/metadado() apos a migracao Boot 4, sem causa raiz isolavel em
 * tempo habil (Lombok 1.18.48 processa o mesmo shape corretamente isolado, so falha dentro da
 * compilacao completa do projeto). API publica identica ao que o Lombok gerava, ver DEBT_LOG.
 */
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
    private final List<String> alertasCriticos;

    @Schema(description = "Metadados técnicos do pipeline de IA — estrutura varia por versão (v1/v2/v3) e domínio (jurídica/financeira)")
    @Size(max = 50)
    private final Map<String, Object> metadados;

    @Schema(hidden = true)
    private final Map<String, Object> essence;

    private final List<EvidenceItem> evidencias;

    private IAResponse(IAResponseBuilder builder) {
        this.origem = builder.origem;
        this.texto = builder.texto;
        this.status = builder.status;
        this.confianca = builder.confianca;
        this.dataGeracao = builder.dataGeracao;
        this.alertasCriticos = builder.alertasCriticos == null
                ? null
                : Collections.unmodifiableList(new ArrayList<>(builder.alertasCriticos));
        this.metadados = builder.metadados == null
                ? null
                : Collections.unmodifiableMap(new LinkedHashMap<>(builder.metadados));
        this.essence = builder.essenceSet ? builder.essence : Collections.emptyMap();
        this.evidencias = builder.evidencias == null
                ? null
                : Collections.unmodifiableList(new ArrayList<>(builder.evidencias));
    }

    public static IAResponseBuilder builder() {
        return new IAResponseBuilder();
    }

    public IAResponseBuilder toBuilder() {
        IAResponseBuilder b = new IAResponseBuilder();
        b.origem = this.origem;
        b.texto = this.texto;
        b.status = this.status;
        b.confianca = this.confianca;
        b.dataGeracao = this.dataGeracao;
        b.alertasCriticos = this.alertasCriticos == null ? null : new ArrayList<>(this.alertasCriticos);
        b.metadados = this.metadados == null ? null : new LinkedHashMap<>(this.metadados);
        b.essence = this.essence;
        b.essenceSet = true;
        b.evidencias = this.evidencias == null ? null : new ArrayList<>(this.evidencias);
        return b;
    }

    public String getOrigem() {
        return origem;
    }

    public String getTexto() {
        return texto;
    }

    public StatusIA getStatus() {
        return status;
    }

    public Double getConfianca() {
        return confianca;
    }

    public Instant getDataGeracao() {
        return dataGeracao;
    }

    public List<String> getAlertasCriticos() {
        return alertasCriticos;
    }

    public Map<String, Object> getMetadados() {
        return metadados;
    }

    public Map<String, Object> getEssence() {
        return essence;
    }

    public List<EvidenceItem> getEvidencias() {
        return evidencias;
    }

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

    public static final class IAResponseBuilder {
        private String origem;
        private String texto;
        private StatusIA status;
        private Double confianca;
        private Instant dataGeracao;
        private List<String> alertasCriticos;
        private Map<String, Object> metadados;
        private Map<String, Object> essence = Collections.emptyMap();
        private boolean essenceSet;
        private List<EvidenceItem> evidencias;

        private IAResponseBuilder() {
        }

        public IAResponseBuilder origem(String origem) {
            this.origem = origem;
            return this;
        }

        public IAResponseBuilder texto(String texto) {
            this.texto = texto;
            return this;
        }

        public IAResponseBuilder status(StatusIA status) {
            this.status = status;
            return this;
        }

        public IAResponseBuilder confianca(Double confianca) {
            this.confianca = confianca;
            return this;
        }

        public IAResponseBuilder dataGeracao(Instant dataGeracao) {
            this.dataGeracao = dataGeracao;
            return this;
        }

        public IAResponseBuilder alerta(String alerta) {
            if (this.alertasCriticos == null) {
                this.alertasCriticos = new ArrayList<>();
            }
            this.alertasCriticos.add(alerta);
            return this;
        }

        public IAResponseBuilder alertasCriticos(Collection<? extends String> alertas) {
            if (this.alertasCriticos == null) {
                this.alertasCriticos = new ArrayList<>();
            } else {
                this.alertasCriticos.clear();
            }
            if (alertas != null) {
                this.alertasCriticos.addAll(alertas);
            }
            return this;
        }

        public IAResponseBuilder metadado(String chave, Object valor) {
            if (this.metadados == null) {
                this.metadados = new LinkedHashMap<>();
            }
            this.metadados.put(chave, valor);
            return this;
        }

        public IAResponseBuilder metadados(Map<? extends String, ?> metadados) {
            if (this.metadados == null) {
                this.metadados = new LinkedHashMap<>();
            } else {
                this.metadados.clear();
            }
            if (metadados != null) {
                this.metadados.putAll(metadados);
            }
            return this;
        }

        public IAResponseBuilder essence(Map<String, Object> essence) {
            this.essence = essence;
            this.essenceSet = true;
            return this;
        }

        public IAResponseBuilder evidencia(EvidenceItem evidencia) {
            if (this.evidencias == null) {
                this.evidencias = new ArrayList<>();
            }
            this.evidencias.add(evidencia);
            return this;
        }

        public IAResponseBuilder evidencias(Collection<? extends EvidenceItem> evidencias) {
            if (this.evidencias == null) {
                this.evidencias = new ArrayList<>();
            } else {
                this.evidencias.clear();
            }
            if (evidencias != null) {
                this.evidencias.addAll(evidencias);
            }
            return this;
        }

        public IAResponse build() {
            return new IAResponse(this);
        }
    }
}
