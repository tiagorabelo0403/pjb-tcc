package com.tcc.pjb.backend.service.processual.validation.material;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleAction;
import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleMachine;
import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleMachineTestFactory;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.core.validator.FaseValidatorService;
import com.tcc.pjb.backend.model.dto.processual.validation.material.MaterialLegalValidationRequest;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class MaterialLegalValidationServiceTest {

    @Test
    void blocksWhenRequiredFieldsAndCompetenceAreMissing() {
        ProcessoRepository processoRepository = Mockito.mock(ProcessoRepository.class);
        PjbAuthorizationService authorizationService = Mockito.mock(PjbAuthorizationService.class);
        MaterialLegalValidationService service = new MaterialLegalValidationService(
                processoRepository,
                authorizationService,
                ProcessoLifecycleMachineTestFactory.standalone(),
                new FaseValidatorService()
        );
        Processo processo = new Processo();
        processo.setId(3L);
        processo.setNumeroProcesso("0003-11");
        processo.setRito(RitoProcessual.COMUM_ORDINARIO);
        processo.setRamoDireito(RamoDireito.CIVIL);
        processo.setStatusProcesso(StatusProcesso.EM_ANDAMENTO);
        when(processoRepository.findById(3L)).thenReturn(Optional.of(processo));

        var response = service.validar(new MaterialLegalValidationRequest(
                3L,
                ProcessoLifecycleAction.PROFERIR_SENTENCA,
                true,
                true,
                true,
                null,
                null,
                null
        ));

        assertFalse(response.permitido());
        assertTrue(response.blockers().stream().anyMatch(item -> item.contains("Documento principal obrigatório")));
    }

    @Test
    void permitsWhenCoreMaterialRequirementsAreSatisfied() {
        ProcessoRepository processoRepository = Mockito.mock(ProcessoRepository.class);
        PjbAuthorizationService authorizationService = Mockito.mock(PjbAuthorizationService.class);
        MaterialLegalValidationService service = new MaterialLegalValidationService(
                processoRepository,
                authorizationService,
                ProcessoLifecycleMachineTestFactory.standalone(),
                new FaseValidatorService()
        );
        Processo processo = new Processo();
        processo.setId(4L);
        processo.setNumeroProcesso("0004-11");
        processo.setRito(RitoProcessual.COMUM_ORDINARIO);
        processo.setRamoDireito(RamoDireito.CIVIL);
        processo.setStatusProcesso(StatusProcesso.EM_ANDAMENTO);
        processo.setTribunalCodigoRoteado("TJCE");
        processo.setUnidadeJudiciariaCodigo("TJCE_QUIXADA_CIVEL");
        when(processoRepository.findById(4L)).thenReturn(Optional.of(processo));

        var response = service.validar(new MaterialLegalValidationRequest(
                4L,
                ProcessoLifecycleAction.CONCLUIR_PARA_DESPACHO,
                false,
                false,
                true,
                "peticao.pdf",
                "fundamentacao",
                "saneamento"
        ));

        assertTrue(response.permitido());
    }
}
