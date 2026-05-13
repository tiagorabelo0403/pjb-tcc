package com.tcc.pjb.backend.core.processo.lifecycle;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import com.tcc.pjb.backend.core.processo.lifecycle.civel.CivilLifecyclePack;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;

@Component
public class RitoLifecyclePackRegistry {

    private final List<RitoLifecyclePack> packs;
    private final RitoLifecyclePack fallbackPack;

    public RitoLifecyclePackRegistry(List<RitoLifecyclePack> packs) {
        this.packs = List.copyOf(Objects.requireNonNull(packs));
        this.fallbackPack = this.packs.stream()
                .filter(pack -> pack instanceof CivilLifecyclePack)
                .findFirst()
                .orElseGet(() -> this.packs.stream().min(Comparator.comparing(pack -> pack.grupoPrincipal().name())).orElseThrow());
    }

    public RitoLifecyclePack resolve(RitoProcessual rito) {
        if (rito == null) {
            return fallbackPack;
        }
        return packs.stream()
                .filter(pack -> pack.supports(rito))
                .findFirst()
                .orElse(fallbackPack);
    }
}
