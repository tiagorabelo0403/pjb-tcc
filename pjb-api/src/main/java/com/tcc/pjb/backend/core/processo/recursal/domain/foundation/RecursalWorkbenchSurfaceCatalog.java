package com.tcc.pjb.backend.core.processo.recursal.domain.foundation;

import com.tcc.pjb.backend.core.operational.OperationalApiRoutes;
import java.util.LinkedHashMap;
import java.util.Map;

public final class RecursalWorkbenchSurfaceCatalog {

    private RecursalWorkbenchSurfaceCatalog() {
    }



    public static String citizenOwnProcesses() {
        return "/api/v1/cidadao/processos";
    }

    public static String citizenFolderProcesses() {
        return "/api/v1/cidadao/pasta/processos";
    }

    public static String personalOwnProcesses() {
        return "/api/v1/processos/pessoais/meus-processos";
    }

    public static String citizenProcessOverview() {
        return "/api/v1/cidadao/processos/{processoId}/overview";
    }

    public static String personalProcessOverview() {
        return "/api/v1/processos/pessoais/{processoId}/overview";
    }

    public static String citizenInstancias() {
        return "/api/v1/cidadao/processos/{processoId}/instancias";
    }

    public static String citizenJulgamentos() {
        return "/api/v1/cidadao/processos/{processoId}/julgamentos";
    }

    public static String citizenJulgamentoDetail() {
        return "/api/v1/cidadao/julgamentos/{julgamentoId}";
    }

    public static String citizenJulgamentoVotesStream() {
        return "/api/v1/cidadao/julgamentos/{julgamentoId}/votos/stream";
    }

    public static String citizenTimelineVisual() {
        return "/api/v1/cidadao/dashboard-enhanced/processos/{processoId}/timeline-visual";
    }

    public static String citizenEventMirror() {
        return "/api/v1/cidadao/dashboard-enhanced/processos/{processoId}/event-mirror";
    }

    public static String publicProcessTimeline() {
        return "/api/v1/public/processos/{numero}/timeline";
    }

    public static String uiLegend() {
        return "/api/v1/ui/legend";
    }

    public static String processualPainelContextual() {
        return "/api/v1/processual/unificado/{processoId}/painel-contextual?profileCode={profileCode}";
    }

    public static String processualPainelContextualTelemetria() {
        return "/api/v1/processual/unificado/{processoId}/painel-contextual/telemetria-conectores";
    }

    public static String processualPainelContextualFontesOficiais() {
        return "/api/v1/processual/unificado/{processoId}/painel-contextual/fontes-oficiais";
    }

    public static String processualPainelContextualBndt() {
        return "/api/v1/processual/unificado/{processoId}/painel-contextual/trabalhista/bndt";
    }

    public static String processualPainelContextualTrilhoPrevidenciario() {
        return "/api/v1/processual/unificado/{processoId}/painel-contextual/previdenciario/trilho";
    }

    public static String processualPainelContextualRotaTatica() {
        return "/api/v1/processual/unificado/{processoId}/painel-contextual/rota-tatica";
    }


    public static String julgamentoProcessos() {
        return "/api/v1/julgamentos/processos/{processoId}";
    }

    public static String julgamentoDetail() {
        return "/api/v1/julgamentos/{julgamentoId}";
    }

    public static String julgamentoVotesStream() {
        return "/api/v1/julgamentos/{julgamentoId}/votos/stream";
    }

    public static String calendarWorkspace() {
        return "/api/v1/calendar/workspace?from={from}&to={to}&processoId={processoId}";
    }

    public static String calendarPanel() {
        return "/api/v1/calendar/panel?from={from}&to={to}&processoId={processoId}";
    }

    public static String calendarNotificationPreview() {
        return "/api/v1/calendar/notification-preview?from={from}&to={to}&processoId={processoId}";
    }

    public static String calendarInstitutionalBridge() {
        return "/api/v1/calendar/institutional-bridge?from={from}&to={to}&processoId={processoId}";
    }

