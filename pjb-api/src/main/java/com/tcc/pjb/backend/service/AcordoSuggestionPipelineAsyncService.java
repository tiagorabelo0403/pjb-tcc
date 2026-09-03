package com.tcc.pjb.backend.service;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.ai.orchestrator.IAOrchestrator;
import com.tcc.pjb.backend.core.kernel.governance.ProcessIntelligenceSnapshotService;
import com.tcc.pjb.backend.model.dto.EssenceResult;
import com.tcc.pjb.backend.model.dto.IARunResult;
import com.tcc.pjb.backend.model.dto.IaSettings;
import com.tcc.pjb.backend.model.entity.ChatMensagem;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Profile;
import com.tcc.pjb.backend.model.entity.PropostaAcordo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.StatusAcordo;
import com.tcc.pjb.backend.model.repository.ChatMensagemRepository;
import com.tcc.pjb.backend.model.repository.PropostaAcordoRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.rito.ProcessoRitoSnapshotService;
import com.tcc.pjb.backend.platform.runtime.execution.PjbExecutionDescriptor;
import com.tcc.pjb.backend.platform.runtime.execution.PjbTransactionalExecutionSupport;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AcordoSuggestionPipelineAsyncService {

    private static final Duration ACORDO_PIPELINE_TIMEOUT = Duration.ofMinutes(2);
    private static final Duration ACORDO_PIPELINE_READ_BUDGET = Duration.ofSeconds(6);
    private static final Duration ACORDO_PIPELINE_WRITE_BUDGET = Duration.ofSeconds(8);

    private final PropostaAcordoRepository propostaAcordoRepository;
    private final UsuarioRepository usuarioRepository;
    private final IAOrchestrator iaOrchestrator;
    private final EssenceFilter essenceFilter;
    private final ProfileEngine profileEngine;
    private final AuditService auditService;
    private final ChatService chatService;
    private final ProcessoRitoSnapshotService processoRitoSnapshotService;
    private final AcordoNegotiationAdvisoryPipelineService negotiationAdvisoryPipelineService;
    private final ProcessIntelligenceSnapshotService processIntelligenceSnapshotService;
    private final ChatMensagemRepository chatMensagemRepository;
    private final PjbTransactionalExecutionSupport transactionalExecutionSupport;

    public AcordoSuggestionPipelineAsyncService(PropostaAcordoRepository propostaAcordoRepository,
                                               UsuarioRepository usuarioRepository,
                                               IAOrchestrator iaOrchestrator,
                                               EssenceFilter essenceFilter,
                                               ProfileEngine profileEngine,
                                               AuditService auditService,
                                               ChatService chatService,
                                               ProcessoRitoSnapshotService processoRitoSnapshotService,
                                               AcordoNegotiationAdvisoryPipelineService negotiationAdvisoryPipelineService,
                                               ProcessIntelligenceSnapshotService processIntelligenceSnapshotService,
                                               ChatMensagemRepository chatMensagemRepository,
                                               PjbTransactionalExecutionSupport transactionalExecutionSupport) {
        this.propostaAcordoRepository = Objects.requireNonNull(propostaAcordoRepository, "propostaAcordoRepository");
        this.usuarioRepository = Objects.requireNonNull(usuarioRepository, "usuarioRepository");
        this.iaOrchestrator = Objects.requireNonNull(iaOrchestrator, "iaOrchestrator");
        this.essenceFilter = Objects.requireNonNull(essenceFilter, "essenceFilter");
        this.profileEngine = Objects.requireNonNull(profileEngine, "profileEngine");
        this.auditService = Objects.requireNonNull(auditService, "auditService");
        this.chatService = Objects.requireNonNull(chatService, "chatService");
        this.processoRitoSnapshotService = Objects.requireNonNull(processoRitoSnapshotService, "processoRitoSnapshotService");
        this.negotiationAdvisoryPipelineService = Objects.requireNonNull(negotiationAdvisoryPipelineService, "negotiationAdvisoryPipelineService");
        this.processIntelligenceSnapshotService = Objects.requireNonNull(processIntelligenceSnapshotService, "processIntelligenceSnapshotService");
        this.chatMensagemRepository = Objects.requireNonNull(chatMensagemRepository, "chatMensagemRepository");
        this.transactionalExecutionSupport = Objects.requireNonNull(transactionalExecutionSupport, "transactionalExecutionSupport");
    }

    public void runForProposal(Long propostaId) {
        if (propostaId == null) {
            return;
        }
        transactionalExecutionSupport.run(
                PjbExecutionDescriptor.job("acordo-suggestion-pipeline.run-for-proposal", ACORDO_PIPELINE_TIMEOUT),
                () -> runForProposalInternal(propostaId)
        );
    }

    private void runForProposalInternal(Long propostaId) {
        if (propostaId == null) {
            return;
        }
        AcordoPipelineSeed seed = transactionalExecutionSupport.executeReadOnly(
                "acordo-suggestion-pipeline.load-seed",
                ACORDO_PIPELINE_READ_BUDGET,
                () -> loadPipelineSeed(propostaId)
        );
        if (seed == null) {
            return;
        }

        Profile profile = profileEngine.loadProfile(
                seed.processo().getModulo() != null ? seed.processo().getModulo().name() : null,
                seed.processo().getJurisdicao(),
                mapCompetencia(seed.processo())
        );

        IARunResult run = gerarRunSafeDeAcordo(seed.processo(), seed.proposta(), seed.settings(), profile);
        EssenceResult decision = essenceFilter.evaluate(seed.proposta().getTermosHtml(), run.getHtml());
        boolean contemClausulasAbusivas = essenceFilter.detectAbusiveClauses(run.getHtml());
        AcordoPipelineOutcome outcome = buildOutcome(run, decision, contemClausulasAbusivas);
        AcordoNegotiationAdvisoryPipelineService.AcordoPipelineAnalysis analysis = negotiationAdvisoryPipelineService.analyze(
                seed.processo(), seed.proposta(), seed.ritoName(), seed.recentChat());

        transactionalExecutionSupport.executeInNewTransaction(
                "acordo-suggestion-pipeline.persist-outcome",
                ACORDO_PIPELINE_WRITE_BUDGET,
                () -> persistPipelineOutcome(seed, outcome, analysis)
        );
    }

    private AcordoPipelineSeed loadPipelineSeed(Long propostaId) {
        PropostaAcordo proposta = propostaAcordoRepository.findById(propostaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Proposta", propostaId));
        IaSettings settings = proposta.getSettings();
        if (settings == null || !settings.isSuggestionsEnabled()) {
            return null;
        }
        Processo processo = proposta.getProcesso();
        if (processo == null) {
            return null;
        }
        warmDetachedReferences(processo, proposta, settings);
        String ritoName = processoRitoSnapshotService.resolve(processo, null).ritoCode();
        List<ChatMensagem> recentChat = recentChat(processo.getId());
        return new AcordoPipelineSeed(proposta.getId(), processo.getId(), proposta, processo, settings, ritoName, recentChat);
    }

    private AcordoPipelineOutcome buildOutcome(IARunResult run, EssenceResult decision, boolean contemClausulasAbusivas) {
        boolean revisaoHumana = !decision.isEssencePreserved() || contemClausulasAbusivas;
        String motivo = !decision.isEssencePreserved()
                ? "Alteração de essência detectada."
                : contemClausulasAbusivas ? "Cláusulas potencialmente abusivas identificadas." : null;
        StatusAcordo status = revisaoHumana ? StatusAcordo.AGUARDANDO_REVISAO_HUMANA : StatusAcordo.EM_NEGOCIACAO;
        String mensagem = revisaoHumana
                ? "Alerta: " + motivo + " Revisão obrigatória."
                : "Sugestões de acordo geradas e aplicadas.";
        return new AcordoPipelineOutcome(run, decision, contemClausulasAbusivas, revisaoHumana, status, motivo, mensagem);
    }


    private void persistPipelineOutcome(AcordoPipelineSeed seed,
                                        AcordoPipelineOutcome outcome,
                                        AcordoNegotiationAdvisoryPipelineService.AcordoPipelineAnalysis analysis) {
        PropostaAcordo proposta = propostaAcordoRepository.findById(seed.propostaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Proposta", seed.propostaId()));
        Processo processo = proposta.getProcesso();
        if (processo == null) {
            throw new IllegalStateException("Processo não encontrado para proposta " + seed.propostaId());
        }
        Usuario usuarioSistema = buscarUsuarioSistema();
        auditService.recordIARun(proposta.getUuid(), outcome.run(), outcome.decision());
        proposta.setIarunId(outcome.run().getRunId());
        proposta.setStatus(outcome.status());
        if (outcome.revisaoHumana()) {
            chatService.postarMensagemSistema(processo, usuarioSistema, outcome.mensagemSistema());
        } else {
            proposta.setTermosHtml(outcome.run().getHtml());
            chatService.postarMensagemSistema(processo, usuarioSistema, outcome.mensagemSistema());
        }
        propostaAcordoRepository.save(proposta);
        processIntelligenceSnapshotService.saveProcessSnapshot(processo, analysis.strategicFocus(), analysis.institutionalPolicySnapshot(), analysis.kernelRiskEscalation(), analysis.governedMessageDecision());
        processIntelligenceSnapshotService.saveNegotiationRound(processo, proposta, usuarioSistema, analysis.governedMessageDecision(), analysis.strategicFocus(), analysis.suggestedNextMessage());
        chatService.postarMensagemSistema(processo, usuarioSistema, analysis.intelligenceDigest());
    }

    private void warmDetachedReferences(Processo processo, PropostaAcordo proposta, IaSettings settings) {
        processo.getId();
        if (processo.getModulo() != null) {
            processo.getModulo().name();
        }
        if (processo.getJurisdicao() != null && processo.getJurisdicao().getEsfera() != null) {
            processo.getJurisdicao().getEsfera().name();
        }
        if (processo.getFaseAtual() != null) {
            processo.getFaseAtual().name();
        }
        if (processo.getStatusProcesso() != null) {
            processo.getStatusProcesso().name();
        }
        proposta.getId();
        proposta.getUuid();
        proposta.getValorAcordo();
        proposta.getTermosHtml();
        settings.isSuggestionsEnabled();
    }

    private List<ChatMensagem> recentChat(Long processoId) {
        return chatMensagemRepository.findTop80ByProcesso_IdOrderByDataEnvioDesc(processoId).stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(ChatMensagem::getDataEnvio, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(20)
                .toList();
    }

    private IARunResult gerarRunSafeDeAcordo(Processo processo, PropostaAcordo proposta, IaSettings settings, Profile profile) {
        try {
            var req = com.tcc.pjb.backend.ai.contract.IARequest.builder()
                    .origem("ACORDO")
                    .acao("GERAR_ACORDO")
                    .payload("processoId", processo.getId())
                    .payload("termosHtml", proposta.getTermosHtml())
                    .payload("profile", profile != null ? profile.getNome() : null)
                    .payload("suggestionsEnabled", settings.isSuggestionsEnabled())
                    .build();

            var resp = iaOrchestrator.processar(req);
            String html = resp != null && resp.getTexto() != null && !resp.getTexto().isBlank()
                    ? resp.getTexto()
                    : proposta.getTermosHtml();

            return new IARunResult(java.util.UUID.randomUUID(), html, 0.55, "LOW_RISK");
        } catch (Exception e) {
            log.warn("Orquestrador indisponível para acordo; seguindo com fallback seguro. processoId={}", processo.getId(), e);
            return new IARunResult(java.util.UUID.randomUUID(), proposta.getTermosHtml(), 0.40, "FALLBACK");
        }
    }


    private Usuario buscarUsuarioSistema() {
        return usuarioRepository.findByTipoUsuario(com.tcc.pjb.backend.model.entity.enums.TipoUsuario.ADMINISTRADOR)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Usuário ADMINISTRADOR do sistema não configurado."));
    }

    private static com.tcc.pjb.backend.model.entity.enums.jurisdicao.Competencia mapCompetencia(Processo processo) {
        if (processo == null || processo.getJurisdicao() == null) {
            return com.tcc.pjb.backend.model.entity.enums.jurisdicao.Competencia.ESTADUAL;
        }
        return mapCompetencia(processo.getJurisdicao().getEsfera());
    }

    private record AcordoPipelineSeed(Long propostaId,
                                      Long processoId,
                                      PropostaAcordo proposta,
                                      Processo processo,
                                      IaSettings settings,
                                      String ritoName,
                                      List<ChatMensagem> recentChat) {
    }

    private record AcordoPipelineOutcome(IARunResult run,
                                         EssenceResult decision,
                                         boolean contemClausulasAbusivas,
                                         boolean revisaoHumana,
                                         StatusAcordo status,
                                         String motivo,
                                         String mensagemSistema) {
    }


    private static com.tcc.pjb.backend.model.entity.enums.jurisdicao.Competencia mapCompetencia(com.tcc.pjb.backend.model.entity.enums.jurisdicao.EsferaJurisdicao esfera) {
        if (esfera == null) {
            return com.tcc.pjb.backend.model.entity.enums.jurisdicao.Competencia.ESTADUAL;
        }
        return switch (esfera) {
            case JUSTICA_FEDERAL -> com.tcc.pjb.backend.model.entity.enums.jurisdicao.Competencia.FEDERAL;
            case JUSTICA_TRABALHO -> com.tcc.pjb.backend.model.entity.enums.jurisdicao.Competencia.TRABALHISTA;
            case JUSTICA_ELEITORAL -> com.tcc.pjb.backend.model.entity.enums.jurisdicao.Competencia.ELEITORAL;
            case JUSTICA_MILITAR -> com.tcc.pjb.backend.model.entity.enums.jurisdicao.Competencia.MILITAR;
            case JUSTICA_ESTADUAL -> com.tcc.pjb.backend.model.entity.enums.jurisdicao.Competencia.ESTADUAL;
            default -> com.tcc.pjb.backend.model.entity.enums.jurisdicao.Competencia.TRIBUNAL_SUPERIOR;
        };
    }
}
