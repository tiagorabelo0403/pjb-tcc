package com.tcc.pjb.backend.integration.mni.infra;

import com.tcc.pjb.backend.integration.mni.domain.MniHttpResponse;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import javax.xml.parsers.DocumentBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

@Component
@ConditionalOnProperty(prefix = "pjb.mni", name = "mock-enabled", havingValue = "false", matchIfMissing = true)
class MniHttpClientImpl implements MniHttpClient {

    private static final Logger log = LoggerFactory.getLogger(MniHttpClientImpl.class);

    private final String baseUrl;
    private final String token;
    private final Duration timeout;
    private final HttpClient httpClient;

    MniHttpClientImpl(
            @Value("${pjb.mni.base-url:}") String baseUrl,
            @Value("${pjb.mni.token:}") String token,
            @Value("${pjb.mni.timeout-seconds:30}") int timeoutSeconds,
            @Qualifier("pjbSharedHttpClient") HttpClient httpClient) {
        this.baseUrl = baseUrl;
        this.token = token;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
    }

    @Override
    public MniHttpResponse enviarAutos(String tribunalDestino, String xml) {
        try {
            String soapEnvelope = buildSoapEnvelope(xml);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/" + tribunalDestino))
                    .header("Content-Type", "text/xml; charset=UTF-8")
                    .header("SOAPAction", "\"enviarAutos\"")
                    .header("Authorization", "Bearer " + token)
                    .POST(HttpRequest.BodyPublishers.ofString(soapEnvelope, StandardCharsets.UTF_8))
                    .timeout(timeout)
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("[MNI] enviarAutos rejeitado status={} tribunal={}", response.statusCode(), tribunalDestino);
                throw new IllegalStateException("MNI retornou HTTP " + response.statusCode());
            }
            return new MniHttpResponse(extrairProtocolo(response.body(), tribunalDestino));
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[MNI] enviarAutos falhou tribunal={} err={}", tribunalDestino, e.getMessage());
            throw new IllegalStateException("MNI enviarAutos falhou: " + e.getMessage(), e);
        }
    }

    private static String buildSoapEnvelope(String bodyContent) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
                "<soap:Body>" + bodyContent + "</soap:Body>" +
                "</soap:Envelope>";
    }

    private static String extrairProtocolo(String responseBody, String tribunalDestino) {
        if (responseBody == null || responseBody.isBlank()) {
            return "MNI-" + tribunalDestino + "-" + System.currentTimeMillis();
        }
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            dbf.setExpandEntityReferences(false);
            dbf.setNamespaceAware(true);
            Document doc = dbf.newDocumentBuilder()
                    .parse(new ByteArrayInputStream(responseBody.getBytes(StandardCharsets.UTF_8)));
            NodeList nodes = doc.getElementsByTagNameNS("*", "protocolo");
            if (nodes.getLength() > 0) {
                return nodes.item(0).getTextContent().strip();
            }
            nodes = doc.getElementsByTagName("protocolo");
            if (nodes.getLength() > 0) {
                return nodes.item(0).getTextContent().strip();
            }
        } catch (Exception e) {
            log.debug("[MNI] protocolo não extraído da resposta: {}", e.getMessage());
        }
        return "MNI-" + tribunalDestino + "-" + System.currentTimeMillis();
    }
}
