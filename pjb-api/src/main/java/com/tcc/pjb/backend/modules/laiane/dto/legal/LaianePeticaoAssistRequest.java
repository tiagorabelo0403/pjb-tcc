package com.tcc.pjb.backend.modules.laiane.dto.legal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaianePeticaoAssistRequest {

    private String kind;

    @Builder.Default
    private Map<String, Object> ctx = new LinkedHashMap<>();

    private String draftMarkdown;
    private Long processoId;
    private String nupn;
    private String classeTpu;
    private String assuntoTpu;
    private String ramoDireito;
    private BigDecimal valorCausa;
    private String ufAutor;
    private String comarcaAutor;
    private String ufReu;
    private String comarcaReu;
    private Boolean requerJuizadoEspecial;
    private Boolean requerVaraEspecializada;
    private String materiaPrincipal;
    private String tipoJustica;
    private Boolean casoUrgente;
    private Boolean preferenciaDigital;
    private String textoFatosResumido;
    private String cpfCnpjAutor;
    private String cpfCnpjReu;
    private String oabAdvogado;
    private String ufAdvogado;
    private String escritorioOab;

    @Builder.Default
    private List<String> documentosAnexados = new ArrayList<>();

    private LocalDate dataFatoGerador;
    private Boolean requerLiminar;
    private Boolean atoJurisdicionalAnterior;
    private String ritoSugerido;
    private String protocolTitle;
}
