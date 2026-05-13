package com.tcc.pjb.backend.core.distribuicao;

import com.tcc.pjb.backend.core.processual.routing.NationalProcessRoutingService.RoutingDecision;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
class DistribuicaoProcessualTrackSupport {

    String resolveSpecializedTrack(DistribuicaoProcessualNacionalEngine.DistribuicaoRequest request, RoutingDecision routing) {
        if (request == null) {
            return "CONHECIMENTO_GERAL";
        }
        RitoProcessual rito = request.rito();
        String corpus = normalizeToken(joinCorpus(
                request.areaEspecializada(),
                request.assunto(),
                request.assuntoLocal(),
                request.assuntoMacro(),
                request.classeProcessual(),
                request.classeProcessualLocal(),
                request.classeMacro(),
                routing.specializationAxis(),
                routing.orgaoJulgadorSugerido(),
                routing.unidadeJudiciariaCodigo(),
                routing.competenceEnvelope(),
                routing.foroSugerido(),
                routing.comarcaSugerida(),
                "CONHECIMENTO_GERAL"));
        if (isConstitutionalTrack(rito, request.grauJurisdicao(), corpus)) {
            return "CONSTITUCIONAL";
        }
        if (request.grauJurisdicao() != null && request.grauJurisdicao() != GrauJurisdicao.PRIMEIRO_GRAU) {
            return "COLEGIADO_RECURSAL";
        }
        if (containsAny(corpus, "CUSTODIA", "AUTO_DE_PRISAO", "FLAGRANTE")
                || rito == RitoProcessual.ESPECIAL_HABEAS_CORPUS
                || rito == RitoProcessual.PENAL_HABEAS_CORPUS_PREVENTIVO) {
            return "CUSTODIA";
        }
        if (rito == RitoProcessual.TRIBUNAL_JURI || containsAny(corpus, "JURI", "PRONUNCIA", "PLENARIO_DO_JURI")) {
            return "TRIBUNAL_JURI";
        }
        if (rito == RitoProcessual.EXECUCAO_PENAL || containsAny(corpus, "EXECUCAO_PENAL", "GUIA_DE_EXECUCAO")) {
            return "EXECUCAO_PENAL";
        }
        if (rito.isJuizado() || containsAny(corpus, "JUIZADO", "JEC", "JECRIM", "JEF")) {
            return "JUIZADO";
        }
        if (rito.isFamiliaSucessoes() || containsAny(corpus, "FAMILIA", "SUCESSOES", "ALIMENTOS", "GUARDA", "DIVORCIO", "INVENTARIO")) {
            return "FAMILIA_SUCESSOES";
        }
        if (rito.isExecucaoFiscalEstrita() || containsAny(corpus, "EXECUCAO_FISCAL", "DIVIDA_ATIVA", "CERTIDAO_DE_DIVIDA_ATIVA", "CDA", "LEF")) {
            return "EXECUCAO_FISCAL";
        }
        if (rito.isTribFazenda() || containsAny(corpus, "FAZENDA", "SERVIDOR", "TRIBUTARIO", "IMPROBIDADE", "ADMINISTRATIVO")) {
            return containsAny(corpus, "IMPROBIDADE", "ADMINISTRATIVO") ? "ADMINISTRATIVO_IMPROBIDADE" : "FAZENDA_PUBLICA";
        }
        if (rito.isEmpresarial() || containsAny(corpus, "EMPRESARIAL", "FALENCIA", "RECUPERACAO", "SOCIEDADE")) {
            return "EMPRESARIAL";
        }
        if (rito.isAmbiental() || containsAny(corpus, "AMBIENTAL", "LICENCIAMENTO", "POLUICAO")) {
            return "AMBIENTAL";
        }
        if (rito.isAgrario() || containsAny(corpus, "AGRARIO", "IMOVEL_RURAL", "POSSE_RURAL")) {
            return "AGRARIO";
        }
        if (rito.isInfancia() || containsAny(corpus, "INFANCIA", "JUVENTUDE", "MEDIDA_SOCIOEDUCATIVA", "ACOLHIMENTO")) {
            return "INFANCIA_JUVENTUDE";
        }
        if (rito.isPrevidenciario() || containsAny(corpus, "PREVIDENCIARIO", "BENEFICIO", "INSS", "BPC")) {
            return "PREVIDENCIARIO";
        }
        if (rito.isTrabalhista() || containsAny(corpus, "TRABALHO", "TRABALHISTA", "VINCULO", "RESCISAO", "VERBAS")) {
            return "TRABALHISTA";
        }
        if (rito.isEleitoral() || containsAny(corpus, "ELEITORAL", "REGISTRO_CANDIDATURA", "PRESTACAO_DE_CONTAS")) {
            return "ELEITORAL";
        }
        if (rito.isMilitar() || containsAny(corpus, "MILITAR", "DISCIPLINAR_MILITAR", "AUDITORIA_MILITAR")) {
            return "MILITAR";
        }
        if (rito.isInternacional() || containsAny(corpus, "INTERNACIONAL", "CARTA_ROGATORIA", "HOMOLOGACAO", "AUTORIDADE_CENTRAL")) {
            return "INTERNACIONAL";
        }
        if (rito.isAutocompositivo() || containsAny(corpus, "CEJUSC", "MEDIACAO", "CONCILIACAO", "ARBITRAGEM")) {
            return "AUTOCOMPOSICAO";
        }
        if (rito.isPenal() || containsAny(corpus, "PENAL", "CRIMINAL", "INQUERITO", "DENUNCIA")) {
            return "CRIMINAL";
        }
        return "CONHECIMENTO_GERAL";
    }

