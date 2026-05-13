package com.tcc.pjb.backend.service.processual.peticionamento.triagem;

import com.tcc.pjb.backend.core.digitalizacao.DocumentIntelligenceRequest;
import com.tcc.pjb.backend.core.digitalizacao.DocumentIntelligenceResult;
import com.tcc.pjb.backend.core.digitalizacao.PjbDocumentIntelligencePort;
import java.util.List;
import java.util.Objects;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.core.io.ByteArrayResource;

public class PjbSpringAiDocumentIntelligenceAdapter implements PjbDocumentIntelligencePort {

    private final ChatClient chatClient;

    public PjbSpringAiDocumentIntelligenceAdapter(ChatClient.Builder chatClientBuilder) {
        this.chatClient = Objects.requireNonNull(chatClientBuilder).build();
    }

    @Override
    public DocumentIntelligenceResult analyze(DocumentIntelligenceRequest request) {
        if (request.conteudo() == null || request.conteudo().length == 0) {
            return new DocumentIntelligenceResult("", 0.0, "DESCONHECIDO", List.of(), false, false);
        }

        String textoExtraido = extrairTexto(request);
        if (textoExtraido.isBlank()) {
            return new DocumentIntelligenceResult("", 0.0, "ILEGIVEL", List.of("documento_sem_texto_extraivel"), false, false);
        }

        String prompt = buildAnalysisPrompt(textoExtraido, request.nomeArquivo());
        String resposta = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        return parseAnalysisResponse(textoExtraido, resposta);
    }

    private String extrairTexto(DocumentIntelligenceRequest request) {
        try {
            ByteArrayResource resource = new ByteArrayResource(request.conteudo()) {
                @Override
                public String getFilename() {
                    return request.nomeArquivo() != null ? request.nomeArquivo() : "documento.pdf";
                }
            };
            PagePdfDocumentReader reader = new PagePdfDocumentReader(resource);
            return reader.get().stream()
                    .map(Document::getText)
                    .filter(t -> t != null && !t.isBlank())
                    .reduce("", (a, b) -> a + "\n" + b)
                    .trim();
        } catch (Exception e) {
            return "";
        }
    }

    private String buildAnalysisPrompt(String texto, String nomeArquivo) {
        return """
                Analise este documento judicial brasileiro e responda APENAS com JSON no formato:
                {"tipo":"TIPO_DOCUMENTO","inconsistencias":["lista"],"duplicado":false}
                Tipos válidos: PETICAO_INICIAL, CONTESTACAO, RECURSO, PROCURACAO, COMPROVANTE, LAUDO, SENTENCA, DESPACHO, CERTIDAO, OUTRO
                Documento: %s
                Texto: %s
                """.formatted(
                nomeArquivo != null ? nomeArquivo : "sem_nome",
                texto.substring(0, Math.min(texto.length(), 2000))
        );
    }

    private DocumentIntelligenceResult parseAnalysisResponse(String textoExtraido, String resposta) {
        String tipo = "OUTRO";
        List<String> inconsistencias = List.of();

        try {
            if (resposta != null) {
                if (resposta.contains("\"tipo\":\"")) {
                    int start = resposta.indexOf("\"tipo\":\"") + 8;
                    int end = resposta.indexOf("\"", start);
                    if (end > start) {
                        tipo = resposta.substring(start, end);
                    }
                }
            }
        } catch (Exception ignored) {
        }

        double confianca = textoExtraido.length() > 200 ? 85.0 : textoExtraido.length() > 50 ? 65.0 : 40.0;
        return new DocumentIntelligenceResult(textoExtraido, confianca, tipo, inconsistencias, true, false);
    }
}
