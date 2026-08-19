package com.tcc.pjb.backend.controller.secretariat.malote;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.service.secretariat.ingest.ProcessoExternoCargaService;
import com.tcc.pjb.backend.service.secretariat.ingest.ProcessoExternoCargaStatus;
import com.tcc.pjb.backend.service.secretariat.ingest.ProcessoExternoImportacaoService;
import com.tcc.pjb.backend.service.secretariat.ingest.SistemaProcessualOrigem;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class SecretariatMaloteDigitalControllerIT {

    private final ProcessoExternoCargaService cargaService = mock(ProcessoExternoCargaService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MockMvc mockMvc =
            MockMvcBuilders.standaloneSetup(new SecretariatMaloteDigitalController(cargaService)).build();

    @Test
    void processaMaloteEDelegaParaOServicoExistente() throws Exception {
        var item = new ProcessoExternoCargaService.CargaItem(
                "PJE", null, "0001234-12.2025.8.06.0001", "Acao Civil", Map.of());
        var resposta = new ProcessoExternoImportacaoService.ImportacaoResponse(
                UUID.randomUUID(), ProcessoExternoCargaStatus.IMPORTADO, SistemaProcessualOrigem.PJE,
                null, "Processo importado de PJe 1.x — conferência recomendada.");
        var resultado = new ProcessoExternoCargaService.CargaResultado(1, 1, 0, 0, List.of(resposta));
        when(cargaService.processarLote(anyList())).thenReturn(resultado);

        mockMvc.perform(post("/api/v1/secretariat/malote/processar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(item))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dados.total").value(1))
                .andExpect(jsonPath("$.dados.importados").value(1));

        verify(cargaService).processarLote(anyList());
    }
}
