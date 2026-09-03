package com.tcc.pjb.backend.service.infra;

import com.tcc.pjb.backend.model.entity.infra.CachePolicyOverride;
import com.tcc.pjb.backend.model.entity.infra.PartitionPlan;
import com.tcc.pjb.backend.model.repository.CachePolicyOverrideRepository;
import com.tcc.pjb.backend.model.repository.PartitionPlanRepository;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.Year;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.platform.runtime.PjbTransactionalBudget;

/**
 * Governança de políticas de cache por papel institucional e de planos de particionamento de
 * tabelas por ano. Extraído de {@link ScaleArchitectureService} porque esses 3 colaboradores
 * (repositório de cache, repositório de partição, JdbcTemplate para DDL) são usados
 * exclusivamente por esse subconjunto de métodos -- confirmado por grep, nenhum é tocado pelos
 * outros grupos de governança de escala (perfil judicial, modelos de secretaria, cobertura
 * processual, read models, postura de banco).
 */
@Service
public class ScaleCachePartitionGovernanceService {

    private final CachePolicyOverrideRepository cachePolicyOverrideRepository;
    private final PartitionPlanRepository partitionPlanRepository;
    private final JdbcTemplate jdbcTemplate;

    public ScaleCachePartitionGovernanceService(CachePolicyOverrideRepository cachePolicyOverrideRepository,
                                                 PartitionPlanRepository partitionPlanRepository,
                                                 JdbcTemplate jdbcTemplate) {
        this.cachePolicyOverrideRepository = Objects.requireNonNull(cachePolicyOverrideRepository);
        this.partitionPlanRepository = Objects.requireNonNull(partitionPlanRepository);
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate);
    }

    @Transactional(readOnly = true)
    public List<CachePolicyView> listarPoliticasCache() {
        List<CachePolicyView> overrides = cachePolicyOverrideRepository.findByEnabledTrueOrderByCacheNameAscRoleNameAsc().stream()
                .map(entity -> new CachePolicyView(entity.getId(), entity.getCacheName(), entity.getRoleName(), entity.getTtlSeconds(),
                        entity.getStaleWhileRevalidateSeconds(), entity.isEnabled(), entity.getNotes(), "OVERRIDE"))
                .toList();
        if (!overrides.isEmpty()) {
            return overrides;
        }
        return defaults().entrySet().stream()
                .map(entry -> {
                    String[] parts = entry.getKey().split("\\|", 2);
                    return new CachePolicyView(null, parts[0], parts[1], entry.getValue().ttlSeconds(), entry.getValue().staleWhileRevalidateSeconds(), true, entry.getValue().notes(), "DEFAULT");
                })
                .toList();
    }

    @Transactional
    public CachePolicyView salvarPoliticaCache(CachePolicyRequest request) {
        CachePolicyOverride entity = cachePolicyOverrideRepository
                .findByCacheNameIgnoreCaseAndRoleNameIgnoreCase(request.cacheName(), request.roleName())
                .orElseGet(CachePolicyOverride::new);
        entity.setCacheName(normalize(request.cacheName()));
        entity.setRoleName(normalize(request.roleName()));
        entity.setTtlSeconds(Math.max(1, request.ttlSeconds()));
        entity.setStaleWhileRevalidateSeconds(Math.max(0, request.staleWhileRevalidateSeconds()));
        entity.setEnabled(request.enabled());
        entity.setNotes(request.notes());
        CachePolicyOverride saved = cachePolicyOverrideRepository.save(entity);
        return new CachePolicyView(saved.getId(), saved.getCacheName(), saved.getRoleName(), saved.getTtlSeconds(),
                saved.getStaleWhileRevalidateSeconds(), saved.isEnabled(), saved.getNotes(), "OVERRIDE");
    }

    @PjbTransactionalBudget(operation = "infra.scale-architecture.listar-planos-particao", maxMillis = 3000)
    @Transactional(readOnly = true)
    public List<PartitionPlanView> listarPlanosParticao() {
        return partitionPlanRepository.findAll().stream()
                .map(plan -> new PartitionPlanView(plan.getId(), plan.getTableName(), plan.getPartitionColumn(), plan.getPartitionPrefix(),
                        plan.getStartYear(), plan.getYearsAhead(), plan.getStatus(), plan.getLastMaterializedYear(), plan.getNotes()))
                .toList();
    }

    @Transactional
    public PartitionPlanView salvarPlanoParticao(PartitionPlanRequest request) {
        PartitionPlan plan = partitionPlanRepository.findByTableNameIgnoreCase(request.tableName())
                .orElseGet(PartitionPlan::new);
        plan.setTableName(normalizeTable(request.tableName()));
        plan.setPartitionColumn(normalize(request.partitionColumn()));
        plan.setPartitionPrefix(normalizeTable(request.partitionPrefix()));
        plan.setStartYear(Math.max(2020, request.startYear()));
        plan.setYearsAhead(Math.max(1, request.yearsAhead()));
        plan.setStatus("ATIVO");
        plan.setNotes(request.notes());
        PartitionPlan saved = partitionPlanRepository.save(plan);
        return new PartitionPlanView(saved.getId(), saved.getTableName(), saved.getPartitionColumn(), saved.getPartitionPrefix(),
                saved.getStartYear(), saved.getYearsAhead(), saved.getStatus(), saved.getLastMaterializedYear(), saved.getNotes());
    }

    @Transactional(readOnly = true)
    public PartitionPreview previewMaterializacao(String tableName) {
        PartitionPlan plan = partitionPlanRepository.findByTableNameIgnoreCase(tableName)
                .orElseThrow(() -> new IllegalArgumentException("Plano de particionamento nao encontrado."));
        int currentYear = Year.now().getValue();
        int initialYear = Math.max(plan.getStartYear(), currentYear);
        int lastYear = Math.max(initialYear, currentYear + Math.max(1, plan.getYearsAhead()));
        List<String> ddls = java.util.stream.IntStream.rangeClosed(initialYear, lastYear)
                .mapToObj(year -> buildPartitionDdl(plan, year))
                .toList();
        return new PartitionPreview(plan.getTableName(), initialYear, lastYear, ddls);
    }

    @Transactional
    public PartitionPreview materializar(String tableName) {
        PartitionPreview preview = previewMaterializacao(tableName);
        for (String ddl : preview.ddlStatements()) {
            jdbcTemplate.execute(ddl);
        }
        PartitionPlan plan = partitionPlanRepository.findByTableNameIgnoreCase(tableName)
                .orElseThrow(() -> new IllegalArgumentException("Plano de particionamento nao encontrado."));
        plan.setLastMaterializedYear(preview.endYear());
        partitionPlanRepository.save(plan);
        return preview;
    }

    private String buildPartitionDdl(PartitionPlan plan, int year) {
        String shardTable = normalizeTable(plan.getPartitionPrefix()) + "_" + year;
        return "CREATE TABLE IF NOT EXISTS " + shardTable +
                " (LIKE " + normalizeTable(plan.getTableName()) + " INCLUDING ALL);";
    }

    private String normalize(String input) {
        return input == null ? "" : input.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeTable(String input) {
        if (input == null) {
            return "";
        }
        String normalized = input.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
        while (normalized.contains("__")) {
            normalized = normalized.replace("__", "_");
        }
        return normalized;
    }

    private Map<String, DefaultCachePolicy> defaults() {
        Map<String, DefaultCachePolicy> out = new LinkedHashMap<>();
        out.put("judge_dashboard|JUIZ", new DefaultCachePolicy(15, 5, "Painel decisional com refresh curto."));
        out.put("public_timeline|CIDADAO", new DefaultCachePolicy(60, 20, "Linha do tempo publica de alto volume."));
        out.put("laiane_judge|MAGISTRADO", new DefaultCachePolicy(20, 10, "IA magistratura com janela curta de reuso."));
        out.put("processo_resumo|ADVOGADO", new DefaultCachePolicy(45, 15, "Resumo processual para advocacia."));
        out.put("camara_governanca|DESEMBARGADOR", new DefaultCachePolicy(30, 10, "Painel colegiado com giro medio."));
        out.put("plenario_publico|MINISTRO", new DefaultCachePolicy(25, 10, "Publicacao de sessao plenaria."));
        out.put("processo_timeline_hot|ADVOGADO", new DefaultCachePolicy(20, 8, "Timeline resumida de processo com invalidação por evento."));
        out.put("audiencia_agenda|SERVIDOR", new DefaultCachePolicy(30, 10, "Agenda forense e mapa de audiencias."));
        out.put("peticionamento_workspace|ADVOGADO", new DefaultCachePolicy(12, 4, "Workspace de peticionamento com leitura quente e protecao de consistencia."));
        return out;
    }

    private record DefaultCachePolicy(int ttlSeconds, int staleWhileRevalidateSeconds, String notes) {
    }

    public record CachePolicyRequest(
            @NotBlank String cacheName,
            @NotBlank String roleName,
            @Min(1) int ttlSeconds,
            @Min(0) int staleWhileRevalidateSeconds,
            boolean enabled,
            String notes
    ) {
    }

    public record CachePolicyView(
            Long id,
            String cacheName,
            String roleName,
            Integer ttlSeconds,
            Integer staleWhileRevalidateSeconds,
            boolean enabled,
            String notes,
            String source
    ) {
    }

    public record PartitionPlanRequest(
            @NotBlank String tableName,
            @NotBlank String partitionColumn,
            @NotBlank String partitionPrefix,
            @Min(2020) int startYear,
            @Min(1) int yearsAhead,
            String notes
    ) {
    }

    public record PartitionPlanView(
            Long id,
            String tableName,
            String partitionColumn,
            String partitionPrefix,
            Integer startYear,
            Integer yearsAhead,
            String status,
            Integer lastMaterializedYear,
            String notes
    ) {
    }

    public record PartitionPreview(
            String tableName,
            Integer startYear,
            Integer endYear,
            List<String> ddlStatements
    ) {
    }
}
