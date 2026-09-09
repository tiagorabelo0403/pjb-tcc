package com.tcc.pjb.backend.service.admin.surface;

import com.tcc.pjb.backend.model.dto.profile.operational.AdminEmergenciaRequest;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceActionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.service.admin.AdministradorNacionalGovernanceService;
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
import com.tcc.pjb.backend.service.surface.common.SurfaceProjectionSupport;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class AdminGovernanceSurfaceService {

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

    public AdminGovernanceSurfaceService(AdministradorNacionalGovernanceService governanceService,
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
