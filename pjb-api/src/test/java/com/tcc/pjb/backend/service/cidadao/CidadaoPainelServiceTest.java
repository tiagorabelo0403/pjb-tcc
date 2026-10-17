package com.tcc.pjb.backend.service.cidadao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.integration.govbr.oidc.GovBrOidcProperties;
import com.tcc.pjb.backend.model.dto.cidadao.CidadaoPainelBootstrapResponse;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.cidadao.CidadaoDashboardSnapshot;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.UsuarioAvatarRepository;
import com.tcc.pjb.backend.repository.cidadao.CidadaoDashboardSnapshotRepository;
import com.tcc.pjb.backend.service.cidadao.dashboard.CidadaoDashboardSnapshotWriteService;
import com.tcc.pjb.backend.service.outbox.OutboxPublisher;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CidadaoPainelServiceTest {

    private static final long USUARIO_ID = 987654L;

    private final CurrentUserService currentUser = mock(CurrentUserService.class);
    private final CidadaoDashboardSnapshotRepository snapshotRepo = mock(CidadaoDashboardSnapshotRepository.class);
    private final Instant atualizadoEm = Instant.now();
    private CidadaoPainelService service;

    @BeforeEach
    void setUp() {
        Usuario usuario = new Usuario();
        usuario.setId(USUARIO_ID);
        usuario.setNome("Maria da Silva");
        usuario.setCpf("11122233344");
        usuario.setTipoUsuario(TipoUsuario.CIDADAO);
        when(currentUser.getRequired()).thenReturn(usuario);
        service = new CidadaoPainelService(currentUser, snapshotRepo,
                mock(CidadaoDashboardSnapshotWriteService.class), mock(OutboxPublisher.class),
                new ObjectMapper(), mock(UsuarioAvatarRepository.class), mock(GovBrOidcProperties.class));
        snapshot("[]");
    }

    private void snapshot(String pendenciasJson) {
        CidadaoDashboardSnapshot snap = CidadaoDashboardSnapshot.builder()
                .cidadaoUserId(USUARIO_ID)
                .updatedAt(atualizadoEm)
                .badgesJson("{}")
                .widgetsJson("[]")
                .pendenciasJson(pendenciasJson)
                .proximosEventosJson("[]")
                .recentesJson("[]")
                .govHubJson("{}")
                .build();
        when(snapshotRepo.findById(USUARIO_ID)).thenReturn(Optional.of(snap));
    }

    @Test
    void etagDoPainelNaoExpoeOIdInternoDoUsuario() {
        String etag = service.painelBootstrap().etag();

        assertThat(etag).startsWith("W/\"cidp-").doesNotContain(String.valueOf(USUARIO_ID));
    }

    @Test
    void etagEstavelParaOMesmoConteudoDoPainel() {
        assertThat(service.painelBootstrap().etag()).isEqualTo(service.painelBootstrap().etag());
    }

    @Test
    void etagMudaQuandoOConteudoDoPainelMuda() {
        String antes = service.painelBootstrap().etag();
        snapshot("[{\"titulo\":\"Nova pendência\"}]");

        assertThat(service.painelBootstrap().etag()).isNotEqualTo(antes);
    }

    @Test
    void snapshotVazioDevolveGovHubComUfPadraoECategoriasVazias() {
        CidadaoPainelBootstrapResponse painel = service.painelBootstrap();

        assertThat(painel.govHub().categories()).isNotNull().isEmpty();
        assertThat(painel.govHub().uf()).isEqualTo("BR");
    }
}
