package com.tcc.pjb.backend.service.triagem;

import com.tcc.pjb.backend.core.procedural.ProceduralCanonicalResolver;
import com.tcc.pjb.backend.core.procedural.ProceduralCanonicalResolver.CanonicalContext;
import com.tcc.pjb.backend.core.validation.document.DocumentoNacionalValidator;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.service.rito.RitoPackService;
import com.tcc.pjb.backend.service.teto.TetoProcessualService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
class TriagemNacionalValidationSupport {

    private static final BigDecimal LIMITE_POSTULACAO_PROPRIA = new BigDecimal("30000.00");
    private static final int MINIMO_TEXTO_FATOS = 160;

    private final DocumentoNacionalValidator documentoValidator;
    private final RitoPackService ritoPackService;
    private final TetoProcessualService tetoProcessualService;
    private final ProceduralCanonicalResolver proceduralCanonicalResolver;

    TriagemNacionalValidationSupport(DocumentoNacionalValidator documentoValidator,
                                     RitoPackService ritoPackService,
                                     TetoProcessualService tetoProcessualService,
                                     ProceduralCanonicalResolver proceduralCanonicalResolver) {
        this.documentoValidator = Objects.requireNonNull(documentoValidator);
        this.ritoPackService = Objects.requireNonNull(ritoPackService);
        this.tetoProcessualService = Objects.requireNonNull(tetoProcessualService);
        this.proceduralCanonicalResolver = Objects.requireNonNull(proceduralCanonicalResolver);
    }

    TriagemNacionalIAEngine.PedidoTriagem normalizarPedido(TriagemNacionalIAEngine.PedidoTriagem pedido) {
        List<String> documentos = pedido.documentosAnexados() == null ? List.of() : pedido.documentosAnexados().stream()
                .filter(Objects::nonNull)
                .map(this::normalizarTextoAlta)
                .filter(s -> !s.isBlank())
                .distinct()
                .toList();
        return new TriagemNacionalIAEngine.PedidoTriagem(
                firstNonBlank(pedido.nupnProvisorio(), "TRIAGEM-" + UUID.randomUUID()),
                normalizeOptional(pedido.classeTpuSugerida()),
                normalizeOptional(pedido.assuntoTpuSugerido()),
                normalizeOptional(pedido.ramoDireito()),
                pedido.valorCausa(),
                normalizeOptional(pedido.textoFatosResumido()),
                normalizeDocumentoOptional(pedido.cpfCnpjAutor()),
                normalizeDocumentoOptional(pedido.cpfCnpjReu()),
                normalizeDigitsOptional(pedido.oabAdvogado()),
                normalizeUfOptional(pedido.ufAdvogado()),
                documentos,
                pedido.dataFatoGerador(),
                pedido.requerLiminar(),
                pedido.atoJurisdicionalAnterior(),
                pedido.processoId()
        );
    }

    void verificarDocumentosDasPartes(TriagemNacionalIAEngine.PedidoTriagem pedido,
                                      List<TriagemNacionalIAEngine.PendenciaTriagem> pendencias) {
        validarDocumentoParte(pedido.cpfCnpjAutor(), TriagemNacionalIAEngine.TipoPendencia.DOCUMENTO_AUTOR_INVALIDO, "autor", pendencias);
        validarDocumentoParte(pedido.cpfCnpjReu(), TriagemNacionalIAEngine.TipoPendencia.DOCUMENTO_REU_INVALIDO, "reu", pendencias);
    }

