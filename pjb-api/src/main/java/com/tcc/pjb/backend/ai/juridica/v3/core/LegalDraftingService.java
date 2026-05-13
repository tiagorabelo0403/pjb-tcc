package com.tcc.pjb.backend.ai.juridica.v3.core;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import lombok.Data;

@Service
public class LegalDraftingService {

    public String draftPeticao(Map<String, Object> ctx) {
        DraftContext c = DraftContext.from(ctx);
        StringBuilder sb = new StringBuilder();

        sb.append("# PETIÇÃO INICIAL\n\n");
        sb.append("**Excelentíssimo(a) Senhor(a) Doutor(a) Juiz(a) de Direito da ");
        sb.append(nonBlank(c.orgaoJudiciario, "Vara Competente"));
        sb.append("**\n\n");

        sb.append("## 1. Partes\n");
        sb.append("**Autor(a):** ").append(nonBlank(c.autor, "[NOME DO AUTOR]"))
                .append("\n\n");
        sb.append("**Réu(É):** ").append(nonBlank(c.reu, "[NOME DO RÉU]"))
                .append("\n\n");

        sb.append("## 2. Síntese Fática\n");
        sb.append(nonBlank(c.fatos, "[DESCREVER OS FATOS COM CRONOLOGIA, PROVAS E CONTEXTO]"))
                .append("\n\n");

        sb.append("## 3. Fundamentos Jurídicos\n");
        sb.append(nonBlank(c.fundamentos, "[ENQUADRAR NORMAS, PRINCÍPIOS, JURISPRUDÊNCIA E PROVAS]"))
                .append("\n\n");

        if (!c.pleadingBlueprint.isEmpty() || c.litigationPosture != null || c.protocolReadiness != null) {
            sb.append("## 4. Arquitetura Estratégica do Caso\n");
            if (c.litigationPosture != null) {
                sb.append("**Postura contenciosa:** ").append(c.litigationPosture).append("\n\n");
            }
            if (c.protocolReadiness != null) {
                sb.append("**Prontidão protocolar:** ").append(c.protocolReadiness).append("\n\n");
            }
            sb.append(formatBullets(c.pleadingBlueprint, "[ESTRUTURAR FATO, TESE, PROVA E PEDIDO EM BLOCOS CONGRUENTES]"));
            sb.append("\n\n");
        }

        sb.append("## 5. Pedidos\n");
        sb.append(formatPedidos(c.pedidos));

        if (c.tutelaUrgencia != null) {
            sb.append("\n\n## 6. Tutela de Urgência\n");
            sb.append(nonBlank(c.tutelaUrgencia, "[REQUISITOS: PROBABILIDADE DO DIREITO E PERIGO DE DANO]"));
        }

        sb.append("\n\n## 7. Provas\n");
        sb.append(formatProvas(c.provas));

        if (!c.evidenceAgenda.isEmpty() || !c.protocolBlockers.isEmpty()) {
            sb.append("\n\n## 8. Agenda Probatória e Controles\n");
            sb.append(formatBullets(c.evidenceAgenda, "[MAPEAR DOCUMENTOS, COMUNICAÇÕES, LAUDOS E DEMAIS LASTROS DO CASO]"));
            if (!c.protocolBlockers.isEmpty()) {
                sb.append("\n\n**Bloqueios ou pontos críticos:**\n");
                sb.append(formatBullets(c.protocolBlockers, "[INDICAR GAPS OU RISCOS A SANEAR ANTES DO PROTOCOLO]"));
            }
        }

        sb.append("\n\n## 9. Valor da Causa\n");
        sb.append(nonBlank(c.valorCausa, "[VALOR]"));

        sb.append("\n\nTermos em que,\nPede deferimento.\n\n");
        sb.append(nonBlank(c.localData, "[LOCAL], [DATA]"));
        sb.append("\n\n");
        sb.append(nonBlank(c.assinatura, "[ASSINATURA / OAB]"));

        return sb.toString();
    }

