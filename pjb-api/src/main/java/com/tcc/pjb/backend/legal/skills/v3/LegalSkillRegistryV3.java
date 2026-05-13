package com.tcc.pjb.backend.legal.skills.v3;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class LegalSkillRegistryV3 {

    private final List<LegalSkillV3> skills;
    private final Map<String, LegalSkillV3> byId;

    public LegalSkillRegistryV3(Collection<? extends LegalSkillV3> skills) {
        Objects.requireNonNull(skills, "skills");
        this.skills = skills.stream()
                .filter(Objects::nonNull)
                .map(skill -> (LegalSkillV3) skill)
                .sorted(Comparator.comparing(LegalSkillV3::id, String.CASE_INSENSITIVE_ORDER))
                .toList();
        this.byId = new ConcurrentHashMap<>();
        for (LegalSkillV3 s : this.skills) {
            this.byId.putIfAbsent(s.id().toUpperCase(), s);
        }
    }

    public Optional<LegalSkillV3> findById(String id) {
        if (id == null || id.isBlank()) return Optional.empty();
        return Optional.ofNullable(byId.get(id.toUpperCase()));
    }

    public Optional<LegalSkillV3> route(LegalSkillRequestV3 request) {
        if (request == null) return Optional.empty();
        var direct = findById(request.getSkill());
        if (direct.isPresent() && direct.get().supports(request)) {
            return direct;
        }
        return skills.stream().filter(s -> s.supports(request)).findFirst();
    }

    public LegalSkillResponseV3 execute(LegalSkillRequestV3 request, Map<String, Object> context) {
        var skill = route(request).orElse(null);
        if (skill == null) {
            return LegalSkillResponseV3.error(request, "Nenhuma LegalSkillV3 encontrada para skill='" + (request.getSkill()) + "'.");
        }
        return skill.execute(request, context);
    }

    public List<LegalSkillV3> list() {
        return skills;
    }
}
