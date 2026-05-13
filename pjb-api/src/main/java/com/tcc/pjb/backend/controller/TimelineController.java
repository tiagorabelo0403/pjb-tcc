package com.tcc.pjb.backend.controller;

import com.tcc.pjb.backend.model.dto.timeline.TimelineItemResponse;
import com.tcc.pjb.backend.service.timeline.surface.TimelineSurfaceFacadeService;
import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/timeline")
@PreAuthorize("isAuthenticated()")
public class TimelineController {

    private final TimelineSurfaceFacadeService timelineSurfaceFacadeService;

    public TimelineController(TimelineSurfaceFacadeService timelineSurfaceFacadeService) {
        this.timelineSurfaceFacadeService = timelineSurfaceFacadeService;
    }

    @GetMapping("/processo/{processoId}")
    @Cacheable(cacheNames = "timeline_processo", key = "#processoId + ':' + @currentUserService.currentUserIdOrZero()", condition = "@cacheRuntime.redisEnabled()")
    public List<TimelineItemResponse> timeline(@PathVariable Long processoId) {
        return timelineSurfaceFacadeService.timeline(processoId);
    }
}
