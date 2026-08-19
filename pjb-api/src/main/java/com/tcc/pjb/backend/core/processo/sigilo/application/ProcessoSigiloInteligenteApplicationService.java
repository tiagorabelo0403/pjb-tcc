package com.tcc.pjb.backend.core.processo.sigilo.application;

import com.tcc.pjb.backend.core.processo.sigilo.domain.ProcessoSigiloAggregate;
import com.tcc.pjb.backend.core.processo.sigilo.domain.ProcessoSigiloDestinatario;
import com.tcc.pjb.backend.core.processo.sigilo.domain.ProcessoSigiloFinding;
import com.tcc.pjb.backend.core.processo.sigilo.domain.ProcessoSigiloInteligenteAggregate;
import com.tcc.pjb.backend.core.processo.sigilo.domain.ProcessoSigiloJurisdicaoBridge;
import com.tcc.pjb.backend.core.processo.sigilo.domain.ProcessoSigiloProtecaoDado;
import com.tcc.pjb.backend.core.processo.unificado.application.ProcessoUnificadoApplicationService;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoAggregate;
import com.tcc.pjb.backend.core.validation.document.DocumentoNacionalValidator;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.Jurisdicao;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.service.SigiloService;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ProcessoSigiloInteligenteApplicationService {

    private final ProcessoRepository processoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProcessoUnificadoApplicationService processoUnificadoApplicationService;
    private final ProcessoSigiloApplicationService processoSigiloApplicationService;
    private final SigiloService sigiloService;
    private final DocumentoNacionalValidator documentoNacionalValidator;

    public ProcessoSigiloInteligenteApplicationService(ProcessoRepository processoRepository,
                                                       UsuarioRepository usuarioRepository,
                                                       ProcessoUnificadoApplicationService processoUnificadoApplicationService,
                                                       ProcessoSigiloApplicationService processoSigiloApplicationService,
                                                       SigiloService sigiloService,
                                                       DocumentoNacionalValidator documentoNacionalValidator) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.usuarioRepository = Objects.requireNonNull(usuarioRepository);
        this.processoUnificadoApplicationService = Objects.requireNonNull(processoUnificadoApplicationService);
        this.processoSigiloApplicationService = Objects.requireNonNull(processoSigiloApplicationService);
        this.sigiloService = Objects.requireNonNull(sigiloService);
        this.documentoNacionalValidator = Objects.requireNonNull(documentoNacionalValidator);
    }

    public ProcessoSigiloInteligenteAggregate avaliar(Long processoId) {
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        ProcessoUnificadoAggregate unificado = processoUnificadoApplicationService.detalhar(processoId);
        ProcessoSigiloAggregate sigilo = processoSigiloApplicationService.detalhar(processoId);
        SigiloService.SigiloDecision decisaoLegada = sigiloService.avaliar(processo);

        boolean operacaoPolicialSigilosa = isOperacaoPolicialSigilosa(processo);
        boolean segredoEstadoPresumido = suggestsSegredoEstado(processo, operacaoPolicialSigilosa);
        NivelSigilo nivelAtual = processo.getNivelSigilo() == null ? NivelSigilo.PUBLICO : processo.getNivelSigilo();
        NivelSigilo nivelRecomendado = calcularNivelRecomendado(nivelAtual, decisaoLegada.nivel(), segredoEstadoPresumido, operacaoPolicialSigilosa);
        boolean protecaoDocumentalReforcada = hasPersonalDocuments(processo) || decisaoLegada.signals().contains(SigiloService.SigiloSignal.LGPD);
        ProcessoSigiloJurisdicaoBridge bridge = buildJurisdicaoBridge(processo, unificado, segredoEstadoPresumido, operacaoPolicialSigilosa);

        ArrayList<String> triggers = new ArrayList<>();
        ArrayList<ProcessoSigiloFinding> findings = new ArrayList<>(sigilo.findings());
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>(sigilo.fundamentos());
        fundamentos.addAll(unificado.competencia().fundamentos());
        fundamentos.add("Somente magistrado decreta segredo de justiça, segredo reforçado ou segredo de Estado; o motor apenas recomenda, bloqueia e notifica.");
        fundamentos.add("A jurisdição deve modular quem entra na trilha sigilosa: advocacia, Ministério Público, Defensoria, servidores do foro e, em operação policial, delegado e juiz natural.");

        if (nivelRecomendado.getNivel() > nivelAtual.getNivel()) {
            triggers.add("RECLASSIFICACAO_SUGERIDA");
            findings.add(new ProcessoSigiloFinding(
                    "RECLASSIFICACAO_JUDICIAL_OBRIGATORIA",
                    "Reclassificação judicial obrigatória",
                    segredoEstadoPresumido ? "CRITICAL" : "ALTA",
                    true,
                    "O motor encontrou sinais suficientes para sugerir nível de sigilo mais forte, mas a decretação deve ser judicial.",
                    List.of("NOTIFICAR_MAGISTRADO_NATURAL", "GERAR_MINUTA_DE_SIGILO", "REVALIDAR_AUDIENCIA_DE_DESTINATARIOS")
            ));
        }
        if (operacaoPolicialSigilosa) {
            triggers.add("OPERACAO_POLICIAL_SIGILOSA");
            findings.add(new ProcessoSigiloFinding(
                    "OPERACAO_POLICIAL_COM_AUDIENCIA_RESTRITA",
                    "Operação policial com audiência restrita",
                    "CRITICAL",
                    true,
                    "Fluxos de operação policial sensível devem limitar a ciência inicial a juiz natural, delegados e núcleo institucional estritamente necessário.",
                    List.of("RESTRINGIR_AUDIENCIA", "ATIVAR_STEP_UP_FORTE", "REDUZIR_CANAIS_EXTERNOS")
            ));
        }
        if (protecaoDocumentalReforcada) {
            triggers.add("PROTECAO_DOCUMENTAL_REFORCADA");
        }
        if (segredoEstadoPresumido) {
            triggers.add("POTENCIAL_SEGREDO_ESTADO");
        }
        if (bridge.admiteAudienceDelegado()) {
            triggers.add("JURISDICAO_COMPATIVEL_COM_AUDIENCIA_DELEGADO");
        }
        if (sigilo.totalFindings() > 0) {
            triggers.add("SIGILO_EXISTENTE_COM_FINDINGS");
        }

        List<ProcessoSigiloProtecaoDado> protecoesDados = buildProtecoesDados(processo, protecaoDocumentalReforcada, operacaoPolicialSigilosa);
        List<ProcessoSigiloDestinatario> destinatarios = buildDestinatarios(processo, nivelRecomendado, bridge, operacaoPolicialSigilosa);

        findings.sort(Comparator.comparing(ProcessoSigiloFinding::blocking).reversed()
                .thenComparing(this::severityRank)
                .thenComparing(ProcessoSigiloFinding::code));

        return new ProcessoSigiloInteligenteAggregate(
                unificado.identity(),
                nivelAtual,
                nivelRecomendado,
                resolveStatus(nivelAtual, nivelRecomendado, segredoEstadoPresumido, operacaoPolicialSigilosa),
                nivelRecomendado.getNivel() > nivelAtual.getNivel(),
                true,
                operacaoPolicialSigilosa,
                protecaoDocumentalReforcada,
                resolveAudienceMode(operacaoPolicialSigilosa, nivelRecomendado),
                bridge,
                List.copyOf(new LinkedHashSet<>(triggers)),
                destinatarios,
                protecoesDados,
                List.copyOf(findings),
                List.copyOf(fundamentos),
                Instant.now()
        );
    }

    private NivelSigilo calcularNivelRecomendado(NivelSigilo atual,
                                                 NivelSigilo legado,
                                                 boolean segredoEstadoPresumido,
                                                 boolean operacaoPolicialSigilosa) {
        NivelSigilo base = legado == null ? NivelSigilo.PUBLICO : legado;
        if (segredoEstadoPresumido) {
            base = NivelSigilo.SEGREDO_ESTADO;
        } else if (operacaoPolicialSigilosa && base.getNivel() < NivelSigilo.SIGILO_N4.getNivel()) {
            base = NivelSigilo.SIGILO_N4;
        }
        return base.getNivel() > atual.getNivel() ? base : atual;
    }

    private ProcessoSigiloJurisdicaoBridge buildJurisdicaoBridge(Processo processo,
                                                                 ProcessoUnificadoAggregate unificado,
                                                                 boolean segredoEstadoPresumido,
                                                                 boolean operacaoPolicialSigilosa) {
        Jurisdicao jurisdicao = processo.getJurisdicao();
        ArrayList<String> fundamentos = new ArrayList<>();
        String esfera = jurisdicao != null && jurisdicao.getEsfera() != null ? jurisdicao.getEsfera().name() : processo.getTipoJustica() != null ? processo.getTipoJustica().name() : null;
        String materia = jurisdicao != null && jurisdicao.getMateria() != null ? jurisdicao.getMateria().name() : processo.getMateria() != null ? processo.getMateria().name() : processo.getRamoDireito() != null ? processo.getRamoDireito().name() : null;
        String grau = jurisdicao != null && jurisdicao.getGrau() != null ? jurisdicao.getGrau().name() : unificado.competencia().grauJurisdicao();
        String tribunal = unificado.competencia().tribunalCodigo();
        String unidade = unificado.competencia().unidadeJudiciariaSugerida();
        String uf = jurisdicao != null ? firstNonBlank(jurisdicao.getUf(), unificado.identity().uf()) : unificado.identity().uf();
        String comarca = jurisdicao != null ? firstNonBlank(jurisdicao.getCidade(), unificado.identity().comarca()) : unificado.identity().comarca();
        String foro = jurisdicao != null ? firstNonBlank(jurisdicao.getForo(), comarca) : comarca;
        boolean admiteAudienceDelegado = operacaoPolicialSigilosa || isPenalSensitive(processo);
        boolean admiteSegredoEstado = segredoEstadoPresumido || isFederalOuSuperior(processo) || containsSensitiveNationalKeywords(processo);
        String anelInstitucional = resolveAnelInstitucional(processo, segredoEstadoPresumido, operacaoPolicialSigilosa);
        fundamentos.add("Esfera=" + firstNonBlank(esfera, "N/D") + "; matéria=" + firstNonBlank(materia, "N/D") + "; grau=" + firstNonBlank(grau, "N/D"));
        fundamentos.add("Anel institucional calculado: " + anelInstitucional + ".");
        if (admiteAudienceDelegado) {
            fundamentos.add("A matéria penal sensível admite audiência institucional de delegado em janela controlada.");
        }
        if (admiteSegredoEstado) {
            fundamentos.add("A combinação de jurisdição, matéria e sinais semânticos autoriza trilha de revisão para eventual segredo de Estado.");
        }
        return new ProcessoSigiloJurisdicaoBridge(
                esfera,
                grau,
                materia,
                tribunal,
                unidade,
                uf,
                comarca,
                foro,
                anelInstitucional,
                admiteSegredoEstado,
                true,
                admiteAudienceDelegado,
                List.copyOf(fundamentos)
        );
    }

    private List<ProcessoSigiloProtecaoDado> buildProtecoesDados(Processo processo,
                                                                 boolean protecaoDocumentalReforcada,
                                                                 boolean operacaoPolicialSigilosa) {
        ArrayList<ProcessoSigiloProtecaoDado> protecoes = new ArrayList<>();
        if (protecaoDocumentalReforcada) {
            if (hasDocument(processo.getParteAutoraCpf())) {
                protecoes.add(new ProcessoSigiloProtecaoDado(
                        "parteAutoraCpf",
                        "PESSOAL_IDENTIFICAVEL",
                        operacaoPolicialSigilosa ? "MASCARAR_E_LIMITAR_EXPORTACAO" : "MASCARAR",
                        safeMask(processo.getParteAutoraCpf()),
                        true,
                        "CPF da parte autora deve sair mascarado em caixa, timeline pública e integrações externas."
                ));
            }
            if (hasDocument(processo.getParteReuCpf())) {
                protecoes.add(new ProcessoSigiloProtecaoDado(
                        "parteReuCpf",
                        "PESSOAL_IDENTIFICAVEL",
                        operacaoPolicialSigilosa ? "MASCARAR_E_BLOQUEAR_REPLICACAO" : "MASCARAR",
                        safeMask(processo.getParteReuCpf()),
                        true,
                        "CPF da parte ré deve permanecer mascarado para perfis não credenciados."
                ));
            }
            protecoes.add(new ProcessoSigiloProtecaoDado(
                    "resumosPublicos",
                    operacaoPolicialSigilosa ? "OPERACIONAL_SENSIVEL" : "LGPD_SENSIVEL",
                    operacaoPolicialSigilosa ? "MINIMIZAR_DADOS_E_BLOQUEAR_BUSCA_EXTERNA" : "REDAZIR_IDENTIFICADORES",
                    null,
                    true,
                    "Resumos públicos e notificações devem expor apenas o mínimo necessário."
            ));
        }
        return List.copyOf(protecoes);
    }

    private List<ProcessoSigiloDestinatario> buildDestinatarios(Processo processo,
                                                                NivelSigilo nivelRecomendado,
                                                                ProcessoSigiloJurisdicaoBridge bridge,
                                                                boolean operacaoPolicialSigilosa) {
        ArrayList<ProcessoSigiloDestinatario> destinatarios = new ArrayList<>();
        for (Usuario usuario : usuariosAtivosNoEscopo(bridge.comarca())) {
            if (usuario == null || !usuario.isAtivoESemanticoValido()) {
                continue;
            }
            TipoUsuario tipo = usuario.getTipoUsuario();
            if (tipo == null) {
                continue;
            }
            if (tipo.isMagistratura()) {
                destinatarios.add(new ProcessoSigiloDestinatario(
                        usuario.getId(),
                        "MAGISTRADO_NATURAL",
                        "Magistrado natural do processo",
                        tipo.name(),
                        usuario.getNome(),
                        canaisPara(tipo, true),
                        false,
                        true,
                        nivelRecomendado.exigeCredencial(),
                        "Somente magistrado pode decretar ou manter a classificação sigilosa sugerida."
                ));
                continue;
            }
            if (operacaoPolicialSigilosa) {
                if (isDelegadoCompativel(tipo, processo)) {
                    destinatarios.add(new ProcessoSigiloDestinatario(
                            usuario.getId(),
                            "DELEGADO_OPERACAO",
                            "Delegado em janela sigilosa controlada",
                            tipo.name(),
                            usuario.getNome(),
                            canaisPara(tipo, true),
                            true,
                            true,
                            true,
                            "Operação policial sensível admite ciência inicial apenas para delegado e magistrado em regime de necessidade de conhecer."
                    ));
                }
                continue;
            }
            if (tipo.isMinisterioPublico() && isMateriaComInstituicaoEssencial(processo)) {
                destinatarios.add(new ProcessoSigiloDestinatario(
                        usuario.getId(),
                        "MINISTERIO_PUBLICO",
                        "Ministério Público institucional",
                        tipo.name(),
                        usuario.getNome(),
                        canaisPara(tipo, false),
                        true,
                        nivelRecomendado.getNivel() >= NivelSigilo.SIGILO_N2.getNivel(),
                        nivelRecomendado.exigeCredencial(),
                        "Fluxos sensíveis podem exigir ciência do Ministério Público em ambiente controlado."
                ));
                continue;
            }
            if (tipo.isDefensoriaPublica() && isMateriaComInstituicaoEssencial(processo)) {
                destinatarios.add(new ProcessoSigiloDestinatario(
                        usuario.getId(),
                        "DEFENSORIA_PUBLICA",
                        "Defensoria Pública institucional",
                        tipo.name(),
                        usuario.getNome(),
                        canaisPara(tipo, false),
                        true,
                        nivelRecomendado.getNivel() >= NivelSigilo.SIGILO_N2.getNivel(),
                        nivelRecomendado.exigeCredencial(),
                        "Fluxos sensíveis podem exigir ciência da Defensoria em ambiente controlado."
                ));
                continue;
            }
            if (tipo.isServidorJudiciario() && nivelRecomendado.getNivel() >= NivelSigilo.SEGREDO_JUSTICA.getNivel()) {
                destinatarios.add(new ProcessoSigiloDestinatario(
                        usuario.getId(),
                        "SERVIDOR_FORO_CREDENCIADO",
                        "Servidor do foro credenciado",
                        tipo.name(),
                        usuario.getNome(),
                        canaisPara(tipo, false),
                        true,
                        true,
                        nivelRecomendado.exigeCredencial(),
                        "Servidores do foro entram apenas para fila interna, caixa institucional e preparação operacional do ambiente sigiloso."
                ));
            }
        }
        if (!operacaoPolicialSigilosa && processo.getUsuario() != null && processo.getUsuario().isAdvogado()) {
            Usuario advogado = processo.getUsuario();
            destinatarios.add(new ProcessoSigiloDestinatario(
                    advogado.getId(),
                    "ADVOGADO_VINCULADO",
                    "Advogado vinculado ao processo",
                    advogado.getTipoUsuario() != null ? advogado.getTipoUsuario().name() : null,
                    advogado.getNome(),
                    canaisPara(advogado.getTipoUsuario(), false),
                    true,
                    nivelRecomendado.getNivel() >= NivelSigilo.SIGILO_N2.getNivel(),
                    nivelRecomendado.exigeCredencial(),
                    "A advocacia vinculada deve ser comunicada sobre restrição, sem substituir a decretação judicial."
            ));
        }
        destinatarios.sort(Comparator.comparing(ProcessoSigiloDestinatario::audienceCode)
                .thenComparing(destinatario -> destinatario.nome() == null ? "" : destinatario.nome()));
        return List.copyOf(deduplicate(destinatarios));
    }

    private List<Usuario> usuariosAtivosNoEscopo(String comarca) {
        List<Usuario> candidatos = comarca == null || comarca.isBlank()
                ? usuarioRepository.findAll()
                : usuarioRepository.findByComarcaAndAtivoTrue(comarca);
        return candidatos.stream()
                .filter(Objects::nonNull)
                .filter(Usuario::isAtivoESemanticoValido)
                .toList();
    }

    private String resolveStatus(NivelSigilo nivelAtual,
                                 NivelSigilo nivelRecomendado,
                                 boolean segredoEstadoPresumido,
                                 boolean operacaoPolicialSigilosa) {
        if (segredoEstadoPresumido) {
            return "REVISAO_PARA_SEGREDO_ESTADO";
        }
        if (operacaoPolicialSigilosa && nivelRecomendado.getNivel() >= NivelSigilo.SIGILO_N4.getNivel()) {
            return "RESTRICAO_OPERACIONAL_IMEDIATA";
        }
        if (nivelRecomendado.getNivel() > nivelAtual.getNivel()) {
            return "RECLASSIFICACAO_JUDICIAL_OBRIGATORIA";
        }
        return "MANTER_CLASSIFICACAO";
    }

    private String resolveAudienceMode(boolean operacaoPolicialSigilosa, NivelSigilo nivelRecomendado) {
        if (operacaoPolicialSigilosa) {
            return "JUIZ_E_DELEGADO";
        }
        if (nivelRecomendado == NivelSigilo.SEGREDO_ESTADO) {
            return "ANEL_INSTITUCIONAL_MAXIMO";
        }
        if (nivelRecomendado.getNivel() >= NivelSigilo.SIGILO_N2.getNivel()) {
            return "JUIZ_PARTES_E_INSTITUICOES_CREDENCIADAS";
        }
        return "PADRAO_COM_GUARDAS";
    }

    private boolean isDelegadoCompativel(TipoUsuario tipoUsuario, Processo processo) {
        if (tipoUsuario == null) {
            return false;
        }
        if (processo.getTipoJustica() == TipoJustica.FEDERAL || containsFederalPoliceKeywords(processo)) {
            return tipoUsuario == TipoUsuario.DELEGADO_POLICIA_FEDERAL;
        }
        return tipoUsuario == TipoUsuario.DELEGADO_POLICIA || tipoUsuario == TipoUsuario.DELEGADO_POLICIA_FEDERAL;
    }

    private boolean isMateriaComInstituicaoEssencial(Processo processo) {
        String corpus = corpus(processo);
        return corpus.contains("PENAL")
                || corpus.contains("INFANCIA")
                || corpus.contains("VIOLENCIA DOMESTICA")
                || corpus.contains("MEDIDA PROTETIVA")
                || corpus.contains("FAMILIA");
    }

    private boolean isOperacaoPolicialSigilosa(Processo processo) {
        String corpus = corpus(processo);
        boolean operacao = corpus.contains("OPERACAO") || corpus.contains("FLAGRANTE") || corpus.contains("INTERCEPTACAO") || corpus.contains("ORGANIZACAO CRIMINOSA");
        boolean policial = corpus.contains("POLICIA") || corpus.contains("DELEGADO") || corpus.contains("INQUERITO") || corpus.contains("INTELIGENCIA");
        boolean penal = isPenalSensitive(processo);
        return operacao && (policial || penal);
    }

    private boolean suggestsSegredoEstado(Processo processo, boolean operacaoPolicialSigilosa) {
        return containsSensitiveNationalKeywords(processo)
                || (operacaoPolicialSigilosa && isFederalOuSuperior(processo) && containsFederalPoliceKeywords(processo));
    }

    private boolean containsSensitiveNationalKeywords(Processo processo) {
        String corpus = corpus(processo);
        return corpus.contains("SEGREDO DE ESTADO")
                || corpus.contains("SEGURANCA NACIONAL")
                || corpus.contains("INFORMACAO CLASSIFICADA")
                || corpus.contains("CONTRAINTELIGENCIA")
                || corpus.contains("ABIN")
                || corpus.contains("TERRORISMO")
                || corpus.contains("DEFESA NACIONAL");
    }

    private boolean containsFederalPoliceKeywords(Processo processo) {
        String corpus = corpus(processo);
        return corpus.contains("POLICIA FEDERAL") || corpus.contains("PF ") || corpus.endsWith(" PF") || corpus.contains("SUPERINTENDENCIA REGIONAL");
    }

    private boolean isFederalOuSuperior(Processo processo) {
        return processo.getTipoJustica() == TipoJustica.FEDERAL
                || processo.getTipoJustica() == TipoJustica.SUPERIOR
                || (processo.getJurisdicao() != null && processouEsferaFederalOuEspecializada(processo.getJurisdicao()));
    }

    private boolean processouEsferaFederalOuEspecializada(Jurisdicao jurisdicao) {
        if (jurisdicao == null || jurisdicao.getEsfera() == null) {
            return false;
        }
        String esfera = jurisdicao.getEsfera().name();
        return esfera.contains("FEDERAL") || esfera.contains("ELEITORAL") || esfera.contains("MILITAR");
    }

    private boolean isPenalSensitive(Processo processo) {
        String corpus = corpus(processo);
        return corpus.contains("PENAL")
                || corpus.contains("CRIMINAL")
                || corpus.contains("INQUERITO")
                || corpus.contains("CUSTODIA")
                || corpus.contains("FLAGRANTE");
    }

    private String resolveAnelInstitucional(Processo processo,
                                            boolean segredoEstadoPresumido,
                                            boolean operacaoPolicialSigilosa) {
        if (segredoEstadoPresumido) {
            return "INSTITUCIONAL_MAXIMO";
        }
        if (operacaoPolicialSigilosa) {
            return "OPERACIONAL_RESTRITO";
        }
        if (isMateriaComInstituicaoEssencial(processo)) {
            return "INSTITUCIONAL_REFORCADO";
        }
        return "PADRAO";
    }

    private boolean hasPersonalDocuments(Processo processo) {
        return hasDocument(processo.getParteAutoraCpf()) || hasDocument(processo.getParteReuCpf());
    }

    private boolean hasDocument(String raw) {
        if (raw == null || raw.isBlank()) {
            return false;
        }
        String normalized = documentoNacionalValidator.normalizarDocumento(raw);
        return normalized.length() == 11 || normalized.length() == 14;
    }

    private String safeMask(String raw) {
        try {
            return documentoNacionalValidator.mascararDocumento(raw);
        } catch (Exception ex) {
            String normalized = documentoNacionalValidator.normalizarDocumento(raw);
            if (normalized.length() > 4) {
                return "***" + normalized.substring(normalized.length() - 4);
            }
            return "***";
        }
    }

    private List<String> canaisPara(TipoUsuario tipoUsuario, boolean critico) {
        LinkedHashSet<String> canais = new LinkedHashSet<>();
        canais.add("PUSH_PJB");
        canais.add("CAIXA_PJB");
        canais.add("EMAIL");
        if (critico || tipoUsuario != null && (tipoUsuario.isMagistratura() || tipoUsuario.isSegurancaPublica())) {
            canais.add("WHATSAPP");
        }
        if (tipoUsuario != null && (tipoUsuario.isMinisterioPublico() || tipoUsuario.isDefensoriaPublica() || tipoUsuario.isServidorJudiciario())) {
            canais.add("CAIXA_INSTITUCIONAL");
        }
        return List.copyOf(canais);
    }

    private List<ProcessoSigiloDestinatario> deduplicate(List<ProcessoSigiloDestinatario> destinatarios) {
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        ArrayList<ProcessoSigiloDestinatario> out = new ArrayList<>();
        for (ProcessoSigiloDestinatario destinatario : destinatarios) {
            String key = destinatario.usuarioId() + ":" + destinatario.audienceCode() + ":" + firstNonBlank(destinatario.tipoUsuario(), "SEM_TIPO");
            if (seen.add(key)) {
                out.add(destinatario);
            }
        }
        return List.copyOf(out);
    }

    private String corpus(Processo processo) {
        return normalize(String.join(" ", List.of(
                safe(processo.getClasseProcessual()),
                safe(processo.getAssunto()),
                safe(processo.getObjetoProcessual()),
                safe(processo.getPedidoPrincipal()),
                safe(processo.getPedidosConsolidados()),
                safe(processo.getMaterialProbatorioResumo()),
                safe(processo.getResumoIA()),
                processo.getMateria() != null ? processo.getMateria().name() : "",
                processo.getRamoDireito() != null ? processo.getRamoDireito().name() : "",
                processo.getTipoJustica() != null ? processo.getTipoJustica().name() : ""
        )));
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^A-Za-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim()
                .toUpperCase(Locale.ROOT);
        return normalized;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private int severityRank(ProcessoSigiloFinding finding) {
        return switch (finding.severity()) {
            case "CRITICAL" -> 0;
            case "ALTA", "ELEVADA" -> 1;
            case "MEDIA" -> 2;
            default -> 3;
        };
    }

    private String firstNonBlank(String first, String fallback) {
        return first != null && !first.isBlank() ? first : fallback;
    }
}