    public static String calendarInstitutionalFocus() {
        return "/api/v1/calendar/institutional-focus?from={from}&to={to}&processoId={processoId}";
    }

    public static String calendarPreferences() {
        return "/api/v1/calendar/preferences";
    }

    public static String processoPrazoReal() {
        return "/api/v1/processos/{processoId}/prazo-real?tipoAto=ATO_PROCESSUAL";
    }

    public static String notificationPreferencesUser() {
        return "/api/v1/notificacoes/preferencias/usuarios/{usuarioId}";
    }

    public static String notificationMulticanalDispatch() {
        return "/api/v1/notificacoes/multicanal/processos/{processoId}/usuarios/{usuarioId}";
    }

    public static String notificationTrackingPixel() {
        return "/api/v1/notificacoes/track/{token}.gif";
    }

    public static String notificationTrackingCiencia() {
        return "/api/v1/notificacoes/track/{token}/ciencia";
    }

    public static String recursalNotificationMobilePosture() {
        return "/api/v1/processual/recursal/notification/mobile/posture";
    }

    public static String recursalNotificationMobileExternalHardening() {
        return "/api/v1/processual/recursal/notification/mobile/external-delivery/hardened";
    }



    public static String forumDistributionResolve() {
        return OperationalApiRoutes.forumDesksResolve(0L);
    }

    public static String secretariatOperationalSnapshot() {
        return OperationalApiRoutes.secretariatOperationalSnapshot();
    }
    public static String secretariatQueuePanel() {
        return OperationalApiRoutes.secretariatQueuePanel();
    }

    public static String secretariatQueueAgenda() {
        return OperationalApiRoutes.secretariatQueueAgenda();
    }

    public static String secretariatQueueGovernance() {
        return OperationalApiRoutes.secretariatQueueGovernance();
    }

    public static String secretariatQueueCoverage() {
        return OperationalApiRoutes.secretariatQueueCoverage();
    }

    public static String secretariatQueueFormalCatalog() {
        return OperationalApiRoutes.secretariatQueueFormalCatalog();
    }

    public static String institutionalSupportSnapshot(String branchCode) {
        return OperationalApiRoutes.institutionalSupportSnapshot(branchCode);
    }

    public static String institutionalSupportAgenda(String branchCode) {
        return OperationalApiRoutes.institutionalSupportAgenda(branchCode);
    }


    public static String secretariatOperationalJuntada() {
        return OperationalApiRoutes.secretariatOperationalJuntada(0L);
    }

    public static String secretariatOperationalIntimacao() {
        return OperationalApiRoutes.secretariatOperationalIntimacao(0L);
    }

    public static String secretariatOperationalConclusao() {
        return OperationalApiRoutes.secretariatOperationalConclusao(0L);
    }

    public static String peticionamentoSessaoInicial() {
        return "/api/v1/peticionamento/inicial/sessao";
    }

    public static String peticionamentoStudioWorkspace() {
        return "/api/v1/peticionamento/studio/workspace";
    }

    public static String peticionamentoStudioQuickDraft() {
        return "/api/v1/peticionamento/studio/minuta-rapida";
    }

    public static String peticionamentoStudioGovernedReview() {
        return "/api/v1/peticionamento/studio/revisao-governada";
    }

    public static String peticionamentoStudioDraftDiff() {
        return "/api/v1/peticionamento/studio/diff-minuta";
    }

    public static String peticionamentoWizardProtocoloSimples() {
        return "/api/v1/peticionamento/studio/wizard-protocolo-simples";
    }

    public static String peticionamentoJourneyInteligente() {
        return "/api/v1/peticionamento/studio/jornada-inteligente";
    }

    public static String peticionamentoInitialDraftStruct() {
        return "/api/v1/peticionamento/inicial/rascunhos/estruturar";
    }

    public static String peticionamentoInitialDraftSave() {
        return "/api/v1/peticionamento/inicial/rascunhos/salvar";
    }

    public static String peticionamentoInitialDraftMine() {
        return "/api/v1/peticionamento/inicial/rascunhos/minhas";
    }

