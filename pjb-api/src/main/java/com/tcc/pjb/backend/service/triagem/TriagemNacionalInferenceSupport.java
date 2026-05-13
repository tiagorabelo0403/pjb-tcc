package com.tcc.pjb.backend.service.triagem;

import com.tcc.pjb.backend.core.procedural.ProceduralCanonicalResolver;
import com.tcc.pjb.backend.core.procedural.ProceduralCanonicalResolver.CanonicalContext;
import com.tcc.pjb.backend.core.validation.document.DocumentoNacionalValidator;
import com.tcc.pjb.backend.model.dto.competencia.CompetenceResolveRequest;
import com.tcc.pjb.backend.model.dto.competencia.CompetenceResolveResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.competencia.CompetenceResolverService;
import com.tcc.pjb.backend.service.identity.ProntuarioNacionalService;
import com.tcc.pjb.backend.shared.text.TextTokenUtils;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
class TriagemNacionalInferenceSupport {

    private static final Logger log = LoggerFactory.getLogger(TriagemNacionalInferenceSupport.class);

    private final ProceduralCanonicalResolver proceduralCanonicalResolver;
    private final CompetenceResolverService competenceResolverService;
    private final ProcessoRepository processoRepository;
    private final ProntuarioNacionalService prontuarioNacionalService;
    private final DocumentoNacionalValidator documentoValidator;

    TriagemNacionalInferenceSupport(ProceduralCanonicalResolver proceduralCanonicalResolver,
                                    CompetenceResolverService competenceResolverService,
                                    ProcessoRepository processoRepository,
                                    ProntuarioNacionalService prontuarioNacionalService,
                                    DocumentoNacionalValidator documentoValidator) {
        this.proceduralCanonicalResolver = Objects.requireNonNull(proceduralCanonicalResolver);
        this.competenceResolverService = Objects.requireNonNull(competenceResolverService);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.prontuarioNacionalService = Objects.requireNonNull(prontuarioNacionalService);
        this.documentoValidator = Objects.requireNonNull(documentoValidator);
    }

