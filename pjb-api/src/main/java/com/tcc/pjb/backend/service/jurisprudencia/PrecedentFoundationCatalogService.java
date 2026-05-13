package com.tcc.pjb.backend.service.jurisprudencia;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.dto.jurisprudencia.PrecedentFoundationQueryRequest;
import com.tcc.pjb.backend.model.dto.jurisprudencia.PrecedentFoundationResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.jurisprudencia.Precedente;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;

@Service
public class PrecedentFoundationCatalogService {

    private final ProcessoRepository processoRepository;
    private final PjbAuthorizationService authorizationService;
    private final JurisprudenciaService jurisprudenciaService;

    public PrecedentFoundationCatalogService(ProcessoRepository processoRepository,
                                             PjbAuthorizationService authorizationService,
                                             JurisprudenciaService jurisprudenciaService) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.authorizationService = Objects.requireNonNull(authorizationService);
        this.jurisprudenciaService = Objects.requireNonNull(jurisprudenciaService);
    }

    public PrecedentFoundationResponse search(PrecedentFoundationQueryRequest request) {
        Objects.requireNonNull(request);
        Processo processo = request.processoId() == null ? null : processoRepository.findById(request.processoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", request.processoId()));
        if (processo != null) {
            authorizationService.requireReadProcesso(processo);
        }
        RamoDireito ramo = request.ramo() != null ? request.ramo() : processo != null ? processo.getRamoDireito() : null;
        String rito = firstNonBlank(request.rito(), processo != null && processo.getRito() != null ? processo.getRito().name() : null);
        String query = buildQuery(request.consulta(), processo);
        int page = request.page() == null ? 0 : Math.max(0, request.page());
        int size = request.size() == null ? 12 : Math.min(Math.max(1, request.size()), 30);
        Page<Precedente> result = jurisprudenciaService.search(request.fonte(), request.tipo(), ramo, rito, query, page, size);
        Map<String, Long> porFonte = result.getContent().stream()
                .collect(java.util.stream.Collectors.groupingBy(precedente -> precedente.getFonte() != null ? precedente.getFonte().name() : "NAO_INFORMADA", LinkedHashMap::new, java.util.stream.Collectors.counting()));
        Map<String, Long> porTipo = result.getContent().stream()
                .collect(java.util.stream.Collectors.groupingBy(precedente -> precedente.getTipo() != null ? precedente.getTipo().name() : "NAO_INFORMADO", LinkedHashMap::new, java.util.stream.Collectors.counting()));
        List<PrecedentFoundationResponse.Item> precedentes = result.getContent().stream().map(this::toItem).toList();
        List<String> fundamentos = new java.util.ArrayList<>();
        if (processo != null) {
            fundamentos.add("Consulta contextualizada pelo ramo, rito e assunto do processo selecionado.");
        }
        fundamentos.add("Resultados priorizados pelo catálogo interno de jurisprudência e filtros materiais informados.");
        if (!porTipo.isEmpty()) {
            fundamentos.add("Tipologia dominante da amostra: " + porTipo.entrySet().iterator().next().getKey() + '.');
        }
        return new PrecedentFoundationResponse(
                processo != null ? processo.getId() : null,
                processo != null ? processo.getNumeroProcesso() : null,
                query,
                ramo != null ? ramo.name() : null,
                rito,
                result.getTotalElements(),
                porFonte,
                porTipo,
                precedentes,
                fundamentos
        );
    }

    private PrecedentFoundationResponse.Item toItem(Precedente precedente) {
        return new PrecedentFoundationResponse.Item(
                precedente.getId(),
                precedente.getFonte() != null ? precedente.getFonte().name() : null,
                precedente.getTipo() != null ? precedente.getTipo().name() : null,
                precedente.getIdentificador(),
                precedente.getTitulo(),
                precedente.getTese(),
                precedente.getEmentaResumo(),
                precedente.getDataPublicacao() != null ? precedente.getDataPublicacao().toString() : null,
                precedente.getUrlReferencia(),
                precedente.getRamoSugerido() != null ? precedente.getRamoSugerido().name() : null,
                precedente.getRitoSugerido() != null ? precedente.getRitoSugerido().name() : null
        );
    }

    private String buildQuery(String raw, Processo processo) {
        if (raw != null && !raw.isBlank()) {
            return raw.trim();
        }
        if (processo == null) {
            return null;
        }
        return java.util.stream.Stream.of(
                        processo.getAssunto(),
                        processo.getObjetoProcessual(),
                        processo.getPedidoPrincipal(),
                        processo.getClasseProcessual(),
                        processo.getParteAutoraNome(),
                        processo.getParteReuNome())
                .filter(Objects::nonNull)
                .filter(value -> !value.isBlank())
                .limit(3)
                .collect(java.util.stream.Collectors.joining(" "));
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
}
