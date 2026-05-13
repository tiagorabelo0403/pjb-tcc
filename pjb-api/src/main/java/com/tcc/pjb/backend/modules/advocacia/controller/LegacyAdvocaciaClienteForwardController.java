package com.tcc.pjb.backend.modules.advocacia.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.view.RedirectView;

@Controller
@ConditionalOnProperty(prefix = "pjb.api.legacy-paths", name = "enabled", havingValue = "true")
@RequestMapping("/com/tcc/pjb/backend/api/modulos/advocacia/clientes")
@PreAuthorize("permitAll()")
public class LegacyAdvocaciaClienteForwardController {

    @RequestMapping(
            value = {"", "/"},
            method = {RequestMethod.GET, RequestMethod.POST})
    public RedirectView forwardLegacy(HttpServletRequest request) {
        String query = request.getQueryString();
        String target = "/api/v1/advocacia/clientes";
        return new RedirectView(query != null ? target + "?" + query : target, true);
    }
}
