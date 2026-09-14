package com.tcc.pjb.backend.service.processual.surface;

import com.tcc.pjb.backend.core.processo.busca.domain.ProcessoAnalyticsAggregate;
import com.tcc.pjb.backend.core.processo.busca.domain.ProcessoBuscaAggregate;
import com.tcc.pjb.backend.core.processo.documental.domain.ProcessoDocumentoAggregate;
import com.tcc.pjb.backend.core.processo.dsl.domain.ProcessoDslAggregate;
import com.tcc.pjb.backend.core.processo.encaixe.domain.ProcessoEncaixeCarteiraAggregate;
import com.tcc.pjb.backend.core.processo.encaixe.domain.ProcessoEncaixeFinalAggregate;
import com.tcc.pjb.backend.core.processo.execucao.domain.ProcessoExecucaoAggregate;
import com.tcc.pjb.backend.core.processo.hardening.domain.ProcessoHardeningAggregate;
import com.tcc.pjb.backend.core.processo.integracao.domain.ProcessoIntegracaoAggregate;
import com.tcc.pjb.backend.core.processo.migracao.domain.ProcessoMigracaoAggregate;
import com.tcc.pjb.backend.core.processo.operacao.domain.ProcessoOperacaoAggregate;
import com.tcc.pjb.backend.core.processo.papel.domain.ProcessoPapelAggregate;
import com.tcc.pjb.backend.core.processo.papel.domain.ProcessoPapelPerfil;
import com.tcc.pjb.backend.core.processo.policy.domain.ProcessoPolicyAggregate;
import com.tcc.pjb.backend.core.processo.posse.domain.ProcessoPosseAggregate;
import com.tcc.pjb.backend.core.processo.prazo.domain.ProcessoPrazoAggregate;
import com.tcc.pjb.backend.core.processo.prazo.domain.ProcessoPrazoMarco;
import com.tcc.pjb.backend.core.processo.pregravacao.domain.ProcessoPreGravacaoAggregate;
import com.tcc.pjb.backend.core.processo.recursal.application.ProcessoRecursalDecisionCarryOverAssembler;
import com.tcc.pjb.backend.core.processo.recursal.domain.ProcessoRecursalAggregate;
import com.tcc.pjb.backend.core.processo.sigilo.domain.ProcessoSigiloAggregate;
import com.tcc.pjb.backend.core.processo.sigilo.domain.ProcessoSigiloInteligenteAggregate;
import com.tcc.pjb.backend.core.processo.sigilo.domain.ProcessoSigiloNotificacaoAggregate;
import com.tcc.pjb.backend.core.processo.timeline.domain.ProcessoTimelineAggregate;
import com.tcc.pjb.backend.core.processo.trabalho.domain.ProcessoTrabalhoAggregate;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoAggregate;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoAto;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoCompetencia;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoDiagnostico;
import com.tcc.pjb.backend.core.processo.vertical.domain.ProcessoVerticalAggregate;
import com.tcc.pjb.backend.model.dto.processual.surface.aggregate.ProcessoSurfaceAggregateResponse;
import com.tcc.pjb.backend.model.dto.processual.surface.aggregate.ProcessoSurfaceIdentityResponse;
import com.tcc.pjb.backend.model.dto.processual.surface.aggregate.ProcessoSurfaceValueItemResponse;
import com.tcc.pjb.backend.model.dto.processual.surface.unificado.ProcessoSurfaceAtoResponse;
import com.tcc.pjb.backend.model.dto.processual.surface.unificado.ProcessoSurfaceCompetenciaResponse;
import com.tcc.pjb.backend.model.dto.processual.surface.unificado.ProcessoSurfaceDiagnosticoResponse;
import com.tcc.pjb.backend.model.dto.processual.surface.unificado.ProcessoSurfacePerfilResponse;
import com.tcc.pjb.backend.model.dto.processual.surface.unificado.ProcessoUnificadoSurfaceResponse;
import com.tcc.pjb.backend.platform.jusos.v2.prazo.NationalPrazoEngine;
import java.lang.reflect.Method;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.modularity.PjbPublicApi;

@Service
@PjbPublicApi(module = PjbModuleId.PROCESSO_LIFECYCLE)
public class ProcessoSurfaceFacadeService {

    private final ProcessoSurfaceUnificadoOrchestrator unificadoOrchestrator;
    private final ProcessoSurfaceCicloJudicialOrchestrator cicloJudicialOrchestrator;
    private final ProcessoSurfacePapelPrazoOrchestrator papelPrazoOrchestrator;
    private final ProcessoSurfaceArtefatoOrchestrator artefatoOrchestrator;
    private final ProcessoSurfaceInfraestruturaOrchestrator infraestruturaOrchestrator;
    private final ProcessoSurfaceInsightGovernancaOrchestrator insightGovernancaOrchestrator;
    private final ProcessoSurfaceVerticalOrchestrator verticalOrchestrator;
    private final ProcessoSurfaceSigiloOrchestrator sigiloOrchestrator;