    TriagemNacionalIAEngine.SugestaoClassificacao sugerirClassificacao(TriagemNacionalIAEngine.PedidoTriagem pedido,
                                                                       List<TriagemNacionalIAEngine.PendenciaTriagem> pendencias) {
        Map<String, Double> scores = new LinkedHashMap<>();
        String texto = normalizarTextoAlta(String.join(" ", List.of(
                Objects.toString(pedido.textoFatosResumido(), ""),
                Objects.toString(pedido.assuntoTpuSugerido(), ""),
                Objects.toString(pedido.classeTpuSugerida(), ""),
                Objects.toString(pedido.ramoDireito(), "")
        )));
        CanonicalContext canonical = proceduralCanonicalResolver.resolve(Map.ofEntries(
                Map.entry("textoCaso", Objects.toString(pedido.textoFatosResumido(), "")),
                Map.entry("classeTpu", Objects.toString(pedido.classeTpuSugerida(), "")),
                Map.entry("classe", Objects.toString(pedido.classeTpuSugerida(), "")),
                Map.entry("assunto", Objects.toString(pedido.assuntoTpuSugerido(), "")),
                Map.entry("ramoDireito", Objects.toString(pedido.ramoDireito(), ""))
        ));

        adicionarScore(scores, "PROCEDIMENTO_COMUM|CIVIL", 0.35);
        if (texto.contains("ALIMENT") || texto.contains("GUARDA") || texto.contains("DIVORC")) {
            adicionarScore(scores, "PROCEDIMENTO_COMUM|FAMILIA", 0.78);
            adicionarScore(scores, "TUTELA_ANTECIPADA|ALIMENTOS", 0.61);
        }
        if (texto.contains("TRABALH") || texto.contains("CLT") || texto.contains("FGTS") || texto.contains("RESCISAO")) {
            adicionarScore(scores, "RECLAMACAO_TRABALHISTA|VERBAS_TRABALHISTAS", 0.91);
        }
        if (texto.contains("EXECUCAO FISCAL") || texto.contains("CDA") || texto.contains("DIVIDA ATIVA")) {
            adicionarScore(scores, "EXECUCAO_FISCAL|DIVIDA_ATIVA", 0.95);
        }
        if (texto.contains("CONSUMIDOR") || texto.contains("COBRANCA INDEVIDA") || texto.contains("NEGATIVACAO") || texto.contains("DANO MORAL")) {
            adicionarScore(scores, "PROCEDIMENTO_COMUM|RESPONSABILIDADE_CIVIL", 0.82);
            adicionarScore(scores, "JUIZADO_ESPECIAL_CIVEL|RELACAO_DE_CONSUMO", 0.71);
        }
        if (texto.contains("MANDADO") && texto.contains("SEGURANCA")) {
            adicionarScore(scores, "MANDADO_DE_SEGURANCA_CIVEL|ATO_ADMINISTRATIVO", 0.88);
        }
        if (texto.contains("HABEAS") || texto.contains("PENAL") || texto.contains("CRIME")) {
            adicionarScore(scores, "PROCEDIMENTO_PENAL_COMUM|CRIMES_CONTRA_A_PESSOA", 0.86);
        }
        if (texto.contains("AIJE") || texto.contains("ABUSO DE PODER") || texto.contains("CAPTACAO ILICITA SUFRAGIO")) {
            adicionarScore(scores, "AIJE — AÇÃO DE INVESTIGAÇÃO JUDICIAL ELEITORAL (ABUSO DE PODER)|ELEITORAL", 0.96);
        }
        if (texto.contains("AIRC") || texto.contains("IMPUGNACAO DE REGISTRO")) {
            adicionarScore(scores, "AIRC — AÇÃO DE IMPUGNAÇÃO DE REGISTRO DE CANDIDATURA|ELEITORAL", 0.95);
        }
        if (texto.contains("IPM") || texto.contains("INQUERITO POLICIAL MILITAR")) {
            adicionarScore(scores, "IPM — INQUÉRITO POLICIAL MILITAR (CPPM ART. 9º)|MILITAR", 0.96);
        }
        if (texto.contains("CRIME MILITAR") || texto.contains("CPPM") || texto.contains("AUDITORIA MILITAR")) {
            adicionarScore(scores, "AÇÃO PENAL MILITAR (CPM / CPPM)|MILITAR", 0.93);
        }
        if (texto.contains("DISSIDIO COLETIVO") || texto.contains("GREVE") || texto.contains("CONVENCAO COLETIVA")) {
            adicionarScore(scores, "DISSÍDIO COLETIVO ECONÔMICO|TRABALHISTA", 0.95);
        }
        if (texto.contains("INSS") || texto.contains("PREVID") || texto.contains("APOSENTADORIA") || texto.contains("LOAS") || texto.contains("BPC")) {
            adicionarScore(scores, "CONCESSÃO/RESTABELECIMENTO DE BENEFÍCIO PREVIDENCIÁRIO (INSS)|PREVIDENCIARIO", 0.92);
        }
        if (texto.contains("EXECUCAO FISCAL") || texto.contains("CDA") || texto.contains("DIVIDA ATIVA")) {
            adicionarScore(scores, "EXECUÇÃO FISCAL (LEI 6.830/80)|TRIBUTARIO", 0.97);
        }
        if (texto.contains("REPETICAO DE INDEBITO") || texto.contains("ANULATORIA DE DEBITO") || texto.contains("MANDADO DE SEGURANCA TRIBUTARIO")) {
            adicionarScore(scores, "AÇÃO ANULATÓRIA DE DÉBITO FISCAL|TRIBUTARIO", 0.91);
        }
        if (texto.contains("IMPROBIDADE") || texto.contains("CONCURSO PUBLICO") || texto.contains("SERVIDOR PUBLICO")) {
            adicionarScore(scores, "AÇÃO DE IMPROBIDADE ADMINISTRATIVA (LEI 8.429/92, PÓS-LEI 14.230/21)|ADMINISTRATIVO", 0.90);
        }
        if (texto.contains("USUCAPIAO") || texto.contains("MONITORIA") || texto.contains("CONSIGNACAO") || texto.contains("POSSE")) {
            adicionarScore(scores, "PROCEDIMENTO COMUM|CIVIL_ESPECIALIZADO", 0.84);
        }
        if (texto.contains("RECUPERACAO JUDICIAL") || texto.contains("FALENCIA") || texto.contains("DESCONSIDERACAO DA PERSONALIDADE JURIDICA")) {
            adicionarScore(scores, "RECUPERAÇÃO JUDICIAL (LEI 11.101/05)|EMPRESARIAL", 0.91);
        }
        if (texto.contains("ADPF") || texto.contains("ADI") || texto.contains("ADC") || texto.contains("MANDADO DE INJUNCAO") || texto.contains("HABEAS DATA")) {
            adicionarScore(scores, "MANDADO DE SEGURANÇA INDIVIDUAL|CONSTITUCIONAL", 0.89);
        }
        if (texto.contains("ADOCAO") || texto.contains("ATO INFRACIONAL") || texto.contains("ECA") || texto.contains("MEDIDA SOCIOEDUCATIVA")) {
            adicionarScore(scores, "AÇÃO POR ATO INFRACIONAL — ECA (LEI 8.069/90)|INFANCIA", 0.90);
        }
        if (texto.contains("AMBIENT") || texto.contains("DESMAT") || texto.contains("LICENCIAMENTO")) {
            adicionarScore(scores, "AÇÃO CIVIL PÚBLICA (LEI 7.347/85)|AMBIENTAL", 0.88);
        }
        if (texto.contains("AGRAR") || texto.contains("REFORMA AGRARIA") || texto.contains("IMOVEL RURAL")) {
            adicionarScore(scores, "DESAPROPRIAÇÃO PARA REFORMA AGRÁRIA (LC 76/93)|AGRARIO", 0.87);
        }
        if (canonical.classeTpuNome() != null && !canonical.classeTpuNome().isBlank()) {
            adicionarScore(scores, canonical.classeTpuNome() + "|" + firstNonBlank(canonical.ramoDireito(), normalizeOptional(pedido.ramoDireito()), "CANONICO"), 0.98);
        }
        if (pedido.classeTpuSugerida() != null && !pedido.classeTpuSugerida().isBlank()) {
            adicionarScore(scores, normalizeOptional(pedido.classeTpuSugerida()) + "|" + firstNonBlank(normalizeOptional(pedido.assuntoTpuSugerido()), "INDETERMINADO"), 0.93);
        }

        List<TriagemNacionalIAEngine.AlternativaTpu> alternativas = scores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(4)
                .map(entry -> {
                    String[] parts = entry.getKey().split("\\|", 2);
                    return new TriagemNacionalIAEngine.AlternativaTpu(parts[0], parts.length > 1 ? parts[1] : "INDETERMINADO", round4(entry.getValue()));
                })
                .toList();

        TriagemNacionalIAEngine.AlternativaTpu principal = alternativas.isEmpty()
                ? new TriagemNacionalIAEngine.AlternativaTpu("PROCEDIMENTO_COMUM", "INDETERMINADO", 0.52)
                : alternativas.get(0);
        boolean confirmacao = principal.probabilidade() < 0.86 || (alternativas.size() > 1 && Math.abs(principal.probabilidade() - alternativas.get(1).probabilidade()) < 0.07);
        if (confirmacao) {
            pendencias.add(new TriagemNacionalIAEngine.PendenciaTriagem(
                    TriagemNacionalIAEngine.TipoPendencia.CLASSIFICACAO_INCERTA,
                    "Classificacao TPU com margem estreita entre candidatos",
                    false,
                    TriagemNacionalIAEngine.SeveridadePendencia.ALERTA,
                    "Revise classe e assunto antes da distribuicao automatica"
            ));
        }
        double confiancaAssunto = Math.max(0.38, principal.probabilidade() - 0.06);
        return new TriagemNacionalIAEngine.SugestaoClassificacao(
                principal.classeTpu(),
                principal.assuntoTpu(),
                round4(principal.probabilidade()),
                round4(confiancaAssunto),
                alternativas,
                confirmacao
        );
    }

