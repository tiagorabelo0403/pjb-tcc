package com.tcc.pjb.backend.service.procedural;

import com.tcc.pjb.backend.core.catalog.TpuClasseCnj;
import com.tcc.pjb.backend.core.catalog.TpuClasseCnj.RamoJustica;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.integration.cnj.CnjTpuSyncService;
import com.tcc.pjb.backend.model.entity.competencia.TipoVaraDistribuicao;
import com.tcc.pjb.backend.model.entity.competencia.UnidadeJudiciariaCompetencia;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.repository.UnidadeJudiciariaCompetenciaRepository;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.platform.runtime.PjbTransactionalBudget;

@Service
public class NationalForumMeshGovernanceService {

    public record MeshGovernanceReport(
            int totalUnits,
            int updatedUnits,
            int classesAdded,
            int assuntosAdded,
            int digitalEndpointsAdjusted,
            int connectorHintsAdjusted,
            List<String> alerts
    ) {
    }

    private static final Map<String, String> CAPITALS = Map.ofEntries(
            Map.entry("AC", "Rio Branco"), Map.entry("AL", "Maceió"), Map.entry("AM", "Manaus"), Map.entry("AP", "Macapá"),
            Map.entry("BA", "Salvador"), Map.entry("CE", "Fortaleza"), Map.entry("DF", "Brasília"), Map.entry("ES", "Vitória"),
            Map.entry("GO", "Goiânia"), Map.entry("MA", "São Luís"), Map.entry("MG", "Belo Horizonte"), Map.entry("MS", "Campo Grande"),
            Map.entry("MT", "Cuiabá"), Map.entry("PA", "Belém"), Map.entry("PB", "João Pessoa"), Map.entry("PE", "Recife"),
            Map.entry("PI", "Teresina"), Map.entry("PR", "Curitiba"), Map.entry("RJ", "Rio de Janeiro"), Map.entry("RN", "Natal"),
            Map.entry("RO", "Porto Velho"), Map.entry("RR", "Boa Vista"), Map.entry("RS", "Porto Alegre"), Map.entry("SC", "Florianópolis"),
            Map.entry("SE", "Aracaju"), Map.entry("SP", "São Paulo"), Map.entry("TO", "Palmas")
    );

    private final UnidadeJudiciariaCompetenciaRepository unidadeRepository;
    private final CnjTpuSyncService cnjTpuSyncService;

    public NationalForumMeshGovernanceService(UnidadeJudiciariaCompetenciaRepository unidadeRepository,
                                              CnjTpuSyncService cnjTpuSyncService) {
        this.unidadeRepository = Objects.requireNonNull(unidadeRepository);
        this.cnjTpuSyncService = Objects.requireNonNull(cnjTpuSyncService);
    }

    @PjbTransactionalBudget(operation = "procedural.national-forum-mesh.reconcile", maxMillis = 15000)
    @Transactional
    public MeshGovernanceReport reconcile() {
        CnjTpuSyncService.TpuCatalogSnapshot snapshot = cnjTpuSyncService.currentSnapshot().orElseGet(cnjTpuSyncService::forceSync);
        List<UnidadeJudiciariaCompetencia> units = unidadeRepository.findAll();
        int updatedUnits = 0;
        int classesAdded = 0;
        int assuntosAdded = 0;
        int digitalAdjusted = 0;
        int connectorAdjusted = 0;
        LinkedHashSet<String> alerts = new LinkedHashSet<>();
        for (UnidadeJudiciariaCompetencia unit : units) {
            boolean changed = false;
            int classesBefore = unit.getClassesTpu().size();
            int assuntosBefore = unit.getAssuntosTpu().size();
            String endpointBefore = safe(unit.getEnderecoDigital());
            String connectorHint = safe(tribunalSigla(unit));

            Set<String> recommendedClasses = recommendClasses(unit, snapshot);
            if (!recommendedClasses.isEmpty()) {
                if (unit.getClassesTpu().addAll(recommendedClasses)) {
                    changed = true;
                }
            }
            Set<String> recommendedAssuntos = recommendAssuntos(unit, recommendedClasses);
            if (!recommendedAssuntos.isEmpty()) {
                if (unit.getAssuntosTpu().addAll(recommendedAssuntos)) {
                    changed = true;
                }
            }
            String expectedEndpoint = expectedDigitalEndpoint(unit);
            if (expectedEndpoint != null && !Objects.equals(expectedEndpoint, endpointBefore)) {
                unit.setEnderecoDigital(expectedEndpoint);
                changed = true;
            }
            String normalizedComarca = normalizedComarca(unit);
            if (normalizedComarca != null && !normalizedComarca.equals(comarcaNome(unit))) {
                unit.setEnderecoFisico(normalizedComarca + " - " + firstNonBlank(comarcaUf(unit), "BR"));
                changed = true;
            }
            if (connectorHint != null && connectorHint.startsWith("TJM-") && unit.getTipoJustica() != TipoJustica.MILITAR_ESTADUAL) {
                alerts.add("Unidade militar com tipo de justiça divergente: " + unit.getCodigo());
            }
            if (changed) {
                unidadeRepository.save(unit);
                updatedUnits++;
            }
            classesAdded += Math.max(0, unit.getClassesTpu().size() - classesBefore);
            assuntosAdded += Math.max(0, unit.getAssuntosTpu().size() - assuntosBefore);
            if (!Objects.equals(endpointBefore, safe(unit.getEnderecoDigital()))) {
                digitalAdjusted++;
            }
            if (!Objects.equals(connectorHint, safe(tribunalSigla(unit)))) {
                connectorAdjusted++;
            }
            if (unit.getClassesTpu().isEmpty()) {
                alerts.add("Unidade sem classes TPU reconciliadas: " + unit.getCodigo());
            }
        }
        return new MeshGovernanceReport(units.size(), updatedUnits, classesAdded, assuntosAdded, digitalAdjusted, connectorAdjusted, List.copyOf(alerts));
    }

