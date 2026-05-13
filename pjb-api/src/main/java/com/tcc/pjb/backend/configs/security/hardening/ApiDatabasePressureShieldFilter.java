package com.tcc.pjb.backend.configs.security.hardening;

import com.tcc.pjb.backend.configs.datasource.DataSourceIntrospectionSupport;
import com.tcc.pjb.backend.configs.datasource.DataSourceIntrospectionSupport.PoolSnapshot;
import com.tcc.pjb.backend.core.observability.RequestContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import javax.sql.DataSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

public class ApiDatabasePressureShieldFilter extends OncePerRequestFilter {

    private final ApiDatabasePressureShieldProperties properties;
    private final ObjectProvider<DataSource> writeDataSourceProvider;
    private final ObjectProvider<DataSource> readDataSourceProvider;
    private final ObjectProvider<DataSource> applicationDataSourceProvider;
    private volatile Instant nextDecisionAt = Instant.EPOCH;
    private volatile boolean lastBlocked;
    private volatile PressureSnapshot lastSnapshot = PressureSnapshot.empty();

    public ApiDatabasePressureShieldFilter(ApiDatabasePressureShieldProperties properties,
                                           ObjectProvider<DataSource> writeDataSourceProvider,
                                           ObjectProvider<DataSource> readDataSourceProvider,
                                           ObjectProvider<DataSource> applicationDataSourceProvider) {
        this.properties = Objects.requireNonNull(properties, "properties");
        this.writeDataSourceProvider = Objects.requireNonNull(writeDataSourceProvider, "writeDataSourceProvider");
        this.readDataSourceProvider = Objects.requireNonNull(readDataSourceProvider, "readDataSourceProvider");
        this.applicationDataSourceProvider = Objects.requireNonNull(applicationDataSourceProvider, "applicationDataSourceProvider");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!properties.isEnabled() || isExempt(request.getRequestURI()) || !isGuarded(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }
        PressureDecision decision = evaluate();
        if (decision.blocked()) {
            writeProblem(response, decision.snapshot());
            return;
        }
        if (properties.isEmitDebugHeaders()) {
            writeHeaders(response, decision.snapshot());
        }
        filterChain.doFilter(request, response);
    }

    private PressureDecision evaluate() {
        Instant now = Instant.now();
        if (now.isBefore(nextDecisionAt)) {
            return new PressureDecision(lastBlocked, lastSnapshot);
        }
        PressureSnapshot snapshot = sample();
        boolean blocked = snapshot.writeActiveRatio() >= properties.getWriteActiveRatioThreshold()
                || snapshot.writeThreadsAwaiting() >= properties.getWriteThreadsAwaitingThreshold()
                || snapshot.readActiveRatio() >= properties.getReadActiveRatioThreshold()
                || snapshot.readThreadsAwaiting() >= properties.getReadThreadsAwaitingThreshold();
        lastSnapshot = snapshot;
        lastBlocked = blocked;
        nextDecisionAt = now.plus(properties.getMinDecisionTtl());
        return new PressureDecision(blocked, snapshot);
    }

    private PressureSnapshot sample() {
        DataSource writeSource = choose(writeDataSourceProvider.getIfAvailable(), applicationDataSourceProvider.getIfAvailable());
        DataSource readSource = choose(readDataSourceProvider.getIfAvailable(), writeSource);
        PoolSnapshot writeSnapshot = DataSourceIntrospectionSupport.snapshot(writeSource);
        PoolSnapshot readSnapshot = DataSourceIntrospectionSupport.snapshot(readSource);
        return new PressureSnapshot(
                writeSnapshot.activeRatio(),
                writeSnapshot.awaiting(),
                readSnapshot.activeRatio(),
                readSnapshot.awaiting()
        );
    }

    private static DataSource choose(DataSource preferred, DataSource fallback) {
        return preferred != null ? preferred : fallback;
    }

    private boolean isExempt(String uri) {
        return startsWithAny(uri, properties.getExemptPrefixes());
    }

    private boolean isGuarded(String uri) {
        return startsWithAny(uri, properties.getGuardedPrefixes());
    }

    private boolean startsWithAny(String uri, List<String> prefixes) {
        if (uri == null || uri.isBlank() || prefixes == null || prefixes.isEmpty()) {
            return false;
        }
        for (String prefix : prefixes) {
            if (prefix != null && !prefix.isBlank() && uri.startsWith(prefix.trim())) {
                return true;
            }
        }
        return false;
    }

    private void writeHeaders(HttpServletResponse response, PressureSnapshot snapshot) {
        response.setHeader("X-PJB-DB-Write-Active-Ratio", format(snapshot.writeActiveRatio()));
        response.setHeader("X-PJB-DB-Write-Awaiting", Integer.toString(snapshot.writeThreadsAwaiting()));
        response.setHeader("X-PJB-DB-Read-Active-Ratio", format(snapshot.readActiveRatio()));
        response.setHeader("X-PJB-DB-Read-Awaiting", Integer.toString(snapshot.readThreadsAwaiting()));
    }

    private void writeProblem(HttpServletResponse response, PressureSnapshot snapshot) throws IOException {
        response.setStatus(properties.getRejectionStatus());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setHeader("Cache-Control", "no-store, max-age=0");
        response.setHeader("Retry-After", "1");
        response.setHeader("X-PJB-DB-Pressure-Shield", "true");
        writeHeaders(response, snapshot);
        String requestId = RequestContext.getRequestId().orElse("");
        String body = "{" +
                "\"type\":\"https://pjb.local/problems/" + escapeJson(properties.getRejectionCode()) + "\"," +
                "\"title\":\"Database Pressure Shield\"," +
                "\"status\":" + properties.getRejectionStatus() + "," +
                "\"detail\":\"A API entrou em protecao por saturacao do pool do banco e orienta nova tentativa em instantes.\"," +
                "\"requestId\":\"" + escapeJson(requestId) + "\"," +
                "\"writeActiveRatio\":" + format(snapshot.writeActiveRatio()) + "," +
                "\"writeThreadsAwaiting\":" + snapshot.writeThreadsAwaiting() + "," +
                "\"readActiveRatio\":" + format(snapshot.readActiveRatio()) + "," +
                "\"readThreadsAwaiting\":" + snapshot.readThreadsAwaiting() +
                "}";
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        response.setContentLength(bytes.length);
        response.getOutputStream().write(bytes);
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.4f", value);
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", " ")
                .replace("\r", " ");
    }

    private record PressureSnapshot(double writeActiveRatio, int writeThreadsAwaiting, double readActiveRatio, int readThreadsAwaiting) {
        private static PressureSnapshot empty() {
            return new PressureSnapshot(0d, 0, 0d, 0);
        }
    }

    private record PressureDecision(boolean blocked, PressureSnapshot snapshot) {
    }
}