    static boolean isConstitutionalTrack(RitoProcessual rito, GrauJurisdicao grauJurisdicao, String corpus) {
        if (grauJurisdicao != null && grauJurisdicao != GrauJurisdicao.PRIMEIRO_GRAU) {
            return true;
        }
        return rito == RitoProcessual.ESPECIAL_ACAO_DIRETA_INCONSTITUCIONALIDADE
                || rito == RitoProcessual.ESPECIAL_ACAO_DECLARATORIA_CONSTITUCIONALIDADE
                || rito == RitoProcessual.ESPECIAL_ARGUICAO_DESCUMPRIMENTO_PRECEITO_FUNDAMENTAL
                || rito == RitoProcessual.ESPECIAL_ACAO_DESCUMPRIMENTO_OBRIGACAO
                || containsAny(corpus, "ADI", "ADC", "ADPF", "ADO", "CONTROLE_CONCENTRADO", "CONSTITUCIONAL");
    }


    String resolveQueueSegment(String specializedTrack, String anchor) {
        String track = firstNonBlank(specializedTrack, "CONHECIMENTO_GERAL");
        if (("CONHECIMENTO_GERAL".equals(track) || "CONHECIMENTO".equals(track)) && containsAny(anchor, "PLANTAO", "URGENTE", "LIMINAR")) {
            return "URGENTE";
        }
        return switch (track) {
            case "CONSTITUCIONAL" -> "CONSTITUCIONAL";
            case "COLEGIADO_RECURSAL" -> "COLEGIADO_RECURSAL";
            case "CUSTODIA" -> "CUSTODIA";
            case "TRIBUNAL_JURI" -> "TRIBUNAL_JURI";
            case "EXECUCAO_PENAL" -> "EXECUCAO_PENAL";
            case "JUIZADO" -> "JUIZADO";
            case "FAMILIA_SUCESSOES" -> "FAMILIA_SUCESSOES";
            case "EXECUCAO_FISCAL" -> "EXECUCAO_FISCAL";
            case "FAZENDA_PUBLICA" -> "FAZENDA_PUBLICA";
            case "ADMINISTRATIVO_IMPROBIDADE" -> "ADMINISTRATIVO_IMPROBIDADE";
            case "EMPRESARIAL" -> "EMPRESARIAL";
            case "AMBIENTAL" -> "AMBIENTAL";
            case "AGRARIO" -> "AGRARIO";
            case "INFANCIA_JUVENTUDE" -> "INFANCIA_JUVENTUDE";
            case "PREVIDENCIARIO" -> "PREVIDENCIARIO";
            case "TRABALHISTA" -> "TRABALHISTA";
            case "ELEITORAL" -> "ELEITORAL";
            case "MILITAR" -> "MILITAR";
            case "INTERNACIONAL" -> "INTERNACIONAL";
            case "AUTOCOMPOSICAO" -> "AUTOCOMPOSICAO";
            case "CRIMINAL" -> "CRIMINAL";
            default -> "CONHECIMENTO";
        };
    }

    String resolveInboxSegment(String specializedTrack, String anchor) {
        return containsAny(anchor, "SIGILO", "RESERVADO") ? "SIGILO" : resolveQueueSegment(specializedTrack, anchor);
    }

