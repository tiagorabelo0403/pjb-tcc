package com.tcc.pjb.backend.core.catalog;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogVersionService {

    private static final Logger log = LoggerFactory.getLogger(CatalogVersionService.class);

    public static final String KEY_RITOS_PACK = "ritos_pack";
    private static final String DEFAULT_VERSION = "2026";
    private static final String DEFAULT_RESOURCE = "ritos/rito_pack_2026.json";

    private final CatalogVersionRepository repository;

    public CatalogVersionService(CatalogVersionRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public CatalogVersion resolveCurrentRitosPack() {
        return resolveOrCreate(KEY_RITOS_PACK, DEFAULT_VERSION, DEFAULT_RESOURCE);
    }

    @Transactional
    public CatalogVersion resolveOrCreate(String key, String version, String resourcePathForChecksum) {
        if (key == null || key.isBlank()) throw new IllegalArgumentException("key obrigatória");
        if (version == null || version.isBlank()) throw new IllegalArgumentException("version obrigatória");

        return repository.findTopByKeyAndActiveTrueOrderByIdDesc(key.trim())
                .orElseGet(() -> {
                    String checksum = sha256Resource(resourcePathForChecksum);
                    CatalogVersion cv = repository.findByKeyAndVersion(key.trim(), version.trim())
                            .orElseGet(CatalogVersion::new);
                    cv.setKey(key.trim());
                    cv.setVersion(version.trim());
                    cv.setChecksum(checksum);
                    cv.setActive(true);
                    CatalogVersion saved = repository.save(cv);
                    log.info("CatalogVersion criada/ativada: key={} version={} checksum={}", saved.getKey(), saved.getVersion(), saved.getChecksum());
                    return saved;
                });
    }

    private static String sha256Resource(String path) {
        if (path == null || path.isBlank()) {
            
            return sha256Hex("NO_RESOURCE");
        }
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] buf = new byte[8192];
            int r;
            while ((r = in.read(buf)) > 0) {
                md.update(buf, 0, r);
            }
            return HexFormat.of().formatHex(md.digest());
        } catch (Exception e) {
            
            return sha256Hex("MISSING_RESOURCE:" + path);
        }
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }
}