    public ProcessoSurfaceFacadeService(ProcessoSurfaceUnificadoOrchestrator unificadoOrchestrator,
                                        ProcessoSurfaceCicloJudicialOrchestrator cicloJudicialOrchestrator,
                                        ProcessoSurfacePapelPrazoOrchestrator papelPrazoOrchestrator,
                                        ProcessoSurfaceArtefatoOrchestrator artefatoOrchestrator,
                                        ProcessoSurfaceInfraestruturaOrchestrator infraestruturaOrchestrator,
                                        ProcessoSurfaceInsightGovernancaOrchestrator insightGovernancaOrchestrator,
                                        ProcessoSurfaceVerticalOrchestrator verticalOrchestrator,
                                        ProcessoSurfaceSigiloOrchestrator sigiloOrchestrator) {
        this.unificadoOrchestrator = Objects.requireNonNull(unificadoOrchestrator);
        this.cicloJudicialOrchestrator = Objects.requireNonNull(cicloJudicialOrchestrator);
        this.papelPrazoOrchestrator = Objects.requireNonNull(papelPrazoOrchestrator);
        this.artefatoOrchestrator = Objects.requireNonNull(artefatoOrchestrator);
        this.infraestruturaOrchestrator = Objects.requireNonNull(infraestruturaOrchestrator);
        this.insightGovernancaOrchestrator = Objects.requireNonNull(insightGovernancaOrchestrator);
        this.verticalOrchestrator = Objects.requireNonNull(verticalOrchestrator);
        this.sigiloOrchestrator = Objects.requireNonNull(sigiloOrchestrator);
    }

    public ProcessoUnificadoSurfaceResponse detalhar(Long processoId) {
        return toUnificado(unificadoOrchestrator.detalhar(processoId));
    }

    public ProcessoSurfaceCompetenciaResponse competencia(Long processoId) {
        return toCompetencia(unificadoOrchestrator.competencia(processoId));
    }

    public List<ProcessoSurfaceAtoResponse> atos(Long processoId) {
        return unificadoOrchestrator.catalogoAtos(processoId).stream().map(this::toAto).toList();
    }

    public ProcessoSurfaceDiagnosticoResponse diagnostico(Long processoId) {
        return toDiagnostico(unificadoOrchestrator.diagnosticar(processoId));
    }

    public ProcessoSurfaceAggregateResponse recursal(Long processoId) {
        return toRecursal(cicloJudicialOrchestrator.recursal(processoId));
    }

    public ProcessoSurfaceAggregateResponse execucao(Long processoId) {
        return toExecucao(cicloJudicialOrchestrator.execucao(processoId));
    }

    public ProcessoSurfaceAggregateResponse papeis(Long processoId) {
        return toPapeis(papelPrazoOrchestrator.papeis(processoId));
    }

    public ProcessoSurfacePerfilResponse papel(Long processoId, String profileCode) {
        return toPerfil(papelPrazoOrchestrator.perfil(processoId, profileCode));
    }

    public ProcessoSurfaceAggregateResponse prazos(Long processoId) {
        return toPrazos(papelPrazoOrchestrator.prazos(processoId));
    }

    public ProcessoSurfaceValueItemResponse prazoEspecifico(Long processoId, String tipoPrazo) {
        ProcessoPrazoMarco marco = papelPrazoOrchestrator.calcularPrazo(processoId, NationalPrazoEngine.TipoPrazo.valueOf(tipoPrazo.toUpperCase()));
        return new ProcessoSurfaceValueItemResponse(String.valueOf(marco));
    }

    public ProcessoSurfaceAggregateResponse workstream(Long processoId) {
        return toWorkstream(artefatoOrchestrator.workstream(processoId));
    }

    public ProcessoSurfaceAggregateResponse documentos(Long processoId) {
        return toDocumentos(artefatoOrchestrator.documentos(processoId));
    }

    public ProcessoSurfaceAggregateResponse timeline(Long processoId) {
        return toTimeline(artefatoOrchestrator.timeline(processoId));
    }

    public ProcessoSurfaceAggregateResponse integracoes(Long processoId) {
        return toIntegracoes(infraestruturaOrchestrator.integracoes(processoId));
    }

    public ProcessoSurfaceAggregateResponse migracao(Long processoId) {
        return toMigracao(infraestruturaOrchestrator.migracao(processoId));
    }

