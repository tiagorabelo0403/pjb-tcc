package com.tcc.pjb.backend.service.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.crypto.KeyMaterialService;
import com.tcc.pjb.backend.model.dto.profile.ChainOfCustodySealRequest;
import com.tcc.pjb.backend.model.dto.profile.ChainOfCustodySyncExportRequest;
import com.tcc.pjb.backend.model.dto.profile.ChainOfCustodySyncReplayRequest;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.pericia.CadeiaCustodiaDigitalSyncEvent;
import com.tcc.pjb.backend.model.repository.CadeiaCustodiaDigitalSyncEventRepository;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;

class DigitalCustodyChainSyncServiceTest {

    @Test
    void exportaBundleAssinadoDaCadeiaDeCustodia() {
        CurrentUserService currentUserService = Mockito.mock(CurrentUserService.class);
        KeyMaterialService keyMaterialService = Mockito.mock(KeyMaterialService.class);
        DigitalCustodyChainLedgerService ledgerService = Mockito.mock(DigitalCustodyChainLedgerService.class);
        CadeiaCustodiaDigitalSyncEventRepository repository = Mockito.mock(CadeiaCustodiaDigitalSyncEventRepository.class);
        ObjectProvider<jakarta.servlet.http.HttpServletRequest> provider = Mockito.mock(ObjectProvider.class);
        DigitalCustodyChainSyncService service = new DigitalCustodyChainSyncService(currentUserService, keyMaterialService, ledgerService, repository, provider);
        when(currentUserService.getRequired()).thenReturn(usuario());
        when(keyMaterialService.getCustodyMeshSigningKey()).thenReturn(new javax.crypto.spec.SecretKeySpec(new byte[32], "HmacSHA256"));
        when(ledgerService.ledger("CST-123")).thenReturn(ledger());
        when(repository.save(Mockito.any())).thenAnswer(inv -> inv.getArgument(0));

        var response = service.exportBundle("CST-123", new ChainOfCustodySyncExportRequest("MP", "PJB-DELEGADO", "nonce-1", "cooperacao"));

        ArgumentCaptor<CadeiaCustodiaDigitalSyncEvent> captor = ArgumentCaptor.forClass(CadeiaCustodiaDigitalSyncEvent.class);
        Mockito.verify(repository).save(captor.capture());
        assertThat(response.assinaturaHmacSha256()).hasSize(64);
        assertThat(response.payloadDigestSha256()).hasSize(64);
        assertThat(captor.getValue().getDirecao().name()).isEqualTo("OUTBOUND");
    }

    @Test
    void verificaReplayContraLedgerLocal() {
        CurrentUserService currentUserService = Mockito.mock(CurrentUserService.class);
        KeyMaterialService keyMaterialService = Mockito.mock(KeyMaterialService.class);
        DigitalCustodyChainLedgerService ledgerService = Mockito.mock(DigitalCustodyChainLedgerService.class);
        CadeiaCustodiaDigitalSyncEventRepository repository = Mockito.mock(CadeiaCustodiaDigitalSyncEventRepository.class);
        ObjectProvider<jakarta.servlet.http.HttpServletRequest> provider = Mockito.mock(ObjectProvider.class);
        DigitalCustodyChainSyncService service = new DigitalCustodyChainSyncService(currentUserService, keyMaterialService, ledgerService, repository, provider);
        when(currentUserService.getRequired()).thenReturn(usuario());
        when(keyMaterialService.getCustodyMeshSigningKey()).thenReturn(new javax.crypto.spec.SecretKeySpec(new byte[32], "HmacSHA256"));
        when(ledgerService.findLedger("CST-123")).thenReturn(java.util.Optional.of(ledger()));
        when(repository.save(Mockito.any())).thenAnswer(inv -> inv.getArgument(0));

        var exported = service.exportBundle("CST-123", new ChainOfCustodySyncExportRequest("MP", "PJB-DELEGADO", "nonce-1", "cooperacao"));
        var replay = service.replayVerify(new ChainOfCustodySyncReplayRequest(
                exported.parceiroInstitucional(),
                exported.noOrigem(),
                exported.nonce(),
                exported.chaveCustodia(),
                exported.digestColecaoSha256(),
                exported.payloadDigestSha256(),
                exported.assinaturaHmacSha256(),
                exported.entradas().stream().map(entry -> new ChainOfCustodySyncReplayRequest.SyncLedgerEntry(
                        entry.ordemLote(),
                        entry.evidenceId(),
                        entry.evidenceNome(),
                        entry.digestSha256(),
                        entry.evidenceChaveCustodia(),
                        entry.metadataDigestSha256(),
                        entry.metadadosCanonicos(),
                        entry.prevHash(),
                        entry.entryHash(),
                        entry.sealedAt(),
                        entry.requestId()
                )).toList()
        ));

        assertThat(replay.assinaturaOk()).isTrue();
        assertThat(replay.digestOk()).isTrue();
        assertThat(replay.correspondenciaLocalOk()).isTrue();
    }

