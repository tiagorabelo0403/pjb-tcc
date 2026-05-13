package com.tcc.pjb.backend.controller.assessor;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.service.profile.PerfilRealtimeTopicService;
import com.tcc.pjb.backend.service.ui.live.UiHistoryLiveHub;

@RestController
@RequestMapping("/api/v1/assessor/live")
public class AssessorSseController {

    private final UiHistoryLiveHub hub;
    private final CurrentUserService currentUserService;
    private final PerfilRealtimeTopicService topicService;

    public AssessorSseController(UiHistoryLiveHub hub, CurrentUserService currentUserService, PerfilRealtimeTopicService topicService) {
        this.hub = hub;
        this.currentUserService = currentUserService;
        this.topicService = topicService;
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasAnyRole('ASSESSOR_JUDICIAL','ASSESSOR_DESEMBARGADOR','ASSESSOR_MINISTRO','SERVIDOR','SERVIDOR_FORUM')")
    public SseEmitter stream(@RequestHeader(name = "Last-Event-ID", required = false) String lastEventId) {
        Usuario usuario = currentUserService.getRequired();
        String topic = topicService.inboxTopic(usuario, "ASSESSOR");
        if (hub.activeSubscribers(topic) >= 4) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS, "Limite de conexões SSE atingido");
        }
        return hub.register(topic, lastEventId);
    }
}