    private Set<String> recommendClasses(UnidadeJudiciariaCompetencia unit,
                                         CnjTpuSyncService.TpuCatalogSnapshot snapshot) {
        LinkedHashSet<String> selected = new LinkedHashSet<>();
        RamoJustica ramoJustica = resolveRamoJustica(unit);
        for (TpuClasseCnj entry : TpuClasseCnj.todasEmOrdemCodigo()) {
            if (!matchesJustica(entry, ramoJustica)) {
                continue;
            }
            if (!matchesRamo(entry, unit)) {
                continue;
            }
            if (!matchesVara(entry, unit.getTipoVara())) {
                continue;
            }
            selected.add(entry.name());
            if (selected.size() >= maxClasses(unit.getTipoVara())) {
                break;
            }
        }
        if (selected.isEmpty() && snapshot != null) {
            snapshot.classes().stream()
                    .sorted(Comparator.comparingInt(CnjTpuSyncService.TpuClasseEntry::codigo))
                    .limit(12)
                    .forEach(item -> selected.add(normalizeToken(item.nome())));
        }
        return selected;
    }

    private Set<String> recommendAssuntos(UnidadeJudiciariaCompetencia unit,
                                          Set<String> classes) {
        LinkedHashSet<String> assuntos = new LinkedHashSet<>();
        if (unit.getRamoDireito() != null) {
            assuntos.add(normalizeToken(unit.getRamoDireito().getDescricao()));
        }
        if (unit.getTipoVara() != null) {
            assuntos.add(normalizeToken(unit.getTipoVara().name()));
        }
        if (unit.getTipoJustica() != null) {
            assuntos.add(normalizeToken(unit.getTipoJustica().name()));
        }
        classes.stream().limit(6).forEach(item -> assuntos.add(normalizeToken(item)));
        assuntos.remove(null);
        assuntos.remove("");
        return assuntos;
    }

    private RamoJustica resolveRamoJustica(UnidadeJudiciariaCompetencia unit) {
        if (unit.getTipoJustica() == TipoJustica.TRABALHO) {
            return RamoJustica.TRABALHO;
        }
        if (unit.getTipoJustica() == TipoJustica.ELEITORAL) {
            return RamoJustica.ELEITORAL;
        }
        if (unit.getTipoJustica() == TipoJustica.MILITAR_ESTADUAL || unit.getTipoJustica() == TipoJustica.MILITAR_FEDERAL) {
            return RamoJustica.MILITAR;
        }
        if (unit.getTipoJustica() == TipoJustica.FEDERAL) {
            return RamoJustica.FEDERAL;
        }
        if (unit.getTipoJustica() == TipoJustica.SUPERIOR) {
            return RamoJustica.SUPERIOR;
        }
        return RamoJustica.ESTADUAL;
    }

    private boolean matchesJustica(TpuClasseCnj entry, RamoJustica ramoJustica) {
        return switch (ramoJustica) {
            case ESTADUAL -> entry.ramoJustica() == RamoJustica.ESTADUAL
                    || entry.ramoJustica() == RamoJustica.ESTADUAL_FEDERAL
                    || entry.ramoJustica() == RamoJustica.ESTADUAL_FEDERAL_SUPERIOR;
            case FEDERAL -> entry.isFederal();
            case TRABALHO -> entry.isTrabalhista();
            case ELEITORAL -> entry.isEleitoral();
            case MILITAR -> entry.isMilitar();
            case SUPERIOR -> entry.ramoJustica() == RamoJustica.SUPERIOR
                    || entry.ramoJustica() == RamoJustica.ESTADUAL_FEDERAL_SUPERIOR;
            default -> true;
        };
    }