    public static String peticionamentoInitialDraftDetail() {
        return "/api/v1/peticionamento/inicial/rascunhos/{draftId}";
    }

    public static String peticionamentoInitialDraftProtocol() {
        return "/api/v1/peticionamento/inicial/rascunhos/{draftId}/protocolar";
    }

    public static String processualPautaAudiencia() {
        return "/api/v1/processual/pauta-audiencia";
    }

    public static String peritoPainel() {
        return "/api/v1/perito/painel";
    }

    public static String peritoNomeacoes() {
        return "/api/v1/perito/nomeacoes";
    }

    public static String peritoOperacionalSnapshot() {
        return "/api/v1/perito/operacional/snapshot";
    }

    public static String peritoOperacionalLaudo() {
        return "/api/v1/perito/operacional/processos/{processoId}/laudo";
    }

    public static String oficialJusticaAgendaOperacional() {
        return OperationalApiRoutes.oficialJusticaAgendaOperacional();
    }

    public static String oficialJusticaProcessWorkbench() {
        return OperationalApiRoutes.oficialJusticaNamedProcessWorkbench(0L);
    }

    public static String oficialJusticaCienteIntimacao() {
        return OperationalApiRoutes.oficialJusticaCienteIntimacao(0L);
    }

    public static String oficialJusticaOficios() {
        return "/api/v1/oficial-justica/processos/{processoId}/oficios";
    }

    public static String oficialJusticaOficiosResposta() {
        return "/api/v1/oficial-justica/processos/{processoId}/oficios/resposta";
    }

    public static String publicConsultaWorkspace() {
        return "/api/v1/public/consultas-publicas/workspace";
    }

    public static String publicConsultaProcesso() {
        return "/api/v1/public/consultas-publicas/processos/{numero}";
    }

    public static String publicConsultaPageResolve() {
        return "/api/v1/public/consultas-publicas/pages/{pageId}";
    }

    public static String officeProcessAccess() {
        return "/api/v1/ui/offices/workspace/processes/{processoId}/access";
    }

    public static String officeProcessReadingMode() {
        return "/api/v1/ui/offices/workspace/processes/{processoId}/reading-mode";
    }

    public static String officeWorkspaceMainDashboard() {
        return "/api/v1/ui/offices/workspace/main-dashboard";
    }

    public static String officeWorkspaceExecutiveDashboard() {
        return "/api/v1/ui/offices/workspace/executive-dashboard";
    }

    public static String processualParticipacaoWorkspace() {
        return "/api/v1/processual/processos/{processoId}/participacao-ativa/workspace";
    }

    public static String processualParticipacaoSubmissoes() {
        return "/api/v1/processual/processos/{processoId}/participacao-ativa/submissoes";
    }


    public static String processualParticipacaoProtocolar() {
        return "/api/v1/processual/processos/{processoId}/participacao-ativa/protocolar";
    }

    public static String defensoriaSnapshot() {
        return "/api/v1/defensor/operacional/snapshot";
    }

    public static String defensoriaMalha() {
        return "/api/v1/defensor/operacional/processos/{processoId}/malha";
    }

    public static String defensoriaDefesa() {
        return "/api/v1/defensor/operacional/processos/{processoId}/defesa";
    }

    public static String defensoriaHabeasCorpus() {
        return "/api/v1/defensor/operacional/processos/{processoId}/habeas-corpus";
    }

    public static String ministerioPublicoPainel() {
        return "/api/v1/mp/painel";
    }

    public static String ministerioPublicoMalha() {
        return "/api/v1/mp/processos/{processoId}/malha";
    }

    public static String ministerioPublicoManifestacao() {
        return "/api/v1/mp/manifestacao/{processoId}";
    }

    public static String ministerioPublicoParecer() {
        return "/api/v1/mp/parecer/{processoId}";
    }

    public static String ministerioPublicoRecurso() {
        return "/api/v1/recursal/processos/{processoId}/recurso";
    }

