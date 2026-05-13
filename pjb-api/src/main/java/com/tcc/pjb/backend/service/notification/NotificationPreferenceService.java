package com.tcc.pjb.backend.service.notification;

import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.security.access.PrivateResourceAccessGuardService;
import com.tcc.pjb.backend.platform.runtime.PjbTransactionalBudget;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.notification.UserNotificationPreference;
import com.tcc.pjb.backend.model.repository.UserNotificationPreferenceRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;

@Service
public class NotificationPreferenceService {

    private final UserNotificationPreferenceRepository repository;
    private final UsuarioRepository usuarioRepository;
    private final PrivateResourceAccessGuardService accessGuard;

    public NotificationPreferenceService(UserNotificationPreferenceRepository repository,
                                         UsuarioRepository usuarioRepository,
                                         PrivateResourceAccessGuardService accessGuard) {
        this.repository = Objects.requireNonNull(repository);
        this.usuarioRepository = Objects.requireNonNull(usuarioRepository);
        this.accessGuard = Objects.requireNonNull(accessGuard);
    }

    @Transactional(readOnly = true)
    @PjbTransactionalBudget(operation = "notification.preference.read", maxMillis = 800, critical = false)
    public PreferenceView consultar(Long usuarioId) {
        Usuario usuario = requireUsuario(usuarioId);
        accessGuard.requireOwnerOrPrivileged(usuario.getId(), null, "preferência de notificação");
        UserNotificationPreference preference = repository.findByUsuario_Id(usuarioId).orElseGet(() -> defaultPreference(usuario));
        return toView(preference);
    }

    @Transactional
    @PjbTransactionalBudget(operation = "notification.preference.persist", maxMillis = 1200, critical = true)
    public PreferenceView salvar(Long usuarioId, PreferenceRequest request) {
        Usuario usuario = requireUsuario(usuarioId);
        accessGuard.requireOwnerOrPrivileged(usuario.getId(), null, "preferência de notificação");
        UserNotificationPreference preference = repository.findByUsuario_Id(usuarioId).orElseGet(() -> defaultPreference(usuario));
        preference.setUsuario(usuario);
        preference.setAllowEmail(Boolean.TRUE.equals(request.allowEmail()));
        preference.setAllowPush(Boolean.TRUE.equals(request.allowPush()));
        preference.setAllowWhatsapp(Boolean.TRUE.equals(request.allowWhatsapp()));
        preference.setAllowArDigital(Boolean.TRUE.equals(request.allowArDigital()));
        preference.setAllowWebhook(Boolean.TRUE.equals(request.allowWebhook()));
        preference.setAllowDigest(Boolean.TRUE.equals(request.allowDigest()));
        preference.setOnlyHighPriority(Boolean.TRUE.equals(request.onlyHighPriority()));
        preference.setAntiSpamWindowMinutes(request.antiSpamWindowMinutes());
        preference.setPushEndpoint(request.pushEndpoint());
        preference.setWhatsappNumber(request.whatsappNumber());
        preference.setArDigitalAddress(request.arDigitalAddress());
        preference.setWebhookUrl(request.webhookUrl());
        return toView(repository.save(preference));
    }

    private Usuario requireUsuario(Long usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario", usuarioId));
    }

    private UserNotificationPreference defaultPreference(Usuario usuario) {
        UserNotificationPreference preference = new UserNotificationPreference();
        preference.setUsuario(usuario);
        preference.setAllowEmail(true);
        preference.setAllowPush(true);
        preference.setAllowWhatsapp(false);
        preference.setAllowArDigital(false);
        preference.setAllowWebhook(false);
        preference.setAllowDigest(true);
        preference.setOnlyHighPriority(false);
        preference.setAntiSpamWindowMinutes(30);
        return preference;
    }

    private PreferenceView toView(UserNotificationPreference preference) {
        return new PreferenceView(
                preference.getUsuario() != null ? preference.getUsuario().getId() : null,
                preference.isAllowEmail(),
                preference.isAllowPush(),
                preference.isAllowWhatsapp(),
                preference.isAllowArDigital(),
                preference.isAllowWebhook(),
                preference.isAllowDigest(),
                preference.isOnlyHighPriority(),
                preference.getAntiSpamWindowMinutes(),
                preference.getPushEndpoint(),
                preference.getWhatsappNumber(),
                preference.getArDigitalAddress(),
                preference.getWebhookUrl()
        );
    }

    public record PreferenceRequest(
            Boolean allowEmail,
            Boolean allowPush,
            Boolean allowWhatsapp,
            Boolean allowArDigital,
            Boolean allowWebhook,
            Boolean allowDigest,
            Boolean onlyHighPriority,
            Integer antiSpamWindowMinutes,
            String pushEndpoint,
            String whatsappNumber,
            String arDigitalAddress,
            String webhookUrl
    ) {
    }

    public record PreferenceView(
            Long usuarioId,
            boolean allowEmail,
            boolean allowPush,
            boolean allowWhatsapp,
            boolean allowArDigital,
            boolean allowWebhook,
            boolean allowDigest,
            boolean onlyHighPriority,
            Integer antiSpamWindowMinutes,
            String pushEndpoint,
            String whatsappNumber,
            String arDigitalAddress,
            String webhookUrl
    ) {
    }
}
