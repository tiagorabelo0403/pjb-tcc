package com.tcc.pjb.backend.model.dto.processual.peticionamento.session;

import com.tcc.pjb.backend.model.dto.processual.peticionamento.media.PeticionamentoMediaBlocoRequest;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tcc.pjb.backend.model.dto.advogado.LaianePeticaoInicialEstruturarRequest;
import com.tcc.pjb.backend.modules.laiane.dto.legal.LaianePeticaoAssistRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
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
public class PeticionamentoSessaoRequest {
    @Size(max = 32)
    private String modo;
    private Long processoId;
    @Size(max = 240)
    private String tituloCaso;
    @Size(max = 320)
    private String parteAutora;
    @Size(max = 320)
    private String parteRe;
    @Size(max = 120)
    private String ramoDireito;
    @Size(max = 120)
    private String ritoProcessual;
    @Size(max = 120)
    private String classeProcessual;
    @Size(max = 120)
    private String assuntoTpu;
    @Size(max = 160)
    private String materiaPrincipal;
    @Size(max = 40)
    private String tipoJustica;
    @Size(max = 8000)
    private String textoFatosResumido;
    @Digits(integer = 18, fraction = 2)
    private BigDecimal valorCausa;
    private Boolean tutelaUrgencia;
    private Boolean casoUrgente;
    private Boolean preferenciaDigital;
    private Boolean requerLiminar;
    private Boolean requerJuizadoEspecial;
    private Boolean requerVaraEspecializada;
    @Size(max = 120000)
    private String draftMarkdown;
    @Size(max = 120000)
    private String textoPeticaoLivre;
    @Size(max = 160)
    private String cidadeFato;
    @Pattern(regexp = "^$|^[A-Za-z]{2}$")
    private String ufFato;
    @Size(max = 160)
    private String cidadeProtocolo;
    @Pattern(regexp = "^$|^[A-Za-z]{2}$")
    private String ufProtocolo;
    @Size(max = 160)
    private String naturezaJuridica;
    @Size(max = 240)
    private String protocolTitle;
    @Size(max = 120)
    private String tipoInstrumentoRepresentacao;
    private Long audienciaId;
    @Size(max = 120)
    private String tipoAudiencia;
    private Boolean contextoConsensual;
    private Boolean poderesEspeciaisTransigir;
    @Size(max = 240)
    private String termoAudienciaReferencia;
    @Size(max = 240)
    private String ataAudienciaReferencia;
    private Boolean prepararPacoteProtocolo;
    private Boolean resolverEnderecoAutomaticamente;
    @Builder.Default
    @Size(max = 96)
    private List<@Size(max = 2000) String> fatos = new ArrayList<>();
    @Builder.Default
    @Size(max = 96)
    private List<@Size(max = 2000) String> fundamentosJuridicos = new ArrayList<>();
    @Builder.Default
    @Size(max = 96)
    private List<@Size(max = 2000) String> pedidos = new ArrayList<>();
    @Builder.Default
    @Size(max = 96)
    private List<@Size(max = 2000) String> provasIndicadas = new ArrayList<>();
    @Builder.Default
    @Size(max = 96)
    private List<@Size(max = 260) String> documentosAnexados = new ArrayList<>();
    @Valid
    @Builder.Default
    @Size(max = 64)
    private List<PeticionamentoMediaBlocoRequest> midiaInline = new ArrayList<>();
    @Builder.Default
    @Size(max = 96)
    private List<@Size(max = 260) String> provasDocumentais = new ArrayList<>();
    @Builder.Default
    @Size(max = 96)
    private List<@Size(max = 260) String> documentosPessoais = new ArrayList<>();
    @Builder.Default
    @Size(max = 96)
    private List<@Size(max = 260) String> documentosRepresentacao = new ArrayList<>();
    @Builder.Default
    private Map<String, Object> ctx = new LinkedHashMap<>();
    @Valid
    private PeticionamentoEnderecoRequest enderecoAutor;
    @Valid
    private PeticionamentoEnderecoRequest enderecoReu;
    @Valid
    private PeticionamentoVisualIdentityRequest identidadeVisual;

    @JsonIgnore
    public PeticionamentoModo modoResolvido() {
        return PeticionamentoModo.fromString(modo);
    }

    @JsonIgnore
    public boolean prepararPacoteProtocoloResolvido() {
        return Boolean.TRUE.equals(prepararPacoteProtocolo);
    }

    @JsonIgnore
    public boolean resolverEnderecoAutomaticamenteResolvido() {
        return resolverEnderecoAutomaticamente == null || resolverEnderecoAutomaticamente;
    }