    public static String procuradoriaSnapshot() {
        return "/api/v1/procuradoria/operacional/snapshot";
    }

    public static String procuradoriaMalha() {
        return "/api/v1/procuradoria/operacional/processos/{processoId}/malha";
    }

    public static String procuradoriaRecurso() {
        return "/api/v1/recursal/processos/{processoId}/recurso";
    }

    public static String procuradoriaParecer() {
        return "/api/v1/procuradoria/operacional/processos/{processoId}/parecer";
    }

    public static String peritoQuesitos() {
        return "/api/v1/perito/operacional/processos/{processoId}/quesitos";
    }

    public static String advogadoPainelPrincipal() {
        return "/api/v1/ui/professional/workspace/advogado-dashboard";
    }

    public static String advogadoPainelDetalhado() {
        return "/api/v1/ui/professional/workspace/advogado-dashboard-detalhado";
    }

    public static String advogadoCitacoesIntimacoes() {
        return "/api/v1/ui/professional/workspace/citacoes-intimacoes";
    }

    public static String advogadoAudienciasFuturas() {
        return "/api/v1/ui/professional/workspace/audiencias-futuras";
    }

    public static String advogadoRecursosTribunal() {
        return "/api/v1/ui/professional/workspace/recursos-tribunal";
    }

    public static String advogadoSessoesJulgamento() {
        return "/api/v1/ui/professional/workspace/sessoes-julgamento";
    }

    public static String advogadoAreaTrabalho() {
        return "/api/v1/ui/professional/workspace/area-trabalho";
    }

    public static String advogadoRelacaoProcessos() {
        return "/api/v1/ui/professional/workspace/relacao-processos";
    }

    public static String peticionamentoDistribuicaoFutura() {
        return "/api/v1/peticionamento/inicial/distribuicao-futura";
    }

    public static String peticionamentoDistribuicaoLote() {
        return "/api/v1/peticionamento/inicial/distribuicao-em-lote";
    }

    public static String peticionamentoIntermediarioBloco() {
        return "/api/v1/peticionamento/intermediario/bloco";
    }

    public static String peticionamentoAssinaturaLote() {
        return "/api/v1/peticionamento/assinatura/lote";
    }

    public static String certidaoNarratoriaProfissional() {
        return "/api/v1/ui/professional/workspace/certidoes/narratoria";
    }

    public static String certidaoExecutivaProfissional() {
        return "/api/v1/ui/professional/workspace/certidoes/execucao";
    }

    public static String certidaoAutenticidadeProfissional() {
        return "/api/v1/certidoes/autenticidade";
    }

    public static String institutionalWorkbenchBoxes() {
        return "/api/v1/institucional/workbench/organization-boxes";
    }

    public static String institutionalWorkbenchBoxHistory() {
        return "/api/v1/institucional/workbench/organization-boxes/history";
    }

    public static String institutionalWorkbenchBoxFilters() {
        return "/api/v1/institucional/workbench/organization-boxes/filters";
    }

    public static String ajuizamentoInferIntent() {
        return "/api/v1/ai/ajuizamento/infer-intent";
    }

    public static String ajuizamentoPreflight() {
        return "/api/v1/ai/ajuizamento/preflight";
    }

    public static String ajuizamentoRouting() {
        return "/api/v1/ai/ajuizamento/routing";
    }

    public static String ajuizamentoCanonical() {
        return "/api/v1/ai/ajuizamento/canonical";
    }

    public static String ajuizamentoCapabilitiesTribunal() {
        return "/api/v1/ai/ajuizamento/catalog/tribunais/capabilities";
    }

    public static String processualPendenciasPainel() {
        return OperationalApiRoutes.processualPendenciasPainel();
    }

    public static String professionalWorkspaceExecutiveDashboard() {
        return "/api/v1/ui/professional/workspace/executive-dashboard";
    }

    public static String professionalWorkspaceOrganizationalDashboard() {
        return "/api/v1/ui/professional/workspace/organizational-executive-dashboard";
    }

