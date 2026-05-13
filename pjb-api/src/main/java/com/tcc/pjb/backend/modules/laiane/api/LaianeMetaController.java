package com.tcc.pjb.backend.modules.laiane.api;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.tcc.pjb.backend.modules.laiane.dto.LaianeMetaDto;
import com.tcc.pjb.backend.modules.laiane.service.LaianeMetaService;

@RestController
@RequestMapping("/api/v1/laiane")
@PreAuthorize("isAuthenticated()")
public class LaianeMetaController {

    private final LaianeMetaService metaService;

    public LaianeMetaController(LaianeMetaService metaService) {
        this.metaService = metaService;
    }

    @GetMapping("/meta")
    public LaianeMetaDto meta() {
        return metaService.meta();
    }
}
