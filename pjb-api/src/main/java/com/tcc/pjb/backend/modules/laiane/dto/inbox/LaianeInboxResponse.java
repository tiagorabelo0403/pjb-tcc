package com.tcc.pjb.backend.modules.laiane.dto.inbox;

import java.util.ArrayList;
import java.util.List;
import com.tcc.pjb.backend.model.dto.workitem.WorkItemDto;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaianeInboxResponse {

    @Builder.Default
    private List<WorkItemDto> workItems = new ArrayList<>();

    @Builder.Default
    private List<LaianeNotificationDto> notifications = new ArrayList<>();
}