    void verificarRepresentacao(TriagemNacionalIAEngine.PedidoTriagem pedido,
                                List<TriagemNacionalIAEngine.PendenciaTriagem> pendencias) {
        boolean postulacaoPropria = pedido.valorCausa() != null && pedido.valorCausa().compareTo(LIMITE_POSTULACAO_PROPRIA) <= 0;
        if (pedido.oabAdvogado() == null || pedido.oabAdvogado().isBlank()) {
            if (!postulacaoPropria) {
                pendencias.add(new TriagemNacionalIAEngine.PendenciaTriagem(
                        TriagemNacionalIAEngine.TipoPendencia.OAB_INVALIDA_OU_SUSPENSA,
                        "Representacao profissional obrigatoria sem identificacao de OAB",
                        true,
                        TriagemNacionalIAEngine.SeveridadePendencia.CRITICA,
                        "Informe a inscricao OAB do subscritor ou enquadre o caso em postulacao propria"
                ));
            }
            return;
        }
        if (pedido.oabAdvogado().length() < 3 || pedido.ufAdvogado() == null || pedido.ufAdvogado().isBlank()) {
            pendencias.add(new TriagemNacionalIAEngine.PendenciaTriagem(
                    TriagemNacionalIAEngine.TipoPendencia.REPRESENTACAO_INCONSISTENTE,
                    "Dados de representacao profissional incompletos",
                    true,
                    TriagemNacionalIAEngine.SeveridadePendencia.CRITICA,
                    "Informe numero OAB e UF validos"
            ));
        }
    }

    void verificarDocumentosObrigatorios(TriagemNacionalIAEngine.PedidoTriagem pedido,
                                         List<TriagemNacionalIAEngine.PendenciaTriagem> pendencias) {
        Set<String> docs = pedido.documentosAnexados().stream().map(this::normalizarTextoAlta).collect(Collectors.toCollection(LinkedHashSet::new));
        String ramo = normalizeOptional(pedido.ramoDireito());
        String classe = normalizeOptional(pedido.classeTpuSugerida());
        String texto = normalizarTextoAlta(firstNonBlank(pedido.textoFatosResumido(), pedido.assuntoTpuSugerido(), classe));

        exigirDocumentoQuando(pendencias, docs, ramo.contains("FAMIL") || texto.contains("ALIMENT") || texto.contains("GUARDA"),
                Set.of("CERTIDAO", "CASAMENTO", "NASCIMENTO"),
                "Acao de familia sem documento basico de estado civil ou filiacao",
                false,
                "Anexe certidao pertinente ao vinculo familiar invocado");

        exigirDocumentoQuando(pendencias, docs, ramo.contains("TRIBUT") || texto.contains("EXECUCAO FISCAL") || classe.contains("EXECUCAO_FISCAL"),
                Set.of("CDA", "CERTIDAO_DIVIDA", "DIVIDA ATIVA"),
                "Execucao fiscal sem CDA ou documento equivalente",
                true,
                "Anexe a certidao de divida ativa ou demonstrativo executivo apto");

        exigirDocumentoQuando(pendencias, docs, texto.contains("CONTRATO") || ramo.contains("CIVIL") || ramo.contains("CONSUMID"),
                Set.of("CONTRATO", "COMPROVANTE", "PROVA", "NOTIFICACAO"),
                "Demanda civel ou consumerista sem suporte documental minimo identificado",
                false,
                "Anexe contrato, comprovantes ou notificacoes relevantes");

        if (pedido.requerLiminar() && docs.stream().noneMatch(d -> d.contains("LAUDO") || d.contains("COMPROVANTE") || d.contains("ATA") || d.contains("BOLETIM") || d.contains("DECISAO"))) {
            pendencias.add(new TriagemNacionalIAEngine.PendenciaTriagem(
                    TriagemNacionalIAEngine.TipoPendencia.LIMINAR_SEM_SUPORTE_MINIMO,
                    "Pedido urgente sem suporte documental minimo identificavel",
                    false,
                    TriagemNacionalIAEngine.SeveridadePendencia.ALERTA,
                    "Anexe elementos de probabilidade do direito e risco de dano"
            ));
        }

        String ritoPackSignal = localizarSinalRitoPack(ramo, classe);
        if (ritoPackSignal != null && docs.isEmpty()) {
            pendencias.add(new TriagemNacionalIAEngine.PendenciaTriagem(
                    TriagemNacionalIAEngine.TipoPendencia.DOCUMENTO_FALTANTE,
                    "Rito identificado sem anexos classificados na triagem inicial",
                    false,
                    TriagemNacionalIAEngine.SeveridadePendencia.ALERTA,
                    "Revise anexos compativeis com o fluxo " + ritoPackSignal
            ));
        }
    }