    TriagemNacionalIAEngine.AnalisePrescricao analisarPrescricao(TriagemNacionalIAEngine.PedidoTriagem pedido) {
        if (pedido.dataFatoGerador() == null) {
            return TriagemNacionalIAEngine.AnalisePrescricao.semAnalise();
        }
        long dias = pedido.dataFatoGerador().until(LocalDate.now(), ChronoUnit.DAYS);
        PrazoLegal prazo = inferirPrazoLegal(pedido);
        boolean prescricao = prazo.natureza() == NaturezaPrazo.PRESCRICAO && dias > prazo.anos() * 365L;
        boolean decadencia = prazo.natureza() == NaturezaPrazo.DECADENCIA && dias > prazo.anos() * 365L;
        String observacao = prescricao || decadencia
                ? "Prazo aparente excedido em triagem automatica"
                : "Prazo ainda nao superado pelos metadados informados";
        return new TriagemNacionalIAEngine.AnalisePrescricao(
                prescricao,
                decadencia,
                prazo.fundamento(),
                prazo.anos(),
                dias,
                observacao
        );
    }

    TriagemNacionalIAEngine.CompetenciaTriagem analisarCompetencia(TriagemNacionalIAEngine.PedidoTriagem pedido,
                                                                   TriagemNacionalIAEngine.SugestaoClassificacao classificacao,
                                                                   List<TriagemNacionalIAEngine.PendenciaTriagem> pendencias) {
        String textoBase = normalizarTextoAlta(pedido.textoFatosResumido());
        CanonicalContext canonicalContext = proceduralCanonicalResolver.resolve(Map.ofEntries(
                Map.entry("textoCaso", Objects.toString(pedido.textoFatosResumido(), "")),
                Map.entry("classeTpu", Objects.toString(classificacao.classeTpu(), "")),
                Map.entry("assunto", Objects.toString(classificacao.assuntoTpu(), "")),
                Map.entry("ramoDireito", Objects.toString(pedido.ramoDireito(), ""))
        ));
        CompetenceResolveRequest req = new CompetenceResolveRequest(
                pedido.textoFatosResumido(),
                classificacao.assuntoTpu(),
                classificacao.classeTpu(),
                canonicalContext.ramoDireito() != null ? canonicalContext.ramoDireito() : pedido.ramoDireito(),
                null,
                null,
                pedido.valorCausa(),
                containsAny(textoBase, Set.of("UNIAO", "INSS", "AUTARQUIA", "TRF")),
                containsAny(textoBase, Set.of("AUTARQUIA", "INSS", "ANVISA", "IBAMA")),
                containsAny(textoBase, Set.of("CAIXA", "CEF", "EMPRESA PUBLICA FEDERAL")),
                containsAny(textoBase, Set.of("ESTADO", "FAZENDA ESTADUAL")),
                containsAny(textoBase, Set.of("MUNICIP", "PREFEITURA")),
                containsAny(textoBase, Set.of("TRABALH", "CLT", "FGTS", "VINCULO")),
                containsAny(textoBase, Set.of("ELEITOR", "CANDIDAT", "TRE", "TSE")),
                containsAny(textoBase, Set.of("MILITAR", "EXERCITO", "PM", "BOMBEIRO"))
        );
        CompetenceResolveResponse resposta = competenceResolverService.resolve(req);
        boolean incompatibilidade = false;
        String ramo = normalizeOptional(canonicalContext.ramoDireito() != null ? canonicalContext.ramoDireito() : pedido.ramoDireito());
        String tipoJustica = resposta.tipoJusticaSugerida();
        if (ramo.contains("TRABALH") && tipoJustica != null && !tipoJustica.contains("TRABALHO")) {
            incompatibilidade = true;
        }
        if (ramo.contains("ELEITOR") && tipoJustica != null && !tipoJustica.contains("ELEITORAL")) {
            incompatibilidade = true;
        }
        if (ramo.contains("MILITAR") && tipoJustica != null && !tipoJustica.contains("MILITAR")) {
            incompatibilidade = true;
        }
        if ((ramo.contains("PENAL") || ramo.contains("CRIM")) && Objects.toString(classificacao.classeTpu(), "").toUpperCase(Locale.ROOT).contains("CIVEL")) {
            incompatibilidade = true;
        }
        if (canonicalContext.classeTpuNome() != null && classificacao.classeTpu() != null
                && !normalizeOptional(classificacao.classeTpu()).contains(normalizeOptional(canonicalContext.classeTpuNome()))) {
            pendencias.add(new TriagemNacionalIAEngine.PendenciaTriagem(
                    TriagemNacionalIAEngine.TipoPendencia.CLASSIFICACAO_INCERTA,
                    "Classe sugerida diverge do catálogo canônico consolidado",
                    false,
                    TriagemNacionalIAEngine.SeveridadePendencia.ALERTA,
                    "Revise a classe TPU e o rito antes do ajuizamento"
            ));
        }
        if (incompatibilidade) {
            pendencias.add(new TriagemNacionalIAEngine.PendenciaTriagem(
                    TriagemNacionalIAEngine.TipoPendencia.INCOMPETENCIA_MANIFESTA,
                    "Sinais de competencia, classe e ramo em conflito na triagem",
                    true,
                    TriagemNacionalIAEngine.SeveridadePendencia.CRITICA,
                    "Revise ramo, classe, tribunal e fundamentos de competencia territorial ou material"
            ));
        }
        List<String> fundamentos = new ArrayList<>(resposta.reasons());
        if (canonicalContext.tribunalCodigo() != null) {
            fundamentos.add("Tribunal canônico resolvido=" + canonicalContext.tribunalCodigo());
        }
        if (canonicalContext.classeTpuCodigo() != null) {
            fundamentos.add("Classe TPU canônica=" + canonicalContext.classeTpuCodigo());
        }
        return new TriagemNacionalIAEngine.CompetenciaTriagem(
                resposta.tipoJusticaSugerida(),
                canonicalContext.rito() != null ? canonicalContext.rito().name() : resposta.ritoSugerido(),
                round4(Math.max(resposta.confidence(), canonicalContext.classeTpuCodigo() != null ? 0.94d : resposta.confidence())),
                incompatibilidade,
                List.copyOf(new LinkedHashSet<>(fundamentos))
        );
    }

