package com.tcc.pjb.backend.core.icp;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.model.entity.Usuario;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class RecursalIcpBrasilMetadataAssemblerTest {

    private final RecursalIcpBrasilMetadataAssembler assembler = new RecursalIcpBrasilMetadataAssembler();

    @Test
    void deveMontarMetadataValidadoSemNulos() {
        Usuario usuario = new Usuario();
        usuario.setId(99L);
        IcpBrasilCertProfile profile = new IcpBrasilCertProfile("sub", "iss", "abc", "123", null, "nome", "A1", "AC-JUS", Instant.now(), Instant.now().plusSeconds(60));
        var metadata = assembler.validated("LTA", "LTA", profile, usuario).toMap();
        assertThat(metadata.get("validationOk")).isEqualTo(true);
        assertThat(metadata.get("serialHex")).isEqualTo("abc");
        assertThat(metadata.get("signerUserId")).isEqualTo(99L);
        assertThat(metadata).doesNotContainKey("failureReason");
        assertThat(metadata).doesNotContainKey("cpfTitular");
        assertThat(metadata).doesNotContainKey("cnpjTitular");
    }
}
