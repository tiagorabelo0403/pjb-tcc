package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.time.Instant;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class NationalProceduralRightsCoverageService {

    public NationalProceduralRightsCoverageSnapshot snapshot() {
        List<NationalProceduralRightsCoverageRow> rows = java.util.Arrays.stream(RitoProcessual.values())
                .map(NationalProceduralRightsCatalogSupport::buildRow)
                .sorted(Comparator.comparing(NationalProceduralRightsCoverageRow::grupo)
                        .thenComparing(NationalProceduralRightsCoverageRow::ramo)
                        .thenComparing(NationalProceduralRightsCoverageRow::rito))
                .toList();
        List<NationalProceduralRightsCoverageFamily> families = rows.stream()
                .collect(Collectors.groupingBy(NationalProceduralRightsCoverageRow::grupo, LinkedHashMap::new, Collectors.toList()))
                .entrySet().stream()
                .map(entry -> toFamily(entry.getKey(), entry.getValue()))
                .toList();
        LinkedHashSet<String> justiceTracks = new LinkedHashSet<>();
        rows.forEach(row -> justiceTracks.addAll(row.justiceTracks()));
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("groups", families.stream().map(NationalProceduralRightsCoverageFamily::familyCode).toList());
        metadata.put("ramos", java.util.Arrays.stream(RamoDireito.values()).map(Enum::name).toList());
        metadata.put("segurosPorSigilo", rows.stream().filter(NationalProceduralRightsCoverageRow::segredoPadrao).count());
        metadata.put("comAtuacaoMp", rows.stream().filter(NationalProceduralRightsCoverageRow::exigeMinisterioPublico).count());
        metadata.put("autocompositivos", rows.stream().filter(NationalProceduralRightsCoverageRow::autocompositivo).count());
        metadata.put("juizados", rows.stream().filter(NationalProceduralRightsCoverageRow::admiteJuizado).count());
        return new NationalProceduralRightsCoverageSnapshot(
                Instant.now(),
                true,
                true,
                true,
                rows.size(),
                RamoDireito.values().length,
                families.size(),
                List.copyOf(justiceTracks),
                NationalProceduralRightsCatalogSupport.constitutionalGuarantees(),
                families,
                rows,
                Collections.unmodifiableMap(metadata)
        );
    }

    public NationalProceduralRightsCoverageRow describe(String ritoRaw) {
        RitoProcessual rito = RitoProcessual.tryParse(ritoRaw).orElse(RitoProcessual.COMUM_ORDINARIO);
        return NationalProceduralRightsCatalogSupport.buildRow(rito);
    }

    private NationalProceduralRightsCoverageFamily toFamily(String familyCode, List<NationalProceduralRightsCoverageRow> rows) {
        Objects.requireNonNull(familyCode);
        Objects.requireNonNull(rows);
        LinkedHashSet<String> justiceTracks = new LinkedHashSet<>();
        LinkedHashSet<String> markers = new LinkedHashSet<>();
        rows.forEach(row -> {
            justiceTracks.addAll(row.justiceTracks());
            markers.addAll(row.marcadores());
        });
        return new NationalProceduralRightsCoverageFamily(
                familyCode,
                familyCode.replace('_', ' '),
                rows.size(),
                rows.stream().anyMatch(NationalProceduralRightsCoverageRow::segredoPadrao),
                rows.stream().anyMatch(NationalProceduralRightsCoverageRow::exigeMinisterioPublico),
                rows.stream().anyMatch(NationalProceduralRightsCoverageRow::admiteConciliacao),
                List.copyOf(justiceTracks),
                List.copyOf(markers)
        );
    }
}
