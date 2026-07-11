package com.tcc.pjb.backend.service.processual.polo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.tcc.pjb.backend.core.processo.polo.application.PoloProcessualApplicationService;
import com.tcc.pjb.backend.core.processo.polo.motor.PoloCompositionPolicy;
import com.tcc.pjb.backend.core.validation.document.DocumentoNacionalValidator;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoParte;
import com.tcc.pjb.backend.model.entity.enums.TipoPolo;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PeticionamentoInicialPolosServiceTest {

    private final PoloProcessualApplicationService poloProcessualApplicationService = mock(PoloProcessualApplicationService.class);
    private final PeticionamentoInicialPolosService service = new PeticionamentoInicialPolosService(
            poloProcessualApplicationService,
            new PoloCompositionPolicy(),
            new DocumentoNacionalValidator());

    @Test
    void registrarUsaPapelPorRitoEmVezDeAutorReuFixo() {
        Processo processo = Processo.builder()
                .id(1L)
                .rito(RitoProcessual.TRABALHISTA_ORDINARIO)
                .parteAutoraNome("Maria Reclamante")
                .parteReuNome("Empresa Reclamada Ltda")
                .build();
        Usuario advogado = Usuario.builder()
                .id(10L)
                .nome("Advogado Teste")
                .tipoUsuario(TipoUsuario.ADVOGADO)
                .oab("12345")
                .oabUf("CE")
                .build();

        service.registrar(processo, advogado);

        ArgumentCaptor<TipoPolo> tipoPoloCaptor = ArgumentCaptor.forClass(TipoPolo.class);
        ArgumentCaptor<TipoParte> tipoParteCaptor = ArgumentCaptor.forClass(TipoParte.class);
        ArgumentCaptor<String> nomeCaptor = ArgumentCaptor.forClass(String.class);
        verify(poloProcessualApplicationService, times(2)).incluir(
                eq(1L),
                tipoPoloCaptor.capture(),
                tipoParteCaptor.capture(),
                nomeCaptor.capture(),
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any());

        assertThat(tipoParteCaptor.getAllValues()).containsExactlyInAnyOrder(TipoParte.RECLAMANTE, TipoParte.RECLAMADA);
        assertThat(tipoPoloCaptor.getAllValues()).containsExactlyInAnyOrder(TipoPolo.ATIVO, TipoPolo.PASSIVO);
        assertThat(nomeCaptor.getAllValues()).containsExactlyInAnyOrder("Maria Reclamante", "Empresa Reclamada Ltda");
    }
}