    void verificarCompletude(TriagemNacionalIAEngine.PedidoTriagem pedido,
                             List<TriagemNacionalIAEngine.PendenciaTriagem> pendencias) {
        String texto = firstNonBlank(pedido.textoFatosResumido(), "");
        if (texto.strip().length() < MINIMO_TEXTO_FATOS) {
            pendencias.add(new TriagemNacionalIAEngine.PendenciaTriagem(
                    TriagemNacionalIAEngine.TipoPendencia.CAUSA_PEDIR_INCOMPLETA,
                    "Narrativa fatico-juridica insuficiente para admissibilidade automatica",
                    false,
                    TriagemNacionalIAEngine.SeveridadePendencia.ALERTA,
                    "Amplie a descricao dos fatos, dano, nexo e pedido"
            ));
        }
        if (!containsAny(texto, Set.of("pede", "requer", "pretende", "postula", "condenacao", "tutela", "declaracao", "obrigacao"))) {
            pendencias.add(new TriagemNacionalIAEngine.PendenciaTriagem(
                    TriagemNacionalIAEngine.TipoPendencia.PEDIDO_INDETERMINADO,
                    "Estrutura textual sem marcador claro de pedido jurisdicional",
                    false,
                    TriagemNacionalIAEngine.SeveridadePendencia.ALERTA,
                    "Explicite o pedido principal e os pedidos acessorios"
            ));
        }
        if (pedido.valorCausa() == null || pedido.valorCausa().compareTo(BigDecimal.ZERO) <= 0) {
            pendencias.add(new TriagemNacionalIAEngine.PendenciaTriagem(
                    TriagemNacionalIAEngine.TipoPendencia.VALOR_CAUSA_INCONSISTENTE,
                    "Valor da causa ausente ou invalido",
                    true,
                    TriagemNacionalIAEngine.SeveridadePendencia.CRITICA,
                    "Informe valor da causa compativel com o pedido"
            ));
        }
    }

    void verificarValorEJuizado(TriagemNacionalIAEngine.PedidoTriagem pedido,
                                List<TriagemNacionalIAEngine.PendenciaTriagem> pendencias) {
        String classe = normalizeOptional(pedido.classeTpuSugerida());
        if (pedido.valorCausa() != null && classe.contains("JUIZADO") && pedido.valorCausa().compareTo(LIMITE_POSTULACAO_PROPRIA.multiply(new BigDecimal("2"))) > 0) {
            pendencias.add(new TriagemNacionalIAEngine.PendenciaTriagem(
                    TriagemNacionalIAEngine.TipoPendencia.JUIZADO_INCOMPATIVEL_COM_VALOR,
                    "Classe orientada a juizado com valor potencialmente incompativel com rito sumarizado",
                    false,
                    TriagemNacionalIAEngine.SeveridadePendencia.ALERTA,
                    "Confirme teto legal do juizado ou adeque a classe processual"
            ));
        }
    }

    void verificarTetoProcessual(TriagemNacionalIAEngine.PedidoTriagem pedido,
                                 List<TriagemNacionalIAEngine.PendenciaTriagem> pendencias) {
        TetoProcessualService.DiagnosticoTetoProcessual diagnostico = tetoProcessualService.diagnosticar(
                pedido.valorCausa(),
                null,
                parseRamo(pedido.ramoDireito()),
                parseRito(pedido.classeTpuSugerida(), pedido.ramoDireito()),
                null,
                LocalDate.now()
        );
        if (diagnostico.violacao()) {
            pendencias.add(new TriagemNacionalIAEngine.PendenciaTriagem(
                    TriagemNacionalIAEngine.TipoPendencia.JUIZADO_INCOMPATIVEL_COM_VALOR,
                    diagnostico.fundamentoLegal(),
                    true,
                    TriagemNacionalIAEngine.SeveridadePendencia.CRITICA,
                    diagnostico.sugestaoOperacional()
            ));
            return;
        }
        if (diagnostico.alerta()) {
            pendencias.add(new TriagemNacionalIAEngine.PendenciaTriagem(
                    TriagemNacionalIAEngine.TipoPendencia.JUIZADO_INCOMPATIVEL_COM_VALOR,
                    diagnostico.fundamentoLegal(),
                    false,
                    TriagemNacionalIAEngine.SeveridadePendencia.ALERTA,
                    diagnostico.sugestaoOperacional()
            ));
        }
    }

