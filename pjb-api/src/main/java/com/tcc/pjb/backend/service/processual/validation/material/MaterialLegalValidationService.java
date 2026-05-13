package com.tcc.pjb.backend.service.processual.validation.material;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleDecision;
import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleMachine;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.core.validator.FaseValidatorService;
import com.tcc.pjb.backend.model.dto.processual.validation.material.MaterialLegalValidationRequest;
import com.tcc.pjb.backend.model.dto.processual.validation.material.MaterialLegalValidationResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;

@Service
public class MaterialLegalValidationService {

    private final ProcessoRepository processoRepository;
    private final PjbAuthorizationService authorizationService;
    private final ProcessoLifecycleMachine processoLifecycleMachine;
    private final FaseValidatorService faseValidatorService;

    public MaterialLegalValidationService(ProcessoRepository processoRepository,
                                          PjbAuthorizationService authorizationService,
                                          ProcessoLifecycleMachine processoLifecycleMachine,
                                          FaseValidatorService faseValidatorService) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.authorizationService = Objects.requireNonNull(authorizationService);
        this.processoLifecycleMachine = Objects.requireNonNull(processoLifecycleMachine);
        this.faseValidatorService = Objects.requireNonNull(faseValidatorService);
    }

    public MaterialLegalValidationResponse validar(MaterialLegalValidationRequest request) {
        Objects.requireNonNull(request);
        Processo processo = processoRepository.findById(request.processoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", request.processoId()));
        authorizationService.requireReadProcesso(processo);
        ProcessoLifecycleDecision decision = processoLifecycleMachine.preview(processo, request.action());
        List<String> blockers = new ArrayList<>();
        List<String> warnings = new ArrayList<>(decision.alertas());
        if (!decision.permitida()) {
            blockers.add(decision.motivo());
        }
        if (processo.getRito() == null) {
            blockers.add("Processo sem rito processual consolidado para validação material do ato.");
        }
        if (processo.getRamoDireito() == null) {
            blockers.add("Processo sem ramo do direito consolidado para o ato requerido.");
        }
        if (request.exigeDocumentoPrincipal() && isBlank(request.documentoPrincipalNome())) {
            blockers.add("Documento principal obrigatório não foi informado para o ato processual.");
        }
        if (request.exigeFundamentacao() && isBlank(request.fundamentacao())) {
            blockers.add("Fundamentação obrigatória ausente para o ato solicitado.");
        }
        if (request.exigeCompetenciaFechada() && competenciaStatus(processo).startsWith("PENDING")) {
            blockers.add("Competência ainda não está fechada em tribunal e unidade judiciária para o ato pretendido.");
        }
        try {
            if (decision.permitida()) {
                faseValidatorService.validarMudancaFase(processo, decision.faseDestino());
            }
        } catch (IllegalStateException ex) {
            blockers.add(ex.getMessage());
        }
        if (processo.getNivelSigilo() != null && processo.getNivelSigilo().exigeCredencial()) {
            warnings.add("Ato exige tratamento de sigilo reforçado e credencial compatível.");
        }
        if (isBlank(request.finalidade())) {
            warnings.add("Finalidade do ato não foi explicitada para trilha decisória complementar.");
        }
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("faseDestino", decision.faseDestino() != null ? decision.faseDestino().name() : null);
        metadata.put("statusDestino", decision.statusDestino() != null ? decision.statusDestino().name() : null);
        metadata.put("responsavelPrimario", decision.responsavelSugerido());
        metadata.put("atoProcessual", decision.atoProcessual() != null ? decision.atoProcessual().codigo() : null);
        metadata.put("finalidade", request.finalidade());
        metadata.put("documentoPrincipal", request.documentoPrincipalNome());
        metadata.put("fundamentacaoInformada", !isBlank(request.fundamentacao()));
        metadata.values().removeIf(Objects::isNull);
        return new MaterialLegalValidationResponse(
                processo.getId(),
                processo.getNumeroProcesso(),
                request.action().name(),
                blockers.isEmpty(),
                competenciaStatus(processo),
                processo.getFaseAtual() != null ? processo.getFaseAtual().name() : null,
                processo.getStatusProcesso() != null ? processo.getStatusProcesso().name() : null,
                blockers,
                warnings,
                metadata,
                Instant.now()
        );
    }

    private String competenciaStatus(Processo processo) {
        if (!isBlank(processo.getTribunalCodigoRoteado()) && !isBlank(processo.getUnidadeJudiciariaCodigo())) {
            return "RESOLVED";
        }
        if (!isBlank(processo.getTribunalCodigoRoteado()) || processo.getJurisdicao() != null) {
            return "PENDING_UNIT";
        }
        return "PENDING_TRIBUNAL";
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
