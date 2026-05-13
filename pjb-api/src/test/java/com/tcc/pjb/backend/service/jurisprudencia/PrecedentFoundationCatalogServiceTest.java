package com.tcc.pjb.backend.service.jurisprudencia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.dto.jurisprudencia.PrecedentFoundationQueryRequest;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.TipoPrecedente;
import com.tcc.pjb.backend.model.entity.enums.TribunalFonte;
import com.tcc.pjb.backend.model.entity.jurisprudencia.Precedente;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;

class PrecedentFoundationCatalogServiceTest {

    @Test
    void buildsContextualQueryFromProcess() {
        ProcessoRepository processoRepository = Mockito.mock(ProcessoRepository.class);
        PjbAuthorizationService authorizationService = Mockito.mock(PjbAuthorizationService.class);
        JurisprudenciaService jurisprudenciaService = Mockito.mock(JurisprudenciaService.class);
        PrecedentFoundationCatalogService service = new PrecedentFoundationCatalogService(processoRepository, authorizationService, jurisprudenciaService);
        Processo processo = new Processo();
        processo.setId(14L);
        processo.setNumeroProcesso("0014-22");
        processo.setAssunto("indenização");
        processo.setPedidoPrincipal("danos morais");
        processo.setRamoDireito(RamoDireito.CIVIL);
        when(processoRepository.findById(14L)).thenReturn(Optional.of(processo));
        Precedente precedente = Precedente.builder().id(1L).fonte(TribunalFonte.STJ).tipo(TipoPrecedente.TEMA_REPETITIVO).titulo("Tema").dataPublicacao(LocalDate.now()).build();
        when(jurisprudenciaService.search(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.<String>any(), Mockito.any(), Mockito.anyInt(), Mockito.anyInt())).thenReturn(new PageImpl<>(java.util.List.of(precedente)));

        var response = service.search(new PrecedentFoundationQueryRequest(14L, null, null, null, null, null, 0, 10));

        assertEquals(1L, response.totalResultados());
        assertFalse(response.queryEfetiva().isBlank());
    }
}
