package com.tcc.pjb.backend.integration.mni.adapter;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import java.io.StringReader;
import java.util.Base64;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

@Component
public class MniXmlToProcessoAdapter {

    private static final Logger log = LoggerFactory.getLogger(MniXmlToProcessoAdapter.class);

    public MniAdapterResult fromXml(String xml, String tribunalOrigem, String motivo) {
        Objects.requireNonNull(xml, "xml");
        Document doc = parseSecure(xml);
        Processo processo = new Processo();
        processo.setNumeroUnificado(firstNonBlank(tag(doc, "numeroUnificado"), tag(doc, "numero_processo"), tag(doc, "numero")));
        processo.setNumeroProcesso(firstNonBlank(processo.getNumeroUnificado(), tag(doc, "numeroProcesso")));
        processo.setTribunal(firstNonBlank(tag(doc, "tribunalDestino"), tribunalOrigem));
        processo.setUf(firstNonBlank(tag(doc, "uf"), inferUf(tribunalOrigem)));
        processo.setComarca(firstNonBlank(tag(doc, "comarca"), "MNI_RECEPCAO"));
        processo.setAssunto(firstNonBlank(tag(doc, "assunto"), tag(doc, "motivo"), motivo));
        processo.setClasseProcessual(firstNonBlank(tag(doc, "classeProcessual"), "MNI_RECEBIDO"));
        processo.setClasseTpuCodigo(firstNonBlank(tag(doc, "classeTpuCodigo"), tag(doc, "classe_tpu_codigo")));
        processo.setConnectorSystem("MNI");
        processo.setConnectorSubmissionStatus("RECEIVED");
        processo.setConnectorProtocolReference(firstNonBlank(tag(doc, "protocoloDestino"), tag(doc, "protocolo")));
        processo.setObjetoProcessual(firstNonBlank(tag(doc, "objetoProcessual"), tag(doc, "conteudo"), xml));
        processo.setRamoDireito(resolveRamo(tag(doc, "ramoDireito")));
        processo.setRito(resolveRito(tag(doc, "rito")));
        processo.setStatusProcesso(StatusProcesso.EM_ANDAMENTO);
        List<MniParteParsed> partes = resolvePartes(doc, processo);
        List<MniMovimentoParsed> movimentos = resolveMovimentos(doc);
        List<MniDocumentoParsed> documentos = resolveDocumentos(doc);
        return new MniAdapterResult(processo, partes, movimentos, documentos);
    }

    /**
     * Mesma ressalva de {@link #resolveMovimentos(Document)}: nome/atributos exatos do elemento
     * "documento" do XSD do MNI não puderam ser confirmados contra fonte oficial. Documento sem
     * conteúdo (base64) reconhecível é descartado — nunca materializa um documento vazio.
     */
    private List<MniDocumentoParsed> resolveDocumentos(Document doc) {
        List<MniDocumentoParsed> documentos = new ArrayList<>();
        for (Node documento : allDescendants(doc, "documento")) {
            String base64 = firstNonBlank(attr(documento, "conteudo"), elementText(documento, "conteudo"));
            byte[] conteudo = decodeBase64(base64);
            if (conteudo == null || conteudo.length == 0) {
                continue;
            }
            String nome = firstNonBlank(attr(documento, "nome"), attr(documento, "descricao"));
            String descricao = attr(documento, "descricao");
            String mimetype = attr(documento, "mimetype");
            Instant dataHora = parseDataHoraMni(attr(documento, "dataHora"));
            documentos.add(new MniDocumentoParsed(nome, descricao, mimetype, conteudo, dataHora));
        }
        return documentos;
    }

