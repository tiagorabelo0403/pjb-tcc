package com.tcc.pjb.backend.service.processual.calculo;

import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialItemResponse;
import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialResumoResponse;
import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialSolicitantePerfil;
import com.tcc.pjb.backend.model.dto.processual.calculo.CustasProcessuaisCalculoAvancadoRequest;
import com.tcc.pjb.backend.model.dto.processual.calculo.FazendaTributarioCalculoAvancadoRequest;
import com.tcc.pjb.backend.model.dto.processual.calculo.FederalPrevidenciarioCjfCalculoAvancadoRequest;
import com.tcc.pjb.backend.model.dto.processual.calculo.TrabalhistaCalculoAvancadoRequest;
import java.text.Normalizer;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class CalculoJudicialFacadeService {

    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss", Locale.ROOT).withZone(ZoneId.of("America/Fortaleza"));

    private final TrabalhistaCalculoAvancadoService trabalhistaService;
    private final FazendaTributarioCalculoAvancadoService fazendaService;
    private final CustasProcessuaisCalculoAvancadoService custasService;
    private final FederalPrevidenciarioCjfCalculoAvancadoService federalPrevidenciarioService;
    private final CalculoJudicialPdfReportService pdfService;
    private final CalculoJudicialProfileResolverService profileResolverService;
    private final CalculoJudicialGeracaoContextResolverService geracaoContextResolverService;
    private final CalculoJudicialFrontendContractService frontendContractService;

    public CalculoJudicialFacadeService(TrabalhistaCalculoAvancadoService trabalhistaService,
                                        FazendaTributarioCalculoAvancadoService fazendaService,
                                        CustasProcessuaisCalculoAvancadoService custasService,
                                        FederalPrevidenciarioCjfCalculoAvancadoService federalPrevidenciarioService,
                                        CalculoJudicialPdfReportService pdfService,
                                        CalculoJudicialProfileResolverService profileResolverService,
                                        CalculoJudicialGeracaoContextResolverService geracaoContextResolverService,
                                        CalculoJudicialFrontendContractService frontendContractService) {
        this.trabalhistaService = Objects.requireNonNull(trabalhistaService);
        this.fazendaService = Objects.requireNonNull(fazendaService);
        this.custasService = Objects.requireNonNull(custasService);
        this.federalPrevidenciarioService = Objects.requireNonNull(federalPrevidenciarioService);
        this.pdfService = Objects.requireNonNull(pdfService);
        this.profileResolverService = Objects.requireNonNull(profileResolverService);
        this.geracaoContextResolverService = Objects.requireNonNull(geracaoContextResolverService);
        this.frontendContractService = Objects.requireNonNull(frontendContractService);
    }

    public CalculoJudicialResumoResponse calcularTrabalhista(TrabalhistaCalculoAvancadoRequest request, Authentication authentication) {
        CalculoJudicialRelatorio report = calcularTrabalhistaRelatorio(request, authentication);
        return toResponse(report);
    }

    public CalculoJudicialPdfDocument calcularTrabalhistaPdf(TrabalhistaCalculoAvancadoRequest request, Authentication authentication) {
        CalculoJudicialRelatorio report = calcularTrabalhistaRelatorio(request, authentication);
        return new CalculoJudicialPdfDocument(pdfService.render(report), filename(report));
    }

    public CalculoJudicialResumoResponse calcularFazenda(FazendaTributarioCalculoAvancadoRequest request, Authentication authentication) {
        CalculoJudicialRelatorio report = calcularFazendaRelatorio(request, authentication);
        return toResponse(report);
    }

    public CalculoJudicialPdfDocument calcularFazendaPdf(FazendaTributarioCalculoAvancadoRequest request, Authentication authentication) {
        CalculoJudicialRelatorio report = calcularFazendaRelatorio(request, authentication);
        return new CalculoJudicialPdfDocument(pdfService.render(report), filename(report));
    }


    public CalculoJudicialResumoResponse calcularCustas(CustasProcessuaisCalculoAvancadoRequest request, Authentication authentication) {
        CalculoJudicialRelatorio report = calcularCustasRelatorio(request, authentication);
        return toResponse(report);
    }

    public CalculoJudicialPdfDocument calcularCustasPdf(CustasProcessuaisCalculoAvancadoRequest request, Authentication authentication) {
        CalculoJudicialRelatorio report = calcularCustasRelatorio(request, authentication);
        return new CalculoJudicialPdfDocument(pdfService.render(report), filename(report));
    }


    public CalculoJudicialResumoResponse calcularFederalPrevidenciario(FederalPrevidenciarioCjfCalculoAvancadoRequest request, Authentication authentication) {
        CalculoJudicialRelatorio report = calcularFederalPrevidenciarioRelatorio(request, authentication);
        return toResponse(report);
    }

    public CalculoJudicialPdfDocument calcularFederalPrevidenciarioPdf(FederalPrevidenciarioCjfCalculoAvancadoRequest request, Authentication authentication) {
        CalculoJudicialRelatorio report = calcularFederalPrevidenciarioRelatorio(request, authentication);
        return new CalculoJudicialPdfDocument(pdfService.render(report), filename(report));
    }

    private CalculoJudicialRelatorio calcularTrabalhistaRelatorio(TrabalhistaCalculoAvancadoRequest request, Authentication authentication) {
        CalculoJudicialSolicitantePerfil perfil = profileResolverService.resolve(authentication, request.perfilSolicitante());
        CalculoJudicialRelatorio report = trabalhistaService.calcular(request, perfil);
        CalculoJudicialGeracaoContext contexto = geracaoContextResolverService.resolve(
                authentication,
                perfil,
                request.nomeSolicitante(),
                request.registroProfissionalSolicitante(),
                report.dominio(),
                report.titulo(),
                report.numeroProcesso(),
                report.totalGeral(),
                report.geradoEm()
        );
        return enrich(report, contexto);
    }

    private CalculoJudicialRelatorio calcularCustasRelatorio(CustasProcessuaisCalculoAvancadoRequest request, Authentication authentication) {
        CalculoJudicialSolicitantePerfil perfil = profileResolverService.resolve(authentication, request.perfilSolicitante());
        CalculoJudicialRelatorio report = custasService.calcular(request, perfil);
        CalculoJudicialGeracaoContext contexto = geracaoContextResolverService.resolve(
                authentication,
                perfil,
                request.nomeSolicitante(),
                request.registroProfissionalSolicitante(),
                report.dominio(),
                report.titulo(),
                report.numeroProcesso(),
                report.totalGeral(),
                report.geradoEm()
        );
        return enrich(report, contexto);
    }


    private CalculoJudicialRelatorio calcularFederalPrevidenciarioRelatorio(FederalPrevidenciarioCjfCalculoAvancadoRequest request, Authentication authentication) {
        CalculoJudicialSolicitantePerfil perfil = profileResolverService.resolve(authentication, request.perfilSolicitante());
        CalculoJudicialRelatorio report = federalPrevidenciarioService.calcular(request, perfil);
        CalculoJudicialGeracaoContext contexto = geracaoContextResolverService.resolve(
                authentication,
                perfil,
                request.nomeSolicitante(),
                request.registroProfissionalSolicitante(),
                report.dominio(),
                report.titulo(),
                report.numeroProcesso(),
                report.totalGeral(),
                report.geradoEm()
        );
        return enrich(report, contexto);
    }

    private CalculoJudicialRelatorio calcularFazendaRelatorio(FazendaTributarioCalculoAvancadoRequest request, Authentication authentication) {
        CalculoJudicialSolicitantePerfil perfil = profileResolverService.resolve(authentication, request.perfilSolicitante());
        CalculoJudicialRelatorio report = fazendaService.calcular(request, perfil);
        CalculoJudicialGeracaoContext contexto = geracaoContextResolverService.resolve(
                authentication,
                perfil,
                request.nomeSolicitante(),
                request.registroProfissionalSolicitante(),
                report.dominio(),
                report.titulo(),
                report.numeroProcesso(),
                report.totalGeral(),
                report.geradoEm()
        );
        return enrich(report, contexto);
    }

    private CalculoJudicialRelatorio enrich(CalculoJudicialRelatorio report, CalculoJudicialGeracaoContext contexto) {
        Map<String, Object> metadata = report == null || report.metadata() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(report.metadata());
        String dominioCanonico = CalculoJudicialDomainSupport.requireSupported(report.dominio());
        metadata.put("solicitanteNome", contexto.solicitanteNome());
        if (contexto.solicitanteRegistro() != null) {
            metadata.put("solicitanteRegistro", contexto.solicitanteRegistro());
        }
        metadata.put("solicitanteRotulo", contexto.solicitanteRotulo());
        metadata.put("pdfDisplaySolicitante", contexto.solicitanteRotulo() + ": " + contexto.solicitanteNome());
        if (contexto.solicitanteRegistro() != null) {
            metadata.put("pdfDisplaySolicitanteRegistro", contexto.solicitanteRegistro());
        }
        if (contexto.equipeAtivaId() != null) {
            metadata.put("equipeAtivaId", contexto.equipeAtivaId());
        }
        if (contexto.equipeAtivaNome() != null) {
            metadata.put("equipeAtivaNome", contexto.equipeAtivaNome());
            metadata.put("pdfDisplayEquipeAtiva", contexto.equipeAtivaNome());
        }
        if (contexto.equipeAtivaRotulo() != null) {
            metadata.put("equipeAtivaRotulo", contexto.equipeAtivaRotulo());
        }
        metadata.put("hashAuditoriaGeracao", contexto.hashAuditoriaGeracao());
        metadata.put("auditHashAlgorithm", "SHA-256");
        metadata.put("apiRoutes", CalculoJudicialDomainSupport.apiRoutes(dominioCanonico));
        metadata.put("apiAliases", CalculoJudicialDomainSupport.aliases(dominioCanonico));
        metadata.putAll(frontendContractService.frontendMeta(dominioCanonico, report.perfilSolicitante(), "resultado"));
        metadata.put("pdfFilenameSuggested", buildFilename(report, contexto));
        metadata.put("contractVersion", frontendContractService.version());
        metadata.put("contractFingerprint", frontendContractService.fingerprint());
        metadata.put("geracaoContexto", Map.of(
                "solicitanteNome", contexto.solicitanteNome(),
                "solicitanteRegistro", blankIfNull(contexto.solicitanteRegistro()),
                "solicitanteRotulo", contexto.solicitanteRotulo(),
                "equipeAtivaId", contexto.equipeAtivaId() == null ? "" : String.valueOf(contexto.equipeAtivaId()),
                "equipeAtivaNome", blankIfNull(contexto.equipeAtivaNome()),
                "equipeAtivaRotulo", blankIfNull(contexto.equipeAtivaRotulo()),
                "hashAuditoriaGeracao", contexto.hashAuditoriaGeracao(),
                "auditHashAlgorithm", "SHA-256"
        ));
        List<String> trilhaAuditoria = new java.util.ArrayList<>(report.trilhaAuditoria());
        trilhaAuditoria.add("solicitante=" + contexto.solicitanteNome());
        if (contexto.solicitanteRegistro() != null) {
            trilhaAuditoria.add("solicitante_registro=" + contexto.solicitanteRegistro());
        }
        if (contexto.equipeAtivaId() != null) {
            trilhaAuditoria.add("equipe_ativa_id=" + contexto.equipeAtivaId());
        }
        if (contexto.equipeAtivaNome() != null) {
            trilhaAuditoria.add("equipe_ativa_nome=" + contexto.equipeAtivaNome());
        }
        trilhaAuditoria.add("hash_auditoria_geracao=" + contexto.hashAuditoriaGeracao());
        return new CalculoJudicialRelatorio(
                report.dominio(),
                report.titulo(),
                report.numeroProcesso(),
                report.perfilSolicitante(),
                report.narrativaCidadao(),
                report.narrativaTecnica(),
                report.subtotalPrincipal(),
                report.subtotalAtualizacao(),
                report.subtotalAcessorios(),
                report.totalGeral(),
                report.itens(),
                report.alertas(),
                report.fundamentos(),
                List.copyOf(trilhaAuditoria),
                safeMetadata(metadata),
                report.geradoEm()
        );
    }

    private String filename(CalculoJudicialRelatorio report) {
        Object suggested = report.metadata().get("pdfFilenameSuggested");
        if (suggested instanceof String value && !value.isBlank()) {
            return value;
        }
        return buildFilename(report, null);
    }

    private String buildFilename(CalculoJudicialRelatorio report, CalculoJudicialGeracaoContext contexto) {
        String prefix = CalculoJudicialDomainSupport.filenamePrefix(report.dominio());
        String solicitante = contexto == null ? "solicitante" : safeRequiredSegment(contexto.solicitanteNomeArquivo(), 72);
        String processo = safeOptionalSegment(report.numeroProcesso(), 40).replace('.', '-');
        String timestamp = FILE_TS.format(report.geradoEm());
        StringBuilder name = new StringBuilder(prefix).append('-').append(solicitante);
        if (!processo.isBlank()) {
            name.append('-').append(processo);
        }
        name.append('-').append(timestamp).append(".pdf");
        return safeFilename(name.toString());
    }

    private String safeFilename(String filename) {
        String ascii = Normalizer.normalize(blankIfNull(filename), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("[^a-zA-Z0-9._-]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("(^-+|-+$)", "");
        if (ascii.isBlank()) {
            return "pjb-calculo-judicial.pdf";
        }
        String trimmed = ascii.length() <= 140 ? ascii : ascii.substring(0, 140).replaceAll("-+$", "");
        return trimmed.endsWith(".pdf") ? trimmed : trimmed + ".pdf";
    }

    private String safeRequiredSegment(String value, int max) {
        String normalized = normalizeSegment(value, max);
        return normalized.isBlank() ? "solicitante" : normalized;
    }

    private String safeOptionalSegment(String value, int max) {
        return normalizeSegment(value, max);
    }

    private String normalizeSegment(String value, int max) {
        String normalized = Normalizer.normalize(blankIfNull(value), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("(^-+|-+$)", "");
        return normalized.length() <= max ? normalized : normalized.substring(0, max).replaceAll("-+$", "");
    }

    private String blankIfNull(String value) {
        return value == null ? "" : value.trim();
    }

    private CalculoJudicialResumoResponse toResponse(CalculoJudicialRelatorio report) {
        CalculoJudicialSolicitantePerfil perfil = report == null || report.perfilSolicitante() == null ? CalculoJudicialSolicitantePerfil.CIDADAO : report.perfilSolicitante();
        List<CalculoJudicialItemResponse> items = safeList(report == null ? null : report.itens()).stream().map(item -> new CalculoJudicialItemResponse(
                item.secao(),
                item.codigo(),
                item.titulo(),
                item.base(),
                item.quantidade(),
                item.aliquota(),
                item.valor(),
                item.formula(),
                perfil.citizenLike() ? item.explicacaoCidadao() : item.explicacaoTecnica(),
                item.baseLegal()
        )).toList();
        Map<String, Object> metadata = report == null || report.metadata() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(report.metadata());
        metadata.put("itemCount", items.size());
        metadata.put("pdfReady", Boolean.TRUE);
        return new CalculoJudicialResumoResponse(
                report.dominio(),
                report.titulo(),
                report.numeroProcesso(),
                perfil,
                report.narrativaCidadao(),
                report.narrativaTecnica(),
                report.subtotalPrincipal(),
                report.subtotalAtualizacao(),
                report.subtotalAcessorios(),
                report.totalGeral(),
                items,
                safeList(report.alertas()),
                safeList(report.fundamentos()),
                safeList(report.trilhaAuditoria()),
                safeMetadata(metadata),
                report.geradoEm()
        );
    }

    private <T> List<T> safeList(List<T> value) {
        return value == null ? List.of() : List.copyOf(value);
    }

    private Map<String, Object> safeMetadata(Map<String, Object> metadata) {
        Map<String, Object> safe = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
        safe.entrySet().removeIf(entry -> entry.getValue() == null);
        return Map.copyOf(safe);
    }
}
