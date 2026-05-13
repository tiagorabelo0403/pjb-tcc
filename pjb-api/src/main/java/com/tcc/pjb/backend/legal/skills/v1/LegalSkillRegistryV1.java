package com.tcc.pjb.backend.legal.skills.v1;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class LegalSkillRegistryV1 {

    private final List<LegalSkillV1> skills;
    private final Map<String, LegalSkillV1> byId;

    public LegalSkillRegistryV1(Collection<? extends LegalSkillV1> skills) {
        Objects.requireNonNull(skills, "skills");
        this.skills = skills.stream()
                .filter(Objects::nonNull)
                .map(skill -> (LegalSkillV1) skill)
                .sorted(Comparator.comparing(LegalSkillV1::id, String.CASE_INSENSITIVE_ORDER))
                .toList();
        this.byId = new ConcurrentHashMap<>();
        for (LegalSkillV1 s : this.skills) {
            this.byId.putIfAbsent(s.id().toUpperCase(), s);
        }
    }

    public Optional<LegalSkillV1> findById(String id) {
        if (id == null || id.isBlank()) return Optional.empty();
        return Optional.ofNullable(byId.get(id.toUpperCase()));
    }

    public Optional<LegalSkillV1> route(LegalSkillRequestV1 request) {
        if (request == null) return Optional.empty();
        var direct = findById(request.getSkill());
        if (direct.isPresent() && direct.get().supports(request)) {
            return direct;
        }
        return skills.stream().filter(s -> s.supports(request)).findFirst();
    }

    public LegalSkillResponseV1 execute(LegalSkillRequestV1 request, Map<String, Object> context) {
        var skill = route(request).orElse(null);
        if (skill == null) {
            return LegalSkillResponseV1.error(request, "Nenhuma LegalSkillV1 encontrada para skill='" + (request.getSkill()) + "'.");
        }
        return skill.execute(request, context);
    }

    public List<LegalSkillV1> list() {
        return skills;
    }
}
