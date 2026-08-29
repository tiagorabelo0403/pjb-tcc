package com.tcc.pjb.backend.core.dje;

import com.tcc.pjb.backend.core.dje.domain.DjePartesNotificacaoResult;

import static org.assertj.core.api.Assertions.assertThat;
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

        DjePartesNotificacaoResult esperado = DjePartesNotificacaoResult.success(10L, "mock");
        when(port.notificar(publicacao)).thenReturn(esperado);

        DjePartesNotificacaoResult result = service.notificarPartes(publicacao);

        assertThat(result).isEqualTo(esperado);
        verify(port).notificar(publicacao);
        verify(auditLedgerService).appendSafely(
                "DJE_NOTIFICACAO_PARTES", "PROCESSO", "20",
                "djeId=10 tipoAto=SENTENCA success=true");
    }
}
