package com.tcc.pjb.backend.service.secretariat.institucional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.MotivoEnfileiramentoInstitucional;
import com.tcc.pjb.backend.model.entity.enums.TipoUnidadeInstitucional;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class VistaInstitucionalServiceTest {

    private final ProcessoRepository processoRepository = mock(ProcessoRepository.class);
    private final SecretariaInstitucionalEnfileiramentoService enfileiramentoService = mock(SecretariaInstitucionalEnfileiramentoService.class);
    private final VistaInstitucionalService service = new VistaInstitucionalService(processoRepository, enfileiramentoService);

    @Test
    void determinarVistaChamaEnfileiramentoComMotivoDespacho() {
        Processo processo = new Processo();
        processo.setId(7L);
        processo.setComarca("Fortaleza");
        when(processoRepository.findById(7L)).thenReturn(Optional.of(processo));

        service.determinarVista(7L, TipoUnidadeInstitucional.PROCURADORIA_PUBLICA, 10);

        verify(enfileiramentoService).enfileirar(eq(7L), eq("Fortaleza"), eq(TipoUnidadeInstitucional.PROCURADORIA_PUBLICA),
                eq(MotivoEnfileiramentoInstitucional.DESPACHO_VISTA), eq(10));
    }

    @Test
    void processoInexistenteLancaIllegalArgumentException() {
        when(processoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.determinarVista(99L, TipoUnidadeInstitucional.PROMOTORIA, 10))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void vistaParaOProprioForumLancaIllegalArgumentExceptionSemConsultarProcesso() {
        assertThatThrownBy(() -> service.determinarVista(7L, TipoUnidadeInstitucional.FORUM, 10))
                .isInstanceOf(IllegalArgumentException.class);
        verify(processoRepository, never()).findById(org.mockito.ArgumentMatchers.any());
    }
}
