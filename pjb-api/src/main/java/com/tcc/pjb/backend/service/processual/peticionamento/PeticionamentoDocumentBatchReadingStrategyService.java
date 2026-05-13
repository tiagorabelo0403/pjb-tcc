
package com.tcc.pjb.backend.service.processual.peticionamento;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class PeticionamentoDocumentBatchReadingStrategyService {

    public BatchReadingReport plan(ResolveRequest request) {
        ResolveRequest safe = request == null ? ResolveRequest.empty() : request;
        ArrayList<DocumentEntry> entries = new ArrayList<>();
        ArrayList<String> blockingIssues = new ArrayList<>();
        ArrayList<String> alerts = new ArrayList<>();

        addSyntheticEntries(entries, safe);
        addAttachedEntries(entries, safe.documentosAnexados());

        if (entries.stream().noneMatch(entry -> entry.category() == DocumentCategory.PECA_BASE)) {
            blockingIssues.add("Nenhuma peça-base foi identificada para leitura prioritária antes do protocolo.");
        }
        if (safe.representacaoExigeProcuracaoFormal()
                && entries.stream().noneMatch(entry -> entry.category() == DocumentCategory.REPRESENTACAO && "ATTACHMENT".equals(entry.source()))) {
            blockingIssues.add("A trilha documental não contém instrumento formal de representação exigido para este peticionamento.");
        }
        if ((safe.casoUrgente() || safe.tutelaUrgencia())
                && entries.stream().noneMatch(entry -> entry.category() == DocumentCategory.URGENCIA && "ATTACHMENT".equals(entry.source()))) {
            alerts.add("O caso foi marcado como urgente, mas não há documento claramente classificado como suporte de urgência ou plantão.");
        }
        if (safe.prepararPacoteProtocolo()
                && entries.stream().noneMatch(entry -> entry.category() == DocumentCategory.PROVA_MATERIAL
                || entry.category() == DocumentCategory.PROVA_TECNICA
                || entry.category() == DocumentCategory.CALCULO_FINANCEIRO)) {
            alerts.add("O pacote final ainda não contém prova material, técnica ou financeira claramente identificada.");
        }

        ArrayList<DocumentEntry> orderedEntries = new ArrayList<>(new LinkedHashSet<>(entries));
        orderedEntries.sort(Comparator
                .comparingInt(DocumentEntry::priority).reversed()
                .thenComparing(entry -> entry.category().ordinal())
                .thenComparing(DocumentEntry::name));

        LinkedHashMap<DocumentCategory, Integer> categoryCount = new LinkedHashMap<>();
        for (DocumentCategory category : DocumentCategory.values()) {
            int count = (int) orderedEntries.stream().filter(entry -> entry.category() == category).count();
            if (count > 0) {
                categoryCount.put(category, count);
            }
        }

        LinkedHashMap<ReadingStage, ArrayList<DocumentEntry>> staged = new LinkedHashMap<>();
        for (ReadingStage stage : ReadingStage.values()) {
            staged.put(stage, new ArrayList<>());
        }
        for (DocumentEntry entry : orderedEntries) {
            staged.computeIfAbsent(entry.stage(), unused -> new ArrayList<>()).add(entry);
        }

        ArrayList<String> mandatorySequence = new ArrayList<>();
        for (ReadingStage stage : ReadingStage.values()) {
            List<DocumentEntry> stageEntries = staged.get(stage);
            if (stageEntries == null || stageEntries.isEmpty()) {
                continue;
            }
            ArrayList<String> names = new ArrayList<>();
            for (DocumentEntry entry : stageEntries) {
                if (entry.mandatory() || entry.priority() >= 85) {
                    names.add(entry.name());
                }
            }
            if (!names.isEmpty()) {
                mandatorySequence.add(stage.label() + ": " + String.join(", ", names));
            }
        }

        ArrayList<Map<String, Object>> orderedDocuments = new ArrayList<>();
        for (DocumentEntry entry : orderedEntries) {
            LinkedHashMap<String, Object> item = new LinkedHashMap<>();
            item.put("name", entry.name());
            item.put("source", entry.source());
            item.put("category", entry.category().name());
            item.put("stage", entry.stage().name());
            item.put("priority", entry.priority());
            item.put("mandatory", entry.mandatory());
            item.put("reason", entry.reason());
            item.put("tags", entry.tags());
            orderedDocuments.add(Map.copyOf(item));
        }

        ArrayList<Map<String, Object>> lanes = new ArrayList<>();
        for (Map.Entry<ReadingStage, ArrayList<DocumentEntry>> stageEntry : staged.entrySet()) {
            if (stageEntry.getValue().isEmpty()) {
                continue;
            }
            ArrayList<String> docs = new ArrayList<>();
            int highPriority = 0;
            for (DocumentEntry entry : stageEntry.getValue()) {
                docs.add(entry.name());
                if (entry.priority() >= 85) {
                    highPriority++;
                }
            }
            LinkedHashMap<String, Object> lane = new LinkedHashMap<>();
            lane.put("lane", stageEntry.getKey().name());
            lane.put("label", stageEntry.getKey().label());
            lane.put("documents", List.copyOf(docs));
            lane.put("highPriorityDocuments", highPriority);
            lane.put("blockingStage", stageEntry.getKey() == ReadingStage.NUCLEO_DA_PECA
                    || (stageEntry.getKey() == ReadingStage.REGULARIDADE_E_REPRESENTACAO && safe.representacaoExigeProcuracaoFormal()));
            lanes.add(Map.copyOf(lane));
        }

        int readinessScore = resolveReadinessScore(orderedEntries, blockingIssues, alerts, safe);
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("profile", resolveProfile(safe, orderedEntries));
        metadata.put("documentCount", orderedEntries.size());
        metadata.put("attachmentCount", safe.documentosAnexados().size());
        metadata.put("syntheticDocumentCount", Math.max(0, orderedEntries.size() - safe.documentosAnexados().size()));
        metadata.put("readinessScore", readinessScore);
        metadata.put("mandatorySequence", List.copyOf(mandatorySequence));
        metadata.put("blockingIssues", List.copyOf(new LinkedHashSet<>(blockingIssues)));
        metadata.put("alerts", List.copyOf(new LinkedHashSet<>(alerts)));
        metadata.put("categoryCount", categoryCount.entrySet().stream().collect(LinkedHashMap::new, (m, e) -> m.put(e.getKey().name(), e.getValue()), Map::putAll));
        metadata.put("strictProtocolOrdering", safe.prepararPacoteProtocolo() || safe.sigiloReforcado());

        LinkedHashMap<String, Object> workspace = new LinkedHashMap<>();
        workspace.put("profile", metadata.get("profile"));
        workspace.put("resolvedDocumentTrack", resolveDocumentTrack(safe, orderedEntries));
        workspace.put("documentCount", orderedEntries.size());
        workspace.put("orderedDocuments", List.copyOf(orderedDocuments));
        workspace.put("readingLanes", List.copyOf(lanes));
        workspace.put("mandatorySequence", List.copyOf(mandatorySequence));
        workspace.put("categories", metadata.get("categoryCount"));
        workspace.put("blockingIssues", List.copyOf(new LinkedHashSet<>(blockingIssues)));
        workspace.put("alerts", List.copyOf(new LinkedHashSet<>(alerts)));
        workspace.put("readinessScore", readinessScore);

        return new BatchReadingReport(
                String.valueOf(metadata.get("profile")),
                List.copyOf(orderedDocuments),
                List.copyOf(lanes),
                List.copyOf(new LinkedHashSet<>(mandatorySequence)),
                List.copyOf(new LinkedHashSet<>(blockingIssues)),
                List.copyOf(new LinkedHashSet<>(alerts)),
                Collections.unmodifiableMap(metadata),
                Map.copyOf(workspace)
        );
    }

    private void addSyntheticEntries(List<DocumentEntry> entries, ResolveRequest request) {
        if (hasText(request.draftMarkdown())) {
            entries.add(new DocumentEntry(
                    "MINUTA_PRINCIPAL_INTERNA",
                    "SESSION_DRAFT",
                    DocumentCategory.PECA_BASE,
                    ReadingStage.NUCLEO_DA_PECA,
                    100,
                    true,
                    "Minuta principal da sessão usada como espinha de leitura.",
                    List.of("DRAFT_MARKDOWN", "PECA_BASE")
            ));
        } else if (hasText(request.textoPeticaoLivre()) || hasText(request.textoFatosResumido())) {
            entries.add(new DocumentEntry(
                    "TEXTO_BASE_DA_PETICAO",
                    "SESSION_TEXT",
                    DocumentCategory.PECA_BASE,
                    ReadingStage.NUCLEO_DA_PECA,
                    96,
                    true,
                    "Texto-base informado diretamente na sessão.",
                    List.of("TEXTO_BASE", "PECA_BASE")
            ));
        }
        if (request.representacaoExigeProcuracaoFormal()) {
            entries.add(new DocumentEntry(
                    "CHECK_REPRESENTACAO_FORMAL",
                    "SESSION_POLICY",
                    DocumentCategory.REPRESENTACAO,
                    ReadingStage.REGULARIDADE_E_REPRESENTACAO,
                    92,
                    true,
                    "A política de representação exige leitura específica de mandato, substabelecimento ou autorização equivalente.",
                    List.of("REPRESENTACAO", "MANDATO")
            ));
        }
        if (request.casoUrgente() || request.tutelaUrgencia()) {
            entries.add(new DocumentEntry(
                    "CHECK_SUPORTE_URGENCIA",
                    "SESSION_FLAGS",
                    DocumentCategory.URGENCIA,
                    ReadingStage.SUPORTE_FINANCEIRO_E_URGENCIA,
                    87,
                    true,
                    "O caso foi marcado como urgente e exige verificação documental priorizada.",
                    List.of("URGENCIA", "TUTELA")
            ));
        }
    }

    private void addAttachedEntries(List<DocumentEntry> entries, List<String> documents) {
        if (documents == null || documents.isEmpty()) {
            return;
        }
        for (String document : documents) {
            String normalizedName = normalize(document);
            if (normalizedName.isBlank()) {
                continue;
            }
            Classification classification = classify(document);
            entries.add(new DocumentEntry(
                    document.trim(),
                    "ATTACHMENT",
                    classification.category(),
                    classification.stage(),
                    classification.priority(),
                    classification.mandatory(),
                    classification.reason(),
                    classification.tags()
            ));
        }
    }

    private Classification classify(String rawName) {
        String name = rawName == null ? "" : rawName.trim();
        String token = normalize(name);

        if (containsAny(token, "peticao", "inicial", "minuta", "manifestacao", "recurso", "contestacao", "contrarrazoes")) {
            return new Classification(DocumentCategory.PECA_BASE, ReadingStage.NUCLEO_DA_PECA, 98, true, "Peça processual nuclear identificada.", tags("PECA_BASE", token));
        }
        if (containsAny(token, "procuracao", "substabelecimento", "mandato", "autorizacao", "autorizacao_assinatura")) {
            return new Classification(DocumentCategory.REPRESENTACAO, ReadingStage.REGULARIDADE_E_REPRESENTACAO, 94, true, "Documento de representação processual identificado.", tags("REPRESENTACAO", token));
        }
        if (containsAny(token, "rg", "cpf", "cnh", "identidade", "comprovante_residencia", "comprovante_endereco", "certidao_nascimento", "certidao_casamento")) {
            return new Classification(DocumentCategory.IDENTIFICACAO, ReadingStage.REGULARIDADE_E_REPRESENTACAO, 82, false, "Documento de qualificação ou identificação identificado.", tags("IDENTIFICACAO", token));
        }
        if (containsAny(token, "laudo", "pericia", "exame", "prontuario", "boletim_medico", "atestado", "parecer_tecnico", "cnis")) {
            return new Classification(DocumentCategory.PROVA_TECNICA, ReadingStage.PROVA_ESTRUTURADA, 89, false, "Prova técnica ou especializada identificada.", tags("PROVA_TECNICA", token));
        }
        if (containsAny(token, "calculo", "planilha", "extrato", "holerite", "contracheque", "demonstrativo", "cda", "divida_ativa", "guia", "boleto")) {
            return new Classification(DocumentCategory.CALCULO_FINANCEIRO, ReadingStage.SUPORTE_FINANCEIRO_E_URGENCIA, 84, false, "Documento de cálculo, dívida ou lastro financeiro identificado.", tags("CALCULO_FINANCEIRO", token));
        }
        if (containsAny(token, "liminar", "urgencia", "tutela", "plantao", "medida_protetiva", "risco", "ameaca")) {
            return new Classification(DocumentCategory.URGENCIA, ReadingStage.SUPORTE_FINANCEIRO_E_URGENCIA, 90, true, "Documento com vocação de suporte urgente identificado.", tags("URGENCIA", token));
        }
        if (containsAny(token, "contrato", "nota", "foto", "video", "audio", "email", "whatsapp", "boletim", "ocorrencia", "ata", "oficio", "comprovante", "sentenca", "acordao")) {
            return new Classification(DocumentCategory.PROVA_MATERIAL, ReadingStage.PROVA_ESTRUTURADA, 80, false, "Prova material ou documental de suporte identificada.", tags("PROVA_MATERIAL", token));
        }
        return new Classification(DocumentCategory.SUPORTE_GERAL, ReadingStage.PROVA_ESTRUTURADA, 70, false, "Documento de suporte geral sem categoria dominante inequívoca.", tags("SUPORTE_GERAL", token));
    }

    private int resolveReadinessScore(List<DocumentEntry> entries,
                                      List<String> blockingIssues,
                                      List<String> alerts,
                                      ResolveRequest request) {
        int score = 58;
        if (!entries.isEmpty()) score += Math.min(16, entries.size() * 3);
        long baseCount = entries.stream().filter(entry -> entry.category() == DocumentCategory.PECA_BASE).count();
        long representationCount = entries.stream().filter(entry -> entry.category() == DocumentCategory.REPRESENTACAO).count();
        long evidenceCount = entries.stream().filter(entry -> entry.category() == DocumentCategory.PROVA_MATERIAL || entry.category() == DocumentCategory.PROVA_TECNICA).count();
        if (baseCount > 0) score += 10;
        if (representationCount > 0) score += 6;
        if (evidenceCount > 0) score += 6;
        if (request.prepararPacoteProtocolo()) score += 4;
        score -= blockingIssues.size() * 18;
        score -= alerts.size() * 6;
        return Math.max(0, Math.min(100, score));
    }

    private String resolveProfile(ResolveRequest request, List<DocumentEntry> entries) {
        boolean heavy = entries.size() >= 6;
        boolean strict = request.prepararPacoteProtocolo() || request.sigiloReforcado() || request.representacaoExigeProcuracaoFormal();
        if (request.sigiloReforcado() && heavy) {
            return "PETICIONAMENTO_BATCH_LEITURA_ESTRITA_V3";
        }
        if (strict) {
            return "PETICIONAMENTO_BATCH_LEITURA_GUARDADA_V2";
        }
        return heavy ? "PETICIONAMENTO_BATCH_LEITURA_INTENSIVA_V2" : "PETICIONAMENTO_BATCH_LEITURA_BALANCEADA_V1";
    }

    private String resolveDocumentTrack(ResolveRequest request, List<DocumentEntry> entries) {
        if (request.prepararPacoteProtocolo() && entries.size() >= 5) {
            return "PROTOCOLO_FINAL_LOTE_COMPLETO";
        }
        if (request.representacaoExigeProcuracaoFormal()) {
            return "REGULARIDADE_E_REPRESENTACAO_PRIORIZADAS";
        }
        return entries.size() >= 4 ? "LEITURA_MULTIBLOCO" : "LEITURA_FOCADA";
    }

    private static List<String> tags(String mainTag, String normalizedName) {
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        tags.add(mainTag);
        if (containsAny(normalizedName, "pdf")) tags.add("PDF");
        if (containsAny(normalizedName, "doc", "docx")) tags.add("DOC");
        if (containsAny(normalizedName, "jpg", "jpeg", "png")) tags.add("IMAGEM");
        if (containsAny(normalizedName, "assinad", "signed")) tags.add("ASSINADO");
        return List.copyOf(tags);
    }

    private static boolean containsAny(String value, String... candidates) {
        if (value == null || value.isBlank() || candidates == null || candidates.length == 0) {
            return false;
        }
        for (String candidate : candidates) {
            String normalized = normalize(candidate);
            if (!normalized.isBlank() && value.contains(normalized)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        normalized = normalized.replaceAll("[^a-z0-9]+", "_");
        normalized = normalized.replaceAll("_+", "_");
        return normalized.replaceAll("^_|_$", "");
    }

    public record ResolveRequest(String tituloCaso,
                                 String ramoDireito,
                                 String ritoProcessual,
                                 String classeProcessual,
                                 String tipoJustica,
                                 String materiaPrincipal,
                                 String naturezaJuridica,
                                 String draftMarkdown,
                                 String textoPeticaoLivre,
                                 String textoFatosResumido,
                                 List<String> documentosAnexados,
                                 boolean tutelaUrgencia,
                                 boolean casoUrgente,
                                 boolean prepararPacoteProtocolo,
                                 boolean representacaoExigeProcuracaoFormal,
                                 boolean sigiloReforcado) {

        public ResolveRequest {
            documentosAnexados = immutableList(documentosAnexados);
        }

        static ResolveRequest empty() {
            return new ResolveRequest(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    List.of(),
                    false,
                    false,
                    false,
                    false,
                    false
            );
        }

        private static List<String> immutableList(List<String> values) {
            if (values == null || values.isEmpty()) {
                return List.of();
            }
            ArrayList<String> out = new ArrayList<>();
            for (String value : values) {
                if (value != null && !value.isBlank()) {
                    String trimmed = value.trim();
                    if (!trimmed.isEmpty() && !out.contains(trimmed)) {
                        out.add(trimmed);
                    }
                }
            }
            return out.isEmpty() ? List.of() : List.copyOf(out);
        }
    }

    public record BatchReadingReport(String profile,
                                     List<Map<String, Object>> orderedDocuments,
                                     List<Map<String, Object>> readingLanes,
                                     List<String> mandatorySequence,
                                     List<String> blockingIssues,
                                     List<String> alerts,
                                     Map<String, Object> metadata,
                                     Map<String, Object> workspace) {
        public boolean blocking() {
            return !blockingIssues.isEmpty();
        }
    }

    private record DocumentEntry(String name,
                                 String source,
                                 DocumentCategory category,
                                 ReadingStage stage,
                                 int priority,
                                 boolean mandatory,
                                 String reason,
                                 List<String> tags) {
    }

    private record Classification(DocumentCategory category,
                                  ReadingStage stage,
                                  int priority,
                                  boolean mandatory,
                                  String reason,
                                  List<String> tags) {
    }

    private enum DocumentCategory {
        PECA_BASE,
        REPRESENTACAO,
        IDENTIFICACAO,
        PROVA_TECNICA,
        CALCULO_FINANCEIRO,
        URGENCIA,
        PROVA_MATERIAL,
        SUPORTE_GERAL
    }

    private enum ReadingStage {
        NUCLEO_DA_PECA("Núcleo da peça"),
        REGULARIDADE_E_REPRESENTACAO("Regularidade e representação"),
        PROVA_ESTRUTURADA("Prova estruturada"),
        SUPORTE_FINANCEIRO_E_URGENCIA("Suporte financeiro e urgência");

        private final String label;

        ReadingStage(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }
}