    List<TriagemNacionalIAEngine.ProcessoSuspeito> detectarConexos(TriagemNacionalIAEngine.PedidoTriagem pedido,
                                                                   TriagemNacionalIAEngine.SugestaoClassificacao classificacao) {
        Map<Long, Processo> candidatos = new LinkedHashMap<>();
        if (pedido.cpfCnpjAutor() != null && !pedido.cpfCnpjAutor().isBlank()) {
            for (Processo processo : processoRepository.findAllByPartesCpf(pedido.cpfCnpjAutor())) {
                if (processo.getId() != null) {
                    candidatos.put(processo.getId(), processo);
                }
            }
        }
        if (pedido.cpfCnpjReu() != null && !pedido.cpfCnpjReu().isBlank()) {
            for (Processo processo : processoRepository.findAllByPartesCpf(pedido.cpfCnpjReu())) {
                if (processo.getId() != null) {
                    candidatos.put(processo.getId(), processo);
                }
            }
        }

        List<TriagemNacionalIAEngine.ProcessoSuspeito> suspeitos = new ArrayList<>();
        RamoDireito ramo = parseRamoDireito(pedido.ramoDireito());
        if (ramo != null && pedido.cpfCnpjAutor() != null && pedido.cpfCnpjReu() != null) {
            try {
                ProntuarioNacionalService.AnaliseConflitoProcessual analise = prontuarioNacionalService.detectarLitispendenciaOuCoisaJulgada(
                        pedido.cpfCnpjAutor(),
                        pedido.cpfCnpjReu(),
                        ramo
                );
                for (String nupn : analise.nupnsEmAndamento()) {
                    suspeitos.add(new TriagemNacionalIAEngine.ProcessoSuspeito(
                            nupn,
                            "NACIONAL",
                            0.96,
                            TriagemNacionalIAEngine.TipoConexao.LITISPENDENTE,
                            "Coincidencia nacional de partes e ramo no prontuario consolidado"
                    ));
                }
                for (String nupn : analise.nupnsArquivados()) {
                    suspeitos.add(new TriagemNacionalIAEngine.ProcessoSuspeito(
                            nupn,
                            "NACIONAL",
                            0.97,
                            TriagemNacionalIAEngine.TipoConexao.COISA_JULGADA,
                            "Coincidencia nacional em processo arquivado compativel com coisa julgada potencial"
                    ));
                }
            } catch (Exception ex) {
                log.debug("Analise nacional de conflito nao disponivel: {}", ex.getMessage());
            }
        }

        Set<String> tokensPedido = TextTokenUtils.tokens(normalizarTextoAlta(String.join(" ", List.of(
                Objects.toString(pedido.textoFatosResumido(), ""),
                firstNonBlank(classificacao.classeTpu(), ""),
                firstNonBlank(classificacao.assuntoTpu(), "")
        ))));

        for (Processo processo : candidatos.values()) {
            if (pedido.processoId() != null && pedido.processoId().equals(processo.getId())) {
                continue;
            }
            double score = 0.0;
            if (pedido.cpfCnpjAutor() != null && pedido.cpfCnpjAutor().equals(normalizeDocumentoOptional(processo.getParteAutoraCpf()))) {
                score += 0.28;
            }
            if (pedido.cpfCnpjReu() != null && pedido.cpfCnpjReu().equals(normalizeDocumentoOptional(processo.getParteReuCpf()))) {
                score += 0.28;
            }
            if (processo.getRamoDireito() != null && processo.getRamoDireito().name().equalsIgnoreCase(Objects.toString(pedido.ramoDireito(), ""))) {
                score += 0.14;
            }
            if (normalizeOptional(processo.getClasseProcessual()).equalsIgnoreCase(classificacao.classeTpu())) {
                score += 0.12;
            }
            Set<String> tokensExistente = TextTokenUtils.tokens(normalizarTextoAlta(String.join(" ", List.of(
                    firstNonBlank(processo.getResumoIA(), ""),
                    firstNonBlank(processo.getClasseProcessual(), ""),
                    firstNonBlank(processo.getAssunto(), "")
            ))));
            score += overlap(tokensPedido, tokensExistente) * 0.36;
            double similaridade = Math.min(0.99, score);
            if (similaridade < 0.58) {
                continue;
            }
            TriagemNacionalIAEngine.TipoConexao tipo = classificarConexao(processo, similaridade);
            suspeitos.add(new TriagemNacionalIAEngine.ProcessoSuspeito(
                    firstNonBlank(processo.getNumeroUnificado(), processo.getNumeroProcesso(), "processo-" + processo.getId()),
                    resolverTribunalCodigo(processo),
                    round4(similaridade),
                    tipo,
                    descreverSimilaridade(processo, similaridade, tipo)
            ));
        }

        return suspeitos.stream()
                .sorted(Comparator.comparingDouble(TriagemNacionalIAEngine.ProcessoSuspeito::similaridade).reversed())
                .distinct()
                .limit(8)
                .toList();
    }