    public static String magistratureExecutiveDashboard() {
        return "/api/v1/ui/professional/workspace/magistrature-executive-dashboard";
    }

    public static String magistratureOrganDashboard() {
        return "/api/v1/ui/professional/workspace/magistrature-organ-dashboard";
    }

    public static String defensoriaExecutiveDashboard() {
        return "/api/v1/ui/professional/workspace/defensoria-executive-dashboard";
    }

    public static String defensoriaOrganDashboard() {
        return "/api/v1/ui/professional/workspace/defensoria-organ-dashboard";
    }

    public static String procuradoriaExecutiveDashboard() {
        return "/api/v1/frontend/app/professional/workspace/procuradoria-executive-dashboard";
    }

    public static String procuradoriaOrganDashboard() {
        return "/api/v1/frontend/app/professional/workspace/procuradoria-organ-dashboard";
    }

    public static String institutionalSupportCoverage(String branchCode) {
        return OperationalApiRoutes.institutionalSupportCoverage(branchCode);
    }

    public static String institutionalSupportPrePauta(String branchCode) {
        return OperationalApiRoutes.institutionalSupportProcessPrePauta(branchCode, 0L);
    }

    public static String magistraturaWorkspace() {
        return "/api/v1/magistratura/atos?processoId={processoId}";
    }

    public static String magistraturaPreview() {
        return "/api/v1/magistratura/processos/{processoId}/atos/preview?action={action}";
    }

    public static String magistraturaAutomationPreview() {
        return "/api/v1/magistratura/processos/{processoId}/atos/automation-preview";
    }

    public static String distribuicaoWorkbench() {
        return "/api/v1/distribuicao/processual/workbench?numeroProcesso={numeroProcesso}";
    }

    public static String institutionalWorkbench() {
        return "/api/v1/institucional/workbench";
    }

    public static String institutionalWorkbenchQuickActions() {
        return "/api/v1/institucional/workbench/quick-actions?processoId={processoId}";
    }

    public static String institutionalWorkbenchOperationalQueue() {
        return "/api/v1/institucional/workbench/operational-queue?limit={limit}";
    }

    public static String institutionalWorkbenchActionPreview() {
        return "/api/v1/institucional/workbench/action-preview?action={action}&processoId={processoId}";
    }

    public static String painelMagistradoPrimeiroGrau(String justicaAxis, String tribunalAxis) {
        return OperationalApiRoutes.judgeGabinetePainel(justicaAxis, tribunalAxis, "unidadeCodigo", "caixaCodigo", "orgao");
    }

    public static String painelColegiadoSegundoGrau(String justicaAxis, String tribunalAxis) {
        return OperationalApiRoutes.desembargadorPainel(justicaAxis, tribunalAxis, "unidadeCodigo", "caixaCodigo", "orgao");
    }

    public static String painelCorteSuperior(String tribunalAxis) {
        return OperationalApiRoutes.ministroPlenarioPainel(tribunalAxis, "unidadeCodigo", "caixaCodigo", "orgao");
    }

    public static String malhaColegiada(Long processoId) {
        return OperationalApiRoutes.desembargadorColegiadoMalhaProcesso(processoId);
    }

    public static String votoColegiado(Long processoId) {
        return OperationalApiRoutes.desembargadorColegiadoVoto(processoId);
    }

    public static String acordaoColegiado(Long processoId) {
        return OperationalApiRoutes.desembargadorColegiadoAcordao(processoId);
    }

    public static String pautaCorteSuperior(Long processoId) {
        return OperationalApiRoutes.ministroPlenarioPauta(processoId);
    }

    public static String decisaoPlenariaCorteSuperior(Long processoId) {
        return OperationalApiRoutes.ministroPlenarioDecisaoPlenaria(processoId);
    }



    public static String secretariatOperationalElectoralCorregedoria() {
        return OperationalApiRoutes.secretariatOperationalElectoralCorregedoria(0L);
    }

    public static String secretariatOperationalElectoralInspecao() {
        return OperationalApiRoutes.secretariatOperationalElectoralInspecao(0L);
    }

