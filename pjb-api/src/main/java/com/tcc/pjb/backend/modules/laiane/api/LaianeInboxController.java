package com.tcc.pjb.backend.modules.laiane.api;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.tcc.pjb.backend.modules.laiane.dto.inbox.LaianeInboxResponse;
import com.tcc.pjb.backend.modules.laiane.service.LaianeInboxService;

@RestController
@RequestMapping("/api/v1/laiane")
@PreAuthorize("isAuthenticated()")
public class LaianeInboxController {

    private final LaianeInboxService inboxService;

    public LaianeInboxController(LaianeInboxService inboxService) {
        this.inboxService = inboxService;
    }

    @GetMapping("/inbox")
    public LaianeInboxResponse inbox() {
        return inboxService.inbox();
    }
}
