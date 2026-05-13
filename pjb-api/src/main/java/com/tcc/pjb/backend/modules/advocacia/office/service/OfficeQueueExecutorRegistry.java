package com.tcc.pjb.backend.modules.advocacia.office.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.modules.advocacia.office.entity.OfficeSignatureQueueItem;

@Service
public class OfficeQueueExecutorRegistry {

    private final Map<String, OfficeQueueExecutor> byResourceType;

    public OfficeQueueExecutorRegistry(List<OfficeQueueExecutor> executors) {
        List<OfficeQueueExecutor> list = executors == null ? List.of() : executors;
        this.byResourceType = list.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableMap(
                        e -> e.resourceType().trim(),
                        Function.identity(),
                        (a, b) -> a
                ));
    }

    public void dispatchApproved(OfficeSignatureQueueItem item, Long decidedByUserId, String reason) {
        if (item == null || item.getResourceType() == null) return;
        OfficeQueueExecutor ex = byResourceType.get(item.getResourceType().trim());
        if (ex != null) {
            ex.onApproved(item, decidedByUserId, reason);
        }
    }

    public void dispatchRejected(OfficeSignatureQueueItem item, Long decidedByUserId, String reason) {
        if (item == null || item.getResourceType() == null) return;
        OfficeQueueExecutor ex = byResourceType.get(item.getResourceType().trim());
        if (ex != null) {
            ex.onRejected(item, decidedByUserId, reason);
        }
    }
}
