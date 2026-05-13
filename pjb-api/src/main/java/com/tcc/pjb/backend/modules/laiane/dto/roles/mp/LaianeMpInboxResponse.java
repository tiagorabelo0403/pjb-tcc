package com.tcc.pjb.backend.modules.laiane.dto.roles.mp;

import java.util.List;
import com.tcc.pjb.backend.modules.laiane.dto.roles.common.LaianeWorkItemLiteDto;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaianeMpInboxResponse {
    private String hint;
    private int page;
    private int size;
    private long total;
    private List<LaianeWorkItemLiteDto> items;
}