    void adicionarPendenciasPorConexao(List<TriagemNacionalIAEngine.ProcessoSuspeito> conexos,
                                       List<TriagemNacionalIAEngine.PendenciaTriagem> pendencias) {
        for (TriagemNacionalIAEngine.ProcessoSuspeito conexo : conexos) {
            if (conexo.tipoConexao() == TriagemNacionalIAEngine.TipoConexao.COISA_JULGADA || conexo.tipoConexao() == TriagemNacionalIAEngine.TipoConexao.IDENTICO) {
                pendencias.add(new TriagemNacionalIAEngine.PendenciaTriagem(
                        TriagemNacionalIAEngine.TipoPendencia.COISA_JULGADA_SUSPEITA,
                        "Processo semelhante identificado: " + conexo.nupn() + " com similaridade de " + percent(conexo.similaridade()),
                        true,
                        TriagemNacionalIAEngine.SeveridadePendencia.CRITICA,
                        "Demonstre distincao fatico-juridica ou informe prevencao/conexao formal"
                ));
            } else if (conexo.tipoConexao() == TriagemNacionalIAEngine.TipoConexao.LITISPENDENTE) {
                pendencias.add(new TriagemNacionalIAEngine.PendenciaTriagem(
                        TriagemNacionalIAEngine.TipoPendencia.LITISPENDENCIA_SUSPEITA,
                        "Feito em andamento semelhante: " + conexo.nupn() + " com similaridade de " + percent(conexo.similaridade()),
                        true,
                        TriagemNacionalIAEngine.SeveridadePendencia.CRITICA,
                        "Revise litispendencia, conexao e prevencao"
                ));
            }
        }
    }

