package com.tcc.pjb.backend.service.intelligence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.model.dto.intelligence.PessoaLocalizacaoRequest;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class PessoaLocalizacaoProviderRegistryTest {

    @Test
    void deveMesclarProvedoresEEliminarDuplicidadeDeEndereco() {
        PessoaLocalizacaoService.EnderecoPessoaRegistryClient a = (cpf, executor, request, processo) -> new PessoaLocalizacaoService.SnapshotEndereco(
                true,
                true,
                List.of(
                        new PessoaLocalizacaoService.EnderecoInfo("A", "RESIDENCIAL", "Rua A", "Centro", "Fortaleza", "CE", "60000-000", true, false, 0.70d, Instant.parse("2026-03-11T12:00:00Z")),
                        new PessoaLocalizacaoService.EnderecoInfo("A", "RESIDENCIAL", "Rua A", "Centro", "Fortaleza", "CE", "60000-000", true, false, 0.60d, Instant.parse("2026-03-11T12:01:00Z"))
                ),
                List.of("A_OK")
        );
        PessoaLocalizacaoService.EnderecoPessoaRegistryClient b = (cpf, executor, request, processo) -> new PessoaLocalizacaoService.SnapshotEndereco(
                true,
                false,
                List.of(new PessoaLocalizacaoService.EnderecoInfo("B", "RESIDENCIAL", "Rua B", "Aldeota", "Fortaleza", "CE", "60100-000", false, false, 0.95d, Instant.parse("2026-03-11T12:02:00Z"))),
                List.of("B_OK")
        );

        PessoaLocalizacaoProviderRegistry registry = new PessoaLocalizacaoProviderRegistry(List.of(a, b), List.of(), List.of());
        PessoaLocalizacaoService.SnapshotEndereco snapshot = registry.consultarEnderecos("12345678909", usuario(), request(), null);

        assertTrue(snapshot.enabled());
        assertTrue(snapshot.realtime());
        assertEquals(2, snapshot.enderecos().size());
        assertTrue(snapshot.highlights().contains("A_OK"));
        assertTrue(snapshot.highlights().contains("B_OK"));
    }

    private static Usuario usuario() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNome("Oficial");
        usuario.setEmail("oficial@pjb.test");
        usuario.setCpf("12345678909");
        usuario.setSenha("x");
        usuario.setTipoUsuario(TipoUsuario.OFICIAL_JUSTICA);
        usuario.syncPerfilETipoUsuario();
        return usuario;
    }

    private static PessoaLocalizacaoRequest request() {
        return new PessoaLocalizacaoRequest("12345678909", 1L, null, null, "teste", "justificativa", "PROC-1", true, true, true, false);
    }
}
