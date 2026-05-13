package com.tcc.pjb.backend.core.transito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import org.junit.jupiter.api.Test;

class TerminalArchiveLinkResolverTest {

    private final TerminalArchiveLinkResolver resolver = new TerminalArchiveLinkResolver();

    @Test
    void resolveArchiveWithResidualReserve() {
        Processo processo = new Processo();
        processo.setStatusProcesso(StatusProcesso.CUMPRIMENTO_SENTENCA);

        TerminalArchiveLinkProfile profile = resolver.resolve(processo, "arquivar", "baixa_parcial_com_saldo", "saldo residual", 60D, 2000D);

        assertEquals("ELEGIVEL_COM_RESERVA", profile.archiveEligibility());
        assertTrue(profile.archiveLinkMode().contains("RESERVA"));
    }

    @Test
    void resolveDesarquivamentoForArchivedCase() {
        Processo processo = new Processo();
        processo.setStatusProcesso(StatusProcesso.ARQUIVADO);

        TerminalArchiveLinkProfile profile = resolver.resolve(processo, "desarquivar", "baixa_frustrada", "localizacao de bens", 0D, 1000D);

        assertEquals("ELEGIVEL", profile.archiveEligibility());
        assertTrue(profile.archiveInbox().contains("reativacao"));
    }
}
