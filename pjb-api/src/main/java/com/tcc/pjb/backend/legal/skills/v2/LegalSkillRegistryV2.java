package com.tcc.pjb.backend.legal.skills.v2;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class LegalSkillRegistryV2 {

    private final List<LegalSkillV2> skills;
    private final Map<String, LegalSkillV2> byId;

    public LegalSkillRegistryV2(Collection<? extends LegalSkillV2> skills) {
        Objects.requireNonNull(skills, "skills");
        this.skills = skills.stream()
                .filter(Objects::nonNull)
                .map(skill -> (LegalSkillV2) skill)
                .sorted(Comparator.comparing(LegalSkillV2::id, String.CASE_INSENSITIVE_ORDER))
                .toList();
        this.byId = new ConcurrentHashMap<>();
        for (LegalSkillV2 s : this.skills) {
            this.byId.putIfAbsent(s.id().toUpperCase(), s);
        }
    }

    public Optional<LegalSkillV2> findById(String id) {
        if (id == null || id.isBlank()) return Optional.empty();
        return Optional.ofNullable(byId.get(id.toUpperCase()));
    }

    public Optional<LegalSkillV2> route(LegalSkillRequestV2 request) {
        if (request == null) return Optional.empty();
        
        var direct = findById(request.getSkill());
        if (direct.isPresent() && direct.get().supports(request)) {
            return direct;
        }
        
        return skills.stream().filter(s -> s.supports(request)).findFirst();
    }

    public LegalSkillResponseV2 execute(LegalSkillRequestV2 request, Map<String, Object> context) {
        var skill = route(request).orElse(null);
        if (skill == null) {
            return LegalSkillResponseV2.error(request, "Nenhuma LegalSkillV2 encontrada para skill='" + (request.getSkill()) + "'.");
        }
        return skill.execute(request, context);
    }

    public List<LegalSkillV2> list() {
        return skills;
    }
}