    @JsonIgnore
    public boolean tutelaUrgenciaResolvida() {
        return Boolean.TRUE.equals(tutelaUrgencia);
    }

    @JsonIgnore
    public LaianePeticaoInicialEstruturarRequest toDraftRequest() {
        return new LaianePeticaoInicialEstruturarRequest(
                processoId,
                tituloCaso,
                parteAutora,
                parteRe,
                ramoDireito,
                ritoProcessual,
                classeProcessual,
                sanitizeList(fatos),
                sanitizeList(fundamentosJuridicos),
                sanitizeList(pedidos),
                sanitizeList(provasIndicadas),
                valorCausa,
                tutelaUrgenciaResolvida()
        );
    }

    @JsonIgnore
    public LaianePeticaoAssistRequest toAssistRequest(String effectiveDraftMarkdown, PeticionamentoEnderecoResponse autor, PeticionamentoEnderecoResponse reu) {
        LinkedHashMap<String, Object> mergedCtx = sanitizeCtx(ctx);
        putIfPresent(mergedCtx, "tituloCaso", tituloCaso);
        putIfPresent(mergedCtx, "parteAutora", parteAutora);
        putIfPresent(mergedCtx, "parteRe", parteRe);
        putIfPresent(mergedCtx, "classeProcessual", classeProcessual);
        putIfPresent(mergedCtx, "ramoDireito", ramoDireito);
        putIfPresent(mergedCtx, "ritoProcessual", ritoProcessual);
        putIfPresent(mergedCtx, "materiaPrincipal", materiaPrincipal);
        putIfPresent(mergedCtx, "naturezaJuridica", naturezaJuridica);
        putIfPresent(mergedCtx, "cidadeFato", cidadeFato);
        putIfPresent(mergedCtx, "ufFato", ufFato);
        putIfPresent(mergedCtx, "cidadeProtocolo", cidadeProtocolo);
        putIfPresent(mergedCtx, "ufProtocolo", ufProtocolo);
        putIfPresent(mergedCtx, "textoPeticaoLivre", textoPeticaoLivre);
        putIfPresent(mergedCtx, "tipoInstrumentoRepresentacao", tipoInstrumentoRepresentacao);
        if (identidadeVisual != null) {
            mergedCtx.put("identidadeVisual", toVisualIdentityMap(identidadeVisual));
        }
        if (autor != null) {
            mergedCtx.put("enderecoAutor", toAddressMap(autor));
        }
        if (reu != null) {
            mergedCtx.put("enderecoReu", toAddressMap(reu));
        }

        if (!midiaInline.isEmpty()) {
            mergedCtx.put("inlineMediaBlocks", toMediaBlocksMap(midiaInline));
        }
        if (!sanitizeList(provasDocumentais).isEmpty()) {
            mergedCtx.put("provasDocumentais", new ArrayList<>(sanitizeList(provasDocumentais)));
        }
        if (!sanitizeList(documentosPessoais).isEmpty()) {
            mergedCtx.put("documentosPessoais", new ArrayList<>(sanitizeList(documentosPessoais)));
        }
        if (!sanitizeList(documentosRepresentacao).isEmpty()) {
            mergedCtx.put("documentosRepresentacao", new ArrayList<>(sanitizeList(documentosRepresentacao)));
        }
        LinkedHashMap<String, Object> postPetitionAttachmentBlock = new LinkedHashMap<>();
        if (!sanitizeList(provasDocumentais).isEmpty()) {
            postPetitionAttachmentBlock.put("provasDocumentais", new ArrayList<>(sanitizeList(provasDocumentais)));
        }
        if (!sanitizeList(documentosPessoais).isEmpty()) {
            postPetitionAttachmentBlock.put("documentosPessoais", new ArrayList<>(sanitizeList(documentosPessoais)));
        }
        if (!sanitizeList(documentosRepresentacao).isEmpty()) {
            postPetitionAttachmentBlock.put("documentosRepresentacao", new ArrayList<>(sanitizeList(documentosRepresentacao)));
        }
        if (!sanitizeList(documentosAnexados).isEmpty()) {
            postPetitionAttachmentBlock.put("documentosGerais", new ArrayList<>(sanitizeList(documentosAnexados)));
        }
        if (!postPetitionAttachmentBlock.isEmpty()) {
            mergedCtx.put("postPetitionAttachmentBlock", Map.copyOf(postPetitionAttachmentBlock));
        }

        return LaianePeticaoAssistRequest.builder()
                .kind("PETICAO_INICIAL")
                .ctx(mergedCtx)
                .draftMarkdown(effectiveDraftMarkdown)
                .processoId(processoId)
                .classeTpu(classeProcessual)
                .assuntoTpu(assuntoTpu)
                .ramoDireito(ramoDireito)
                .valorCausa(valorCausa)
                .ufAutor(autor != null ? autor.getUf() : null)
                .comarcaAutor(autor != null ? autor.getCidade() : null)
                .ufReu(reu != null ? reu.getUf() : null)
                .comarcaReu(reu != null ? reu.getCidade() : null)
                .requerJuizadoEspecial(requerJuizadoEspecial)
                .requerVaraEspecializada(requerVaraEspecializada)
                .materiaPrincipal(materiaPrincipal)
                .tipoJustica(tipoJustica)
                .casoUrgente(casoUrgente)
                .preferenciaDigital(preferenciaDigital)
                .textoFatosResumido(textoFatosResumido)
                .documentosAnexados(new ArrayList<>(sanitizeList(documentosAnexados)))
                .requerLiminar(requerLiminar)
                .ritoSugerido(ritoProcessual)
                .protocolTitle(protocolTitle)
                .build();
    }

