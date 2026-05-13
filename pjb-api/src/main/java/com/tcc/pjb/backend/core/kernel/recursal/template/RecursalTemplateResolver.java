package com.tcc.pjb.backend.core.kernel.recursal.template;

import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.kernel.recursal.context.ProceduralContext;

@Service
public class RecursalTemplateResolver {

    private final List<RecursalTemplate> ordered;

    public RecursalTemplateResolver(List<RecursalTemplate> templates) {
        this.ordered = templates.stream()
                .sorted(Comparator.comparingInt(RecursalTemplate::priority).reversed())
                .toList();
    }

    public RecursalTemplate resolve(ProceduralContext ctx) {
        for (RecursalTemplate t : ordered) {
            if (t.supports(ctx)) return t;
        }

        if (!ordered.isEmpty()) return ordered.getLast();
        throw new IllegalStateException("Nenhum RecursalTemplate registrado");
    }
}