    private boolean matchesRamo(TpuClasseCnj entry, UnidadeJudiciariaCompetencia unit) {
        RamoDireito ramo = unit.getRamoDireito();
        if (ramo == null) {
            return true;
        }
        if (entry.ramoDireito() == ramo) {
            return true;
        }
        return switch (ramo) {
            case CIVIL -> Set.of(RamoDireito.CIVIL, RamoDireito.CONSUMIDOR, RamoDireito.EMPRESARIAL, RamoDireito.FAMILIA).contains(entry.ramoDireito());
            case ADMINISTRATIVO -> Set.of(RamoDireito.ADMINISTRATIVO, RamoDireito.TRIBUTARIO, RamoDireito.PREVIDENCIARIO, RamoDireito.CONSTITUCIONAL).contains(entry.ramoDireito());
            case PENAL -> Set.of(RamoDireito.PENAL, RamoDireito.CONSTITUCIONAL).contains(entry.ramoDireito());
            case TRABALHISTA -> true;
            default -> false;
        };
    }

    private boolean matchesVara(TpuClasseCnj entry, TipoVaraDistribuicao tipoVara) {
        if (tipoVara == null) {
            return true;
        }
        return switch (tipoVara) {
            case FAZENDA_PUBLICA -> entry.ramoDireito() == RamoDireito.ADMINISTRATIVO
                    || entry.ramoDireito() == RamoDireito.TRIBUTARIO
                    || entry.ramoDireito() == RamoDireito.PREVIDENCIARIO
                    || entry.ramoDireito() == RamoDireito.CONSTITUCIONAL;
            case CRIMINAL_GERAL, CRIMINAL_JUIZADO, TRIBUNAL_JURI -> entry.ramoDireito() == RamoDireito.PENAL || entry.ramoDireito() == RamoDireito.CONSTITUCIONAL;
            case TRABALHO -> entry.ramoDireito() == RamoDireito.TRABALHISTA;
            case INFANCIA_JUVENTUDE -> entry.ramoDireito() == RamoDireito.INFANCIA_JUVENTUDE || entry.ramoDireito() == RamoDireito.FAMILIA;
            case CIVEL_FAMILIA -> entry.ramoDireito() == RamoDireito.FAMILIA;
            case MEIO_AMBIENTE -> entry.ramoDireito() == RamoDireito.AMBIENTAL || entry.ramoDireito() == RamoDireito.ADMINISTRATIVO;
            case SAUDE_PUBLICA -> entry.ramoDireito() == RamoDireito.PREVIDENCIARIO || entry.ramoDireito() == RamoDireito.ADMINISTRATIVO;
            default -> true;
        };
    }

    private int maxClasses(TipoVaraDistribuicao tipoVara) {
        if (tipoVara == null) {
            return 12;
        }
        return switch (tipoVara) {
            case VARA_UNICA -> 18;
            case FAZENDA_PUBLICA, CRIMINAL_GERAL, TRABALHO -> 10;
            default -> 12;
        };
    }

    private String expectedDigitalEndpoint(UnidadeJudiciariaCompetencia unit) {
        String tribunal = normalizeToken(tribunalSigla(unit));
        if (tribunal == null) {
            return null;
        }
        String slug = tribunal.toLowerCase(Locale.ROOT).replace('_', '-');
        String suffix = normalizeToken(firstNonBlank(unit.getCodigo(), comarcaNome(unit), comarcaUf(unit)));
        if (suffix == null) {
            suffix = slug;
        }
        return "https://" + slug + ".pjb.local/" + suffix.toLowerCase(Locale.ROOT).replace('_', '-');
    }

    private String normalizedComarca(UnidadeJudiciariaCompetencia unit) {
        String uf = safe(comarcaUf(unit));
        if (uf == null) {
            return safe(comarcaNome(unit));
        }
        return firstNonBlank(safe(comarcaNome(unit)), CAPITALS.get(uf.toUpperCase(Locale.ROOT)));
    }

    private static String tribunalSigla(UnidadeJudiciariaCompetencia unit) {
        return unit.getTribunal() != null ? unit.getTribunal().getSigla() : null;
    }

    private static String comarcaNome(UnidadeJudiciariaCompetencia unit) {
        return unit.getComarca() != null ? unit.getComarca().getNome() : null;
    }

    private static String comarcaUf(UnidadeJudiciariaCompetencia unit) {
        return unit.getComarca() != null ? unit.getComarca().getUf() : null;
    }

    private String normalizeToken(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT)
                .replace('Ç', 'C')
                .replace('Á', 'A')
                .replace('À', 'A')
                .replace('Ã', 'A')
                .replace('Â', 'A')
                .replace('É', 'E')
                .replace('Ê', 'E')
                .replace('Í', 'I')
                .replace('Ó', 'O')
                .replace('Ô', 'O')
                .replace('Õ', 'O')
                .replace('Ú', 'U')
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
