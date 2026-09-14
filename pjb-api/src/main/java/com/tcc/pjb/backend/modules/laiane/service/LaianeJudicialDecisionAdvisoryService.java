package com.tcc.pjb.backend.modules.laiane.service;

import com.tcc.pjb.backend.modules.laiane.dto.roles.judge.JudicialDecisionTemplateCode;
import com.tcc.pjb.backend.modules.laiane.dto.roles.judge.LaianeAdvisoryMode;
import com.tcc.pjb.backend.modules.laiane.dto.roles.judge.LaianeJudicialDecisionAdvisoryResponse;
import com.tcc.pjb.backend.modules.laiane.dto.roles.judge.LaianeSentencaDraftRequest;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class LaianeJudicialDecisionAdvisoryService {

    public LaianeJudicialDecisionAdvisoryResponse analyze(Processo processo, LaianeSentencaDraftRequest req) {
        String corpus = buildCorpus(processo, req);
        if (containsAny(corpus, "ACORDO", "TRANSACAO", "TERMO DE ACORDO", "CONCILIACAO EXITOSA", "HOMOLOGACAO DE ACORDO")) {
            return acordo(processo, req, corpus);
        }
        if (containsAny(corpus, "DESISTENCIA", "DESISTE DA ACAO", "DESISTIU DA ACAO", "DESISTENCIA DA ACAO", "EXTINCAO SEM RESOLUCAO DO MERITO")) {
            return desistencia(processo, req, corpus);
        }
        if (containsAny(corpus, "RECONHECIMENTO DA PROCEDENCIA", "RECONHECE A PROCEDENCIA", "RECONHECIMENTO DO PEDIDO")) {
            return reconhecimento(processo, req, corpus);
        }
        if (containsAny(corpus, "MARIA DA PENHA", "VIOLENCIA DOMESTICA", "VIOLENCIA FAMILIAR CONTRA A MULHER", "MEDIDA PROTETIVA", "OFENDIDA", "AGRESSOR")) {
            return mariaDaPenhaUrgente(processo, req, corpus);
        }
        if (containsAny(corpus, "LEITO DE UTI", "UTI", "TERAPIA INTENSIVA", "SUPORTE VITAL", "VENTILACAO MECANICA", "RISCO DE MORTE")) {
            return saudeUtiUrgente(processo, req, corpus);
        }
        if (containsAny(corpus, "MEDICAMENTO", "TRATAMENTO", "CIRURGIA", "TRANSFERENCIA HOSPITALAR", "ONCOLOG", "SUS", "SAUDE")) {
            return saudeUrgente(processo, req, corpus);
        }
        return generica(processo, req, corpus);
    }

    private LaianeJudicialDecisionAdvisoryResponse acordo(Processo processo,
                                                          LaianeSentencaDraftRequest req,
                                                          String corpus) {
        LinkedHashSet<String> checklist = new LinkedHashSet<>();
        checklist.add("Confirmar capacidade das partes, representação válida e higidez do termo de acordo.");
        checklist.add("Verificar disponibilidade do direito material e eventual necessidade de intervenção do Ministério Público.");
        checklist.add("Conferir cláusulas essenciais, forma de cumprimento, vencimento, mora e forma de extinção das obrigações.");
        checklist.add("Explicar a aderência entre o acordo firmado e o pedido efetivamente deduzido no processo.");
        LinkedHashSet<String> pendencias = new LinkedHashSet<>();
        if (!containsAny(corpus, "VALOR", "PARCELA", "PAGAMENTO", "OBRIGACAO DE FAZER", "PRAZO")) {
            pendencias.add("Detalhar obrigações, prazo de cumprimento, valor ou regime prestacional do acordo.");
        }
        if (ramoSensivel(processo)) {
            pendencias.add("Validar disponibilidade material do objeto e eventual tutela de interesse indisponível ou sensível.");
        }
        LinkedHashMap<String, String> fillables = new LinkedHashMap<>();
        fillables.put("partes_qualificadas", "[QUALIFICACAO_COMPLETA_DAS_PARTES]");
        fillables.put("clausulas_acordo", "[CLAUSULAS_ESSENCIAIS_COM_PRAZO_E_VALORES]");
        fillables.put("inadimplemento", "[MULTA_MORA_VENCIMENTO_ANTECIPADO_SE_CABIVEL]");
        fillables.put("custas_honorarios", "[REGIME_DE_CUSTAS_E_HONORARIOS]");
        fillables.put("disposicoes_finais", "[EXTINCAO_E_BAIXA_SE_CABIVEL]");
        LinkedHashMap<String, Object> metadata = baseMetadata(processo, req, corpus);
        metadata.put("templateFamily", "NEGOCIAL_HOMOLOGATORIA");
        metadata.put("cpcAnchorPrimary", "ART_487_III_B");
        metadata.put("publicationLocked", true);
        List<String> pendenciasFinal = List.copyOf(pendencias);
        LaianeAdvisoryMode mode = resolveMode(pendenciasFinal);
        return new LaianeJudicialDecisionAdvisoryResponse(
                JudicialDecisionTemplateCode.HOMOLOGACAO_ACORDO,
                mode,
                true,
                true,
                "Caso com sinal forte de transação judicial, apto a minuta assistida de homologação com resolução de mérito, preservada a revisão integral do magistrado.",
                List.of("CPC art. 487, III, b", "CPC art. 489"),
                List.copyOf(checklist),
                pendenciasFinal,
                mode == LaianeAdvisoryMode.RESTRITIVO ? null : "Homologo, por sentença, a transação celebrada pelas partes, com resolução de mérito, nos termos do art. 487, III, b, do CPC, observadas as cláusulas e consequências jurídicas expressamente registradas neste feito.",
                Map.copyOf(fillables),
                Map.copyOf(metadata)
        );
    }

    private LaianeJudicialDecisionAdvisoryResponse desistencia(Processo processo,
                                                               LaianeSentencaDraftRequest req,
                                                               String corpus) {
        LinkedHashSet<String> checklist = new LinkedHashSet<>();
        checklist.add("Confirmar manifestação inequívoca de desistência e o momento processual em que foi apresentada.");
        checklist.add("Verificar se houve contestação e, em caso afirmativo, a necessidade de consentimento da parte contrária.");
        checklist.add("Explicitar o enquadramento extintivo sem resolução de mérito e seus efeitos processuais.");
        LinkedHashSet<String> pendencias = new LinkedHashSet<>();
        if (!containsAny(corpus, "CONTESTACAO", "CONSENTIMENTO DO REU", "ANUENCIA")) {
            pendencias.add("Checar se houve contestação e se há necessidade de anuência da parte contrária para homologação da desistência.");
        }
        LinkedHashMap<String, String> fillables = new LinkedHashMap<>();
        fillables.put("manifestacao_desistencia", "[TRECHO_DA_MANIFESTACAO_DA_PARTE]");
        fillables.put("anuencia_reu", "[INDICAR_SE_HOUVE_OU_NAO_CONSENTIMENTO_DA_PARTE_CONTRARIA]");
        fillables.put("custas_honorarios", "[REGIME_DE_CUSTAS_E_HONORARIOS]");
        fillables.put("baixa_arquivamento", "[DETERMINACAO_DE_BAIXA_E_ARQUIVAMENTO_SE_CABIVEL]");
        LinkedHashMap<String, Object> metadata = baseMetadata(processo, req, corpus);
        metadata.put("templateFamily", "EXTINTIVA_SEM_MERITO");
        metadata.put("cpcAnchorPrimary", "ART_485_VIII");
        metadata.put("publicationLocked", true);
        List<String> pendenciasFinal = List.copyOf(pendencias);
        LaianeAdvisoryMode mode = resolveMode(pendenciasFinal);
        return new LaianeJudicialDecisionAdvisoryResponse(
                JudicialDecisionTemplateCode.HOMOLOGACAO_DESISTENCIA_EXTINCAO_SEM_MERITO,
                mode,
                true,
                true,
                "Caso com sinal forte de desistência da ação, apto a minuta assistida de homologação e extinção sem resolução de mérito, preservada a validação judicial integral.",
                List.of("CPC art. 485, VIII", "CPC art. 485, §§ 4º e 5º", "CPC art. 489"),
                List.copyOf(checklist),
                pendenciasFinal,
                mode == LaianeAdvisoryMode.RESTRITIVO ? null : "Homologo a desistência da ação e extingo o processo sem resolução do mérito, nos termos do art. 485, VIII, do CPC, ressalvadas as condições processuais incidentes ao estágio em que o feito se encontra.",
                Map.copyOf(fillables),
                Map.copyOf(metadata)
        );
    }

    private LaianeJudicialDecisionAdvisoryResponse mariaDaPenhaUrgente(Processo processo,
                                                                   LaianeSentencaDraftRequest req,
                                                                   String corpus) {
        LinkedHashSet<String> checklist = new LinkedHashSet<>();
        checklist.add("Identificar risco atual ou iminente à integridade física, psicológica, sexual, patrimonial ou moral da ofendida e de dependentes.");
        checklist.add("Definir, com aderência ao caso concreto, o feixe de medidas protetivas adequadas, o regime de intimação e o modo de cumprimento.");
        checklist.add("Preservar sigilo operacional da identidade da ofendida quando cabível e evitar exposição indevida em peças e comandos executivos.");
        checklist.add("Registrar eventual necessidade de apoio policial, comunicação institucional e restrição de porte ou contato do agressor.");
        LinkedHashSet<String> pendencias = new LinkedHashSet<>();
        if (!containsAny(corpus, "RISCO", "AMEACA", "DESCUMPRIMENTO", "ARMA", "PERSEGUICAO", "AGRESSAO")) {
            pendencias.add("Detalhar o vetor concreto de risco que justifica a urgência protetiva e a extensão das medidas postuladas.");
        }
        LinkedHashMap<String, String> fillables = new LinkedHashMap<>();
        fillables.put("quadro_fatico_risco", "[DESCREVER_RISCO_ATUAL_OU_IMINENTE_E_DADOS_RELEVANTES_DA_OFENDIDA]");
        fillables.put("medidas_protetivas", "[INDICAR_MEDIDAS_PROTETIVAS_CONCRETAS_E_SUAS_CONDICOES_DE_CUMPRIMENTO]");
        fillables.put("rede_protecao_e_comunicacoes", "[DEFINIR_ACIONAMENTO_DE_REDE_DE_PROTECAO_E_COMUNICACOES_NECESSARIAS]");
        fillables.put("cumprimento_e_fiscalizacao", "[DISPOR_SOBRE_INTIMACAO_CUMPRIMENTO_E_EVENTUAL_APOIO_POLICIAL]");
        LinkedHashMap<String, Object> metadata = baseMetadata(processo, req, corpus);
        metadata.put("templateFamily", "URGENTE_PROTETIVA_MARIA_DA_PENHA");
        metadata.put("cpcAnchorPrimary", "PROTECAO_URGENTE");
        metadata.put("publicationLocked", true);
        List<String> pendenciasFinal = List.copyOf(pendencias);
        LaianeAdvisoryMode mode = resolveMode(pendenciasFinal);
        return new LaianeJudicialDecisionAdvisoryResponse(
                JudicialDecisionTemplateCode.MEDIDA_PROTETIVA_URGENTE_MARIA_DA_PENHA,
                mode,
                true,
                true,
                "Caso com forte sinal de violência doméstica e necessidade de medida protetiva urgente, apto a minuta assistida estritamente sob revisão integral da autoridade judicial competente.",
                List.of("Lei 11.340/2006, arts. 18, 19, 22 e 23", "Lei 14.550/2023", "Lei 14.857/2024"),
                List.copyOf(checklist),
                pendenciasFinal,
                mode == LaianeAdvisoryMode.RESTRITIVO ? null : "Defiro, em cognição sumária e sem prejuízo de ulterior reavaliação, as medidas protetivas estritamente adequadas ao quadro de risco delineado nos autos, resguardada a identidade da ofendida e fixadas as providências executivas de imediato cumprimento.",
                Map.copyOf(fillables),
                Map.copyOf(metadata)
        );
    }

    private LaianeJudicialDecisionAdvisoryResponse saudeUtiUrgente(Processo processo,
                                                                   LaianeSentencaDraftRequest req,
                                                                   String corpus) {
        LinkedHashSet<String> checklist = new LinkedHashSet<>();
        checklist.add("Verificar relatório médico atual, gravidade clínica, urgência do suporte intensivo e nexo entre o estado do paciente e a providência jurisdicional requerida.");
        checklist.add("Explicitar a probabilidade do direito, o perigo de dano e a adequação da tutela específica ou do resultado prático equivalente.");
        checklist.add("Precisar o comando executivo com aderência clínica, canal de cumprimento, regulação do leito e temporalidade da ordem.");
        checklist.add("Indicar, quando pertinente, a necessidade de apoio técnico-científico complementar sem inviabilizar a tutela urgente.");
        LinkedHashSet<String> pendencias = new LinkedHashSet<>();
        if (!containsAny(corpus, "LAUDO", "RELATORIO MEDICO", "RISCO DE MORTE", "INTERNACAO", "LEITO")) {
            pendencias.add("Conferir documentação clínica mínima, atualidade do quadro e narrativa objetiva da necessidade de leito intensivo.");
        }
        LinkedHashMap<String, String> fillables = new LinkedHashMap<>();
        fillables.put("quadro_clinico", "[DESCREVER_DIAGNOSTICO_GRAVIDADE_E_RISCO_CLINICO_ATUAL]");
        fillables.put("comando_urgente", "[DEFINIR_FORNECIMENTO_DE_LEITO_UTI_OU_SUPORTE_INTENSIVO_COM_PRAZO_E_RESPONSAVEIS]");
        fillables.put("regulacao_e_cumprimento", "[DISPOR_SOBRE_REGULACAO_COMUNICACAO_E_CUMPRIMENTO_IMEDIATO]");
        fillables.put("multa_e_medidas_executivas", "[FIXAR_ASTREINTES_E_MEDIDAS_EXECUTIVAS_SE_CABIVEIS]");
        LinkedHashMap<String, Object> metadata = baseMetadata(processo, req, corpus);
        metadata.put("templateFamily", "URGENTE_SAUDE_UTI");
        metadata.put("cpcAnchorPrimary", "ART_300");
        metadata.put("publicationLocked", true);
        List<String> pendenciasFinal = List.copyOf(pendencias);
        LaianeAdvisoryMode mode = resolveMode(pendenciasFinal);
        return new LaianeJudicialDecisionAdvisoryResponse(
                JudicialDecisionTemplateCode.TUTELA_URGENTE_LEITO_UTI,
                mode,
                true,
                true,
                "Caso com sinal de necessidade urgente de leito intensivo, apto a minuta assistida de tutela de urgência, preservada a cognição judicial plena e a validação humana integral.",
                List.of("CF art. 196", "Lei 8.080/1990, arts. 2º e 7º", "CPC arts. 300, 497 e 536", "CNJ Resolução 238/2016", "CNJ Provimento 84/2019"),
                List.copyOf(checklist),
                pendenciasFinal,
                mode == LaianeAdvisoryMode.RESTRITIVO ? null : "Defiro, em tutela de urgência e sem prejuízo de ulterior reavaliação, as providências necessárias à viabilização de leito intensivo ou suporte clínico equivalente, em prazo compatível com o risco concreto delineado nos autos.",
                Map.copyOf(fillables),
                Map.copyOf(metadata)
        );
    }

    private LaianeJudicialDecisionAdvisoryResponse saudeUrgente(Processo processo,
                                                                LaianeSentencaDraftRequest req,
                                                                String corpus) {
        LinkedHashSet<String> checklist = new LinkedHashSet<>();
        checklist.add("Verificar documentação clínica mínima, urgência contemporânea e aderência do pedido ao quadro concreto do paciente.");
        checklist.add("Ajustar o comando judicial ao tratamento, medicamento, transferência ou providência específica efetivamente requerida.");
        checklist.add("Definir o modo de cumprimento, o prazo operacional e as consequências do inadimplemento com proporcionalidade.");
        LinkedHashMap<String, String> fillables = new LinkedHashMap<>();
        fillables.put("diagnostico_e_urgencia", "[DESCREVER_QUADRO_CLINICO_E_URGENCIA_CONCRETA]");
        fillables.put("providencia_requerida", "[DEFINIR_PROVIDENCIA_DE_SAUDE_COM_PRAZO_E_RESPONSAVEIS]");
        fillables.put("cumprimento_e_astreintes", "[DISPOR_SOBRE_CUMPRIMENTO_FISCALIZACAO_E_ASTREINTES]");
        LinkedHashMap<String, Object> metadata = baseMetadata(processo, req, corpus);
        metadata.put("templateFamily", "URGENTE_SAUDE");
        metadata.put("cpcAnchorPrimary", "ART_300");
        metadata.put("publicationLocked", true);
        LaianeAdvisoryMode mode = resolveMode(List.of());
        return new LaianeJudicialDecisionAdvisoryResponse(
                JudicialDecisionTemplateCode.TUTELA_URGENTE_SAUDE,
                mode,
                true,
                true,
                "Caso com sinal de urgência em saúde, apto a minuta assistida de tutela provisória estritamente sujeita à revisão judicial integral.",
                List.of("CF art. 196", "Lei 8.080/1990, arts. 2º e 7º", "CPC arts. 300, 497 e 536"),
                List.copyOf(checklist),
                List.of(),
                mode == LaianeAdvisoryMode.RESTRITIVO ? null : "Defiro, em tutela de urgência e na estrita extensão do quadro clínico demonstrado, as providências necessárias para assegurar o acesso tempestivo ao tratamento ou procedimento indicado nos autos.",
                Map.copyOf(fillables),
                Map.copyOf(metadata)
        );
    }

    private LaianeJudicialDecisionAdvisoryResponse reconhecimento(Processo processo,
                                                                  LaianeSentencaDraftRequest req,
                                                                  String corpus) {
        LinkedHashSet<String> checklist = new LinkedHashSet<>();
        checklist.add("Confirmar a higidez da manifestação de reconhecimento do pedido e a extensão objetiva da procedência reconhecida.");
        checklist.add("Fixar os efeitos patrimoniais, consectários legais e sucumbência com aderência ao caso concreto.");
        LinkedHashMap<String, String> fillables = new LinkedHashMap<>();
        fillables.put("reconhecimento", "[TRECHO_DA_MANIFESTACAO_DE_RECONHECIMENTO]");
        fillables.put("efeitos_condenatorios", "[VALORES_OBRIGACOES_CORRECAO_MORA]");
        fillables.put("sucumbencia", "[HONORARIOS_CUSTAS_E_CONSECTARIOS]");
        LinkedHashMap<String, Object> metadata = baseMetadata(processo, req, corpus);
        metadata.put("templateFamily", "HOMOLOGATORIA_COM_MERITO");
        metadata.put("cpcAnchorPrimary", "ART_487_III_A");
        metadata.put("publicationLocked", true);
        LaianeAdvisoryMode mode = resolveMode(List.of());
        return new LaianeJudicialDecisionAdvisoryResponse(
                JudicialDecisionTemplateCode.RECONHECIMENTO_PROCEDENCIA_HOMOLOGADO,
                mode,
                true,
                true,
                "Caso com sinal de reconhecimento da procedência, apto a minuta assistida homologatória com resolução de mérito, sempre sob revisão judicial plena.",
                List.of("CPC art. 487, III, a", "CPC art. 489"),
                List.copyOf(checklist),
                List.of(),
                mode == LaianeAdvisoryMode.RESTRITIVO ? null : "Homologo o reconhecimento da procedência do pedido, com resolução de mérito, nos termos do art. 487, III, a, do CPC, fixando os efeitos concretos conforme a extensão material reconhecida nos autos.",
                Map.copyOf(fillables),
                Map.copyOf(metadata)
        );
    }

    private LaianeJudicialDecisionAdvisoryResponse generica(Processo processo,
                                                            LaianeSentencaDraftRequest req,
                                                            String corpus) {
        LinkedHashMap<String, Object> metadata = baseMetadata(processo, req, corpus);
        metadata.put("templateFamily", "GENERICA_ASSISTIDA");
        metadata.put("publicationLocked", true);
        return new LaianeJudicialDecisionAdvisoryResponse(
                JudicialDecisionTemplateCode.ASSISTENCIA_DECISORIA_GENERICA,
                LaianeAdvisoryMode.BLOQUEADOR,
                true,
                true,
                "Caso sem enquadramento terminal padronizado imediato; a assistência permanece estruturante e não substitui a fundamentação individualizada do magistrado.",
                List.of("CPC art. 489"),
                List.of(
                        "Definir com precisão o estado do processo e a questão principal a ser resolvida.",
                        "Explicitar fatos provados, controvérsias remanescentes e o enquadramento normativo aplicável.",
                        "Redigir dispositivo aderente ao pedido e às consequências processuais concretas."
                ),
                List.of(),
                null,
                Map.of(),
                Map.copyOf(metadata)
        );
    }

    /**
     * SUGESTIVO/RESTRITIVO só se aplicam a um padrão de caso já reconhecido (chamado pelos seis
     * métodos de template); BLOQUEADOR é reservado ao caso sem padrão ({@link #generica}), atribuído
     * diretamente lá, nunca por esta função.
     */
    private LaianeAdvisoryMode resolveMode(List<String> pendencias) {
        return pendencias.isEmpty() ? LaianeAdvisoryMode.SUGESTIVO : LaianeAdvisoryMode.RESTRITIVO;
    }

    private LinkedHashMap<String, Object> baseMetadata(Processo processo, LaianeSentencaDraftRequest req, String corpus) {
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("processoId", processo != null ? processo.getId() : null);
        metadata.put("numeroProcesso", processo != null ? firstNonBlank(processo.getNumeroUnificado(), processo.getNumeroProcesso()) : null);
        metadata.put("ramoDireito", processo != null && processo.getRamoDireito() != null ? processo.getRamoDireito().name() : null);
        metadata.put("rito", processo != null && processo.getRito() != null ? processo.getRito().name() : null);
        metadata.put("faseAtual", processo != null && processo.getFaseAtual() != null ? processo.getFaseAtual().name() : null);
        metadata.put("statusProcesso", processo != null && processo.getStatusProcesso() != null ? processo.getStatusProcesso().name() : null);
        metadata.put("corpusFingerprint", Integer.toHexString(Objects.toString(corpus, "").hashCode()));
        metadata.put("incluirAnalise", req != null && Boolean.TRUE.equals(req.getIncluirAnalise()));
        metadata.entrySet().removeIf(e -> e.getKey() == null || e.getValue() == null);
        return metadata;
    }

    private boolean ramoSensivel(Processo processo) {
        if (processo == null || processo.getRamoDireito() == null) {
            return false;
        }
        RamoDireito ramo = processo.getRamoDireito();
        return ramo == RamoDireito.FAMILIA || ramo == RamoDireito.INFANCIA_JUVENTUDE || ramo == RamoDireito.PENAL || ramo == RamoDireito.MILITAR;
    }

    private String buildCorpus(Processo processo, LaianeSentencaDraftRequest req) {
        List<String> parts = new ArrayList<>();
        if (processo != null) {
            parts.add(firstNonBlank(processo.getAssunto(), processo.getClasseProcessual()));
            parts.add(firstNonBlank(processo.getResumoIA(), processo.getObjetoProcessual(), processo.getResultadoFinal()));
            parts.add(firstNonBlank(processo.getPedidoPrincipal(), processo.getPedidosConsolidados()));
            parts.add(firstNonBlank(processo.getJanelaAcordoResumo()));
        }
        if (req != null) {
            parts.add(firstNonBlank(req.getFatos(), req.getFundamentos(), req.getDispositivoExtra()));
            if (req.getPedidos() != null && !req.getPedidos().isEmpty()) {
                parts.add(String.join(" ", req.getPedidos()));
            }
            if (req.getProvas() != null && !req.getProvas().isEmpty()) {
                parts.add(String.join(" ", req.getProvas()));
            }
        }
        return normalize(parts.stream().filter(Objects::nonNull).map(String::trim).filter(s -> !s.isBlank()).reduce((a, b) -> a + ' ' + b).orElse(""));
    }

    private boolean containsAny(String source, String... needles) {
        if (source == null || source.isBlank() || needles == null) {
            return false;
        }
        String normalized = normalize(source);
        for (String needle : needles) {
            if (needle != null && !needle.isBlank() && normalized.contains(normalize(needle))) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", " ");
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
