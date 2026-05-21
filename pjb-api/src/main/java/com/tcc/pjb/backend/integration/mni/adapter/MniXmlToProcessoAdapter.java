package com.tcc.pjb.backend.integration.mni.adapter;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import java.io.StringReader;
import java.util.Locale;
import java.util.Objects;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

@Component
public class MniXmlToProcessoAdapter {

    public Processo fromXml(String xml, String tribunalOrigem, String motivo) {
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
        return processo;
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
        return RitoProcessual.tryParse(raw).orElse(RitoProcessual.COMUM_ORDINARIO);
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
