package com.tcc.pjb.backend.integration.mni.api;

import com.tcc.pjb.backend.integration.mni.application.MniRecepcaoService;
import com.tcc.pjb.backend.integration.mni.domain.MniRecepcaoCommand;
import com.tcc.pjb.backend.integration.mni.domain.MniRecepcaoJsonPayload;
import com.tcc.pjb.backend.integration.mni.domain.MniRecepcaoRequest;
import com.tcc.pjb.backend.integration.mni.domain.MniRecepcaoResult;
import jakarta.validation.constraints.NotBlank;
import java.util.Objects;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/mni")
public class MniEndpointController {

    private final MniRecepcaoService mniRecepcaoService;

    public MniEndpointController(MniRecepcaoService mniRecepcaoService) {
        this.mniRecepcaoService = Objects.requireNonNull(mniRecepcaoService);
    }

    @PostMapping(value = "/recepcao", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('MAGISTRADO','SERVIDOR_JUDICIAL','ADMIN')")
    public MniRecepcaoResult recepcao(@RequestParam("tribunalOrigem") @NotBlank String tribunalOrigem,
                                     @RequestParam(name = "motivo", required = false) String motivo,
                                     @RequestBody String xml) {
        return mniRecepcaoService.receberAutos(new MniRecepcaoCommand(tribunalOrigem, motivo, xml));
    }

    @PostMapping(value = "/recepcao/json", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('MAGISTRADO','SERVIDOR_JUDICIAL','ADMIN')")
    public MniRecepcaoResult recepcaoJson(@RequestBody MniRecepcaoJsonPayload payload) {
        String tribunalOrigem = payload == null ? null : payload.tribunalOrigem();
        String motivo = payload == null ? null : payload.motivo();
        String xml = payload == null ? null : payload.xml();
        return mniRecepcaoService.receberAutos(new MniRecepcaoCommand(tribunalOrigem, motivo, xml));
    }
}
