package com.tcc.pjb.backend.modules.laiane.dto.roles.mp;

import java.util.List;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaianeMpAuditResponse {
    private int page;
    private int size;
    private long total;
    private List<LaianeMpAuditEventDto> items;
}
