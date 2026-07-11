package com.tcc.pjb.backend.service.procedural;

import com.tcc.pjb.backend.core.compiler.LegalCompilerService;
import com.tcc.pjb.backend.core.procedural.ProceduralForumAllocationReport;
import com.tcc.pjb.backend.core.procedural.ProceduralRoutingReport;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.MateriaJurisdicao;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class AjuizamentoCanonicalContextService {

    private final ProceduralCatalogService proceduralCatalogService;

    public AjuizamentoCanonicalContextService(ProceduralCatalogService proceduralCatalogService) {
        this.proceduralCatalogService = proceduralCatalogService;
    }

    public void consolidate(Processo processo,
                            LegalCompilerService.CompiledProcess compiled,
                            ProceduralRoutingReport routing) {
        if (processo == null) {
            return;
        }
        applyCompiledContext(processo, compiled);
        applyRoutingContext(processo, routing);
        backfillProceduralAxis(processo, routing);
        backfillClasseTpu(processo, routing);
        backfillMateria(processo, routing);
        backfillTerritorialSnapshot(processo, routing);
        backfillAdvisoryIntelligence(processo, routing);
    }

    private void applyCompiledContext(Processo processo, LegalCompilerService.CompiledProcess compiled) {
        if (compiled == null) {
            return;
        }
        if (shouldAdoptTipoJustica(processo.getTipoJustica(), compiled.getTipoJustica())) {
            processo.setTipoJustica(compiled.getTipoJustica());
        }
        if (shouldAdoptRamo(processo.getRamoDireito(), compiled.getRamoDireito())) {
            processo.setRamoDireito(compiled.getRamoDireito());
        }
        if (shouldAdoptRito(processo.getRito(), compiled.getRito())) {
            processo.setRito(compiled.getRito());
        }
        if (shouldAdoptMateria(processo.getMateria(), compiled.getMateria(), processo.getRamoDireito())) {
            processo.setMateria(compiled.getMateria());
        }
        if (processo.getNivelSigilo() == null && compiled.getNivelSigilo() != null) {
            processo.setNivelSigilo(compiled.getNivelSigilo());
        }
        if ((processo.getScoreComplexidade() == null || processo.getScoreComplexidade() <= 0)
                && compiled.getScoreComplexidade() != null && compiled.getScoreComplexidade() > 0) {
            processo.setScoreComplexidade(compiled.getScoreComplexidade());
        }
    }

    private void applyRoutingContext(Processo processo, ProceduralRoutingReport routing) {
        if (routing == null) {
            return;
        }
        TipoJustica tipoJustica = TipoJustica.fromString(routing.tipoJusticaSugerida());
        if (shouldAdoptTipoJustica(processo.getTipoJustica(), tipoJustica)) {
            processo.setTipoJustica(tipoJustica);
        }

        RamoDireito ramo = inferRamo(routing);
        if (shouldAdoptRamo(processo.getRamoDireito(), ramo)) {
            processo.setRamoDireito(ramo);
        }

        RitoProcessual rito = parseRito(routing.ritoSugerido());
        if (shouldAdoptRito(processo.getRito(), rito)) {
            processo.setRito(rito);
        }

        if (routing.riskLevel() != null) {
            processo.setRoutingRiskLevel(routing.riskLevel());
        }
        if (routing.confidence() > 0d) {
            processo.setRoutingConfidence(BigDecimal.valueOf(routing.confidence()));
        }
        processo.setTribunalCodigoRoteado(firstNonBlank(processo.getTribunalCodigoRoteado(), routing.tribunalCodigo()));

        ProceduralForumAllocationReport forumAllocation = routing.forumAllocation();
        if (forumAllocation != null) {
            processo.setClasseTpuCodigo(firstNonBlank(processo.getClasseTpuCodigo(), forumAllocation.classeTpuCodigo()));
            processo.setUnidadeJudiciariaCodigo(firstNonBlank(processo.getUnidadeJudiciariaCodigo(), forumAllocation.unidadeJudiciariaCodigo()));
            processo.setCompetenciaTerritorialModo(firstNonBlank(processo.getCompetenciaTerritorialModo(), forumAllocation.competenciaTerritorialModo()));
            processo.setPreventionMode(firstNonBlank(processo.getPreventionMode(), forumAllocation.preventionMode()));
            processo.setLinkageMode(firstNonBlank(processo.getLinkageMode(), forumAllocation.linkageMode()));
            processo.setConnectorSystem(firstNonBlank(processo.getConnectorSystem(), forumAllocation.connectorSystem()));
            processo.setPreProtocoloStatus(firstNonBlank(processo.getPreProtocoloStatus(), forumAllocation.preProtocoloStatus()));
        }
    }

    private void backfillProceduralAxis(Processo processo, ProceduralRoutingReport routing) {
        if (processo == null) {
            return;
        }
        String corpus = canonicalCorpus(processo, routing, processo.getRito());
        TipoJustica tipoJustica = inferTipoJusticaFromCorpus(processo, routing, corpus);
        if (shouldAdoptTipoJustica(processo.getTipoJustica(), tipoJustica)) {
            processo.setTipoJustica(tipoJustica);
        }

        RamoDireito ramo = inferRamo(processo, routing, corpus);
        if (shouldAdoptRamo(processo.getRamoDireito(), ramo)) {
            processo.setRamoDireito(ramo);
        }

        RitoProcessual rito = inferSpecificRito(processo, routing, corpus);
        if (shouldAdoptRito(processo.getRito(), rito)) {
            processo.setRito(rito);
        }
        alignRamoWithRito(processo);

        if (processo.getRamoDireito() == null && processo.getRito() != null) {
            processo.setRamoDireito(processo.getRito().suggestedRamo());
        }
    }

    private void backfillClasseTpu(Processo processo, ProceduralRoutingReport routing) {
        if (processo == null) {
            return;
        }
        Map<String, Object> canonicalContext = canonicalContext(routing);
        String classeCodigo = firstNonBlank(
                processo.getClasseTpuCodigo(),
                routing != null && routing.forumAllocation() != null ? routing.forumAllocation().classeTpuCodigo() : null,
                text(canonicalContext.get("classeTpuCodigo"))
        );
        String classeNome = firstNonBlank(
                processo.getClasseProcessual(),
                routing != null && routing.forumAllocation() != null ? routing.forumAllocation().classeTpuNome() : null,
                text(canonicalContext.get("classeTpuNome"))
        );

        if (classeCodigo == null) {
            Optional<com.tcc.pjb.backend.core.catalog.TpuClasseCnj> resolved = proceduralCatalogService.resolveClasseTpu(
                    firstNonBlank(classeNome, processo.getAssunto(), processo.getPedidoPrincipal()),
                    processo.getRito()
            );
            if (resolved.isPresent()) {
                classeCodigo = String.valueOf(resolved.get().codigoTpu());
                classeNome = firstNonBlank(classeNome, resolved.get().descricao());
            }
        }

        if (processo.getClasseTpuCodigo() == null && classeCodigo != null) {
            processo.setClasseTpuCodigo(classeCodigo);
        }
        if ((processo.getClasseProcessual() == null || processo.getClasseProcessual().isBlank()) && classeNome != null) {
            processo.setClasseProcessual(classeNome);
        }
    }

    private void backfillMateria(Processo processo, ProceduralRoutingReport routing) {
        MateriaJurisdicao inferred = inferMateria(processo, routing);
        if (shouldAdoptMateria(processo.getMateria(), inferred, processo.getRamoDireito())) {
            processo.setMateria(inferred);
        }
        if (processo.getNivelSigilo() == null && processo.getRito() != null && processo.getRito().requiresSegredoByDefault()) {
            processo.setNivelSigilo(NivelSigilo.SEGREDO_JUSTICA);
        }
    }

    private void backfillTerritorialSnapshot(Processo processo, ProceduralRoutingReport routing) {
        if (processo == null || routing == null) {
            return;
        }
        ProceduralForumAllocationReport forumAllocation = routing.forumAllocation();
        processo.setUf(firstNonBlank(processo.getUf(), forumAllocation != null ? forumAllocation.ufSugerida() : null, routing.ufSugerida()));
        processo.setComarca(firstNonBlank(processo.getComarca(), forumAllocation != null ? forumAllocation.comarcaSugerida() : null, routing.cidadeSugerida()));
        processo.setVara(firstNonBlank(processo.getVara(), forumAllocation != null ? forumAllocation.varaSugerida() : null, routing.varaSugerida()));
        processo.setTribunal(firstNonBlank(processo.getTribunal(), forumAllocation != null ? forumAllocation.tribunalNome() : null, routing.tribunalNome(), routing.tribunalCodigo()));
    }

    private void backfillAdvisoryIntelligence(Processo processo, ProceduralRoutingReport routing) {
        if (processo == null) {
            return;
        }
        var advisory = com.tcc.pjb.backend.core.procedural.ProceduralIntelligenceAdvisor.analyzeProcess(processo, routing);
        if (advisory == null) {
            return;
        }
        if (shouldAdoptTipoJustica(processo.getTipoJustica(), advisory.suggestedTipoJustica())) {
            processo.setTipoJustica(advisory.suggestedTipoJustica());
        }
        if (shouldAdoptRamo(processo.getRamoDireito(), advisory.suggestedRamo())) {
            processo.setRamoDireito(advisory.suggestedRamo());
        }
        if (shouldAdoptRito(processo.getRito(), advisory.suggestedRito())) {
            processo.setRito(advisory.suggestedRito());
        }
        alignRamoWithRito(processo);
        if (shouldAdoptMateria(processo.getMateria(), advisory.suggestedMateria(), processo.getRamoDireito())) {
            processo.setMateria(advisory.suggestedMateria());
        }
        if (processo.getNivelSigilo() == null && advisory.suggestedSigilo() != null) {
            processo.setNivelSigilo(advisory.suggestedSigilo());
        }
        if ((processo.getRoutingConfidence() == null || processo.getRoutingConfidence().signum() <= 0) && advisory.confidence() > 0d) {
            processo.setRoutingConfidence(BigDecimal.valueOf(advisory.confidence()));
        }
    }

    private RamoDireito inferRamo(ProceduralRoutingReport routing) {
        return inferRamo(null, routing, null);
    }

    private RamoDireito inferRamo(Processo processo, ProceduralRoutingReport routing, String corpus) {
        if (routing != null) {
            Map<String, Object> canonicalContext = canonicalContext(routing);
            RamoDireito fromCanonical = RamoDireito.fromString(text(canonicalContext.get("ramoDireito")));
            if (fromCanonical != null) {
                return fromCanonical;
            }
            RamoDireito fromFamily = RamoDireito.fromString(routing.actionFamily());
            if (fromFamily != null) {
                return fromFamily;
            }
            RitoProcessual ritoSugerido = parseRito(routing.ritoSugerido());
            if (ritoSugerido != null && ritoSugerido != RitoProcessual.COMUM_ORDINARIO) {
                return ritoSugerido.suggestedRamo();
            }
        }
        String normalized = corpus != null ? corpus : canonicalCorpus(processo, routing, processo != null ? processo.getRito() : null);
        if (containsAny(normalized, "ELEITOR", "TSE", "TRE", "ZONA ELEITORAL", "CANDIDAT", "PARTIDO POLITIC", "CAPTACAO ILICITA SUFRAGIO", "AIRC", "AIJE", "AIME", "RCED")) {
            return RamoDireito.ELEITORAL;
        }
        if (containsAny(normalized, "IPM", "CPPM", "CRIME MILITAR", "JUSTICA MILITAR", "AUDITORIA MILITAR", "AUTORIDADE MILITAR", "CONSELHO DE JUSTICA", "TRANSGRESSAO DISCIPLINAR MILITAR", "POLICIA MILITAR", "BOMBEIRO MILITAR", "EXERCITO", "MARINHA", "AERONAUTICA")) {
            return RamoDireito.MILITAR;
        }
        if (hasFamilyCore(normalized)) {
            return RamoDireito.FAMILIA;
        }
        if (hasInfanciaCore(normalized)) {
            return RamoDireito.INFANCIA_JUVENTUDE;
        }
        if (containsAny(normalized, "ACAO DE CUMPRIMENTO", "ART 872 CLT", "DESCUMPRIMENTO DE CONVENCAO COLETIVA", "DESCUMPRIMENTO DE ACORDO COLETIVO", "INSTRUMENTO COLETIVO DESCUMPRIDO", "CLT", "RECLAMACAO TRABALHISTA", "VERBAS RESCISORIAS", "HORAS EXTRAS", "FGTS", "ADICIONAL INSALUBRIDADE", "ADICIONAL PERICULOSIDADE", "VINCULO EMPREGATICIO", "ACIDENTE DO TRABALHO", "DOENCA OCUPACIONAL", "DISSIDIO COLETIVO", "CONVENCAO COLETIVA", "ACORDO COLETIVO", "MPT", "MINISTERIO PUBLICO DO TRABALHO")) {
            return RamoDireito.TRABALHISTA;
        }
        if (containsAny(normalized, "INSS", "LOAS", "BPC", "APOSENTADOR", "AUXILIO", "PENSAO POR MORTE", "SALARIO MATERNIDADE", "BENEFICIO PREVIDENCIARIO", "RPPS")) {
            return RamoDireito.PREVIDENCIARIO;
        }
        if (hasExecucaoFiscalCore(normalized) || containsAny(normalized, "TRIBUTO", "TRIBUTAR", "ICMS", "ISS", "IPTU", "IPVA", "ITCMD", "ITBI", "FAZENDA PUBLICA", "ANULATORIA DE DEBITO", "REPETICAO DE INDEBITO")) {
            return RamoDireito.TRIBUTARIO;
        }
        if (containsAny(normalized, "CONSUMIDOR", "CDC", "NEGATIVACAO", "SERASA", "SPC", "COBRANCA INDEVIDA", "BANCO", "FINANCIAMENTO", "CARTAO", "PRODUTO", "SERVICO")) {
            return RamoDireito.CONSUMIDOR;
        }
        if (containsAny(normalized, "CRIME", "DENUNCIA", "QUEIXA CRIME", "TRIBUNAL DO JURI", "MARIA DA PENHA", "LEI DE DROGAS", "LAVAGEM DE DINHEIRO", "ORGANIZACAO CRIMINOSA")) {
            return RamoDireito.PENAL;
        }
        if (containsAny(normalized, "IMPROBIDADE", "PROCESSO ADMINISTRATIVO DISCIPLINAR", "PAD", "CONCURSO PUBLICO", "SERVIDOR PUBLICO", "LICITACAO", "ATO ADMINISTRATIVO")) {
            return RamoDireito.ADMINISTRATIVO;
        }
        if (containsAny(normalized, "DESMAT", "LICENCIAMENTO", "IBAMA", "AMBIENT")) {
            return RamoDireito.AMBIENTAL;
        }
        if (containsAny(normalized, "REFORMA AGRARIA", "IMOVEL RURAL", "USUCAPIAO RURAL", "ASSENTAMENTO", "POSSE RURAL")) {
            return RamoDireito.AGRARIO;
        }
        if (containsAny(normalized, "RECUPERACAO JUDICIAL", "RECUPERACAO EXTRAJUDICIAL", "FALENCIA", "SOCIEDADE EMPRESARIA", "CONTRATO SOCIAL", "DESCONSIDERACAO DA PERSONALIDADE JURIDICA")) {
            return RamoDireito.EMPRESARIAL;
        }
        if (containsAny(normalized, "ADI", "ADC", "ADPF", "MANDADO DE INJUNCAO", "HABEAS DATA", "CONTROLE CONCENTRADO")) {
            return RamoDireito.CONSTITUCIONAL;
        }
        if (processo != null && processo.getRito() != null) {
            return processo.getRito().suggestedRamo();
        }
        return null;
    }

    private MateriaJurisdicao inferMateria(Processo processo, ProceduralRoutingReport routing) {
        if (processo == null) {
            return null;
        }
        RitoProcessual rito = processo.getRito() != null ? processo.getRito() : parseRito(routing != null ? routing.ritoSugerido() : null);
        String corpus = canonicalCorpus(processo, routing, rito);
        RamoDireito ramo = processo.getRamoDireito() != null ? processo.getRamoDireito() : inferRamo(processo, routing, corpus);

        if ((rito != null && rito.isEleitoral()) || containsAny(corpus, "ELEITOR", "TSE", "TRE", "ZONA ELEITORAL", "CANDIDAT", "PARTIDO POLITIC", "PROPAGANDA ELEITORAL", "DIREITO DE RESPOSTA", "AIJE", "AIME", "AIRC", "RCED", "INELEGIBILIDADE", "CAPTACAO ILICITA SUFRAGIO", "PRESTACAO DE CONTAS ELEITORAL")) {
            return MateriaJurisdicao.ELEITORAL;
        }
        if ((rito != null && rito.isMilitar()) || containsAny(corpus, "IPM", "CPPM", "CRIME MILITAR", "JUSTICA MILITAR", "AUDITORIA MILITAR", "AUTORIDADE MILITAR", "CONSELHO DE JUSTICA", "TRANSGRESSAO DISCIPLINAR MILITAR", "PAD MILITAR", "POLICIA MILITAR", "BOMBEIRO MILITAR", "EXERCITO", "MARINHA", "AERONAUTICA")) {
            if (containsAny(corpus, "EXECUCAO PENAL MILITAR", "PROGRESSAO DE REGIME", "LIVRAMENTO CONDICIONAL", "REMICAO")) {
                return MateriaJurisdicao.EXECUCAO_PENAL;
            }
            return MateriaJurisdicao.MILITAR;
        }
        if ((rito != null && rito.isTrabalhista()) || containsAny(corpus, "CLT", "RECLAMACAO TRABALHISTA", "VERBAS RESCISORIAS", "HORAS EXTRAS", "FGTS", "ADICIONAL INSALUBRIDADE", "ADICIONAL PERICULOSIDADE", "VINCULO EMPREGATICIO", "ACIDENTE DO TRABALHO", "DOENCA OCUPACIONAL", "DISSIDIO COLETIVO", "CONVENCAO COLETIVA", "ACORDO COLETIVO", "MPT", "MINISTERIO PUBLICO DO TRABALHO")) {
            return MateriaJurisdicao.TRABALHISTA;
        }
        if ((rito != null && rito.isPenal()) || containsAny(corpus, "CRIME", "DENUNCIA", "QUEIXA CRIME", "TRIBUNAL DO JURI", "JURI", "LEI DE DROGAS", "MARIA DA PENHA", "LAVAGEM DE DINHEIRO", "ORGANIZACAO CRIMINOSA", "CRIMES CIBERNETICOS", "RACISMO", "TORTURA", "TERRORISMO", "HOMICIDIO", "ROUBO", "FURTO")) {
            return MateriaJurisdicao.PENAL;
        }
        if (containsAny(corpus, "IMPROBIDADE", "PROCESSO ADMINISTRATIVO DISCIPLINAR", "PAD", "CONCURSO PUBLICO", "SERVIDOR PUBLICO", "LICITACAO", "ATO ADMINISTRATIVO", "AGENTE PUBLICO")) {
            return MateriaJurisdicao.ADMINISTRATIVO;
        }
        if (hasExecucaoFiscalCore(corpus)) {
            return MateriaJurisdicao.EXECUCAO_FISCAL;
        }
        if (containsAny(corpus, "TRIBUTO", "TRIBUTAR", "ICMS", "ISS", "IPTU", "IPVA", "ITCMD", "ITBI", "CONTRIBUINTE", "FAZENDA PUBLICA", "REPETICAO DE INDEBITO", "ANULATORIA DE DEBITO")) {
            return MateriaJurisdicao.TRIBUTARIA;
        }
        if (containsAny(corpus, "INVENTAR", "ARROLAMENTO", "SUCESS", "HERANCA", "PARTILHA CAUSA MORTIS")) {
            return MateriaJurisdicao.SUCESSOES;
        }
        if (hasFamilyCore(corpus)) {
            return MateriaJurisdicao.FAMILIA;
        }
        if (hasInfanciaCore(corpus)) {
            return MateriaJurisdicao.INFANCIA_JUVENTUDE;
        }
        if (containsAny(corpus, "MEDICAMENTO", "LEITO", "CIRURG", "UTI", "TRATAMENTO", "TERAPIA", "HOME CARE", "PLANO DE SAUDE", "SUS")) {
            return MateriaJurisdicao.SAUDE;
        }
        if (containsAny(corpus, "MATRICULA", "ESCOLA", "UNIVERSIDADE", "CRECHE", "ENSINO", "EDUCACAO", "TRANSPORTE ESCOLAR")) {
            return MateriaJurisdicao.EDUCACAO;
        }
        if (containsAny(corpus, "CONSUMIDOR", "CDC", "NEGATIVACAO", "SERASA", "SPC", "COBRANCA INDEVIDA", "TELEFON", "INTERNET", "BANCO", "FINANCIAMENTO", "CARTAO", "PRODUTO", "SERVICO")) {
            return MateriaJurisdicao.CONSUMIDOR;
        }
        if (containsAny(corpus, "FALENCIA", "RECUPERACAO JUDICIAL", "RECUPERACAO EXTRAJUDICIAL")) {
            return MateriaJurisdicao.FALENCIAS;
        }
        if ((rito != null && rito.isEmpresarial()) || containsAny(corpus, "SOCIEDADE EMPRESARIA", "EMPRESA", "CONTRATO SOCIAL", "ASSEMBLEIA DE SOCIOS", "DISSOLUCAO SOCIETARIA", "DESCONSIDERACAO DA PERSONALIDADE JURIDICA")) {
            return MateriaJurisdicao.EMPRESARIAL;
        }
        if (containsAny(corpus, "REGISTRO CIVIL", "REGISTRO IMOBILIARIO", "RETIFICACAO DE REGISTRO", "AVERBACAO", "CARTORIO")) {
            return MateriaJurisdicao.REGISTROS_PUBLICOS;
        }
        if (rito != null && rito.isExecucaoFiscalEstrita() || containsAny(corpus, "EXECUCAO FISCAL", "DIVIDA ATIVA", "CDA", "LEF", "IPTU", "IPVA", "ICMS", "ISS")) {
            return MateriaJurisdicao.EXECUCAO_FISCAL;
        }
        if (containsAny(corpus, "PREVID", "INSS", "LOAS", "BPC", "APOSENTADOR", "AUXILIO", "PENSAO POR MORTE", "SALARIO MATERNIDADE", "BENEFICIO")) {
            return MateriaJurisdicao.PREVIDENCIARIA;
        }
        if (containsAny(corpus, "EXECUCAO PENAL", "LEP", "REMICAO", "PROGRESSAO DE REGIME")) {
            return MateriaJurisdicao.EXECUCAO_PENAL;
        }
        if (containsAny(corpus, "AMBIENT", "IBAMA", "DESMAT", "LICENCIAMENTO")) {
            return MateriaJurisdicao.AMBIENTAL;
        }
        if (containsAny(corpus, "AGRARIO", "IMOVEL RURAL", "USUCAPIAO RURAL", "ASSENTAMENTO", "POSSE RURAL")) {
            return MateriaJurisdicao.AGRARIO;
        }
        if (containsAny(corpus, "URBANISMO", "ZONEAMENTO", "PARCELAMENTO DO SOLO", "USUCAPIAO URBANA", "OBRA NOVA")) {
            return MateriaJurisdicao.URBANISMO;
        }
        if (containsAny(corpus, "MANDADO DE SEGURANCA", "MANDADO DE INJUNCAO", "ADI", "ADC", "ADPF", "HABEAS DATA")) {
            return MateriaJurisdicao.CONSTITUCIONAL;
        }
        if (ramo != null) {
            return MateriaJurisdicao.fromRamo(ramo);
        }
        return MateriaJurisdicao.MULTIMATERIA;
    }

    private String canonicalCorpus(Processo processo, ProceduralRoutingReport routing, RitoProcessual rito) {
        return normalize(String.join(" ", collectText(
                processo != null ? processo.getClasseProcessual() : null,
                processo != null ? processo.getClasseTpuCodigo() : null,
                processo != null ? processo.getAssunto() : null,
                processo != null ? processo.getObjetoProcessual() : null,
                processo != null ? processo.getPedidoPrincipal() : null,
                processo != null ? processo.getPedidosConsolidados() : null,
                processo != null ? processo.getMaterialProbatorioResumo() : null,
                routing != null ? routing.actionNature() : null,
                routing != null ? routing.actionFamily() : null,
                routing != null ? routing.tribunalNome() : null,
                routing != null ? routing.tribunalCodigo() : null,
                rito != null ? rito.name() : null,
                processo != null && processo.getRamoDireito() != null ? processo.getRamoDireito().name() : null,
                processo != null && processo.getTipoJustica() != null ? processo.getTipoJustica().name() : null
        )));
    }

    private TipoJustica inferTipoJusticaFromCorpus(Processo processo, ProceduralRoutingReport routing, String corpus) {
        if (containsAny(corpus, "ELEITOR", "TSE", "TRE", "ZONA ELEITORAL", "CANDIDAT", "PARTIDO POLITIC", "AIJE", "AIME", "AIRC", "RCED", "INELEGIBILIDADE")) {
            return TipoJustica.ELEITORAL;
        }
        if (containsAny(corpus, "CLT", "RECLAMACAO TRABALHISTA", "VERBAS RESCISORIAS", "HORAS EXTRAS", "FGTS", "DISSIDIO COLETIVO", "CONVENCAO COLETIVA", "ACORDO COLETIVO", "VARA DO TRABALHO", "TRT", "TST")) {
            return TipoJustica.TRABALHO;
        }
        if (containsAny(corpus, "IPM", "CPPM", "CRIME MILITAR", "JUSTICA MILITAR", "AUDITORIA MILITAR", "STM", "TJM", "AUTORIDADE MILITAR", "CONSELHO DE JUSTICA")) {
            if (containsAny(corpus, "EXERCITO", "MARINHA", "AERONAUTICA", "FORCAS ARMADAS", "UNIAO", "STM")) {
                return TipoJustica.MILITAR_FEDERAL;
            }
            if (containsAny(corpus, "POLICIA MILITAR", "BOMBEIRO MILITAR", "TJM", "CORPO DE BOMBEIROS")) {
                return TipoJustica.MILITAR_ESTADUAL;
            }
            if (processo != null && processo.getTipoJustica() != null && processo.getTipoJustica().name().startsWith("MILITAR")) {
                return processo.getTipoJustica();
            }
            if (routing != null) {
                TipoJustica fromRouting = TipoJustica.fromString(routing.tipoJusticaSugerida());
                if (fromRouting != null && fromRouting.name().startsWith("MILITAR")) {
                    return fromRouting;
                }
            }
            return TipoJustica.MILITAR_ESTADUAL;
        }
        if (containsAny(corpus, "JUSTICA FEDERAL", "TRF", "JEF", "SUBSECAO JUDICIARIA", "SECAO JUDICIARIA", "UNIAO", "AUTARQUIA FEDERAL", "INSS", "IBAMA", "ICMBIO", "ANVISA", "INCRA", "RECEITA FEDERAL", "CAIXA ECONOMICA FEDERAL", "UNIVERSIDADE FEDERAL", "POLICIA FEDERAL")) {
            return TipoJustica.FEDERAL;
        }
        if (containsAny(corpus, "HOMOLOGACAO DE SENTENCA ESTRANGEIRA", "CARTA ROGATORIA", "COOPERACAO JURIDICA INTERNACIONAL", "STJ", "STF", "ADI", "ADC", "ADPF", "CONTROLE CONCENTRADO")) {
            return TipoJustica.SUPERIOR;
        }
        return null;
    }

    private RitoProcessual inferSpecificRito(Processo processo, ProceduralRoutingReport routing, String corpus) {
        if (containsAny(corpus, "HOMOLOGACAO DE SENTENCA ESTRANGEIRA", "SENTENCA ESTRANGEIRA HOMOLOGAR")) {
            return RitoProcessual.HOMOLOGACAO_SENTENCA_ESTRANGEIRA;
        }
        if (containsAny(corpus, "CARTA ROGATORIA")) {
            return RitoProcessual.CARTA_ROGATORIA;
        }
        if (containsAny(corpus, "COOPERACAO JURIDICA INTERNACIONAL", "AUXILIO DIRETO INTERNACIONAL", "MLAT", "CARTA DE ORDEM INTERNACIONAL")) {
            return RitoProcessual.COOPERACAO_JURIDICA_INTERNACIONAL;
        }
        if (containsAny(corpus, "DIREITO DE RESPOSTA")) {
            return RitoProcessual.ELEITORAL_DIREITO_RESPOSTA;
        }
        if (containsAny(corpus, "PROPAGANDA ELEITORAL", "PROPAGANDA IRREGULAR")) {
            return RitoProcessual.ELEITORAL_PROPAGANDA;
        }
        if (containsAny(corpus, "CAPTACAO ILICITA SUFRAGIO", "COMPRA DE VOTOS", "ART 41 A")) {
            return RitoProcessual.ELEITORAL_CAPTACAO_ILICITA_SUFRAGIO;
        }
        if (containsAny(corpus, "AIJE", "ABUSO DE PODER", "CONDUTA VEDADA")) {
            return RitoProcessual.ELEITORAL_AIJE;
        }
        if (containsAny(corpus, "AIME", "MANDATO ELETIVO")) {
            return RitoProcessual.ELEITORAL_AIME;
        }
        if (containsAny(corpus, "AIRC", "IMPUGNACAO DE REGISTRO", "IMPUGNACAO AO REGISTRO")) {
            return RitoProcessual.ELEITORAL_AIRC;
        }
        if (containsAny(corpus, "RCED", "RECURSO CONTRA EXPEDICAO DO DIPLOMA")) {
            return RitoProcessual.ELEITORAL_RCED;
        }
        if (containsAny(corpus, "PRESTACAO DE CONTAS", "CONTAS ELEITORAIS", "ARRECADACAO DE CAMPANHA")) {
            return RitoProcessual.ELEITORAL_PRESTACAO_CONTAS;
        }
        if (containsAny(corpus, "REGISTRO DE CANDIDATURA", "DRAP", "DEMONSTRATIVO DE REGULARIDADE DE ATOS PARTIDARIOS")) {
            return RitoProcessual.ELEITORAL_REGISTRO_CANDIDATURA;
        }
        if (containsAny(corpus, "INELEGIBILIDADE", "LC 64")) {
            return RitoProcessual.ELEITORAL_INELEGIBILIDADE;
        }
        if (containsAny(corpus, "ATO INFRACIONAL", "MEDIDA SOCIOEDUCATIVA", "ADOLESCENTE EM CONFLITO COM A LEI")) {
            return RitoProcessual.INFANCIA_JUVENTUDE_INFRACIONAL;
        }
        if (containsAny(corpus, "DISSIDIO COLETIVO", "DISSÍDIO COLETIVO", "GREVE", "CATEGORIA PROFISSIONAL")) {
            return RitoProcessual.TRABALHISTA_DISSIDIO_COLETIVO;
        }
        if (containsAny(corpus, "INQUERITO POLICIAL MILITAR", "IPM", "ENCARREGADO DO IPM")) {
            return RitoProcessual.MILITAR_IPM;
        }
        if (containsAny(corpus, "PAD MILITAR", "PROCESSO ADMINISTRATIVO DISCIPLINAR MILITAR", "TRANSGRESSAO DISCIPLINAR MILITAR", "COMISSAO DISCIPLINAR MILITAR")) {
            return RitoProcessual.MILITAR_PAD;
        }
        if (containsAny(corpus, "HABEAS CORPUS") && containsAny(corpus, "MILITAR", "CPPM", "AUDITORIA MILITAR", "STM", "TJM")) {
            return RitoProcessual.MILITAR_HABEAS_CORPUS_MILITAR;
        }
        if (containsAny(corpus, "CONSELHO DE JUSTICA")) {
            return RitoProcessual.MILITAR_CONSELHO_JUSTICA;
        }
        if (containsAny(corpus, "CRIME MILITAR", "JUSTICA MILITAR", "AUDITORIA MILITAR", "CPPM")) {
            return RitoProcessual.MILITAR_PROCESSO_PENAL_MILITAR;
        }
        if (containsAny(corpus, "ACAO DE CUMPRIMENTO", "AÇÃO DE CUMPRIMENTO", "ART 872 CLT", "ART. 872 CLT", "DESCUMPRIMENTO DE CONVENCAO COLETIVA", "DESCUMPRIMENTO DE ACORDO COLETIVO", "INSTRUMENTO COLETIVO DESCUMPRIDO")) {
            return RitoProcessual.TRABALHISTA_ACAO_CUMPRIMENTO;
        }
        if (containsAny(corpus, "DISSIDIO COLETIVO", "GREVE", "CATEGORIA PROFISSIONAL") || containsAny(corpus, "CONVENCAO COLETIVA", "ACORDO COLETIVO") && !containsAny(corpus, "DESCUMPRIMENTO", "ACAO DE CUMPRIMENTO", "ART 872 CLT", "ART. 872 CLT")) {
            return RitoProcessual.TRABALHISTA_DISSIDIO_COLETIVO;
        }
        if (containsAny(corpus, "INQUERITO JUDICIAL", "FALTA GRAVE", "ART 853 CLT", "ART. 853 CLT", "EMPREGADO ESTAVEL", "EMPREGADO ESTÁVEL")) {
            return RitoProcessual.TRABALHISTA_INQUERITO_FALTA_GRAVE;
        }
        if (containsAny(corpus, "MANDADO DE SEGURANCA") && containsAny(corpus, "TRABALH", "CLT", "TRT", "TST", "VARA DO TRABALHO")) {
            return RitoProcessual.TRABALHISTA_MANDADO_SEGURANCA;
        }
        if (containsAny(corpus, "ACAO RESCISORIA") && containsAny(corpus, "TRABALH", "CLT", "TRT", "TST")) {
            return RitoProcessual.TRABALHISTA_ACAO_RESCISORIA;
        }
        if (containsAny(corpus, "EXECUCAO TRABALHISTA", "CUMPRIMENTO DE SENTENCA TRABALHISTA", "LIQUIDACAO TRABALHISTA", "ART 876 CLT", "CUMPRIMENTO DE ACORDO TRABALHISTA")) {
            return containsAny(corpus, "ACORDO TRABALHISTA") ? RitoProcessual.TRABALHISTA_CUMPRIMENTO_SENTENCA : RitoProcessual.TRABALHISTA_EXECUCAO;
        }
        if (containsAny(corpus, "TUTELA CAUTELAR") && containsAny(corpus, "TRABALH", "CLT", "TRT", "TST")) {
            return RitoProcessual.TRABALHISTA_TUTELA_CAUTELAR;
        }
        if (containsAny(corpus, "ACIDENTE DO TRABALHO", "DOENCA OCUPACIONAL", "CAT")) {
            return RitoProcessual.TRABALHISTA_ACIDENTE_TRABALHO;
        }
        if (containsAny(corpus, "RECLAMACAO TRABALHISTA", "CLT", "FGTS", "HORAS EXTRAS", "VERBAS RESCISORIAS", "VINCULO EMPREGATICIO")) {
            if (containsAny(corpus, "SUMARIO", "SUMÁRIO", "ALCADA", "ALÇADA", "LEI 5584", "LEI 5 584", "LEI 5.584")) {
                return RitoProcessual.TRABALHISTA_SUMARIO_ALCADA;
            }
            if (containsAny(corpus, "SUMARISSIMO", "RITO SUMARISSIMO")) {
                return RitoProcessual.TRABALHISTA_SUMARISSIMO;
            }
            return RitoProcessual.TRABALHISTA_ORDINARIO;
        }
        if (containsAny(corpus, "ALIMENTOS", "PENSAO ALIMENTICIA", "ALIMENTANDO")) {
            return RitoProcessual.CIVIL_FAMILIA_ALIMENTOS;
        }
        if (containsAny(corpus, "DIVORCIO", "DISSOLUCAO DE CASAMENTO")) {
            return RitoProcessual.CIVIL_FAMILIA_DIVORCIO;
        }
        if (containsAny(corpus, "INVENTARIO", "ARROLAMENTO", "PARTILHA", "HERANCA")) {
            return RitoProcessual.CIVIL_INVENTARIO_ARROLAMENTO;
        }
        if (containsAny(corpus, "USUCAPIAO")) {
            return containsAny(corpus, "RURAL", "IMOVEL RURAL", "PRO LABORE") ? RitoProcessual.AGRARIO_USUCAPIAO_RURAL : RitoProcessual.CIVIL_USUCAPIAO;
        }
        if (containsAny(corpus, "REINTEGRACAO DE POSSE", "MANUTENCAO DE POSSE", "INTERDITO PROIBITORIO", "ESBULHO", "TURBACAO POSSESSORIA")) {
            if (containsAny(corpus, "INTERDITO PROIBITORIO")) {
                return RitoProcessual.CIVIL_INTERDITO_PROIBITORIO;
            }
            return RitoProcessual.CIVIL_POSSESSORIA;
        }
        if (containsAny(corpus, "ACAO MONITORIA", "MONITORIA")) {
            return RitoProcessual.CIVIL_ACAO_MONITORIA;
        }
        if (containsAny(corpus, "CONSIGNACAO EM PAGAMENTO", "CONSIGNACAO DE PAGAMENTO")) {
            return RitoProcessual.CIVIL_CONSIGNACAO_PAGAMENTO;
        }
        if (containsAny(corpus, "RETIFICACAO DE REGISTRO", "REGISTRO PUBLICO", "AVERBACAO", "CARTORIO")) {
            return RitoProcessual.CIVIL_RETIFICACAO_REGISTRO;
        }
        if (containsAny(corpus, "NUNCIACAO DE OBRA NOVA", "OBRA NOVA")) {
            return RitoProcessual.CIVIL_NUNCIACAO_OBRA_NOVA;
        }
        if (containsAny(corpus, "ADOCAO") && containsAny(corpus, "INFANC", "JUVENTUDE", "ECA", "MENOR")) {
            return RitoProcessual.INFANCIA_JUVENTUDE_ADOCAO;
        }
        if (containsAny(corpus, "ATO INFRACIONAL", "MEDIDA SOCIOEDUCATIVA", "ADOLESCENTE EM CONFLITO COM A LEI")) {
            return RitoProcessual.INFANCIA_JUVENTUDE_INFRACIONAL;
        }
        if (containsAny(corpus, "ECA", "CRIANCA", "ADOLESCENTE") && containsAny(corpus, "GUARDA", "PROTECAO", "MEDIDA PROTETIVA")) {
            return RitoProcessual.INFANCIA_JUVENTUDE_ECA;
        }
        if (containsAny(corpus, "EMBARGOS A EXECUCAO FISCAL", "EMBARGOS EXECUCAO FISCAL")) {
            return RitoProcessual.TRIBUTARIO_EMBARGOS_EXECUCAO_FISCAL;
        }
        if (hasExecucaoFiscalCore(corpus)) {
            return RitoProcessual.EXECUCAO_FISCAL;
        }
        if (containsAny(corpus, "MANDADO DE SEGURANCA") && containsAny(corpus, "TRIBUTO", "FAZENDA", "FISCAL", "ICMS", "IPTU", "IPVA", "ISS")) {
            return RitoProcessual.TRIBUTARIO_MANDADO_SEGURANCA;
        }
        if (containsAny(corpus, "REPETICAO DE INDEBITO", "COMPENSACAO TRIBUTARIA")) {
            return RitoProcessual.TRIBUTARIO_REPETICAO_INDEBITO;
        }
        if (containsAny(corpus, "ANULATORIA DE DEBITO", "ANULACAO DE AUTO DE INFRACAO", "CREDITO TRIBUTARIO")) {
            return RitoProcessual.TRIBUTARIO_ANULATORIA_DEBITO;
        }
        if (containsAny(corpus, "DECLARATORIA TRIBUTARIA", "INEXIGIBILIDADE DE TRIBUTO")) {
            return RitoProcessual.TRIBUTARIO_DECLARATORIA;
        }
        if (containsAny(corpus, "CAUTELAR FISCAL", "ARROLAMENTO DE BENS FISCAL")) {
            return RitoProcessual.TRIBUTARIO_CAUTELAR_FISCAL;
        }
        if (containsAny(corpus, "BPC", "LOAS", "BENEFICIO DE PRESTACAO CONTINUADA")) {
            return RitoProcessual.PREVIDENCIARIO_BPC_LOAS;
        }
        if (containsAny(corpus, "AUXILIO POR INCAPACIDADE", "AUXILIO DOENCA", "AUXILIO ACIDENTE", "INCAPACIDADE LABORATIVA")) {
            return RitoProcessual.PREVIDENCIARIO_AUXILIO_INCAPACIDADE;
        }
        if (containsAny(corpus, "APOSENTADORIA ESPECIAL")) {
            return RitoProcessual.PREVIDENCIARIO_ESPECIAL;
        }
        if (containsAny(corpus, "APOSENTADORIA", "TEMPO DE CONTRIBUICAO", "INVALIDEZ PREVIDENCIARIA")) {
            return RitoProcessual.PREVIDENCIARIO_APOSENTADORIA;
        }
        if (containsAny(corpus, "REVISAO DE BENEFICIO", "REVISIONAL PREVIDENCIARIA")) {
            return RitoProcessual.PREVIDENCIARIO_REVISAO_BENEFICIO;
        }
        if (containsAny(corpus, "RESTABELECIMENTO DE BENEFICIO", "CESSACAO INDEVIDA DE BENEFICIO")) {
            return RitoProcessual.PREVIDENCIARIO_RESTABELECIMENTO;
        }
        if (containsAny(corpus, "SALARIO MATERNIDADE")) {
            return RitoProcessual.PREVIDENCIARIO_SALARIO_MATERNIDADE;
        }
        if (containsAny(corpus, "PENSAO POR MORTE")) {
            return RitoProcessual.PREVIDENCIARIO_PENSAO_MORTE;
        }
        if (containsAny(corpus, "SEGURADO ESPECIAL", "TRABALHADOR RURAL", "RURAL PREVIDENCIARIO")) {
            return RitoProcessual.PREVIDENCIARIO_RURAL;
        }
        if (containsAny(corpus, "RPPS", "REGIME PROPRIO DE PREVIDENCIA")) {
            return RitoProcessual.PREVIDENCIARIO_RPPS;
        }
        if (containsAny(corpus, "INSS", "PREVIDENCIAR", "JEF", "JUIZADO ESPECIAL FEDERAL")) {
            return containsAny(corpus, "JEF", "JUIZADO ESPECIAL FEDERAL") ? RitoProcessual.PREVIDENCIARIO_JEF : RitoProcessual.PREVIDENCIARIO_COMUM;
        }
        if (containsAny(corpus, "MANDADO DE SEGURANCA") && containsAny(corpus, "SERVIDOR", "CONCURSO PUBLICO", "ATO ADMINISTRATIVO", "AUTORIDADE COATORA", "PAD")) {
            return RitoProcessual.ESPECIAL_MANDADO_SEGURANCA;
        }
        if (containsAny(corpus, "MANDADO DE SEGURANCA COLETIVO")) {
            return RitoProcessual.ESPECIAL_MANDADO_SEGURANCA_COLETIVO;
        }
        if (containsAny(corpus, "ACAO DE DESCUMPRIMENTO DE OBRIGACAO", "AÇÃO DE DESCUMPRIMENTO DE OBRIGAÇÃO", "DESCUMPRIMENTO DE OBRIGACAO ESPECIFICA", "DESCUMPRIMENTO DE OBRIGAÇÃO ESPECÍFICA")) {
            return RitoProcessual.ESPECIAL_ACAO_DESCUMPRIMENTO_OBRIGACAO;
        }
        if (containsAny(corpus, "HABEAS DATA")) {
            return RitoProcessual.ESPECIAL_HABEAS_DATA;
        }
        if (containsAny(corpus, "MANDADO DE INJUNCAO COLETIVO")) {
            return RitoProcessual.ESPECIAL_MANDADO_INJUNCAO_COLETIVO;
        }
        if (containsAny(corpus, "MANDADO DE INJUNCAO")) {
            return RitoProcessual.ESPECIAL_MANDADO_INJUNCAO;
        }
        if (containsAny(corpus, "ACAO POPULAR")) {
            return RitoProcessual.ESPECIAL_ACAO_POPULAR;
        }
        if (containsAny(corpus, "ADI", "ACAO DIRETA DE INCONSTITUCIONALIDADE")) {
            return RitoProcessual.ESPECIAL_ACAO_DIRETA_INCONSTITUCIONALIDADE;
        }
        if (containsAny(corpus, "ADC", "ACAO DECLARATORIA DE CONSTITUCIONALIDADE")) {
            return RitoProcessual.ESPECIAL_ACAO_DECLARATORIA_CONSTITUCIONALIDADE;
        }
        if (containsAny(corpus, "ADPF", "ARGUICAO DE DESCUMPRIMENTO DE PRECEITO FUNDAMENTAL")) {
            return RitoProcessual.ESPECIAL_ARGUICAO_DESCUMPRIMENTO_PRECEITO_FUNDAMENTAL;
        }
        if (containsAny(corpus, "IMPROBIDADE ADMINISTRATIVA")) {
            return RitoProcessual.IMPROBIDADE_ADMINISTRATIVA;
        }
        if (containsAny(corpus, "PAD", "PROCESSO ADMINISTRATIVO DISCIPLINAR") && !containsAny(corpus, "MILITAR")) {
            return RitoProcessual.ADMINISTRATIVO_PAD;
        }
        if (containsAny(corpus, "CONCURSO PUBLICO", "EDITAL", "NOMEACAO", "POSSE EM CARGO PUBLICO")) {
            return RitoProcessual.ADMINISTRATIVO_CONCURSO_PUBLICO;
        }
        if (containsAny(corpus, "SERVIDOR PUBLICO", "REMUNERACAO DE SERVIDOR", "REENQUADRAMENTO FUNCIONAL")) {
            return RitoProcessual.ADMINISTRATIVO_SERVIDORES;
        }
        if (containsAny(corpus, "TRIBUNAL DO JURI", "JURI", "CRIME DOLOSO CONTRA A VIDA")) {
            return RitoProcessual.TRIBUNAL_JURI;
        }
        if (containsAny(corpus, "HABEAS CORPUS") && containsAny(corpus, "PENAL", "CRIME", "PRISAO", "PACIENTE", "AUTORIDADE COATORA")) {
            return RitoProcessual.ESPECIAL_HABEAS_CORPUS;
        }
        if (containsAny(corpus, "LEI DE DROGAS", "TRAFICO DE DROGAS", "ENTORPECENTE")) {
            return RitoProcessual.PENAL_LEI_DROGAS;
        }
        if (containsAny(corpus, "MARIA DA PENHA", "VIOLENCIA DOMESTICA", "VIOLENCIA FAMILIAR CONTRA A MULHER")) {
            return RitoProcessual.PENAL_MARIA_DA_PENHA;
        }
        if (containsAny(corpus, "LAVAGEM DE DINHEIRO")) {
            return RitoProcessual.PENAL_LAVAGEM_DINHEIRO;
        }
        if (containsAny(corpus, "ORGANIZACAO CRIMINOSA", "COLABORACAO PREMIADA")) {
            return RitoProcessual.PENAL_ORGANIZACAO_CRIMINOSA;
        }
        if (containsAny(corpus, "CRIME CIBERNETICO", "INVASAO DE DISPOSITIVO", "FRAUDE ELETRONICA", "GOLPE VIRTUAL")) {
            return RitoProcessual.PENAL_CRIMES_CIBERNETICOS;
        }
        if (containsAny(corpus, "RACISMO", "INJURIA RACIAL")) {
            return RitoProcessual.PENAL_RACISMO;
        }
        if (containsAny(corpus, "TORTURA")) {
            return RitoProcessual.PENAL_TORTURA;
        }
        if (containsAny(corpus, "TERRORISMO")) {
            return RitoProcessual.PENAL_TERRORISMO;
        }
        if (containsAny(corpus, "REVISAO CRIMINAL") && containsAny(corpus, "MILITAR", "CPPM")) {
            return RitoProcessual.MILITAR_PROCESSO_PENAL_MILITAR;
        }
        if (containsAny(corpus, "REVISAO CRIMINAL")) {
            return RitoProcessual.PENAL_REVISAO_CRIMINAL;
        }
        if (containsAny(corpus, "EXECUCAO PENAL MILITAR")) {
            return RitoProcessual.MILITAR_PROCESSO_PENAL_MILITAR;
        }
        if (containsAny(corpus, "EXECUCAO PENAL", "LEP", "PROGRESSAO DE REGIME", "REMICAO")) {
            return RitoProcessual.EXECUCAO_PENAL;
        }
        if (containsAny(corpus, "ACAO CIVIL PUBLICA") && containsAny(corpus, "AMBIENT", "DANO AMBIENTAL", "LICENCIAMENTO", "IBAMA")) {
            return RitoProcessual.AMBIENTAL_ACP;
        }
        if (containsAny(corpus, "CRIME AMBIENTAL", "DESMATAMENTO", "POLUICAO AMBIENTAL")) {
            return RitoProcessual.AMBIENTAL_CRIMINAL;
        }
        if (containsAny(corpus, "TUTELA DE URGENCIA AMBIENTAL", "EMBARGO AMBIENTAL")) {
            return RitoProcessual.AMBIENTAL_TUTELA_URGENTE;
        }
        if (containsAny(corpus, "RECUPERACAO JUDICIAL")) {
            return RitoProcessual.RECUPERACAO_JUDICIAL;
        }
        if (containsAny(corpus, "RECUPERACAO EXTRAJUDICIAL")) {
            return RitoProcessual.RECUPERACAO_EXTRAJUDICIAL;
        }
        if (containsAny(corpus, "FALENCIA", "FALIDO")) {
            return RitoProcessual.FALENCIA;
        }
        if (containsAny(corpus, "DESCONSIDERACAO DA PERSONALIDADE JURIDICA", "IDPJ")) {
            return RitoProcessual.INCIDENTE_DESCONSIDERACAO_PERSONALIDADE_JURIDICA;
        }
        if (containsAny(corpus, "DESAPROPRIACAO PARA REFORMA AGRARIA")) {
            return RitoProcessual.AGRARIO_DESAPROPRIACAO;
        }
        if (containsAny(corpus, "POSSE RURAL", "TERRA RURAL", "CONFLITO AGRARIO", "ASSENTAMENTO")) {
            return RitoProcessual.AGRARIO_POSSE_TERRA;
        }
        if (containsAny(corpus, "ACAO CIVIL PUBLICA AGRARIA", "ACP AGRARIA")) {
            return RitoProcessual.AGRARIO_ACP_AGRARIA;
        }
        if (routing != null) {
            RitoProcessual fromRouting = parseRito(routing.ritoSugerido());
            if (fromRouting != null) {
                return fromRouting;
            }
        }
        return processo != null ? processo.getRito() : null;
    }
    private void alignRamoWithRito(Processo processo) {
        if (processo == null || processo.getRito() == null) {
            return;
        }
        RamoDireito suggested = processo.getRito().suggestedRamo();
        if (suggested == null || suggested == RamoDireito.CIVIL || suggested == processo.getRamoDireito()) {
            return;
        }
        if (processo.getRamoDireito() == null
                || processo.getRamoDireito() == RamoDireito.CIVIL
                || processo.getRito().isFamiliaSucessoes()
                || processo.getRito().isInfancia()
                || processo.getRito().isTrabalhista()
                || processo.getRito().isTribFazenda()) {
            processo.setRamoDireito(suggested);
        }
    }

    private boolean hasFamilyCore(String corpus) {
        return containsAny(corpus, "ALIMENT", "PENSAO ALIMENTICIA", "ALIMENTANDO", "DIVORC", "GUARDA", "UNIAO ESTAVEL", "PATERN", "CURATELA", "INTERDICAO", "INVENTARIO", "ARROLAMENTO", "HERANCA", "VISITAS");
    }

    private boolean hasInfanciaCore(String corpus) {
        return containsAny(corpus, "ATO INFRACIONAL", "MEDIDA SOCIOEDUCATIVA", "ADOLESCENTE EM CONFLITO COM A LEI", "CONSELHO TUTELAR", "ACOLHIMENTO INSTITUCIONAL", "DESTITUICAO DO PODER FAMILIAR", "ECA")
                || containsAny(corpus, "ADOCAO") && containsAny(corpus, "INFANC", "JUVENTUDE", "ECA", "MENOR");
    }

    private boolean hasExecucaoFiscalCore(String corpus) {
        return containsAny(corpus, "EXECUCAO FISCAL", "DIVIDA ATIVA", "CERTIDAO DE DIVIDA ATIVA", "CERTIDÃO DE DÍVIDA ATIVA", "CDA", "LEF");
    }

    private boolean shouldAdoptTipoJustica(TipoJustica current, TipoJustica candidate) {
        if (candidate == null || candidate == current) {
            return false;
        }
        if (current == TipoJustica.ESTADUAL && candidate == TipoJustica.SUPERIOR) {
            return false;
        }
        return current == null || current == TipoJustica.ESTADUAL && candidate != TipoJustica.ESTADUAL;
    }

    private boolean shouldAdoptRamo(RamoDireito current, RamoDireito candidate) {
        if (candidate == null || candidate == current) {
            return false;
        }
        return current == null || current == RamoDireito.CIVIL && candidate != RamoDireito.CIVIL;
    }

    private boolean shouldAdoptRito(RitoProcessual current, RitoProcessual candidate) {
        if (candidate == null || candidate == current) {
            return false;
        }
        if (current == null) {
            return true;
        }
        if (current == RitoProcessual.JUIZADO_ESPECIAL && candidate.isJuizado() && candidate != RitoProcessual.JUIZADO_ESPECIAL) {
            return true;
        }
        if (sameFamily(current, candidate) && isMoreSpecificRito(candidate, current)) {
            return true;
        }
        return false;
    }

    private boolean shouldAdoptMateria(MateriaJurisdicao current, MateriaJurisdicao candidate, RamoDireito ramoAtual) {
        if (candidate == null || candidate == current) {
            return false;
        }
        if (current == null || current == MateriaJurisdicao.MULTIMATERIA) {
            return true;
        }
        if (current == MateriaJurisdicao.CIVIL && candidate != MateriaJurisdicao.CIVIL) {
            return true;
        }
        MateriaJurisdicao baseline = MateriaJurisdicao.fromRamo(ramoAtual);
        return current == baseline && candidate != baseline;
    }

    private boolean sameFamily(RitoProcessual left, RitoProcessual right) {
        if (left == null || right == null) {
            return false;
        }
        return left.group() == right.group()
                || left.isTrabalhista() && right.isTrabalhista()
                || left.isEleitoral() && right.isEleitoral()
                || left.isMilitar() && right.isMilitar();
    }

    private boolean isMoreSpecificRito(RitoProcessual candidate, RitoProcessual current) {
        return specificity(candidate) > specificity(current);
    }

    private int specificity(RitoProcessual rito) {
        if (rito == null) {
            return -1;
        }
        if (rito == RitoProcessual.COMUM_ORDINARIO || rito == RitoProcessual.JUIZADO_ESPECIAL || rito == RitoProcessual.ELEITORAL || rito == RitoProcessual.MILITAR || rito == RitoProcessual.TRABALHISTA_ORDINARIO) {
            return 0;
        }
        if (rito == RitoProcessual.TRABALHISTA_SUMARISSIMO || rito == RitoProcessual.TRABALHISTA_SUMARIO_ALCADA) {
            return 1;
        }
        return rito.name().length();
    }

    private Map<String, Object> canonicalContext(ProceduralRoutingReport routing) {
        if (routing == null || routing.metadata() == null || routing.metadata().isEmpty()) {
            return Map.of();
        }
        Object canonicalMetadata = routing.metadata().get("canonicalMetadata");
        if (canonicalMetadata instanceof Map<?, ?> outer) {
            Object canonicalContext = outer.get("canonicalContext");
            if (canonicalContext instanceof Map<?, ?> context) {
                LinkedHashMap<String, Object> out = new LinkedHashMap<>();
                for (Map.Entry<?, ?> entry : context.entrySet()) {
                    if (entry.getKey() != null && entry.getValue() != null) {
                        out.put(String.valueOf(entry.getKey()), entry.getValue());
                    }
                }
                return out;
            }
        }
        return Map.of();
    }

    private List<String> collectText(String... values) {
        List<String> out = new ArrayList<>();
        if (values == null) {
            return out;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                out.add(value);
            }
        }
        return out;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("[^A-Za-z0-9 ]", " ")
                .replaceAll(" +", " ")
                .toUpperCase(Locale.ROOT)
                .trim();
    }

    private boolean containsAny(String text, String... needles) {
        if (text == null || text.isBlank() || needles == null) {
            return false;
        }
        for (String needle : needles) {
            if (needle != null && !needle.isBlank() && text.contains(normalize(needle))) {
                return true;
            }
        }
        return false;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null) {
                String trimmed = value.trim();
                if (!trimmed.isBlank()) {
                    return trimmed;
                }
            }
        }
        return null;
    }

    private String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private RitoProcessual parseRito(String value) {
        return value == null || value.isBlank() ? null : RitoProcessual.tryParse(value).orElse(null);
    }
}