    public ProcessoSurfaceAggregateResponse operacao(Long processoId) {
        return toOperacao(infraestruturaOrchestrator.operacao(processoId));
    }

    public ProcessoSurfaceAggregateResponse busca(String cpf,
                                                  String nome,
                                                  String numero,
                                                  String uf,
                                                  String comarca,
                                                  String ramo,
                                                  String status,
                                                  String tribunal,
                                                  int page,
                                                  int size) {
        return toBusca(insightGovernancaOrchestrator.buscar(cpf, nome, numero, uf, comarca, ramo, status, tribunal, page, size));
    }

    public ProcessoSurfaceAggregateResponse analytics(String ramo, String tribunal, String uf, String comarca) {
        return toAnalytics(insightGovernancaOrchestrator.analytics(ramo, tribunal, uf, comarca));
    }

    public ProcessoSurfaceAggregateResponse encaixeFinal(Long processoId) {
        return toEncaixeFinal(insightGovernancaOrchestrator.encaixeFinal(processoId));
    }

    public ProcessoSurfaceAggregateResponse encaixeFinalCarteira(int limit) {
        return toEncaixeCarteira(insightGovernancaOrchestrator.encaixeCarteira(limit));
    }

    public ProcessoSurfaceAggregateResponse dsl(Long processoId) {
        return toDsl(insightGovernancaOrchestrator.dsl(processoId));
    }

    public ProcessoSurfaceAggregateResponse policy(Long processoId, LocalDate em) {
        return toPolicy(em == null
                ? insightGovernancaOrchestrator.policy(processoId)
                : insightGovernancaOrchestrator.policy(processoId, em));
    }

    public ProcessoSurfaceAggregateResponse posse(Long processoId) {
        return toPosse(insightGovernancaOrchestrator.posse(processoId));
    }

    public ProcessoSurfaceAggregateResponse preGravacao(Long processoId, String profileCode, String actionCode) {
        return toPreGravacao(insightGovernancaOrchestrator.preGravacao(processoId, profileCode, actionCode));
    }

    public ProcessoSurfaceAggregateResponse fatiaCivel(Long processoId) {
        return toVertical(verticalOrchestrator.civel(processoId));
    }

    public ProcessoSurfaceAggregateResponse fatiaPenalCustodia(Long processoId) {
        return toVertical(verticalOrchestrator.penalCustodia(processoId));
    }

    public ProcessoSurfaceAggregateResponse fatiaExecucaoFiscal(Long processoId) {
        return toVertical(verticalOrchestrator.execucaoFiscal(processoId));
    }

    public ProcessoSurfaceAggregateResponse sigilo(Long processoId) {
        return toSigilo(sigiloOrchestrator.sigilo(processoId));
    }

    public ProcessoSurfaceAggregateResponse hardening(Long processoId) {
        return toHardening(sigiloOrchestrator.hardening(processoId));
    }

    public ProcessoSurfaceAggregateResponse sigiloInteligente(Long processoId) {
        return toSigiloInteligente(sigiloOrchestrator.sigiloInteligente(processoId));
    }

    public ProcessoSurfaceAggregateResponse sigiloNotificacoes(Long processoId) {
        return toSigiloNotificacoes(sigiloOrchestrator.planejarSigiloNotificacoes(processoId));
    }

    public ProcessoSurfaceAggregateResponse dispararSigiloNotificacoes(Long processoId) {
        return toSigiloNotificacoes(sigiloOrchestrator.dispararSigiloNotificacoes(processoId));
    }

    private ProcessoUnificadoSurfaceResponse toUnificado(ProcessoUnificadoAggregate aggregate) {
        return new ProcessoUnificadoSurfaceResponse(
                identity(aggregate.identity()),
                toCompetencia(aggregate.competencia()),
                toDiagnostico(aggregate.diagnostico()),
                aggregate.atosPermitidos().stream().map(this::toAto).toList(),
                aggregate.atosBloqueados().stream().map(this::toAto).toList(),
                aggregate.proximoMelhorAto(),
                aggregate.generatedAt());
    }

    private ProcessoSurfaceCompetenciaResponse toCompetencia(ProcessoUnificadoCompetencia competencia) {
        return new ProcessoSurfaceCompetenciaResponse(
                competencia.tipoJustica(),
                competencia.grauJurisdicao(),
                competencia.ramoDireito(),
                competencia.ritoProcessual(),
                competencia.faseProcessual(),
                competencia.statusProcessual(),
                competencia.tribunalCodigo(),
                competencia.tribunalNome(),
                competencia.orgaoJulgadorSugerido(),
                competencia.unidadeJudiciariaSugerida(),
                competencia.filaDistribuicao(),
                competencia.mesaTriagem(),
                competencia.preventionMode(),
                competencia.distributionMode(),
                competencia.routingRiskLevel(),
                competencia.sigiloPadrao(),
                competencia.conciliacaoObrigatoria(),
                competencia.prazoTriagemHoras(),
                competencia.alertas(),
                competencia.fundamentos(),
                competencia.reviewChecklist(),
                competencia.metadata());
    }

