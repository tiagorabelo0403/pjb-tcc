package com.tcc.pjb.backend.core.kernel.advisory;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

final class ProcessMaterialDossierHeuristics {

    private final ProcessMaterialDossierTextSupport textSupport;

    ProcessMaterialDossierHeuristics(ProcessMaterialDossierTextSupport textSupport) {
        this.textSupport = Objects.requireNonNull(textSupport);
    }

    ProcessMaterialDossierAnalysis analyze(ProcessMaterialDossierInput input) {
        String normalizedText = textSupport.normalize(input.narrative());
        String normalizedEvidence = textSupport.normalize(input.evidenceText());
        int evidenceDensity = density(input.evidenceItems(), normalizedEvidence, normalizedText);
        int pedidoDensity = Math.max(input.claims().size(), textSupport.countKeywords(normalizedText, Set.of("PEDIDO", "CONDENACAO", "OBRIGACAO", "REVISAO", "TUTELA", "DECLARACAO")));
        int controversyDensity = textSupport.countKeywords(normalizedText, Set.of(
                "CONTRATO", "COBRAN", "INDENIZ", "RESPONSABIL", "SAUDE", "BENEFICIO", "ALIMENTO",
                "GUARDA", "POSSE", "USUCAP", "RESCISAO", "ANULACAO", "CONSUMIDOR", "TRABALHO", "TRIBUT"
        ));
        LinkedHashSet<String> controversyAxes = new LinkedHashSet<>(deriveControversyAxes(input, normalizedText));
        LinkedHashSet<String> thesisVectors = new LinkedHashSet<>(deriveThesisVectors(input, normalizedText));
        LinkedHashSet<String> evidenceAnchors = new LinkedHashSet<>(deriveEvidenceAnchors(input, normalizedEvidence, normalizedText));
        LinkedHashSet<String> proofGaps = new LinkedHashSet<>(deriveProofGaps(input, normalizedEvidence, normalizedText));
        LinkedHashSet<String> petitionSections = new LinkedHashSet<>(derivePetitionSections(input, normalizedText));
        LinkedHashSet<String> settlementLevers = new LinkedHashSet<>(deriveSettlementLevers(input, normalizedText));
        LinkedHashSet<String> protocolChecklist = ProcessMaterialDossierTextSupport.mergeOrderedSet(
                deriveProtocolChecklist(input, normalizedText),
                textSupport.limitNormalized(input.riskSignals(), 4)
        );
        if (evidenceAnchors.isEmpty()) {
            evidenceAnchors.add("Mapear lastro probatório mínimo antes do próximo ato relevante.");
        }
        if (proofGaps.isEmpty()) {
            proofGaps.add("Sem lacuna probatória crítica evidente na leitura automatizada atual.");
        }
        if (settlementLevers.isEmpty()) {
            settlementLevers.add("Negociação deve permanecer vinculada ao objeto, ao pedido e à executabilidade do resultado.");
        }
        String evidentiaryBracket = classifyEvidence(input.evidenceScore(), evidenceDensity, proofGaps.size());
        String negotiationBracket = classifyNegotiation(input.negotiationScore(), normalizedText);
        int dossierReadinessScore = dossierReadinessScore(input, evidenceDensity, proofGaps.size(), protocolChecklist.size());
        String attentionBand = attentionBand(dossierReadinessScore, proofGaps.size(), input);
        String objectLabel = textSupport.truncate(ProcessMaterialDossierTextSupport.firstNonBlank(input.objectLabel(), summarizeObject(normalizedText)), 240);
        String primaryRelief = textSupport.truncate(ProcessMaterialDossierTextSupport.firstNonBlank(input.primaryRelief(), inferRelief(normalizedText)), 240);
        return new ProcessMaterialDossierAnalysis(
                objectLabel,
                primaryRelief,
                evidentiaryBracket,
                negotiationBracket,
                List.copyOf(controversyAxes),
                List.copyOf(thesisVectors),
                List.copyOf(evidenceAnchors),
                List.copyOf(proofGaps),
                List.copyOf(petitionSections),
                List.copyOf(settlementLevers),
                List.copyOf(protocolChecklist),
                evidenceDensity,
                pedidoDensity,
                controversyDensity,
                dossierReadinessScore,
                attentionBand,
                executiveSummary(input, evidentiaryBracket, negotiationBracket, proofGaps, objectLabel, primaryRelief),
                strategicFocus(proofGaps, settlementLevers, protocolChecklist)
        );
    }