    private static List<Map<String, Object>> toMediaBlocksMap(List<PeticionamentoMediaBlocoRequest> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        ArrayList<Map<String, Object>> out = new ArrayList<>();
        for (PeticionamentoMediaBlocoRequest value : values) {
            if (value == null) {
                continue;
            }
            out.add(value.toMap());
        }
        return out.isEmpty() ? List.of() : List.copyOf(out);
    }

    private static LinkedHashMap<String, Object> sanitizeCtx(Map<String, Object> source) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        if (source == null || source.isEmpty()) {
            return out;
        }
        source.forEach((key, value) -> {
            if (key == null || key.isBlank() || value == null) {
                return;
            }
            if (value instanceof String text) {
                String normalized = text.trim();
                if (!normalized.isEmpty()) {
                    out.put(key, normalized);
                }
                return;
            }
            out.put(key, value);
        });
        return out;
    }

    private static List<String> sanitizeList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        ArrayList<String> out = new ArrayList<>();
        for (String value : values) {
            if (value == null) {
                continue;
            }
            String normalized = value.trim();
            if (!normalized.isEmpty() && !out.contains(normalized)) {
                out.add(normalized);
            }
        }
        return out.isEmpty() ? List.of() : List.copyOf(out);
    }

    private static void putIfPresent(Map<String, Object> target, String key, Object value) {
        if (target == null || key == null || key.isBlank() || value == null) {
            return;
        }
        if (value instanceof String text && text.isBlank()) {
            return;
        }
        target.put(key, value);
    }

    private static Map<String, Object> toAddressMap(PeticionamentoEnderecoResponse response) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        putIfPresent(map, "cep", response.getCep());
        putIfPresent(map, "logradouro", response.getLogradouro());
        putIfPresent(map, "numero", response.getNumero());
        putIfPresent(map, "complemento", response.getComplemento());
        putIfPresent(map, "bairro", response.getBairro());
        putIfPresent(map, "cidade", response.getCidade());
        putIfPresent(map, "uf", response.getUf());
        putIfPresent(map, "referencia", response.getReferencia());
        map.put("autoPreenchido", response.isAutoPreenchido());
        map.put("valido", response.isValido());
        putIfPresent(map, "origem", response.getOrigem());
        if (response.getAvisos() != null && !response.getAvisos().isEmpty()) {
            map.put("avisos", List.copyOf(response.getAvisos()));
        }
        return map;
    }

    private static Map<String, Object> toVisualIdentityMap(PeticionamentoVisualIdentityRequest identity) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        if (identity == null) {
            return map;
        }
        putIfPresent(map, "nomeExibicao", identity.getNomeExibicao());
        putIfPresent(map, "nomeInstituicao", identity.getNomeInstituicao());
        putIfPresent(map, "brasaoOuLogomarcaUri", identity.getBrasaoOuLogomarcaUri());
        putIfPresent(map, "cabecalhoLivre", identity.getCabecalhoLivre());
        putIfPresent(map, "rodapeLivre", identity.getRodapeLivre());
        putIfPresent(map, "paletaPrimaria", identity.getPaletaPrimaria());
        putIfPresent(map, "paletaSecundaria", identity.getPaletaSecundaria());
        if (identity.getExibirRegistroProfissional() != null) {
            map.put("exibirRegistroProfissional", identity.getExibirRegistroProfissional());
        }
        if (identity.getExibirBrasaoOuLogomarca() != null) {
            map.put("exibirBrasaoOuLogomarca", identity.getExibirBrasaoOuLogomarca());
        }
        return map;
    }
}
