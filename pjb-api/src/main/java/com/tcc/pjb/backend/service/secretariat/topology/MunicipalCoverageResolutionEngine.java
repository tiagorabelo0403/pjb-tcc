package com.tcc.pjb.backend.service.secretariat.topology;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.forum.routing.ForumDeskKey;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.EsferaJurisdicao;
import com.tcc.pjb.backend.model.entity.Jurisdicao;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.MateriaJurisdicao;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.repository.JurisdicaoRepository;

@Service
public class MunicipalCoverageResolutionEngine {

    private final JurisdicaoRepository jurisdicaoRepository;

    public MunicipalCoverageResolutionEngine(JurisdicaoRepository jurisdicaoRepository) {
        this.jurisdicaoRepository = Objects.requireNonNull(jurisdicaoRepository);
    }

    public MunicipalCoverageProfile resolve(Processo processo, TipoJustica tipoJustica, ForumDeskKey deskKey) {
        Objects.requireNonNull(processo, "processo");
        Objects.requireNonNull(deskKey, "deskKey");
        Jurisdicao jurisdicao = resolveEffectiveJurisdicao(processo, tipoJustica, deskKey);
        String sourceMunicipality = firstNonBlank(
                processo.getJurisdicao() != null ? processo.getJurisdicao().getMunicipioOuComarca() : null,
                processo.getComarca(),
                deskKey.comarca(),
                processo.getUf()
        );
        String seatMunicipality = firstNonBlank(
                jurisdicao != null ? jurisdicao.getMunicipioOuComarca() : null,
                deskKey.comarca(),
                processo.getComarca(),
                sourceMunicipality,
                "CENTRAL"
        );
        String territorialScope = firstNonBlank(
                jurisdicao != null ? jurisdicao.getForo() : null,
                jurisdicao != null ? jurisdicao.getSecaoOuSubsecao() : null,
                jurisdicao != null ? jurisdicao.getCircunscricao() : null,
                seatMunicipality,
                deskKey.territorialLabel(),
                "COBERTURA_GERAL"
        );
        String sourceUf = firstNonBlank(
                processo.getJurisdicao() != null ? processo.getJurisdicao().getUf() : null,
                processo.getUf(),
                deskKey.uf(),
                "BR"
        );
        String mode = resolveMode(jurisdicao, sourceMunicipality, seatMunicipality, territorialScope);
        String jurisdictionCode = firstNonBlank(jurisdicao != null ? jurisdicao.getCodigo() : null, "SEM_JURISDICAO_CADASTRADA");
        String coverageKey = normalize(sourceUf) + ':' + normalize(sourceMunicipality) + ':' + normalize(seatMunicipality) + ':' + normalize(jurisdictionCode);
        List<String> notes = new ArrayList<>();
        notes.add("Cobertura municipal resolvida para a malha cartorária a partir da jurisdição efetiva e do desk do fórum.");
        notes.add("Município de origem: " + safe(sourceMunicipality) + '.');
        notes.add("Município ou sede competente: " + safe(seatMunicipality) + '.');
        notes.add("Modo de cobertura: " + mode + '.');
        if (jurisdicao != null) {
            notes.add("Jurisdição efetiva: " + safe(jurisdicao.getNome()) + " (" + safe(jurisdicao.getSigla()) + ").");
        } else {
            notes.add("Cobertura resolvida por malha presumida de topologia quando não houver unidade jurisdicional explícita no processo.");
        }
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("sourceMunicipality", sourceMunicipality);
        metadata.put("seatMunicipality", seatMunicipality);
        metadata.put("territorialScope", territorialScope);
        metadata.put("sourceUf", sourceUf);
        metadata.put("jurisdictionCode", jurisdicao != null ? jurisdicao.getCodigo() : null);
        metadata.put("jurisdictionName", jurisdicao != null ? jurisdicao.getNome() : null);
        metadata.put("jurisdictionSigla", jurisdicao != null ? jurisdicao.getSigla() : null);
        metadata.put("jurisdictionEsfera", jurisdicao != null && jurisdicao.getEsfera() != null ? jurisdicao.getEsfera().name() : null);
        metadata.put("jurisdictionMateria", jurisdicao != null && jurisdicao.getMateria() != null ? jurisdicao.getMateria().name() : null);
        metadata.put("territorialCompetence", jurisdicao != null ? jurisdicao.getCompetenciaTerritorial() : null);
        metadata.put("coverageMode", mode);
        metadata.entrySet().removeIf(entry -> entry.getValue() == null);
        return new MunicipalCoverageProfile(
                coverageKey,
                sourceUf,
                sourceMunicipality,
                seatMunicipality,
                territorialScope,
                mode,
                jurisdictionCode,
                List.copyOf(notes),
                Collections.unmodifiableMap(metadata)
        );
    }