    private List<String> deriveControversyAxes(ProcessMaterialDossierInput input, String normalizedText) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        String objectLabel = ProcessMaterialDossierTextSupport.firstNonBlank(input.objectLabel(), summarizeObject(normalizedText), "caso");
        out.add("Delimitar com precisão o objeto litigioso: " + textSupport.truncate(objectLabel, 160) + ".");
        if (!input.claims().isEmpty()) {
            out.add("Conectar cada pedido a fato, fundamento e consequência prática para evitar dispersão decisória.");
        }
        if (textSupport.containsAny(normalizedText, "CONTRATO", "ADITIVO", "RESCISAO", "CLAUSULA")) {
            out.add("Fixar a cronologia contratual, inadimplemento alegado e cláusulas diretamente controvertidas.");
        }
        if (textSupport.containsAny(normalizedText, "SAUDE", "MEDICAMENTO", "TRATAMENTO", "CIRURGIA")) {
            out.add("Distinguir urgência clínica, cobertura devida e risco concreto de dano na linha narrativa central.");
        }
        if (textSupport.containsAny(normalizedText, "CONSUMIDOR", "FATURA", "COBRAN", "NEGATIVAC", "SERVICO")) {
            out.add("Separar falha de serviço, dano experimentado e resposta do fornecedor em eixos autônomos.");
        }
        if (textSupport.containsAny(normalizedText, "ALIMENTO", "GUARDA", "VISITA", "CONVIV")) {
            out.add("Materializar necessidade atual, capacidade contributiva e interesse da pessoa vulnerável em blocos distintos.");
        }
        if (textSupport.containsAny(normalizedText, "BENEFICIO", "INSS", "PREVIDENCI")) {
            out.add("Demonstrar DER, qualidade de segurado e prova do requisito específico do benefício postulado.");
        }
        if (textSupport.containsAny(normalizedText, "TRABALHO", "VERBA", "HORA EXTRA", "RESCIS")) {
            out.add("Organizar período contratual, verbas inadimplidas e documentos de jornada ou pagamento.");
        }
        if (textSupport.containsAny(normalizedText, "TRIBUT", "EXECUCAO FISCAL", "CDA")) {
            out.add("Separar legalidade do crédito, constituição tributária e eventual excesso ou nulidade formal.");
        }
        return List.copyOf(out);
    }

    private List<String> deriveThesisVectors(ProcessMaterialDossierInput input, String normalizedText) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        String ramo = textSupport.normalize(input.ramoDireito());
        if (textSupport.containsAny(normalizedText, "CONTRATO", "RESCISAO", "INADIMPLEMENT")) {
            out.add("Tese de inadimplemento qualificado com correlação entre obrigação assumida, quebra contratual e recomposição integral.");
        }
        if (textSupport.containsAny(normalizedText, "INDENIZ", "DANO MORAL", "DANO MATERIAL")) {
            out.add("Tese de responsabilidade civil com foco em nexo causal, dano comprovável e extensão econômica da reparação.");
        }
        if (textSupport.containsAny(normalizedText, "OBRIGACAO DE FAZER", "FORNECIMENTO", "AUTORIZACAO", "ENTREGA")) {
            out.add("Tese mandamental voltada à utilidade prática imediata, com coerção executiva e obrigação específica bem desenhada.");
        }
        if (textSupport.containsAny(normalizedText, "NEGATIVAC", "COBRAN", "FATURA")) {
            out.add("Tese de cobrança indevida ou falha de serviço com inversão argumentativa centrada em documentos financeiros e comunicação prévia.");
        }
        if (textSupport.containsAny(normalizedText, "ALIMENTO", "GUARDA", "VISITA")) {
            out.add("Tese de proteção integral e proporcionalidade familiar, sustentada por necessidade atual e capacidade contributiva demonstrável.");
        }
        if (textSupport.containsAny(normalizedText, "BENEFICIO", "INSS", "AUXILIO", "APOSENT")) {
            out.add("Tese previdenciária dependente de qualidade de segurado, carência e prova material convergente com a narrativa fática.");
        }
        if (textSupport.containsAny(normalizedText, "TRABALHO", "VERBA", "HORAS", "FGTS")) {
            out.add("Tese trabalhista ancorada em subordinação, jornada, recibos e ônus dinâmico da prova conforme a controvérsia instalada.");
        }
        if (textSupport.containsAny(ramo, "TRIBUTARIO") || textSupport.containsAny(normalizedText, "TRIBUT", "ICMS", "IPI", "IPTU", "ISS")) {
            out.add("Tese tributária com ênfase em legalidade estrita, base de cálculo e eventual vício formal do lançamento ou cobrança.");
        }
        if (out.isEmpty()) {
            out.add("Tese principal deve unir fato nuclear, regra incidente e remédio jurisdicional sem duplicidades argumentativas.");
        }
        return List.copyOf(out);
    }

    private List<String> deriveEvidenceAnchors(ProcessMaterialDossierInput input,
                                               String normalizedEvidence,
                                               String normalizedText) {
        LinkedHashSet<String> out = new LinkedHashSet<>(input.evidenceItems().stream()
                .map(textSupport::sanitizeBullet)
                .filter(s -> s != null && !s.isBlank())
                .limit(6)
                .map(v -> "Âncora documental: " + textSupport.truncate(v, 180) + ".")
                .toList());
        if (textSupport.containsAny(normalizedEvidence, "CONTRATO", "ADITIVO") || textSupport.containsAny(normalizedText, "CONTRATO", "ADITIVO")) {
            out.add("Instrumento contratual e aditivos devem ancorar objeto, obrigação e inadimplemento alegado.");
        }
        if (textSupport.containsAny(normalizedEvidence, "FATURA", "BOLETO", "RECIBO", "PIX", "EXTRATO", "COMPROVANTE")) {
            out.add("Documentos financeiros precisam fechar origem, vencimento, adimplemento parcial e saldo controvertido.");
        }
        if (textSupport.containsAny(normalizedEvidence, "WHATSAPP", "EMAIL", "MENSAGEM", "PRINT", "AUDIO", "VIDEO")) {
            out.add("Comunicações digitais devem ser vinculadas a autoria, data, contexto e integridade mínima do conteúdo.");
        }
        if (textSupport.containsAny(normalizedEvidence, "LAUDO", "RELATORIO", "ATESTADO", "PERICIA")) {
            out.add("Laudos e relatórios técnicos devem ser descritos como prova do requisito específico controvertido.");
        }
        if (textSupport.containsAny(normalizedEvidence, "TESTEMUNHA", "DECLARACAO")) {
            out.add("Prova oral deve ser conectada a fatos delimitados para evitar testemunho genérico ou redundante.");
        }
        return List.copyOf(out);
    }

    private List<String> deriveProofGaps(ProcessMaterialDossierInput input,
                                         String normalizedEvidence,
                                         String normalizedText) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (input.evidenceItems().isEmpty() && textSupport.blank(input.evidenceText())) {
            out.add("Ausência de lastro probatório resumido exige inventário mínimo de documentos antes do protocolo ou da rodada negocial.");
        }
        if (!input.authorIdPresent()) {
            out.add("Falta identificador mínimo do polo ativo para estabilizar qualificação e rastreabilidade documental.");
        }
        if (!input.counterpartyIdPresent()) {
            out.add("Falta identificador mínimo do polo passivo para robustecer citação, contraditório e coerência cadastral.");
        }
        if (textSupport.containsAny(normalizedText, "URG", "LIMINAR", "TUTELA") && !textSupport.containsAny(normalizedEvidence, "LAUDO", "ATESTADO", "RELATORIO", "NOTIFICACAO", "COMPROVANTE")) {
            out.add("Pedido urgente sem âncora documental explícita reduz densidade de probabilidade do direito e perigo de dano.");
        }
        if (textSupport.containsAny(normalizedText, "CONTRATO", "INADIMPLEMENT") && !textSupport.containsAny(normalizedEvidence, "CONTRATO", "ADITIVO", "FATURA", "RECIBO", "COMPROVANTE")) {
            out.add("Controvérsia contratual pede ao menos contrato, cobrança correlata e rastro de adimplemento ou inadimplemento.");
        }
        if (textSupport.containsAny(normalizedText, "BENEFICIO", "INSS") && !textSupport.containsAny(normalizedEvidence, "CNIS", "ATESTADO", "LAUDO", "CERTIDAO", "COMPROVANTE")) {
            out.add("Pretensão previdenciária demanda prova material mínima do requisito legal e do vínculo contributivo ou fático pertinente.");
        }
        if (textSupport.containsAny(normalizedText, "TRABALHO", "HORA", "VERBA") && !textSupport.containsAny(normalizedEvidence, "HOLERITE", "RECIBO", "CARTAO", "PONTO", "FGTS", "CTPS")) {
            out.add("Pedido trabalhista carece de documentos de jornada, recibos ou registros que delimitem a extensão do crédito.");
        }
        return List.copyOf(out);
    }

    private List<String> derivePetitionSections(ProcessMaterialDossierInput input, String normalizedText) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        out.add("Quadro fático cronológico com marcos temporais, documentos-chave e identificação dos atores centrais.");
        out.add("Bloco de fundamentos jurídicos separado por tese, requisito legal e prova correspondente.");
        out.add("Capítulo de pedidos com versão principal, subsidiária, critérios de liquidação e parâmetros executivos.");
        if (textSupport.containsAny(normalizedText, "URG", "LIMINAR", "TUTELA")) {
            out.add("Seção autônoma de tutela de urgência com risco concreto, reversibilidade e utilidade prática da medida.");
        }
        if (textSupport.containsAny(normalizedText, "INDENIZ", "DANO MORAL", "DANO MATERIAL")) {
            out.add("Memória de quantificação da reparação, distinguindo dano material, moral e consectários legais.");
        }
        if (textSupport.containsAny(normalizedText, "CONTRATO", "COBRAN", "TRIBUT", "BENEFICIO")) {
            out.add("Anexo narrativo ou quadro-resumo com valores, eventos e documentos de suporte por item controvertido.");
        }
        if (!textSupport.blank(input.ritoName())) {
            out.add("Adequação formal ao rito " + input.ritoName() + " com checagem de competência, preparo e documentos obrigatórios.");
        }
        return List.copyOf(out);
    }

    private List<String> deriveSettlementLevers(ProcessMaterialDossierInput input, String normalizedText) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (input.negotiationScore() != null && input.negotiationScore() >= 70) {
            out.add("Existe margem para proposta ancorada em objeto delimitado, prova suficiente e execução objetiva do resultado.");
        }
        if (textSupport.containsAny(normalizedText, "COBRAN", "PARCEL", "NEGOCI", "ACORDO", "CONCILI", "INDENIZ")) {
            out.add("Levar faixa econômica escalonada e cronograma de cumprimento tende a reduzir fricção negocial inicial.");
        }
        if (textSupport.containsAny(normalizedText, "OBRIGACAO DE FAZER", "FORNECIMENTO", "SAUDE", "ENTREGA")) {
            out.add("A composição deve privilegiar executabilidade imediata, prazo curto e gatilhos claros de inadimplemento.");
        }
        if (textSupport.containsAny(normalizedText, "FAMILIA", "ALIMENTO", "GUARDA", "VISITA")) {
            out.add("Negociação precisa preservar estabilidade relacional, calendário verificável e mecanismos de revisão equilibrada.");
        }
        if (textSupport.containsAny(normalizedText, "TRABALHO", "VERBA", "RESCISAO")) {
            out.add("Parcelamento e discriminação de verbas podem ampliar aderência sem perder segurança de cumprimento.");
        }
        if (input.valorCausa() != null && input.valorCausa().compareTo(BigDecimal.ZERO) > 0) {
            out.add("Valor econômico definido permite calibrar âncora, piso de fechamento e concessões marginais admissíveis.");
        }
        return List.copyOf(out);
    }

    private List<String> deriveProtocolChecklist(ProcessMaterialDossierInput input, String normalizedText) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        out.add("Fechar objeto, pedido principal e versão consolidada dos pedidos em linguagem congruente.");
        out.add("Vincular cada documento a um fato, a uma tese e a um efeito processual concreto.");
        if (textSupport.blank(input.ritoName())) {
            out.add("Resolver o rito processual antes do protocolo para evitar desalinhamento de forma e workflow.");
        } else {
            out.add("Conferir aderência do pacote documental ao rito " + input.ritoName() + ".");
        }
        if (textSupport.blank(input.ramoDireito())) {
            out.add("Consolidar ramo de direito ou matéria principal para estabilizar competência e repertório jurisprudencial.");
        }
        if (input.valorCausa() == null || input.valorCausa().compareTo(BigDecimal.ZERO) <= 0) {
            out.add("Fixar memória de cálculo do valor da causa ou fundamento para ausência de expressão econômica imediata.");
        }
        if (!input.authorIdPresent() || !input.counterpartyIdPresent()) {
            out.add("Sanear identificação das partes antes do próximo marco relevante do fluxo.");
        }
        if (textSupport.containsAny(normalizedText, "TUTELA", "LIMINAR", "URG")) {
            out.add("Separar prova da probabilidade do direito, perigo de dano e reversibilidade em peça própria.");
        }
        return List.copyOf(out);
    }

    private String classifyEvidence(Integer score, int density, int proofGapCount) {
        int base = score == null ? density * 10 : score;
        if (base >= 75 && proofGapCount <= 2) {
            return "FORTE";
        }
        if (base >= 50) {
            return "MODERADA";
        }
        return "INICIAL";
    }

    private String classifyNegotiation(Integer score, String normalizedText) {
        int base = score == null ? 0 : score;
        if (base >= 75) {
            return "ALTA";
        }
        if (base >= 55) {
            return "MODERADA";
        }
        return "RESTRITA";
    }

    private String inferRelief(String normalizedText) {
        if (textSupport.containsAny(normalizedText, "INDENIZ")) {
            return "Condenação indenizatória";
        }
        if (textSupport.containsAny(normalizedText, "OBRIGACAO DE FAZER", "FORNECIMENTO", "AUTORIZACAO", "ENTREGA")) {
            return "Obrigação de fazer";
        }
        if (textSupport.containsAny(normalizedText, "ALIMENTO", "PENSAO")) {
            return "Fixação ou revisão de alimentos";
        }
        if (textSupport.containsAny(normalizedText, "COBRAN", "PAGAMENTO", "PARCEL")) {
            return "Cobrança de quantia";
        }
        if (textSupport.containsAny(normalizedText, "DECLARACAO", "ANULACAO", "RESCISAO", "REVISAO")) {
            return "Tutela declaratória ou desconstitutiva";
        }
        return "Tutela jurisdicional principal a ser consolidada";
    }

    private String summarizeObject(String normalizedText) {
        if (textSupport.blank(normalizedText)) {
            return null;
        }
        if (textSupport.containsAny(normalizedText, "CONTRATO")) {
            return "controvérsia contratual";
        }
        if (textSupport.containsAny(normalizedText, "CONSUMIDOR", "SERVICO", "FATURA", "COBRAN")) {
            return "falha de serviço ou relação de consumo";
        }
        if (textSupport.containsAny(normalizedText, "SAUDE", "MEDICAMENTO", "TRATAMENTO")) {
            return "fornecimento ou cobertura em saúde";
        }
        if (textSupport.containsAny(normalizedText, "ALIMENTO", "GUARDA", "VISITA")) {
            return "relação familiar controvertida";
        }
        if (textSupport.containsAny(normalizedText, "BENEFICIO", "INSS")) {
            return "pretensão previdenciária";
        }
        return "objeto litigioso principal";
    }

    private int density(List<String> evidenceItems, String normalizedEvidence, String normalizedText) {
        int density = Math.min(6, evidenceItems == null ? 0 : evidenceItems.size());
        density += Math.min(5, textSupport.countKeywords(normalizedEvidence, Set.of(
                "CONTRATO", "COMPROVANTE", "RECIBO", "BOLETO", "FATURA", "EXTRATO", "LAUDO", "ATESTADO", "WHATSAPP", "EMAIL", "TESTEMUNHA"
        )));
        if (textSupport.containsAny(normalizedText, "LAUDO", "ATESTADO", "COMPROVANTE", "CONTRATO")) {
            density += 1;
        }
        return Math.min(10, density);
    }

    private int dossierReadinessScore(ProcessMaterialDossierInput input,
                                      int evidenceDensity,
                                      int proofGapCount,
                                      int protocolChecklistCount) {
        int base = 40;
        base += evidenceDensity * 4;
        base += Math.min(15, input.claims().size() * 3);
        if (input.authorIdPresent()) {
            base += 6;
        }
        if (input.counterpartyIdPresent()) {
            base += 6;
        }
        if (!textSupport.blank(input.ramoDireito())) {
            base += 6;
        }
        if (!textSupport.blank(input.ritoName())) {
            base += 6;
        }
        base -= Math.min(28, proofGapCount * 7);
        base -= Math.max(0, protocolChecklistCount - 6) * 2;
        return textSupport.clamp(base, 0, 100);
    }

    private String attentionBand(int readinessScore,
                                 int proofGapCount,
                                 ProcessMaterialDossierInput input) {
        if (proofGapCount >= 4 || readinessScore < 45) {
            return "CRITICA";
        }
        if (!input.authorIdPresent() || !input.counterpartyIdPresent() || readinessScore < 70 || !input.riskSignals().isEmpty()) {
            return "ATIVA";
        }
        return "ESTAVEL";
    }

    private String executiveSummary(ProcessMaterialDossierInput input,
                                    String evidentiaryBracket,
                                    String negotiationBracket,
                                    LinkedHashSet<String> proofGaps,
                                    String objectLabel,
                                    String primaryRelief) {
        StringBuilder summary = new StringBuilder();
        summary.append("Caso em ").append(ProcessMaterialDossierTextSupport.firstNonBlank(input.phase(), "PROCESSO"));
        summary.append(" com objeto ").append(ProcessMaterialDossierTextSupport.firstNonBlank(objectLabel, "não consolidado"));
        summary.append(" e tutela principal ").append(ProcessMaterialDossierTextSupport.firstNonBlank(primaryRelief, "pendente de consolidação"));
        summary.append(". Prova ").append(evidentiaryBracket.toLowerCase());
        summary.append(", margem negocial ").append(negotiationBracket.toLowerCase());
        summary.append(" e ").append(proofGaps.isEmpty() ? "sem lacuna crítica aparente" : "lacunas prioritárias já mapeadas").append('.');
        return textSupport.truncate(summary.toString(), 320);
    }

    private String strategicFocus(LinkedHashSet<String> proofGaps,
                                  LinkedHashSet<String> settlementLevers,
                                  LinkedHashSet<String> protocolChecklist) {
        if (!proofGaps.isEmpty()) {
            return textSupport.truncate(proofGaps.iterator().next(), 220);
        }
        if (!protocolChecklist.isEmpty()) {
            return textSupport.truncate(protocolChecklist.iterator().next(), 220);
        }
        if (!settlementLevers.isEmpty()) {
            return textSupport.truncate(settlementLevers.iterator().next(), 220);
        }
        return "Manter coerência entre objeto, prova e ato seguinte do fluxo processual.";
    }
}
