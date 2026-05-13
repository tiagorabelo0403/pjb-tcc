package com.tcc.pjb.backend.controller.processual.substituicao.arquitetura;

import com.tcc.pjb.backend.model.dto.api.ApiQueryResponse;
import com.tcc.pjb.backend.controller.processual.substituicao.routes.PjbSubstituicaoNacionalRoutes;
import com.tcc.pjb.backend.model.dto.processual.substituicao.arquitetura.PjbArquiteturaSubstituicaoNacionalResponse;
import com.tcc.pjb.backend.model.dto.processual.substituicao.federativa.centrocomando.PjbSubstituicaoFederativaCentroComandoResponse;
import com.tcc.pjb.backend.model.dto.processual.substituicao.federativa.cutover.PjbSubstituicaoFederativaCutoverMatrixResponse;
import com.tcc.pjb.backend.model.dto.processual.substituicao.federativa.cutover.PjbSubstituicaoFederativaCutoverTribunalResponse;
import com.tcc.pjb.backend.model.dto.processual.substituicao.federativa.malhajulgadora.PjbSubstituicaoFederativaMalhaJulgadoraResponse;
import com.tcc.pjb.backend.model.dto.processual.substituicao.federativa.malhajulgadora.PjbSubstituicaoFederativaMalhaJulgadoraTribunalResponse;
import com.tcc.pjb.backend.model.dto.processual.substituicao.federativa.nucleoduro.PjbSubstituicaoFederativaNucleoDuroResponse;
import com.tcc.pjb.backend.model.dto.processual.substituicao.federativa.precedentes.PjbSubstituicaoFederativaPrecedentesQualificadosResponse;
import com.tcc.pjb.backend.model.dto.processual.substituicao.federativa.precedentes.PjbSubstituicaoFederativaPrecedentesQualificadosTribunalResponse;
import com.tcc.pjb.backend.model.dto.processual.substituicao.federativa.tutelacoletiva.PjbSubstituicaoFederativaTutelaColetivaResponse;
import com.tcc.pjb.backend.model.dto.processual.substituicao.federativa.tutelacoletiva.PjbSubstituicaoFederativaTutelaColetivaTribunalResponse;
import com.tcc.pjb.backend.model.dto.processual.substituicao.federativa.poscoletiva.PjbSubstituicaoFederativaPosColetivaResponse;
import com.tcc.pjb.backend.model.dto.processual.substituicao.federativa.poscoletiva.PjbSubstituicaoFederativaPosColetivaTribunalResponse;
import com.tcc.pjb.backend.model.dto.processual.substituicao.federativa.nucleoduro.PjbSubstituicaoFederativaNucleoDuroTribunalResponse;
import com.tcc.pjb.backend.model.dto.processual.substituicao.federativa.common.PjbSubstituicaoFederativaTribunalResponse;
import com.tcc.pjb.backend.model.dto.processual.substituicao.federativa.warroom.PjbSubstituicaoFederativaWarRoomResponse;
import com.tcc.pjb.backend.model.dto.processual.substituicao.federativa.warroom.PjbSubstituicaoFederativaWarRoomTribunalResponse;
import com.tcc.pjb.backend.model.dto.processual.substituicao.nacional.programa.PjbSubstituicaoNacionalProgramaResponse;
import com.tcc.pjb.backend.model.dto.processual.sustentacao.PjbPlataformaSustentacaoResponse;
import com.tcc.pjb.backend.service.api.ApiResponseFactory;
import com.tcc.pjb.backend.service.processual.substituicao.arquitetura.PjbArquiteturaSubstituicaoNacionalFacadeService;
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
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(PjbSubstituicaoNacionalRoutes.CANONICAL_BASE)
public class PjbArquiteturaSubstituicaoNacionalController {

    private final PjbArquiteturaSubstituicaoNacionalFacadeService arquiteturaFacadeService;
    private final PjbSubstituicaoNacionalProgramaFacadeService programaFacadeService;
    private final PjbSubstituicaoFederativaCentroComandoFacadeService federativoCentroComandoFacadeService;
    private final PjbSubstituicaoFederativaWarRoomFacadeService federativoWarRoomFacadeService;
    private final PjbSubstituicaoFederativaCutoverMatrixFacadeService federativoCutoverMatrixFacadeService;
    private final PjbSubstituicaoFederativaNucleoDuroFacadeService federativoNucleoDuroFacadeService;
    private final PjbSubstituicaoFederativaMalhaJulgadoraFacadeService federativoMalhaJulgadoraFacadeService;
    private final PjbSubstituicaoFederativaPrecedentesQualificadosFacadeService federativoPrecedentesQualificadosFacadeService;
    private final PjbSubstituicaoFederativaTutelaColetivaFacadeService federativoTutelaColetivaFacadeService;
    private final PjbSubstituicaoFederativaPosColetivaFacadeService federativoPosColetivaFacadeService;
    private final PjbPlataformaSustentacaoFacadeService plataformaSustentacaoFacadeService;
    private final ApiResponseFactory apiResponseFactory;

