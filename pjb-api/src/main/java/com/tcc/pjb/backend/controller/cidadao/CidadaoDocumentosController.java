package com.tcc.pjb.backend.controller.cidadao;

import com.tcc.pjb.backend.model.dto.cidadao.CidadaoDocumentoMetaDto;
import com.tcc.pjb.backend.model.dto.pastadigital.PageSearchResponse;
import com.tcc.pjb.backend.model.dto.pastadigital.PastaDigitalResponse;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.cidadao.CidadaoDocumentoDownloadService;
import com.tcc.pjb.backend.service.cidadao.CidadaoDocumentoMetaService;
import com.tcc.pjb.backend.service.pastadigital.PastaDigitalService;
import com.tcc.pjb.backend.service.processo.ProcessoAccessApplicationService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cidadao/processos/{processoId}/documentos")
public class CidadaoDocumentosController {

    private final ProcessoAccessApplicationService processoAccessApplicationService;
    private final PastaDigitalService pastaDigitalService;
    private final CidadaoDocumentoDownloadService downloadService;
    private final CidadaoDocumentoMetaService metaService;
    private final CapabilityRateLimiter rateLimiter;

    public CidadaoDocumentosController(ProcessoAccessApplicationService processoAccessApplicationService,
                                       PastaDigitalService pastaDigitalService,
                                       CidadaoDocumentoDownloadService downloadService,
                                       CidadaoDocumentoMetaService metaService,
                                       CapabilityRateLimiter rateLimiter) {
        this.processoAccessApplicationService = processoAccessApplicationService;
        this.pastaDigitalService = pastaDigitalService;
        this.downloadService = downloadService;
        this.metaService = metaService;
        this.rateLimiter = rateLimiter;
    }

    @GetMapping
    @PreAuthorize("hasRole('CIDADAO')")
    public PastaDigitalResponse pasta(@PathVariable Long processoId) {
        processoAccessApplicationService.loadAndRequireCitizenPartyRead(processoId);
        return pastaDigitalService.pastaDigital(processoId);
    }

    @GetMapping("/busca")
    @PreAuthorize("hasRole('CIDADAO')")
    public PageSearchResponse busca(@PathVariable Long processoId,
                                    @RequestParam("q") String q,
                                    @RequestParam(value = "limit", defaultValue = "20") int limit) {
        processoAccessApplicationService.loadAndRequireCitizenPartyRead(processoId);
        return pastaDigitalService.buscarNoProcesso(processoId, q, limit);
    }

    @GetMapping(value = "/{documentoId}/pdf", produces = "application/pdf")
    @PreAuthorize("hasRole('CIDADAO')")
    public ResponseEntity<Resource> baixarPdf(
            Authentication authentication,
            @PathVariable Long processoId,
            @PathVariable UUID documentoId,
            HttpServletRequest request) {
        rateLimiter.enforce(CapabilityRateLimitDomain.CITIZEN, authentication, "cidadao_documento_pdf", ApiVersion.V1);
        if (request != null) {
            request.setAttribute("PJB_PROCESSO_ID", processoId);
            request.setAttribute("PJB_DOCUMENTO_ID", documentoId.toString());
        }
        return downloadService.baixarPdf(processoId, documentoId);
    }

    @GetMapping("/meta")
    @PreAuthorize("hasRole('CIDADAO')")
    public List<CidadaoDocumentoMetaDto> meta(@PathVariable Long processoId) {
        return metaService.listar(processoId);
    }
}
