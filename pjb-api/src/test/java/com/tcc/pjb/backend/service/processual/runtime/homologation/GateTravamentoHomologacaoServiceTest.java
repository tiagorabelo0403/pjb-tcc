package com.tcc.pjb.backend.service.processual.runtime.homologation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.comunicacao.institucional.gate.domain.InstitutionalGateState;
import com.tcc.pjb.backend.core.comunicacao.institucional.gate.domain.InstitutionalGateStatus;
import com.tcc.pjb.backend.core.comunicacao.institucional.gate.infrastructure.InstitutionalGateStateRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GateTravamentoHomologacaoServiceTest {

    @Mock
    private InstitutionalGateStateRepository repository;

    @InjectMocks
    private GateTravamentoHomologacaoService service;

    @Test
    void deveBloquearHomologacaoQuandoVistaDoMpPendente() {
        Instant now = Instant.now();
        InstitutionalGateState gate = new InstitutionalGateState(
                "gate-1",
                "exp-1",
                10L,
                "0001",
                "GATE_MP_INTERESSE_INCAPAZ",
                InstitutionalGateStatus.AGUARDANDO_CIENCIA,
                true,
                "Vista pendente",
                null,
                now,
                now,
                null,
                List.of("gate_pendente_ciencia"),
                "hash"
        );
        when(repository.findByProcessoId(10L)).thenReturn(List.of(gate));

        var response = service.avaliar(10L, "HOMOLOGAR_ACORDO");

        assertTrue(response.blocked());
        assertEquals(List.of("GATE_MP_VISTA_PENDENTE"), response.blockerCodes());
        assertEquals("MINISTERIO_PUBLICO", response.details().getFirst().categoria());
    }
}