    public PjbArquiteturaSubstituicaoNacionalController(PjbArquiteturaSubstituicaoNacionalFacadeService arquiteturaFacadeService,
                                                        PjbSubstituicaoNacionalProgramaFacadeService programaFacadeService,
                                                        PjbSubstituicaoFederativaCentroComandoFacadeService federativoCentroComandoFacadeService,
                                                        PjbSubstituicaoFederativaWarRoomFacadeService federativoWarRoomFacadeService,
                                                        PjbSubstituicaoFederativaCutoverMatrixFacadeService federativoCutoverMatrixFacadeService,
                                                        PjbSubstituicaoFederativaNucleoDuroFacadeService federativoNucleoDuroFacadeService,
                                                        PjbSubstituicaoFederativaMalhaJulgadoraFacadeService federativoMalhaJulgadoraFacadeService,
                                                        PjbSubstituicaoFederativaPrecedentesQualificadosFacadeService federativoPrecedentesQualificadosFacadeService,
                                                        PjbSubstituicaoFederativaTutelaColetivaFacadeService federativoTutelaColetivaFacadeService,
                                                        PjbSubstituicaoFederativaPosColetivaFacadeService federativoPosColetivaFacadeService,
                                                        PjbPlataformaSustentacaoFacadeService plataformaSustentacaoFacadeService,
                                                        ApiResponseFactory apiResponseFactory) {
        this.arquiteturaFacadeService = Objects.requireNonNull(arquiteturaFacadeService);
        this.programaFacadeService = Objects.requireNonNull(programaFacadeService);
        this.federativoCentroComandoFacadeService = Objects.requireNonNull(federativoCentroComandoFacadeService);
        this.federativoWarRoomFacadeService = Objects.requireNonNull(federativoWarRoomFacadeService);
        this.federativoCutoverMatrixFacadeService = Objects.requireNonNull(federativoCutoverMatrixFacadeService);
        this.federativoNucleoDuroFacadeService = Objects.requireNonNull(federativoNucleoDuroFacadeService);
        this.federativoMalhaJulgadoraFacadeService = Objects.requireNonNull(federativoMalhaJulgadoraFacadeService);
        this.federativoPrecedentesQualificadosFacadeService = Objects.requireNonNull(federativoPrecedentesQualificadosFacadeService);
        this.federativoTutelaColetivaFacadeService = Objects.requireNonNull(federativoTutelaColetivaFacadeService);
        this.federativoPosColetivaFacadeService = Objects.requireNonNull(federativoPosColetivaFacadeService);
        this.plataformaSustentacaoFacadeService = Objects.requireNonNull(plataformaSustentacaoFacadeService);
        this.apiResponseFactory = Objects.requireNonNull(apiResponseFactory);
    }

    @GetMapping(PjbSubstituicaoNacionalRoutes.PATH_ARQUITETURA)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ADMIN')")
    public ResponseEntity<ApiQueryResponse<PjbArquiteturaSubstituicaoNacionalResponse>> avaliar() {
        PjbArquiteturaSubstituicaoNacionalResponse response = arquiteturaFacadeService.avaliar();
        return ResponseEntity.ok(apiResponseFactory.queryOk(response, response.fundamentos()));
    }

    @GetMapping(PjbSubstituicaoNacionalRoutes.PATH_PROGRAMA)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ADMIN')")
    public ResponseEntity<ApiQueryResponse<PjbSubstituicaoNacionalProgramaResponse>> programa() {
        PjbSubstituicaoNacionalProgramaResponse response = programaFacadeService.avaliar();
        return ResponseEntity.ok(apiResponseFactory.queryOk(response, response.fundamentos()));
    }

    @GetMapping(PjbSubstituicaoNacionalRoutes.PATH_CENTRO_COMANDO)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ADMIN')")
    public ResponseEntity<ApiQueryResponse<PjbSubstituicaoFederativaCentroComandoResponse>> centroComando() {
        PjbSubstituicaoFederativaCentroComandoResponse response = federativoCentroComandoFacadeService.avaliar();
        return ResponseEntity.ok(apiResponseFactory.queryOk(response, response.fundamentos()));
    }

