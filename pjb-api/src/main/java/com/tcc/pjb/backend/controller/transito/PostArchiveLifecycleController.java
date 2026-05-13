package com.tcc.pjb.backend.controller.transito;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.tcc.pjb.backend.model.dto.api.ApiCommandResponse;
import com.tcc.pjb.backend.model.dto.transito.PostArchiveLifecycleRequest;
import com.tcc.pjb.backend.model.dto.transito.PostArchiveLifecycleResponse;
import com.tcc.pjb.backend.service.api.ApiResponseFactory;
import com.tcc.pjb.backend.service.processual.postarchive.PostArchiveLifecycleService;

@RestController
@RequestMapping("/api/v1/transito/post-arquivamento")
@PreAuthorize("hasAnyRole('JUIZ','MAGISTRADO','SERVIDOR','SERVIDOR_FORUM','ADVOGADO','PROCURADOR','ADMINISTRADOR','ADMIN')")
public class PostArchiveLifecycleController {

    private final PostArchiveLifecycleService service;
    private final ApiResponseFactory responseFactory;

    public PostArchiveLifecycleController(PostArchiveLifecycleService service,
                                          ApiResponseFactory responseFactory) {
        this.service = service;
        this.responseFactory = responseFactory;
    }

    @PostMapping("/avaliar")
    public ResponseEntity<ApiCommandResponse<PostArchiveLifecycleResponse>> avaliar(@Valid @RequestBody PostArchiveLifecycleRequest request) {
        PostArchiveLifecycleResponse response = service.evaluate(request);
        return ResponseEntity.ok(responseFactory.commandOk("Post-arquivamento avaliado.", response, List.of()));
    }
}
