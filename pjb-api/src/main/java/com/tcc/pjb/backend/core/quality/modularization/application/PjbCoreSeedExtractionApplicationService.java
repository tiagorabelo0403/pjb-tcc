package com.tcc.pjb.backend.core.quality.modularization.application;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.quality.modularization.domain.PjbCoreSeedDriftIssueView;
import com.tcc.pjb.backend.core.quality.modularization.domain.PjbCoreSeedExtractionSnapshot;
import com.tcc.pjb.backend.core.quality.modularization.domain.PjbCoreSeedPackageMirrorView;
import com.tcc.pjb.backend.core.quality.modularization.domain.PjbCoreSeedParityView;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import jakarta.inject.Inject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
@Service
public class PjbCoreSeedExtractionApplicationService {

    private static final String PACKAGE_NAME = "com.tcc.pjb.backend.core.modularity";
    private static final String SOURCE_ROOT = "src/main/java/com/tcc/pjb/backend/core/modularity";
    private static final String MODULE_ROOT = "pjb-core/src/main/java/com/tcc/pjb/backend/core/modularity";

    private final AuditLedgerService auditLedgerService;
    private final Path projectRoot;

    @Inject
    @Autowired
    public PjbCoreSeedExtractionApplicationService(AuditLedgerService auditLedgerService) {
        this(auditLedgerService, Path.of(""));
    }

    PjbCoreSeedExtractionApplicationService(AuditLedgerService auditLedgerService, Path projectRoot) {
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
        this.projectRoot = projectRoot == null ? Path.of("").toAbsolutePath().normalize() : projectRoot.toAbsolutePath().normalize();
    }

    @Transactional(readOnly = true)
    public PjbCoreSeedExtractionSnapshot snapshot() {
        List<PjbCoreSeedParityView> parity = parity();
        int sourceFileCount = sourceFiles().size();
        int mirroredFileCount = moduleFiles().size();
        int alignedFileCount = (int) parity.stream().filter(PjbCoreSeedParityView::contentAligned).count();
        List<String> blockers = new ArrayList<>();
        if (!Files.isDirectory(sourceRoot())) {
            blockers.add("pacote fonte core.modularity inexistente no monolito raiz");
        }
        if (!Files.isDirectory(moduleRoot())) {
            blockers.add("espelho fisico do pacote ainda nao existe em pjb-core");
        }
        if (sourceFileCount != mirroredFileCount) {
            blockers.add("espelho fisico ainda nao cobre a mesma quantidade de classes do pacote fonte");
        }
        if (alignedFileCount != sourceFileCount || alignedFileCount != mirroredFileCount) {
            blockers.add("conteudo do seed mirror ainda nao esta totalmente alinhado entre raiz e pjb-core");
        }
        PjbCoreSeedExtractionSnapshot snapshot = new PjbCoreSeedExtractionSnapshot(
                Files.isDirectory(sourceRoot()),
                Files.isDirectory(moduleRoot()),
                blockers.isEmpty(),
                sourceFileCount,
                mirroredFileCount,
                alignedFileCount,
                blockers,
                Instant.now());
        auditLedgerService.appendSafely("MODULARIZATION_CORE_SEED_SNAPSHOT_QUERY", "MODULARIZATION", "PJB_CORE_SEED", null, "aligned=" + alignedFileCount);
        return snapshot;
    }

    @Transactional(readOnly = true)
    public List<PjbCoreSeedPackageMirrorView> mirrors() {
        List<PjbCoreSeedPackageMirrorView> views = List.of(new PjbCoreSeedPackageMirrorView(
                PACKAGE_NAME,
                SOURCE_ROOT,
                MODULE_ROOT,
                relativeNames(sourceFiles()),
                relativeNames(moduleFiles())));
        auditLedgerService.appendSafely("MODULARIZATION_CORE_SEED_MIRRORS_QUERY", "MODULARIZATION", "PJB_CORE_SEED", null, "count=" + views.size());
        return views;
    }

    @Transactional(readOnly = true)
    public List<PjbCoreSeedDriftIssueView> drift() {
        Set<String> names = new LinkedHashSet<>();
        names.addAll(relativeNames(sourceFiles()));
        names.addAll(relativeNames(moduleFiles()));
        List<PjbCoreSeedDriftIssueView> issues = new ArrayList<>();
        for (String name : names) {
            Path source = sourceRoot().resolve(name);
            Path module = moduleRoot().resolve(name);
            boolean sourcePresent = Files.exists(source);
            boolean modulePresent = Files.exists(module);
            if (sourcePresent && !modulePresent) {
                issues.add(new PjbCoreSeedDriftIssueView("ALTO", name, "missing.module.mirror", name + " existe na raiz e ainda nao foi espelhado em pjb-core"));
                continue;
            }
            if (!sourcePresent && modulePresent) {
                issues.add(new PjbCoreSeedDriftIssueView("MEDIO", name, "orphan.module.file", name + " existe em pjb-core sem correspondente na raiz"));
                continue;
            }
            String sourceHash = hash(source);
            String moduleHash = hash(module);
            if (!sourceHash.equals(moduleHash)) {
                issues.add(new PjbCoreSeedDriftIssueView("ALTO", name, "content.hash.mismatch", name + " divergiu entre a raiz monolitica e o seed mirror do modulo"));
            }
        }
        auditLedgerService.appendSafely("MODULARIZATION_CORE_SEED_DRIFT_QUERY", "MODULARIZATION", "PJB_CORE_SEED", null, "count=" + issues.size());
        return List.copyOf(issues);
    }

    @Transactional(readOnly = true)
    public List<PjbCoreSeedParityView> parity() {
        Set<String> names = new LinkedHashSet<>();
        names.addAll(relativeNames(sourceFiles()));
        names.addAll(relativeNames(moduleFiles()));
        List<PjbCoreSeedParityView> views = names.stream()
                .map(name -> {
                    Path source = sourceRoot().resolve(name);
                    Path module = moduleRoot().resolve(name);
                    boolean sourcePresent = Files.exists(source);
                    boolean modulePresent = Files.exists(module);
                    String sourceHash = sourcePresent ? hash(source) : "";
                    String moduleHash = modulePresent ? hash(module) : "";
                    return new PjbCoreSeedParityView(name, sourcePresent, modulePresent, sourcePresent && modulePresent && sourceHash.equals(moduleHash), sourceHash, moduleHash);
                })
                .sorted(Comparator.comparing(PjbCoreSeedParityView::className))
                .toList();
        auditLedgerService.appendSafely("MODULARIZATION_CORE_SEED_PARITY_QUERY", "MODULARIZATION", "PJB_CORE_SEED", null, "count=" + views.size());
        return views;
    }

    private Path sourceRoot() {
        return projectRoot.resolve(SOURCE_ROOT);
    }

    private Path moduleRoot() {
        return projectRoot.resolve(MODULE_ROOT);
    }

    private List<Path> sourceFiles() {
        return javaFiles(sourceRoot());
    }

    private List<Path> moduleFiles() {
        return javaFiles(moduleRoot());
    }

    private List<Path> javaFiles(Path root) {
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        try (Stream<Path> walk = Files.walk(root)) {
            return walk.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
        } catch (IOException exception) {
            return List.of();
        }
    }

    private List<String> relativeNames(List<Path> files) {
        return files.stream().map(path -> path.getFileName().toString()).sorted().toList();
    }

    private String hash(Path path) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(Files.readAllBytes(path)));
        } catch (IOException | NoSuchAlgorithmException exception) {
            return "";
        }
    }
}