    double calcularConfiancaGeral(List<TriagemNacionalIAEngine.PendenciaTriagem> pendencias,
                                  TriagemNacionalIAEngine.SugestaoClassificacao classificacao,
                                  TriagemNacionalIAEngine.CompetenciaTriagem competencia,
                                  List<TriagemNacionalIAEngine.ProcessoSuspeito> conexos) {
        double score = 0.25;
        score += classificacao.confiancaClasse() * 0.30;
        score += classificacao.confiancaAssunto() * 0.10;
        score += competencia.confianca() * 0.22;
        score += pendencias.stream().noneMatch(TriagemNacionalIAEngine.PendenciaTriagem::impeditiva) ? 0.08 : 0.0;
        score -= pendencias.stream().filter(TriagemNacionalIAEngine.PendenciaTriagem::impeditiva).count() * 0.12;
        score -= pendencias.stream().filter(p -> p.severidade() == TriagemNacionalIAEngine.SeveridadePendencia.ALERTA).count() * 0.04;
        score -= pendencias.stream().filter(p -> p.severidade() == TriagemNacionalIAEngine.SeveridadePendencia.CRITICA).count() * 0.06;
        if (!conexos.isEmpty()) {
            score -= Math.min(0.18, conexos.get(0).similaridade() * 0.20);
        }
        return Math.max(0.0, Math.min(1.0, score));
    }

