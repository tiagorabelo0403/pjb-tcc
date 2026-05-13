package com.tcc.pjb.backend.core.security.device.download;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.security.device.DeviceSecurityProperties;
import com.tcc.pjb.backend.model.repository.security.DownloadEventRepository;

@Service
public class DownloadBudgetService {

    private final DeviceSecurityProperties props;
    private final DownloadEventRepository repo;

    public DownloadBudgetService(DeviceSecurityProperties props, DownloadEventRepository repo) {
        this.props = Objects.requireNonNull(props);
        this.repo = Objects.requireNonNull(repo);
    }

    public void enforceRestrictedBudget(Long userId, Long deviceId) {
        enforceRestrictedBudget(userId, deviceId, null, null);
    }

    public void enforceRestrictedBudget(Long userId, Long deviceId, Long processoId, String documentoId) {
        if (userId == null) throw new IllegalArgumentException("userId obrigatório");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from = now.minusHours(1);

        BudgetHit global = enforceGlobal(userId, deviceId, from, now);
        BudgetHit proc = enforceProcess(userId, processoId, from, now);
        BudgetHit doc = enforceDocumento(userId, documentoId, from, now);

        List<String> scopes = new ArrayList<>();
        long retryAfter = 0;

        if (global != null) {
            scopes.add("global");
            retryAfter = Math.max(retryAfter, global.retryAfterSeconds());
        }
        if (proc != null) {
            scopes.add("processo");
            retryAfter = Math.max(retryAfter, proc.retryAfterSeconds());
        }
        if (doc != null) {
            scopes.add("documento");
            retryAfter = Math.max(retryAfter, doc.retryAfterSeconds());
        }

        if (scopes.isEmpty()) return;
        String msg = "Limite de downloads sensíveis atingido (" + String.join(",", scopes) + "). Tente novamente mais tarde.";
        throw new DownloadBudgetExceededException(msg, Math.max(1L, retryAfter));
    }

    private BudgetHit enforceGlobal(Long userId, Long deviceId, LocalDateTime from, LocalDateTime now) {
        int maxCount = Math.max(1, props.getRestrictedDownloadMaxPerHour());
        long maxBytes = Math.max(1L, props.getRestrictedDownloadMaxBytesPerHour());

        long count;
        long bytes;
        LocalDateTime oldest;

        if (deviceId != null) {
            count = repo.countRecentByUserAndDevice(userId, deviceId, from);
            bytes = repo.sumBytesRecentByUserAndDevice(userId, deviceId, from);
            oldest = repo.oldestRecentByUserAndDevice(userId, deviceId, from);
        } else {
            count = repo.countRecentByUser(userId, from);
            bytes = repo.sumBytesRecentByUser(userId, from);
            oldest = repo.oldestRecentByUser(userId, from);
        }

        if (count < maxCount && bytes < maxBytes) return null;
        return BudgetHit.of(oldest, now);
    }

    private BudgetHit enforceProcess(Long userId, Long processoId, LocalDateTime from, LocalDateTime now) {
        if (processoId == null) return null;

        int maxCount = Math.max(1, props.getRestrictedDownloadMaxPerHourPerProcess());
        long maxBytes = Math.max(1L, props.getRestrictedDownloadMaxBytesPerHourPerProcess());

        long count = repo.countRecentByUserAndProcess(userId, processoId, from);
        long bytes = repo.sumBytesRecentByUserAndProcess(userId, processoId, from);
        LocalDateTime oldest = repo.oldestRecentByUserAndProcess(userId, processoId, from);

        if (count < maxCount && bytes < maxBytes) return null;
        return BudgetHit.of(oldest, now);
    }

    private BudgetHit enforceDocumento(Long userId, String documentoId, LocalDateTime from, LocalDateTime now) {
        if (documentoId == null || documentoId.isBlank()) return null;
        String doc = trim(documentoId, 36);
        if (doc == null) return null;

        int maxCount = Math.max(1, props.getRestrictedDownloadMaxPerHourPerDocument());
        long maxBytes = Math.max(1L, props.getRestrictedDownloadMaxBytesPerHourPerDocument());

        long count = repo.countRecentByUserAndDocumento(userId, doc, from);
        long bytes = repo.sumBytesRecentByUserAndDocumento(userId, doc, from);
        LocalDateTime oldest = repo.oldestRecentByUserAndDocumento(userId, doc, from);

        if (count < maxCount && bytes < maxBytes) return null;
        return BudgetHit.of(oldest, now);
    }

    private record BudgetHit(long retryAfterSeconds) {
        static BudgetHit of(LocalDateTime oldest, LocalDateTime now) {
            if (oldest == null) return new BudgetHit(60);
            long sec = Duration.between(oldest, now).getSeconds();
            long retry = Math.max(1L, 3600L - sec);
            return new BudgetHit(retry);
        }
    }

    private static String trim(String v, int max) {
        if (v == null) return null;
        String s = v.trim();
        if (s.isEmpty()) return null;
        if (s.length() > max) s = s.substring(0, max);
        return s;
    }
}
