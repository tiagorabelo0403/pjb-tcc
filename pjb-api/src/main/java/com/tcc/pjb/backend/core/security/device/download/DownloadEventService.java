package com.tcc.pjb.backend.core.security.device.download;

import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.security.DownloadEvent;
import com.tcc.pjb.backend.model.repository.security.DownloadEventRepository;

@Service
public class DownloadEventService {

    private final DownloadEventRepository repo;

    public DownloadEventService(DownloadEventRepository repo) {
        this.repo = Objects.requireNonNull(repo);
    }

    @Transactional
    public void record(Usuario usuario, Long deviceId, String path, long bytes, String watermarkId) {
        record(usuario, deviceId, path, bytes, watermarkId, null, null);
    }

    @Transactional
    public void record(Usuario usuario, Long deviceId, String path, long bytes, String watermarkId, Long processoId, String documentoId) {
        if (usuario == null || usuario.getId() == null) throw new IllegalArgumentException("usuario obrigatório");
        if (path == null || path.isBlank()) throw new IllegalArgumentException("path obrigatório");
        if (bytes < 0) bytes = 0;

        DownloadEvent e = new DownloadEvent();
        e.setUsuario(usuario);
        e.setDeviceId(deviceId);
        e.setProcessoId(processoId);
        e.setDocumentoId(trim(documentoId, 36));
        e.setPath(trim(path, 300));
        e.setBytes(bytes);
        e.setWatermarkId(trim(watermarkId, 96));
        repo.save(e);
    }

    private static String trim(String v, int max) {
        if (v == null) return null;
        String s = v.trim();
        if (s.isEmpty()) return null;
        if (s.length() > max) s = s.substring(0, max);
        return s;
    }
}