    void verificarCoerenciaPartes(TriagemNacionalIAEngine.PedidoTriagem pedido,
                                  List<TriagemNacionalIAEngine.PendenciaTriagem> pendencias) {
        if (pedido.cpfCnpjAutor() != null && pedido.cpfCnpjAutor().equals(pedido.cpfCnpjReu())) {
            pendencias.add(new TriagemNacionalIAEngine.PendenciaTriagem(
                    TriagemNacionalIAEngine.TipoPendencia.INTERESSE_PROCESSUAL_DUVIDOSO,
                    "Autor e reu compartilham o mesmo documento nacional",
                    true,
                    TriagemNacionalIAEngine.SeveridadePendencia.CRITICA,
                    "Revise legitimidade ativa e passiva ou demonstre representacao em polos distintos"
            ));
        }
        if (pedido.atoJurisdicionalAnterior()) {
            pendencias.add(new TriagemNacionalIAEngine.PendenciaTriagem(
                    TriagemNacionalIAEngine.TipoPendencia.RISCO_DUPLICIDADE_INTERNA,
                    "Peticionante sinalizou existencia de ato jurisdicional anterior sobre o mesmo conflito",
                    false,
                    TriagemNacionalIAEngine.SeveridadePendencia.ALERTA,
                    "Indique o numero do feito anterior ou descreva a distincao material"
            ));
        }
    }

    private void validarDocumentoParte(String documento,
                                       TriagemNacionalIAEngine.TipoPendencia tipo,
                                       String papel,
                                       List<TriagemNacionalIAEngine.PendenciaTriagem> pendencias) {
        if (documento == null || documento.isBlank()) {
            pendencias.add(new TriagemNacionalIAEngine.PendenciaTriagem(tipo, "Documento do " + papel + " nao informado", true,
                    TriagemNacionalIAEngine.SeveridadePendencia.CRITICA, "Informe CPF ou CNPJ valido da parte"));
            return;
        }
        try {
            documentoValidator.validarDocumento(documento);
        } catch (Exception ex) {
            pendencias.add(new TriagemNacionalIAEngine.PendenciaTriagem(tipo, "Documento do " + papel + " invalido", true,
                    TriagemNacionalIAEngine.SeveridadePendencia.CRITICA, "Revise o CPF ou CNPJ informado"));
        }
    }

    private void exigirDocumentoQuando(List<TriagemNacionalIAEngine.PendenciaTriagem> pendencias,
                                       Set<String> documentos,
                                       boolean condicao,
                                       Set<String> marcadores,
                                       String descricao,
                                       boolean impeditiva,
                                       String orientacao) {
        if (!condicao) {
            return;
        }
        boolean presente = documentos.stream().anyMatch(doc -> marcadores.stream().anyMatch(doc::contains));
        if (!presente) {
            pendencias.add(new TriagemNacionalIAEngine.PendenciaTriagem(
                    TriagemNacionalIAEngine.TipoPendencia.DOCUMENTO_FALTANTE,
                    descricao,
                    impeditiva,
                    impeditiva ? TriagemNacionalIAEngine.SeveridadePendencia.CRITICA : TriagemNacionalIAEngine.SeveridadePendencia.ALERTA,
                    orientacao
            ));
        }
    }

    private RamoDireito parseRamo(String ramo) {
        if (ramo == null || ramo.isBlank()) {
            return null;
        }
        return RamoDireito.fromString(ramo);
    }