    private ProcessoSurfaceDiagnosticoResponse toDiagnostico(ProcessoUnificadoDiagnostico diagnostico) {
        return new ProcessoSurfaceDiagnosticoResponse(
                diagnostico.healthy(),
                diagnostico.totalFindings(),
                diagnostico.blockingFindings(),
                diagnostico.atosPermitidos(),
                diagnostico.atosBloqueados(),
                diagnostico.atosSensiveis(),
                diagnostico.atosComSegurancaElevada(),
                toItems(diagnostico.findings()),
                diagnostico.fundamentos(),
                diagnostico.generatedAt());
    }

    private ProcessoSurfaceAtoResponse toAto(ProcessoUnificadoAto ato) {
        return new ProcessoSurfaceAtoResponse(ato.codigo(), ato.titulo(), ato.categoria(), ato.permitido(), ato.sensivel(), ato.motivo(), ato.alertas());
    }

    private ProcessoSurfacePerfilResponse toPerfil(ProcessoPapelPerfil perfil) {
        return new ProcessoSurfacePerfilResponse(
                perfil.codigo(),
                perfil.nomeExibicao(),
                perfil.painel(),
                perfil.processProfile(),
                perfil.trustFloor(),
                perfil.accentColor(),
                perfil.visualizar(),
                perfil.receber(),
                perfil.preparar(),
                perfil.aprovar(),
                perfil.assinar(),
                perfil.peticionar(),
                perfil.certificar(),
                perfil.redistribuir(),
                perfil.recorrer(),
                perfil.embargar(),
                perfil.sugerir(),
                perfil.separadores(),
                perfil.guardas(),
                perfil.fundamentos());
    }

    private ProcessoSurfaceAggregateResponse toRecursal(ProcessoRecursalAggregate aggregate) {
        java.util.LinkedHashMap<String, String> labels = new java.util.LinkedHashMap<>(mapOf(
                "instanciaAtual", aggregate.instanciaAtual()));
        if (aggregate.cadernoDecisorioOrigem() != null) {
            putIfPresent(labels, "cadernoDecisorioEscopo", aggregate.cadernoDecisorioOrigem().scope());
            putIfPresent(labels, "tipoDecisaoAnterior", aggregate.cadernoDecisorioOrigem().sourceDecisionType());
            putIfPresent(labels, "orgaoOrigem", aggregate.cadernoDecisorioOrigem().sourceOrganLabel());
            putIfPresent(labels, "processoOrigem", aggregate.cadernoDecisorioOrigem().numeroCnjOrigem());
            if (aggregate.cadernoDecisorioOrigem().documentoOriginalDecisao() != null) {
                putIfPresent(labels, "documentoOriginalDecisao", aggregate.cadernoDecisorioOrigem().documentoOriginalDecisao().displayTitle());
                putIfPresent(labels, "pdfDecisaoOriginal", aggregate.cadernoDecisorioOrigem().documentoOriginalDecisao().pdfEndpoint());
            }
            if (!aggregate.cadernoDecisorioOrigem().trilhaDecisoriaIntegral().isEmpty()) {
                putIfPresent(labels, "totalDecisoesAcopladas", String.valueOf(aggregate.cadernoDecisorioOrigem().trilhaDecisoriaIntegral().size()));
            }
        }
        return aggregate(identity(aggregate.identity()), "recursal", aggregate.familiaRecursal(), labels, metricsOf(
                        "totalCabiveis", aggregate.totalCabiveis(),
                        "totalMesmosAutos", aggregate.totalMesmosAutos(),
                        "totalExterno", aggregate.totalExterno(),
                        "totalEmbargos", aggregate.totalEmbargos()),
                mergeLists(aggregate.travas(), aggregate.proximosPassos()), aggregate.alertas(), recursalItems(aggregate), aggregate.geradoEm());
    }

    private java.util.List<?> recursalItems(ProcessoRecursalAggregate aggregate) {
        java.util.ArrayList<Object> itens = new java.util.ArrayList<>();
        itens.addAll(ProcessoRecursalDecisionCarryOverAssembler.toSurfaceLines(aggregate.cadernoDecisorioOrigem()));
        itens.addAll(aggregate.janelas());
        return java.util.List.copyOf(itens);
    }

