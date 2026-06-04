package com.tcc.pjb.backend.service.painel.surface;

import com.tcc.pjb.backend.model.dto.intelligence.PessoaLocalizacaoRequest;
import com.tcc.pjb.backend.model.dto.intelligence.PessoaLocalizacaoResponse;
import com.tcc.pjb.backend.model.dto.oficial_justica.OficialJusticaCienciaIntimacaoRequest;
import com.tcc.pjb.backend.model.dto.oficial_justica.OficialJusticaDiligenciaQueueResponse;
import com.tcc.pjb.backend.model.dto.oficial_justica.OficialJusticaEnderecoTriageResponse;
import com.tcc.pjb.backend.model.dto.oficial_justica.OficialJusticaPessoaRastreioResponse;
import com.tcc.pjb.backend.model.dto.oficial_justica.OficialJusticaProcessoAcessoResponse;
import com.tcc.pjb.backend.model.dto.oficial_justica.OficialJusticaProcessoWorkbenchResponse;
import com.tcc.pjb.backend.model.dto.profile.operational.MinisterioPublicoParecerRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.OficialJusticaOficioCartorioAckRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.OficialJusticaOficioChannelAckRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.OficialJusticaOficioConfirmationRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.OficialJusticaOficioReconciliationRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.OficialJusticaOficioRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.OficialJusticaOficioRetryRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.PsicossocialParecerRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.PsicossocialRelatorioRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.DelegadoDiligenciaRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.DelegadoInqueritoMultimidiaRequest;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceActionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceCollectionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.service.assessor.AssessorPainelService;
import com.tcc.pjb.backend.service.conciliacao.ConciliadorMediadorPainelService;
import com.tcc.pjb.backend.service.curadoria.CuradorAusentesPainelService;
import com.tcc.pjb.backend.service.defensor.DefensorPublicoPainelService;
import com.tcc.pjb.backend.service.delegado.DelegadoPainelService;
import com.tcc.pjb.backend.service.extrajudicial.CartorioExtrajudicialPainelService;
import com.tcc.pjb.backend.service.intelligence.PessoaLocalizacaoService;
import com.tcc.pjb.backend.service.mp.MinisterioPublicoPainelService;
import com.tcc.pjb.backend.service.oficial_justica.OficialJusticaPainelService;
import com.tcc.pjb.backend.service.perito.PeritoPainelService;
import com.tcc.pjb.backend.service.psicossocial.PsicossocialJudicialPainelService;
import com.tcc.pjb.backend.service.surface.common.SurfaceProjectionSupport;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class InstitutionalPainelSurfaceFacadeService {

    private final PsicossocialJudicialPainelService psicossocialService;
    private final CartorioExtrajudicialPainelService extrajudicialService;
    private final ConciliadorMediadorPainelService conciliacaoService;
    private final PeritoPainelService peritoService;
    private final AssessorPainelService assessorService;
    private final CuradorAusentesPainelService curadoriaService;
    private final DefensorPublicoPainelService defensorService;
    private final MinisterioPublicoPainelService ministerioPublicoService;
    private final OficialJusticaPainelService oficialJusticaService;
    private final DelegadoPainelService delegadoService;
    private final PessoaLocalizacaoService pessoaLocalizacaoService;
    private final SurfaceProjectionSupport projectionSupport;

    public InstitutionalPainelSurfaceFacadeService(PsicossocialJudicialPainelService psicossocialService,
                                                   CartorioExtrajudicialPainelService extrajudicialService,
                                                   ConciliadorMediadorPainelService conciliacaoService,
                                                   PeritoPainelService peritoService,
                                                   AssessorPainelService assessorService,
                                                   CuradorAusentesPainelService curadoriaService,
                                                   DefensorPublicoPainelService defensorService,
                                                   MinisterioPublicoPainelService ministerioPublicoService,
                                                   OficialJusticaPainelService oficialJusticaService,
                                                   DelegadoPainelService delegadoService,
                                                   PessoaLocalizacaoService pessoaLocalizacaoService,
                                                   SurfaceProjectionSupport projectionSupport) {
        this.psicossocialService = Objects.requireNonNull(psicossocialService);
        this.extrajudicialService = Objects.requireNonNull(extrajudicialService);
        this.conciliacaoService = Objects.requireNonNull(conciliacaoService);
        this.peritoService = Objects.requireNonNull(peritoService);
        this.assessorService = Objects.requireNonNull(assessorService);
        this.curadoriaService = Objects.requireNonNull(curadoriaService);
        this.defensorService = Objects.requireNonNull(defensorService);
        this.ministerioPublicoService = Objects.requireNonNull(ministerioPublicoService);
        this.oficialJusticaService = Objects.requireNonNull(oficialJusticaService);
        this.delegadoService = Objects.requireNonNull(delegadoService);
        this.pessoaLocalizacaoService = Objects.requireNonNull(pessoaLocalizacaoService);
        this.projectionSupport = Objects.requireNonNull(projectionSupport);
    }

    public SurfaceCollectionResponse psicossocialCasosPrioritarios() { return projectionSupport.collection("psicossocial.casos-prioritarios", psicossocialService.listarCasosPrioritarios()); }
    public SurfaceActionResponse psicossocialRegistrarParecer(Long processoId, PsicossocialParecerRequest request) { return projectionSupport.action("psicossocial.parecer", "registrar-parecer", processoId, psicossocialService.registrarParecer(processoId, request)); }
    public SurfaceActionResponse psicossocialEntregarRelatorio(Long processoId, PsicossocialRelatorioRequest request) { return projectionSupport.action("psicossocial.relatorio", "entregar-relatorio", processoId, psicossocialService.entregarRelatorio(processoId, request)); }
    public SurfaceCollectionResponse extrajudicialAtosPendentes() { return projectionSupport.collection("extrajudicial.atos-pendentes", extrajudicialService.listarAtosPendentes()); }
    public SurfaceCollectionResponse conciliacaoSessoesHoje() { return projectionSupport.collection("conciliacao.sessoes-hoje", conciliacaoService.listarSessoesHoje()); }
    public SurfaceCollectionResponse conciliacaoSessoesPendentes() { return projectionSupport.collection("conciliacao.sessoes-pendentes", conciliacaoService.listarSessoesPendentes()); }
    public SurfaceActionResponse conciliacaoRegistrarAcordo(String sessaoId, Object request) { return projectionSupport.action("conciliacao.sessao", "registrar-acordo", parseLong(sessaoId), conciliacaoService.registrarAcordo(sessaoId, request)); }
    public SurfaceActionResponse conciliacaoEncerrarSemAcordo(String sessaoId, Object request) { return projectionSupport.action("conciliacao.sessao", "encerrar-sem-acordo", parseLong(sessaoId), conciliacaoService.encerrarSessaoSemAcordo(sessaoId, request)); }
    public SurfaceCollectionResponse conciliacaoAcordosPendentes() { return projectionSupport.collection("conciliacao.acordos-pendentes-homologacao", conciliacaoService.listarAcordosPendentesHomologacao()); }
    public SurfaceSnapshotResponse conciliacaoMetricasMes() { return projectionSupport.snapshot("conciliacao.metricas-mes", conciliacaoService.carregarMetricasMes()); }
    public SurfaceCollectionResponse peritoNomeacoes() { return projectionSupport.collection("perito.nomeacoes", peritoService.listarNomeacoesAtivas()); }
    public SurfaceActionResponse peritoAceitarNomeacao(Long nomeacaoId) { return projectionSupport.action("perito.nomeacao", "aceitar-nomeacao", nomeacaoId, peritoService.aceitarNomeacao(nomeacaoId)); }
    public SurfaceActionResponse peritoRecusarNomeacao(Long nomeacaoId, Object request) { return projectionSupport.action("perito.nomeacao", "recusar-nomeacao", nomeacaoId, peritoService.recusarNomeacao(nomeacaoId, request)); }
    public SurfaceCollectionResponse peritoLaudosPendentes() { return projectionSupport.collection("perito.laudos-pendentes", peritoService.listarLaudosPendentes()); }
    public SurfaceActionResponse peritoEntregarLaudo(Long processoId, Object request) { return projectionSupport.action("perito.laudo", "entregar-laudo", processoId, peritoService.entregarLaudo(processoId, request)); }
    public SurfaceCollectionResponse peritoPrazosVencendo() { return projectionSupport.collection("perito.prazos-vencendo", peritoService.listarPrazosVencendo()); }
    public SurfaceCollectionResponse assessorMinutasGabinete() { return projectionSupport.collection("assessor.minutas-gabinete", assessorService.listarMinutasPendentesGabinete()); }
    public SurfaceCollectionResponse assessorAgenda() { return projectionSupport.collection("assessor.agenda", assessorService.listarAgendaHoje()); }
    public SurfaceCollectionResponse assessorDespachosPendentes() { return projectionSupport.collection("assessor.despachos-pendentes", assessorService.listarProcessosPendentesDespacho()); }
    public SurfaceSnapshotResponse assessorGuardrailsProcesso(Long processoId) { return projectionSupport.snapshot("assessor.guardrails-processo", assessorService.guardrailsProcesso(processoId)); }
    public SurfaceSnapshotResponse assessorHandoffProcesso(Long processoId) { return projectionSupport.snapshot("assessor.handoff-processo", assessorService.handoffProcesso(processoId)); }
    public SurfaceSnapshotResponse assessorMatrizProcesso(Long processoId) { return projectionSupport.snapshot("assessor.matriz-processo", assessorService.matrizProcesso(processoId)); }
    public SurfaceActionResponse assessorDevolverParaGabinete(Long processoId, String observacao) { return projectionSupport.action("assessor.handoff", "devolver-gabinete", processoId, assessorService.devolverParaGabinete(processoId, observacao)); }

    public SurfaceCollectionResponse psicossocialAgendaTecnica() { return projectionSupport.collection("psicossocial.agenda-tecnica", psicossocialService.listarAgendaTecnica()); }
    public SurfaceCollectionResponse psicossocialVisitasDomiciliares() { return projectionSupport.collection("psicossocial.visitas-domiciliares", psicossocialService.listarVisitasDomiciliares()); }
    public SurfaceSnapshotResponse psicossocialResumoSensibilidade() { return projectionSupport.snapshot("psicossocial.resumo-sensibilidade", psicossocialService.resumoSensibilidadeCasos()); }
    public SurfaceCollectionResponse extrajudicialCertidoesPendentes() { return projectionSupport.collection("extrajudicial.certidoes-pendentes", extrajudicialService.listarCertidoesPendentes()); }
    public SurfaceCollectionResponse extrajudicialIndisponibilidadesPendentes() { return projectionSupport.collection("extrajudicial.indisponibilidades-pendentes", extrajudicialService.listarIndisponibilidadesPendentes()); }
    public SurfaceSnapshotResponse extrajudicialMonitoramentoOperacional() { return projectionSupport.snapshot("extrajudicial.monitoramento-operacional", extrajudicialService.monitoramentoOperacional()); }
    public SurfaceCollectionResponse curadoriaBensSobCuradoria() { return projectionSupport.collection("curadoria.bens-sob-curadoria", curadoriaService.listarBensSobCuradoria()); }
    public SurfaceCollectionResponse curadoriaPrestacoesContas() { return projectionSupport.collection("curadoria.prestacoes-contas", curadoriaService.listarPrestacoesContas()); }
    public SurfaceSnapshotResponse curadoriaResumoRiscoPatrimonial() { return projectionSupport.snapshot("curadoria.resumo-risco-patrimonial", curadoriaService.resumoRiscoPatrimonial()); }

    public SurfaceCollectionResponse curadoriaExpedientes() { return projectionSupport.collection("curadoria.expedientes", curadoriaService.listarExpedientesPrioritarios()); }
    public SurfaceCollectionResponse defensorAssistidos() { return projectionSupport.collection("defensoria.assistidos", defensorService.listarAssistidosAtivos()); }
    public SurfaceSnapshotResponse defensorProcessosAssistido(Long assistidoId) { return projectionSupport.snapshot("defensoria.processos-assistido", defensorService.listarProcessosDoAssistido(assistidoId)); }
    public SurfaceActionResponse defensorRegistrarPeticao(Long processoId, Object request) { return projectionSupport.action("defensoria.peticao", "registrar-peticao", processoId, defensorService.registrarPeticao(processoId, request)); }
    public SurfaceActionResponse defensorInterporRecurso(Long processoId, String tipoRecurso, String razoes, String fundamentacao, boolean pedidoEfeitoSuspensivo, boolean preparoDispensado, String observacoes) { return projectionSupport.action("defensoria.recurso", "interpor-recurso", processoId, defensorService.interporRecurso(processoId, tipoRecurso, razoes, fundamentacao, pedidoEfeitoSuspensivo, preparoDispensado, observacoes)); }
    public SurfaceCollectionResponse defensorAudienciasHoje() { return projectionSupport.collection("defensoria.audiencias-hoje", defensorService.listarAudienciasHoje()); }
    public SurfaceCollectionResponse defensorPrazosCriticos() { return projectionSupport.collection("defensoria.prazos-criticos", defensorService.listarPrazosCriticos()); }
    public SurfaceActionResponse defensorRequerimentoGratuidade(Long processoId, Object request) { return projectionSupport.action("defensoria.gratuidade", "registrar-requerimento-gratuidade", processoId, defensorService.registrarRequerimentoGratuidade(processoId, request)); }
    public SurfaceCollectionResponse ministerioPublicoManifestacoesPendentes() { return projectionSupport.collection("mp.manifestacoes-pendentes", ministerioPublicoService.listarManifestacoesPendentes()); }
    public SurfaceSnapshotResponse ministerioPublicoMalhaProcesso(Long processoId) { return projectionSupport.snapshot("mp.malha-processo", ministerioPublicoService.malhaProcesso(processoId)); }
    public SurfaceActionResponse ministerioPublicoRegistrarManifestacao(Long processoId, Object request) { return projectionSupport.action("mp.manifestacao", "registrar-manifestacao", processoId, ministerioPublicoService.registrarManifestacao(processoId, request)); }
    public SurfaceActionResponse ministerioPublicoRegistrarParecer(Long processoId, MinisterioPublicoParecerRequest request) { return projectionSupport.action("mp.parecer", "registrar-parecer", processoId, ministerioPublicoService.registrarParecer(processoId, request)); }
    public SurfaceActionResponse ministerioPublicoInterporRecurso(Long processoId, String tipoRecurso, String razoes, String fundamentacao, boolean pedidoEfeitoSuspensivo, boolean preparoDispensado, String observacoes) { return projectionSupport.action("mp.recurso", "interpor-recurso", processoId, ministerioPublicoService.interporRecurso(processoId, tipoRecurso, razoes, fundamentacao, pedidoEfeitoSuspensivo, preparoDispensado, observacoes)); }
    public SurfaceCollectionResponse ministerioPublicoInqueritosAcompanhamento() { return projectionSupport.collection("mp.inqueritos-acompanhamento", ministerioPublicoService.listarInqueritosEmAcompanhamento()); }
    public SurfaceActionResponse ministerioPublicoRequisitarDiligencia(Long processoId, Object request) { return projectionSupport.action("mp.diligencia", "requisitar-diligencia", processoId, ministerioPublicoService.requisitarDiligencia(processoId, request)); }
    public SurfaceCollectionResponse ministerioPublicoPrazosCriticos() { return projectionSupport.collection("mp.prazos-criticos", ministerioPublicoService.listarPrazosDentroDe48h()); }
    public SurfaceCollectionResponse oficialMandados() { return projectionSupport.collection("oficial.mandados", oficialJusticaService.listarMandados()); }
    public SurfaceActionResponse oficialRegistrarCumprimento(String mandadoId, Object request) { return projectionSupport.action("oficial.mandado", "registrar-cumprimento", parseLong(mandadoId), oficialJusticaService.registrarCumprimento(mandadoId, request)); }
    public SurfaceActionResponse oficialRegistrarFrustracao(String mandadoId, Object request) { return projectionSupport.action("oficial.mandado", "registrar-frustracao", parseLong(mandadoId), oficialJusticaService.registrarFrustracao(mandadoId, request)); }
    public SurfaceActionResponse oficialRegistrarAvaliacao(Long processoId, Object request) { return projectionSupport.action("oficial.penhora", "registrar-avaliacao", processoId, oficialJusticaService.registrarAvaliacao(processoId, request)); }
    public SurfaceActionResponse oficialConfirmarCienciaIntimacao(Long processoId, OficialJusticaCienciaIntimacaoRequest request) { return projectionSupport.action("oficial.intimacao", "confirmar-ciencia", processoId, oficialJusticaService.confirmarCienciaIntimacao(processoId, request)); }
    public SurfaceActionResponse oficialEmitirOficio(Long processoId, OficialJusticaOficioRequest request) { return projectionSupport.action("oficial.oficio", "emitir-oficio", processoId, oficialJusticaService.emitirOficio(processoId, request)); }
    public SurfaceActionResponse oficialResponderOficio(Long processoId, OficialJusticaOficioRequest request) { return projectionSupport.action("oficial.oficio", "responder-oficio", processoId, oficialJusticaService.responderOficio(processoId, request)); }
    public SurfaceSnapshotResponse oficialCatalogoOficios() { return projectionSupport.snapshot("oficial.oficio.catalogo", oficialJusticaService.catalogoOficios()); }
    public SurfaceSnapshotResponse oficialExecucoesOficios() { return projectionSupport.snapshot("oficial.oficio.execucoes", oficialJusticaService.listarExecucoesOficios(20)); }
    public SurfaceSnapshotResponse oficialStatusExecucaoOficio(String executionId) { return projectionSupport.snapshot("oficial.oficio.execucao", oficialJusticaService.statusExecucaoOficio(executionId)); }
    public SurfaceActionResponse oficialConfirmarEntregaOficio(String executionId, OficialJusticaOficioConfirmationRequest request) { return projectionSupport.action("oficial.oficio.execucao", "confirmar-entrega", null, oficialJusticaService.confirmarEntregaOficio(executionId, request)); }
    public SurfaceSnapshotResponse oficialMalhaExternaOficio(String executionId) { return projectionSupport.snapshot("oficial.oficio.malha", oficialJusticaService.malhaExternaOficio(executionId)); }
    public SurfaceActionResponse oficialConfirmarCanalOficio(String executionId, OficialJusticaOficioChannelAckRequest request) { return projectionSupport.action("oficial.oficio.execucao", "ack-canal", null, oficialJusticaService.confirmarCanalOficio(executionId, request)); }
    public SurfaceActionResponse oficialAckCartorioOficio(String executionId, OficialJusticaOficioCartorioAckRequest request) { return projectionSupport.action("oficial.oficio.execucao", "ack-cartorio", null, oficialJusticaService.ackCartorioOficio(executionId, request)); }
    public SurfaceActionResponse oficialReconciliarOficio(String executionId, OficialJusticaOficioReconciliationRequest request) { return projectionSupport.action("oficial.oficio.execucao", "reconciliar", null, oficialJusticaService.reconciliarOficio(executionId, request)); }
    public SurfaceActionResponse oficialRetentarEntregaOficio(String executionId, OficialJusticaOficioRetryRequest request) { return projectionSupport.action("oficial.oficio.execucao", "retentativa", null, oficialJusticaService.retentarEntregaOficio(executionId, request)); }
    public SurfaceCollectionResponse oficialRotaDia() { return projectionSupport.collection("oficial.rota-dia", oficialJusticaService.gerarRotaDia()); }
    public SurfaceSnapshotResponse oficialPendenciasOperacionais(int limit, String rito, String vara, Boolean somentePendentes) { return projectionSupport.snapshot("oficial.pendencias-operacionais", oficialJusticaService.pendenciasOperacionais(limit, rito, vara, somentePendentes)); }
    public SurfaceSnapshotResponse oficialProcessosNomeados(int limit, String rito, String vara, Boolean somentePendentes) { return projectionSupport.snapshot("oficial.processos-nomeados", oficialJusticaService.processosNomeados(limit, rito, vara, somentePendentes)); }
    public OficialJusticaProcessoAcessoResponse oficialAcessoProcessoNomeado(Long processoId) { return oficialJusticaService.acessoProcessoNomeado(processoId); }
    public SurfaceSnapshotResponse oficialResumoWorkbenchOperacional() { return projectionSupport.snapshot("oficial.workbench.resumo", oficialJusticaService.resumoWorkbenchOperacional()); }
    public OficialJusticaDiligenciaQueueResponse oficialFilaDiligenciasViva(int limit, String rito, String vara, String pasta, String prioridade, Boolean somentePendentes) { return oficialJusticaService.filaDiligenciasViva(limit, rito, vara, pasta, prioridade, somentePendentes); }
    public OficialJusticaProcessoWorkbenchResponse oficialProcessoWorkbench(Long processoId) { return oficialJusticaService.processoWorkbench(processoId); }
    public SurfaceSnapshotResponse oficialResumoRastreioOperacional() { return projectionSupport.snapshot("oficial.rastreio.resumo", oficialJusticaService.resumoRastreioOperacional()); }
    public SurfaceSnapshotResponse oficialTriagemEnderecos(int limit, boolean incluirEnderecoEstrito, boolean incluirProntuario, boolean incluirRestricoes) { return projectionSupport.snapshot("oficial.rastreio.triagem-enderecos", oficialJusticaService.triagemEnderecos(limit, incluirEnderecoEstrito, incluirProntuario, incluirRestricoes)); }
    public OficialJusticaPessoaRastreioResponse oficialRastrearMandado(String mandadoId, boolean incluirEnderecoEstrito, boolean incluirProntuario, boolean incluirRestricoes) { return oficialJusticaService.rastrearMandado(mandadoId, incluirEnderecoEstrito, incluirProntuario, incluirRestricoes); }
    public OficialJusticaPessoaRastreioResponse oficialRastrearProcessoAlvo(Long processoId, String polo, boolean incluirEnderecoEstrito, boolean incluirProntuario, boolean incluirRestricoes) { return oficialJusticaService.rastrearProcessoAlvo(processoId, polo, incluirEnderecoEstrito, incluirProntuario, incluirRestricoes); }
    public SurfaceCollectionResponse oficialConsultasRecentes() { return projectionSupport.collection("oficial.localizador.consultas-recentes", pessoaLocalizacaoService.listarRecentes(PessoaLocalizacaoService.CanalConsulta.OFICIAL_JUSTICA, 20)); }
    public SurfaceSnapshotResponse oficialMetricasLocalizador() { return projectionSupport.snapshot("oficial.localizador.metricas", pessoaLocalizacaoService.metricas(PessoaLocalizacaoService.CanalConsulta.OFICIAL_JUSTICA, 10)); }
    public PessoaLocalizacaoResponse oficialLocalizarPessoa(PessoaLocalizacaoRequest request) { return pessoaLocalizacaoService.localizar(request, PessoaLocalizacaoService.CanalConsulta.OFICIAL_JUSTICA); }
    public SurfaceCollectionResponse delegadoInqueritosPendentes() { return projectionSupport.collection("delegado.inqueritos-pendentes", delegadoService.listarInqueritosPendentes()); }
    public SurfaceCollectionResponse delegadoMandadosPendentes() { return projectionSupport.collection("delegado.mandados-pendentes", delegadoService.listarMandadosPendentes()); }
    public SurfaceActionResponse delegadoSolicitarAcessoProcesso(Long processoId) { return projectionSupport.action("delegado.processo", "solicitar-acesso", processoId, delegadoService.solicitarAcessoProcesso(processoId)); }
    public SurfaceActionResponse delegadoRequisitarDiligencia(DelegadoDiligenciaRequest request) {
        Objects.requireNonNull(request);
        return projectionSupport.action("delegado.diligencia", "requisitar", request.processoId(), delegadoService.registrarDiligencia(request));
    }
    public SurfaceActionResponse delegadoRegistrarPecaInquerito(Long inqueritoId, DelegadoInqueritoMultimidiaRequest request) { return projectionSupport.action("delegado.inquerito", "registrar-peca-multimidia", inqueritoId, delegadoService.registrarPecaInquerito(inqueritoId, request)); }
    public SurfaceCollectionResponse delegadoAlertas() { return projectionSupport.collection("delegado.alertas", delegadoService.listarAlertasCrime()); }
    public SurfaceCollectionResponse delegadoConsultasRecentes() { return projectionSupport.collection("delegado.localizador.consultas-recentes", pessoaLocalizacaoService.listarRecentes(PessoaLocalizacaoService.CanalConsulta.DELEGADO, 20)); }
    public SurfaceSnapshotResponse delegadoMetricasLocalizador() { return projectionSupport.snapshot("delegado.localizador.metricas", pessoaLocalizacaoService.metricas(PessoaLocalizacaoService.CanalConsulta.DELEGADO, 10)); }
    public PessoaLocalizacaoResponse delegadoLocalizarPessoa(PessoaLocalizacaoRequest request) { return pessoaLocalizacaoService.localizar(request, PessoaLocalizacaoService.CanalConsulta.DELEGADO); }

    private Long parseLong(String value) {
        try {
            return value == null ? null : Long.parseLong(value);
        } catch (Exception ex) {
            return null;
        }
    }
}
