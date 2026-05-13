package com.tcc.pjb.backend.service.secretariat.surface;

import com.tcc.pjb.backend.model.dto.profile.operational.SecretariaBaixaOrigemRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.SecretariaBalcaoVirtualMilitarRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.SecretariaCorregedoriaEleitoralRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.SecretariaExecucaoTrabalhistaRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.SecretariaInspecaoCorregedoriaRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.SecretariaMidiaProcessualRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.SecretariaPautaColegiadaRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.SecretariaPesquisaEleitoralRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.SecretariaPlantaoMilitarRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.SecretariaPublicacaoAcordaoRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.SecretariaPublicacaoPautaRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.SecretariaSustentacaoOralRequest;
import com.tcc.pjb.backend.model.dto.secretariat.oficial.SecretariaOficialCumprimentoMaterializacaoRequest;
import com.tcc.pjb.backend.model.dto.secretariat.oficial.SecretariaOficialCumprimentoReclassificacaoRequest;
import com.tcc.pjb.backend.model.dto.secretariat.surface.SecretariatJuntadaItemResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceActionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.service.secretariat.routing.SecretariaEspecializadaRoutingService;
import com.tcc.pjb.backend.service.secretariat.oficial.SecretariaOficialCumprimentoRoutingService;
import com.tcc.pjb.backend.service.secretariat.oficial.SecretariatOfficialActsDrawerService;
import com.tcc.pjb.backend.service.secretariat.operational.SecretariatCollegiateOperationalExecutionService;
import com.tcc.pjb.backend.service.secretariat.operational.SecretariatSpecializedOperationalExecutionService;
import com.tcc.pjb.backend.service.secretariat.orchestration.SecretariatOperationalOrchestrationService;
import com.tcc.pjb.backend.service.juiz.handoff.JuizGabineteHandoffService;
import com.tcc.pjb.backend.service.secretariat.topology.JudicialTopologyCoordinationMatrixService;
import com.tcc.pjb.backend.service.secretariat.topology.JudicialTopologySegregationMeshService;
import com.tcc.pjb.backend.service.secretariat.export.SecretariatMinutaJuntadaPdfService;
import com.tcc.pjb.backend.service.secretariat.access.SecretariatInstitutionalVisibilityService;
import com.tcc.pjb.backend.service.servidor.ServidorSecretariaOperacionalService;
import com.tcc.pjb.backend.service.surface.common.SurfaceProjectionSupport;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SecretariatOperationalSurfaceFacadeService {

    private final ServidorSecretariaOperacionalService operacionalService;
    private final SecretariaEspecializadaRoutingService routingService;
    private final SecretariaOficialCumprimentoRoutingService officialClosureRoutingService;
    private final SecretariatOfficialActsDrawerService officialActsDrawerService;
    private final SecretariatMinutaJuntadaPdfService minutaService;
    private final SurfaceProjectionSupport projectionSupport;
    private final SecretariatOperationalOrchestrationService orchestrationService;
    private final SecretariatCollegiateOperationalExecutionService collegiateOperationalExecutionService;
    private final SecretariatSpecializedOperationalExecutionService specializedOperationalExecutionService;
    private final JuizGabineteHandoffService handoffService;
    private final JudicialTopologySegregationMeshService judicialTopologySegregationMeshService;
    private final JudicialTopologyCoordinationMatrixService judicialTopologyCoordinationMatrixService;
    private final SecretariatInstitutionalVisibilityService visibilityService;

    public SecretariatOperationalSurfaceFacadeService(ServidorSecretariaOperacionalService operacionalService,
                                                      SecretariaEspecializadaRoutingService routingService,
                                                      SecretariaOficialCumprimentoRoutingService officialClosureRoutingService,
                                                      SecretariatOfficialActsDrawerService officialActsDrawerService,
                                                      SecretariatMinutaJuntadaPdfService minutaService,
                                                      SurfaceProjectionSupport projectionSupport,
                                                      SecretariatOperationalOrchestrationService orchestrationService,
                                                      SecretariatCollegiateOperationalExecutionService collegiateOperationalExecutionService,
                                                      SecretariatSpecializedOperationalExecutionService specializedOperationalExecutionService,
                                                      JuizGabineteHandoffService handoffService,
                                                      JudicialTopologySegregationMeshService judicialTopologySegregationMeshService,
                                                      JudicialTopologyCoordinationMatrixService judicialTopologyCoordinationMatrixService,
                                                      SecretariatInstitutionalVisibilityService visibilityService) {
        this.operacionalService = operacionalService;
        this.routingService = routingService;
        this.officialClosureRoutingService = officialClosureRoutingService;
        this.officialActsDrawerService = officialActsDrawerService;
        this.minutaService = minutaService;
        this.projectionSupport = projectionSupport;
        this.orchestrationService = orchestrationService;
        this.collegiateOperationalExecutionService = collegiateOperationalExecutionService;
        this.specializedOperationalExecutionService = specializedOperationalExecutionService;
        this.handoffService = handoffService;
        this.judicialTopologySegregationMeshService = judicialTopologySegregationMeshService;
        this.judicialTopologyCoordinationMatrixService = judicialTopologyCoordinationMatrixService;
        this.visibilityService = visibilityService;
    }

    public SurfaceSnapshotResponse snapshotOperacional() {
        return projectionSupport.snapshot("secretaria.operacional", operacionalService.bootstrapSecretaria());
    }

    public SurfaceActionResponse realizarJuntada(Long processoId, String tipoDocumento, String descricao, String origem) {
        visibilityService.requireProcessAccess(processoId);
        return projectionSupport.action("secretaria.operacional", "realizarJuntada", processoId,
                operacionalService.realizarJuntada(processoId, tipoDocumento, descricao, origem));
    }

    public SurfaceActionResponse expedirIntimacao(Long processoId, String destinatario, String conteudo, String prazo, Long oficialId, Boolean reativarOficial, String origemOperacional, String fundamentoOperacional, String observacaoOperacional, Boolean manterRetornoForumAberto) {
        visibilityService.requireProcessAccess(processoId);
        return projectionSupport.action("secretaria.operacional", "expedirIntimacao", processoId,
                operacionalService.expedicaoIntimacao(processoId, destinatario, conteudo, prazo, oficialId, reativarOficial, origemOperacional, fundamentoOperacional, observacaoOperacional, manterRetornoForumAberto));
    }

    public SurfaceActionResponse conclusaoParaDespacho(Long processoId, String motivoConclusao) {
        visibilityService.requireProcessAccess(processoId);
        return projectionSupport.action("secretaria.operacional", "conclusaoParaDespacho", processoId,
                operacionalService.conclusaoParaDespacho(processoId, motivoConclusao));
    }

    public SurfaceActionResponse saneamentoFila(String queueCode, int limite) {
        visibilityService.requireQueueAccess(queueCode);
        return projectionSupport.action("secretaria.operacional", "saneamentoFila", null,
                operacionalService.saneamentoBulkFila(queueCode, limite));
    }
    public SurfaceSnapshotResponse oficialCumprimentos(String inboxKey, int limit) {
        visibilityService.requireInboxAccess(inboxKey);
        return projectionSupport.snapshot("secretaria.operacional", officialClosureRoutingService.inbox(inboxKey, limit));
    }

    public SurfaceActionResponse reclassificarOficialCumprimento(Long deskWorkItemId, SecretariaOficialCumprimentoReclassificacaoRequest request) {
        visibilityService.requireDeskAccess(deskWorkItemId);
        return projectionSupport.action("secretaria.operacional", "reclassificarOficialCumprimento", null,
                officialClosureRoutingService.reclassificar(deskWorkItemId, request));
    }

    public SurfaceSnapshotResponse proximaProvidenciaOficialCumprimento(Long deskWorkItemId) {
        visibilityService.requireDeskAccess(deskWorkItemId);
        return projectionSupport.snapshot("secretaria.operacional", officialClosureRoutingService.proximaProvidencia(deskWorkItemId));
    }

    public SurfaceActionResponse materializarAtoOficialCumprimento(Long deskWorkItemId, SecretariaOficialCumprimentoMaterializacaoRequest request) {
        visibilityService.requireDeskAccess(deskWorkItemId);
        return projectionSupport.action("secretaria.operacional", "materializarAtoOficialCumprimento", null,
                officialClosureRoutingService.materializarAto(deskWorkItemId, request));
    }



    public SurfaceActionResponse instaurarProcedimentoCorregedoriaEleitoral(Long processoId, SecretariaCorregedoriaEleitoralRequest request) {
        visibilityService.requireProcessAccess(processoId);
        return projectionSupport.action("secretaria.operacional.eleitoral", "instaurarProcedimentoCorregedoriaEleitoral", processoId,
                specializedOperationalExecutionService.instaurarProcedimentoCorregedoria(processoId, request));
    }

    public SurfaceActionResponse registrarInspecaoCorregedoriaEleitoral(Long processoId, SecretariaInspecaoCorregedoriaRequest request) {
        visibilityService.requireProcessAccess(processoId);
        return projectionSupport.action("secretaria.operacional.eleitoral", "registrarInspecaoCorregedoriaEleitoral", processoId,
                specializedOperationalExecutionService.registrarInspecaoCorregedoria(processoId, request));
    }

    public SurfaceActionResponse validarPesquisaEleitoral(Long processoId, SecretariaPesquisaEleitoralRequest request) {
        visibilityService.requireProcessAccess(processoId);
        return projectionSupport.action("secretaria.operacional.eleitoral", "validarPesquisaEleitoral", processoId,
                specializedOperationalExecutionService.validarPesquisaEleitoral(processoId, request));
    }

    public SurfaceActionResponse receberMidiaProcessualTrabalhista(Long processoId, SecretariaMidiaProcessualRequest request) {
        visibilityService.requireProcessAccess(processoId);
        return projectionSupport.action("secretaria.operacional.trabalhista", "receberMidiaProcessualTrabalhista", processoId,
                specializedOperationalExecutionService.receberMidiaProcessual(processoId, request));
    }

    public SurfaceActionResponse disponibilizarMidiaProcessualTrabalhista(Long processoId, SecretariaMidiaProcessualRequest request) {
        visibilityService.requireProcessAccess(processoId);
        return projectionSupport.action("secretaria.operacional.trabalhista", "disponibilizarMidiaProcessualTrabalhista", processoId,
                specializedOperationalExecutionService.disponibilizarMidiaProcessual(processoId, request));
    }

    public SurfaceActionResponse impulsionarExecucaoTrabalhista(Long processoId, SecretariaExecucaoTrabalhistaRequest request) {
        visibilityService.requireProcessAccess(processoId);
        return projectionSupport.action("secretaria.operacional.trabalhista", "impulsionarExecucaoTrabalhista", processoId,
                specializedOperationalExecutionService.impulsionarExecucaoTrabalhista(processoId, request));
    }

    public SurfaceActionResponse receberPlantaoMilitar(Long processoId, SecretariaPlantaoMilitarRequest request) {
        visibilityService.requireProcessAccess(processoId);
        return projectionSupport.action("secretaria.operacional.militar", "receberPlantaoMilitar", processoId,
                specializedOperationalExecutionService.receberUrgenciaPlantao(processoId, request));
    }

    public SurfaceActionResponse registrarBalcaoVirtualMilitar(Long processoId, SecretariaBalcaoVirtualMilitarRequest request) {
        visibilityService.requireProcessAccess(processoId);
        return projectionSupport.action("secretaria.operacional.militar", "registrarBalcaoVirtualMilitar", processoId,
                specializedOperationalExecutionService.registrarAtendimentoBalcaoVirtual(processoId, request));
    }

    public SurfaceSnapshotResponse gavetasOficialCumprimento(String inboxKey, int limit) {
        visibilityService.requireInboxAccess(inboxKey);
        return projectionSupport.snapshot("secretaria.operacional.gavetas", officialActsDrawerService.drawers(inboxKey, limit));
    }

    public SurfaceSnapshotResponse detalheGavetaOficialCumprimento(String inboxKey, String drawerKey, int limit) {
        visibilityService.requireInboxAccess(inboxKey);
        return projectionSupport.snapshot("secretaria.operacional.gavetas", officialActsDrawerService.drawerDetail(inboxKey, drawerKey, limit));
    }

    public SurfaceActionResponse incluirEmPautaColegiada(Long processoId, SecretariaPautaColegiadaRequest request) {
        visibilityService.requireProcessAccess(processoId);
        return projectionSupport.action("secretaria.operacional.colegiado", "incluirEmPautaColegiada", processoId,
                collegiateOperationalExecutionService.incluirEmPauta(processoId, request));
    }

    public SurfaceActionResponse publicarPautaColegiada(Long julgamentoId, SecretariaPublicacaoPautaRequest request) {
        Long processoId = collegiateOperationalExecutionService.processoIdPorJulgamento(julgamentoId);
        visibilityService.requireProcessAccess(processoId);
        return projectionSupport.action("secretaria.operacional.colegiado", "publicarPautaColegiada", processoId,
                collegiateOperationalExecutionService.publicarPauta(julgamentoId, request));
    }

    public SurfaceActionResponse registrarSustentacaoOralColegiada(Long julgamentoId, SecretariaSustentacaoOralRequest request) {
        Long processoId = collegiateOperationalExecutionService.processoIdPorJulgamento(julgamentoId);
        visibilityService.requireProcessAccess(processoId);
        return projectionSupport.action("secretaria.operacional.colegiado", "registrarSustentacaoOralColegiada", processoId,
                collegiateOperationalExecutionService.registrarSustentacaoOral(julgamentoId, request));
    }

    public SurfaceActionResponse publicarAcordaoColegiado(Long julgamentoId, SecretariaPublicacaoAcordaoRequest request) {
        Long processoId = collegiateOperationalExecutionService.processoIdPorJulgamento(julgamentoId);
        visibilityService.requireProcessAccess(processoId);
        return projectionSupport.action("secretaria.operacional.colegiado", "publicarAcordaoColegiado", processoId,
                collegiateOperationalExecutionService.publicarAcordao(julgamentoId, request));
    }

    public SurfaceActionResponse baixarOrigemColegiada(Long julgamentoId, SecretariaBaixaOrigemRequest request) {
        Long processoId = collegiateOperationalExecutionService.processoIdPorJulgamento(julgamentoId);
        visibilityService.requireProcessAccess(processoId);
        return projectionSupport.action("secretaria.operacional.colegiado", "baixarOrigemColegiada", processoId,
                collegiateOperationalExecutionService.baixarOrigem(julgamentoId, request));
    }

    public SurfaceSnapshotResponse consultarPorRamo(String ramoDireito) {
        return projectionSupport.snapshot("secretaria.especializada", routingService.consultarPorRamo(ramoDireito));
    }

    public SurfaceSnapshotResponse diagnosticar(Long processoId) {
        visibilityService.requireProcessAccess(processoId);
        return projectionSupport.snapshot("secretaria.especializada", routingService.diagnosticarProcesso(processoId));
    }

    public SurfaceActionResponse enfileirar(Long processoId) {
        visibilityService.requireProcessAccess(processoId);
        return projectionSupport.action("secretaria.especializada", "enfileirar", processoId,
                routingService.enfileirarProcesso(processoId));
    }


    public SurfaceSnapshotResponse matriz(Long processoId) {
        visibilityService.requireProcessAccess(processoId);
        return projectionSupport.snapshot("secretaria.matriz", judicialTopologyCoordinationMatrixService.snapshot(processoId));
    }

    public SurfaceSnapshotResponse handoff(Long processoId) {
        visibilityService.requireProcessAccess(processoId);
        return projectionSupport.snapshot("secretaria.handoff", handoffService.snapshot(processoId));
    }

    public SurfaceSnapshotResponse topologia(Long processoId) {
        visibilityService.requireProcessAccess(processoId);
        return projectionSupport.snapshot("secretaria.topologia", orchestrationService.topologia(processoId));
    }

    public SurfaceSnapshotResponse malha(Long processoId) {
        visibilityService.requireProcessAccess(processoId);
        return projectionSupport.snapshot("secretaria.malha", judicialTopologySegregationMeshService.snapshot(processoId));
    }

    public SurfaceActionResponse receber(Long processoId, String origem, Boolean audienciaSensivel) {
        visibilityService.requireProcessAccess(processoId);
        return projectionSupport.action("secretaria.topologia", "receber", processoId,
                orchestrationService.receive(processoId, origem, audienciaSensivel));
    }

    public SurfaceSnapshotResponse radar(Long processoId) {
        visibilityService.requireProcessAccess(processoId);
        return projectionSupport.snapshot("secretaria.topologia", orchestrationService.radar(processoId));
    }


    public SurfaceSnapshotResponse avaliarEstabilidade(Long processoId) {
        visibilityService.requireProcessAccess(processoId);
        return projectionSupport.snapshot("secretaria.estabilidade", orchestrationService.avaliarEstabilidade(processoId));
    }

    public SurfaceActionResponse estabilizarSecretaria(Long processoId) {
        visibilityService.requireProcessAccess(processoId);
        return projectionSupport.action("secretaria.estabilidade", "estabilizar", processoId,
                orchestrationService.estabilizarSecretaria(processoId));
    }

    public SurfaceSnapshotResponse avaliarPauta(Long processoId, java.time.LocalDateTime inicio, Integer duracaoMinutos, String tipo, String local) {
        visibilityService.requireProcessAccess(processoId);
        return projectionSupport.snapshot("secretaria.pauta", orchestrationService.avaliarPauta(processoId, inicio, duracaoMinutos, tipo, local));
    }

    public SurfaceActionResponse registrarPauta(Long processoId, java.time.LocalDateTime inicio, Integer duracaoMinutos, String tipo, String local) {
        visibilityService.requireProcessAccess(processoId);
        return projectionSupport.action("secretaria.pauta", "registrar", processoId,
                orchestrationService.registrarPauta(processoId, inicio, duracaoMinutos, tipo, local));
    }


    public SurfaceSnapshotResponse checklist(Long processoId) {
        visibilityService.requireProcessAccess(processoId);
        return projectionSupport.snapshot("secretaria.checklist", orchestrationService.checklist(processoId));
    }

    public SurfaceSnapshotResponse avaliarDistribuicaoInterna(Long processoId, String stage) {
        visibilityService.requireProcessAccess(processoId);
        return projectionSupport.snapshot("secretaria.distribuicao-interna", orchestrationService.avaliarDistribuicaoInterna(processoId, stage));
    }

    public SurfaceActionResponse distribuirInternamente(Long processoId, String stage) {
        visibilityService.requireProcessAccess(processoId);
        return projectionSupport.action("secretaria.distribuicao-interna", "atribuir", processoId,
                orchestrationService.distribuirInternamente(processoId, stage));
    }

    public SurfaceSnapshotResponse avaliarRecursosAudiencia(Long processoId, java.time.LocalDateTime inicio, Integer duracaoMinutos, String tipo, String local) {
        visibilityService.requireProcessAccess(processoId);
        return projectionSupport.snapshot("secretaria.salas", orchestrationService.avaliarRecursosAudiencia(processoId, inicio, duracaoMinutos, tipo, local));
    }

    public SurfaceActionResponse reservarRecursosAudiencia(Long processoId, java.time.LocalDateTime inicio, Integer duracaoMinutos, String tipo, String local) {
        visibilityService.requireProcessAccess(processoId);
        return projectionSupport.action("secretaria.salas", "reservar", processoId,
                orchestrationService.reservarRecursosAudiencia(processoId, inicio, duracaoMinutos, tipo, local));
    }


    public SurfaceSnapshotResponse avaliarPresencaAudiencia(Long processoId, java.time.LocalDateTime inicio, Integer duracaoMinutos, String tipo, String local) {
        visibilityService.requireProcessAccess(processoId);
        return projectionSupport.snapshot("secretaria.presenca", orchestrationService.avaliarPresencaAudiencia(processoId, inicio, duracaoMinutos, tipo, local));
    }

    public SurfaceActionResponse registrarPresencaAudiencia(Long processoId, java.time.LocalDateTime inicio, Integer duracaoMinutos, String tipo, String local, String papel, String nome, String situacao) {
        visibilityService.requireProcessAccess(processoId);
        return projectionSupport.action("secretaria.presenca", "registrar", processoId,
                orchestrationService.registrarPresencaAudiencia(processoId, inicio, duracaoMinutos, tipo, local, papel, nome, situacao));
    }

    public SurfaceSnapshotResponse avaliarExpedicaoLote(Long processoId, String lote) {
        visibilityService.requireProcessAccess(processoId);
        return projectionSupport.snapshot("secretaria.expedicao", orchestrationService.avaliarExpedicaoLote(processoId, lote));
    }

    public SurfaceActionResponse materializarExpedicaoLote(Long processoId, String lote) {
        visibilityService.requireProcessAccess(processoId);
        return projectionSupport.action("secretaria.expedicao", "materializar", processoId,
                orchestrationService.materializarExpedicaoLote(processoId, lote));
    }

    public SurfaceSnapshotResponse avaliarRedistribuicao(Long processoId, String stage) {
        visibilityService.requireProcessAccess(processoId);
        return projectionSupport.snapshot("secretaria.redistribuicao", orchestrationService.avaliarRedistribuicao(processoId, stage));
    }

    public SurfaceActionResponse redistribuir(Long processoId, String stage) {
        visibilityService.requireProcessAccess(processoId);
        return projectionSupport.action("secretaria.redistribuicao", "executar", processoId,
                orchestrationService.redistribuir(processoId, stage));
    }

    public SurfaceSnapshotResponse avaliarGargalos(Long processoId) {
        visibilityService.requireProcessAccess(processoId);
        return projectionSupport.snapshot("secretaria.gargalos", orchestrationService.avaliarGargalos(processoId));
    }

    public SurfaceSnapshotResponse planejarAtos(Long processoId) {
        visibilityService.requireProcessAccess(processoId);
        return projectionSupport.snapshot("secretaria.atos", orchestrationService.planejarAtos(processoId));
    }

    public SurfaceSnapshotResponse avaliarSla(Long processoId, String stage) {
        visibilityService.requireProcessAccess(processoId);
        return projectionSupport.snapshot("secretaria.sla", orchestrationService.avaliarSla(processoId, stage));
    }

    public SurfaceActionResponse escalarSla(Long processoId, String stage) {
        visibilityService.requireProcessAccess(processoId);
        return projectionSupport.action("secretaria.sla", "escalar", processoId,
                orchestrationService.escalarSla(processoId, stage));
    }

    public List<SecretariatJuntadaItemResponse> listarJuntadas(Long processoId, Integer limit) {
        visibilityService.requireProcessAccess(processoId);
        return minutaService.listarJuntadas(processoId, limit).stream()
                .map(item -> new SecretariatJuntadaItemResponse(item.seq(), item.createdAt(), item.eventType(), item.label(), item.docCount(), item.eventoId()))
                .toList();
    }
}
