package com.tcc.pjb.backend.service.processual.protocolo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.security.abac.AuthzDecision;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.dto.processual.protocolo.ProtocoloReciboResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProtocoloReciboConsultaServiceTest {

    private final ProcessoRepository processoRepository = mock(ProcessoRepository.class);
    private final PjbAuthorizationService authorizationService = mock(PjbAuthorizationService.class);
    private final ProtocoloReciboService protocoloReciboService = mock(ProtocoloReciboService.class);

    private final ProtocoloReciboConsultaService service =
            new ProtocoloReciboConsultaService(processoRepository, authorizationService, protocoloReciboService);

    private Processo processo;
    private Usuario solicitante;

    @BeforeEach
    void preparar() {
        processo = new Processo();
        processo.setId(7L);
        processo.setNumeroProcesso("0001234-55.2026.8.06.0001");
        solicitante = new Usuario();
    }

    @Test
    void processoLegivelPeloSolicitanteDevolveORecibo() {
        ProtocoloReciboResponse esperado = new ProtocoloReciboResponse("doc-1", 7L,
                "0001234-55.2026.8.06.0001", "PJB-PROTOCOLO:7", "abc123",
                Instant.parse("2026-09-13T12:00:00Z"), "recibo");
        when(processoRepository.findById(7L)).thenReturn(Optional.of(processo));
        when(authorizationService.canReadProcesso(processo))
                .thenReturn(AuthzDecision.allow("parte do processo", "v1"));
        when(protocoloReciboService.obterOuEmitirRecibo(processo, solicitante)).thenReturn(esperado);

        assertThat(service.reciboDoProcesso(7L, solicitante)).isSameAs(esperado);
    }

    @Test
    void leituraNegadaImpedeAEmissaoDoRecibo() {
        when(processoRepository.findById(7L)).thenReturn(Optional.of(processo));
        when(authorizationService.canReadProcesso(processo))
                .thenReturn(AuthzDecision.deny("sem vinculo com o processo", "v1"));

        assertThatThrownBy(() -> service.reciboDoProcesso(7L, solicitante))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("sem vinculo com o processo");

        verify(protocoloReciboService, never()).obterOuEmitirRecibo(any(), any());
    }

    @Test
    void processoInexistenteNaoChegaAAvaliarAutorizacao() {
        when(processoRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reciboDoProcesso(7L, solicitante))
                .isInstanceOf(RecursoNaoEncontradoException.class);

        verify(authorizationService, never()).canReadProcesso(any());
        verify(protocoloReciboService, never()).obterOuEmitirRecibo(any(), any());
    }
}