    public static String secretariatOperationalElectoralPesquisa() {
        return OperationalApiRoutes.secretariatOperationalElectoralPesquisa(0L);
    }

    public static String secretariatOperationalLabourMidiaRecebimento() {
        return OperationalApiRoutes.secretariatOperationalLabourMidiaRecebimento(0L);
    }

    public static String secretariatOperationalLabourMidiaDisponibilizacao() {
        return OperationalApiRoutes.secretariatOperationalLabourMidiaDisponibilizacao(0L);
    }

    public static String secretariatOperationalLabourExecucao() {
        return OperationalApiRoutes.secretariatOperationalLabourExecucao(0L);
    }

    public static String secretariatOperationalMilitaryPlantao() {
        return OperationalApiRoutes.secretariatOperationalMilitaryPlantao(0L);
    }

    public static String secretariatOperationalMilitaryBalcao() {
        return OperationalApiRoutes.secretariatOperationalMilitaryBalcao(0L);
    }

    public static String advogadoAutosDigitaisCapa() {
        return "/api/v1/ui/professional/workspace/autos-digitais/capa";
    }

    public static String advogadoAutosDigitaisLembretes() {
        return "/api/v1/ui/professional/workspace/autos-digitais/lembretes";
    }

    public static String advogadoAutosDigitaisAssuntos() {
        return "/api/v1/ui/professional/workspace/autos-digitais/assuntos";
    }

    public static String advogadoAutosDigitaisPartes() {
        return "/api/v1/ui/professional/workspace/autos-digitais/partes";
    }

    public static String advogadoAutosDigitaisEventos() {
        return "/api/v1/ui/professional/workspace/autos-digitais/eventos";
    }

    public static String advogadoAutosDigitaisInformacoesAdicionais() {
        return "/api/v1/ui/professional/workspace/autos-digitais/informacoes-adicionais";
    }

    public static String advogadoAutosDigitaisAcoes() {
        return "/api/v1/ui/professional/workspace/autos-digitais/acoes";
    }

    public static String advogadoSolicitacaoHabilitacao() {
        return "/api/v1/advogados/processos/{processoId}/habilitacao";
    }

    public static String advogadoAssociacaoProcessosPublicos() {
        return "/api/v1/advogados/processos/{processoId}/associacao-publica";
    }

    public static String advogadoHabilitacaoSigilosa() {
        return "/api/v1/advogados/processos/{processoId}/habilitacao-sigilosa";
    }

    public static String advogadoEscritorio() {
        return "/api/v1/advogado/escritorio";
    }

    public static String advogadoAssistentes() {
        return "/api/v1/advogado/assistentes";
    }

    public static String advogadoSubstabelecimento() {
        return "/api/v1/advogado/substabelecimentos";
    }

    public static String advogadoSubstabelecimentoCancelamento() {
        return "/api/v1/advogado/substabelecimentos/{substabelecimentoId}/cancelar";
    }

    public static String recursalVideoconferencia() {
        return "/api/v1/processual/recursal/midias/videoconferencia";
    }

    public static String recursalMidiasRepositorio() {
        return "/api/v1/processual/recursal/midias/repositorio";
    }

    public static String recursalMidiasGravacaoAudiencia() {
        return "/api/v1/processual/recursal/midias/gravacoes-audiencia";
    }

    public static String recursalMidiasVisualizacao() {
        return "/api/v1/processual/recursal/midias/visualizacao";
    }

    public static String recursalDocumentViewer() {
        return "/api/v1/processual/recursal/document-viewer";
    }

    public static String recursalDocumentAuthenticity() {
        return "/api/v1/processual/recursal/document-authenticity";
    }

    public static String recursalDocumentSignatureEvidence() {
        return "/api/v1/processual/recursal/document-signature-evidence";
    }

    public static String recursalBusinessIntelligence() {
        return "/api/v1/processual/recursal/analytics/business-intelligence";
    }

    public static String recursalIndexacaoBusca() {
        return "/api/v1/processual/recursal/analytics/indexacao-busca";
    }