    @GetMapping(PjbSubstituicaoNacionalRoutes.PATH_CENTRO_COMANDO_TRIBUNAL)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ADMIN')")
    public ResponseEntity<ApiQueryResponse<PjbSubstituicaoFederativaTribunalResponse>> centroComandoTribunal(@PathVariable String tribunalCodigo) {
        PjbSubstituicaoFederativaTribunalResponse response = federativoCentroComandoFacadeService.avaliarTribunal(tribunalCodigo);
        return ResponseEntity.ok(apiResponseFactory.queryOk(response, response.bloqueadores()));
    }

    @GetMapping(PjbSubstituicaoNacionalRoutes.PATH_WAR_ROOM)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ADMIN')")
    public ResponseEntity<ApiQueryResponse<PjbSubstituicaoFederativaWarRoomResponse>> warRoom() {
        PjbSubstituicaoFederativaWarRoomResponse response = federativoWarRoomFacadeService.avaliar();
        return ResponseEntity.ok(apiResponseFactory.queryOk(response, response.fundamentos()));
    }

    @GetMapping(PjbSubstituicaoNacionalRoutes.PATH_WAR_ROOM_TRIBUNAL)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ADMIN')")
    public ResponseEntity<ApiQueryResponse<PjbSubstituicaoFederativaWarRoomTribunalResponse>> warRoomTribunal(@PathVariable String tribunalCodigo) {
        PjbSubstituicaoFederativaWarRoomTribunalResponse response = federativoWarRoomFacadeService.avaliarTribunal(tribunalCodigo);
        return ResponseEntity.ok(apiResponseFactory.queryOk(response, response.bloqueadores()));
    }

    @GetMapping(PjbSubstituicaoNacionalRoutes.PATH_CUTOVER_MATRIX)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ADMIN')")
    public ResponseEntity<ApiQueryResponse<PjbSubstituicaoFederativaCutoverMatrixResponse>> cutoverMatrix() {
        PjbSubstituicaoFederativaCutoverMatrixResponse response = federativoCutoverMatrixFacadeService.avaliar();
        return ResponseEntity.ok(apiResponseFactory.queryOk(response, response.fundamentos()));
    }

    @GetMapping(PjbSubstituicaoNacionalRoutes.PATH_CUTOVER_MATRIX_TRIBUNAL)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ADMIN')")
    public ResponseEntity<ApiQueryResponse<PjbSubstituicaoFederativaCutoverTribunalResponse>> cutoverMatrixTribunal(@PathVariable String tribunalCodigo) {
        PjbSubstituicaoFederativaCutoverTribunalResponse response = federativoCutoverMatrixFacadeService.avaliarTribunal(tribunalCodigo);
        return ResponseEntity.ok(apiResponseFactory.queryOk(response, response.bloqueadores()));
    }

    @GetMapping(PjbSubstituicaoNacionalRoutes.PATH_NUCLEO_DURO)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ADMIN')")
    public ResponseEntity<ApiQueryResponse<PjbSubstituicaoFederativaNucleoDuroResponse>> nucleoDuro() {
        PjbSubstituicaoFederativaNucleoDuroResponse response = federativoNucleoDuroFacadeService.avaliar();
        return ResponseEntity.ok(apiResponseFactory.queryOk(response, response.fundamentos()));
    }

    @GetMapping(PjbSubstituicaoNacionalRoutes.PATH_NUCLEO_DURO_TRIBUNAL)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ADMIN')")
    public ResponseEntity<ApiQueryResponse<PjbSubstituicaoFederativaNucleoDuroTribunalResponse>> nucleoDuroTribunal(@PathVariable String tribunalCodigo) {
        PjbSubstituicaoFederativaNucleoDuroTribunalResponse response = federativoNucleoDuroFacadeService.avaliarTribunal(tribunalCodigo);
        return ResponseEntity.ok(apiResponseFactory.queryOk(response, response.bloqueadores()));
    }


    @GetMapping(PjbSubstituicaoNacionalRoutes.PATH_MALHA_JULGADORA)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ADMIN')")
    public ResponseEntity<ApiQueryResponse<PjbSubstituicaoFederativaMalhaJulgadoraResponse>> malhaJulgadora() {
        PjbSubstituicaoFederativaMalhaJulgadoraResponse response = federativoMalhaJulgadoraFacadeService.avaliar();
        return ResponseEntity.ok(apiResponseFactory.queryOk(response, response.fundamentos()));
    }