    private static byte[] decodeBase64(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Base64.getMimeDecoder().decode(raw.trim());
        } catch (IllegalArgumentException e) {
            log.warn("Conteúdo de documento MNI não é base64 válido, descartando documento");
            return null;
        }
    }

    /**
     * Formato literal de dataHora do movimento no XSD do MNI (intercomunicacao-2.2.2) não pôde ser
     * confirmado contra fonte oficial nesta implementação — tenta os dois formatos observados em
     * integrações reais (yyyyMMddHHmmss compacto e ISO local date-time) e descarta silenciosamente
     * o movimento se nenhum dos dois for reconhecido, em vez de fabricar uma data.
     */
    private List<MniMovimentoParsed> resolveMovimentos(Document doc) {
        List<MniMovimentoParsed> movimentos = new ArrayList<>();
        for (Node movimento : allDescendants(doc, "movimento")) {
            Instant dataHora = parseDataHoraMni(attr(movimento, "dataHora"));
            if (dataHora == null) {
                continue;
            }
            String descricao = firstNonBlank(
                    attr(movimento, "descricao"),
                    attr(firstDescendant(movimento, "movimentoNacional"), "descricao"),
                    attr(firstDescendant(movimento, "movimentoLocal"), "descricao"),
                    elementText(movimento, "complemento"));
            movimentos.add(new MniMovimentoParsed(dataHora, descricao));
        }
        return movimentos;
    }

    private static Instant parseDataHoraMni(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.length() >= 14) {
            try {
                LocalDateTime ldt = LocalDateTime.parse(digits.substring(0, 14), DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
                return ldt.toInstant(ZoneOffset.UTC);
            } catch (DateTimeParseException ignored) {
                // tenta o próximo formato abaixo
            }
        }
        try {
            return LocalDateTime.parse(raw.trim()).toInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private List<MniParteParsed> resolvePartes(Document doc, Processo processo) {
        List<MniParteParsed> partes = new ArrayList<>();
        NodeList all = doc.getElementsByTagName("*");
        boolean primeiroAT = true;
        boolean primeiroPA = true;
        for (int i = 0; i < all.getLength(); i++) {
            Node n = all.item(i);
            if (n.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            String local = n.getLocalName() != null ? n.getLocalName() : n.getNodeName();
            if (!"polo".equalsIgnoreCase(local)) {
                continue;
            }
            String tipoPolo = attr(n, "polo");
            List<Node> pessoas = allDescendants(n, "pessoa");
            for (Node pessoa : pessoas) {
                String nome = attr(pessoa, "nome");
                String documento = attr(pessoa, "numeroDocumentoPrincipal");
                String uf = normalizeUf(elementText(firstDescendant(pessoa, "endereco"), "estado"));
                String tipoPessoa = attr(pessoa, "tipoPessoa");
                partes.add(new MniParteParsed(tipoPolo, nome, documento, uf, tipoPessoa));
                if ("AT".equalsIgnoreCase(tipoPolo) && primeiroAT) {
                    processo.setParteAutoraNome(nome);
                    processo.setParteAutoraCpf(documento);
                    processo.setUfAutor(uf);
                    primeiroAT = false;
                } else if ("PA".equalsIgnoreCase(tipoPolo) && primeiroPA) {
                    processo.setParteReuNome(nome);
                    processo.setParteReuCpf(documento);
                    processo.setUfReu(uf);
                    primeiroPA = false;
                }
            }
            for (Node ip : allDescendants(n, "interessePublico")) {
                String nome = ip.getTextContent();
                if (nome != null) {
                    nome = nome.trim();
                }
                partes.add(new MniParteParsed(tipoPolo, nome, null, null, "interesse_publico"));
            }
        }
        return partes;
    }

    private static String elementText(Node scope, String tagName) {
        if (scope == null) {
            return null;
        }
        Node match = firstDescendant(scope, tagName);
        if (match == null) {
            return null;
        }
        String text = match.getTextContent();
        return text == null ? null : text.trim();
    }

    private static String normalizeUf(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim().toUpperCase(Locale.ROOT);
        return trimmed.length() == 2 ? trimmed : null;
    }

    private static String attr(Node node, String name) {
        if (node == null || node.getAttributes() == null) {
            return null;
        }
        Node a = node.getAttributes().getNamedItem(name);
        return a == null ? null : a.getNodeValue();
    }

    private static Node firstDescendant(Node parent, String tagName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            String local = child.getLocalName() != null ? child.getLocalName() : child.getNodeName();
            if (tagName.equalsIgnoreCase(local)) {
                return child;
            }
            Node deeper = firstDescendant(child, tagName);
            if (deeper != null) {
                return deeper;
            }
        }
        return null;
    }

    private static List<Node> allDescendants(Node parent, String tagName) {
        List<Node> result = new ArrayList<>();
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            String local = child.getLocalName() != null ? child.getLocalName() : child.getNodeName();
            if (tagName.equalsIgnoreCase(local)) {
                result.add(child);
            }
            result.addAll(allDescendants(child, tagName));
        }
        return result;
    }

    private static Document parseSecure(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setExpandEntityReferences(false);
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(null);
            return builder.parse(new InputSource(new StringReader(xml)));
        } catch (Exception e) {
            try {
                return DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            } catch (Exception ex) {
                throw new IllegalStateException("DocumentBuilder indisponível", ex);
            }
        }
    }

    private static String tag(Document doc, String tagName) {
        if (doc == null || tagName == null || tagName.isBlank()) {
            return null;
        }
        NodeList all = doc.getElementsByTagName("*");
        for (int i = 0; i < all.getLength(); i++) {
            Node n = all.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                String local = n.getLocalName() != null ? n.getLocalName() : n.getNodeName();
                if (tagName.equalsIgnoreCase(local)) {
                    String text = n.getTextContent();
                    return text == null ? null : text.trim();
                }
            }
        }
        return null;
    }

    private RamoDireito resolveRamo(String raw) {
        RamoDireito ramo = RamoDireito.fromString(raw);
        return ramo == null ? RamoDireito.CIVIL : ramo;
    }

    private RitoProcessual resolveRito(String raw) {
        return RitoProcessual.tryParse(raw).orElseGet(() -> {
            log.warn("Rito MNI não reconhecido, aplicando fallback COMUM_ORDINARIO: raw={}", raw);
            return RitoProcessual.COMUM_ORDINARIO;
        });
    }

    private String inferUf(String tribunalOrigem) {
        if (tribunalOrigem == null || tribunalOrigem.isBlank()) {
            return null;
        }
        String normalized = tribunalOrigem.trim().toUpperCase(Locale.ROOT);
        if (normalized.length() >= 2) {
            return normalized.substring(Math.max(0, normalized.length() - 2));
        }
        return normalized;
    }

    private String firstNonBlank(String... values) {
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
