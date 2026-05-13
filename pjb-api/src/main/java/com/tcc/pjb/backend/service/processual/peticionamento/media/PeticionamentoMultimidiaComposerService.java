package com.tcc.pjb.backend.service.processual.peticionamento.media;

import com.tcc.pjb.backend.model.dto.processual.peticionamento.media.PeticionamentoMediaBlocoRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class PeticionamentoMultimidiaComposerService {

    public CompositionReport compose(ResolveRequest request) {
        ResolveRequest safe = request == null ? ResolveRequest.empty() : request;
        ArrayList<String> alerts = new ArrayList<>();
        ArrayList<String> blockers = new ArrayList<>();
        ArrayList<Map<String, Object>> inlineBlocks = new ArrayList<>();
        ArrayList<String> anchors = new ArrayList<>();
        int imageCount = 0;
        int audioCount = 0;
        int videoCount = 0;
        int sensitiveCount = 0;

        for (PeticionamentoMediaBlocoRequest block : safe.inlineMediaBlocks()) {
            if (block == null) {
                continue;
            }
            if ("DOCUMENTO".equals(block.tipoResolvido())) {
                blockers.add("Documento não pode ser inserido no corpo da petição; use o bloco pós-petição de anexos para o item " + block.ancoraResolvida() + ".");
            }
            Map<String, Object> blockMap = new LinkedHashMap<>(block.toMap());
            blockMap.put("inlineEligible", !"DOCUMENTO".equals(block.tipoResolvido()));
            blockMap.put("renderLane", "NARRATIVA_INTERATIVA");
            inlineBlocks.add(Map.copyOf(blockMap));
            if (!anchors.contains(block.ancoraResolvida())) {
                anchors.add(block.ancoraResolvida());
            } else {
                alerts.add("Âncora multimídia repetida detectada: " + block.ancoraResolvida() + ".");
            }
            switch (block.tipoResolvido()) {
                case "IMAGEM" -> imageCount++;
                case "AUDIO" -> audioCount++;
                case "VIDEO" -> videoCount++;
                default -> { }
            }
            if (block.blurInicialObrigatorio()) {
                sensitiveCount++;
            }
            if (!hasText(block.getTitulo()) && ("AUDIO".equals(block.tipoResolvido()) || "VIDEO".equals(block.tipoResolvido()))) {
                alerts.add("Mídia " + block.tipoResolvido().toLowerCase(java.util.Locale.ROOT) + " sem título explícito reduz inteligibilidade judicial.");
            }
        }

        if (inlineBlocks.size() > 24) {
            blockers.add("A petição multimídia excedeu o limite prudencial de 24 blocos inline na mesma peça.");
        }
        if (videoCount > 8) {
            alerts.add("Quantidade elevada de vídeos inline; consolidar trechos essenciais melhora leitura e estabilidade da sessão.");
        }

        LinkedHashMap<String, Object> postPetitionAttachmentBlock = new LinkedHashMap<>();
        postPetitionAttachmentBlock.put("enabled", !safe.postPetitionSectionsEmpty());
        postPetitionAttachmentBlock.put("provasDocumentais", List.copyOf(safe.provasDocumentais()));
        postPetitionAttachmentBlock.put("documentosPessoais", List.copyOf(safe.documentosPessoais()));
        postPetitionAttachmentBlock.put("documentosRepresentacao", List.copyOf(safe.documentosRepresentacao()));
        postPetitionAttachmentBlock.put("documentosGerais", List.copyOf(safe.documentosAnexados()));

        LinkedHashMap<String, Object> sections = new LinkedHashMap<>();
        sections.put("corpoNarrativoInterativo", !inlineBlocks.isEmpty());
        sections.put("postPetitionAttachmentBlock", Map.copyOf(postPetitionAttachmentBlock));

        LinkedHashMap<String, Object> workspace = new LinkedHashMap<>();
        workspace.put("inlineNarrativeEnabled", !inlineBlocks.isEmpty());
        workspace.put("inlineAllowedTypes", List.of("IMAGEM", "AUDIO", "VIDEO"));
        workspace.put("blockCount", inlineBlocks.size());
        workspace.put("imageCount", imageCount);
        workspace.put("audioCount", audioCount);
        workspace.put("videoCount", videoCount);
        workspace.put("sensitiveCount", sensitiveCount);
        workspace.put("narrativeAnchors", List.copyOf(anchors));
        workspace.put("inlineBlocks", List.copyOf(inlineBlocks));
        workspace.put("sections", Map.copyOf(sections));
        workspace.put("canonicalRendering", List.of("HTML_INTERATIVO", "SNAPSHOT_PROTOCOLAR_ESTATICA"));
        workspace.put("proofSeparation", List.of("POST_PETITION_PROVAS_DOCUMENTAIS", "POST_PETITION_DOCUMENTOS_PESSOAIS", "POST_PETITION_DOCUMENTOS_REPRESENTACAO", "POST_PETITION_DOCUMENTOS_GERAIS"));
        workspace.put("multimediaProfile", resolveProfile(imageCount, audioCount, videoCount, sensitiveCount));
        workspace.put("documentInlineForbidden", true);

        return new CompositionReport(
                resolveProfile(imageCount, audioCount, videoCount, sensitiveCount),
                !inlineBlocks.isEmpty(),
                blockers.isEmpty(),
                List.copyOf(blockers),
                List.copyOf(alerts),
                Map.copyOf(workspace),
                Map.of(
                        "inlineMediaBlocks", List.copyOf(inlineBlocks),
                        "sections", Map.copyOf(sections),
                        "anchors", List.copyOf(anchors),
                        "interactive", !inlineBlocks.isEmpty(),
                        "documentInlineForbidden", true
                )
        );
    }

    private static String resolveProfile(int imageCount, int audioCount, int videoCount, int sensitiveCount) {
        if (videoCount > 0 || audioCount > 0) {
            return sensitiveCount > 0 ? "MULTIMIDIA_SENSIVEL_REFORCADA" : "MULTIMIDIA_NARRATIVA_EXPANDIDA";
        }
        if (imageCount > 0) {
            return sensitiveCount > 0 ? "IMAGEM_SENSIVEL_CONTROLADA" : "IMAGEM_INLINE_GUIADA";
        }
        return "PETICAO_TEXTUAL_CANONICA";
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    public record ResolveRequest(
            List<PeticionamentoMediaBlocoRequest> inlineMediaBlocks,
            List<String> provasDocumentais,
            List<String> documentosPessoais,
            List<String> documentosRepresentacao,
            List<String> documentosAnexados) {
        public ResolveRequest {
            inlineMediaBlocks = safeList(inlineMediaBlocks);
            provasDocumentais = safeStrings(provasDocumentais);
            documentosPessoais = safeStrings(documentosPessoais);
            documentosRepresentacao = safeStrings(documentosRepresentacao);
            documentosAnexados = safeStrings(documentosAnexados);
        }

        public static ResolveRequest empty() {
            return new ResolveRequest(List.of(), List.of(), List.of(), List.of(), List.of());
        }

        public boolean postPetitionSectionsEmpty() {
            return provasDocumentais.isEmpty() && documentosPessoais.isEmpty() && documentosRepresentacao.isEmpty() && documentosAnexados.isEmpty();
        }

        private static List<PeticionamentoMediaBlocoRequest> safeList(List<PeticionamentoMediaBlocoRequest> values) {
            if (values == null || values.isEmpty()) {
                return List.of();
            }
            ArrayList<PeticionamentoMediaBlocoRequest> out = new ArrayList<>();
            for (PeticionamentoMediaBlocoRequest value : values) {
                if (value != null) {
                    out.add(value);
                }
            }
            return out.isEmpty() ? List.of() : List.copyOf(out);
        }

        private static List<String> safeStrings(List<String> values) {
            if (values == null || values.isEmpty()) {
                return List.of();
            }
            ArrayList<String> out = new ArrayList<>();
            for (String value : values) {
                if (value != null && !value.trim().isEmpty() && !out.contains(value.trim())) {
                    out.add(value.trim());
                }
            }
            return out.isEmpty() ? List.of() : List.copyOf(out);
        }
    }

    public record CompositionReport(
            String profile,
            boolean enabled,
            boolean protocolSafe,
            List<String> blockers,
            List<String> alerts,
            Map<String, Object> workspace,
            Map<String, Object> protocolSection) {
        public CompositionReport {
            profile = Objects.requireNonNullElse(profile, "PETICAO_TEXTUAL_CANONICA");
            blockers = blockers == null ? List.of() : List.copyOf(blockers);
            alerts = alerts == null ? List.of() : List.copyOf(alerts);
            workspace = workspace == null ? Map.of() : Map.copyOf(workspace);
            protocolSection = protocolSection == null ? Map.of() : Map.copyOf(protocolSection);
        }

        public boolean blocking() {
            return !blockers.isEmpty();
        }
    }
}
