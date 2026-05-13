package com.tcc.pjb.backend.configs.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.core.lgpd.ProcessoSigiloRlsEnvelope;
import java.util.List;
import org.junit.jupiter.api.Test;

class PjbProcessoSigiloRlsContextTest {

    @Test
    void deveMaterializarConfiguracaoDeSessaoAPartirDoEnvelope() {
        PjbProcessoSigiloRlsContext context = new PjbProcessoSigiloRlsContext();
        ProcessoSigiloRlsEnvelope envelope = new ProcessoSigiloRlsEnvelope(
                91L,
                "tb_processo",
                "SIGILO_N3",
                "SIGILO_N3",
                "UNID-11",
                "TJCE",
                "PROC_SIGILO|TJCE|UNID-11|SIGILO_N3",
                true,
                true,
                true,
                true,
                false,
                List.of("DADOS_SENSIVEIS"),
                List.of(),
                null,
                null
        );

        PjbProcessoSigiloRlsContext.SessionSettings previous = context.bind(envelope);
        PjbProcessoSigiloRlsContext.SessionSettings current = context.currentOrDefault();
        context.restore(previous);

        assertThat(current.sigiloClearance()).isEqualTo("SIGILO_N3");
        assertThat(current.tribunalCode()).isEqualTo("TJCE");
        assertThat(current.unitCode()).isEqualTo("UNID-11");
        assertThat(current.sigiloScope()).isEqualTo("PROC_SIGILO|TJCE|UNID-11|SIGILO_N3");
        assertThat(context.current()).isNull();
    }

    @Test
    void deveFornecerDefaultsQuandoNaoHaContextoVinculado() {
        PjbProcessoSigiloRlsContext context = new PjbProcessoSigiloRlsContext();

        PjbProcessoSigiloRlsContext.SessionSettings current = context.currentOrDefault();

        assertThat(current.sigiloClearance()).isEqualTo("PUBLICO");
        assertThat(current.tribunalCode()).isEmpty();
        assertThat(current.unitCode()).isEmpty();
        assertThat(current.sigiloScope()).isEmpty();
    }
}