    private ProcessoSurfaceAggregateResponse toExecucao(ProcessoExecucaoAggregate aggregate) {
        return aggregate(identity(aggregate.identity()), "execucao", aggregate.processoExecutivo() ? "EXECUTIVO" : "NAO_EXECUTIVO", mapOf(), metricsOf(
                        "totalTrilhas", aggregate.totalTrilhas(),
                        "totalBloqueantes", aggregate.totalBloqueantes(),
                        "totalMandados", aggregate.totalMandados(),
                        "totalOperacoesCustodia", aggregate.totalOperacoesCustodia()),
                aggregate.proximoMelhorPasso(), aggregate.alertas(), aggregate.trilhas(), aggregate.geradoEm());
    }

    private ProcessoSurfaceAggregateResponse toPapeis(ProcessoPapelAggregate aggregate) {
        return aggregate(identity(aggregate.identity()), "papeis", "PERFIS", mapOf(), metricsOf(
                        "totalPerfis", aggregate.totalPerfis(),
                        "totalAssinantes", aggregate.totalAssinantes(),
                        "totalRecursais", aggregate.totalRecursais(),
                        "totalCertificadores", aggregate.totalCertificadores()),
                List.of(), aggregate.alertas(), aggregate.perfis().stream().map(p -> p.codigo() + ":" + p.nomeExibicao()).toList(), aggregate.geradoEm());
    }

    private ProcessoSurfaceAggregateResponse toPrazos(ProcessoPrazoAggregate aggregate) {
        return aggregate(identity(aggregate.identity()), "prazos", aggregate.janelaPredominante(), mapOf(
                        "ciencia", String.valueOf(aggregate.ciencia())), metricsOf(
                        "totalMarcos", aggregate.totalMarcos(),
                        "marcosVencidos", aggregate.marcosVencidos(),
                        "marcosCriticos", aggregate.marcosCriticos(),
                        "marcosComCienciaObrigatoria", aggregate.marcosComCienciaObrigatoria()),
                aggregate.proximaOndaOperacional(), aggregate.alertasEstruturais(), aggregate.marcos(), aggregate.generatedAt());
    }

    private ProcessoSurfaceAggregateResponse toWorkstream(ProcessoTrabalhoAggregate aggregate) {
        return aggregate(identity(aggregate.identity()), "workstream", aggregate.faixaOperacional(), mapOf(), metricsOf(
                        "totalWorkItems", aggregate.totalWorkItems(),
                        "pendentes", aggregate.pendentes(),
                        "emExecucao", aggregate.emExecucao(),
                        "bloqueantes", aggregate.bloqueantes(),
                        "vencidos", aggregate.vencidos(),
                        "semResponsavelNominal", aggregate.semResponsavelNominal()),
                aggregate.proximoMelhorFluxo(), aggregate.gates(), aggregate.filas(), aggregate.generatedAt());
    }

    private ProcessoSurfaceAggregateResponse toDocumentos(ProcessoDocumentoAggregate aggregate) {
        return aggregate(identity(aggregate.identity()), "documentos", "DOCUMENTAL", mapOf(), metricsOf(
                        "totalDocumentos", aggregate.totalDocumentos(),
                        "lotes", aggregate.lotes(),
                        "minutas", aggregate.minutas(),
                        "assinados", aggregate.assinados(),
                        "custodiados", aggregate.custodiados(),
                        "publicados", aggregate.publicados()),
                aggregate.trilhaAssinavel(), aggregate.alertas(), aggregate.grupos(), aggregate.generatedAt());
    }

    private ProcessoSurfaceAggregateResponse toTimeline(ProcessoTimelineAggregate aggregate) {
        return aggregate(identity(aggregate.identity()), "timeline", "EVOLUCAO", mapOf(
                        "eixosAtivos", String.join(", ", aggregate.eixosAtivos())), metricsOf(
                        "totalEventos", aggregate.totalEventos(),
                        "totalPendencias", aggregate.totalPendencias(),
                        "totalBloqueantes", aggregate.totalBloqueantes()),
                aggregate.proximoCiclo(), aggregate.alertas(), aggregate.eventos(), aggregate.geradoEm());
    }

    private ProcessoSurfaceAggregateResponse toIntegracoes(ProcessoIntegracaoAggregate aggregate) {
        return aggregate(identity(aggregate.identity()), "integracoes", aggregate.prontidaoEnvio(), mapOf(
                        "trilhaConnector", aggregate.trilhaConnector(),
                        "prontidaoShadow", aggregate.prontidaoShadow()), Map.of(),
                aggregate.proximasAcoes(), aggregate.alertas(), aggregate.eventos(), aggregate.geradoEm());
    }

