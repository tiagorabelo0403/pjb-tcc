package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.MateriaJurisdicao;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class ProceduralIntelligenceAdvisor {

    private ProceduralIntelligenceAdvisor() {
    }

    public static ProceduralIntelligenceAdvisoryReport analyzeRouting(Map<String, Object> payload,
                                                                     String actionNature,
                                                                     String actionFamily,
                                                                     TipoJustica tipoJustica,
                                                                     String ritoSugerido,
                                                                     String classeTpuCodigo,
                                                                     String classeTpuNome,
                                                                     String complexityBand,
                                                                     String probatoryProfile,
                                                                     double routingConfidence,
                                                                     String routingRiskLevel) {
        LinkedHashMap<String, Object> source = new LinkedHashMap<>();
        if (payload != null) {
            source.putAll(payload);
        }
        source.put("__routingActionNature", actionNature);
        source.put("__routingActionFamily", actionFamily);
        source.put("__routingTipoJustica", tipoJustica != null ? tipoJustica.name() : null);
        source.put("__routingRito", ritoSugerido);
        source.put("__routingClasseTpuCodigo", classeTpuCodigo);
        source.put("__routingClasseTpuNome", classeTpuNome);
        source.put("__routingComplexityBand", complexityBand);
        source.put("__routingProbatoryProfile", probatoryProfile);
        source.put("__routingRiskLevel", routingRiskLevel);
        source.put("__routingConfidence", routingConfidence);
        return analyzeSource(source);
    }

    public static ProceduralIntelligenceAdvisoryReport analyzeProcess(Processo processo, ProceduralRoutingReport routing) {
        LinkedHashMap<String, Object> source = new LinkedHashMap<>();
        if (processo != null) {
            source.put("classeProcessual", processo.getClasseProcessual());
            source.put("classeTpuCodigo", processo.getClasseTpuCodigo());
            source.put("assunto", processo.getAssunto());
            source.put("objetoProcessual", processo.getObjetoProcessual());
            source.put("pedidoPrincipal", processo.getPedidoPrincipal());
            source.put("pedidosConsolidados", processo.getPedidosConsolidados());
            source.put("materialProbatorioResumo", processo.getMaterialProbatorioResumo());
            source.put("ramoDireito", processo.getRamoDireito() != null ? processo.getRamoDireito().name() : null);
            source.put("rito", processo.getRito() != null ? processo.getRito().name() : null);
            source.put("materia", processo.getMateria() != null ? processo.getMateria().name() : null);
            source.put("tipoJustica", processo.getTipoJustica() != null ? processo.getTipoJustica().name() : null);
            source.put("tribunal", processo.getTribunal());
            source.put("tribunalCodigo", processo.getTribunalCodigoRoteado());
            source.put("uf", processo.getUf());
            source.put("comarca", processo.getComarca());
            source.put("vara", processo.getVara());
        }
        if (routing != null) {
            source.put("__routingActionNature", routing.actionNature());
            source.put("__routingActionFamily", routing.actionFamily());
            source.put("__routingTipoJustica", routing.tipoJusticaSugerida());
            source.put("__routingRito", routing.ritoSugerido());
            source.put("__routingClasseTpuCodigo", routing.forumAllocation() != null ? routing.forumAllocation().classeTpuCodigo() : null);
            source.put("__routingClasseTpuNome", routing.forumAllocation() != null ? routing.forumAllocation().classeTpuNome() : null);
            source.put("__routingComplexityBand", routing.complexityBand());
            source.put("__routingProbatoryProfile", routing.probatoryProfile());
            source.put("__routingRiskLevel", routing.riskLevel());
            source.put("__routingConfidence", routing.confidence());
        }
        return analyzeSource(source);
    }

    private static ProceduralIntelligenceAdvisoryReport analyzeSource(Map<String, Object> source) {
        String corpus = buildCorpus(source);
        SignalMatrix signals = scoreSignals(source, corpus);
        CandidateNature winner = choosePrimaryNature(signals);
        TipoJustica tipoJustica = resolveTipoJustica(source, corpus, signals);
        RamoDireito ramo = resolveRamo(source, corpus, signals, winner.natureza());
        RitoProcessual rito = resolveRito(source, corpus, ramo, winner.natureza());
        MateriaJurisdicao materia = resolveMateria(source, corpus, ramo, rito, winner.natureza());
        EnumSet<NaturezaJuridicaQualifier> qualifiers = mergeQualifiers(winner.natureza(), source, corpus, ramo, rito, materia);
        NivelSigilo sigilo = resolveSigilo(corpus, ramo, rito, winner.natureza(), qualifiers);
        List<String> recommendedDocuments = recommendDocuments(corpus, ramo, rito, materia, winner.natureza(), qualifiers);
        List<String> riskFlags = buildRiskFlags(source, corpus, ramo, rito, materia, winner.natureza(), qualifiers);
        List<String> discardedAlternatives = signals.candidates().stream()
                .filter(candidate -> candidate != winner)
                .sorted(Comparator.comparingDouble(CandidateNature::score).reversed())
                .limit(4)
                .map(candidate -> candidate.natureza().name() + ":" + formatScore(candidate.score()))
                .toList();
        double confidence = computeConfidence(source, winner, qualifiers, riskFlags, ramo, rito, materia, tipoJustica);
        boolean fallbackUsed = winner.fallback() || ramo == null || rito == null || materia == null || tipoJustica == null;
        boolean reviewRequired = confidence < 0.63d || riskFlags.stream().anyMatch(flag -> flag.startsWith("INCONSISTENCIA"));
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("mode", "ADVISORY_SHADOW");
        metadata.put("signalCounts", signals.summary());
        metadata.put("routingActionNature", text(source.get("__routingActionNature")));
        metadata.put("routingActionFamily", text(source.get("__routingActionFamily")));
        metadata.put("routingRiskLevel", text(source.get("__routingRiskLevel")));
        metadata.put("routingConfidence", decimal(source.get("__routingConfidence")));
        metadata.put("classeTpuCodigo", firstNonBlank(text(source.get("classeTpuCodigo")), text(source.get("__routingClasseTpuCodigo"))));
        metadata.put("classeTpuNome", firstNonBlank(text(source.get("classeProcessual")), text(source.get("__routingClasseTpuNome"))));
        metadata.put("probatoryProfile", firstNonBlank(text(source.get("__routingProbatoryProfile")), inferProbatoryProfile(corpus, recommendedDocuments)));
        metadata.put("complexityBand", firstNonBlank(text(source.get("__routingComplexityBand")), inferComplexityBand(qualifiers, riskFlags, materia)));
        metadata.put("corpusFingerprint", Integer.toHexString(corpus.hashCode()));
        metadata.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
        return new ProceduralIntelligenceAdvisoryReport(
                Instant.now(),
                winner.natureza(),
                List.copyOf(qualifiers),
                tipoJustica,
                ramo,
                rito,
                materia,
                sigilo,
                confidence,
                resolveUncertaintyLevel(confidence, reviewRequired, fallbackUsed),
                primaryReason(source, corpus, ramo, rito, materia, winner.natureza()),
                List.copyOf(signals.supportingSignals()),
                discardedAlternatives,
                recommendedDocuments,
                riskFlags,
                qualifiers.contains(NaturezaJuridicaQualifier.URGENTE),
                reviewRequired,
                fallbackUsed,
                Collections.unmodifiableMap(metadata)
        );
    }

    private static SignalMatrix scoreSignals(Map<String, Object> source, String corpus) {
        LinkedHashMap<NaturezaJuridicaCanonical, CandidateNature> scores = new LinkedHashMap<>();
        for (NaturezaJuridicaCanonical natureza : NaturezaJuridicaCanonical.values()) {
            scores.put(natureza, new CandidateNature(natureza, 0d, false));
        }
        LinkedHashSet<String> supportingSignals = new LinkedHashSet<>();
        apply(scores, supportingSignals, corpus, NaturezaJuridicaCanonical.MANDAMENTAL, 1.85d,
                "MANDADO DE SEGURANCA", "HABEAS CORPUS", "HABEAS DATA", "MANDADO DE INJUNCAO", "LIMINAR CONTRA ATO COATOR", "AUTORIDADE COATORA", "DIREITO LIQUIDO E CERTO");
        apply(scores, supportingSignals, corpus, NaturezaJuridicaCanonical.EXECUTIVA, 1.85d,
                "EXECUCAO FISCAL", "CUMPRIMENTO DE SENTENCA", "CUMPRIMENTO DE ACORDO", "TITULO EXECUTIVO", "PENHORA", "EXPROPRIACAO", "CDA", "DIVIDA ATIVA", "LEF", "LIQUIDACAO DE SENTENCA", "EMBARGOS A EXECUCAO");
        apply(scores, supportingSignals, corpus, NaturezaJuridicaCanonical.CAUTELAR, 1.45d,
                "CAUTELAR", "ARRESTO", "SEQUESTRO", "BUSCA E APREENSAO CAUTELAR", "PRODUCAO ANTECIPADA DE PROVA", "EXIBICAO CAUTELAR");
        apply(scores, supportingSignals, corpus, NaturezaJuridicaCanonical.TUTELA_PROVISORIA, 1.55d,
                "TUTELA DE URGENCIA", "TUTELA ANTECIPADA", "LIMINAR", "PERIGO DE DANO", "RISCO AO RESULTADO UTIL", "URGENTE", "MEDICAMENTO", "LEITO", "UTI", "HOME CARE");
        apply(scores, supportingSignals, corpus, NaturezaJuridicaCanonical.HOMOLOGATORIA, 1.6d,
                "HOMOLOGACAO", "HOMOLOGAR", "ACORDO EXTRAJUDICIAL", "SENTENCA ESTRANGEIRA", "PLANO DE PARTILHA", "ALVARA JUDICIAL");
        apply(scores, supportingSignals, corpus, NaturezaJuridicaCanonical.JURISDICAO_VOLUNTARIA, 1.45d,
                "JURISDICAO VOLUNTARIA", "INVENTARIO", "ARROLAMENTO", "ALVARA", "RETIFICACAO DE REGISTRO", "AVERBACAO", "REGISTRO PUBLICO", "CONSENSUAL", "ADJUDICACAO COMPULSORIA EXTRAJUDICIAL");
        apply(scores, supportingSignals, corpus, NaturezaJuridicaCanonical.SANCIONATORIA, 1.7d,
                "AIJE", "AIME", "INELEGIBILIDADE", "IMPROBIDADE", "PAD", "SANCAO", "PENALIDADE", "MULTA", "CRIME", "DENUNCIA", "CONDENACAO PENAL", "TRIBUNAL DO JURI", "CAPTACAO ILICITA");
        apply(scores, supportingSignals, corpus, NaturezaJuridicaCanonical.INVESTIGATIVA, 1.75d,
                "INQUERITO", "IPM", "INVESTIGACAO", "DILIGENCIA", "QUEBRA DE SIGILO", "BUSCA E APREENSAO INVESTIGATIVA", "PROCEDIMENTO INVESTIGATORIO");
        apply(scores, supportingSignals, corpus, NaturezaJuridicaCanonical.IMPUGNATIVA, 1.55d,
                "IMPUGNACAO", "EMBARGOS", "RECURSO", "APELACAO", "AGRAVO", "AIRC", "RCED", "RESCISAO DE JULGADO", "RESISTENCIA A ATO", "ANULATORIA", "DESCONSTITUICAO");
        apply(scores, supportingSignals, corpus, NaturezaJuridicaCanonical.REVISIONAL, 1.55d,
                "REVISIONAL", "REVISAO", "REVISAO DE BENEFICIO", "REVISAO CONTRATUAL", "REVISAO DE ALIMENTOS", "REVISAO CRIMINAL");
        apply(scores, supportingSignals, corpus, NaturezaJuridicaCanonical.ESTRUTURAL_COLETIVA, 1.8d,
                "ACAO CIVIL PUBLICA", "ACP", "COLETIVA", "TUTELA COLETIVA", "DANO AMBIENTAL", "POLITICA PUBLICA", "DEMANDA ESTRUTURAL", "DIREITOS DIFUSOS", "DIREITOS COLETIVOS", "DIREITOS INDIVIDUAIS HOMOGENEOS", "MINISTERIO PUBLICO");
        apply(scores, supportingSignals, corpus, NaturezaJuridicaCanonical.CONHECIMENTO_DECLARATORIA, 1.25d,
                "DECLARATORIA", "DECLARACAO DE INEXISTENCIA", "RECONHECIMENTO DE DIREITO", "DECLARACAO", "NULIDADE", "INEXIGIBILIDADE");
        apply(scores, supportingSignals, corpus, NaturezaJuridicaCanonical.CONHECIMENTO_CONDENATORIA, 1.35d,
                "COBRANCA", "INDENIZACAO", "OBRIGACAO DE FAZER", "OBRIGACAO DE NAO FAZER", "ALIMENTOS", "RECLAMACAO TRABALHISTA", "ADIMPLEMENTO", "RESCISAO INDIRETA", "DANOS MORAIS", "DANOS MATERIAIS", "REPETICAO DE INDEBITO");
        apply(scores, supportingSignals, corpus, NaturezaJuridicaCanonical.CONHECIMENTO_CONSTITUTIVA, 1.35d,
                "DIVORCIO", "DISSOLUCAO", "UNIAO ESTAVEL", "PATERNIDADE", "MATERNIDADE", "ADOCAO", "INTERDICAO", "CURATELA", "GUARDA", "ALTERACAO DE REGIME", "DESCONSIDERACAO DA PERSONALIDADE JURIDICA");

        String actionNature = normalize(text(source.get("__routingActionNature")));
        String actionFamily = normalize(text(source.get("__routingActionFamily")));
        if (!actionNature.isBlank()) {
            apply(scores, supportingSignals, actionNature, NaturezaJuridicaCanonical.EXECUTIVA, 1.3d,
                    "EXECUCAO", "EXECUCAO_FISCAL", "CUMPRIMENTO_SENTENCA", "COBRANCA_REPETICAO");
            apply(scores, supportingSignals, actionNature, NaturezaJuridicaCanonical.MANDAMENTAL, 1.45d,
                    "MANDADO_SEGURANCA", "HABEAS_CORPUS", "HABEAS_DATA");
            apply(scores, supportingSignals, actionNature, NaturezaJuridicaCanonical.INVESTIGATIVA, 1.4d,
                    "IPM", "INQUERITO", "INVESTIGATORIO");
            apply(scores, supportingSignals, actionNature, NaturezaJuridicaCanonical.SANCIONATORIA, 1.35d,
                    "AIJE", "AIME", "IMPROBIDADE_ADMINISTRATIVA", "PROCESSO_PENAL_MILITAR", "TRIBUNAL_DO_JURI");
            apply(scores, supportingSignals, actionNature, NaturezaJuridicaCanonical.IMPUGNATIVA, 1.3d,
                    "AIRC", "RCED", "EMBARGOS", "IMPUGNACAO", "ACAO_RESCISORIA");
            apply(scores, supportingSignals, actionNature, NaturezaJuridicaCanonical.REVISIONAL, 1.25d,
                    "REVISIONAL", "REVISAO");
            apply(scores, supportingSignals, actionNature, NaturezaJuridicaCanonical.HOMOLOGATORIA, 1.2d,
                    "HOMOLOGACAO", "ACORDO_EXTRAJUDICIAL");
        }
        if (!actionFamily.isBlank()) {
            apply(scores, supportingSignals, actionFamily, NaturezaJuridicaCanonical.ESTRUTURAL_COLETIVA, 0.9d,
                    "COLETIVA", "AMBIENTAL", "CONSTITUCIONAL");
            apply(scores, supportingSignals, actionFamily, NaturezaJuridicaCanonical.SANCIONATORIA, 0.9d,
                    "PENAL", "ELEITORAL", "MILITAR");
            apply(scores, supportingSignals, actionFamily, NaturezaJuridicaCanonical.CONHECIMENTO_CONDENATORIA, 0.7d,
                    "TRABALHISTA", "PREVIDENCIARIO", "CIVIL", "CONSUMIDOR");
        }
        if (containsAny(corpus, "SEM CONFLITO", "CONSENSUAL", "HOMOLOGACAO DE ACORDO", "VOLUNTARIA")) {
            supportingSignals.add("VOLUNTARIEDADE");
            increase(scores, NaturezaJuridicaCanonical.JURISDICAO_VOLUNTARIA, 0.9d);
            increase(scores, NaturezaJuridicaCanonical.HOMOLOGATORIA, 0.75d);
        }
        if (containsAny(corpus, "TUTELA COLETIVA", "POLITICA PUBLICA", "SISTEMA CARCERARIO", "SAUDE PUBLICA EM MASSA", "MORADIA COLETIVA")) {
            supportingSignals.add("ESTRUTURALIDADE");
            increase(scores, NaturezaJuridicaCanonical.ESTRUTURAL_COLETIVA, 1.1d);
        }
        List<CandidateNature> candidates = scores.values().stream()
                .sorted(Comparator.comparingDouble(CandidateNature::score).reversed())
                .toList();
        return new SignalMatrix(candidates, List.copyOf(supportingSignals));
    }

    private static CandidateNature choosePrimaryNature(SignalMatrix signals) {
        if (signals.candidates().isEmpty()) {
            return new CandidateNature(NaturezaJuridicaCanonical.CONHECIMENTO_CONDENATORIA, 0.45d, true);
        }
        CandidateNature first = signals.candidates().getFirst();
        if (first.score() > 0.01d) {
            return first;
        }
        return new CandidateNature(NaturezaJuridicaCanonical.CONHECIMENTO_CONDENATORIA, 0.45d, true);
    }

    private static TipoJustica resolveTipoJustica(Map<String, Object> source, String corpus, SignalMatrix signals) {
        TipoJustica explicit = TipoJustica.fromString(firstNonBlank(text(source.get("tipoJustica")), text(source.get("__routingTipoJustica"))));
        if (explicit != null) {
            return explicit;
        }
        if (containsAny(corpus, "TRE", "TSE", "ZONA ELEITORAL", "ELEICAO", "CANDIDAT", "PARTIDO POLITIC")) {
            return TipoJustica.ELEITORAL;
        }
        if (containsAny(corpus, "TRT", "TST", "VARA DO TRABALHO", "CLT", "FGTS", "HORAS EXTRAS", "VERBAS RESCISORIAS")) {
            return TipoJustica.TRABALHO;
        }
        if (containsAny(corpus, "STM", "TJM", "AUDITORIA MILITAR", "IPM", "CPPM", "CRIME MILITAR")) {
            return containsAny(corpus, "EXERCITO", "MARINHA", "AERONAUTICA", "UNIAO", "FORCAS ARMADAS", "STM")
                    ? TipoJustica.MILITAR_FEDERAL
                    : TipoJustica.MILITAR_ESTADUAL;
        }
        if (containsAny(corpus, "TRF", "JUSTICA FEDERAL", "JEF", "SECAO JUDICIARIA", "SUBSECAO JUDICIARIA", "INSS", "AUTARQUIA FEDERAL", "UNIAO", "RECEITA FEDERAL", "IBAMA", "ICMBIO")) {
            return TipoJustica.FEDERAL;
        }
        if (containsAny(corpus, "STJ", "STF", "SENTENCA ESTRANGEIRA", "CARTA ROGATORIA", "ADI", "ADC", "ADPF")) {
            return TipoJustica.SUPERIOR;
        }
        return null;
    }

    private static RamoDireito resolveRamo(Map<String, Object> source,
                                           String corpus,
                                           SignalMatrix signals,
                                           NaturezaJuridicaCanonical natureza) {
        RamoDireito explicit = RamoDireito.fromString(firstNonBlank(text(source.get("ramoDireito")), text(source.get("materia")), text(source.get("__routingActionFamily"))));
        if (explicit != null) {
            return explicit;
        }
        if (containsAny(corpus, "ELEITOR", "AIJE", "AIME", "AIRC", "RCED", "REGISTRO DE CANDIDATURA", "PARTIDO POLITIC")) {
            return RamoDireito.ELEITORAL;
        }
        if (containsAny(corpus, "IPM", "CPPM", "AUDITORIA MILITAR", "CRIME MILITAR", "TRANSGRESSAO DISCIPLINAR MILITAR")) {
            return RamoDireito.MILITAR;
        }
        if (containsAny(corpus, "CLT", "RECLAMACAO TRABALHISTA", "FGTS", "VERBAS RESCISORIAS", "DISSIDIO COLETIVO")) {
            return RamoDireito.TRABALHISTA;
        }
        if (containsAny(corpus, "PREVID", "INSS", "BPC", "LOAS", "APOSENTADOR", "AUXILIO", "PENSAO POR MORTE")) {
            return RamoDireito.PREVIDENCIARIO;
        }
        if (containsAny(corpus, "TRIBUTO", "ICMS", "ISS", "IPTU", "IPVA", "FAZENDA PUBLICA", "EXECUCAO FISCAL", "CDA")) {
            return RamoDireito.TRIBUTARIO;
        }
        if (containsAny(corpus, "IMPROBIDADE", "PAD", "CONCURSO PUBLICO", "ATO ADMINISTRATIVO", "SERVIDOR PUBLICO")) {
            return RamoDireito.ADMINISTRATIVO;
        }
        if (containsAny(corpus, "ALIMENTOS", "DIVORCIO", "GUARDA", "PATERNIDADE", "CURATELA", "INTERDICAO")) {
            return RamoDireito.FAMILIA;
        }
        if (containsAny(corpus, "CONSUMIDOR", "CDC", "NEGATIVACAO", "SERASA", "SPC", "COBRANCA INDEVIDA", "PRODUTO", "SERVICO")) {
            return RamoDireito.CONSUMIDOR;
        }
        if (containsAny(corpus, "FALENCIA", "RECUPERACAO JUDICIAL", "RECUPERACAO EXTRAJUDICIAL", "SOCIEDADE EMPRESARIA", "DISSOLUCAO SOCIETARIA")) {
            return RamoDireito.EMPRESARIAL;
        }
        if (containsAny(corpus, "AGRARIO", "IMOVEL RURAL", "USUCAPIAO RURAL", "ASSENTAMENTO", "POSSE RURAL")) {
            return RamoDireito.AGRARIO;
        }
        if (containsAny(corpus, "AMBIENT", "IBAMA", "DESMAT", "LICENCIAMENTO")) {
            return RamoDireito.AMBIENTAL;
        }
        if (containsAny(corpus, "HABEAS CORPUS", "DENUNCIA", "QUEIXA CRIME", "TRIBUNAL DO JURI", "LEI DE DROGAS", "MARIA DA PENHA")) {
            return RamoDireito.PENAL;
        }
        if (containsAny(corpus, "SENTENCA ESTRANGEIRA", "CARTA ROGATORIA", "COOPERACAO JURIDICA INTERNACIONAL")) {
            return RamoDireito.INTERNACIONAL;
        }
        if (natureza == NaturezaJuridicaCanonical.MANDAMENTAL && containsAny(corpus, "ATO COATOR", "AUTORIDADE COATORA", "DIREITO LIQUIDO E CERTO")) {
            return RamoDireito.CONSTITUCIONAL;
        }
        return null;
    }

    private static RitoProcessual resolveRito(Map<String, Object> source,
                                              String corpus,
                                              RamoDireito ramo,
                                              NaturezaJuridicaCanonical natureza) {
        RitoProcessual explicit = RitoProcessual.fromString(firstNonBlank(text(source.get("rito")), text(source.get("__routingRito"))));
        if (explicit != null) {
            return explicit;
        }
        if (containsAny(corpus, "HABEAS CORPUS") && ramo == RamoDireito.MILITAR) {
            return RitoProcessual.MILITAR_HABEAS_CORPUS_MILITAR;
        }
        if (containsAny(corpus, "MANDADO DE SEGURANCA") && ramo == RamoDireito.TRABALHISTA) {
            return RitoProcessual.TRABALHISTA_MANDADO_SEGURANCA;
        }
        if (containsAny(corpus, "MANDADO DE SEGURANCA") && ramo == RamoDireito.TRIBUTARIO) {
            return RitoProcessual.TRIBUTARIO_MANDADO_SEGURANCA;
        }
        if (containsAny(corpus, "EXECUCAO FISCAL", "DIVIDA ATIVA", "CDA")) {
            return RitoProcessual.EXECUCAO_FISCAL;
        }
        if (containsAny(corpus, "EMBARGOS A EXECUCAO FISCAL")) {
            return RitoProcessual.TRIBUTARIO_EMBARGOS_EXECUCAO_FISCAL;
        }
        if (containsAny(corpus, "REPETICAO DE INDEBITO") && ramo == RamoDireito.TRIBUTARIO) {
            return RitoProcessual.TRIBUTARIO_REPETICAO_INDEBITO;
        }
        if (containsAny(corpus, "ANULATORIA DE DEBITO")) {
            return RitoProcessual.TRIBUTARIO_ANULATORIA_DEBITO;
        }
        if (containsAny(corpus, "AIJE", "ABUSO DE PODER", "CAPTACAO ILICITA SUFRAGIO")) {
            return RitoProcessual.ELEITORAL_AIJE;
        }
        if (containsAny(corpus, "AIME")) {
            return RitoProcessual.ELEITORAL_AIME;
        }
        if (containsAny(corpus, "AIRC", "IMPUGNACAO DE REGISTRO")) {
            return RitoProcessual.ELEITORAL_AIRC;
        }
        if (containsAny(corpus, "RCED", "EXPEDICAO DO DIPLOMA")) {
            return RitoProcessual.ELEITORAL_RCED;
        }
        if (containsAny(corpus, "IPM", "INQUERITO POLICIAL MILITAR")) {
            return RitoProcessual.MILITAR_IPM;
        }
        if (containsAny(corpus, "PROCESSO ADMINISTRATIVO DISCIPLINAR MILITAR", "PAD MILITAR")) {
            return RitoProcessual.MILITAR_PAD;
        }
        if (containsAny(corpus, "INQUERITO JUDICIAL", "FALTA GRAVE", "ART 853", "ART. 853") && ramo == RamoDireito.TRABALHISTA) {
            return RitoProcessual.TRABALHISTA_INQUERITO_FALTA_GRAVE;
        }
        if (containsAny(corpus, "ACAO DE CUMPRIMENTO", "AÇÃO DE CUMPRIMENTO", "ART 872", "ART. 872") && ramo == RamoDireito.TRABALHISTA) {
            return RitoProcessual.TRABALHISTA_ACAO_CUMPRIMENTO;
        }
        if (containsAny(corpus, "RECLAMACAO TRABALHISTA", "VERBAS RESCISORIAS", "HORAS EXTRAS", "VINCULO EMPREGATICIO")) {
            if (containsAny(corpus, "SUMARIO", "ALCADA", "ALÇADA", "LEI 5.584", "LEI 5584")) {
                return RitoProcessual.TRABALHISTA_SUMARIO_ALCADA;
            }
            return containsAny(corpus, "SUMARISSIMO") ? RitoProcessual.TRABALHISTA_SUMARISSIMO : RitoProcessual.TRABALHISTA_ORDINARIO;
        }
        if (containsAny(corpus, "DISSIDIO COLETIVO", "GREVE", "CONVENCAO COLETIVA")) {
            return RitoProcessual.TRABALHISTA_DISSIDIO_COLETIVO;
        }
        if (containsAny(corpus, "INVENTARIO", "ARROLAMENTO", "HERANCA", "PARTILHA")) {
            return RitoProcessual.CIVIL_INVENTARIO_ARROLAMENTO;
        }
        if (containsAny(corpus, "ALIMENTOS", "PENSAO ALIMENTICIA")) {
            return RitoProcessual.CIVIL_FAMILIA_ALIMENTOS;
        }
        if (containsAny(corpus, "DIVORCIO", "DISSOLUCAO DE CASAMENTO")) {
            return RitoProcessual.CIVIL_FAMILIA_DIVORCIO;
        }
        if (containsAny(corpus, "USUCAPIAO")) {
            return containsAny(corpus, "RURAL", "IMOVEL RURAL", "POSSE RURAL")
                    ? RitoProcessual.AGRARIO_USUCAPIAO_RURAL
                    : RitoProcessual.CIVIL_USUCAPIAO;
        }
        if (containsAny(corpus, "SENTENCA ESTRANGEIRA")) {
            return RitoProcessual.HOMOLOGACAO_SENTENCA_ESTRANGEIRA;
        }
        if (containsAny(corpus, "CARTA ROGATORIA")) {
            return RitoProcessual.CARTA_ROGATORIA;
        }
        if (containsAny(corpus, "COOPERACAO JURIDICA INTERNACIONAL", "AUXILIO DIRETO INTERNACIONAL")) {
            return RitoProcessual.COOPERACAO_JURIDICA_INTERNACIONAL;
        }
        if (natureza == NaturezaJuridicaCanonical.MANDAMENTAL && ramo == RamoDireito.CONSTITUCIONAL) {
            return RitoProcessual.ESPECIAL_MANDADO_SEGURANCA;
        }
        return null;
    }

    private static MateriaJurisdicao resolveMateria(Map<String, Object> source,
                                                    String corpus,
                                                    RamoDireito ramo,
                                                    RitoProcessual rito,
                                                    NaturezaJuridicaCanonical natureza) {
        String explicitText = firstNonBlank(text(source.get("materia")), text(source.get("ramoDireito")));
        if (explicitText != null) {
            try {
                return MateriaJurisdicao.valueOf(normalizeEnumToken(explicitText));
            } catch (IllegalArgumentException ignored) {
                MateriaJurisdicao fromRamo = MateriaJurisdicao.fromRamo(RamoDireito.fromString(explicitText));
                if (fromRamo != null) {
                    return fromRamo;
                }
            }
        }
        if (containsAny(corpus, "ELEITOR", "AIJE", "AIME", "AIRC", "RCED", "REGISTRO DE CANDIDATURA")) {
            return MateriaJurisdicao.ELEITORAL;
        }
        if (containsAny(corpus, "IPM", "CRIME MILITAR", "CPPM", "AUDITORIA MILITAR")) {
            return MateriaJurisdicao.MILITAR;
        }
        if (containsAny(corpus, "CLT", "FGTS", "VERBAS RESCISORIAS", "HORAS EXTRAS", "DISSIDIO COLETIVO")) {
            return MateriaJurisdicao.TRABALHISTA;
        }
        if (containsAny(corpus, "TRIBUTO", "ICMS", "ISS", "IPTU", "IPVA", "EXECUCAO FISCAL", "DIVIDA ATIVA", "CDA")) {
            return rito != null && rito.isExecucaoFiscalEstrita() ? MateriaJurisdicao.EXECUCAO_FISCAL : MateriaJurisdicao.TRIBUTARIA;
        }
        if (containsAny(corpus, "PREVID", "INSS", "BPC", "LOAS", "APOSENTADOR", "AUXILIO", "PENSAO POR MORTE")) {
            return MateriaJurisdicao.PREVIDENCIARIA;
        }
        if (containsAny(corpus, "ALIMENTOS", "DIVORCIO", "GUARDA", "PATERNIDADE", "CURATELA", "INTERDICAO")) {
            return MateriaJurisdicao.FAMILIA;
        }
        if (containsAny(corpus, "INVENTARIO", "ARROLAMENTO", "HERANCA", "PARTILHA")) {
            return MateriaJurisdicao.SUCESSOES;
        }
        if (containsAny(corpus, "CONSUMIDOR", "CDC", "NEGATIVACAO", "SERASA", "SPC", "COBRANCA INDEVIDA")) {
            return MateriaJurisdicao.CONSUMIDOR;
        }
        if (containsAny(corpus, "MEDICAMENTO", "LEITO", "UTI", "TRATAMENTO", "HOME CARE", "PLANO DE SAUDE", "SUS")) {
            return MateriaJurisdicao.SAUDE;
        }
        if (containsAny(corpus, "MATRICULA", "ESCOLA", "UNIVERSIDADE", "CRECHE", "ENSINO")) {
            return MateriaJurisdicao.EDUCACAO;
        }
        if (containsAny(corpus, "HABEAS CORPUS", "DENUNCIA", "QUEIXA CRIME", "TRIBUNAL DO JURI", "LEI DE DROGAS", "MARIA DA PENHA", "ROUBO", "FURTO", "HOMICIDIO")) {
            return MateriaJurisdicao.PENAL;
        }
        if (containsAny(corpus, "IMPROBIDADE", "PAD", "CONCURSO PUBLICO", "LICITACAO", "ATO ADMINISTRATIVO")) {
            return MateriaJurisdicao.ADMINISTRATIVO;
        }
        if (containsAny(corpus, "AMBIENT", "IBAMA", "DESMAT", "LICENCIAMENTO")) {
            return MateriaJurisdicao.AMBIENTAL;
        }
        if (containsAny(corpus, "FALENCIA", "RECUPERACAO JUDICIAL", "RECUPERACAO EXTRAJUDICIAL")) {
            return MateriaJurisdicao.FALENCIAS;
        }
        if (containsAny(corpus, "EMPRESA", "SOCIEDADE EMPRESARIA", "CONTRATO SOCIAL", "DISSOLUCAO SOCIETARIA", "DESCONSIDERACAO DA PERSONALIDADE JURIDICA")) {
            return MateriaJurisdicao.EMPRESARIAL;
        }
        if (containsAny(corpus, "AGRARIO", "IMOVEL RURAL", "USUCAPIAO RURAL", "ASSENTAMENTO", "POSSE RURAL")) {
            return MateriaJurisdicao.AGRARIO;
        }
        if (containsAny(corpus, "MANDADO DE SEGURANCA", "MANDADO DE INJUNCAO", "ADI", "ADC", "ADPF", "HABEAS DATA")) {
            return MateriaJurisdicao.CONSTITUCIONAL;
        }
        if (ramo != null) {
            return MateriaJurisdicao.fromRamo(ramo);
        }
        if (natureza == NaturezaJuridicaCanonical.ESTRUTURAL_COLETIVA) {
            return MateriaJurisdicao.CONSTITUCIONAL;
        }
        return null;
    }

    private static EnumSet<NaturezaJuridicaQualifier> mergeQualifiers(NaturezaJuridicaCanonical natureza,
                                                                      Map<String, Object> source,
                                                                      String corpus,
                                                                      RamoDireito ramo,
                                                                      RitoProcessual rito,
                                                                      MateriaJurisdicao materia) {
        EnumSet<NaturezaJuridicaQualifier> qualifiers = natureza != null
                ? EnumSet.copyOf(natureza.baselineQualifiers())
                : EnumSet.noneOf(NaturezaJuridicaQualifier.class);
        if (containsAny(corpus, "ACP", "ACAO CIVIL PUBLICA", "DISSIDIO COLETIVO", "DIREITOS DIFUSOS", "DIREITOS COLETIVOS", "COLETIVA", "CLASS ACTION", "PUBLICA")) {
            qualifiers.add(NaturezaJuridicaQualifier.COLETIVA);
            qualifiers.remove(NaturezaJuridicaQualifier.INDIVIDUAL);
        } else {
            qualifiers.add(NaturezaJuridicaQualifier.INDIVIDUAL);
        }
        if (natureza != null && natureza.isVoluntaryByNature() || containsAny(corpus, "CONSENSUAL", "VOLUNTARIA", "SEM CONFLITO", "HOMOLOGACAO")) {
            qualifiers.add(NaturezaJuridicaQualifier.VOLUNTARIA);
            qualifiers.remove(NaturezaJuridicaQualifier.CONTENCIOSA);
        } else {
            qualifiers.add(NaturezaJuridicaQualifier.CONTENCIOSA);
        }
        if (containsAny(corpus, "UNIAO", "ESTADO", "MUNICIPIO", "FAZENDA PUBLICA", "MINISTERIO PUBLICO", "AUTARQUIA FEDERAL", "ADMINISTRACAO PUBLICA", "TRIBUNAL", "GOVERNO")) {
            qualifiers.add(NaturezaJuridicaQualifier.PUBLICA);
        } else {
            qualifiers.add(NaturezaJuridicaQualifier.PRIVADA);
        }
        if (containsAny(corpus, "COBRANCA", "EXECUCAO", "DIVIDA", "PENHORA", "INDENIZACAO", "VERBAS", "PATRIMONIAL", "REPETICAO DE INDEBITO", "RECUPERACAO JUDICIAL", "FALENCIA")
                || (materia != null && Set.of(MateriaJurisdicao.TRIBUTARIA, MateriaJurisdicao.EXECUCAO_FISCAL, MateriaJurisdicao.EMPRESARIAL, MateriaJurisdicao.FALENCIAS).contains(materia))) {
            qualifiers.add(NaturezaJuridicaQualifier.PATRIMONIAL);
        }
        if (containsAny(corpus, "DANOS MORAIS", "LIBERDADE", "HONRA", "IMAGEM", "SAUDE", "ALIMENTOS", "GUARDA", "PATERNIDADE", "MARIA DA PENHA")
                || (materia != null && Set.of(MateriaJurisdicao.FAMILIA, MateriaJurisdicao.SAUDE, MateriaJurisdicao.PENAL, MateriaJurisdicao.CONSTITUCIONAL).contains(materia))) {
            qualifiers.add(NaturezaJuridicaQualifier.EXTRAPATRIMONIAL);
        }
        if (containsAny(corpus, "LIMINAR", "TUTELA", "PERIGO DE DANO", "RISCO AO RESULTADO UTIL", "MEDICAMENTO", "LEITO", "UTI", "ALIMENTOS", "HABEAS CORPUS")
                || natureza != null && natureza.isUrgentByNature()) {
            qualifiers.add(NaturezaJuridicaQualifier.URGENTE);
        }
        if (containsAny(corpus, "POLITICA PUBLICA", "SISTEMA", "PLANO ESTRUTURAL", "CUMPRIMENTO ESTRUTURAL", "TUTELA COLETIVA")) {
            qualifiers.add(NaturezaJuridicaQualifier.ESTRUTURAL);
        }
        if (natureza != null && natureza.isEnforcementOriented()) {
            qualifiers.add(NaturezaJuridicaQualifier.EXECUTIVA);
        }
        if (natureza == NaturezaJuridicaCanonical.MANDAMENTAL) {
            qualifiers.add(NaturezaJuridicaQualifier.MANDAMENTAL);
        }
        if (natureza == NaturezaJuridicaCanonical.SANCIONATORIA) {
            qualifiers.add(NaturezaJuridicaQualifier.REPRESSIVA);
        }
        if (natureza == NaturezaJuridicaCanonical.TUTELA_PROVISORIA || natureza == NaturezaJuridicaCanonical.CAUTELAR) {
            qualifiers.add(NaturezaJuridicaQualifier.PREVENTIVA);
        }
        if (natureza == NaturezaJuridicaCanonical.CONHECIMENTO_CONDENATORIA || natureza == NaturezaJuridicaCanonical.EXECUTIVA) {
            qualifiers.add(NaturezaJuridicaQualifier.SATISFATIVA);
        }
        if (ramo == RamoDireito.ELEITORAL || ramo == RamoDireito.MILITAR || ramo == RamoDireito.ADMINISTRATIVO) {
            qualifiers.add(NaturezaJuridicaQualifier.PUBLICA);
        }
        if (rito != null && rito.isEmpresarial()) {
            qualifiers.add(NaturezaJuridicaQualifier.PATRIMONIAL);
        }
        return qualifiers;
    }

    private static NivelSigilo resolveSigilo(String corpus,
                                             RamoDireito ramo,
                                             RitoProcessual rito,
                                             NaturezaJuridicaCanonical natureza,
                                             Set<NaturezaJuridicaQualifier> qualifiers) {
        if (containsAny(corpus, "SEGREDO DE JUSTICA", "SEGREDO DE ESTADO", "DADOS MEDICOS SENSIVEIS", "ADOLESCENTE", "CRIANCA", "VIOLENCIA DOMESTICA", "ABUSO SEXUAL")) {
            return NivelSigilo.SEGREDO_JUSTICA;
        }
        if (rito != null && rito.requiresSegredoByDefault()) {
            return NivelSigilo.SEGREDO_JUSTICA;
        }
        if (natureza != null && natureza.suggestsConfidentiality() && (ramo == RamoDireito.PENAL || ramo == RamoDireito.MILITAR || ramo == RamoDireito.ELEITORAL)) {
            return NivelSigilo.SEGREDO_JUSTICA;
        }
        if (qualifiers.contains(NaturezaJuridicaQualifier.EXTRAPATRIMONIAL) && containsAny(corpus, "MENOR", "ALIMENTOS", "GUARDA", "CURATELA", "INTERDICAO")) {
            return NivelSigilo.SEGREDO_JUSTICA;
        }
        return null;
    }

    private static List<String> recommendDocuments(String corpus,
                                                   RamoDireito ramo,
                                                   RitoProcessual rito,
                                                   MateriaJurisdicao materia,
                                                   NaturezaJuridicaCanonical natureza,
                                                   Set<NaturezaJuridicaQualifier> qualifiers) {
        LinkedHashSet<String> docs = new LinkedHashSet<>();
        if (natureza == NaturezaJuridicaCanonical.MANDAMENTAL) {
            docs.add("prova_pre_constituida");
            docs.add("identificacao_da_autoridade_coatora");
        }
        if (natureza == NaturezaJuridicaCanonical.EXECUTIVA || rito != null && rito.isExecucaoFiscalEstrita()) {
            docs.add("titulo_executivo");
            docs.add("demonstrativo_atualizado_do_debito");
        }
        if (rito != null && rito.isExecucaoFiscalEstrita()) {
            docs.add("certidao_de_divida_ativa");
        }
        if (ramo == RamoDireito.PREVIDENCIARIO || materia == MateriaJurisdicao.PREVIDENCIARIA) {
            docs.add("cnis");
            docs.add("requerimento_administrativo_previo");
            docs.add("documentacao_medica_ou_laboral");
        }
        if (ramo == RamoDireito.TRABALHISTA || materia == MateriaJurisdicao.TRABALHISTA) {
            docs.add("ctps_ou_documento_equivalente");
            docs.add("contracheques_ou_recibos");
            docs.add("trct_ou_documento_rescisorio");
        }
        if (materia == MateriaJurisdicao.FAMILIA && containsAny(corpus, "ALIMENTOS", "PENSAO ALIMENTICIA")) {
            docs.add("comprovacao_da_necessidade_do_alimentando");
            docs.add("indicacao_da_capacidade_contributiva_do_reu");
        }
        if (materia == MateriaJurisdicao.SUCESSOES) {
            docs.add("certidao_de_obito");
            docs.add("relacao_de_herdeiros");
            docs.add("relacao_patrimonial_basica");
        }
        if (materia == MateriaJurisdicao.CONSUMIDOR) {
            docs.add("comprovantes_da_relacao_de_consumo");
            docs.add("protocolo_de_atendimento_ou_reclamacao");
        }
        if (materia == MateriaJurisdicao.AMBIENTAL || natureza == NaturezaJuridicaCanonical.ESTRUTURAL_COLETIVA) {
            docs.add("laudo_tecnico_ou_relatorio_material");
            docs.add("evidencia_fotografica_ou_documental");
        }
        if (natureza == NaturezaJuridicaCanonical.INVESTIGATIVA) {
            docs.add("elementos_indiciarios_minimos");
            docs.add("delimitacao_do_objeto_investigado");
        }
        return List.copyOf(docs);
    }

    private static List<String> buildRiskFlags(Map<String, Object> source,
                                               String corpus,
                                               RamoDireito ramo,
                                               RitoProcessual rito,
                                               MateriaJurisdicao materia,
                                               NaturezaJuridicaCanonical natureza,
                                               Set<NaturezaJuridicaQualifier> qualifiers) {
        LinkedHashSet<String> flags = new LinkedHashSet<>();
        if (natureza == NaturezaJuridicaCanonical.MANDAMENTAL && !containsAny(corpus, "ATO COATOR", "AUTORIDADE COATORA", "DIREITO LIQUIDO E CERTO")) {
            flags.add("INCONSISTENCIA_PROVA_PRE_CONSTITUIDA_MANDAMENTAL");
        }
        if (natureza == NaturezaJuridicaCanonical.EXECUTIVA && !containsAny(corpus, "TITULO", "SENTENCA", "ACORDO", "CDA", "DEBITO", "PENHORA", "EXECUCAO")) {
            flags.add("INCONSISTENCIA_BASE_EXECUTIVA_INSUFICIENTE");
        }
        if (ramo == RamoDireito.TRIBUTARIO && rito != null && !rito.isExecucaoFiscalEstrita() && containsAny(corpus, "CDA", "DIVIDA ATIVA", "EXECUCAO FISCAL")) {
            flags.add("INCONSISTENCIA_RITO_TRIBUTARIO_EXECUTIVO");
        }
        if (ramo == RamoDireito.PREVIDENCIARIO && !containsAny(corpus, "INSS", "LOAS", "BPC", "APOSENTADOR", "AUXILIO", "BENEFICIO")) {
            flags.add("INCONSISTENCIA_SINAIS_PREVIDENCIARIOS_FRACOS");
        }
        if (qualifiers.contains(NaturezaJuridicaQualifier.URGENTE) && !containsAny(corpus, "RISCO", "PERIGO", "URGENTE", "LIMINAR", "MEDICAMENTO", "LEITO", "UTI", "ALIMENTOS", "LIBERDADE")) {
            flags.add("URGENCIA_SEM_ELEMENTO_FATICO_CLARO");
        }
        if (natureza == NaturezaJuridicaCanonical.JURISDICAO_VOLUNTARIA && containsAny(corpus, "LITIGIO", "CONTESTACAO", "RESISTENCIA", "CONFLITO INTENSO")) {
            flags.add("INCONSISTENCIA_VOLUNTARIA_COM_CONTENCIOSIDADE_ELEVADA");
        }
        if (materia == MateriaJurisdicao.CONSTITUCIONAL && !containsAny(corpus, "ATO COATOR", "NORMA", "CONSTITUICAO", "PRECEITO FUNDAMENTAL", "DIREITO LIQUIDO E CERTO")) {
            flags.add("INCONSISTENCIA_CONSTITUCIONAL_FRACA");
        }
        if (ramo == null || rito == null || materia == null) {
            flags.add("CANONICAL_AXIS_INCOMPLETE");
        }
        return List.copyOf(flags);
    }

    private static double computeConfidence(Map<String, Object> source,
                                            CandidateNature winner,
                                            Set<NaturezaJuridicaQualifier> qualifiers,
                                            List<String> riskFlags,
                                            RamoDireito ramo,
                                            RitoProcessual rito,
                                            MateriaJurisdicao materia,
                                            TipoJustica tipoJustica) {
        double base = Math.min(0.55d + winner.score() * 0.12d, 0.94d);
        if (qualifiers.contains(NaturezaJuridicaQualifier.URGENTE)) {
            base += 0.03d;
        }
        if (ramo != null) {
            base += 0.04d;
        }
        if (rito != null) {
            base += 0.05d;
        }
        if (materia != null && materia != MateriaJurisdicao.MULTIMATERIA) {
            base += 0.05d;
        }
        if (tipoJustica != null) {
            base += 0.03d;
        }
        base -= riskFlags.size() * 0.04d;
        Double routingConfidence = decimal(source.get("__routingConfidence"));
        if (routingConfidence != null && routingConfidence > 0d) {
            base = (base * 0.62d) + (Math.min(routingConfidence, 1d) * 0.38d);
        }
        return clamp(base, 0.41d, 0.97d);
    }

    private static String primaryReason(Map<String, Object> source,
                                        String corpus,
                                        RamoDireito ramo,
                                        RitoProcessual rito,
                                        MateriaJurisdicao materia,
                                        NaturezaJuridicaCanonical natureza) {
        List<String> anchors = new ArrayList<>();
        if (natureza != null) {
            anchors.add(natureza.label());
        }
        if (ramo != null) {
            anchors.add(ramo.name());
        }
        if (rito != null) {
            anchors.add(rito.name());
        }
        if (materia != null) {
            anchors.add(materia.name());
        }
        if (containsAny(corpus, "MANDADO DE SEGURANCA", "HABEAS CORPUS", "LIMINAR", "ATO COATOR")) {
            anchors.add("sinais_mandamentais_ou_urgentes");
        }
        if (containsAny(corpus, "EXECUCAO FISCAL", "CDA", "DIVIDA ATIVA", "CUMPRIMENTO DE SENTENCA")) {
            anchors.add("sinais_executivos");
        }
        if (containsAny(corpus, "INVENTARIO", "ARROLAMENTO", "RETIFICACAO DE REGISTRO", "ALVARA")) {
            anchors.add("sinais_voluntarios_homologatorios");
        }
        if (anchors.isEmpty()) {
            anchors.add(firstNonBlank(text(source.get("classeProcessual")), text(source.get("assunto")), "heuristica_canonica"));
        }
        return String.join(" | ", anchors);
    }

    private static String resolveUncertaintyLevel(double confidence, boolean reviewRequired, boolean fallbackUsed) {
        if (fallbackUsed || confidence < 0.58d) {
            return "ALTA";
        }
        if (reviewRequired || confidence < 0.75d) {
            return "MEDIA";
        }
        return "BAIXA";
    }

    private static String inferProbatoryProfile(String corpus, List<String> recommendedDocuments) {
        if (containsAny(corpus, "LAUDO", "PERICIA", "EXAME", "PRONTUARIO", "DOCUMENTO MEDICO")) {
            return "TECNICO_DOCUMENTAL";
        }
        if (containsAny(corpus, "TESTEMUNHA", "ORELHA", "PROVA TESTEMUNHAL", "TESTEMUNHAL")) {
            return "TESTEMUNHAL";
        }
        if (recommendedDocuments.contains("prova_pre_constituida")) {
            return "PRE_CONSTITUIDA";
        }
        return "DOCUMENTAL";
    }

    private static String inferComplexityBand(Set<NaturezaJuridicaQualifier> qualifiers,
                                              List<String> riskFlags,
                                              MateriaJurisdicao materia) {
        int score = 0;
        if (qualifiers.contains(NaturezaJuridicaQualifier.COLETIVA)) {
            score += 2;
        }
        if (qualifiers.contains(NaturezaJuridicaQualifier.ESTRUTURAL)) {
            score += 3;
        }
        if (qualifiers.contains(NaturezaJuridicaQualifier.URGENTE)) {
            score += 1;
        }
        score += riskFlags.size();
        if (materia != null && Set.of(MateriaJurisdicao.CONSTITUCIONAL, MateriaJurisdicao.AMBIENTAL, MateriaJurisdicao.ELEITORAL, MateriaJurisdicao.MILITAR).contains(materia)) {
            score += 2;
        }
        return score >= 6 ? "ALTA" : score >= 3 ? "MEDIA" : "BAIXA";
    }

    private static void apply(Map<NaturezaJuridicaCanonical, CandidateNature> scores,
                              Set<String> supportingSignals,
                              String corpus,
                              NaturezaJuridicaCanonical natureza,
                              double increment,
                              String... tokens) {
        if (containsAny(corpus, tokens)) {
            increase(scores, natureza, increment);
            supportingSignals.add(natureza.name());
        }
    }

    private static void increase(Map<NaturezaJuridicaCanonical, CandidateNature> scores,
                                 NaturezaJuridicaCanonical natureza,
                                 double increment) {
        CandidateNature current = scores.get(natureza);
        if (current == null) {
            scores.put(natureza, new CandidateNature(natureza, increment, false));
            return;
        }
        scores.put(natureza, new CandidateNature(natureza, current.score() + increment, current.fallback()));
    }

    private static String buildCorpus(Map<String, Object> source) {
        List<String> parts = new ArrayList<>();
        add(parts, text(source.get("classeProcessual")));
        add(parts, text(source.get("classeTpuCodigo")));
        add(parts, text(source.get("assunto")));
        add(parts, text(source.get("objetoProcessual")));
        add(parts, text(source.get("pedidoPrincipal")));
        add(parts, text(source.get("pedidosConsolidados")));
        add(parts, text(source.get("materialProbatorioResumo")));
        add(parts, text(source.get("ramoDireito")));
        add(parts, text(source.get("rito")));
        add(parts, text(source.get("materia")));
        add(parts, text(source.get("tipoJustica")));
        add(parts, text(source.get("tribunal")));
        add(parts, text(source.get("tribunalCodigo")));
        add(parts, text(source.get("__routingActionNature")));
        add(parts, text(source.get("__routingActionFamily")));
        add(parts, text(source.get("__routingTipoJustica")));
        add(parts, text(source.get("__routingRito")));
        add(parts, text(source.get("__routingClasseTpuNome")));
        add(parts, text(source.get("__routingClasseTpuCodigo")));
        add(parts, text(source.get("uf")));
        add(parts, text(source.get("comarca")));
        add(parts, text(source.get("vara")));
        return normalize(String.join(" ", parts));
    }

    private static void add(Collection<String> parts, String value) {
        if (value != null && !value.isBlank()) {
            parts.add(value);
        }
    }

    private static String text(Object value) {
        return value == null ? null : Objects.toString(value, null);
    }

    private static Double decimal(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Double.parseDouble(Objects.toString(value, ""));
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toUpperCase(Locale.ROOT)
                .replace('_', ' ')
                .replace('-', ' ')
                .replace('/', ' ')
                .replaceAll("[^A-Z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return normalized;
    }

    private static String normalizeEnumToken(String value) {
        return normalize(value).replace(' ', '_');
    }

    private static boolean containsAny(String corpus, String... tokens) {
        if (corpus == null || corpus.isBlank() || tokens == null || tokens.length == 0) {
            return false;
        }
        for (String token : tokens) {
            if (token == null || token.isBlank()) {
                continue;
            }
            if (corpus.contains(normalize(token))) {
                return true;
            }
        }
        return false;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String formatScore(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private record CandidateNature(NaturezaJuridicaCanonical natureza, double score, boolean fallback) {
    }

    private record SignalMatrix(List<CandidateNature> candidates, List<String> supportingSignals) {
        Map<String, Object> summary() {
            LinkedHashMap<String, Object> out = new LinkedHashMap<>();
            for (CandidateNature candidate : candidates.stream().limit(6).toList()) {
                out.put(candidate.natureza().name(), candidate.score());
            }
            return Collections.unmodifiableMap(out);
        }
    }
}
