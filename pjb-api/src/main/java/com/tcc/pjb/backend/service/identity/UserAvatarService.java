package com.tcc.pjb.backend.service.identity;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.storage.ObjectReadResult;
import com.tcc.pjb.backend.core.storage.ObjectStoragePort;
import com.tcc.pjb.backend.core.storage.ObjectWriteResult;
import com.tcc.pjb.backend.model.entity.identity.UsuarioAvatar;
import com.tcc.pjb.backend.model.repository.UsuarioAvatarRepository;

@Service
public class UserAvatarService {

  private final UsuarioAvatarRepository avatarRepo;
  private final ObjectStoragePort storage;

  public UserAvatarService(UsuarioAvatarRepository avatarRepo, ObjectStoragePort storage) {
    this.avatarRepo = avatarRepo;
    this.storage = storage;
  }

  public Optional<UsuarioAvatar> find(Long usuarioId) {
    return avatarRepo.findByUsuarioId(usuarioId);
  }

  @Transactional
  public UsuarioAvatar upsert(Long usuarioId, byte[] bytes, String contentType, String source) throws IOException {
    if (bytes == null || bytes.length == 0) {
      throw new IllegalArgumentException("avatar_empty");
    }
    if (bytes.length > 2_000_000) {
      throw new IllegalArgumentException("avatar_too_large");
    }

    String ct = normalizeContentType(bytes, contentType);
    String ext = ct.equals("image/png") ? "png" : "jpg";
    String key = "avatars/user/" + usuarioId + "/" + Instant.now().toEpochMilli() + "-" + UUID.randomUUID() + "." + ext;

    ObjectWriteResult wr = storage.put(
        key,
        new ByteArrayInputStream(bytes),
        bytes.length,
        ct,
        Map.of(
            "usuarioId", String.valueOf(usuarioId),
            "source", source
        )
    );

    Instant now = Instant.now();

    UsuarioAvatar current = avatarRepo.findByUsuarioId(usuarioId).orElse(null);
    if (current == null) {
      UsuarioAvatar created = new UsuarioAvatar(
          usuarioId,
          wr.key(),
          ct,
          wr.sizeBytes(),
          wr.sha256(),
          source,
          now
      );
      return avatarRepo.save(created);
    }

    current.update(wr.key(), ct, wr.sizeBytes(), wr.sha256(), source, now);
    return avatarRepo.save(current);
  }

  public ObjectReadResult read(Long usuarioId) throws IOException {
    UsuarioAvatar a = avatarRepo.findByUsuarioId(usuarioId)
        .orElseThrow(() -> new IllegalStateException("avatar_not_found"));
    return storage.get(a.getStorageKey());
  }

  private static String normalizeContentType(byte[] bytes, String provided) {
    String p = provided != null ? provided.trim().toLowerCase() : "";
    if (p.equals("image/jpeg") || p.equals("image/jpg")) return "image/jpeg";
    if (p.equals("image/png")) return "image/png";

    if (bytes.length >= 4) {
      if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8) return "image/jpeg";
      if ((bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47) return "image/png";
    }

    return "image/jpeg";
  }
}