    private ProcessoSurfaceAggregateResponse toMigracao(ProcessoMigracaoAggregate aggregate) {
        return aggregate(identity(aggregate.identity()), "migracao", aggregate.readiness(), mapOf(
                        "canCutOver", String.valueOf(aggregate.canCutOver())), Map.of(),
                aggregate.proximasOndas(), aggregate.alertas(), aggregate.comparacoes(), aggregate.geradoEm());
    }

    private ProcessoSurfaceAggregateResponse toOperacao(ProcessoOperacaoAggregate aggregate) {
        return aggregate(identity(aggregate.identity()), "operacao", aggregate.readiness(), mapOf(
                        "resilienceState", aggregate.resilienceState(),
                        "observabilityState", aggregate.observabilityState(),
                        "migrationState", aggregate.migrationState(),
                        "saturacaoMaxima", String.valueOf(aggregate.saturacaoMaxima())), metricsOf(
                        "totalBloqueios", aggregate.totalBloqueios()),
                aggregate.acoesImediatas(), aggregate.alertas(), aggregate.faixas(), aggregate.geradoEm());
    }

    private ProcessoSurfaceAggregateResponse toBusca(ProcessoBuscaAggregate aggregate) {
        Map<String, String> labels = new LinkedHashMap<>(aggregate.filtros());
        labels.put("pagina", String.valueOf(aggregate.pagina()));
        labels.put("tamanho", String.valueOf(aggregate.tamanho()));
        labels.put("filtragemPosPagina", String.valueOf(aggregate.filtragemPosPagina()));
        return aggregate(null, "busca", "BUSCA", labels, metricsOf("totalAmostra", aggregate.totalAmostra()), List.of(), aggregate.alertas(), mergeCollections(aggregate.resultados(), aggregate.facets()), aggregate.geradoEm());
    }

    private ProcessoSurfaceAggregateResponse toAnalytics(ProcessoAnalyticsAggregate aggregate) {
        Map<String, String> labels = new LinkedHashMap<>(aggregate.escopo());
        labels.put("tempoMedioDias", String.valueOf(aggregate.tempoMedioDias()));
        labels.put("taxaRecursal", String.valueOf(aggregate.taxaRecursal()));
        labels.put("taxaAcordo", String.valueOf(aggregate.taxaAcordo()));
        labels.put("taxaUrgencia", String.valueOf(aggregate.taxaUrgencia()));
        return aggregate(null, "analytics", "ANALYTICS", labels, metricsOf(
                        "totalProcessos", aggregate.totalProcessos(),
                        "totalAtivos", aggregate.totalAtivos()),
                List.of(), aggregate.alertas(), aggregate.indicadores(), aggregate.geradoEm());
    }

    private ProcessoSurfaceAggregateResponse toEncaixeFinal(ProcessoEncaixeFinalAggregate aggregate) {
        ProcessoSurfaceIdentityResponse identity = new ProcessoSurfaceIdentityResponse(aggregate.processoId(), aggregate.numeroProcesso(), null, null, null, null, null, null, null, null, List.of());
        return aggregate(identity, "encaixe-final", aggregate.readiness(), mapOf(), metricsOf(
                        "score", aggregate.score(),
                        "totalFindings", aggregate.totalFindings(),
                        "totalBloqueantes", aggregate.totalBloqueantes()),
                aggregate.acoesCorretivas(), List.of(), mergeCollections(aggregate.eixos(), aggregate.findings()), aggregate.geradoEm());
    }

    private ProcessoSurfaceAggregateResponse toEncaixeCarteira(ProcessoEncaixeCarteiraAggregate aggregate) {
        return aggregate(null, "encaixe-carteira", "CARTEIRA", mapOf(), metricsOf(
                        "totalEscaneados", aggregate.totalEscaneados(),
                        "totalBloqueantes", aggregate.totalBloqueantes(),
                        "scoreMedio", aggregate.scoreMedio()),
                List.of(), aggregate.alertas(), mergeCollections(aggregate.processos(), aggregate.tendencias()), aggregate.geradoEm());
    }

    private ProcessoSurfaceAggregateResponse toDsl(ProcessoDslAggregate aggregate) {
        return aggregate(identity(aggregate.identity()), "dsl", String.valueOf(aggregate.version()), mapOf(), metricsOf(
                        "totalRules", aggregate.totalRules(),
                        "blockingRules", aggregate.blockingRules()),
                aggregate.invariants(), List.of(), aggregate.blocks(), aggregate.generatedAt());
    }

    private ProcessoSurfaceAggregateResponse toPolicy(ProcessoPolicyAggregate aggregate) {
        return aggregate(identity(aggregate.identity()), "policy", String.valueOf(aggregate.referenceDate()), mapOf(), metricsOf(
                        "totalWindows", aggregate.totalWindows(),
                        "activeWindows", aggregate.activeWindows(),
                        "blockingPolicies", aggregate.blockingPolicies()),
                aggregate.invariants(), List.of(), mergeCollections(aggregate.windows(), aggregate.decisions()), aggregate.generatedAt());
    }