    String construirResumo(TriagemNacionalIAEngine.PedidoTriagem pedido,
                           TriagemNacionalIAEngine.VereditoTriagem veredito,
                           List<TriagemNacionalIAEngine.PendenciaTriagem> pendencias,
                           TriagemNacionalIAEngine.SugestaoClassificacao classificacao,
                           TriagemNacionalIAEngine.CompetenciaTriagem competencia,
                           TriagemNacionalIAEngine.AnalisePrescricao prescricao,
                           List<TriagemNacionalIAEngine.ProcessoSuspeito> conexos,
                           double confianca) {
        long impeditivas = pendencias.stream().filter(TriagemNacionalIAEngine.PendenciaTriagem::impeditiva).count();
        StringBuilder sb = new StringBuilder(512);
        sb.append("Triagem ").append(veredito.name());
        sb.append(" | confianca=").append(percent(confianca));
        sb.append(" | classe=").append(classificacao.classeTpu());
        sb.append(" | assunto=").append(classificacao.assuntoTpu());
        sb.append(" | competencia=").append(firstNonBlank(competencia.tipoJusticaSugerido(), "NAO_IDENTIFICADA"));
        sb.append(" | rito=").append(firstNonBlank(competencia.ritoSugerido(), "NAO_IDENTIFICADO"));
        sb.append(" | pendencias=").append(pendencias.size()).append("/").append(impeditivas).append(" impeditivas");
        sb.append(" | conexos=").append(conexos.size());
        if (prescricao.prescricaoAparente() || prescricao.decadenciaAparente()) {
            sb.append(" | prazo=").append(prescricao.fundamentoLegal());
        }
        if (pedido.requerLiminar()) {
            sb.append(" | urgencia=SIM");
        }
        return sb.toString();
    }

    private PrazoLegal inferirPrazoLegal(TriagemNacionalIAEngine.PedidoTriagem pedido) {
        String texto = normalizarTextoAlta(String.join(" ", List.of(
                Objects.toString(pedido.ramoDireito(), ""),
                Objects.toString(pedido.classeTpuSugerida(), ""),
                Objects.toString(pedido.assuntoTpuSugerido(), ""),
                Objects.toString(pedido.textoFatosResumido(), "")
        )));
        if (texto.contains("EXECUCAO FISCAL") || texto.contains("TRIBUT") || texto.contains("DIVIDA ATIVA")) {
            return new PrazoLegal(5, NaturezaPrazo.PRESCRICAO, "CTN e legislacao correlata - heuristica quinquenal");
        }
        if (texto.contains("TRABALH") || texto.contains("CLT") || texto.contains("FGTS") || texto.contains("RESCISAO")) {
            return new PrazoLegal(5, NaturezaPrazo.PRESCRICAO, "CF e CLT - heuristica quinquenal trabalhista");
        }
        if (texto.contains("CONSUMID") || texto.contains("PRODUTO") || texto.contains("SERVICO")) {
            return new PrazoLegal(5, NaturezaPrazo.PRESCRICAO, "CDC e responsabilidade civil - heuristica quinquenal");
        }
        if (texto.contains("ANULACAO") || texto.contains("DECADENCIA") || texto.contains("PRAZO DECADENCIAL")) {
            return new PrazoLegal(4, NaturezaPrazo.DECADENCIA, "Heuristica decadencial baseada na materia informada");
        }
        return new PrazoLegal(10, NaturezaPrazo.PRESCRICAO, "CC art. 205 - heuristica geral decenal");
    }

