package com.tcc.pjb.backend.service.processual.surface;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.controller.processual.surface.evolution.ProcessoEvolucaoOperacionalController;
import com.tcc.pjb.backend.controller.processual.surface.governance.ProcessoGovernancaVersionadaController;
import com.tcc.pjb.backend.controller.processual.surface.hardening.ProcessoFatiasSensivelController;
import com.tcc.pjb.backend.controller.processual.surface.hardening.ProcessoFechamentoAvancadoController;
import com.tcc.pjb.backend.controller.processual.surface.hardening.ProcessoSigiloInteligenteController;
import com.tcc.pjb.backend.controller.processual.surface.unificado.ProcessoOrquestracaoUnificadaController;
import com.tcc.pjb.backend.controller.processual.surface.unificado.ProcessoPlataformaNacionalController;
import com.tcc.pjb.backend.controller.processual.surface.unificado.ProcessoUnificadoNacionalController;
import com.tcc.pjb.backend.model.dto.processual.surface.aggregate.ProcessoSurfaceAggregateResponse;
import com.tcc.pjb.backend.model.dto.processual.surface.aggregate.ProcessoSurfaceIdentityResponse;
import com.tcc.pjb.backend.model.dto.processual.surface.aggregate.ProcessoSurfaceValueItemResponse;
import com.tcc.pjb.backend.model.dto.processual.surface.unificado.ProcessoSurfaceAtoResponse;
import com.tcc.pjb.backend.model.dto.processual.surface.unificado.ProcessoSurfaceCompetenciaResponse;
import com.tcc.pjb.backend.model.dto.processual.surface.unificado.ProcessoSurfaceDiagnosticoResponse;
import com.tcc.pjb.backend.model.dto.processual.surface.unificado.ProcessoSurfacePerfilResponse;
import com.tcc.pjb.backend.model.dto.processual.surface.unificado.ProcessoUnificadoSurfaceResponse;
import org.junit.jupiter.api.Test;

class ProcessoSurfaceStructureRefinementArchitectureTest {

    @Test
    void dtoDeSurfaceDevemViverEmSubpacotesAggregateEUnificado() {
        assertThat(ProcessoSurfaceAggregateResponse.class.getPackageName()).endsWith(".aggregate");
        assertThat(ProcessoSurfaceIdentityResponse.class.getPackageName()).endsWith(".aggregate");
        assertThat(ProcessoSurfaceValueItemResponse.class.getPackageName()).endsWith(".aggregate");
        assertThat(ProcessoSurfaceAtoResponse.class.getPackageName()).endsWith(".unificado");
        assertThat(ProcessoSurfaceCompetenciaResponse.class.getPackageName()).endsWith(".unificado");
        assertThat(ProcessoSurfaceDiagnosticoResponse.class.getPackageName()).endsWith(".unificado");
        assertThat(ProcessoSurfacePerfilResponse.class.getPackageName()).endsWith(".unificado");
        assertThat(ProcessoUnificadoSurfaceResponse.class.getPackageName()).endsWith(".unificado");
    }

    @Test
    void controllersDeSurfaceDevemViverEmSubpacotesDedicados() {
        assertThat(ProcessoUnificadoNacionalController.class.getPackageName()).endsWith(".unificado");
        assertThat(ProcessoOrquestracaoUnificadaController.class.getPackageName()).endsWith(".unificado");
        assertThat(ProcessoPlataformaNacionalController.class.getPackageName()).endsWith(".unificado");
        assertThat(ProcessoGovernancaVersionadaController.class.getPackageName()).endsWith(".governance");
        assertThat(ProcessoFatiasSensivelController.class.getPackageName()).endsWith(".hardening");
        assertThat(ProcessoFechamentoAvancadoController.class.getPackageName()).endsWith(".hardening");
        assertThat(ProcessoSigiloInteligenteController.class.getPackageName()).endsWith(".hardening");
        assertThat(ProcessoEvolucaoOperacionalController.class.getPackageName()).endsWith(".evolution");
    }
}
