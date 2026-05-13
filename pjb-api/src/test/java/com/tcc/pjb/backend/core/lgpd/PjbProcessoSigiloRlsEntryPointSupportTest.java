package com.tcc.pjb.backend.core.lgpd;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.configs.datasource.PjbProcessoSigiloRlsContext;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class PjbProcessoSigiloRlsEntryPointSupportTest {

    @Test
    void deveVincularERestaurarContextoAoExecutarComProcesso() {
        ProcessoSigiloRlsEnvelopeService envelopeService = mock(ProcessoSigiloRlsEnvelopeService.class);
        PjbProcessoSigiloRlsContext context = new PjbProcessoSigiloRlsContext();
        ProcessoSigiloRlsEnvelope previousEnvelope = envelope(7L, "SIGILO_N1", "TJCE", "FORO-1", "PROC_SIGILO|TJCE|FORO-1|SIGILO_N1");
        ProcessoSigiloRlsEnvelope targetEnvelope = envelope(42L, "SIGILO_N3", "TRF5", "VARA-9", "PROC_SIGILO|TRF5|VARA-9|SIGILO_N3");
        PjbProcessoSigiloRlsContext.SessionSettings previous = context.bind(previousEnvelope);
        assertThat(previous).isNull();
        when(envelopeService.materialize(42L, "KAFKA")).thenReturn(targetEnvelope);
        PjbProcessoSigiloRlsEntryPointSupport support = new PjbProcessoSigiloRlsEntryPointSupport(envelopeService, context);
        AtomicReference<PjbProcessoSigiloRlsContext.SessionSettings> observed = new AtomicReference<>();

        support.runWithProcessoContext(42L, "KAFKA", () -> observed.set(context.current()));

        assertThat(observed.get()).isEqualTo(new PjbProcessoSigiloRlsContext.SessionSettings("SIGILO_N3", "TRF5", "VARA-9", "PROC_SIGILO|TRF5|VARA-9|SIGILO_N3"));
        assertThat(context.current()).isEqualTo(new PjbProcessoSigiloRlsContext.SessionSettings("SIGILO_N1", "TJCE", "FORO-1", "PROC_SIGILO|TJCE|FORO-1|SIGILO_N1"));
    }

    @Test
    void deveRestaurarContextoAnteriorQuandoExecucaoFalhar() {
        ProcessoSigiloRlsEnvelopeService envelopeService = mock(ProcessoSigiloRlsEnvelopeService.class);
        PjbProcessoSigiloRlsContext context = new PjbProcessoSigiloRlsContext();
        context.bind(envelope(9L, "SIGILO_N2", "TJPI", "VARA-2", "PROC_SIGILO|TJPI|VARA-2|SIGILO_N2"));
        when(envelopeService.materialize(99L, "EVENT")).thenReturn(envelope(99L, "SIGILO_N4", "STM", "AUDITORIA-1", "PROC_SIGILO|STM|AUDITORIA-1|SIGILO_N4"));
        PjbProcessoSigiloRlsEntryPointSupport support = new PjbProcessoSigiloRlsEntryPointSupport(envelopeService, context);

        assertThatThrownBy(() -> support.runWithProcessoContext(99L, "EVENT", () -> {
            throw new IllegalStateException("falha-esperada");
        })).isInstanceOf(IllegalStateException.class)
                .hasMessage("falha-esperada");

        assertThat(context.current()).isEqualTo(new PjbProcessoSigiloRlsContext.SessionSettings("SIGILO_N2", "TJPI", "VARA-2", "PROC_SIGILO|TJPI|VARA-2|SIGILO_N2"));
    }

    @Test
    void deveExecutarSemConsultarEnvelopeQuandoProcessoNaoForInformado() {
        ProcessoSigiloRlsEnvelopeService envelopeService = mock(ProcessoSigiloRlsEnvelopeService.class);
        PjbProcessoSigiloRlsContext context = new PjbProcessoSigiloRlsContext();
        PjbProcessoSigiloRlsEntryPointSupport support = new PjbProcessoSigiloRlsEntryPointSupport(envelopeService, context);

        String result = support.supplyWithProcessoContext(null, "EVENT", () -> "ok");

        assertThat(result).isEqualTo("ok");
        assertThat(context.current()).isNull();
        verifyNoInteractions(envelopeService);
    }

    private ProcessoSigiloRlsEnvelope envelope(Long processoId,
                                               String clearance,
                                               String tribunal,
                                               String unidade,
                                               String scope) {
        return new ProcessoSigiloRlsEnvelope(
                processoId,
                "processo",
                clearance,
                clearance,
                unidade,
                tribunal,
                scope,
                true,
                true,
                true,
                true,
                false,
                java.util.List.of("PROCESSUAL"),
                java.util.List.of(),
                null,
                null
        );
    }
}