    public String draftRecurso(Map<String, Object> ctx) {
        DraftContext c = DraftContext.from(ctx);
        StringBuilder sb = new StringBuilder();

        sb.append("# RECURSO\n\n");
        sb.append("**Processo:** ").append(nonBlank(c.numeroProcesso, "[NÚMERO]"))
                .append("\n\n");
        sb.append("**Recorrente:** ").append(nonBlank(c.autor, "[NOME]"))
                .append("\n\n");
        sb.append("**Recorrido:** ").append(nonBlank(c.reu, "[NOME]"))
                .append("\n\n");

        sb.append("## 1. Tempestividade\n");
        sb.append(nonBlank(c.tempestividade, "[DEMONSTRAR DATA DA INTIMAÇÃO E PRAZO LEGAL]"))
                .append("\n\n");

        sb.append("## 2. Síntese da Decisão Recorrida\n");
        sb.append(nonBlank(c.decisao, "[RESUMO DA DECISÃO E PONTOS IMPUGNADOS]"))
                .append("\n\n");

        sb.append("## 3. Razões Recursais\n");
        sb.append(nonBlank(c.fundamentos, "[ERRO DE FATO/DIREITO, PRECEDENTES, TESE]"))
                .append("\n\n");

        if (!c.pleadingBlueprint.isEmpty() || !c.controlPoints.isEmpty()) {
            sb.append("## 4. Arquitetura Recursal\n");
            sb.append(formatBullets(c.pleadingBlueprint.isEmpty() ? c.controlPoints : c.pleadingBlueprint, "[ORGANIZAR PRELIMINARES, ERRO DE JULGAMENTO, PROVA E PEDIDO RECURSAL]"));
            sb.append("\n\n");
        }

        sb.append("## 5. Pedidos\n");
        sb.append(formatPedidos(c.pedidos));

        sb.append("\n\nTermos em que,\nPede deferimento.\n\n");
        sb.append(nonBlank(c.localData, "[LOCAL], [DATA]"));
        sb.append("\n\n");
        sb.append(nonBlank(c.assinatura, "[ASSINATURA / OAB]"));

        return sb.toString();
    }

