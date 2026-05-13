package com.tcc.pjb.backend.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.tcc.pjb.backend.configs.EquipeContexto;
import com.tcc.pjb.backend.core.kernel.advisory.ProcessMaterialDossierReport;
import com.tcc.pjb.backend.core.kernel.advisory.ProcessMaterialDossierService;
import com.tcc.pjb.backend.core.kernel.advisory.ProcessMaterialStrategyReport;
import com.tcc.pjb.backend.core.kernel.advisory.ProcessMaterialStrategyService;
import com.tcc.pjb.backend.core.kernel.advisory.SettlementAdvisoryReport;
import com.tcc.pjb.backend.core.kernel.advisory.SettlementAdvisoryService;
import com.tcc.pjb.backend.core.kernel.advisory.SettlementIntelligenceService;
import com.tcc.pjb.backend.core.procedural.ProceduralRitoNames;
import com.tcc.pjb.backend.core.util.PayloadMaps;
import com.tcc.pjb.backend.inovacao.batna.FacilitadorBatnaService;
import com.tcc.pjb.backend.model.dto.IaSettings;
import com.tcc.pjb.backend.model.dto.intelligence.JudgeAgreementApprovalPromptResponse;
import com.tcc.pjb.backend.model.dto.intelligence.JudgeAgreementDecisionResponse;
import com.tcc.pjb.backend.model.dto.PdfGenerationResult;
import com.tcc.pjb.backend.model.dto.PropostaFinanceiraDTO;
import com.tcc.pjb.backend.service.ProfileEngine;
import com.tcc.pjb.backend.model.dto.SavedAudit;
import com.tcc.pjb.backend.model.entity.AcordoHomologado;
import com.tcc.pjb.backend.model.dto.AcordoHomologadoEvent;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.Competencia;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.EsferaJurisdicao;
import com.tcc.pjb.backend.model.entity.Jurisdicao;
import com.tcc.pjb.backend.model.entity.MembroEquipe;
import com.tcc.pjb.backend.model.entity.ModeloContrato;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Profile;
import com.tcc.pjb.backend.model.entity.PropostaAcordo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.MateriaJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.PapelEquipe;
import com.tcc.pjb.backend.model.entity.enums.StatusAcordo;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.repository.AcordoHomologadoRepository;
import com.tcc.pjb.backend.model.repository.ModeloContratoRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.PropostaAcordoRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.engine.FinancialValidatorEngine;
import com.tcc.pjb.backend.service.engine.LegalRhythmEngine;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.exception.RegraNegocioException;
import com.tcc.pjb.backend.service.notification.NotificationService;
import com.tcc.pjb.backend.service.rito.ProcessoRitoSnapshotService;
import com.tcc.pjb.backend.service.ui.UiHistoryService;
import com.tcc.pjb.backend.service.intelligence.AgreementChatLedgerService;
import com.tcc.pjb.backend.service.intelligence.JudgeAgreementApprovalService;
import com.tcc.pjb.backend.service.intelligence.ProcessOutcomePredictionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AcordoService {

    private final ProcessoRepository processoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ModeloContratoRepository modeloContratoRepository;
    private final PropostaAcordoRepository propostaAcordoRepository;
    private final com.tcc.pjb.backend.model.repository.ChatMensagemRepository chatMensagemRepository;
    private final AcordoHomologadoRepository acordoHomologadoRepository;

    private final ProfileEngine profileEngine;
    private final PdfGeneratorService pdfGeneratorService;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final DomainEventPublisher domainEventPublisher;
    private final ChatService chatService;
    private final AcordoSuggestionPipelineAsyncService suggestionPipeline;

    private final UiHistoryService uiHistoryService;
    private final ProcessoRitoSnapshotService processoRitoSnapshotService;
    private final SettlementIntelligenceService settlementIntelligenceService;
    private final ProcessMaterialDossierService processMaterialDossierService;
    private final ProcessMaterialStrategyService processMaterialStrategyService;
    private final SettlementAdvisoryService settlementAdvisoryService;
    private final ProcessOutcomePredictionService processOutcomePredictionService;
    private final JudgeAgreementApprovalService judgeAgreementApprovalService;
    private final WorkItemRepository workItemRepository;
    private final AgreementChatLedgerService agreementChatLedgerService;

    private final LegalRhythmEngine rhythmEngine;
    private final FinancialValidatorEngine financialEngine;
    private final FacilitadorBatnaService facilitadorBatnaService;

    @Transactional(readOnly = true)
    public List<ModeloContrato> sugerirModelosInteligentes(Long processoId) {
        Processo processo = buscarProcesso(processoId);
        MateriaJurisdicao materia = processo.getJurisdicao().getMateria();
        FaseProcessual fase = processo.getFaseAtual();

        if (materia == MateriaJurisdicao.PENAL && !EquipeContexto.isUsuarioMinisterioPublico()) {
            log.warn("Tentativa de acesso a modelos penais por usuário sem perfil MP.");
        }

        return modeloContratoRepository.findSmartModels(materia, fase, processo.getValorCausa());
    }

    @Transactional
    public PropostaAcordo criarPropostaUnificada(Long processoId, Long modeloId, IaSettings settings, PropostaFinanceiraDTO dadosFinanceiros) {
        MembroEquipe membro = validarPermissoesCriacao();
        Processo processo = buscarProcesso(processoId);

        if (rhythmEngine.isConclusoParaSentenca(processo)) {
            chatService.postarMensagemSistema(processo, buscarUsuarioSistema(), "⚠️ ATENÇÃO: Processo concluso para sentença. O acordo deve ser protocolado com urgência para evitar julgamento simultâneo.");
        }

        if (dadosFinanceiros != null) {
            financialEngine.validarViabilidade(dadosFinanceiros, processo.getValorCausa());
        }

        ModeloContrato modelo = modeloContratoRepository.findById(modeloId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Modelo", modeloId));

        ProcessMaterialDossierReport materialDossier = processMaterialDossierService.analyzeProcess(processo, buildNegotiationSignals(processo));
        ProcessMaterialStrategyReport materialStrategy = processMaterialStrategyService.analyzeProcess(processo, materialDossier, buildNegotiationSignals(processo));
        var negotiationWindow = settlementIntelligenceService.analyze(
                processo,
                dadosFinanceiros != null ? dadosFinanceiros.getValorTotal() : null,
                mergeNegotiationSignals(processo, materialDossier, materialStrategy)
        );
        SettlementAdvisoryReport settlementAdvisory = settlementAdvisoryService.analyze(
                processo,
                processoRitoSnapshotService.resolve(processo, null).ritoCode(),
                dadosFinanceiros != null ? dadosFinanceiros.getValorTotal() : null,
                mergeNegotiationSignals(processo, materialDossier, materialStrategy),
                null
        );

        Profile profile = profileEngine.loadProfile(processo.getModulo() != null ? processo.getModulo().name() : null, processo.getJurisdicao(), mapCompetencia(processo));
        String termosHtml = preencherTemplateAvancado(modelo.getTemplateHtml(), processo, dadosFinanceiros);

        PropostaAcordo proposta = PropostaAcordo.builder()
                .processo(processo)
                .proponente(membro.getUsuario())
                .equipe(membro.getEquipe())
                .termosHtml(termosHtml)
                .status(definirStatusInicial(membro))
                .settings(settings)
                .profileId(profile.getId())
                .valorAcordo(dadosFinanceiros != null ? dadosFinanceiros.getValorTotal() : BigDecimal.ZERO)
                .build();

        proposta = propostaAcordoRepository.save(proposta);

        if (!negotiationWindow.risks().isEmpty()) {
            chatService.postarMensagemSistema(processo, "Janela negocial assistida detectou riscos relevantes: " + String.join(" | ", negotiationWindow.risks()));
        }
        if (!settlementAdvisory.executionSafeguards().isEmpty()) {
            chatService.postarMensagemSistema(processo, "Estrutura recomendada para o acordo: " + String.join(" | ", settlementAdvisory.executionSafeguards()));
        }
        if (!settlementAdvisory.conditionalClauses().isEmpty()) {
            chatService.postarMensagemSistema(processo, "Cláusulas condicionais sugeridas: " + String.join(" | ", settlementAdvisory.conditionalClauses()));
        }
        postMaterialStrategySignals(processo, materialStrategy);

        try {
            facilitadorBatnaService.gerarParaProcessoEProposta(
                    processo.getId(),
                    proposta.getId(),
                    dadosFinanceiros != null ? dadosFinanceiros.getValorTotal() : null,
                    false
            );
        } catch (RuntimeException ex) {
            log.warn("BATNA nao gerado na criacao da proposta | processo={} proposta={} err={}", processo.getId(), proposta.getId(), ex.getMessage());
        }

        scheduleSuggestionPipelineAfterCommit(proposta.getId());
        return proposta;
    }

    private void scheduleSuggestionPipelineAfterCommit(Long propostaId) {
        if (propostaId == null) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            suggestionPipeline.runForProposal(propostaId);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                suggestionPipeline.runForProposal(propostaId);
            }
        });
    }


    @Transactional
    public JudgeAgreementApprovalPromptResponse solicitarHomologacaoJudicial(Long propostaId, String resumoExecutivo) {
        validarPermissoesCriacao();
        PropostaAcordo proposta = propostaAcordoRepository.findById(propostaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Proposta", propostaId));
        Processo processo = proposta.getProcesso();
        if (processo == null) {
            throw new IllegalStateException("Proposta sem processo vinculado.");
        }
        if (proposta.getStatus() == StatusAcordo.HOMOLOGADO) {
            throw new RegraNegocioException("Fluxo inválido: proposta já homologada.");
        }
        if (proposta.getStatus() == StatusAcordo.REJEITADO_PELO_JUIZ) {
            throw new RegraNegocioException("Fluxo inválido: proposta rejeitada exige revisão antes de nova submissão.");
        }
        ProcessMaterialDossierReport materialDossier = processMaterialDossierService.analyzeProcess(processo, buildNegotiationSignals(processo));
        ProcessMaterialStrategyReport materialStrategy = processMaterialStrategyService.analyzeProcess(processo, materialDossier, buildNegotiationSignals(processo));
        SettlementAdvisoryReport settlementAdvisory = settlementAdvisoryService.analyze(
                processo,
                processoRitoSnapshotService.resolve(processo, null).ritoCode(),
                proposta.getValorAcordo(),
                mergeNegotiationSignals(processo, materialDossier, materialStrategy),
                null
        );
        proposta.setStatus(StatusAcordo.AGUARDANDO_HOMOLOGACAO_JUIZ);
        propostaAcordoRepository.save(proposta);
        JudgeAgreementApprovalPromptResponse prompt = judgeAgreementApprovalService.requestApproval(
                processo,
                proposta,
                settlementAdvisory,
                processOutcomePredictionService.analyze(processo),
                resumoExecutivo
        );
        chatService.postarMensagemSistema(processo, buscarUsuarioSistema(), "A proposta foi encaminhada ao gabinete para apreciação judicial do acordo.");
        AgreementChatLedgerService.RoundSnapshot snapshot = agreementChatLedgerService.nextRoundSnapshot(chatMensagemRepository.findByProcesso_IdOrderByDataEnvioAsc(processo.getId()), "homologação judicial do acordo");
        chatService.postarMensagemSistema(processo, agreementChatLedgerService.renderRoundSystemMessage(new AgreementChatLedgerService.RoundSnapshot(snapshot.round(), snapshot.version(), "SUBMISSAO_JUDICIAL")));
        return prompt;
    }

    @Transactional
    public AcordoHomologado homologarAcordoJudicial(Long propostaId, String hashAssinaturaJuiz) {
        validarPermissaoJuiz();

        PropostaAcordo proposta = propostaAcordoRepository.findById(propostaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Proposta", propostaId));

        if (proposta.getStatus() != StatusAcordo.AGUARDANDO_HOMOLOGACAO_JUIZ) {
            throw new RegraNegocioException("Fluxo inválido: Proposta não está na mesa do Juiz.");
        }

        Usuario juiz = EquipeContexto.getMembroDaEquipeAtiva().getUsuario();
        Processo processo = proposta.getProcesso();

        PdfGenerationResult pdf = pdfGeneratorService.generatePdfWithSealAndQr(
                proposta.getTermosHtml(),
                proposta.getUuid(),
                java.util.Map.of(
                        "processo", java.util.Objects.toString(processo.getNumeroUnificado(), ""),
                        "juiz", java.util.Objects.toString(juiz.getNome(), ""),
                        "data_homologacao", LocalDateTime.now().toString()
                )
        );

        AcordoHomologado acordo = AcordoHomologado.builder()
                .proposta(proposta)
                .processo(processo)
                .juiz(juiz)
                .urlPdfHomologado(pdf.getUrl())
                .hashAssinaturaJuiz(hashAssinaturaJuiz)
                .hashAssinaturaParte1(proposta.getHashAssinaturaParte1())
                .hashAssinaturaParte2(proposta.getHashAssinaturaParte2())
                .dataHomologacao(LocalDateTime.now())
                .build();

        AcordoHomologado salvo = acordoHomologadoRepository.save(acordo);

        StatusProcesso fromStatus = processo.getStatusProcesso();
        String fromResultado = processo.getResultadoFinal();

        processo.setStatusProcesso(StatusProcesso.JULGADO);
        processo.setResultadoFinal("ACORDO_HOMOLOGADO");
        Processo savedProcesso = processoRepository.save(processo);

        uiHistoryService.recordProcessoStatusChange(
                savedProcesso,
                fromStatus,
                fromResultado,
                StatusProcesso.JULGADO,
                "ACORDO_HOMOLOGADO",
                "Acordo homologado judicialmente"
        );

        proposta.setStatus(StatusAcordo.HOMOLOGADO);
        propostaAcordoRepository.save(proposta);
        finalizarWorkItemHomologacao(processo, proposta, "HOMOLOGAR", true);
        chatService.postarMensagemSistema(processo, agreementChatLedgerService.renderDecisionMessage("HOMOLOGAR", "Homologação judicial concluída com assinatura e conversão do acordo em título executivo judicial."));

        auditService.recordAgreementHomologation(proposta.getUuid(), new SavedAudit(proposta, salvo, pdf));
        domainEventPublisher.publish(new AcordoHomologadoEvent(processo.getId(), proposta.getUuid(), juiz.getId()));
        notificarPartesEnvolvidas(processo, juiz, pdf);
        return salvo;
    }


    @Transactional
    public JudgeAgreementDecisionResponse decidirHomologacaoJudicial(Long propostaId, String action, String justification, String hashAssinaturaJuiz, boolean notifyParties) {
        validarPermissaoJuiz();
        PropostaAcordo proposta = propostaAcordoRepository.findById(propostaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Proposta", propostaId));
        Processo processo = proposta.getProcesso();
        if (processo == null) {
            throw new IllegalStateException("Proposta sem processo vinculado.");
        }
        if (proposta.getStatus() != StatusAcordo.AGUARDANDO_HOMOLOGACAO_JUIZ) {
            throw new RegraNegocioException("Fluxo inválido: a proposta não está aguardando decisão judicial.");
        }
        String normalizedAction = action == null ? "" : action.trim().toUpperCase(java.util.Locale.ROOT);
        if (normalizedAction.isBlank()) {
            throw new RegraNegocioException("Ação judicial do acordo é obrigatória.");
        }
        if ("HOMOLOGAR".equals(normalizedAction)) {
            homologarAcordoJudicial(propostaId, hashAssinaturaJuiz);
            List<String> notified = notifyParties ? notificarPartesDecisao(processo, normalizedAction, justification) : List.of();
            return new JudgeAgreementDecisionResponse(
                    processo.getId(),
                    proposta.getId(),
                    normalizedAction,
                    StatusAcordo.HOMOLOGADO.name(),
                    "CONCLUIDO",
                    "Publicar sentença homologatória e acompanhar eventual cumprimento do acordo.",
                    agreementChatLedgerService.renderDecisionMessage(normalizedAction, justification),
                    notified
            );
        }
        if ("DEVOLVER_PARA_REVISAO".equals(normalizedAction)) {
            proposta.setStatus(StatusAcordo.AGUARDANDO_REVISAO_HUMANA);
            propostaAcordoRepository.save(proposta);
            finalizarWorkItemHomologacao(processo, proposta, normalizedAction, true);
            String message = agreementChatLedgerService.renderDecisionMessage(normalizedAction, justification);
            chatService.postarMensagemSistema(processo, message);
            List<String> notified = notifyParties ? notificarPartesDecisao(processo, normalizedAction, justification) : List.of();
            return new JudgeAgreementDecisionResponse(
                    processo.getId(),
                    proposta.getId(),
                    normalizedAction,
                    proposta.getStatus().name(),
                    "CONCLUIDO",
                    "Reabrir rodada negocial controlada e consolidar nova versão da minuta.",
                    message,
                    notified
            );
        }
        if ("REJEITAR".equals(normalizedAction)) {
            proposta.setStatus(StatusAcordo.REJEITADO_PELO_JUIZ);
            propostaAcordoRepository.save(proposta);
            finalizarWorkItemHomologacao(processo, proposta, normalizedAction, false);
            String message = agreementChatLedgerService.renderDecisionMessage(normalizedAction, justification);
            chatService.postarMensagemSistema(processo, message);
            List<String> notified = notifyParties ? notificarPartesDecisao(processo, normalizedAction, justification) : List.of();
            return new JudgeAgreementDecisionResponse(
                    processo.getId(),
                    proposta.getId(),
                    normalizedAction,
                    proposta.getStatus().name(),
                    "CANCELADO",
                    "Encerrar a tentativa atual de acordo ou abrir nova negociação com base material distinta.",
                    message,
                    notified
            );
        }
        throw new RegraNegocioException("Ação judicial do acordo inválida: " + normalizedAction);
    }

    @Transactional(readOnly = true)
    public Integer calcularPrazoResposta(Long processoId) {
        Processo processo = buscarProcesso(processoId);
        Jurisdicao jurisdicao = processo.getJurisdicao();
        String ritoName = processoRitoSnapshotService.resolve(processo, null).ritoCode();
        if (ProceduralRitoNames.isOneOf(ritoName, "JUIZADO_ESPECIAL", "JUIZADO_ESPECIAL_CIVEL")) {
            return Optional.ofNullable(jurisdicao.getPrazoRespostaDiasJE()).orElse(10);
        }
        if (rhythmEngine.isPeriodoSuspensao(jurisdicao)) {
            return 0;
        }
        return Optional.ofNullable(jurisdicao.getPrazoComumDias()).orElse(15);
    }

    private List<String> buildNegotiationSignals(Processo processo) {
        List<String> signals = new java.util.ArrayList<>();
        if (processo == null) {
            return List.of();
        }
        if (processo.getFaseAtual() != null) {
            signals.add("Fase atual: " + processo.getFaseAtual().name());
        }
        if (processo.getStatusProcesso() != null) {
            signals.add("Status do processo: " + processo.getStatusProcesso().name());
        }
        if (processo.getObjetoProcessual() != null && !processo.getObjetoProcessual().isBlank()) {
            signals.add("Objeto processual: " + processo.getObjetoProcessual().trim());
        }
        if (processo.getPedidoPrincipal() != null && !processo.getPedidoPrincipal().isBlank()) {
            signals.add("Pedido principal: " + processo.getPedidoPrincipal().trim());
        }
        if (processo.getMaterialProbatorioResumo() != null && !processo.getMaterialProbatorioResumo().isBlank()) {
            signals.add("Base probatória: " + compact(processo.getMaterialProbatorioResumo(), 220));
        }
        if (processo.getJanelaAcordoResumo() != null && !processo.getJanelaAcordoResumo().isBlank()) {
            signals.add(processo.getJanelaAcordoResumo().trim());
        }
        if (processo.getPotencialAcordoScore() != null) {
            signals.add("Potencial negocial: " + processo.getPotencialAcordoScore() + "/100");
        }
        if (processo.getResultadoFinal() != null && !processo.getResultadoFinal().isBlank()) {
            signals.add(processo.getResultadoFinal().trim());
        }
        return List.copyOf(signals);
    }

    private List<String> mergeNegotiationSignals(Processo processo, ProcessMaterialDossierReport materialDossier, ProcessMaterialStrategyReport materialStrategy) {
        LinkedHashSet<String> merged = new LinkedHashSet<>(buildNegotiationSignals(processo));
        if (materialDossier != null) {
            merged.addAll(materialDossier.evidenceAnchors());
            merged.addAll(materialDossier.settlementLevers());
            merged.addAll(materialDossier.proofGaps());
        }
        if (materialStrategy != null) {
            merged.add(materialStrategy.litigationPosture());
            merged.add(materialStrategy.protocolReadiness());
            merged.add(materialStrategy.negotiationStance());
            merged.addAll(materialStrategy.protocolBlockers());
            merged.addAll(materialStrategy.negotiationGuardrails());
            merged.addAll(materialStrategy.controlPoints());
        }
        merged.removeIf(s -> s == null || s.isBlank());
        return List.copyOf(merged);
    }



    private void postMaterialStrategySignals(Processo processo, ProcessMaterialStrategyReport materialStrategy) {
        if (processo == null || materialStrategy == null) {
            return;
        }
        LinkedHashSet<String> postureSignals = new LinkedHashSet<>();
        postureSignals.add("Postura: " + materialStrategy.litigationPosture());
        postureSignals.add("Prontidão protocolar: " + materialStrategy.protocolReadiness());
        postureSignals.add("Postura negocial: " + materialStrategy.negotiationStance());
        postureSignals.add("Maturidade probatória: " + materialStrategy.evidenceReadiness());
        materialStrategy.protocolBlockers().stream().limit(2).forEach(postureSignals::add);
        materialStrategy.negotiationGuardrails().stream().limit(2).forEach(postureSignals::add);
        postureSignals.removeIf(s -> s == null || s.isBlank());
        if (!postureSignals.isEmpty()) {
            chatService.postarMensagemSistema(processo, "Matriz material estratégica: " + String.join(" | ", postureSignals));
        }
        if (!materialStrategy.executionChecklist().isEmpty()) {
            chatService.postarMensagemSistema(processo, "Checklist executivo do caso: " + String.join(" | ", materialStrategy.executionChecklist().stream().limit(4).toList()));
        }
    }

    private MembroEquipe validarPermissoesCriacao() {
        MembroEquipe membro = EquipeContexto.getMembroDaEquipeAtiva();
        if (membro == null) {
            throw new SecurityException("Sem contexto de equipe.");
        }
        return membro;
    }

    private void validarPermissaoJuiz() {
        MembroEquipe membro = EquipeContexto.getMembroDaEquipeAtiva();
        if (membro == null || membro.getPapel() != PapelEquipe.JUIZ_GABINETE) {
            throw new SecurityException("Acesso negado: requer privilegios de magistrado.");
        }
    }

    private StatusAcordo definirStatusInicial(MembroEquipe membro) {
        return membro.getPapel() == PapelEquipe.ESTAGIARIO ? StatusAcordo.RASCUNHO : StatusAcordo.EM_NEGOCIACAO;
    }

    private String preencherTemplateAvancado(String html, Processo processo, PropostaFinanceiraDTO financeiro) {
        if (html == null) {
            return "";
        }
        String base = html
                .replace("[NUMERO_PROCESSO]", java.util.Objects.toString(processo.getNumeroUnificado(), ""))
                .replace("[JURISDICAO]", processo.getJurisdicao() == null ? "" : java.util.Objects.toString(processo.getJurisdicao().getNome(), ""))
                .replace("[PARTES_QUALIFICACAO]", gerarQualificacaoCompleta(processo));
        if (financeiro != null) {
            base = base
                    .replace("[VALOR_TOTAL]", java.util.Objects.toString(financeiro.getValorTotalFormatado(), ""))
                    .replace("[QTD_PARCELAS]", String.valueOf(financeiro.getParcelas()))
                    .replace("[MULTA_INADIMPLEMENTO]", financeiro.getMultaPercentual() + "%");
        }
        return base;
    }

    private String gerarQualificacaoCompleta(Processo processo) {
        return "Autor: " + java.util.Objects.toString(processo.getParteAutoraNome(), "") + " vs Réu: " + java.util.Objects.toString(processo.getParteReuNome(), "");
    }

    private void notificarPartesEnvolvidas(Processo processo, Usuario juiz, PdfGenerationResult pdf) {
        chatService.postarMensagemSistema(processo, buscarUsuarioSistema(), "⚖️ Sentença Homologatória proferida. Processo extinto com resolução de mérito.");
        notificationService.notifyJudge(juiz, processo, "Homologação Realizada", "Acordo selado no processo " + java.util.Objects.toString(processo.getNumeroUnificado(), ""), pdf.getUrl());
        notificationService.notifyLawyers(processo, "Acordo Homologado", pdf.getUrl());
    }

    private Processo buscarProcesso(Long id) {
        return processoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", id));
    }


    private void finalizarWorkItemHomologacao(Processo processo, PropostaAcordo proposta, String action, boolean conclude) {
        if (processo == null || processo.getId() == null) {
            return;
        }
        String templateCode = "ACORDO:HOMOLOGACAO_JUDICIAL_PROMPT:" + (proposta != null && proposta.getId() != null ? proposta.getId() : processo.getId());
        Optional<com.tcc.pjb.backend.model.entity.workflow.WorkItem> direct = workItemRepository.findFirstByProcesso_IdAndTemplateCodeAndStatusNot(
                processo.getId(),
                templateCode,
                com.tcc.pjb.backend.model.entity.enums.WorkItemStatus.CANCELADO
        );
        if (direct == null) {
            direct = Optional.empty();
        }
        List<com.tcc.pjb.backend.model.entity.workflow.WorkItem> processItems = workItemRepository.findAllByProcesso(processo.getId());
        if (processItems == null) {
            processItems = List.of();
        }
        List<com.tcc.pjb.backend.model.entity.workflow.WorkItem> stableProcessItems = processItems;
        direct.or(() -> stableProcessItems.stream()
                        .filter(item -> item.getTitulo() != null && item.getTitulo().toUpperCase(java.util.Locale.ROOT).contains("ACORDO"))
                        .findFirst())
                .ifPresent(item -> {
                    item.setBlocking(false);
                    item.setDescricao((item.getDescricao() == null ? "" : item.getDescricao() + " ; ") + "decisaoJudicial=" + action);
                    item.setStatus(conclude ? com.tcc.pjb.backend.model.entity.enums.WorkItemStatus.CONCLUIDO : com.tcc.pjb.backend.model.entity.enums.WorkItemStatus.CANCELADO);
                    workItemRepository.save(item);
                });
    }

    private List<String> notificarPartesDecisao(Processo processo, String action, String justification) {
        Usuario sistema = buscarUsuarioSistema();
        String title = "Decisão judicial no fluxo do acordo";
        String message = agreementChatLedgerService.renderDecisionMessage(action, justification);
        LinkedHashSet<String> notified = new LinkedHashSet<>();
        notificationService.notifyUser(sistema, processo, title, message, "/api/v1/processos/" + processo.getId() + "/acordo/intelligence");
        notificationService.notifyLawyers(processo, title, "/api/v1/processos/" + processo.getId() + "/acordo/intelligence");
        notified.add(sistema.getEmail());
        notified.add("ADVOGADOS_DO_PROCESSO");
        return List.copyOf(notified);
    }

    private Usuario buscarUsuarioSistema() {
        return usuarioRepository.findByTipoUsuario(com.tcc.pjb.backend.model.entity.enums.TipoUsuario.ADMINISTRADOR)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Usuário ADMINISTRADOR do sistema não configurado."));
    }

    private static Competencia mapCompetencia(Processo processo) {
        if (processo == null || processo.getJurisdicao() == null) {
            return Competencia.ESTADUAL;
        }
        return mapCompetencia(processo.getJurisdicao().getEsfera());
    }

    private static Competencia mapCompetencia(EsferaJurisdicao esfera) {
        if (esfera == null) {
            return Competencia.ESTADUAL;
        }
        return switch (esfera) {
            case JUSTICA_FEDERAL -> Competencia.FEDERAL;
            case JUSTICA_TRABALHO -> Competencia.TRABALHISTA;
            case JUSTICA_ELEITORAL -> Competencia.ELEITORAL;
            case JUSTICA_MILITAR -> Competencia.MILITAR;
            case JUSTICA_ESTADUAL -> Competencia.ESTADUAL;
            default -> Competencia.TRIBUNAL_SUPERIOR;
        };
    }

    private static String compact(String value, int max) {
        if (value == null) {
            return null;
        }
        String cleaned = value.replaceAll("\\s+", " ").trim();
        if (cleaned.length() <= max) {
            return cleaned;
        }
        return cleaned.substring(0, Math.max(0, max));
    }
}