    public static String recursalNotificaPendencias() {
        return "/api/v1/processual/recursal/analytics/notifica-pendencias";
    }

    public static String recursalMobileAcompanhamento() {
        return "/api/v1/processual/recursal/analytics/mobile-acompanhamento";
    }

    public static String recursalNotificationGovernance() {
        return "/api/v1/processual/recursal/analytics/notifica-pendencias";
    }

    public static String recursalNotificationScience() {
        return "/api/v1/processual/recursal/notification/science";
    }

    public static String recursalNotificationPreferencesFine() {
        return "/api/v1/processual/recursal/notification/preferences/fine";
    }

    public static String recursalNotificationFederatedDelivery() {
        return "/api/v1/processual/recursal/notification/federated-delivery";
    }

    public static Map<String, String> ramoSurfaceSummary(String ramo) {
        String eixo = ramo == null ? "" : ramo.trim().toUpperCase();
        LinkedHashMap<String, String> surfaces = new LinkedHashMap<>();
        switch (eixo) {
            case "TRABALHISTA" -> {
                surfaces.put("profissional", professionalWorkspaceExecutiveDashboard());
                surfaces.put("organizacional", professionalWorkspaceOrganizationalDashboard());
                surfaces.put("midia-recebimento", secretariatOperationalLabourMidiaRecebimento());
                surfaces.put("midia-disponibilizacao", secretariatOperationalLabourMidiaDisponibilizacao());
                surfaces.put("execucao", secretariatOperationalLabourExecucao());
                surfaces.put("colegiado", painelColegiadoSegundoGrau("trabalhista", "trt"));
            }
            case "ELEITORAL" -> {
                surfaces.put("cidadao-overview", citizenProcessOverview());
                surfaces.put("corregedoria", secretariatOperationalElectoralCorregedoria());
                surfaces.put("inspecao", secretariatOperationalElectoralInspecao());
                surfaces.put("pesquisa", secretariatOperationalElectoralPesquisa());
                surfaces.put("colegiado", painelColegiadoSegundoGrau("eleitoral", "tre"));
            }
            case "MILITAR" -> {
                surfaces.put("cidadao-overview", citizenProcessOverview());
                surfaces.put("plantao", secretariatOperationalMilitaryPlantao());
                surfaces.put("balcao", secretariatOperationalMilitaryBalcao());
                surfaces.put("colegiado", painelColegiadoSegundoGrau("militar", "tm"));
                surfaces.put("magistratura", magistratureExecutiveDashboard());
            }
            case "PENAL" -> {
                surfaces.put("cidadao-timeline", citizenTimelineVisual());
                surfaces.put("participacao-ativa", processualParticipacaoWorkspace());
                surfaces.put("magistratura", magistraturaWorkspace());
                surfaces.put("colegiado", malhaColegiada(0L));
                surfaces.put("acordao", acordaoColegiado(0L));
            }
            default -> {
                surfaces.put("cidadao-timeline", citizenTimelineVisual());
                surfaces.put("representacao", officeProcessReadingMode());
                surfaces.put("colegiado", malhaColegiada(0L));
                surfaces.put("acordao", acordaoColegiado(0L));
                surfaces.put("legenda", uiLegend());
            }
        }
        return Map.copyOf(surfaces);
    }

    public static Map<String, String> familySummary(String justicaAxis, String tribunalAxis, String corteAxis) {
        LinkedHashMap<String, String> families = new LinkedHashMap<>();
        families.put("painel-magistrado", painelMagistradoPrimeiroGrau(justicaAxis, tribunalAxis));
        families.put("painel-colegiado", painelColegiadoSegundoGrau(justicaAxis, tribunalAxis));
        families.put("magistratura-workspace", magistraturaWorkspace());
        families.put("distribuicao-workbench", distribuicaoWorkbench());
        families.put("institutional-workbench", institutionalWorkbench());
        families.put("painel-corte-superior", painelCorteSuperior(corteAxis));
        return Map.copyOf(families);
    }
}
