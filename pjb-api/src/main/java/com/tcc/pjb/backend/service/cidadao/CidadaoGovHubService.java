package com.tcc.pjb.backend.service.cidadao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.cidadao.CidadaoGovHubCategoriaDto;
import com.tcc.pjb.backend.model.dto.cidadao.CidadaoGovHubDto;
import com.tcc.pjb.backend.model.dto.cidadao.CidadaoGovHubItemDto;
import com.tcc.pjb.backend.model.dto.cidadao.CidadaoGovHubRequisitosDto;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.gov.GovServiceRegistry;
import com.tcc.pjb.backend.model.entity.gov.GovServiceType;
import com.tcc.pjb.backend.model.repository.EstadosRepository;
import com.tcc.pjb.backend.repository.gov.GovServiceRegistryRepository;

@Service
public class CidadaoGovHubService {

    private final GovServiceRegistryRepository repo;
    private final ObjectMapper mapper;
    private final CurrentUserService currentUser;
    private final EstadosRepository estadosRepository;

    public CidadaoGovHubService(GovServiceRegistryRepository repo, ObjectMapper mapper, CurrentUserService currentUser, EstadosRepository estadosRepository) {
        this.repo = Objects.requireNonNull(repo);
        this.mapper = Objects.requireNonNull(mapper);
        this.currentUser = Objects.requireNonNull(currentUser);
        this.estadosRepository = Objects.requireNonNull(estadosRepository);
    }

    public CidadaoGovHubDto hubForCurrentUser() {
        Usuario u = currentUser.getRequired();
        String uf = normalizeUf(u != null ? u.getUf() : null);
        return hubForUf(uf);
    }

    @Cacheable(cacheNames = "govHubByUf", key = "#uf")
    public CidadaoGovHubDto hubForUf(String uf) {
        String u = normalizeUf(uf);
        List<String> ufs = new ArrayList<>();
        ufs.add(u);
        if (!"BR".equals(u)) {
            ufs.add("BR");
        }

        List<GovServiceRegistry> entries = repo.findEnabledByUfs(ufs);
        Map<String, List<GovServiceRegistry>> byCat = new LinkedHashMap<>();
        for (GovServiceRegistry e : entries) {
            if (e == null) continue;
            String cat = normCat(e.getCategory());
            byCat.computeIfAbsent(cat, k -> new ArrayList<>()).add(e);
        }

        List<CidadaoGovHubCategoriaDto> categories = new ArrayList<>();
        for (Map.Entry<String, List<GovServiceRegistry>> kv : byCat.entrySet()) {
            String cat = kv.getKey();
            List<GovServiceRegistry> list = kv.getValue();
            list.sort(Comparator.comparing(GovServiceRegistry::getName, Comparator.nullsLast(String::compareToIgnoreCase)));
            List<CidadaoGovHubItemDto> items = new ArrayList<>();
            for (GovServiceRegistry e : list) {
                items.add(new CidadaoGovHubItemDto(
                        e.getName(),
                        effectiveServiceType(e).externalValue(),
                        e.getUrl(),
                        requisitos(e.getRequirementsJson())
                ));
            }
            categories.add(new CidadaoGovHubCategoriaDto(cat, titleForCat(cat), List.copyOf(items)));
        }

        return new CidadaoGovHubDto(u, List.copyOf(categories));
    }

    private static GovServiceType effectiveServiceType(GovServiceRegistry entry) {
        if (entry == null) {
            return GovServiceType.defaultValue();
        }
        return entry.getServiceType() != null ? entry.getServiceType() : GovServiceType.defaultValue();
    }

    private CidadaoGovHubRequisitosDto requisitos(String json) {
        if (json == null || json.isBlank()) {
            return new CidadaoGovHubRequisitosDto(false, null, false, List.of());
        }
        try {
            Map<?, ?> raw = mapper.readValue(json, Map.class);
            boolean requiresGov = bool(raw.get("requiresGovBr"));
            String min = str(raw.get("minGovBrLevel"));
            boolean stepUp = bool(raw.get("stepUp"));
            List<String> checklist = listOfStrings(raw.get("checklist"));
            return new CidadaoGovHubRequisitosDto(requiresGov, min, stepUp, checklist);
        } catch (Exception ex) {
            return new CidadaoGovHubRequisitosDto(false, null, false, List.of());
        }
    }

    private static boolean bool(Object o) {
        if (o == null) return false;
        if (o instanceof Boolean b) return b;
        String s = String.valueOf(o).trim().toLowerCase(Locale.ROOT);
        return "true".equals(s) || "1".equals(s) || "yes".equals(s) || "sim".equals(s);
    }

    private static String str(Object o) {
        if (o == null) return null;
        String s = String.valueOf(o).trim();
        return s.isEmpty() ? null : s;
    }

    private static List<String> listOfStrings(Object o) {
        if (o == null) return List.of();
        if (o instanceof Collection<?> c) {
            List<String> out = new ArrayList<>();
            for (Object x : c) {
                String s = str(x);
                if (s != null) out.add(s);
            }
            return List.copyOf(out);
        }
        String s = str(o);
        return s == null ? List.of() : List.of(s);
    }

    private String normalizeUf(String uf) {
        if (uf == null || uf.isBlank()) return "BR";
        String u = uf.trim().toUpperCase(Locale.ROOT);
        if (u.length() != 2) return "BR";
        return estadosRepository.existsByUfIgnoreCaseAndAtivoTrue(u) ? u : "BR";
    }

    private static String normCat(String cat) {
        if (cat == null || cat.isBlank()) return "OUTROS";
        return cat.trim().toUpperCase(Locale.ROOT);
    }

    private static String titleForCat(String cat) {
        Map<String, String> t = new HashMap<>();
        t.put("BO", "Boletim de Ocorrência");
        t.put("INSS", "INSS");
        t.put("RECEITA", "Receita Federal");
        t.put("FAZENDA", "Fazenda");
        t.put("CARTORIO", "Cartórios");
        t.put("DEFENSORIA", "Defensoria");
        t.put("MP", "Ministério Público");
        t.put("OUTROS", "Outros serviços");
        return t.getOrDefault(cat, cat);
    }
}
