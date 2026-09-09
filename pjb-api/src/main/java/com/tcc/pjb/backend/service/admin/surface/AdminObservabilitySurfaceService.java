package com.tcc.pjb.backend.service.admin.surface;

import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
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
import com.tcc.pjb.backend.service.surface.common.SurfaceProjectionSupport;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class AdminObservabilitySurfaceService {

    private final NationalObservabilityService observabilityService;
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

    public AdminObservabilitySurfaceService(NationalObservabilityService observabilityService,
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
        this.observabilityService = Objects.requireNonNull(observabilityService);
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
}
