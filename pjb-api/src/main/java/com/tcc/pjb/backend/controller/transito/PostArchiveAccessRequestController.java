package com.tcc.pjb.backend.controller.transito;

import com.tcc.pjb.backend.model.dto.api.ApiCommandResponse;
import com.tcc.pjb.backend.model.dto.transito.PostArchiveAccessRequest;
import com.tcc.pjb.backend.model.dto.transito.PostArchiveAccessResponse;
import com.tcc.pjb.backend.service.api.ApiResponseFactory;
import com.tcc.pjb.backend.service.processual.postarchive.PostArchiveAccessRequestService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transito/post-arquivamento")
public class PostArchiveAccessRequestController {

    private final PostArchiveAccessRequestService service;
    private final ApiResponseFactory responseFactory;

    public PostArchiveAccessRequestController(PostArchiveAccessRequestService service,
                                              ApiResponseFactory responseFactory) {
        this.service = service;
        this.responseFactory = responseFactory;
    }

    @PostMapping("/solicitar-acesso-controlado")
    @PreAuthorize("hasAnyRole('JUIZ','MAGISTRADO','SERVIDOR','SERVIDOR_FORUM','ADVOGADO','PROCURADOR','ADMINISTRADOR','ADMIN','CIDADAO')")
    public ResponseEntity<ApiCommandResponse<PostArchiveAccessResponse>> solicitar(@Valid @RequestBody PostArchiveAccessRequest request) {
        PostArchiveAccessResponse response = service.solicitar(request);
        return ResponseEntity.ok(responseFactory.commandOk("Solicitação de acesso controlado avaliada.", response, List.of()));
    }
}
