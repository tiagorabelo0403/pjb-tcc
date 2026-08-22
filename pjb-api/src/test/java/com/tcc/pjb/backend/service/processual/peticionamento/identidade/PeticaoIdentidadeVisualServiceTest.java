package com.tcc.pjb.backend.service.processual.peticionamento.identidade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.AccessDeniedPjbException;
import com.tcc.pjb.backend.core.storage.ObjectStoragePort;
import com.tcc.pjb.backend.core.storage.ObjectWriteResult;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.identidade.IdentidadeVisualRequest;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.identidade.IdentidadeVisualResponse;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.peticionamento.PeticaoIdentidadeVisual;
import com.tcc.pjb.backend.model.repository.PeticaoIdentidadeVisualRepository;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PeticaoIdentidadeVisualServiceTest {

    private PeticaoIdentidadeVisualRepository repository;
    private ObjectStoragePort storage;
    private CurrentUserService currentUserService;
    private PeticaoIdentidadeVisualService service;

    @BeforeEach
    void setUp() {
        repository = mock(PeticaoIdentidadeVisualRepository.class);
        storage = mock(ObjectStoragePort.class);
        currentUserService = mock(CurrentUserService.class);
        service = new PeticaoIdentidadeVisualService(repository, storage, currentUserService);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private void authenticateAs(TipoUsuario tipo) {
        Usuario usuario = new Usuario();
        usuario.setId(42L);
        usuario.setTipoUsuario(tipo);
        when(currentUserService.getRequired()).thenReturn(usuario);
    }

    @Test
    void naoPeticionanteEhBloqueado() {
        authenticateAs(TipoUsuario.CIDADAO);
        assertThatThrownBy(() -> service.obterMinha()).isInstanceOf(AccessDeniedPjbException.class);
    }

    @Test
    void obterMinhaSemPerfilRetornaVazia() {
        authenticateAs(TipoUsuario.ADVOGADO);
        when(repository.findByUsuarioId(42L)).thenReturn(Optional.empty());

        IdentidadeVisualResponse resp = service.obterMinha();

        assertThat(resp.temLogo()).isFalse();
        assertThat(resp.escopo()).isEqualTo("INDIVIDUAL");
        assertThat(resp.nomeExibicao()).isNull();
    }

    @Test
    void salvarMinhaCriaPerfilComTextoECores() {
        authenticateAs(TipoUsuario.ADVOGADO);
        when(repository.findByUsuarioId(42L)).thenReturn(Optional.empty());
        IdentidadeVisualRequest req = new IdentidadeVisualRequest(
                "Dra. Fulana Advocacia", "Fulana & Associados", "Cabecalho", "Rodape",
                "#0A3D62", "#B71540", false, true);

        IdentidadeVisualResponse resp = service.salvarMinha(req);

        assertThat(resp.nomeExibicao()).isEqualTo("Dra. Fulana Advocacia");
        assertThat(resp.paletaPrimaria()).isEqualTo("#0A3D62");
        assertThat(resp.exibirRegistroProfissional()).isFalse();
        assertThat(resp.temLogo()).isFalse();
    }

    @Test
    void uploadLogoValidoArmazenaNoObjectStorageNaoNoBanco() throws Exception {
        authenticateAs(TipoUsuario.ADVOGADO);
        when(repository.findByUsuarioId(42L)).thenReturn(Optional.empty());
        when(storage.put(any(), any(), anyLong(), eq("image/png"), any()))
                .thenReturn(new ObjectWriteResult("k", URI.create("http://x/k"), 100L, "sha256x", "sha384x"));
        byte[] png = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 1, 2, 3, 4};

        IdentidadeVisualResponse resp = service.uploadLogo(png, "image/png");

        assertThat(resp.temLogo()).isTrue();
        assertThat(resp.logoUrl()).isEqualTo(IdentidadeVisualResponse.LOGO_URL);
        assertThat(resp.logoSha256()).isEqualTo("sha256x");
    }

    @Test
    void uploadLogoAcimaDoLimiteEhRejeitado() {
        authenticateAs(TipoUsuario.ADVOGADO);
        byte[] big = new byte[2_000_001];
        big[0] = (byte) 0x89;
        assertThatThrownBy(() -> service.uploadLogo(big, "image/png"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("logo_too_large");
    }

    @Test
    void uploadLogoConteudoNaoImagemEhRejeitado() {
        authenticateAs(TipoUsuario.ADVOGADO);
        assertThatThrownBy(() -> service.uploadLogo("nao e imagem".getBytes(), "text/plain"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void resolvePresetRetornaFormaConsumidaPelaSessao() {
        PeticaoIdentidadeVisual entity = new PeticaoIdentidadeVisual(42L);
        entity.setNomeExibicao("Dra. Fulana");
        entity.setPaletaPrimaria("#0A3D62");
        entity.aplicarLogo("peticao-identidade/user/42/x.png", "image/png", 100L, "sha256x");
        when(repository.findByUsuarioId(42L)).thenReturn(Optional.of(entity));

        Optional<Map<String, Object>> preset = service.resolvePreset(42L);

        assertThat(preset).isPresent();
        assertThat(preset.get()).containsEntry("nomeExibicao", "Dra. Fulana");
        assertThat(preset.get()).containsEntry("brasaoOuLogomarcaUri", IdentidadeVisualResponse.LOGO_URL);
        assertThat(preset.get()).containsEntry("origem", "PERFIL_SALVO");
    }

    @Test
    void resolvePresetSemPerfilRetornaVazio() {
        when(repository.findByUsuarioId(99L)).thenReturn(Optional.empty());
        assertThat(service.resolvePreset(99L)).isEmpty();
        assertThat(service.resolvePreset(null)).isEmpty();
    }

    @Test
    void removerLogoLimpaCamposDeLogo() {
        authenticateAs(TipoUsuario.DEFENSOR_PUBLICO);
        PeticaoIdentidadeVisual entity = new PeticaoIdentidadeVisual(42L);
        entity.aplicarLogo("k", "image/png", 100L, "sha256x");
        when(repository.findByUsuarioId(42L)).thenReturn(Optional.of(entity));

        IdentidadeVisualResponse resp = service.removerLogo();

        assertThat(resp.temLogo()).isFalse();
    }
}
