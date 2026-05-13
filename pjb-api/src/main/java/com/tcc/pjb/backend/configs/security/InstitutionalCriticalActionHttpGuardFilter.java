package com.tcc.pjb.backend.configs.security;

import com.tcc.pjb.backend.core.comunicacao.institucional.InstitutionalRequestContextKeys;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalDocumentSecurityGateApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalDocumentSecurityGate;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalSensitiveAct;
import com.tcc.pjb.backend.service.exception.RegraNegocioException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 35)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class InstitutionalCriticalActionHttpGuardFilter extends OncePerRequestFilter {

    private static final String HEADER_GATE_OPERATION = "X-PJB-Institutional-Gate-Operation";
    private static final String HEADER_GATE_ENFORCED = "X-PJB-Institutional-Gate-Enforced";
    private static final String HEADER_GATE_ALLOWED = "X-PJB-Institutional-Gate-Allowed";
    private static final String HEADER_GATE_BLOCKED = "X-PJB-Institutional-Gate-Blocked";

    private final ObjectProvider<InstitutionalDocumentSecurityGateApplicationService> gateApplicationServiceProvider;

    public InstitutionalCriticalActionHttpGuardFilter(ObjectProvider<InstitutionalDocumentSecurityGateApplicationService> gateApplicationServiceProvider) {
        this.gateApplicationServiceProvider = gateApplicationServiceProvider;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return resolvePolicy(request) == null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        GuardPolicy policy = resolvePolicy(request);
        if (policy == null) {
            filterChain.doFilter(request, response);
            return;
        }
        InstitutionalDocumentSecurityGateApplicationService gateApplicationService = gateApplicationServiceProvider.getIfAvailable();
        if (gateApplicationService == null) {
            filterChain.doFilter(request, response);
            return;
        }
        String unitCode = firstNonBlank(
                request.getHeader("X-PJB-Institutional-Unit-Code"),
                request.getParameter("unidadeCodigo"),
                stringAttribute(request, InstitutionalRequestContextKeys.ATTR_UNIDADE_CODIGO));
        String boxCode = firstNonBlank(
                request.getHeader("X-PJB-Institutional-Box-Code"),
                request.getParameter("caixaCodigo"),
                stringAttribute(request, InstitutionalRequestContextKeys.ATTR_CAIXA_CODIGO));
        try {
            InstitutionalDocumentSecurityGate gate = gateApplicationService.enforce(unitCode, boxCode, policy.act(), policy.operationCode(), true);
            bindHeaders(response, gate);
            filterChain.doFilter(request, response);
        } catch (RegraNegocioException ex) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(buildForbiddenBody(policy.operationCode(), ex.getMessage()));
        }
    }

    private void bindHeaders(HttpServletResponse response, InstitutionalDocumentSecurityGate gate) {
        response.setHeader(HEADER_GATE_OPERATION, safe(gate.operationCode()));
        response.setHeader(HEADER_GATE_ENFORCED, Boolean.toString(gate.enforced()));
        response.setHeader(HEADER_GATE_ALLOWED, Boolean.toString(gate.allowed()));
        response.setHeader(HEADER_GATE_BLOCKED, Boolean.toString(gate.blocked()));
    }

    private GuardPolicy resolvePolicy(HttpServletRequest request) {
        String method = safe(request.getMethod());
        String path = normalizePath(request.getRequestURI());
        if (!"POST".equals(method)) {
            return null;
        }
        if (path.startsWith("/api/v1/mp/manifestacao/")) {
            return new GuardPolicy(InstitutionalSensitiveAct.ASSINAR_MANIFESTACAO, "MP_MANIFESTACAO");
        }
        if (path.startsWith("/api/v1/mp/parecer/")) {
            return new GuardPolicy(InstitutionalSensitiveAct.ASSINAR_MANIFESTACAO, "MP_PARECER");
        }
        if (path.startsWith("/api/v1/mp/recurso/")) {
            return new GuardPolicy(InstitutionalSensitiveAct.PETICIONAR_EM_NOME_DO_ORGAO, "MP_RECURSO");
        }
        if (path.startsWith("/api/v1/mp/requisicao/diligencia/")) {
            return new GuardPolicy(InstitutionalSensitiveAct.ASSINAR_MANIFESTACAO, "MP_REQUISICAO_DILIGENCIA");
        }
        if ("/api/v1/extrajudicial/escrituras".equals(path)) {
            return new GuardPolicy(InstitutionalSensitiveAct.ASSINAR_MANIFESTACAO, "EXTRAJUDICIAL_LAVRAR_ESCRITURA");
        }
        if (path.contains("/vincular-processo/")) {
            return new GuardPolicy(InstitutionalSensitiveAct.PETICIONAR_EM_NOME_DO_ORGAO, "EXTRAJUDICIAL_VINCULAR_PROCESSO");
        }
        if (path.endsWith("/parecer") && path.startsWith("/api/v1/psicossocial/processos/")) {
            return new GuardPolicy(InstitutionalSensitiveAct.ASSINAR_MANIFESTACAO, "PSICOSSOCIAL_PARECER");
        }
        if (path.endsWith("/relatorio") && path.startsWith("/api/v1/psicossocial/processos/")) {
            return new GuardPolicy(InstitutionalSensitiveAct.ASSINAR_MANIFESTACAO, "PSICOSSOCIAL_RELATORIO");
        }
        if (path.endsWith("/redistribuicao") && path.startsWith("/api/v1/distribuicao/processual/processos/")) {
            return new GuardPolicy(InstitutionalSensitiveAct.REDISTRIBUICAO_SENSIVEL, "DISTRIBUICAO_REDISCRITICA");
        }
        if (path.endsWith("/redistribuicao") && path.startsWith("/api/v1/secretaria/especializada/processos/")) {
            return new GuardPolicy(InstitutionalSensitiveAct.REDISTRIBUICAO_SENSIVEL, "SECRETARIA_REDISCRITICA");
        }
        if (path.startsWith("/api/v1/procuradoria/operacional/processos/") && path.endsWith("/recurso")) {
            return new GuardPolicy(InstitutionalSensitiveAct.PETICIONAR_EM_NOME_DO_ORGAO, "PROCURADORIA_RECURSO");
        }
        if (path.startsWith("/api/v1/procuradoria/operacional/processos/") && path.endsWith("/parecer")) {
            return new GuardPolicy(InstitutionalSensitiveAct.ASSINAR_MANIFESTACAO, "PROCURADORIA_PARECER");
        }
        if (path.startsWith("/api/v1/defensor/recurso/")) {
            return new GuardPolicy(InstitutionalSensitiveAct.PETICIONAR_EM_NOME_DO_ORGAO, "DEFENSORIA_RECURSO");
        }
        if (path.startsWith("/api/v1/perito/operacional/processos/") && path.endsWith("/laudo")) {
            return new GuardPolicy(InstitutionalSensitiveAct.ASSINAR_MANIFESTACAO, "PERITO_LAUDO");
        }
        if (path.startsWith("/api/v1/perito/laudos/") && path.endsWith("/entrega")) {
            return new GuardPolicy(InstitutionalSensitiveAct.ASSINAR_MANIFESTACAO, "PERITO_ENTREGA_LAUDO");
        }
        if (path.startsWith("/api/v1/oficial-justica/processos/") && path.endsWith("/oficios")) {
            return new GuardPolicy(InstitutionalSensitiveAct.PETICIONAR_EM_NOME_DO_ORGAO, "OFICIAL_OFICIO");
        }
        if (path.startsWith("/api/v1/oficial-justica/processos/") && path.endsWith("/oficios/resposta")) {
            return new GuardPolicy(InstitutionalSensitiveAct.ASSINAR_MANIFESTACAO, "OFICIAL_RESPOSTA_OFICIO");
        }
        if (path.contains("/mandados/") && path.endsWith("/certidoes/auto")) {
            return new GuardPolicy(InstitutionalSensitiveAct.ASSINAR_MANIFESTACAO, "OFICIAL_CERTIDAO_AUTO");
        }
        if (path.contains("/mandados/") && path.endsWith("/formalizacao-processual")) {
            return new GuardPolicy(InstitutionalSensitiveAct.PETICIONAR_EM_NOME_DO_ORGAO, "OFICIAL_FORMALIZACAO_PROCESSUAL");
        }
        if (path.contains("/mandados/") && path.endsWith("/anexacoes-institucionais")) {
            return new GuardPolicy(InstitutionalSensitiveAct.PETICIONAR_EM_NOME_DO_ORGAO, "OFICIAL_ANEXACAO_INSTITUCIONAL");
        }
        if (path.contains("/mandados/") && path.contains("/malha-institucional/expedicoes")) {
            return new GuardPolicy(InstitutionalSensitiveAct.PETICIONAR_EM_NOME_DO_ORGAO, "OFICIAL_DISPATCH_INSTITUCIONAL");
        }
        if (path.contains("/mandados/") && path.endsWith("/encerramento-soberano")) {
            return new GuardPolicy(InstitutionalSensitiveAct.ASSINAR_MANIFESTACAO, "OFICIAL_ENCERRAMENTO_SOBERANO");
        }
        if (path.startsWith("/api/v1/delegado/requisicao/diligencia")) {
            return new GuardPolicy(InstitutionalSensitiveAct.ASSINAR_MANIFESTACAO, "DELEGADO_REQUISICAO_DILIGENCIA");
        }
        if (path.contains("/diligencias/") && path.endsWith("/certidoes/auto")) {
            return new GuardPolicy(InstitutionalSensitiveAct.ASSINAR_MANIFESTACAO, "DELEGADO_CERTIDAO_AUTO");
        }
        if (path.contains("/diligencias/") && path.contains("/malha-institucional/expedicoes")) {
            return new GuardPolicy(InstitutionalSensitiveAct.PETICIONAR_EM_NOME_DO_ORGAO, "DELEGADO_DISPATCH_INSTITUCIONAL");
        }
        if (path.contains("/diligencias/") && path.endsWith("/formalizacao-processual")) {
            return new GuardPolicy(InstitutionalSensitiveAct.PETICIONAR_EM_NOME_DO_ORGAO, "DELEGADO_FORMALIZACAO_PROCESSUAL");
        }
        if (path.contains("/diligencias/") && path.endsWith("/anexacoes-institucionais")) {
            return new GuardPolicy(InstitutionalSensitiveAct.PETICIONAR_EM_NOME_DO_ORGAO, "DELEGADO_ANEXACAO_INSTITUCIONAL");
        }
        if (path.startsWith("/api/v1/juiz/gabinete-decisoes/processos/") && path.endsWith("/despacho")) {
            return new GuardPolicy(InstitutionalSensitiveAct.ASSINAR_MANIFESTACAO, "JUIZ_DESPACHO");
        }
        if (path.startsWith("/api/v1/juiz/gabinete-decisoes/processos/") && path.endsWith("/sentenca")) {
            return new GuardPolicy(InstitutionalSensitiveAct.ASSINAR_MANIFESTACAO, "JUIZ_SENTENCA");
        }
        if (path.startsWith("/api/v1/juiz/gabinete-decisoes/processos/") && path.endsWith("/certidao-tj")) {
            return new GuardPolicy(InstitutionalSensitiveAct.ASSINAR_MANIFESTACAO, "JUIZ_CERTIDAO_TJ");
        }
        if (path.startsWith("/api/v1/magistratura/processos/") && path.endsWith("/atos")) {
            return new GuardPolicy(InstitutionalSensitiveAct.ASSINAR_MANIFESTACAO, "MAGISTRATURA_ATO_PROCESSUAL");
        }
        if (path.startsWith("/api/v1/processo/transito-julgado/processos/") && path.endsWith("/certidao")) {
            return new GuardPolicy(InstitutionalSensitiveAct.ASSINAR_MANIFESTACAO, "TRANSITO_CERTIDAO");
        }
        if (path.startsWith("/api/v1/processo/transito-julgado/processos/") && path.endsWith("/homologacao-expropriacao")) {
            return new GuardPolicy(InstitutionalSensitiveAct.ASSINAR_MANIFESTACAO, "TRANSITO_HOMOLOGACAO_EXPROPRIACAO");
        }
        return null;
    }

    private String buildForbiddenBody(String operationCode, String message) {
        LinkedHashMap<String, String> payload = new LinkedHashMap<>();
        payload.put("status", "INSTITUTIONAL_DOCUMENT_GATE_BLOCKED");
        payload.put("operationCode", safe(operationCode));
        payload.put("message", safe(message));
        StringBuilder out = new StringBuilder();
        out.append('{');
        boolean first = true;
        for (Map.Entry<String, String> entry : payload.entrySet()) {
            if (!first) {
                out.append(',');
            }
            first = false;
            out.append('"').append(entry.getKey()).append('"').append(':').append('"').append(escape(entry.getValue())).append('"');
        }
        out.append('}');
        return out.toString();
    }

    private String normalizePath(String path) {
        return safe(path).toLowerCase(Locale.ROOT).replaceAll("/+", "/");
    }

    private String stringAttribute(HttpServletRequest request, String key) {
        Object value = request.getAttribute(key);
        return value == null ? null : String.valueOf(value);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String escape(String value) {
        return safe(value)
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }

    private record GuardPolicy(InstitutionalSensitiveAct act, String operationCode) {
    }
}