    @Test
    void verificaReplaySimetricoParaChaveCustodiaDe64Caracteres() throws java.security.NoSuchAlgorithmException {
        CurrentUserService currentUserService = Mockito.mock(CurrentUserService.class);
        KeyMaterialService keyMaterialService = Mockito.mock(KeyMaterialService.class);
        DigitalCustodyChainLedgerService ledgerService = Mockito.mock(DigitalCustodyChainLedgerService.class);
        CadeiaCustodiaDigitalSyncEventRepository repository = Mockito.mock(CadeiaCustodiaDigitalSyncEventRepository.class);
        ObjectProvider<jakarta.servlet.http.HttpServletRequest> provider = Mockito.mock(ObjectProvider.class);
        DigitalCustodyChainSyncService service = new DigitalCustodyChainSyncService(currentUserService, keyMaterialService, ledgerService, repository, provider);
        when(currentUserService.getRequired()).thenReturn(usuario());
        when(keyMaterialService.getCustodyMeshSigningKey()).thenReturn(new javax.crypto.spec.SecretKeySpec(new byte[32], "HmacSHA256"));

        String chaveCustodia64 = java.util.HexFormat.of().formatHex(
                java.security.MessageDigest.getInstance("SHA-256").digest("evidencia-real-64-chars".getBytes(StandardCharsets.UTF_8)));
        assertThat(chaveCustodia64).hasSize(64);
        var ledgerCom64 = ledger(chaveCustodia64);
        when(ledgerService.findLedger("CST-123")).thenReturn(java.util.Optional.of(ledgerCom64));
        when(repository.save(Mockito.any())).thenAnswer(inv -> inv.getArgument(0));

        var exported = service.exportBundle("CST-123", new ChainOfCustodySyncExportRequest("MP", "PJB-DELEGADO", "nonce-1", "cooperacao"));
        assertThat(exported.entradas()).extracting("evidenceChaveCustodia").containsExactly(chaveCustodia64);

        var replay = service.replayVerify(new ChainOfCustodySyncReplayRequest(
                exported.parceiroInstitucional(),
                exported.noOrigem(),
                exported.nonce(),
                exported.chaveCustodia(),
                exported.digestColecaoSha256(),
                exported.payloadDigestSha256(),
                exported.assinaturaHmacSha256(),
                exported.entradas().stream().map(entry -> new ChainOfCustodySyncReplayRequest.SyncLedgerEntry(
                        entry.ordemLote(),
                        entry.evidenceId(),
                        entry.evidenceNome(),
                        entry.digestSha256(),
                        entry.evidenceChaveCustodia(),
                        entry.metadataDigestSha256(),
                        entry.metadadosCanonicos(),
                        entry.prevHash(),
                        entry.entryHash(),
                        entry.sealedAt(),
                        entry.requestId()
                )).toList()
        ));

        assertThat(replay.assinaturaOk()).isTrue();
        assertThat(replay.digestOk()).isTrue();
        assertThat(replay.correspondenciaLocalOk()).isTrue();
    }

    private static com.tcc.pjb.backend.model.dto.profile.ChainOfCustodyLedgerResponse ledger() {
        return ledger("EVD-AAAAAAAAAAAAAAAAAAAA");
    }

    private static com.tcc.pjb.backend.model.dto.profile.ChainOfCustodyLedgerResponse ledger(String evidenceChaveCustodia) {
        return new com.tcc.pjb.backend.model.dto.profile.ChainOfCustodyLedgerResponse(
                "L-1",
                "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
                "CST-123",
                TipoUsuario.DELEGADO_POLICIA.name(),
                Instant.parse("2026-03-11T12:00:00Z"),
                1,
                true,
                List.of(new com.tcc.pjb.backend.model.dto.profile.ChainOfCustodyLedgerResponse.LedgerEntry(
                        1,
                        "ev-1",
                        "arquivo.bin",
                        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                        evidenceChaveCustodia,
                        "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
                        List.of("origem=mobile"),
                        null,
                        "cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc",
                        Instant.parse("2026-03-11T12:00:00Z"),
                        "req-1"
                ))
        );
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
}
