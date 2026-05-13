package com.tcc.pjb.backend.controller.admin;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.tcc.pjb.backend.model.dto.admin.RitoFeedbackRequest;
import com.tcc.pjb.backend.model.dto.admin.RitoFeedbackResponse;
import com.tcc.pjb.backend.service.rito.RitoFeedbackService;







@RestController
@RequestMapping("/api/admin/ritos")
public class AdminRitoFeedbackController {

    private final RitoFeedbackService feedbackService;

    public AdminRitoFeedbackController(RitoFeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping("/feedback")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RitoFeedbackResponse> feedback(@Valid @RequestBody RitoFeedbackRequest request) {
        return ResponseEntity.ok(feedbackService.registerFeedback(request));
    }
}
