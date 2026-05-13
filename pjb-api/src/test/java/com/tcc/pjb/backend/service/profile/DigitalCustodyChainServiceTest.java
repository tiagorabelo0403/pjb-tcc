package com.tcc.pjb.backend.service.profile;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.profile.ChainOfCustodySealRequest;
import com.tcc.pjb.backend.model.dto.profile.ChainOfCustodySealResponse;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class DigitalCustodyChainServiceTest {

    @Test
    void selaLoteEPersisteEmLedger() {
        CurrentUserService currentUserService = Mockito.mock(CurrentUserService.class);
        DigitalCustodyChainLedgerService ledgerService = Mockito.mock(DigitalCustodyChainLedgerService.class);
        DigitalCustodyChainService service = new DigitalCustodyChainService(currentUserService, ledgerService, java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor());
        when(currentUserService.getRequired()).thenReturn(usuario());
        when(ledgerService.persist(any(), any(), any(), eq(TipoUsuario.DELEGADO_POLICIA.name()), eq(99L), any(), any())).thenAnswer(inv -> {
            List<ChainOfCustodySealResponse.SealedEvidence> evidencias = inv.getArgument(6);
            return new ChainOfCustodySealResponse(inv.getArgument(0), inv.getArgument(1), inv.getArgument(2), inv.getArgument(3), inv.getArgument(5), evidencias);
        });
        String payload = Base64.getEncoder().encodeToString("prova-digital".getBytes(StandardCharsets.UTF_8));

        var response = service.seal(new ChainOfCustodySealRequest(
                List.of(new ChainOfCustodySealRequest.EvidenceItem("ev-1", "foto.png", payload, null, java.util.Map.of("origem", "mobile"))),
                "L-2026-001"
        ));

        ArgumentCaptor<String> digestCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(ledgerService).persist(eq("L-2026-001"), digestCaptor.capture(), any(), eq(TipoUsuario.DELEGADO_POLICIA.name()), eq(99L), any(), any());
        assertThat(digestCaptor.getValue()).hasSize(64);
        assertThat(response.chaveCustodia()).startsWith("CST-");
        assertThat(response.evidencias()).hasSize(1);
    }

    @Test
    void rejeitaDigestExternoDivergente() {
        CurrentUserService currentUserService = Mockito.mock(CurrentUserService.class);
        DigitalCustodyChainLedgerService ledgerService = Mockito.mock(DigitalCustodyChainLedgerService.class);
        DigitalCustodyChainService service = new DigitalCustodyChainService(currentUserService, ledgerService, java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor());
        when(currentUserService.getRequired()).thenReturn(usuario());
        String payload = Base64.getEncoder().encodeToString("abc".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> service.seal(new ChainOfCustodySealRequest(
                List.of(new ChainOfCustodySealRequest.EvidenceItem("ev-1", "arquivo.bin", payload, sha256("xyz"), java.util.Map.of())),
                "L-2026-002"
        ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("digest_externo_divergente");
    }

    private static Usuario usuario() {
        Usuario usuario = new Usuario();
        usuario.setId(99L);
        usuario.setTipoUsuario(TipoUsuario.DELEGADO_POLICIA);
        usuario.setPerfil(TipoUsuario.DELEGADO_POLICIA.name());
        usuario.setCpf("12345678901");
        usuario.setEmail("delegado@pjb.test");
        usuario.setSenha("x");
        return usuario;
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
