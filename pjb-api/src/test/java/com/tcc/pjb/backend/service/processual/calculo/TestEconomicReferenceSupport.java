package com.tcc.pjb.backend.service.processual.calculo;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.repository.SalarioMinimoNacionalRepository;
import com.tcc.pjb.backend.service.financeiro.SalarioMinimoNacionalService;
import java.util.List;
import java.util.Optional;

public final class TestEconomicReferenceSupport {

    private TestEconomicReferenceSupport() {
    }

    public static CalculoJudicialEconomicReferenceService economicReferenceService() {
        SalarioMinimoNacionalRepository repository = mock(SalarioMinimoNacionalRepository.class);
        when(repository.findTopByVigenteDesdeLessThanEqualAndAtivoTrueOrderByVigenteDesdeDesc(org.mockito.ArgumentMatchers.any())).thenReturn(Optional.empty());
        when(repository.findTopByAnoReferenciaLessThanEqualAndAtivoTrueOrderByAnoReferenciaDesc(org.mockito.ArgumentMatchers.anyInt())).thenReturn(Optional.empty());
        when(repository.findByAnoReferencia(org.mockito.ArgumentMatchers.anyInt())).thenReturn(Optional.empty());
        when(repository.findAllByAtivoTrueOrderByAnoReferenciaAsc()).thenReturn(java.util.List.of());
        return new CalculoJudicialEconomicReferenceService(new SalarioMinimoNacionalService(repository));
    }
}
