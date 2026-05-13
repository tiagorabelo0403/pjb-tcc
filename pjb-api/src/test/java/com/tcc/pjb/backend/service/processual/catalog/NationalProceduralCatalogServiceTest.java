package com.tcc.pjb.backend.service.processual.catalog;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.processual.ato.AtoProcessualCatalogService;
import com.tcc.pjb.backend.model.dto.processual.catalog.NationalProceduralCatalogRequest;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.ui.assunto.AssuntoCatalogRegistry;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.io.DefaultResourceLoader;

class NationalProceduralCatalogServiceTest {

    @Test
    void shouldReturnCatalogSlices() {
        ProcessoRepository processoRepository = Mockito.mock(ProcessoRepository.class);
        Processo processo = new Processo();
        processo.setId(5L);
        processo.setNumeroProcesso("0005");
        processo.setRamoDireito(RamoDireito.CIVIL);
        processo.setClasseProcessual("Procedimento Comum");
        processo.setAssunto("Indenização");
        when(processoRepository.findById(5L)).thenReturn(Optional.of(processo));
        NationalProceduralCatalogService service = new NationalProceduralCatalogService(
                processoRepository,
                new AssuntoCatalogRegistry(new ObjectMapper(), new DefaultResourceLoader(), ""),
                new AtoProcessualCatalogService()
        );
        var response = service.consultar(new NationalProceduralCatalogRequest("inden", 5L, 5));
        assertFalse(response.classes().isEmpty());
        assertFalse(response.movimentos().isEmpty());
    }
}
