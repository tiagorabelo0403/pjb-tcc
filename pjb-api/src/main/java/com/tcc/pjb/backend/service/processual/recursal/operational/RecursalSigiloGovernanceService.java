package com.tcc.pjb.backend.service.processual.recursal.operational;

import com.tcc.pjb.backend.core.processo.sigilo.application.ProcessoSigiloApplicationService;
import com.tcc.pjb.backend.core.processo.sigilo.application.ProcessoSigiloInteligenteApplicationService;
import com.tcc.pjb.backend.core.processo.sigilo.application.ProcessoSigiloNotificacaoApplicationService;
import com.tcc.pjb.backend.core.processo.sigilo.domain.ProcessoSigiloAggregate;
import com.tcc.pjb.backend.core.processo.sigilo.domain.ProcessoSigiloInteligenteAggregate;
import com.tcc.pjb.backend.core.processo.sigilo.domain.ProcessoSigiloNotificacaoAggregate;
import com.tcc.pjb.backend.core.processo.sigilo.domain.ProcessoSigiloNotificacaoItem;
import com.tcc.pjb.backend.core.processo.sigilo.domain.ProcessoSigiloFinding;
import com.tcc.pjb.backend.core.processo.sigilo.domain.ProcessoSigiloGuarda;
import com.tcc.pjb.backend.core.kernel.recursal.LegalAppealType;
import com.tcc.pjb.backend.model.dto.processual.recursal.admissibilidade.RecursalAdmissibilityResponse;
import com.tcc.pjb.backend.model.dto.processual.recursal.ia.RecursalIaConferenciaResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.service.SigiloService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class RecursalSigiloGovernanceService {

    private final ProcessoSigiloApplicationService processoSigiloApplicationService;
    private final ProcessoSigiloInteligenteApplicationService processoSigiloInteligenteApplicationService;
    private final ProcessoSigiloNotificacaoApplicationService processoSigiloNotificacaoApplicationService;
    private final SigiloService sigiloService;

    public RecursalSigiloGovernanceService(ProcessoSigiloApplicationService processoSigiloApplicationService,
                                           ProcessoSigiloInteligenteApplicationService processoSigiloInteligenteApplicationService,
                                           ProcessoSigiloNotificacaoApplicationService processoSigiloNotificacaoApplicationService,
                                           SigiloService sigiloService) {
        this.processoSigiloApplicationService = Objects.requireNonNull(processoSigiloApplicationService);
        this.processoSigiloInteligenteApplicationService = Objects.requireNonNull(processoSigiloInteligenteApplicationService);
        this.processoSigiloNotificacaoApplicationService = Objects.requireNonNull(processoSigiloNotificacaoApplicationService);
        this.sigiloService = Objects.requireNonNull(sigiloService);
    }

    public Map<String, Object> avaliar(Processo processo,
                                       LegalAppealType appealType,
                                       String razoes,
                                       String fundamentacao,
                                       String observacoes,
                                       RecursalAdmissibilityResponse admissibility,
                                       RecursalIaConferenciaResponse aiReview) {
        if (processo == null || processo.getId() == null || appealType == null) {
            return Map.of();
        }
        ProcessoSigiloAggregate base = safeBase(processo.getId());
        ProcessoSigiloInteligenteAggregate inteligente = safeInteligente(processo.getId());
        ProcessoSigiloNotificacaoAggregate notificacoes = safeNotificacoes(processo.getId());
        SigiloService.SigiloDecision decisaoTexto = sigiloService.avaliarCorpus(buildCorpus(processo, appealType, razoes, fundamentacao, observacoes, aiReview));

        NivelSigilo nivelAtual = resolveNivelAtual(processo, base, inteligente);
        NivelSigilo nivelRecomendado = maxNivel(
                nivelAtual,
                base == null ? null : base.nivelSigilo(),
                inteligente == null ? null : inteligente.nivelRecomendado(),
                decisaoTexto == null ? null : decisaoTexto.nivel(),
                nivelEspecialRecursal(appealType, decisaoTexto, processo)
        );

        boolean herdaSigiloProcesso = nivelAtual.getNivel() >= NivelSigilo.SEGREDO_JUSTICA.getNivel();
        boolean revisaoJudicialObrigatoria = (inteligente != null && inteligente.revisaoJudicialObrigatoria()) || nivelRecomendado.getNivel() > nivelAtual.getNivel();
        boolean decretoExclusivoMagistrado = inteligente == null || inteligente.decretoExclusivoMagistrado() || revisaoJudicialObrigatoria;
        boolean recursoSigiloso = nivelRecomendado.getNivel() >= NivelSigilo.SEGREDO_JUSTICA.getNivel();
        boolean segredoEstadoEmRevisao = nivelRecomendado == NivelSigilo.SEGREDO_ESTADO;
        boolean embargosAtivos = isEmbargos(appealType);
        boolean embargosSigilosos = embargosAtivos && recursoSigiloso;
        boolean contraditorioControlado = recursoSigiloso || (admissibility != null && admissibility.stepUpRequired());
        boolean stepUpAcessoRecurso = (base != null && base.exigeStepUp()) || (admissibility != null && admissibility.stepUpRequired()) || nivelRecomendado.exigeCredencial();
        boolean certificadoOuCredencialReforcada = (admissibility != null && admissibility.certificateRequired()) || nivelRecomendado.getNivel() >= NivelSigilo.SIGILO_N3.getNivel();
        boolean mascaraDados = (inteligente != null && inteligente.protecaoDocumentalReforcada()) || hasSensitiveSignals(decisaoTexto);

        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        fundamentos.add("O sigilo recursal herda o envelope do processo e pode ser reforçado quando as razões, a fundamentação, os anexos ou os embargos introduzem material sensível novo.");
        fundamentos.add("Somente magistrado decreta segredo de justiça reforçado ou segredo de Estado; a automação apenas recomenda, restringe circulação e notifica.");
        if (base != null) {
            mergeStrings(fundamentos, base.fundamentos());
        }
        if (inteligente != null) {
            mergeStrings(fundamentos, inteligente.fundamentos());
        }
        if (decisaoTexto != null) {
            mergeStrings(fundamentos, decisaoTexto.recomendacoes());
        }
        if (admissibility != null && admissibility.fundamentos() != null) {
            mergeStrings(fundamentos, admissibility.fundamentos());
        }

        LinkedHashSet<String> acoes = new LinkedHashSet<>();
        if (revisaoJudicialObrigatoria) {
            acoes.add("ABRIR_REVISAO_JUDICIAL_DE_SIGILO_RECURSAL");
        }
        if (contraditorioControlado) {
            acoes.add("CONTROLAR_CONTRARRAZOES_E_INTIMACOES_RECURSAIS");
        }
        if (mascaraDados) {
            acoes.add("MASCARAR_IDENTIFICADORES_E_DADOS_SENSIVEIS_DA_PECA");
        }
        if (stepUpAcessoRecurso) {
            acoes.add("EXIGIR_STEP_UP_PARA_LEITURA_E_PROTOCOLO");
        }
        if (certificadoOuCredencialReforcada) {
            acoes.add("EXIGIR_CREDENCIAL_REFORCADA_NA_ASSINATURA");
        }
        if (segredoEstadoEmRevisao) {
            acoes.add("ACIONAR_GUARDA_MAXIMA_E_DUPLA_AUTORIZACAO");
        }

        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", resolveStatus(nivelAtual, nivelRecomendado, revisaoJudicialObrigatoria, segredoEstadoEmRevisao));
        out.put("tipoRecursoCanonico", appealType.name());
        out.put("nivelAtual", nivelAtual.name());
        out.put("nivelRecomendado", nivelRecomendado.name());
        out.put("herdaSigiloProcesso", herdaSigiloProcesso);
        out.put("recursoSigiloso", recursoSigiloso);
        out.put("segredoJustica", recursoSigiloso && !segredoEstadoEmRevisao);
        out.put("segredoEstadoEmRevisao", segredoEstadoEmRevisao);
        out.put("revisaoJudicialObrigatoria", revisaoJudicialObrigatoria);
        out.put("decretoExclusivoMagistrado", decretoExclusivoMagistrado);
        out.put("embargosSigilosos", embargosSigilosos);
        out.put("contrarrazoesControladas", contraditorioControlado);
        out.put("workspaceLeituraModo", contraditorioControlado ? "RESTRITO" : "PADRAO");
        out.put("protocolSubmissionMode", contraditorioControlado ? "RESTRITO_COM_JUSTIFICATIVA" : "PADRAO");
        out.put("stepUpAcessoRecurso", stepUpAcessoRecurso);
        out.put("certificateOrStrongCredentialRequired", certificadoOuCredencialReforcada);
        out.put("mascaramentoObrigatorio", mascaraDados);
        out.put("protecaoDocumentalReforcada", inteligente != null && inteligente.protecaoDocumentalReforcada());
        put(out, "audienceMode", inteligente == null ? null : inteligente.audienceMode());
        put(out, "disclosureMode", base == null ? null : base.disclosureMode());
        if (decisaoTexto != null) {
            out.put("scoreSemanticoRecursal", decisaoTexto.score());
            out.put("signals", decisaoTexto.signals().stream().map(Enum::name).toList());
            out.put("recomendacoesSemanticas", decisaoTexto.recomendacoes());
        }
        if (base != null) {
            out.put("guardas", base.guardas().stream().limit(6).map(this::mapGuarda).toList());
            out.put("findings", base.findings().stream().limit(6).map(this::mapFinding).toList());
            out.put("allowedDirectProfiles", base.allowedDirectProfiles());
        }
        if (inteligente != null) {
            out.put("triggers", inteligente.triggers());
            out.put("destinatarios", inteligente.destinatarios().stream().limit(8).map(item -> {
                LinkedHashMap<String, Object> map = new LinkedHashMap<>();
                put(map, "usuarioId", item.usuarioId());
                put(map, "audienceCode", item.audienceCode());
                put(map, "audienceLabel", item.audienceLabel());
                put(map, "tipoUsuario", item.tipoUsuario());
                put(map, "nomeUsuario", item.nome());
                map.put("channels", item.channels());
                map.put("stepUpRequired", item.exigeStepUp());
                map.put("credencialRequired", item.exigeCredencial());
                put(map, "rationale", item.rationale());
                return Map.copyOf(map);
            }).toList());
        }
        if (notificacoes != null) {
            LinkedHashMap<String, Object> planejamento = new LinkedHashMap<>();
            put(planejamento, "status", notificacoes.statusPlanejamento());
            planejamento.put("total", notificacoes.totalDestinatarios());
            planejamento.put("altaPrioridade", notificacoes.totalAltaPrioridade());
            planejamento.put("canais", notificacoes.channels());
            planejamento.put("itens", notificacoes.notificacoes().stream().limit(6).map(this::mapNotificacao).toList());
            out.put("notificacoesPlanejadas", Map.copyOf(planejamento));
        }
        out.put("acoes", List.copyOf(acoes));
        out.put("fundamentos", List.copyOf(fundamentos.stream().filter(Objects::nonNull).map(String::trim).filter(item -> !item.isBlank()).toList()));
        return Collections.unmodifiableMap(out);
    }

    private ProcessoSigiloAggregate safeBase(Long processoId) {
        try {
            return processoSigiloApplicationService.detalhar(processoId);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private ProcessoSigiloInteligenteAggregate safeInteligente(Long processoId) {
        try {
            return processoSigiloInteligenteApplicationService.avaliar(processoId);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private ProcessoSigiloNotificacaoAggregate safeNotificacoes(Long processoId) {
        try {
            return processoSigiloNotificacaoApplicationService.planejar(processoId);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private String buildCorpus(Processo processo,
                               LegalAppealType appealType,
                               String razoes,
                               String fundamentacao,
                               String observacoes,
                               RecursalIaConferenciaResponse aiReview) {
        ArrayList<String> parts = new ArrayList<>();
        add(parts, appealType.name());
        add(parts, processo.getClasseProcessual());
        add(parts, processo.getAssunto());
        add(parts, processo.getObjetoProcessual());
        add(parts, processo.getResumoIA());
        add(parts, processo.getAnaliseTriagemV1());
        add(parts, processo.getMaterialProbatorioResumo());
        add(parts, razoes);
        add(parts, fundamentacao);
        add(parts, observacoes);
        if (aiReview != null && aiReview.analiseEstruturada() != null) {
            add(parts, String.join(" ", aiReview.analiseEstruturada().riscosAnulacao()));
            add(parts, String.join(" ", aiReview.analiseEstruturada().fundamentosEstruturais()));
            add(parts, String.join(" ", aiReview.analiseEstruturada().checklistBlindagem()));
        }
        return String.join(" ", parts);
    }

    private NivelSigilo resolveNivelAtual(Processo processo,
                                          ProcessoSigiloAggregate base,
                                          ProcessoSigiloInteligenteAggregate inteligente) {
        if (inteligente != null && inteligente.nivelAtual() != null) {
            return inteligente.nivelAtual();
        }
        if (base != null && base.nivelSigilo() != null) {
            return base.nivelSigilo();
        }
        return processo.getNivelSigilo() == null ? NivelSigilo.PUBLICO : processo.getNivelSigilo();
    }

    private NivelSigilo nivelEspecialRecursal(LegalAppealType appealType,
                                              SigiloService.SigiloDecision decisaoTexto,
                                              Processo processo) {
        if (appealType == LegalAppealType.EMBARGOS_DECLARACAO && processo.getNivelSigilo() != null && processo.getNivelSigilo().getNivel() >= NivelSigilo.SEGREDO_JUSTICA.getNivel()) {
            return processo.getNivelSigilo();
        }
        if ((appealType == LegalAppealType.RE || appealType == LegalAppealType.RESP || appealType == LegalAppealType.RECLAMACAO_CONSTITUCIONAL)
                && decisaoTexto != null
                && decisaoTexto.signals().contains(SigiloService.SigiloSignal.PENAL_SENSIVEL)) {
            return NivelSigilo.SIGILO_N4;
        }
        return null;
    }

    private NivelSigilo maxNivel(NivelSigilo... values) {
        NivelSigilo winner = NivelSigilo.PUBLICO;
        if (values == null) {
            return winner;
        }
        for (NivelSigilo value : values) {
            if (value != null && value.getNivel() > winner.getNivel()) {
                winner = value;
            }
        }
        return winner;
    }

    private boolean isEmbargos(LegalAppealType appealType) {
        return appealType == LegalAppealType.EMBARGOS_DECLARACAO
                || appealType == LegalAppealType.EMBARGOS_EXECUCAO
                || appealType == LegalAppealType.EMBARGOS_EXECUCAO_FISCAL
                || appealType == LegalAppealType.EMBARGOS_INFRINGENTES
                || appealType == LegalAppealType.EMBARGOS_TERCEIRO;
    }

    private boolean hasSensitiveSignals(SigiloService.SigiloDecision decisaoTexto) {
        return decisaoTexto != null && !decisaoTexto.signals().isEmpty();
    }

    private String resolveStatus(NivelSigilo atual,
                                 NivelSigilo recomendado,
                                 boolean revisaoJudicialObrigatoria,
                                 boolean segredoEstadoEmRevisao) {
        if (segredoEstadoEmRevisao) {
            return "SEGREDO_ESTADO_EM_REVISAO";
        }
        if (revisaoJudicialObrigatoria && recomendado.getNivel() > atual.getNivel()) {
            return "RECLASSIFICACAO_RECURSAL_SUGERIDA";
        }
        if (recomendado.getNivel() >= NivelSigilo.SEGREDO_JUSTICA.getNivel()) {
            return "SIGILO_RECURSAL_ATIVO";
        }
        return "PUBLICIDADE_CONTROLADA";
    }

    private Map<String, Object> mapGuarda(ProcessoSigiloGuarda guarda) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        put(map, "codigo", guarda.code());
        put(map, "titulo", guarda.title());
        put(map, "familia", guarda.scope());
        put(map, "criticidade", guarda.severity());
        map.put("ativo", guarda.mandatory());
        map.put("duplaAutorizacao", guarda.blocking());
        map.put("stepUpObrigatorio", guarda.requiresInstitutionalContext());
        map.put("perfisAutorizados", guarda.allowedModes());
        map.put("preCondicoes", guarda.requiredCapabilities());
        return Map.copyOf(map);
    }

    private Map<String, Object> mapFinding(ProcessoSigiloFinding finding) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        put(map, "code", finding.code());
        put(map, "title", finding.title());
        put(map, "severity", finding.severity());
        map.put("blocking", finding.blocking());
        put(map, "description", finding.detail());
        map.put("suggestedActions", finding.correctiveActions());
        return Map.copyOf(map);
    }

    private Map<String, Object> mapNotificacao(ProcessoSigiloNotificacaoItem item) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        put(map, "usuarioId", item.usuarioId());
        put(map, "audienceCode", item.audienceCode());
        put(map, "audienceLabel", item.audienceLabel());
        map.put("channels", item.channels());
        map.put("highPriority", item.highPriority());
        put(map, "title", item.title());
        put(map, "action", item.action());
        put(map, "deepLink", item.deepLink());
        put(map, "rationale", item.rationale());
        return Map.copyOf(map);
    }

    private void mergeStrings(LinkedHashSet<String> target, List<String> values) {
        if (target == null || values == null) {
            return;
        }
        for (String value : values) {
            if (value == null) {
                continue;
            }
            String normalized = value.trim();
            if (!normalized.isBlank()) {
                target.add(normalized);
            }
        }
    }

    private void add(List<String> parts, String value) {
        if (value == null) {
            return;
        }
        String normalized = value.trim();
        if (!normalized.isBlank()) {
            parts.add(normalized);
        }
    }

    private void put(Map<String, Object> target, String key, Object value) {
        if (target != null && key != null && value != null) {
            target.put(key, value);
        }
    }
}
