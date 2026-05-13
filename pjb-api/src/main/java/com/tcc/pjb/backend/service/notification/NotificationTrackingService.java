package com.tcc.pjb.backend.service.notification;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.entity.NotificationHistory;
import com.tcc.pjb.backend.model.repository.NotificationHistoryRepository;

@Service
public class NotificationTrackingService {

    private static final byte[] PIXEL_BYTES = Base64.getDecoder().decode("R0lGODlhAQABAIABAP///wAAACwAAAAAAQABAAACAkQBADs=");

    private final NotificationHistoryRepository historyRepository;
    private final String publicBaseUrl;

    public NotificationTrackingService(NotificationHistoryRepository historyRepository,
                                       @Value("${pjb.notifications.public-base-url:http://localhost:8080}") String publicBaseUrl) {
        this.historyRepository = Objects.requireNonNull(historyRepository);
        this.publicBaseUrl = normalizeBase(publicBaseUrl);
    }

    public String newTrackingToken() {
        return UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().substring(0, 8);
    }

    public String buildPixelUrl(String token) {
        return publicBaseUrl + "/api/v1/notificacoes/track/" + token + ".gif";
    }

    public String buildCienciaUrl(String token) {
        return publicBaseUrl + "/api/v1/notificacoes/track/" + token + "/ciencia";
    }

    @Transactional
    public void markRead(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        historyRepository.findByTrackingToken(token).ifPresent(history -> {
            if (history.getLidoEm() == null) {
                history.setLidoEm(LocalDateTime.now());
            }
            if (history.getTrackingHash() == null || history.getTrackingHash().isBlank()) {
                history.setTrackingHash(Hashes.sha256Hex(token));
            }
            historyRepository.save(history);
        });
    }

    @Transactional
    public void markCiencia(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        historyRepository.findByTrackingToken(token).ifPresent(history -> {
            if (history.getLidoEm() == null) {
                history.setLidoEm(LocalDateTime.now());
            }
            history.setCienciaConfirmadaEm(LocalDateTime.now());
            if (history.getTrackingHash() == null || history.getTrackingHash().isBlank()) {
                history.setTrackingHash(Hashes.sha256Hex(token));
            }
            historyRepository.save(history);
        });
    }

    public byte[] pixelBytes() {
        return PIXEL_BYTES.clone();
    }

    public MediaType mediaType() {
        return MediaType.IMAGE_GIF;
    }

    private String normalizeBase(String value) {
        String out = value == null || value.isBlank() ? "http://localhost:8080" : value.trim();
        while (out.endsWith("/")) {
            out = out.substring(0, out.length() - 1);
        }
        return out;
    }
}
