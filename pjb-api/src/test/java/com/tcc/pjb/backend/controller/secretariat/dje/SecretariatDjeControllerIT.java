package com.tcc.pjb.backend.controller.secretariat.dje;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tcc.pjb.backend.core.dje.DjeApplicationService;
import com.tcc.pjb.backend.core.dje.domain.DjeLifecycleExecutionSummary;
import com.tcc.pjb.backend.core.dje.domain.DjeProcessoPublicationView;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class SecretariatDjeControllerIT {

    private final DjeApplicationService applicationService = mock(DjeApplicationService.class);
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new SecretariatDjeController(applicationService)).build();

    @Test
    void executaLifecycleDoDjeEDelegaParaOServicoExistente() throws Exception {
        DjeLifecycleExecutionSummary summary = new DjeLifecycleExecutionSummary(3, 2);
        when(applicationService.lifecycleRun(eq(null), eq(null))).thenReturn(summary);

        mockMvc.perform(post("/api/v1/secretariat/dje/lifecycle/run"))
                .andExpect(status().isOk());

        verify(applicationService).lifecycleRun(eq(null), eq(null));
    }

    @Test
    void executaLifecycleComDataEBatchSizeInformados() throws Exception {
        DjeLifecycleExecutionSummary summary = new DjeLifecycleExecutionSummary(1, 1);
        when(applicationService.lifecycleRun(eq(LocalDate.of(2027, 3, 10)), eq(50))).thenReturn(summary);

        mockMvc.perform(post("/api/v1/secretariat/dje/lifecycle/run")
                        .param("hoje", "2027-03-10")
                        .param("batchSize", "50"))
                .andExpect(status().isOk());

        verify(applicationService).lifecycleRun(eq(LocalDate.of(2027, 3, 10)), eq(50));
    }

    @Test
    void consultaPublicacaoDoProcessoDelegandoParaOServicoExistente() throws Exception {
        DjeProcessoPublicationView view = new DjeProcessoPublicationView(80L, 900L, "PUBLICADO", "TJCE");
        when(applicationService.processoPublication(80L)).thenReturn(view);

        mockMvc.perform(get("/api/v1/secretariat/dje/processos/{processoId}/publicacao", 80L))
                .andExpect(status().isOk());

        verify(applicationService).processoPublication(80L);
    }
}
