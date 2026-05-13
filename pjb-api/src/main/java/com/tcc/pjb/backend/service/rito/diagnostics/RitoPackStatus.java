package com.tcc.pjb.backend.service.rito.diagnostics;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;

@Component
public class RitoPackStatus {

    private volatile boolean loaded;
    private volatile Instant loadedAt;
    private volatile String checksum;
    private volatile String version;

    private final CopyOnWriteArrayList<String> issues = new CopyOnWriteArrayList<>();

    public void markLoaded(String version, String checksum, List<String> issues) {
        this.loaded = true;
        this.loadedAt = Instant.now();
        this.version = version;
        this.checksum = checksum;
        this.issues.clear();
        if (issues != null) this.issues.addAll(issues);
    }

    public void markFailed(String version, String checksum, String error) {
        this.loaded = false;
        this.loadedAt = Instant.now();
        this.version = version;
        this.checksum = checksum;
        this.issues.clear();
        if (error != null && !error.isBlank()) this.issues.add(error.trim());
    }

    public boolean isLoaded() {
        return loaded;
    }

    public Instant getLoadedAt() {
        return loadedAt;
    }

    public String getChecksum() {
        return checksum;
    }

    public String getVersion() {
        return version;
    }

    public List<String> getIssues() {
        return Collections.unmodifiableList(new ArrayList<>(issues));
    }
}
