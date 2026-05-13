package com.tcc.pjb.backend.controller.forum;

import com.tcc.pjb.backend.core.operational.OperationalApiRoutes;
import com.tcc.pjb.backend.model.dto.secretariat.oficial.ForumOfficialReturnReactivationRequest;
import com.tcc.pjb.backend.service.forum.ForumOfficialReturnOperationalService;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(OperationalApiRoutes.FORUM_BASE + OperationalApiRoutes.PATH_FORUM_OFFICIAL_RETURNS)
@PreAuthorize("hasAnyAuthority('ROLE_SERVIDOR_JUDICIARIO','ROLE_SERVIDOR_FORUM','ROLE_ADMIN','ROLE_ADMINISTRADOR','ROLE_MAGISTRADO','ROLE_JUIZ')")
public class ForumOfficialReturnController {

    private final ForumOfficialReturnOperationalService service;

    public ForumOfficialReturnController(ForumOfficialReturnOperationalService service) {
        this.service = Objects.requireNonNull(service);
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> inbox(@RequestParam("inboxKey") String inboxKey,
                                                     @RequestParam(defaultValue = "24") int limit) {
        return ResponseEntity.ok(service.inbox(inboxKey, limit));
    }

    @PostMapping(OperationalApiRoutes.PATH_FORUM_OFFICIAL_RETURN_REACTIVATE)
    public ResponseEntity<Map<String, Object>> reativar(@PathVariable Long deskWorkItemId,
                                                        @Valid @RequestBody(required = false) ForumOfficialReturnReactivationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.reativar(deskWorkItemId, request));
    }
}