    private com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual parseRito(String classe, String ramo) {
        CanonicalContext canonical = proceduralCanonicalResolver.resolve(Map.of(
                "classeTpu", firstNonBlank(classe, ""),
                "classe", firstNonBlank(classe, ""),
                "ramoDireito", firstNonBlank(ramo, "")
        ));
        if (canonical.rito() != null) {
            return canonical.rito();
        }
        if (classe == null || classe.isBlank()) {
            return null;
        }
        String token = normalizeOptional(classe);
        if (token.contains("AIJE")) {
            return com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual.ELEITORAL_AIJE;
        }
        if (token.contains("AIRC") || token.contains("IMPUGNACAO REGISTRO")) {
            return com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual.ELEITORAL_AIRC;
        }
        if (token.contains("AIME")) {
            return com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual.ELEITORAL_AIME;
        }
        if (token.contains("RCED")) {
            return com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual.ELEITORAL_RCED;
        }
        if (token.contains("IPM")) {
            return com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual.MILITAR_IPM;
        }
        if (token.contains("DISSIDIO")) {
            return com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual.TRABALHISTA_DISSIDIO_COLETIVO;
        }
        if (token.contains("EXECUCAO FISCAL")) {
            return com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual.EXECUCAO_FISCAL;
        }
        if (token.contains("BPC") || token.contains("LOAS")) {
            return com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual.PREVIDENCIARIO_BPC_LOAS;
        }
        if (token.contains("MANDADO SEGURANCA") && token.contains("TRIBUT")) {
            return com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual.TRIBUTARIO_MANDADO_SEGURANCA;
        }
        if (token.contains("JUIZADO") && token.contains("FAZENDA")) {
            return com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual.JUIZADO_ESPECIAL_FAZENDA_PUBLICA;
        }
        if (token.contains("JUIZADO") && token.contains("FEDERAL")) {
            return com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual.JUIZADO_ESPECIAL_FEDERAL;
        }
        if (token.contains("JUIZADO") && token.contains("CRIM")) {
            return com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual.JUIZADO_ESPECIAL_CRIMINAL;
        }
        if (token.contains("JUIZADO")) {
            return com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual.JUIZADO_ESPECIAL_CIVEL;
        }
        return null;
    }

    private String localizarSinalRitoPack(String ramo, String classe) {
        String ramoNorm = normalizeOptional(ramo);
        String classeNorm = normalizeOptional(classe);
        for (var rito : ritoPackService.catalogDrivenRitos()) {
            String norm = normalizeOptional(rito.name());
            if ((!ramoNorm.isBlank() && norm.contains(ramoNorm)) || (!classeNorm.isBlank() && norm.contains(classeNorm))) {
                return rito.name();
            }
        }
        for (String key : ritoPackService.definitions().keySet()) {
            String norm = normalizeOptional(key);
            if ((!ramoNorm.isBlank() && norm.contains(ramoNorm)) || (!classeNorm.isBlank() && norm.contains(classeNorm))) {
                return key;
            }
        }
        return null;
    }

    private String normalizarTextoAlta(String value) {
        if (value == null) {
            return "";
        }
        return value.toUpperCase(Locale.ROOT).replaceAll("\\s+", " ").strip();
    }

    private static boolean containsAny(String text, Set<String> needles) {
        String base = normalizeOptional(text);
        for (String needle : needles) {
            if (base.contains(normalizeOptional(needle))) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeOptional(String value) {
        return value == null ? "" : value.strip().toUpperCase(Locale.ROOT);
    }

    private String normalizeDocumentoOptional(String value) {
        String doc = documentoValidator.normalizarDocumento(value);
        return doc.isBlank() ? null : doc;
    }

    private static String normalizeDigitsOptional(String value) {
        if (value == null) {
            return null;
        }
        String digits = value.replaceAll("\\D+", "");
        return digits.isBlank() ? null : digits;
    }

    private static String normalizeUfOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String uf = value.strip().toUpperCase(Locale.ROOT);
        return uf.length() == 2 ? uf : null;
    }

    static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
