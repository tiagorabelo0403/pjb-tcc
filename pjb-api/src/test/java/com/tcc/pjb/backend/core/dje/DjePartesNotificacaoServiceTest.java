package com.tcc.pjb.backend.core.dje;

import com.tcc.pjb.backend.core.dje.domain.DjePartesNotificacaoResult;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.model.entity.judicial.DjePublicacao;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DjePartesNotificacaoServiceTest {

    @Test
    void deveDelegarParaPortaDeNotificacao() {
        DjePartesNotificacaoPort port = Mockito.mock(DjePartesNotificacaoPort.class);
        AuditLedgerService auditLedgerService = Mockito.mock(AuditLedgerService.class);
        DjePartesNotificacaoService service = new DjePartesNotificacaoService(port, auditLedgerService);
        DjePublicacao publicacao = DjePublicacao.builder()
                .id(10L)
                .processoId(20L)
                .tipoAto("SENTENCA")
                .status("PUBLICADO")
                .createdAt(Instant.now())
                .build();

        when(port.notificar(publicacao)).thenReturn(DjePartesNotificacaoResult.success(10L, "mock"));
        service.notificarPartes(publicacao);

        verify(port).notificar(publicacao);
    }
}