        List<String> buildSpecializedAlertas(DistribuicaoProcessualNacionalEngine.DistribuicaoRequest request, RoutingDecision routing, String specializedTrack) {
        Set<String> itens = new LinkedHashSet<>();
        String anchor = normalizeToken(firstNonBlank(request.areaEspecializada(), request.assunto(), routing.orgaoJulgadorSugerido(), specializedTrack));
        itens.add("Trilho especializado detectado: " + specializedTrack + '.');
        if (containsAny(anchor, "PLANTAO", "URGENTE", "LIMINAR") || request.urgente()) {
            itens.add("Fluxo urgente exige triagem reforçada antes da distribuição final.");
        }
        if (request.sigiloReforcado()) {
            itens.add("Sigilo reforçado exige conferência de competência e visibilidade antes do sorteio.");
        }
        switch (specializedTrack) {
            case "CUSTODIA" -> itens.add("Custódia exige conferência imediata de audiência, preso, autoridade coatora e unidade de plantão.");
            case "TRIBUNAL_JURI" -> itens.add("Júri demanda validação de competência constitucional e eventual prevenção por pronúncia anterior.");
            case "EXECUCAO_PENAL" -> itens.add("Execução penal requer checagem de guia, estabelecimento e vinculação por apenado.");
            case "JUIZADO" -> itens.add("Juizado exige verificação de teto, rito e vedação de complexidade excessiva.");
            case "EXECUCAO_FISCAL" -> itens.add("Execução fiscal exige conferência de CDA, dívida ativa, ente exequente e competência fazendária especializada.");
            case "FAZENDA_PUBLICA", "ADMINISTRATIVO_IMPROBIDADE" -> itens.add("Ações contra ente público exigem conferência do polo passivo e competência fazendária adequada.");
            case "FAMILIA_SUCESSOES" -> itens.add("Família e sucessões exigem validação de segredo, dependência e prevenção por núcleo familiar.");
            case "EMPRESARIAL" -> itens.add("Empresarial exige validação de juízo universal, grupo econômico e prevenção concursal.");
            case "INTERNACIONAL" -> itens.add("Internacional exige checagem de autoridade central, carta rogatória e tradução necessária.");
            default -> {
            }
        }
        if (routing.tipoJustica() == TipoJustica.FEDERAL && containsAny(anchor, "PREVIDENCIARIO")) {
            itens.add("Fluxo previdenciário federal requer conferência de subseção, benefício e ente pagador.");
        }
        return List.copyOf(itens);
    }

    List<String> buildSpecializedFundamentos(DistribuicaoProcessualNacionalEngine.DistribuicaoRequest request, RoutingDecision routing, String specializedTrack) {
        Set<String> itens = new LinkedHashSet<>();
        itens.add("Competência material orientada pelo trilho especializado " + specializedTrack + '.');
        itens.add("Envelope decisório do roteamento nacional: " + firstNonBlank(routing.competenceEnvelope(), "ENVELOPE_NAO_INFORMADO") + '.');
        if (request.temDependencia() || request.temConexao() || request.temContinencia()) {
            itens.add("Relação processual declarada exige observância de prevenção, dependência, conexão ou continência.");
        }
        if (request.sigiloReforcado()) {
            itens.add("Regime de sigilo impacta a malha de distribuição e a governança de mesa competente.");
        }
        switch (specializedTrack) {
            case "CONSTITUCIONAL" -> itens.add("Controle concentrado e competência originária exigem trilho colegiado e mesa constitucional especializada.");
            case "COLEGIADO_RECURSAL" -> itens.add("Processo em grau recursal demanda órgão colegiado e prevenção por relatoria ou órgão prolator.");
            case "CUSTODIA" -> itens.add("Custódia e habeas corpus exigem tutela imediata da liberdade e distribuição sob urgência reforçada.");
            case "TRIBUNAL_JURI" -> itens.add("Competência do júri é definida pela natureza constitucional dos crimes dolosos contra a vida.");
            case "EXECUCAO_PENAL" -> itens.add("Execução penal depende da guia e do vínculo do apenado à unidade de execução competente.");
            case "JUIZADO" -> itens.add("Microssistema dos juizados condiciona teto econômico, simplicidade e especialização do rito.");
            case "FAMILIA_SUCESSOES" -> itens.add("Família e sucessões exigem prevenção por núcleo familiar e eventual segredo processual reforçado.");
            case "EXECUCAO_FISCAL" -> itens.add("Execução fiscal possui trilho próprio de cobrança de dívida ativa, com conferência de CDA, LEF e ente exequente.");
            case "FAZENDA_PUBLICA" -> itens.add("Fazenda pública exige competência especializada diante de ente público, tributo ou servidor.");
            case "ADMINISTRATIVO_IMPROBIDADE" -> itens.add("Improbidade e administrativo exigem mesa especializada e governança reforçada sobre ente público e regime sancionador.");
            case "EMPRESARIAL" -> itens.add("Juízo empresarial depende de prevenção por falência, recuperação ou grupo econômico correlato.");
            case "INTERNACIONAL" -> itens.add("Cooperação jurídica internacional condiciona autoridade central, carta rogatória ou homologação.");
            default -> {
            }
        }
        return List.copyOf(itens);
    }

