package com.tcc.pjb.backend.core.kernel.recursal.plan;

import java.time.LocalDate;
import java.util.Objects;

public record WorkItemDirective(
        String queue,
        String title,
        String description,
        LocalDate dueDate
) {

    public WorkItemDirective {
        queue = Objects.toString(queue, "").trim();
        title = Objects.toString(title, "").trim();
        description = Objects.toString(description, "").trim();
    }
}