    private ProcessoSurfaceAggregateResponse toPosse(ProcessoPosseAggregate aggregate) {
        return aggregate(identity(aggregate.identity()), "posse", "POSSE", mapOf(), metricsOf(
                        "totalItems", aggregate.totalItems(),
                        "openItems", aggregate.openItems(),
                        "transitivelyClaimable", aggregate.transitivelyClaimable(),
                        "findings", aggregate.findings()),
                List.of(), aggregate.alerts(), aggregate.items(), aggregate.generatedAt());
    }

    private ProcessoSurfaceAggregateResponse toPreGravacao(ProcessoPreGravacaoAggregate aggregate) {
        return aggregate(identity(aggregate.identity()), "pre-gravacao", aggregate.actionCode(), mapOf(
                        "profileCode", aggregate.profileCode(),
                        "persistenciaPermitida", String.valueOf(aggregate.persistenciaPermitida())), metricsOf(
                        "totalTriggers", aggregate.totalTriggers(),
                        "blockingTriggers", aggregate.blockingTriggers(),
                        "stepUpTriggers", aggregate.stepUpTriggers(),
                        "mandatoryGuardCount", aggregate.mandatoryGuardCount()),
                mergeLists(aggregate.correctivePlan(), aggregate.mandatoryGuards()), aggregate.fundamentos(), aggregate.triggers(), aggregate.generatedAt());
    }

    private ProcessoSurfaceAggregateResponse toVertical(ProcessoVerticalAggregate aggregate) {
        return aggregate(identity(aggregate.identity()), aggregate.sliceCode(), aggregate.sliceTitle(), mapOf(
                        "ritoDominante", aggregate.ritoDominante(),
                        "faseAtual", aggregate.faseAtual(),
                        "statusAtual", aggregate.statusAtual()), metricsOf(
                        "totalEtapas", aggregate.totalEtapas(),
                        "totalLanes", aggregate.totalLanes(),
                        "totalPendenciasCriticas", aggregate.totalPendenciasCriticas(),
                        "totalHandoffs", aggregate.totalHandoffs()),
                mergeLists(aggregate.nextBestFlow(), aggregate.processChips()), aggregate.alertas(), mergeCollections(aggregate.lanes(), aggregate.etapas()), aggregate.generatedAt());
    }

    private ProcessoSurfaceAggregateResponse toSigilo(ProcessoSigiloAggregate aggregate) {
        return aggregate(identity(aggregate.identity()), "sigilo", String.valueOf(aggregate.nivelSigilo()), mapOf(
                        "disclosureMode", aggregate.disclosureMode(),
                        "exigeCredencial", String.valueOf(aggregate.exigeCredencial()),
                        "exigeStepUp", String.valueOf(aggregate.exigeStepUp()),
                        "exigeDuplaAutorizacao", String.valueOf(aggregate.exigeDuplaAutorizacao())), metricsOf(
                        "pendingApprovals", aggregate.pendingApprovals(),
                        "approvedCredentials", aggregate.approvedCredentials(),
                        "totalGuardas", aggregate.totalGuardas(),
                        "totalFindings", aggregate.totalFindings()),
                aggregate.fundamentos(), aggregate.chips(), mergeCollections(aggregate.guardas(), aggregate.findings()), aggregate.generatedAt());
    }

    private ProcessoSurfaceAggregateResponse toHardening(ProcessoHardeningAggregate aggregate) {
        return aggregate(identity(aggregate.identity()), "hardening", aggregate.readiness(), mapOf(), metricsOf(
                        "hardeningScore", aggregate.hardeningScore(),
                        "blockingFindings", aggregate.blockingFindings(),
                        "totalFindings", aggregate.totalFindings()),
                aggregate.correctivePlan(), aggregate.fundamentos(), mergeCollections(aggregate.hardeningAxes(), aggregate.findings()), aggregate.generatedAt());
    }

