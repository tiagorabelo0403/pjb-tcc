package com.tcc.pjb.backend.service.processual.peticionamento.studio;

import com.tcc.pjb.backend.model.dto.processual.peticionamento.media.PeticionamentoMediaBlocoRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class PeticionamentoStudioEvidenceSummaryService {

    public EvidenceSummaryReport summarize(ResolveRequest request) {
        ResolveRequest safe = request == null ? ResolveRequest.empty() : request;
        ArrayList<Map<String, Object>> items = new ArrayList<>();
        ArrayList<String> warnings = new ArrayList<>();

        for (PeticionamentoMediaBlocoRequest media : safe.inlineMediaBlocks()) {
            if (media == null) {
                continue;
            }
            LinkedHashMap<String, Object> item = new LinkedHashMap<>();
            String sourceType = media.tipoResolvido();
            String evidenceType = media.categoriaResolvida();
            String label = firstNonBlank(media.getTitulo(), media.getDescricao(), media.getUploadItemId(), media.getStorageKey(), media.blocoIdResolvido());
            item.put("sourceType", sourceType);
            item.put("evidenceType", evidenceType);
            item.put("label", label);
            item.put("summaryMode", "ROTULAGEM_E_METADADOS_ASSISTIDOS");
            item.put("summary", buildMediaSummary(media));
            item.put("legalUse", resolveLegalUse(evidenceType, sourceType));
            item.put("recommendedSection", resolveRecommendedSection(evidenceType));
            item.put("sensitive", media.blurInicialObrigatorio());
            item.put("chainOfCustody", media.blurInicialObrigatorio() ? "REFORCADA" : "PADRAO_CONTROLADA");
            item.put("warnings", resolveWarnings(media));
            item.put("recommendedActions", resolveRecommendedActions(media));
            items.add(Map.copyOf(item));
            if (media.blurInicialObrigatorio()) {
                warnings.add("Mídia sensível exige trilha controlada de visualização antes de entrar na narrativa principal.");
            }
        }

        appendDocumentItems(items, warnings, safe.provasDocumentais(), "PROVA_DOCUMENTAL", "Documento probatório informado para reforço narrativo e amarração com os fatos articulados.", "Das Provas");
        appendDocumentItems(items, warnings, safe.documentosPessoais(), "DOCUMENTO_PESSOAL", "Documento pessoal informado para qualificação e legitimidade subjetiva do caso.", "Da Qualificação das Partes");
        appendDocumentItems(items, warnings, safe.documentosRepresentacao(), "DOCUMENTO_REPRESENTACAO", "Documento de representação indicado para legitimar a atuação processual e o protocolo.", "Da Representação Processual");
        appendDocumentItems(items, warnings, safe.documentosAnexados(), "DOCUMENTO_GERAL", "Documento geral informado para compor o dossiê do caso e a cronologia probatória.", "Dos Anexos e Referências");

        LinkedHashMap<String, Object> summary = new LinkedHashMap<>();
        summary.put("profile", items.isEmpty() ? "DOSSIE_SEM_EVIDENCIA_MATERIALIZADA" : "DOSSIE_EVIDENCIA_INTELIGENTE_ATIVO");
        summary.put("summaryMode", "ROTULAGEM_E_METADADOS_ASSISTIDOS");
        summary.put("items", List.copyOf(items));
        summary.put("warnings", deduplicate(warnings));
        summary.put("counts", Map.of(
                "total", items.size(),
                "media", safe.inlineMediaBlocks().size(),
                "documentosGerais", safe.documentosAnexados().size(),
                "documentosProbatorios", safe.provasDocumentais().size(),
                "documentosPessoais", safe.documentosPessoais().size(),
                "documentosRepresentacao", safe.documentosRepresentacao().size()
        ));
        if (!safe.pericialWorkspace().isEmpty()) {
            summary.put("pericialWorkspace", safe.pericialWorkspace());
        }
        return new EvidenceSummaryReport(
                String.valueOf(summary.get("profile")),
                List.copyOf(items),
                deduplicate(warnings),
                Map.copyOf(summary)
        );
    }

    private void appendDocumentItems(List<Map<String, Object>> target,
                                     List<String> warnings,
                                     List<String> values,
                                     String evidenceType,
                                     String summaryTemplate,
                                     String section) {
        for (String value : values) {
            String normalized = trimToNull(value);
            if (normalized == null) {
                continue;
            }
            LinkedHashMap<String, Object> item = new LinkedHashMap<>();
            item.put("sourceType", "DOCUMENTO_REFERENCIADO");
            item.put("evidenceType", evidenceType);
            item.put("label", normalized);
            item.put("summaryMode", "REFERENCIA_DECLARADA_PELA_EQUIPE");
            item.put("summary", summaryTemplate);
            item.put("legalUse", resolveDocumentLegalUse(evidenceType));
            item.put("recommendedSection", section);
            item.put("sensitive", false);
            item.put("chainOfCustody", "PADRAO_CONTROLADA");
            item.put("warnings", List.of());
            item.put("recommendedActions", List.of("VINCULAR_A_UM_FATO_OU_PEDIDO", "GERAR_LEGENDA_PROBATORIA_CURTA"));
            target.add(Map.copyOf(item));
            if ("DOCUMENTO_REPRESENTACAO".equals(evidenceType)) {
                warnings.add("A peça rápida deve conferir a regularidade do instrumento de representação antes do protocolo.");
            }
        }
    }

    private String buildMediaSummary(PeticionamentoMediaBlocoRequest media) {
        String sourceType = media.tipoResolvido();
        String evidenceType = media.categoriaResolvida();
        String title = firstNonBlank(media.getTitulo(), media.getDescricao(), media.getUploadItemId(), media.getStorageKey(), "mídia sem rótulo");
        String qualifier = switch (sourceType) {
            case "IMAGEM" -> "Imagem indicada pela equipe e resumida por rotulagem e metadados, sem converter a imagem em verdade automática.";
            case "AUDIO" -> "Áudio indicado pela equipe com leitura orientada por metadados, dependente de transcrição e validação humana.";
            case "VIDEO" -> "Vídeo indicado pela equipe com leitura orientada por metadados, dependente de keyframes, transcrição e validação humana.";
            default -> "Arquivo indicado pela equipe com leitura orientada por rotulagem declarada.";
        };
        return title + ": " + qualifier + " Uso sugerido: " + resolveLegalUse(evidenceType, sourceType).toLowerCase(Locale.ROOT) + ".";
    }

    private List<String> resolveWarnings(PeticionamentoMediaBlocoRequest media) {
        ArrayList<String> warnings = new ArrayList<>();
        if (media.blurInicialObrigatorio()) {
            warnings.add("Conteúdo potencialmente sensível com visualização controlada.");
        }
        if (media.getTamanhoBytes() != null && media.getTamanhoBytes() > 50_000_000L) {
            warnings.add("Arquivo pesado; preferir resumo probatório e trilha de visualização derivada.");
        }
        if ("AUDIO".equals(media.tipoResolvido()) || "VIDEO".equals(media.tipoResolvido())) {
            warnings.add("Recomendável gerar transcrição com marca temporal antes do uso argumentativo intenso.");
        }
        return List.copyOf(warnings);
    }

    private List<String> resolveRecommendedActions(PeticionamentoMediaBlocoRequest media) {
        ArrayList<String> actions = new ArrayList<>();
        actions.add("GERAR_REFERENCIA_CURTA_NA_NARRATIVA");
        actions.add("CONECTAR_A_UM_FATO_E_A_UM_PEDIDO");
        if ("IMAGEM".equals(media.tipoResolvido())) {
            actions.add("VALIDAR_ORIGEM_E_AUTENTICIDADE_DA_IMAGEM");
        }
        if ("AUDIO".equals(media.tipoResolvido()) || "VIDEO".equals(media.tipoResolvido())) {
            actions.add("GERAR_TRANSCRICAO_TEMPORAL");
        }
        if (media.blurInicialObrigatorio()) {
            actions.add("APLICAR_STEP_UP_DE_VISUALIZACAO");
        }
        return List.copyOf(actions);
    }

    private String resolveRecommendedSection(String evidenceType) {
        return switch (Objects.requireNonNullElse(evidenceType, "INLINE_NARRATIVA")) {
            case "PROVA_DOCUMENTAL" -> "Das Provas";
            case "DOCUMENTO_PESSOAL" -> "Da Qualificação das Partes";
            case "DOCUMENTO_REPRESENTACAO" -> "Da Representação Processual";
            case "PROVA_TECNICA" -> "Da Prova Técnica";
            default -> "Dos Fatos e da Cronologia";
        };
    }

    private String resolveLegalUse(String evidenceType, String sourceType) {
        if ("DOCUMENTO_PESSOAL".equals(evidenceType)) {
            return "APOIAR_QUALIFICACAO_SUBJETIVA";
        }
        if ("DOCUMENTO_REPRESENTACAO".equals(evidenceType)) {
            return "APOIAR_LEGITIMIDADE_POSTULATORIA";
        }
        if ("PROVA_TECNICA".equals(evidenceType)) {
            return "APOIAR_NARRATIVA_TECNICA_E_REQUERIMENTO_PERICIAL";
        }
        return switch (Objects.requireNonNullElse(sourceType, "DOCUMENTO")) {
            case "IMAGEM" -> "APOIAR_NARRATIVA_FATICA_E_REFERENCIA_PROBATORIA";
            case "AUDIO" -> "APOIAR_CRONOLOGIA_DECLARACOES_E_CONTEXTO";
            case "VIDEO" -> "APOIAR_CRONOLOGIA_CONDUTA_E_CONTEXTO";
            default -> "APOIAR_NARRATIVA_FATICA_E_DOCUMENTAL";
        };
    }

    private String resolveDocumentLegalUse(String evidenceType) {
        return switch (Objects.requireNonNullElse(evidenceType, "DOCUMENTO_GERAL")) {
            case "DOCUMENTO_PESSOAL" -> "APOIAR_QUALIFICACAO_SUBJETIVA";
            case "DOCUMENTO_REPRESENTACAO" -> "APOIAR_LEGITIMIDADE_POSTULATORIA";
            case "PROVA_DOCUMENTAL" -> "APOIAR_NARRATIVA_FATICA_E_DOCUMENTAL";
            default -> "APOIAR_DOSSIE_E_ENCADEAMENTO_FATICO";
        };
    }

    private List<String> deduplicate(List<String> values) {
        ArrayList<String> out = new ArrayList<>();
        for (String value : values) {
            String normalized = trimToNull(value);
            if (normalized != null && !out.contains(normalized)) {
                out.add(normalized);
            }
        }
        return List.copyOf(out);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            String normalized = trimToNull(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public record ResolveRequest(List<PeticionamentoMediaBlocoRequest> inlineMediaBlocks,
                                 List<String> documentosAnexados,
                                 List<String> provasDocumentais,
                                 List<String> documentosPessoais,
                                 List<String> documentosRepresentacao,
                                 Map<String, Object> pericialWorkspace) {
        public ResolveRequest {
            inlineMediaBlocks = inlineMediaBlocks == null ? List.of() : List.copyOf(inlineMediaBlocks);
            documentosAnexados = documentosAnexados == null ? List.of() : List.copyOf(documentosAnexados);
            provasDocumentais = provasDocumentais == null ? List.of() : List.copyOf(provasDocumentais);
            documentosPessoais = documentosPessoais == null ? List.of() : List.copyOf(documentosPessoais);
            documentosRepresentacao = documentosRepresentacao == null ? List.of() : List.copyOf(documentosRepresentacao);
            pericialWorkspace = pericialWorkspace == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(pericialWorkspace));
        }

        public static ResolveRequest empty() {
            return new ResolveRequest(List.of(), List.of(), List.of(), List.of(), List.of(), Map.of());
        }
    }

    public record EvidenceSummaryReport(String profile,
                                        List<Map<String, Object>> items,
                                        List<String> warnings,
                                        Map<String, Object> workspace) {
        public EvidenceSummaryReport {
            profile = profile == null || profile.isBlank() ? "DOSSIE_SEM_EVIDENCIA_MATERIALIZADA" : profile.trim();
            items = items == null ? List.of() : List.copyOf(items);
            warnings = warnings == null ? List.of() : List.copyOf(warnings);
            workspace = workspace == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(workspace));
        }
    }
}
