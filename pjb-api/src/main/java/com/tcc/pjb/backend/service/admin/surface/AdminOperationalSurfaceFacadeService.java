package com.tcc.pjb.backend.service.admin.surface;

import com.tcc.pjb.backend.model.dto.profile.operational.AdminEmergenciaRequest;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceActionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.admin.AdministradorNacionalGovernanceService;
import com.tcc.pjb.backend.service.observabilidade.NationalObservabilityService;
import com.tcc.pjb.backend.service.processual.substituicao.federativa.centrocomando.PjbSubstituicaoFederativaCentroComandoFacadeService;
import com.tcc.pjb.backend.service.processual.substituicao.federativa.cutover.PjbSubstituicaoFederativaCutoverMatrixFacadeService;
import com.tcc.pjb.backend.service.processual.substituicao.federativa.malhajulgadora.PjbSubstituicaoFederativaMalhaJulgadoraFacadeService;
import com.tcc.pjb.backend.service.processual.substituicao.federativa.nucleoduro.PjbSubstituicaoFederativaNucleoDuroFacadeService;
import com.tcc.pjb.backend.service.processual.substituicao.federativa.precedentes.PjbSubstituicaoFederativaPrecedentesQualificadosFacadeService;
import com.tcc.pjb.backend.service.processual.substituicao.federativa.tutelacoletiva.PjbSubstituicaoFederativaTutelaColetivaFacadeService;
import com.tcc.pjb.backend.service.processual.substituicao.federativa.poscoletiva.PjbSubstituicaoFederativaPosColetivaFacadeService;
import com.tcc.pjb.backend.service.processual.substituicao.federativa.warroom.PjbSubstituicaoFederativaWarRoomFacadeService;
import com.tcc.pjb.backend.service.processual.substituicao.nacional.programa.PjbSubstituicaoNacionalProgramaFacadeService;
import com.tcc.pjb.backend.service.processual.sustentacao.PjbPlataformaSustentacaoFacadeService;
import com.tcc.pjb.backend.service.rito.RitoResolutionService;
import com.tcc.pjb.backend.service.surface.common.SurfaceProjectionSupport;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class AdminOperationalSurfaceFacadeService {

    private final ProcessoRepository processoRepository;
    private final RitoResolutionService ritoResolutionService;
    private final NationalObservabilityService observabilityService;
    private final AdministradorNacionalGovernanceService governanceService;
    private final PjbSubstituicaoNacionalProgramaFacadeService substituicaoNacionalProgramaFacadeService;
    private final PjbSubstituicaoFederativaCentroComandoFacadeService substituicaoFederativaCentroComandoFacadeService;
    private final PjbSubstituicaoFederativaWarRoomFacadeService substituicaoFederativaWarRoomFacadeService;
    private final PjbSubstituicaoFederativaCutoverMatrixFacadeService substituicaoFederativaCutoverMatrixFacadeService;
    private final PjbSubstituicaoFederativaNucleoDuroFacadeService substituicaoFederativaNucleoDuroFacadeService;
    private final PjbSubstituicaoFederativaMalhaJulgadoraFacadeService substituicaoFederativaMalhaJulgadoraFacadeService;
    private final PjbSubstituicaoFederativaPrecedentesQualificadosFacadeService substituicaoFederativaPrecedentesQualificadosFacadeService;
    private final PjbSubstituicaoFederativaTutelaColetivaFacadeService substituicaoFederativaTutelaColetivaFacadeService;
    private final PjbSubstituicaoFederativaPosColetivaFacadeService substituicaoFederativaPosColetivaFacadeService;
    private final PjbPlataformaSustentacaoFacadeService plataformaSustentacaoFacadeService;
    private final SurfaceProjectionSupport projectionSupport;

    public AdminOperationalSurfaceFacadeService(ProcessoRepository processoRepository,
                                                RitoResolutionService ritoResolutionService,
                                                NationalObservabilityService observabilityService,
                                                AdministradorNacionalGovernanceService governanceService,
                                                PjbSubstituicaoNacionalProgramaFacadeService substituicaoNacionalProgramaFacadeService,
                                                PjbSubstituicaoFederativaCentroComandoFacadeService substituicaoFederativaCentroComandoFacadeService,
                                                PjbSubstituicaoFederativaWarRoomFacadeService substituicaoFederativaWarRoomFacadeService,
                                                PjbSubstituicaoFederativaCutoverMatrixFacadeService substituicaoFederativaCutoverMatrixFacadeService,
                                                PjbSubstituicaoFederativaNucleoDuroFacadeService substituicaoFederativaNucleoDuroFacadeService,
                                                PjbSubstituicaoFederativaMalhaJulgadoraFacadeService substituicaoFederativaMalhaJulgadoraFacadeService,
                                                PjbSubstituicaoFederativaPrecedentesQualificadosFacadeService substituicaoFederativaPrecedentesQualificadosFacadeService,
                                                PjbSubstituicaoFederativaTutelaColetivaFacadeService substituicaoFederativaTutelaColetivaFacadeService,
                                                PjbSubstituicaoFederativaPosColetivaFacadeService substituicaoFederativaPosColetivaFacadeService,
                                                PjbPlataformaSustentacaoFacadeService plataformaSustentacaoFacadeService,
                                                SurfaceProjectionSupport projectionSupport) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.ritoResolutionService = Objects.requireNonNull(ritoResolutionService);
        this.observabilityService = Objects.requireNonNull(observabilityService);
        this.governanceService = Objects.requireNonNull(governanceService);
        this.substituicaoNacionalProgramaFacadeService = Objects.requireNonNull(substituicaoNacionalProgramaFacadeService);
        this.substituicaoFederativaCentroComandoFacadeService = Objects.requireNonNull(substituicaoFederativaCentroComandoFacadeService);
        this.substituicaoFederativaWarRoomFacadeService = Objects.requireNonNull(substituicaoFederativaWarRoomFacadeService);
        this.substituicaoFederativaCutoverMatrixFacadeService = Objects.requireNonNull(substituicaoFederativaCutoverMatrixFacadeService);
        this.substituicaoFederativaNucleoDuroFacadeService = Objects.requireNonNull(substituicaoFederativaNucleoDuroFacadeService);
        this.substituicaoFederativaMalhaJulgadoraFacadeService = Objects.requireNonNull(substituicaoFederativaMalhaJulgadoraFacadeService);
        this.substituicaoFederativaPrecedentesQualificadosFacadeService = Objects.requireNonNull(substituicaoFederativaPrecedentesQualificadosFacadeService);
        this.substituicaoFederativaTutelaColetivaFacadeService = Objects.requireNonNull(substituicaoFederativaTutelaColetivaFacadeService);
        this.substituicaoFederativaPosColetivaFacadeService = Objects.requireNonNull(substituicaoFederativaPosColetivaFacadeService);
        this.plataformaSustentacaoFacadeService = Objects.requireNonNull(plataformaSustentacaoFacadeService);
        this.projectionSupport = Objects.requireNonNull(projectionSupport);
    }

    public Optional<SurfaceSnapshotResponse> ritoDiagnostico(Long processoId) {
        Processo processo = processoRepository.findById(processoId).orElse(null);
        if (processo == null) {
            return Optional.empty();
        }
        var detail = ritoResolutionService.resolveDetailed(processo, null);
        LinkedHashMap<String, Object> body = new LinkedHashMap<>();
        body.put("processoId", processo.getId());
        body.put("numero", processo.getNumeroUnificado());
        body.put("materia", processo.getMateria() != null ? processo.getMateria().name() : null);
        body.put("classeProcessual", processo.getClasseProcessual());
        body.put("assunto", processo.getAssunto());
        body.put("ritoDb", processo.getRito() != null ? processo.getRito().name() : null);
        body.put("ritoResolved", detail.resolution().rito() != null ? detail.resolution().rito().name() : null);
        body.put("ritoTitle", detail.resolution().ritoTitle());
        body.put("ramoSugerido", detail.resolution().ramoSugerido());
        body.put("confidence", detail.resolution().confidence());
        body.put("reasons", detail.resolution().reasons());
        body.put("status", detail.status());
        body.put("blocking", detail.blocking());
        body.put("canonicalContext", detail.canonicalContext() != null ? detail.canonicalContext().toMap() : Map.of());
        body.put("metadata", detail.metadata());
        return Optional.of(projectionSupport.snapshot("admin.ritos.diagnostico", body));
    }

    public SurfaceSnapshotResponse observabilityDashboard() {
        return projectionSupport.snapshot("admin.observability.dashboard", observabilityService.nationalDashboard());
    }

    public SurfaceSnapshotResponse observabilitySlaReport() {
        return projectionSupport.snapshot("admin.observability.sla-report", observabilityService.slaReport());
    }

    public SurfaceSnapshotResponse observabilityRunbookStatus() {
        return projectionSupport.snapshot("admin.observability.runbook-status", observabilityService.runbookStatus());
    }

    public SurfaceSnapshotResponse observabilitySubstituicaoReadiness() {
        return projectionSupport.snapshot("admin.observability.substituicao-readiness", substituicaoNacionalProgramaFacadeService.avaliar());
    }

    public SurfaceSnapshotResponse observabilitySubstituicaoCentroComando() {
        return projectionSupport.snapshot("admin.observability.substituicao-centro-comando", substituicaoFederativaCentroComandoFacadeService.avaliar());
    }

    public SurfaceSnapshotResponse observabilitySubstituicaoWarRoom() {
        return projectionSupport.snapshot("admin.observability.substituicao-war-room", substituicaoFederativaWarRoomFacadeService.avaliar());
    }

    public SurfaceSnapshotResponse observabilitySubstituicaoWarRoomTribunal(String tribunalCodigo) {
        return projectionSupport.snapshot("admin.observability.substituicao-war-room-tribunal", substituicaoFederativaWarRoomFacadeService.avaliarTribunal(tribunalCodigo));
    }

    public SurfaceSnapshotResponse observabilitySubstituicaoCutoverMatrix() {
        return projectionSupport.snapshot("admin.observability.substituicao-cutover-matrix", substituicaoFederativaCutoverMatrixFacadeService.avaliar());
    }

    public SurfaceSnapshotResponse observabilitySubstituicaoCutoverMatrixTribunal(String tribunalCodigo) {
        return projectionSupport.snapshot("admin.observability.substituicao-cutover-matrix-tribunal", substituicaoFederativaCutoverMatrixFacadeService.avaliarTribunal(tribunalCodigo));
    }

    public SurfaceSnapshotResponse observabilitySubstituicaoNucleoDuro() {
        return projectionSupport.snapshot("admin.observability.substituicao-nucleo-duro", substituicaoFederativaNucleoDuroFacadeService.avaliar());
    }

    public SurfaceSnapshotResponse observabilitySubstituicaoNucleoDuroTribunal(String tribunalCodigo) {
        return projectionSupport.snapshot("admin.observability.substituicao-nucleo-duro-tribunal", substituicaoFederativaNucleoDuroFacadeService.avaliarTribunal(tribunalCodigo));
    }

    public SurfaceSnapshotResponse observabilitySubstituicaoMalhaJulgadora() {
        return projectionSupport.snapshot("admin.observability.substituicao-malha-julgadora", substituicaoFederativaMalhaJulgadoraFacadeService.avaliar());
    }

    public SurfaceSnapshotResponse observabilitySubstituicaoMalhaJulgadoraTribunal(String tribunalCodigo) {
        return projectionSupport.snapshot("admin.observability.substituicao-malha-julgadora-tribunal", substituicaoFederativaMalhaJulgadoraFacadeService.avaliarTribunal(tribunalCodigo));
    }

    public SurfaceSnapshotResponse observabilitySubstituicaoPrecedentesQualificados() {
        return projectionSupport.snapshot("admin.observability.substituicao-precedentes-qualificados", substituicaoFederativaPrecedentesQualificadosFacadeService.avaliar());
    }

    public SurfaceSnapshotResponse observabilitySubstituicaoPrecedentesQualificadosTribunal(String tribunalCodigo) {
        return projectionSupport.snapshot("admin.observability.substituicao-precedentes-qualificados-tribunal", substituicaoFederativaPrecedentesQualificadosFacadeService.avaliarTribunal(tribunalCodigo));
    }

    public SurfaceSnapshotResponse observabilitySubstituicaoTutelaColetiva() {
        return projectionSupport.snapshot("admin.observability.substituicao-tutela-coletiva", substituicaoFederativaTutelaColetivaFacadeService.avaliar());
    }

    public SurfaceSnapshotResponse observabilitySubstituicaoTutelaColetivaTribunal(String tribunalCodigo) {
        return projectionSupport.snapshot("admin.observability.substituicao-tutela-coletiva-tribunal", substituicaoFederativaTutelaColetivaFacadeService.avaliarTribunal(tribunalCodigo));
    }

    public SurfaceSnapshotResponse observabilitySubstituicaoPosColetiva() {
        return projectionSupport.snapshot("admin.observability.substituicao-pos-coletiva", substituicaoFederativaPosColetivaFacadeService.avaliar());
    }

    public SurfaceSnapshotResponse observabilitySubstituicaoPosColetivaTribunal(String tribunalCodigo) {
        return projectionSupport.snapshot("admin.observability.substituicao-pos-coletiva-tribunal", substituicaoFederativaPosColetivaFacadeService.avaliarTribunal(tribunalCodigo));
    }


    public SurfaceSnapshotResponse observabilityPlataformaSustentacao() {
        return projectionSupport.snapshot("admin.observability.plataforma-sustentacao", plataformaSustentacaoFacadeService.avaliar());
    }

    public SurfaceSnapshotResponse governanceSnapshot() {
        return projectionSupport.snapshot("admin.governance.snapshot", governanceService.bootstrapGovernance());
    }

    public SurfaceSnapshotResponse governanceMetricasTribunal(String uf) {
        return projectionSupport.snapshot("admin.governance.tribunal", governanceService.metricasPorTribunal(uf));
    }

    public SurfaceSnapshotResponse governanceMetricasComarca(String uf, String comarca) {
        return projectionSupport.snapshot("admin.governance.comarca", governanceService.metricasPorComarca(uf, comarca));
    }

    public SurfaceActionResponse governanceExecutarReconciliacaoGlobal() {
        return projectionSupport.action("admin.governance", "reconciliacao-global", null, governanceService.executarReconciliacaoGlobal());
    }

    public SurfaceActionResponse governanceAtivarModoEmergencia(AdminEmergenciaRequest request) {
        return projectionSupport.action("admin.governance", "modo-emergencia", null, governanceService.ativarModoEmergencia(request.motivo(), request.responsavel()));
    }

    public SurfaceSnapshotResponse governanceMalhaJulgadoraSubstituicao() {
        return projectionSupport.snapshot("admin.governance.substituicao-malha-julgadora", substituicaoFederativaMalhaJulgadoraFacadeService.avaliar());
    }

    public SurfaceSnapshotResponse governanceMalhaJulgadoraTribunal(String tribunalCodigo) {
        return projectionSupport.snapshot("admin.governance.substituicao-malha-julgadora-tribunal", substituicaoFederativaMalhaJulgadoraFacadeService.avaliarTribunal(tribunalCodigo));
    }

    public SurfaceSnapshotResponse governancePrecedentesQualificadosSubstituicao() {
        return projectionSupport.snapshot("admin.governance.substituicao-precedentes-qualificados", substituicaoFederativaPrecedentesQualificadosFacadeService.avaliar());
    }

    public SurfaceSnapshotResponse governancePrecedentesQualificadosTribunal(String tribunalCodigo) {
        return projectionSupport.snapshot("admin.governance.substituicao-precedentes-qualificados-tribunal", substituicaoFederativaPrecedentesQualificadosFacadeService.avaliarTribunal(tribunalCodigo));
    }

    public SurfaceSnapshotResponse governanceTutelaColetivaSubstituicao() {
        return projectionSupport.snapshot("admin.governance.substituicao-tutela-coletiva", substituicaoFederativaTutelaColetivaFacadeService.avaliar());
    }

    public SurfaceSnapshotResponse governanceTutelaColetivaTribunal(String tribunalCodigo) {
        return projectionSupport.snapshot("admin.governance.substituicao-tutela-coletiva-tribunal", substituicaoFederativaTutelaColetivaFacadeService.avaliarTribunal(tribunalCodigo));
    }

    public SurfaceSnapshotResponse governancePosColetivaSubstituicao() {
        return projectionSupport.snapshot("admin.governance.substituicao-pos-coletiva", substituicaoFederativaPosColetivaFacadeService.avaliar());
    }

    public SurfaceSnapshotResponse governancePosColetivaTribunal(String tribunalCodigo) {
        return projectionSupport.snapshot("admin.governance.substituicao-pos-coletiva-tribunal", substituicaoFederativaPosColetivaFacadeService.avaliarTribunal(tribunalCodigo));
    }


    public SurfaceSnapshotResponse governancePlataformaSustentacao() {
        return projectionSupport.snapshot("admin.governance.plataforma-sustentacao", plataformaSustentacaoFacadeService.avaliar());
    }

    public SurfaceSnapshotResponse governanceHealthCheck() {
        return projectionSupport.snapshot("admin.governance.health-check", governanceService.healthCheckNacional());
    }

    public SurfaceSnapshotResponse governanceProgramaSubstituicao() {
        return projectionSupport.snapshot("admin.governance.substituicao-programa", substituicaoNacionalProgramaFacadeService.avaliar());
    }

    public SurfaceSnapshotResponse governanceCentroComandoSubstituicao() {
        return projectionSupport.snapshot("admin.governance.substituicao-centro-comando", substituicaoFederativaCentroComandoFacadeService.avaliar());
    }

    public SurfaceSnapshotResponse governanceCentroComandoTribunal(String tribunalCodigo) {
        return projectionSupport.snapshot("admin.governance.substituicao-centro-comando-tribunal", substituicaoFederativaCentroComandoFacadeService.avaliarTribunal(tribunalCodigo));
    }

    public SurfaceSnapshotResponse governanceWarRoomSubstituicao() {
        return projectionSupport.snapshot("admin.governance.substituicao-war-room", substituicaoFederativaWarRoomFacadeService.avaliar());
    }

    public SurfaceSnapshotResponse governanceWarRoomTribunal(String tribunalCodigo) {
        return projectionSupport.snapshot("admin.governance.substituicao-war-room-tribunal", substituicaoFederativaWarRoomFacadeService.avaliarTribunal(tribunalCodigo));
    }

    public SurfaceSnapshotResponse governanceCutoverMatrixSubstituicao() {
        return projectionSupport.snapshot("admin.governance.substituicao-cutover-matrix", substituicaoFederativaCutoverMatrixFacadeService.avaliar());
    }

    public SurfaceSnapshotResponse governanceCutoverMatrixTribunal(String tribunalCodigo) {
        return projectionSupport.snapshot("admin.governance.substituicao-cutover-matrix-tribunal", substituicaoFederativaCutoverMatrixFacadeService.avaliarTribunal(tribunalCodigo));
    }

    public SurfaceSnapshotResponse governanceNucleoDuroSubstituicao() {
        return projectionSupport.snapshot("admin.governance.substituicao-nucleo-duro", substituicaoFederativaNucleoDuroFacadeService.avaliar());
    }

    public SurfaceSnapshotResponse governanceNucleoDuroTribunal(String tribunalCodigo) {
        return projectionSupport.snapshot("admin.governance.substituicao-nucleo-duro-tribunal", substituicaoFederativaNucleoDuroFacadeService.avaliarTribunal(tribunalCodigo));
    }
}