    private static TriagemNacionalIAEngine.TipoConexao classificarConexao(Processo processo, double similaridade) {
        StatusProcesso status = processo.getStatusProcesso();
        if (similaridade >= 0.92) {
            if (status == StatusProcesso.ARQUIVADO) {
                return TriagemNacionalIAEngine.TipoConexao.COISA_JULGADA;
            }
            return TriagemNacionalIAEngine.TipoConexao.IDENTICO;
        }
        if (similaridade >= 0.80) {
            if (status != StatusProcesso.ARQUIVADO) {
                return TriagemNacionalIAEngine.TipoConexao.LITISPENDENTE;
            }
            return TriagemNacionalIAEngine.TipoConexao.CONTINENTE;
        }
        return TriagemNacionalIAEngine.TipoConexao.CONEXO;
    }

    private static String descreverSimilaridade(Processo processo, double similaridade, TriagemNacionalIAEngine.TipoConexao tipo) {
        return "Processo local com status " + firstNonBlank(processo.getStatusProcesso() != null ? processo.getStatusProcesso().name() : null, "DESCONHECIDO")
                + ", tipo=" + tipo.name() + ", score=" + percent(similaridade);
    }

    private static RamoDireito parseRamoDireito(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return RamoDireito.valueOf(normalizeOptional(valor));
        } catch (Exception ignored) {
            return null;
        }
    }

    private String normalizarTextoAlta(String value) {
        if (value == null) {
            return "";
        }
        return value.toUpperCase(Locale.ROOT).replaceAll("\\s+", " ").strip();
    }

    private String normalizeDocumentoOptional(String value) {
        String doc = documentoValidator.normalizarDocumento(value);
        return doc.isBlank() ? null : doc;
    }

    private static boolean containsAny(String text, Set<String> needles) {
        String base = normalizeOptional(text);
        for (String needle : needles) {
            if (base.contains(normalizeOptional(needle))) {
                return true;
            }
        }
        return false;
    }

    private static void adicionarScore(Map<String, Double> mapa, String key, double score) {
        mapa.merge(key, score, Math::max);
    }

    private static String normalizeOptional(String value) {
        return value == null ? "" : value.strip().toUpperCase(Locale.ROOT);
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

    private static double overlap(Set<String> a, Set<String> b) {
        if (a == null || b == null || a.isEmpty() || b.isEmpty()) {
            return 0.0;
        }
        Set<String> inter = new LinkedHashSet<>(a);
        inter.retainAll(b);
        Set<String> uni = new LinkedHashSet<>(a);
        uni.addAll(b);
        return uni.isEmpty() ? 0.0 : (double) inter.size() / (double) uni.size();
    }

    private static String resolverTribunalCodigo(Processo processo) {
        if (processo.getTipoJustica() == null) {
            return "PJB";
        }
        return switch (processo.getTipoJustica()) {
            case FEDERAL -> "TRF";
            case TRABALHO -> "TRT";
            case ELEITORAL -> "TRE";
            case MILITAR_ESTADUAL, MILITAR_FEDERAL -> "TJM";
            case SUPERIOR -> "STJ";
            default -> "TJ";
        };
    }

    private static String percent(double value) {
        return String.format(Locale.ROOT, "%.0f%%", value * 100.0);
    }

    private static double round4(double value) {
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP).doubleValue();
    }

    private enum NaturezaPrazo {
        PRESCRICAO,
        DECADENCIA
    }

    private record PrazoLegal(int anos, NaturezaPrazo natureza, String fundamento) {
    }
}