    public String draftSentenca(Map<String, Object> ctx) {
        DraftContext c = DraftContext.from(ctx);
        Map<String, Object> decisionBlueprint = asMap(ctx != null ? ctx.get("decision_blueprint") : null);
        StringBuilder sb = new StringBuilder();

        sb.append("# SENTENÇA\n\n");
        sb.append("**Processo:** ").append(nonBlank(c.numeroProcesso, "[NÚMERO]"))
                .append("\n\n");
        sb.append("**Juízo/Órgão:** ").append(nonBlank(c.orgaoJudiciario, "Vara Competente"))
                .append("\n\n");

        sb.append("## I. RELATÓRIO\n");
        sb.append(nonBlank(c.fatos, "[SÍNTESE DO PROCESSO: partes, pedidos, fatos relevantes, andamento e provas essenciais]"))
                .append("\n\n");

        sb.append("## II. FUNDAMENTAÇÃO\n");
        sb.append(nonBlank(c.fundamentos, "[ANALISAR PRELIMINARES, MÉRITO, ÔNUS DA PROVA, JURISPRUDÊNCIA E NORMAS APLICÁVEIS]"))
                .append("\n\n");

        List<String> legalAnchors = asStringList(decisionBlueprint.get("legalAnchors"));
        List<String> reasoningChecklist = asStringList(decisionBlueprint.get("reasoningChecklist"));
        List<String> pendingFacts = asStringList(decisionBlueprint.get("pendingFacts"));
        Map<String, Object> fillableVariables = asMap(decisionBlueprint.get("fillableVariables"));

        if (!legalAnchors.isEmpty()) {
            sb.append("### 2.1. Vetores normativos mínimos\n");
            sb.append(formatBullets(legalAnchors, "[INDICAR BASE NORMATIVA MÍNIMA DO PROVIMENTO]"));
            sb.append("\n\n");
        }

        if (!reasoningChecklist.isEmpty()) {
            sb.append("### 2.2. Pontos obrigatórios de enfrentamento\n");
            sb.append(formatBullets(reasoningChecklist, "[EXPLICITAR QUESTÕES DE FATO E DE DIREITO INDISPENSÁVEIS À FUNDAMENTAÇÃO]"));
            sb.append("\n\n");
        }

        sb.append("### 2.3. Provas\n");
        sb.append(formatProvas(c.provas)).append("\n\n");

        sb.append("### 2.4. Pedidos\n");
        sb.append(formatPedidos(c.pedidos)).append("\n\n");

        if (!pendingFacts.isEmpty()) {
            sb.append("### 2.5. Pontos que exigem validação judicial expressa\n");
            sb.append(formatBullets(pendingFacts, "[VALIDAR FATOS OU CONDIÇÕES PROCESSUAIS ANTES DA PUBLICAÇÃO]"));
            sb.append("\n\n");
        }

        sb.append("## III. DISPOSITIVO\n");
        String dispositiveBase = asString(decisionBlueprint.get("dispositiveBase"));
        if (dispositiveBase != null) {
            sb.append(dispositiveBase).append("\n\n");
        } else {
            sb.append("Diante do exposto, ");
            sb.append("[JULGO PROCEDENTE/IMPROCEDENTE/PARCIALMENTE PROCEDENTE] ");
            sb.append("o(s) pedido(s), nos termos acima, com as consequências legais.\n\n");
        }

        if (c.dispositivoExtra != null) {
            sb.append(c.dispositivoExtra).append("\n\n");
        } else if (!fillableVariables.isEmpty()) {
            sb.append("### 3.1. Variáveis de preenchimento obrigatório\n");
            for (Map.Entry<String, Object> entry : fillableVariables.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) {
                    continue;
                }
                sb.append("- ").append(entry.getKey()).append(": ").append(String.valueOf(entry.getValue())).append("\n");
            }
            sb.append("\n");
        } else {
            sb.append("[FIXAR CONDENAÇÃO/OBRIGAÇÃO, HONORÁRIOS, CUSTAS, CORREÇÃO/MORA, TUTELA, E DEMAIS PROVIDÊNCIAS]\n\n");
        }

        sb.append("Publique-se. Registre-se. Intimem-se.\n\n");
        sb.append(nonBlank(c.localData, "[LOCAL], [DATA]")).append("\n\n");
        sb.append(nonBlank(c.assinatura, "[ASSINATURA / MAGISTRADO]"));