    @GetMapping(PjbSubstituicaoNacionalRoutes.PATH_MALHA_JULGADORA_TRIBUNAL)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ADMIN')")
    public ResponseEntity<ApiQueryResponse<PjbSubstituicaoFederativaMalhaJulgadoraTribunalResponse>> malhaJulgadoraTribunal(@PathVariable String tribunalCodigo) {
        PjbSubstituicaoFederativaMalhaJulgadoraTribunalResponse response = federativoMalhaJulgadoraFacadeService.avaliarTribunal(tribunalCodigo);
        return ResponseEntity.ok(apiResponseFactory.queryOk(response, response.bloqueadores()));
    }

    @GetMapping(PjbSubstituicaoNacionalRoutes.PATH_PRECEDENTES_QUALIFICADOS)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ADMIN')")
    public ResponseEntity<ApiQueryResponse<PjbSubstituicaoFederativaPrecedentesQualificadosResponse>> precedentesQualificados() {
        PjbSubstituicaoFederativaPrecedentesQualificadosResponse response = federativoPrecedentesQualificadosFacadeService.avaliar();
        return ResponseEntity.ok(apiResponseFactory.queryOk(response, response.fundamentos()));
    }

    @GetMapping(PjbSubstituicaoNacionalRoutes.PATH_PRECEDENTES_QUALIFICADOS_TRIBUNAL)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ADMIN')")
    public ResponseEntity<ApiQueryResponse<PjbSubstituicaoFederativaPrecedentesQualificadosTribunalResponse>> precedentesQualificadosTribunal(@PathVariable String tribunalCodigo) {
        PjbSubstituicaoFederativaPrecedentesQualificadosTribunalResponse response = federativoPrecedentesQualificadosFacadeService.avaliarTribunal(tribunalCodigo);
        return ResponseEntity.ok(apiResponseFactory.queryOk(response, response.bloqueadores()));
    }

    @GetMapping(PjbSubstituicaoNacionalRoutes.PATH_TUTELA_COLETIVA)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ADMIN')")
    public ResponseEntity<ApiQueryResponse<PjbSubstituicaoFederativaTutelaColetivaResponse>> tutelaColetiva() {
        PjbSubstituicaoFederativaTutelaColetivaResponse response = federativoTutelaColetivaFacadeService.avaliar();
        return ResponseEntity.ok(apiResponseFactory.queryOk(response, response.fundamentos()));
    }

    @GetMapping(PjbSubstituicaoNacionalRoutes.PATH_TUTELA_COLETIVA_TRIBUNAL)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ADMIN')")
    public ResponseEntity<ApiQueryResponse<PjbSubstituicaoFederativaTutelaColetivaTribunalResponse>> tutelaColetivaTribunal(@PathVariable String tribunalCodigo) {
        PjbSubstituicaoFederativaTutelaColetivaTribunalResponse response = federativoTutelaColetivaFacadeService.avaliarTribunal(tribunalCodigo);
        return ResponseEntity.ok(apiResponseFactory.queryOk(response, response.bloqueadores()));
    }

    @GetMapping(PjbSubstituicaoNacionalRoutes.PATH_POS_COLETIVA)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ADMIN')")
    public ResponseEntity<ApiQueryResponse<PjbSubstituicaoFederativaPosColetivaResponse>> posColetiva() {
        PjbSubstituicaoFederativaPosColetivaResponse response = federativoPosColetivaFacadeService.avaliar();
        return ResponseEntity.ok(apiResponseFactory.queryOk(response, response.fundamentos()));
    }

    @GetMapping(PjbSubstituicaoNacionalRoutes.PATH_POS_COLETIVA_TRIBUNAL)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ADMIN')")
    public ResponseEntity<ApiQueryResponse<PjbSubstituicaoFederativaPosColetivaTribunalResponse>> posColetivaTribunal(@PathVariable String tribunalCodigo) {
        PjbSubstituicaoFederativaPosColetivaTribunalResponse response = federativoPosColetivaFacadeService.avaliarTribunal(tribunalCodigo);
        return ResponseEntity.ok(apiResponseFactory.queryOk(response, response.bloqueadores()));
    }

    @GetMapping(PjbSubstituicaoNacionalRoutes.PATH_SUSTENTACAO)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ADMIN')")
    public ResponseEntity<ApiQueryResponse<PjbPlataformaSustentacaoResponse>> sustentacao() {
        PjbPlataformaSustentacaoResponse response = plataformaSustentacaoFacadeService.avaliar();
        return ResponseEntity.ok(apiResponseFactory.queryOk(response, response.fundamentos()));
    }

}
