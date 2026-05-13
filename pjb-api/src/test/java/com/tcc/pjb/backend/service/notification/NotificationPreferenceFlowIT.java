package com.tcc.pjb.backend.service.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;

import com.tcc.pjb.backend.PjbIntegrationTestBase;
import com.tcc.pjb.backend.core.security.access.PrivateResourceAccessGuardService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.notification.UserNotificationPreference;
import com.tcc.pjb.backend.model.repository.UserNotificationPreferenceRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class NotificationPreferenceFlowIT extends PjbIntegrationTestBase {

    @Autowired
    private NotificationPreferenceService service;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UserNotificationPreferenceRepository preferenceRepository;

    @MockitoBean
    private PrivateResourceAccessGuardService accessGuard;

    @Test
    void deveProjetarPreferenciaPadraoQuandoUsuarioAindaNaoPossuirRegistroPersistido() {
        Usuario usuario = usuarioRepository.save(novoUsuario("notif.default@pjb.test", "11111111111"));
        doNothing().when(accessGuard).requireOwnerOrPrivileged(usuario.getId(), null, "preferência de notificação");

        NotificationPreferenceService.PreferenceView view = service.consultar(usuario.getId());

        assertThat(view.usuarioId()).isEqualTo(usuario.getId());
        assertThat(view.allowEmail()).isTrue();
        assertThat(view.allowPush()).isTrue();
        assertThat(view.allowWhatsapp()).isFalse();
        assertThat(view.allowDigest()).isTrue();
        assertThat(view.antiSpamWindowMinutes()).isEqualTo(30);
        assertThat(preferenceRepository.findByUsuario_Id(usuario.getId())).isEmpty();
    }

    @Test
    void devePersistirPreferenciaCustomizadaComJanelaECanaisDeclarados() {
        Usuario usuario = usuarioRepository.save(novoUsuario("notif.persist@pjb.test", "22222222222"));
        doNothing().when(accessGuard).requireOwnerOrPrivileged(usuario.getId(), null, "preferência de notificação");

        NotificationPreferenceService.PreferenceView view = service.salvar(usuario.getId(), new NotificationPreferenceService.PreferenceRequest(
                true,
                false,
                true,
                false,
                true,
                false,
                true,
                45,
                "https://push.pjb.test/devices/abc",
                "+5585999999999",
                null,
                "https://hooks.pjb.test/intimacoes"
        ));

        UserNotificationPreference persisted = preferenceRepository.findByUsuario_Id(usuario.getId()).orElseThrow();
        assertThat(view.usuarioId()).isEqualTo(usuario.getId());
        assertThat(view.allowPush()).isFalse();
        assertThat(view.allowWhatsapp()).isTrue();
        assertThat(view.allowWebhook()).isTrue();
        assertThat(view.onlyHighPriority()).isTrue();
        assertThat(view.antiSpamWindowMinutes()).isEqualTo(45);
        assertThat(persisted.isAllowEmail()).isTrue();
        assertThat(persisted.isAllowPush()).isFalse();
        assertThat(persisted.isAllowWhatsapp()).isTrue();
        assertThat(persisted.isAllowWebhook()).isTrue();
        assertThat(persisted.isAllowDigest()).isFalse();
        assertThat(persisted.isOnlyHighPriority()).isTrue();
        assertThat(persisted.getPushEndpoint()).isEqualTo("https://push.pjb.test/devices/abc");
        assertThat(persisted.getWebhookUrl()).isEqualTo("https://hooks.pjb.test/intimacoes");
        assertThat(persisted.getWhatsappNumber()).isEqualTo("+5585999999999");
        assertThat(persisted.isAtivo()).isTrue();
    }

    private static Usuario novoUsuario(String email, String cpf) {
        Usuario usuario = new Usuario();
        usuario.setNome("Usuário de Notificação");
        usuario.setEmail(email);
        usuario.setCpf(cpf);
        usuario.setTipoUsuario(TipoUsuario.ADVOGADO);
        usuario.setAtivo(true);
        usuario.setUf("CE");
        usuario.setComarca("Fortaleza");
        usuario.setPerfil("ADVOGADO");
        return usuario;
    }
}