    private ProcessoSurfaceAggregateResponse toSigiloInteligente(ProcessoSigiloInteligenteAggregate aggregate) {
        return aggregate(identity(aggregate.identity()), "sigilo-inteligente", aggregate.statusClassificacao(), mapOf(
                        "nivelAtual", String.valueOf(aggregate.nivelAtual()),
                        "nivelRecomendado", String.valueOf(aggregate.nivelRecomendado()),
                        "audienceMode", aggregate.audienceMode(),
                        "revisaoJudicialObrigatoria", String.valueOf(aggregate.revisaoJudicialObrigatoria()),
                        "decretoExclusivoMagistrado", String.valueOf(aggregate.decretoExclusivoMagistrado()),
                        "operacaoPolicialSigilosa", String.valueOf(aggregate.operacaoPolicialSigilosa()),
                        "protecaoDocumentalReforcada", String.valueOf(aggregate.protecaoDocumentalReforcada())), Map.of(),
                mergeLists(aggregate.fundamentos(), aggregate.triggers()), List.of(), mergeCollections(aggregate.destinatarios(), mergeCollections(aggregate.protecoesDados(), aggregate.findings())), aggregate.generatedAt());
    }

    private ProcessoSurfaceAggregateResponse toSigiloNotificacoes(ProcessoSigiloNotificacaoAggregate aggregate) {
        return aggregate(identity(aggregate.identity()), "sigilo-notificacoes", aggregate.statusPlanejamento(), mapOf(
                        "channels", String.join(", ", aggregate.channels())), metricsOf(
                        "totalDestinatarios", aggregate.totalDestinatarios(),
                        "totalComUsuario", aggregate.totalComUsuario(),
                        "totalAltaPrioridade", aggregate.totalAltaPrioridade()),
                aggregate.fundamentos(), List.of(), aggregate.notificacoes(), aggregate.generatedAt());
    }

    private ProcessoSurfaceAggregateResponse aggregate(ProcessoSurfaceIdentityResponse identity,
                                                       String dominio,
                                                       String estado,
                                                       Map<String, String> labels,
                                                       Map<String, Long> metricas,
                                                       List<String> proximosPassos,
                                                       List<String> alertas,
                                                       Collection<?> itens,
                                                       Instant generatedAt) {
        return new ProcessoSurfaceAggregateResponse(identity, dominio, estado, labels, metricas, alertas == null ? List.of() : alertas, proximosPassos == null ? List.of() : proximosPassos, toItems(itens), generatedAt == null ? Instant.now() : generatedAt);
    }

    private ProcessoSurfaceIdentityResponse identity(Object identity) {
        if (identity == null) {
            return null;
        }
        return new ProcessoSurfaceIdentityResponse(
                readLong(identity, "processoId"),
                readString(identity, "numeroProcesso", "numeroUnificado"),
                readString(identity, "tribunal"),
                readString(identity, "uf"),
                readString(identity, "comarca"),
                readString(identity, "unidadeJudiciaria", "unidade"),
                readString(identity, "ramoDireito", "ramo"),
                readString(identity, "ritoProcessual", "rito"),
                readString(identity, "faseProcessual", "fase"),
                readString(identity, "statusProcessual", "status"),
                readStringList(identity, "marcadores", "etiquetas"));
    }

    private List<ProcessoSurfaceValueItemResponse> toItems(Collection<?> itens) {
        if (itens == null) {
            return List.of();
        }
        return itens.stream().map(item -> new ProcessoSurfaceValueItemResponse(String.valueOf(item))).toList();
    }

    private List<String> mergeLists(List<String> left, List<String> right) {
        return mergeCollections(left, right).stream().map(String::valueOf).toList();
    }

    private List<?> mergeCollections(Collection<?> left, Collection<?> right) {
        List<Object> merged = new java.util.ArrayList<>();
        if (left != null) {
            merged.addAll(left);
        }
        if (right != null) {
            merged.addAll(right);
        }
        return List.copyOf(merged);
    }

    private Map<String, String> mapOf(String... values) {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < values.length; i += 2) {
            if (values[i + 1] != null) {
                map.put(values[i], values[i + 1]);
            }
        }
        return Map.copyOf(map);
    }

    private void putIfPresent(Map<String, String> target, String key, String value) {
        if (value != null && !value.isBlank()) {
            target.put(key, value);
        }
    }

    private Map<String, Long> metricsOf(Object... values) {
        LinkedHashMap<String, Long> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < values.length; i += 2) {
            Object value = values[i + 1];
            if (value instanceof Number number) {
                map.put(String.valueOf(values[i]), number.longValue());
            }
        }
        return Map.copyOf(map);
    }

    private Long readLong(Object target, String method) {
        Object value = invoke(target, method);
        return value instanceof Number number ? number.longValue() : null;
    }

    private String readString(Object target, String... methods) {
        for (String method : methods) {
            Object value = invoke(target, method);
            if (value != null) {
                return String.valueOf(value);
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<String> readStringList(Object target, String... methods) {
        for (String method : methods) {
            Object value = invoke(target, method);
            if (value instanceof List<?> list) {
                return list.stream().map(String::valueOf).toList();
            }
        }
        return List.of();
    }

    private Object invoke(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}
