package com.tcc.pjb.backend.core.security.webauthn;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.security.TrustedDevice;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.model.repository.security.TrustedDeviceRepository;
import com.yubico.webauthn.CredentialRepository;
import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor;
import com.yubico.webauthn.data.PublicKeyCredentialType;

@Component
public class WebAuthnCredentialRepository implements CredentialRepository {

    private final TrustedDeviceRepository deviceRepo;
    private final UsuarioRepository usuarioRepo;

    public WebAuthnCredentialRepository(TrustedDeviceRepository deviceRepo, UsuarioRepository usuarioRepo) {
        this.deviceRepo = deviceRepo;
        this.usuarioRepo = usuarioRepo;
    }

    @Override
    public Set<PublicKeyCredentialDescriptor> getCredentialIdsForUsername(String username) {
        if (username == null || username.isBlank()) return Collections.emptySet();
        Usuario u = usuarioRepo.findByEmail(username).orElse(null);
        if (u == null) return Collections.emptySet();
        List<TrustedDevice> devices = deviceRepo.findActiveByUser(u.getId());
        return devices.stream()
                .filter(d -> d.getCredentialId() != null && !d.getCredentialId().isBlank())
                .map(TrustedDevice::getCredentialId)
                .map(WebAuthnCredentialRepository::safeBase64Url)
                .flatMap(Optional::stream)
                .map(id -> PublicKeyCredentialDescriptor.builder()
                        .id(id)
                        .type(PublicKeyCredentialType.PUBLIC_KEY)
                        .build())
                .collect(Collectors.toSet());
    }

    @Override
    public Optional<ByteArray> getUserHandleForUsername(String username) {
        if (username == null || username.isBlank()) return Optional.empty();
        Usuario u = usuarioRepo.findByEmail(username).orElse(null);
        if (u == null) return Optional.empty();
        return Optional.of(userHandleFor(u.getId()));
    }

    @Override
    public Optional<String> getUsernameForUserHandle(ByteArray userHandle) {
        if (userHandle == null) return Optional.empty();
        long id = longFromUserHandle(userHandle);
        if (id <= 0) return Optional.empty();
        Usuario u = usuarioRepo.findById(id).orElse(null);
        return u != null ? Optional.of(u.getEmail()) : Optional.empty();
    }

    @Override
    public Optional<RegisteredCredential> lookup(ByteArray credentialId, ByteArray userHandle) {
        if (credentialId == null || userHandle == null) return Optional.empty();
        long userId = longFromUserHandle(userHandle);
        if (userId <= 0) return Optional.empty();
        String cred = credentialId.getBase64Url();
        TrustedDevice d = deviceRepo.findByCredentialId(cred).orElse(null);
        if (d == null || d.getUsuario() == null || d.isRevogado()) return Optional.empty();
        if (!d.getUsuario().getId().equals(userId)) return Optional.empty();
        if (d.getPublicKey() == null || d.getPublicKey().isBlank()) return Optional.empty();
        ByteArray publicKey = safeBase64Url(d.getPublicKey()).orElse(null);
        if (publicKey == null) return Optional.empty();
        return Optional.of(RegisteredCredential.builder()
                .credentialId(credentialId)
                .userHandle(userHandle)
                .publicKeyCose(publicKey)
                .signatureCount(Math.max(0, d.getSignCount()))
                .build());
    }

    @Override
    public Set<RegisteredCredential> lookupAll(ByteArray credentialId) {
        if (credentialId == null) return Collections.emptySet();
        String cred = credentialId.getBase64Url();
        TrustedDevice d = deviceRepo.findByCredentialId(cred).orElse(null);
        if (d == null || d.getUsuario() == null || d.isRevogado()) return Collections.emptySet();
        if (d.getPublicKey() == null || d.getPublicKey().isBlank()) return Collections.emptySet();
        ByteArray uh = userHandleFor(d.getUsuario().getId());
        ByteArray publicKey = safeBase64Url(d.getPublicKey()).orElse(null);
        if (publicKey == null) return Collections.emptySet();
        return Set.of(RegisteredCredential.builder()
                .credentialId(credentialId)
                .userHandle(uh)
                .publicKeyCose(publicKey)
                .signatureCount(Math.max(0, d.getSignCount()))
                .build());
    }

    private static Optional<ByteArray> safeBase64Url(String value) {
        if (value == null || value.isBlank()) return Optional.empty();
        try {
            return Optional.of(ByteArray.fromBase64Url(value));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public static ByteArray userHandleFor(Long userId) {
        if (userId == null) return new ByteArray(new byte[0]);
        ByteBuffer buf = ByteBuffer.allocate(Long.BYTES);
        buf.putLong(userId);
        return new ByteArray(buf.array());
    }

    public static long longFromUserHandle(ByteArray userHandle) {
        try {
            byte[] b = userHandle.getBytes();
            if (b.length != Long.BYTES) return -1;
            return ByteBuffer.wrap(b).getLong();
        } catch (Exception e) {
            return -1;
        }
    }
}
