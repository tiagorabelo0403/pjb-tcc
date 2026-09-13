package com.tcc.pjb.backend.service.secretariat.autopilot;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

public final class PjbSecretariatAutopilotService {

    public List<PjbSecretariatAutopilotTask> prioritize(List<PjbSecretariatAutopilotTask> tasks, Instant now) {
        Instant reference = now == null ? Instant.EPOCH : now;
        return (tasks == null ? List.<PjbSecretariatAutopilotTask>of() : tasks).stream()
                .sorted(Comparator.comparingInt((PjbSecretariatAutopilotTask task) -> score(task, reference)).reversed()
                        .thenComparing(PjbSecretariatAutopilotTask::dueAt))
                .toList();
    }

    public int score(PjbSecretariatAutopilotTask task, Instant now) {
        if (task == null) {
            return 0;
        }
        int score = task.urgency() + Math.min(30, task.queueAgeDays());
        if (task.legallyPreferred()) {
            score += 20;
        }
        if (task.type() == PjbSecretariatAutopilotTaskType.DEADLINE || task.type() == PjbSecretariatAutopilotTaskType.URGENT_CASE) {
            score += 25;
        }
        if (task.dueAt().isBefore(now)) {
            score += 30;
        } else if (Duration.between(now, task.dueAt()).toHours() <= 24) {
            score += 18;
        }
        return score;
    }
}