    List<String> buildSpecializedReviewChecklist(DistribuicaoProcessualNacionalEngine.DistribuicaoRequest request, RoutingDecision routing, String specializedTrack) {
        Set<String> itens = new LinkedHashSet<>();
        itens.add("Validar tribunal, unidade e desk profile antes da distribuição final.");
        itens.add("Conferir prevenção, conexão, continência ou dependência antes do sorteio útil.");
        if (request.sigiloReforcado()) {
            itens.add("Revalidar acessos e mesa competente compatíveis com sigilo reforçado.");
        }
        switch (specializedTrack) {
            case "CONSTITUCIONAL" -> itens.add("Conferir legitimidade ativa, classe constitucional e órgão originário competente antes da autuação final.");
            case "COLEGIADO_RECURSAL" -> itens.add("Confirmar órgão prolator, relatoria preventiva e classe recursal adequada antes da remessa colegiada.");
            case "CUSTODIA" -> itens.add("Confirmar autoridade coatora, unidade de custódia, preso e janela temporal da audiência imediatamente.");
            case "TRIBUNAL_JURI" -> itens.add("Conferir pronúncia, comarca do fato e eventual prevenção por plenário do júri antes da distribuição.");
            case "EXECUCAO_PENAL" -> itens.add("Validar guia, estabelecimento, apenado e unidade de execução penal antes da vinculação final.");
            case "JUIZADO" -> itens.add("Confirmar teto legal, simplicidade e vedação de complexidade incompatível com o microssistema do juizado.");
            case "FAMILIA_SUCESSOES" -> itens.add("Conferir vínculo familiar, prevenção por dependência e segredo adequado antes da autuação definitiva.");
            case "EXECUCAO_FISCAL" -> itens.add("Confirmar CDA, dívida ativa, ente exequente, valor atualizado e competência fiscal antes do sorteio definitivo.");
            case "FAZENDA_PUBLICA" -> itens.add("Confirmar ente público, matéria fazendária e competência territorial antes do sorteio definitivo.");
            case "AMBIENTAL" -> itens.add("Confirmar dano, licenciamento, ente fiscalizador e base territorial do conflito ambiental antes da distribuição definitiva.");
            case "AGRARIO" -> itens.add("Conferir imóvel rural, conflito possessório, contexto coletivo e âncora territorial agrária antes da distribuição útil.");
            case "EMPRESARIAL" -> itens.add("Confirmar juízo universal, grupo econômico e prevenção de recuperação ou falência antes da remessa final.");
            case "ADMINISTRATIVO_IMPROBIDADE" -> itens.add("Conferir ente público, regime jurídico e especialização administrativa antes do sorteio ou remessa útil.");
            case "INTERNACIONAL" -> itens.add("Conferir autoridade central, tradução, carta rogatória ou homologação antes da distribuição definitiva.");
            case "AUTOCOMPOSICAO" -> itens.add("Validar cláusula compromissória, CEJUSC ou câmara consensual antes do direcionamento definitivo.");
            default -> {
            }
        }
        if (routing.grau() == GrauJurisdicao.PRIMEIRO_GRAU && routing.tipoJustica() == TipoJustica.ESTADUAL && containsAny(firstNonBlank(request.comarca(), routing.comarcaSugerida()), "CAPITAL")) {
            itens.add("Em comarca de capital, revisar vara e especialização concreta para evitar mistura entre múltiplas unidades homogêneas.");
        }
        return List.copyOf(itens);
    }

    private static String joinCorpus(String... values) {
        if (values == null) {
            return "CONHECIMENTO_GERAL";
        }
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                if (!builder.isEmpty()) {
                    builder.append(' ');
                }
                builder.append(value.trim());
            }
        }
        return builder.isEmpty() ? "CONHECIMENTO_GERAL" : builder.toString();
    }

    private static boolean containsAny(String value, String... needles) {
        String normalized = normalizeToken(value);
        if (normalized == null || needles == null) {
            return false;
        }
        for (String needle : needles) {
            String token = normalizeToken(needle);
            if (token != null && matchesToken(normalized, token)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesToken(String normalized, String token) {
        return normalized.equals(token)
                || normalized.startsWith(token + "_")
                || normalized.endsWith("_" + token)
                || normalized.contains("_" + token + "_");
    }

    private static String normalizeToken(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("(^_|_$)", "");
        return normalized.isBlank() ? null : normalized;
    }

    private static String firstNonBlank(String... values) {
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
