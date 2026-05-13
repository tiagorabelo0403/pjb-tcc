package com.tcc.pjb.backend.core.kernel.recursal.plan;

import java.util.ArrayList;
import java.util.List;

public record RecursalPlan(
        List<ProceedingUpsert> proceedings,
        List<EdgeUpsert> edges,
        List<SyncDirective> sync,
        List<WorkItemDirective> workItems,
        List<String> notes
) {

    public RecursalPlan {
        proceedings = normalize(proceedings);
        edges = normalize(edges);
        sync = normalize(sync);
        workItems = normalize(workItems);
        notes = normalize(notes);
    }

    private static <T> List<T> normalize(List<T> v) {
        if (v == null || v.isEmpty()) return List.of();
        List<T> copy = new ArrayList<>(v.size());
        for (T t : v) {
            if (t != null) copy.add(t);
        }
        return List.copyOf(copy);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final List<ProceedingUpsert> proceedings = new ArrayList<>();
        private final List<EdgeUpsert> edges = new ArrayList<>();
        private final List<SyncDirective> sync = new ArrayList<>();
        private final List<WorkItemDirective> workItems = new ArrayList<>();
        private final List<String> notes = new ArrayList<>();

        public Builder addProceeding(ProceedingUpsert p) { if (p != null) proceedings.add(p); return this; }
        public Builder addEdge(EdgeUpsert e) { if (e != null) edges.add(e); return this; }
        public Builder addSync(SyncDirective d) { if (d != null) sync.add(d); return this; }
        public Builder addWorkItem(WorkItemDirective w) { if (w != null) workItems.add(w); return this; }
        public Builder note(String n) { if (n != null && !n.isBlank()) notes.add(n.trim()); return this; }

        public RecursalPlan build() {
            return new RecursalPlan(proceedings, edges, sync, workItems, notes);
        }
    }
}