    private Jurisdicao resolveEffectiveJurisdicao(Processo processo, TipoJustica tipoJustica, ForumDeskKey deskKey) {
        if (processo.getJurisdicao() != null) {
            return processo.getJurisdicao();
        }
        String uf = firstNonBlank(processo.getUf(), deskKey.uf());
        if (uf == null || uf.isBlank()) {
            return null;
        }
        List<Jurisdicao> candidates = jurisdicaoRepository.findByUf(uf.trim().toUpperCase(Locale.ROOT));
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        EsferaJurisdicao esfera = mapEsfera(tipoJustica);
        GrauJurisdicao grau = switch (deskKey.instance()) {
            case FIRST -> GrauJurisdicao.PRIMEIRO_GRAU;
            case SECOND -> GrauJurisdicao.SEGUNDO_GRAU;
            case SUPERIOR -> GrauJurisdicao.SUPERIOR;
        };
        MateriaJurisdicao materiaEsperada = MateriaJurisdicao.fromRamo(processo.getRamoDireito());
        String targetTerritory = firstNonBlank(
                processo.getComarca(),
                deskKey.comarca(),
                processo.getVara(),
                processo.getUnidadeJudiciariaCodigo(),
                processo.getClasseProcessual()
        );
        return candidates.stream()
                .filter(Jurisdicao::getAtivo)
                .filter(j -> esfera == null || j.getEsfera() == null || j.getEsfera() == esfera)
                .filter(j -> j.getGrau() == null || j.getGrau() == grau)
                .sorted(Comparator.comparingInt((Jurisdicao j) -> score(j, targetTerritory, materiaEsperada)).reversed())
                .findFirst()
                .filter(j -> score(j, targetTerritory, materiaEsperada) > 0)
                .orElse(null);
    }

    private int score(Jurisdicao jurisdicao, String targetTerritory, MateriaJurisdicao materiaEsperada) {
        int score = 0;
        if (jurisdicao == null) {
            return score;
        }
        if (matches(targetTerritory, jurisdicao.getCidade())) {
            score += 50;
        }
        if (matches(targetTerritory, jurisdicao.getForo())) {
            score += 35;
        }
        if (matches(targetTerritory, jurisdicao.getSecaoOuSubsecao())) {
            score += 30;
        }
        if (matches(targetTerritory, jurisdicao.getNome())) {
            score += 20;
        }
        if (jurisdicao.getMateria() == materiaEsperada) {
            score += 30;
        }
        if (jurisdicao.getMateria() == MateriaJurisdicao.MULTIMATERIA) {
            score += 10;
        }
        if (Boolean.TRUE.equals(jurisdicao.getPermiteJuizadoEspecial())) {
            score += 3;
        }
        return score;
    }

    private boolean matches(String left, String right) {
        if (left == null || left.isBlank() || right == null || right.isBlank()) {
            return false;
        }
        String l = normalize(left);
        String r = normalize(right);
        return l.equals(r) || l.contains(r) || r.contains(l);
    }

    private String resolveMode(Jurisdicao jurisdicao, String sourceMunicipality, String seatMunicipality, String territorialScope) {
        if (jurisdicao == null) {
            return "PRESUMIDA_POR_TOPOLOGIA";
        }
        if (jurisdicao.getSecaoOuSubsecao() != null && !jurisdicao.getSecaoOuSubsecao().isBlank()) {
            return "SECAO_SUBSECAO_COMPETENTE";
        }
        if (jurisdicao.getForo() != null && !jurisdicao.getForo().isBlank() && !matches(jurisdicao.getForo(), seatMunicipality)) {
            return "FORO_ESPECIALIZADO_COMPETENTE";
        }
        if (!matches(sourceMunicipality, seatMunicipality)) {
            return "MUNICIPIO_COBERTO_POR_SEDE_DISTINTA";
        }
        if (territorialScope != null && !territorialScope.isBlank()) {
            return "SEDE_PROPRIA_COM_COBERTURA_TERRITORIAL";
        }
        return "SEDE_PROPRIA";
    }

    private EsferaJurisdicao mapEsfera(TipoJustica tipoJustica) {
        if (tipoJustica == null) {
            return null;
        }
        return switch (tipoJustica) {
            case FEDERAL -> EsferaJurisdicao.JUSTICA_FEDERAL;
            case TRABALHO -> EsferaJurisdicao.JUSTICA_TRABALHO;
            case ELEITORAL -> EsferaJurisdicao.JUSTICA_ELEITORAL;
            case MILITAR_ESTADUAL, MILITAR_FEDERAL -> EsferaJurisdicao.JUSTICA_MILITAR;
            default -> EsferaJurisdicao.JUSTICA_ESTADUAL;
        };
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "N/A" : value.trim();
    }

    private static String firstNonBlank(String... values) {
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

    private static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "-";
        }
        return raw.trim().toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("(^_|_$)", "");
    }

    public record MunicipalCoverageProfile(
            String coverageKey,
            String sourceUf,
            String sourceMunicipality,
            String seatMunicipality,
            String territorialScope,
            String coverageMode,
            String jurisdictionCode,
            List<String> notes,
            Map<String, Object> metadata
    ) {

        public Map<String, Object> toMap() {
            LinkedHashMap<String, Object> out = new LinkedHashMap<>();
            out.put("coverageKey", coverageKey);
            out.put("sourceUf", sourceUf);
            out.put("sourceMunicipality", sourceMunicipality);
            out.put("seatMunicipality", seatMunicipality);
            out.put("territorialScope", territorialScope);
            out.put("coverageMode", coverageMode);
            out.put("jurisdictionCode", jurisdictionCode);
            out.put("notes", notes);
            out.put("metadata", metadata);
            out.entrySet().removeIf(entry -> entry.getValue() == null);
            return out;
        }
    }
}
