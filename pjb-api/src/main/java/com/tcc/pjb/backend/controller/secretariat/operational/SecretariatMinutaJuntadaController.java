package com.tcc.pjb.backend.controller.secretariat.operational;


import com.tcc.pjb.backend.core.operational.OperationalApiRoutes;
import com.tcc.pjb.backend.model.dto.secretariat.surface.SecretariatJuntadaItemResponse;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.service.secretariat.export.SecretariatMinutaJuntadaPdfService;
import com.tcc.pjb.backend.service.secretariat.surface.SecretariatOperationalSurfaceFacadeService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(OperationalApiRoutes.SECRETARIAT_BASE)
@PreAuthorize("hasAnyAuthority('ROLE_SERVIDOR_JUDICIARIO','ROLE_ADMINISTRADOR')")
public class SecretariatMinutaJuntadaController {

    private final SecretariatMinutaJuntadaPdfService pdf;
    private final SecretariatOperationalSurfaceFacadeService facadeService;

    public SecretariatMinutaJuntadaController(SecretariatMinutaJuntadaPdfService pdf,
                                              SecretariatOperationalSurfaceFacadeService facadeService) {
        this.pdf = pdf;
        this.facadeService = facadeService;
    }

    @GetMapping(value = OperationalApiRoutes.PATH_SECRETARIAT_PROCESSO_MINUTA_JUNTADA_PDF, produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> minuta(@PathVariable("processoId") Long processoId,
                                         @RequestParam(value = "seq", required = false) Long seq,
                                         HttpServletRequest request) {
        if (request != null) {
            request.setAttribute("PJB_PROCESSO_ID", processoId);
        }
        var result = pdf.gerarMinuta(processoId, seq);
        String filename = ("minuta_juntada_processo_" + processoId + "_seq_" + result.seqUsed() + ".pdf")
                .replaceAll("[^0-9A-Za-z._-]", "_");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        headers.setContentLength(result.pdfBytes().length);
        NivelSigilo sigilo = result.effectiveSecrecy();
        if (sigilo != null && sigilo.exigeCredencial()) {
            headers.setCacheControl("no-store");
            headers.setPragma("no-cache");
        }
        return ResponseEntity.ok().headers(headers).body(result.pdfBytes());
    }

    @GetMapping(value = OperationalApiRoutes.PATH_SECRETARIAT_PROCESSO_JUNTADAS, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<SecretariatJuntadaItemResponse> listarJuntadas(@PathVariable("processoId") Long processoId,
                                                               @RequestParam(value = "limit", required = false) Integer limit) {
        return facadeService.listarJuntadas(processoId, limit);
    }
}
