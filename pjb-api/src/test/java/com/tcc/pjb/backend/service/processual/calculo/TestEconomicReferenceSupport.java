package com.tcc.pjb.backend.service.processual.calculo;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.time.PjbTimeService;
import com.tcc.pjb.backend.model.repository.SalarioMinimoNacionalRepository;
import com.tcc.pjb.backend.service.financeiro.SalarioMinimoNacionalService;
import com.tcc.pjb.backend.service.financeiro.TetoRpvNacionalService;
import java.time.Clock;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

public final class TestEconomicReferenceSupport {

    private TestEconomicReferenceSupport() {
    }

    public static TetoRpvNacionalService tetoRpvNacionalService() {
        return new TetoRpvNacionalService(new SalarioMinimoNacionalService(repositorioVazio()));
    }

    public static CalculoJudicialEconomicReferenceService economicReferenceService() {
        return economicReferenceService(Clock.systemUTC());
    }

    public static CalculoJudicialEconomicReferenceService economicReferenceService(Clock clock) {
        SalarioMinimoNacionalRepository repository = mock(SalarioMinimoNacionalRepository.class);
        when(repository.findTopByVigenteDesdeLessThanEqualAndAtivoTrueOrderByVigenteDesdeDesc(org.mockito.ArgumentMatchers.any())).thenReturn(Optional.empty());
        when(repository.findTopByAnoReferenciaLessThanEqualAndAtivoTrueOrderByAnoReferenciaDesc(org.mockito.ArgumentMatchers.anyInt())).thenReturn(Optional.empty());
        when(repository.findByAnoReferencia(org.mockito.ArgumentMatchers.anyInt())).thenReturn(Optional.empty());
        when(repository.findAllByAtivoTrueOrderByAnoReferenciaAsc()).thenReturn(java.util.List.of());
        return new CalculoJudicialEconomicReferenceService(new SalarioMinimoNacionalService(repository),
                new PjbTimeService(clock, ZoneId.of("America/Sao_Paulo")));
    }

    private static SalarioMinimoNacionalRepository repositorioVazio() {
        SalarioMinimoNacionalRepository repository = mock(SalarioMinimoNacionalRepository.class);
        when(repository.findTopByVigenteDesdeLessThanEqualAndAtivoTrueOrderByVigenteDesdeDesc(org.mockito.ArgumentMatchers.any())).thenReturn(Optional.empty());
        when(repository.findTopByAnoReferenciaLessThanEqualAndAtivoTrueOrderByAnoReferenciaDesc(org.mockito.ArgumentMatchers.anyInt())).thenReturn(Optional.empty());
        return repository;
    }
}