        return sb.toString();
    }

    public String draftContrato(Map<String, Object> ctx) {
        DraftContext c = DraftContext.from(ctx);
        StringBuilder sb = new StringBuilder();

        sb.append("# CONTRATO\n\n");
        sb.append("**Partes:** ").append(nonBlank(c.autor, "[CONTRATANTE]"))
                .append(" x ").append(nonBlank(c.reu, "[CONTRATADA]"))
                .append("\n\n");

        sb.append("## 1. Objeto\n");
        sb.append(nonBlank(c.objeto, "[DESCREVER OBJETO CONTRATUAL]"))
                .append("\n\n");

        sb.append("## 2. Preço e Forma de Pagamento\n");
        sb.append(nonBlank(c.valorCausa, "[VALOR / CONDIÇÕES]"))
                .append("\n\n");

        sb.append("## 3. Prazo e Vigência\n");
        sb.append(nonBlank(c.prazo, "[INÍCIO, TÉRMINO, RENOVAÇÃO]"))
                .append("\n\n");

        sb.append("## 4. Obrigações das Partes\n");
        sb.append(nonBlank(c.obrigacoes, "[OBRIGAÇÕES, ENTREGAS, SLA, RESPONSABILIDADES]"))
                .append("\n\n");

        sb.append("## 5. Rescisão e Penalidades\n");
        sb.append(nonBlank(c.rescisao, "[HIPÓTESES DE RESCISÃO, MULTAS, CLÁUSULAS]"))
                .append("\n\n");

        sb.append("## 6. Foro\n");
        sb.append(nonBlank(c.foro, "[FORO]"))
                .append("\n\n");

        sb.append(nonBlank(c.localData, "[LOCAL], [DATA]"));
        sb.append("\n\n");
        sb.append(nonBlank(c.assinatura, "[ASSINATURAS]"));

        return sb.toString();
    }

    private static String formatPedidos(List<String> pedidos) {
        if (pedidos == null || pedidos.isEmpty()) {
            return "- [LISTAR PEDIDOS DE FORMA OBJETIVA E NUMERADA]";
        }
        StringBuilder sb = new StringBuilder();
        int i = 1;
        for (String p : pedidos) {
            if (p == null || p.isBlank()) continue;
            sb.append(i++).append(") ").append(p.trim()).append("\n");
        }
        if (sb.isEmpty()) {
            return "- [LISTAR PEDIDOS DE FORMA OBJETIVA E NUMERADA]";
        }
        return sb.toString().trim();
    }

    private static String formatBullets(List<String> items, String fallback) {
        if (items == null || items.isEmpty()) {
            return fallback;
        }
        StringBuilder sb = new StringBuilder();
        for (String item : items) {
            if (item == null || item.isBlank()) continue;
            sb.append("- ").append(item.trim()).append("\n");
        }
        if (sb.isEmpty()) {
            return fallback;
        }
        return sb.toString().trim();
    }

    private static String formatProvas(List<String> provas) {
        if (provas == null || provas.isEmpty()) {
            return "- [ROL DE DOCUMENTOS / TESTEMUNHAS / PERÍCIA / OUTROS]";
        }
        StringBuilder sb = new StringBuilder();
        for (String p : provas) {
            if (p == null || p.isBlank()) continue;
            sb.append("- ").append(p.trim()).append("\n");
        }
        if (sb.isEmpty()) {
            return "- [ROL DE DOCUMENTOS / TESTEMUNHAS / PERÍCIA / OUTROS]";
        }
        return sb.toString().trim();
    }

    private static String nonBlank(String v, String fallback) {
        return (v == null || v.isBlank()) ? fallback : v.trim();
    }

    @SuppressWarnings("unchecked")
    private static List<String> asStringList(Object o) {
        if (o == null) return List.of();
        List<String> out = new java.util.ArrayList<>();
        if (o instanceof List<?> l) {
            for (Object x : l) {
                if (x == null) continue;
                addStructured(out, String.valueOf(x));
            }
            return out.stream().distinct().toList();
        }
        addStructured(out, String.valueOf(o));
        return out.stream().distinct().toList();
    }

    private static void addStructured(List<String> out, String raw) {
        if (raw == null) return;
        String normalized = raw.replace('\r', '\n');
        String[] parts = normalized.split("\\R|;|\\|");
        for (String part : parts) {
            String s = part == null ? null : part.replaceFirst("^[\\-•*\\d.)\\s]+", "").trim();
            if (s != null && !s.isBlank()) {
                out.add(s);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object o) {
        if (o instanceof Map<?, ?> raw) {
            java.util.LinkedHashMap<String, Object> out = new java.util.LinkedHashMap<>();
            raw.forEach((k, v) -> {
                if (k != null && v != null) {
                    out.put(String.valueOf(k), v);
                }
            });
            return java.util.Collections.unmodifiableMap(out);
        }
        return java.util.Map.of();
    }

    private static String asString(Object o) {
        if (o == null) return null;
        String s = String.valueOf(o).trim();
        return s.isBlank() ? null : s;
    }

    private static String asJoinedString(Object o) {
        List<String> items = asStringList(o);
        if (items.isEmpty()) {
            return asString(o);
        }
        return String.join("; ", items);
    }

    @Data
    private static class DraftContext {
        String orgaoJudiciario;
        String autor;
        String reu;
        String fatos;
        String fundamentos;
        List<String> pedidos;
        String tutelaUrgencia;
        List<String> provas;
        String valorCausa;
        String numeroProcesso;
        String tempestividade;
        String decisao;
        String dispositivoExtra;
        String objeto;
        String prazo;
        String obrigacoes;
        String rescisao;
        String foro;
        String localData;
        String assinatura;
        String litigationPosture;
        String protocolReadiness;
        String negotiationStance;
        String evidenceReadiness;
        List<String> pleadingBlueprint;
        List<String> evidenceAgenda;
        List<String> protocolBlockers;
        List<String> negotiationGuardrails;
        List<String> controlPoints;

        static DraftContext from(Map<String, Object> ctx) {
            DraftContext c = new DraftContext();
            if (ctx == null) ctx = java.util.Map.of();
            c.orgaoJudiciario = asString(first(ctx, "orgao_judiciario", "vara", "juizo"));
            c.autor = asString(first(ctx, "autor", "requerente", "impetrante", "contratante", "parte_autora_nome"));
            c.reu = asString(first(ctx, "reu", "requerido", "impetrado", "contratada", "parte_reu_nome"));
            c.fatos = asString(first(ctx, "fatos", "narrativa", "resumo", "texto_fatos_resumido", "objeto_processual"));
            c.fundamentos = asJoinedString(first(ctx, "fundamentos", "fundamentacao", "tese", "teses", "thesis_vectors", "tese_principal"));
            c.pedidos = asStringList(first(ctx, "pedidos", "pedido", "pedido_principal", "pedidos_consolidados"));
            c.tutelaUrgencia = asString(first(ctx, "tutela_urgencia", "liminar", "tutelaUrgencia"));
            c.provas = asStringList(first(ctx, "provas", "documentos", "evidenceAnchors", "material_probatorio_resumo"));
            c.valorCausa = asString(first(ctx, "valor_causa", "valor", "valorCausa"));
            c.numeroProcesso = asString(first(ctx, "numero_processo", "processo", "nupn", "numeroUnificado"));
            c.tempestividade = asString(first(ctx, "tempestividade"));
            c.decisao = asString(first(ctx, "decisao", "sentenca"));
            c.dispositivoExtra = asString(first(ctx, "dispositivo_extra", "dispositivo"));
            c.objeto = asString(first(ctx, "objeto", "objeto_processual"));
            c.prazo = asString(first(ctx, "prazo", "vigencia"));
            c.obrigacoes = asString(first(ctx, "obrigacoes"));
            c.rescisao = asString(first(ctx, "rescisao"));
            c.foro = asString(first(ctx, "foro"));
            c.localData = asString(first(ctx, "local_data", "data"));
            c.assinatura = asString(first(ctx, "assinatura"));
            c.litigationPosture = asString(first(ctx, "litigation_posture", "litigationPosture", "estrategiaContenciosa"));
            c.protocolReadiness = asString(first(ctx, "protocol_readiness", "protocolReadiness", "prontidaoProtocolar"));
            c.negotiationStance = asString(first(ctx, "negotiation_stance", "negotiationStance", "posturaNegocial"));
            c.evidenceReadiness = asString(first(ctx, "evidence_readiness", "evidenceReadiness", "maturidadeProbatoria"));
            c.pleadingBlueprint = asStringList(first(ctx, "pleading_blueprint", "pleadingBlueprint", "petitionSections", "planoEstrutural"));
            c.evidenceAgenda = asStringList(first(ctx, "evidence_agenda", "evidenceAgenda"));
            c.protocolBlockers = asStringList(first(ctx, "protocol_blockers", "protocolBlockers", "proofGaps", "gapsEstrategicos"));
            c.negotiationGuardrails = asStringList(first(ctx, "negotiation_guardrails", "negotiationGuardrails"));
            c.controlPoints = asStringList(first(ctx, "control_points", "controlPoints"));
            return c;
        }

        private static Object first(Map<String, Object> ctx, String... keys) {
            for (String k : keys) {
                if (k == null) continue;
                Object v = ctx.get(k);
                if (v == null) continue;
                if (v instanceof String s) {
                    if (!s.isBlank()) return s;
                } else {
                    return v;
                }
            }
            return null;
        }
    }
}
