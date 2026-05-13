package com.tcc.pjb.backend.service.processual.peticionamento.studio;

import com.tcc.pjb.backend.service.advogado.LaianePeticaoInicialDraftService;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class PeticionamentoStudioDraftAssemblerService {

    public QuickDraftReport assemble(ResolveRequest request) {
        ResolveRequest safe = request == null ? ResolveRequest.empty() : request;
        String title = resolveTitle(safe);
        String juizo = buildJuizo(safe.procedure());
        List<String> facts = selectList(safe.manualDraft() == null ? null : safe.manualDraft().fatosEstruturados(), safe.requestFacts());
        List<String> grounds = selectList(safe.manualDraft() == null ? null : safe.manualDraft().fundamentosEstruturados(), safe.requestGrounds());
        List<String> requests = selectList(safe.manualDraft() == null ? null : safe.manualDraft().pedidosEstruturados(), safe.requestPedidos());
        List<String> proofs = selectList(safe.manualDraft() == null ? null : safe.manualDraft().provasIndicadas(), safe.requestProofs());
        List<Map<String, Object>> jurisprudenceItems = listOfMaps(safe.jurisprudence().get("items"));
        List<Map<String, Object>> evidenceItems = listOfMaps(safe.evidence().get("items"));
        List<Map<String, Object>> timelineItems = listOfMaps(safe.caseTimeline().get("items"));
        List<Map<String, Object>> proofMatrixItems = listOfMaps(safe.proofRequestMatrix().get("items"));
        List<String> checklist = mergeDistinct(
                safe.manualDraft() == null ? List.of() : safe.manualDraft().checklistDocumental(),
                listOfStrings(safe.protocolChecklist().get("summary")),
                listOfStrings(safe.riskMatrix().get("checklist")),
                listOfStrings(safe.riskMatrix().get("blockingIssues")),
                listOfStrings(safe.riskMatrix().get("alerts"))
        );

        String markdown = buildMarkdown(
                title,
                juizo,
                safe.partes(),
                safe.procedure(),
                facts,
                grounds,
                jurisprudenceItems,
                evidenceItems,
                timelineItems,
                proofMatrixItems,
                proofs,
                requests,
                safe.valorCausa(),
                checklist
        );

        LinkedHashMap<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("profile", profileCode(safe.procedure()));
        envelope.put("juizoSugerido", juizo);
        envelope.put("factCount", facts.size());
        envelope.put("groundCount", grounds.size());
        envelope.put("requestCount", requests.size());
        envelope.put("evidenceCount", evidenceItems.size());
        envelope.put("jurisprudenceCount", jurisprudenceItems.size());
        envelope.put("timelineCount", timelineItems.size());
        envelope.put("proofMatrixCount", proofMatrixItems.size());
        envelope.put("usesCuratedJurisprudence", !jurisprudenceItems.isEmpty());
        envelope.put("reviewMandatory", true);

        return new QuickDraftReport(title, markdown, List.copyOf(checklist), Map.copyOf(envelope));
    }

    private String buildMarkdown(String title,
                                 String juizo,
                                 Map<String, Object> partes,
                                 Map<String, Object> procedure,
                                 List<String> facts,
                                 List<String> grounds,
                                 List<Map<String, Object>> jurisprudenceItems,
                                 List<Map<String, Object>> evidenceItems,
                                 List<Map<String, Object>> timelineItems,
                                 List<Map<String, Object>> proofMatrixItems,
                                 List<String> proofs,
                                 List<String> requests,
                                 BigDecimal valorCausa,
                                 List<String> checklist) {
        String family = petitionFamily(procedure);
        if ("EMBARGOS".equals(family)) {
            return buildEmbargosMarkdown(title, juizo, partes, procedure, facts, grounds, jurisprudenceItems, evidenceItems, timelineItems, proofMatrixItems, proofs, requests, checklist);
        }
        if (isRecursalFamily(family)) {
            return buildRecursalMarkdown(title, juizo, partes, procedure, facts, grounds, jurisprudenceItems, evidenceItems, timelineItems, proofMatrixItems, proofs, requests, checklist);
        }
        return buildBaseMarkdown(title, juizo, partes, procedure, facts, grounds, jurisprudenceItems, evidenceItems, timelineItems, proofMatrixItems, proofs, requests, valorCausa, checklist);
    }

    private String resolveTitle(ResolveRequest request) {
        String family = petitionFamily(request.procedure());
        String base = firstNonBlank(request.title(), "PETIÇÃO RÁPIDA");
        if ("EMBARGOS".equals(family)) {
            return firstNonBlank(base, "Embargos em elaboração");
        }
        if ("CONTRARRAZOES_RECURSAIS".equals(family)) {
            return firstNonBlank(base, "Contrarrazões recursais em elaboração");
        }
        if (isRecursalFamily(family)) {
            return firstNonBlank(base, "Petição recursal em elaboração");
        }
        return base;
    }

    private String buildBaseMarkdown(String title,
                                     String juizo,
                                     Map<String, Object> partes,
                                     Map<String, Object> procedure,
                                     List<String> facts,
                                     List<String> grounds,
                                     List<Map<String, Object>> jurisprudenceItems,
                                     List<Map<String, Object>> evidenceItems,
                                     List<Map<String, Object>> timelineItems,
                                     List<Map<String, Object>> proofMatrixItems,
                                     List<String> proofs,
                                     List<String> requests,
                                     BigDecimal valorCausa,
                                     List<String> checklist) {
        StringBuilder builder = new StringBuilder();
        builder.append("# ").append(title).append("\n\n");
        builder.append("## Endereçamento\n\n");
        builder.append(juizo).append("\n\n");
        builder.append("## Partes e qualificação mínima\n\n");
        builder.append("- Parte autora: ").append(stringValue(partes.get("parteAutora"), "A qualificar")).append("\n");
        builder.append("- Parte ré: ").append(stringValue(partes.get("parteRe"), "A qualificar")).append("\n");
        builder.append("- Classe sugerida: ").append(stringValue(procedure.get("classeProcessual"), "A definir")).append("\n");
        builder.append("- Ramo: ").append(stringValue(procedure.get("ramoDireito"), "A definir")).append("\n");
        builder.append("- Rito: ").append(stringValue(procedure.get("ritoProcessual"), "A definir")).append("\n\n");

        builder.append("## Síntese fática\n\n");
        appendBullets(builder, facts.isEmpty() ? List.of("Descrever os fatos essenciais em ordem cronológica, com datas, valores e agentes envolvidos.") : facts);
        builder.append("\n");

        builder.append("## Competência, rito e enquadramento\n\n");
        builder.append("A presente medida foi estruturada para tramitar em ")
                .append(stringValue(procedure.get("justicaSugerida"), "justiça competente a definir"))
                .append(", com rito ")
                .append(stringValue(procedure.get("ritoProcessual"), "a definir"))
                .append(" e classe processual sugerida ")
                .append(stringValue(procedure.get("classeProcessual"), "a definir"))
                .append(". O enquadramento procedimental foi consolidado a partir do dossiê do caso, da moldura territorial e do tipo de pretensão informado.\n\n");

        builder.append("## Fundamentos jurídicos\n\n");
        appendBullets(builder, grounds.isEmpty() ? List.of("Consolidar fundamentos normativos específicos ao caso concreto antes da assinatura final.") : grounds);
        builder.append("\n");
        appendJurisprudence(builder, jurisprudenceItems);
        appendTimeline(builder, timelineItems);
        appendEvidence(builder, evidenceItems, proofs);
        appendProofMatrix(builder, proofMatrixItems);

        builder.append("## Pedidos\n\n");
        appendBullets(builder, requests.isEmpty() ? List.of("Formalizar os pedidos principais, subsidiários e acessórios em ordem lógica.") : requests);
        builder.append("\n");

        builder.append("## Valor da causa\n\n");
        builder.append(valorCausa == null ? "A consolidar." : formatCurrency(valorCausa)).append("\n\n");

        builder.append("## Fechamento técnico\n\n");
        builder.append("Requer-se o regular processamento da presente peça, com observância do rito sugerido, análise dos pedidos formulados e apreciação do conjunto probatório já indicado.\n\n");

        builder.append("## Checklist de revisão antes da assinatura\n\n");
        appendBullets(builder, checklist.isEmpty() ? List.of("Revisar narrativa, pedidos, representação e provas antes do protocolo.") : checklist);
        return builder.toString();
    }

    private String buildRecursalMarkdown(String title,
                                         String juizo,
                                         Map<String, Object> partes,
                                         Map<String, Object> procedure,
                                         List<String> facts,
                                         List<String> grounds,
                                         List<Map<String, Object>> jurisprudenceItems,
                                         List<Map<String, Object>> evidenceItems,
                                         List<Map<String, Object>> timelineItems,
                                         List<Map<String, Object>> proofMatrixItems,
                                         List<String> proofs,
                                         List<String> requests,
                                         List<String> checklist) {
        Map<String, Object> blueprint = mapOf(procedure.get("recursalBlueprint"));
        StringBuilder builder = new StringBuilder();
        builder.append("# ").append(title).append("\n\n");
        builder.append("## Endereçamento recursal\n\n");
        builder.append(juizo).append("\n\n");
        builder.append("## Moldura procedimental da peça\n\n");
        builder.append("- Família da peça: ").append(stringValue(procedure.get("petitionFamily"), "RECURSAL")).append("\n");
        builder.append("- Espécie recursal canônica: ").append(stringValue(procedure.get("canonicalAppealType"), "A definir")).append("\n");
        builder.append("- Classe processual sugerida: ").append(stringValue(procedure.get("classeProcessual"), "A definir")).append("\n");
        builder.append("- Ramo: ").append(stringValue(procedure.get("ramoDireito"), "A definir")).append("\n");
        builder.append("- Rito: ").append(stringValue(procedure.get("ritoProcessual"), "A definir")).append("\n");
        if (Boolean.TRUE.equals(procedure.get("recursalCounterReasons"))) {
            builder.append("- Modo recursal: CONTRARRAZOES\n");
        }
        builder.append("\n");

        builder.append("## Síntese da decisão e do contexto impugnativo\n\n");
        appendBullets(builder, facts.isEmpty() ? List.of("Sintetizar a decisão atacada, os capítulos impugnados e o contexto fático-processual relevante.") : facts);
        builder.append("\n");

        builder.append("## Cabimento, tempestividade e regularidade formal\n\n");
        appendBullets(builder, listOfStrings(blueprint.get("travasDeValidacao")).isEmpty() ? List.of("Conferir cabimento da espécie, tempestividade, preparo e representação antes da assinatura final.") : listOfStrings(blueprint.get("travasDeValidacao")));
        builder.append("\n");

        builder.append(Boolean.TRUE.equals(procedure.get("recursalCounterReasons")) ? "## Contrarrazões e manutenção do julgado\n\n" : "## Razões recursais\n\n");
        appendBullets(builder, grounds.isEmpty() ? List.of("Estruturar o erro, nulidade ou desacerto do pronunciamento recorrido de forma dialética e organizada.") : grounds);
        builder.append("\n");
        appendJurisprudence(builder, jurisprudenceItems);
        appendTimeline(builder, timelineItems);

        builder.append("## Dossiê documental recursal\n\n");
        List<String> documentChecklist = mergeDistinct(documentLabels(blueprint.get("documentosObrigatorios")), proofs);
        if (!evidenceItems.isEmpty()) {
            for (Map<String, Object> item : evidenceItems) {
                builder.append("- ")
                        .append(stringValue(item.get("label"), "Evidência sem rótulo"))
                        .append(": ")
                        .append(stringValue(item.get("summary"), "Resumo probatório indisponível."))
                        .append("\n");
            }
        } else {
            appendBullets(builder, documentChecklist.isEmpty() ? List.of("Consolidar decisão recorrida, prova da ciência e peças obrigatórias do instrumento ou da espécie recursal.") : documentChecklist);
        }
        builder.append("\n");
        appendProofMatrix(builder, proofMatrixItems);

        builder.append(Boolean.TRUE.equals(procedure.get("recursalCounterReasons")) ? "## Pedido de rejeição do recurso\n\n" : "## Pedido recursal\n\n");
        appendBullets(builder, requests.isEmpty() ? List.of(Boolean.TRUE.equals(procedure.get("recursalCounterReasons")) ? "Requerer o não provimento do recurso adverso, com manutenção integral ou parcial do julgado recorrido." : "Formalizar o provimento recursal pretendido, inclusive reforma, invalidação, integração ou retratação, conforme a espécie.") : requests);
        builder.append("\n");

        builder.append("## Fechamento recursal\n\n");
        builder.append(Boolean.TRUE.equals(procedure.get("recursalCounterReasons"))
                ? "Requer-se o recebimento destas contrarrazões, com posterior não provimento do recurso adverso e preservação do capítulo favorável já firmado.\n\n"
                : "Requer-se o conhecimento e provimento do recurso, com observância dos filtros próprios da espécie, do acervo documental obrigatório e da técnica recursal adequada.\n\n");

        builder.append("## Checklist de revisão antes da assinatura\n\n");
        appendBullets(builder, checklist.isEmpty() ? List.of("Fechar decisão atacada, tempestividade, preparo, representação e dialeticidade recursal.") : checklist);
        return builder.toString();
    }

    private String buildEmbargosMarkdown(String title,
                                         String juizo,
                                         Map<String, Object> partes,
                                         Map<String, Object> procedure,
                                         List<String> facts,
                                         List<String> grounds,
                                         List<Map<String, Object>> jurisprudenceItems,
                                         List<Map<String, Object>> evidenceItems,
                                         List<Map<String, Object>> timelineItems,
                                         List<Map<String, Object>> proofMatrixItems,
                                         List<String> proofs,
                                         List<String> requests,
                                         List<String> checklist) {
        Map<String, Object> blueprint = mapOf(procedure.get("recursalBlueprint"));
        List<String> embargosGrounds = listOfStrings(procedure.get("embargosGrounds"));
        StringBuilder builder = new StringBuilder();
        builder.append("# ").append(title).append("\n\n");
        builder.append("## Endereçamento\n\n");
        builder.append(juizo).append("\n\n");
        builder.append("## Identificação dos embargos\n\n");
        builder.append("- Espécie canônica: ").append(stringValue(procedure.get("canonicalAppealType"), "EMBARGOS")).append("\n");
        builder.append("- Classe processual sugerida: ").append(stringValue(procedure.get("classeProcessual"), "A definir")).append("\n");
        builder.append("- Ramo: ").append(stringValue(procedure.get("ramoDireito"), "A definir")).append("\n");
        if (!embargosGrounds.isEmpty()) {
            builder.append("- Vícios mapeados: ").append(String.join(", ", embargosGrounds)).append("\n");
        }
        builder.append("\n");

        builder.append("## Decisão embargada e contexto imediato\n\n");
        appendBullets(builder, facts.isEmpty() ? List.of("Indicar a decisão embargada, o trecho relevante e o contexto imediato que justifica a integração ou correção do julgado.") : facts);
        builder.append("\n");

        builder.append("## Vício da decisão\n\n");
        List<String> embargosReasons = mergeDistinct(
                grounds,
                embargosGrounds.isEmpty() ? List.of() : List.of("Delimitar expressamente os vícios identificados: " + String.join(", ", embargosGrounds) + ".")
        );
        appendBullets(builder, embargosReasons.isEmpty() ? List.of("Demonstrar com precisão a omissão, contradição, obscuridade ou erro material, sem rediscutir indevidamente o mérito fora dos limites dos embargos.") : embargosReasons);
        builder.append("\n");
        appendJurisprudence(builder, jurisprudenceItems);
        appendTimeline(builder, timelineItems);

        builder.append("## Dossiê documental e referência decisória\n\n");
        appendBullets(builder, mergeDistinct(documentLabels(blueprint.get("documentosObrigatorios")), proofs).isEmpty() ? List.of("Anexar a decisão embargada integral e destacar o trecho exato em que se localiza o vício alegado.") : mergeDistinct(documentLabels(blueprint.get("documentosObrigatorios")), proofs));
        builder.append("\n");
        appendProofMatrix(builder, proofMatrixItems);

        builder.append("## Pedidos integrativos\n\n");
        appendBullets(builder, requests.isEmpty() ? List.of("Requerer o conhecimento dos embargos e a integração, aclaramento ou correção material da decisão embargada, com os efeitos legalmente cabíveis.") : requests);
        builder.append("\n");

        builder.append("## Fechamento técnico\n\n");
        builder.append("Requer-se o conhecimento dos presentes embargos, com apreciação específica do vício apontado e prolação de decisão integrativa ou corretiva nos limites próprios da espécie.\n\n");

        builder.append("## Checklist de revisão antes da assinatura\n\n");
        appendBullets(builder, checklist.isEmpty() ? List.of("Fechar decisão embargada, janela temporal e vício específico sem converter os embargos em recurso amplo.") : checklist);
        return builder.toString();
    }

    private void appendJurisprudence(StringBuilder builder, List<Map<String, Object>> jurisprudenceItems) {
        builder.append("## Jurisprudência aderente\n\n");
        if (jurisprudenceItems.isEmpty()) {
            builder.append("- A janela jurisprudencial oficial ainda precisa de massa semântica adicional ou refinamento humano.\n\n");
            return;
        }
        for (Map<String, Object> item : jurisprudenceItems) {
            builder.append("- ")
                    .append(stringValue(item.get("titulo"), stringValue(item.get("identificador"), "Precedente oficial")))
                    .append(": ")
                    .append(stringValue(item.get("tese"), stringValue(item.get("ementaResumo"), "ementa resumida indisponível")))
                    .append(" [")
                    .append(stringValue(item.get("fonte"), "fonte oficial"))
                    .append("]\n");
        }
        builder.append("\n");
    }

    private void appendTimeline(StringBuilder builder, List<Map<String, Object>> timelineItems) {
        builder.append("## Timeline do caso\n\n");
        if (timelineItems.isEmpty()) {
            builder.append("- Timeline ainda incompleta; consolidar fatos datados, decisão/ciência e eventos probatórios antes do protocolo.\n\n");
            return;
        }
        for (Map<String, Object> item : timelineItems) {
            builder.append("- ")
                    .append(stringValue(item.get("title"), stringValue(item.get("phase"), "Evento")));
            String dateHint = stringValue(item.get("dateHint"), null);
            if (dateHint != null) {
                builder.append(" [").append(dateHint).append("]");
            }
            builder.append(": ")
                    .append(stringValue(item.get("detail"), "Detalhe pendente."))
                    .append("\n");
        }
        builder.append("\n");
    }

    private void appendProofMatrix(StringBuilder builder, List<Map<String, Object>> proofMatrixItems) {
        builder.append("## Matriz prova x pedido\n\n");
        if (proofMatrixItems.isEmpty()) {
            builder.append("- A matriz prova x pedido ainda não foi consolidada; revisar pedidos e suportes documentais antes do protocolo.\n\n");
            return;
        }
        for (Map<String, Object> item : proofMatrixItems) {
            builder.append("- ")
                    .append(stringValue(item.get("requestLabel"), "Pedido sem rótulo"))
                    .append(" — força: ")
                    .append(stringValue(item.get("strength"), "A DEFINIR"))
                    .append(". Fatos: ")
                    .append(listOfStrings(item.get("supportFacts")).isEmpty() ? "nenhum" : String.join("; ", listOfStrings(item.get("supportFacts"))))
                    .append(". Provas: ")
                    .append(listOfStrings(item.get("supportEvidence")).isEmpty() ? "nenhuma" : String.join("; ", listOfStrings(item.get("supportEvidence"))))
                    .append(". Fundamentos: ")
                    .append(listOfStrings(item.get("supportGrounds")).isEmpty() ? "nenhum" : String.join("; ", listOfStrings(item.get("supportGrounds"))))
                    .append(".\n");
        }
        builder.append("\n");
    }

    private void appendEvidence(StringBuilder builder, List<Map<String, Object>> evidenceItems, List<String> proofs) {
        builder.append("## Provas e anexos inteligentemente vinculados\n\n");
        if (!evidenceItems.isEmpty()) {
            for (Map<String, Object> item : evidenceItems) {
                builder.append("- ")
                        .append(stringValue(item.get("label"), "Evidência sem rótulo"))
                        .append(": ")
                        .append(stringValue(item.get("summary"), "Resumo probatório indisponível."))
                        .append("\n");
            }
        } else if (!proofs.isEmpty()) {
            appendBullets(builder, proofs);
        } else {
            builder.append("- Vincular os documentos e mídias relevantes a cada fato e pedido antes do protocolo.\n");
        }
        builder.append("\n");
    }

    private String profileCode(Map<String, Object> procedure) {
        String family = petitionFamily(procedure);
        if ("EMBARGOS".equals(family)) {
            return "PETITION_STUDIO_EMBARGOS_V3";
        }
        if (isRecursalFamily(family)) {
            return "PETITION_STUDIO_RECURSAL_V3";
        }
        return "PETICAO_RAPIDA_ASSISTIDA_V2";
    }

    private String petitionFamily(Map<String, Object> procedure) {
        return stringValue(procedure == null ? null : procedure.get("petitionFamily"), "PETICAO_BASE");
    }

    private boolean isRecursalFamily(String family) {
        String normalized = trimToNull(family);
        return normalized != null && !"PETICAO_BASE".equals(normalized);
    }

    private List<String> documentLabels(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        ArrayList<String> labels = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                String label = trimToNull(stringValue(map.get("label"), null));
                if (label != null) {
                    labels.add(label);
                }
            }
        }
        return labels.isEmpty() ? List.of() : List.copyOf(labels);
    }

    private Map<String, Object> mapOf(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return Map.of();
        }
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        map.forEach((key, entry) -> {
            if (key != null) {
                out.put(String.valueOf(key), entry);
            }
        });
        return Collections.unmodifiableMap(out);
    }

    private String buildJuizo(Map<String, Object> procedure) {
        String justica = stringValue(procedure.get("justicaSugerida"), "JUSTIÇA COMPETENTE");
        String unidade = stringValue(procedure.get("unidadeSugerida"), null);
        String comarca = stringValue(procedure.get("comarca"), null);
        String uf = stringValue(procedure.get("uf"), null);
        StringBuilder builder = new StringBuilder("AO JUÍZO COMPETENTE DA ").append(justica.toUpperCase(Locale.ROOT));
        if (unidade != null) {
            builder.append(" - ").append(unidade.toUpperCase(Locale.ROOT));
        }
        if (comarca != null) {
            builder.append(" - ").append(comarca.toUpperCase(Locale.ROOT));
        }
        if (uf != null) {
            builder.append('/').append(uf.toUpperCase(Locale.ROOT));
        }
        return builder.toString();
    }

    private void appendBullets(StringBuilder builder, List<String> values) {
        for (String value : values) {
            String normalized = trimToNull(value);
            if (normalized == null) {
                continue;
            }
            builder.append("- ").append(normalized).append("\n");
        }
    }

    private List<String> selectList(List<String> primary, List<String> fallback) {
        List<String> first = sanitize(primary);
        return first.isEmpty() ? sanitize(fallback) : first;
    }

    private List<String> sanitize(List<String> values) {
        ArrayList<String> out = new ArrayList<>();
        if (values == null) {
            return List.of();
        }
        for (String value : values) {
            String normalized = trimToNull(value);
            if (normalized != null && !out.contains(normalized)) {
                out.add(normalized);
            }
        }
        return out.isEmpty() ? List.of() : List.copyOf(out);
    }

    private List<Map<String, Object>> listOfMaps(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        ArrayList<Map<String, Object>> out = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                LinkedHashMap<String, Object> normalized = new LinkedHashMap<>();
                map.forEach((key, val) -> {
                    if (key != null) {
                        normalized.put(String.valueOf(key), val);
                    }
                });
                out.add(Map.copyOf(normalized));
            }
        }
        return out.isEmpty() ? List.of() : List.copyOf(out);
    }

    private List<String> listOfStrings(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        ArrayList<String> out = new ArrayList<>();
        for (Object item : list) {
            String normalized = trimToNull(item == null ? null : String.valueOf(item));
            if (normalized != null && !out.contains(normalized)) {
                out.add(normalized);
            }
        }
        return out.isEmpty() ? List.of() : List.copyOf(out);
    }

    private List<String> mergeDistinct(List<String>... values) {
        ArrayList<String> out = new ArrayList<>();
        if (values == null) {
            return List.of();
        }
        for (List<String> block : values) {
            for (String item : sanitize(block)) {
                if (!out.contains(item)) {
                    out.add(item);
                }
            }
        }
        return out.isEmpty() ? List.of() : List.copyOf(out);
    }

    private String stringValue(Object value, String fallback) {
        String normalized = trimToNull(value == null ? null : String.valueOf(value));
        return normalized == null ? fallback : normalized;
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

    private String formatCurrency(BigDecimal value) {
        return NumberFormat.getCurrencyInstance(Locale.of("pt", "BR")).format(value);
    }

    public record ResolveRequest(String title,
                                 Map<String, Object> partes,
                                 Map<String, Object> procedure,
                                 Map<String, Object> evidence,
                                 Map<String, Object> jurisprudence,
                                 Map<String, Object> caseTimeline,
                                 Map<String, Object> proofRequestMatrix,
                                 Map<String, Object> protocolChecklist,
                                 Map<String, Object> riskMatrix,
                                 LaianePeticaoInicialDraftService.DraftView manualDraft,
                                 List<String> requestFacts,
                                 List<String> requestGrounds,
                                 List<String> requestPedidos,
                                 List<String> requestProofs,
                                 BigDecimal valorCausa) {
        public ResolveRequest {
            partes = partes == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(partes));
            procedure = procedure == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(procedure));
            evidence = evidence == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(evidence));
            jurisprudence = jurisprudence == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(jurisprudence));
            caseTimeline = caseTimeline == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(caseTimeline));
            proofRequestMatrix = proofRequestMatrix == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(proofRequestMatrix));
            protocolChecklist = protocolChecklist == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(protocolChecklist));
            riskMatrix = riskMatrix == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(riskMatrix));
            requestFacts = requestFacts == null ? List.of() : List.copyOf(requestFacts);
            requestGrounds = requestGrounds == null ? List.of() : List.copyOf(requestGrounds);
            requestPedidos = requestPedidos == null ? List.of() : List.copyOf(requestPedidos);
            requestProofs = requestProofs == null ? List.of() : List.copyOf(requestProofs);
        }

        public static ResolveRequest empty() {
            return new ResolveRequest(null, Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), null, List.of(), List.of(), List.of(), List.of(), null);
        }
    }

    public record QuickDraftReport(String title,
                                   String markdown,
                                   List<String> checklist,
                                   Map<String, Object> envelope) {
        public QuickDraftReport {
            title = title == null || title.isBlank() ? "PETIÇÃO RÁPIDA" : title.trim();
            markdown = markdown == null || markdown.isBlank() ? "# Petição em elaboração\n" : markdown;
            checklist = checklist == null ? List.of() : List.copyOf(checklist);
            envelope = envelope == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(envelope));
        }
    }
}
