package com.tcc.pjb.backend.configs.datasource;

import org.springframework.stereotype.Component;

@Component
public class PjbAdaptiveDataPlaneContext {

    private final ThreadLocal<PjbAdaptiveDataPlaneService.AdaptiveDecision> holder = new ThreadLocal<>();

    public void bind(PjbAdaptiveDataPlaneService.AdaptiveDecision decision) {
        if (decision == null) {
            holder.remove();
            return;
        }
        holder.set(decision);
    }

    public PjbAdaptiveDataPlaneService.AdaptiveDecision current() {
        return holder.get();
    }

    public void clear() {
        holder.remove();
    }
}
