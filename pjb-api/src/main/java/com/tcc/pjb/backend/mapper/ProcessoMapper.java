package com.tcc.pjb.backend.mapper;

import com.tcc.pjb.backend.core.procedural.CanonicalRitoSelector;
import com.tcc.pjb.backend.core.procedural.CanonicalRitoSelector.SelectedRito;
import com.tcc.pjb.backend.core.procedural.ProceduralCanonicalResolver.CanonicalContext;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.dto.ProcessoRequest;
import com.tcc.pjb.backend.model.dto.ProcessoResponse;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.MateriaJurisdicao;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import jakarta.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.List;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, injectionStrategy = InjectionStrategy.SETTER)
public abstract class ProcessoMapper {

    protected CanonicalRitoSelector canonicalRitoSelector;

    @Resource
    void setCanonicalRitoSelector(CanonicalRitoSelector canonicalRitoSelector) {
        this.canonicalRitoSelector = canonicalRitoSelector;
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "numeroUnificado", expression = "java(java.util.UUID.randomUUID().toString())")
    @Mapping(target = "numeroProcesso", expression = "java(java.util.UUID.randomUUID().toString())")
    @Mapping(target = "dataDistribuicao", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "classeProcessual", source = "classe")
    @Mapping(target = "assunto", source = "assunto")
    @Mapping(target = "objetoProcessual", source = "objetoProcessual")
    @Mapping(target = "pedidoPrincipal", source = "pedidoPrincipal")
    @Mapping(target = "pedidosConsolidados", expression = "java(joinStructuredLines(request.getPedidos()))")
    @Mapping(target = "materialProbatorioResumo", expression = "java(joinStructuredLines(request.getProvas()))")
    @Mapping(target = "resumoIA", expression = "java(resolveResumoInicial(request))")
    @Mapping(target = "materia", expression = "java(resolveMateria(request))")
    @Mapping(target = "ramoDireito", expression = "java(resolveRamoDireito(request))")
    @Mapping(target = "tipoJustica", expression = "java(resolveTipoJustica(request))")
    @Mapping(target = "rito", expression = "java(resolveRito(request))")
    @Mapping(target = "tribunal", source = "tribunalPretendido")
    @Mapping(target = "ufAutor", source = "ufAutor")
    @Mapping(target = "comarcaAutor", source = "comarcaAutor")
    @Mapping(target = "cidadeAutor", source = "cidadeAutor")
    @Mapping(target = "ufReu", source = "ufReu")
    @Mapping(target = "comarcaReu", source = "comarcaReu")
    @Mapping(target = "cidadeReu", source = "cidadeReu")
    public abstract Processo toEntity(ProcessoRequest request);

    @Mapping(target = "numeroProcesso", expression = "java(processo.getNumeroProcesso())")
    @Mapping(target = "classe", source = "classeProcessual")
    @Mapping(target = "materia", expression = "java(processo.getMateria() != null ? processo.getMateria().name() : null)")
    @Mapping(target = "rito", expression = "java(processo.getRito() != null ? processo.getRito().name() : null)")
    public abstract ProcessoResponse toResponse(Processo processo);

    protected MateriaJurisdicao resolveMateria(ProcessoRequest request) {
        MateriaJurisdicao explicit = MateriaJurisdicao.fromString(request.getMateria());
        if (explicit != null && explicit != MateriaJurisdicao.MULTIMATERIA) {
            return explicit;
        }
        SelectedRito selected = resolveSelectedRito(request);
        CanonicalContext context = selected.canonicalContext();
        RamoDireito ramo = context.ramoDireito() == null ? resolveRamoDireito(request) : RamoDireito.fromString(context.ramoDireito());
        return ramo != null ? MateriaJurisdicao.fromRamo(ramo) : MateriaJurisdicao.CIVIL;
    }

    protected RamoDireito resolveRamoDireito(ProcessoRequest request) {
        CanonicalContext context = resolveSelectedRito(request).canonicalContext();
        RamoDireito resolved = context.ramoDireito() == null ? null : RamoDireito.fromString(context.ramoDireito());
        if (resolved != null) {
            return resolved;
        }
        MateriaJurisdicao materia = MateriaJurisdicao.fromString(request.getMateria());
        return materia != null ? materia.getRamo() : RamoDireito.CIVIL;
    }

    protected TipoJustica resolveTipoJustica(ProcessoRequest request) {
        CanonicalContext context = resolveSelectedRito(request).canonicalContext();
        if (context.ramoJusticaNacional() == null || context.ramoJusticaNacional().isBlank()) {
            return TipoJustica.ESTADUAL;
        }
        return switch (context.ramoJusticaNacional()) {
            case "FEDERAL" -> TipoJustica.FEDERAL;
            case "ELEITORAL" -> TipoJustica.ELEITORAL;
            case "TRABALHO" -> TipoJustica.TRABALHO;
            case "MILITAR_UNIAO", "MILITAR_SUPERIOR" -> TipoJustica.MILITAR_FEDERAL;
            case "MILITAR_ESTADUAL" -> TipoJustica.MILITAR_ESTADUAL;
            case "SUPERIOR", "SUPERIOR_STF", "ELEITORAL_SUPERIOR", "TRABALHO_SUPERIOR" -> TipoJustica.SUPERIOR;
            default -> TipoJustica.ESTADUAL;
        };
    }

    protected RitoProcessual resolveRito(ProcessoRequest request) {
        return resolveSelectedRito(request).rito();
    }

    protected SelectedRito resolveSelectedRito(ProcessoRequest request) {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("rito", request.getRito());
        payload.put("ramoDireito", request.getMateria());
        payload.put("materia", request.getMateria());
        payload.put("classe", request.getClasse());
        payload.put("classeProcessual", request.getClasse());
        payload.put("assunto", request.getAssunto());
        payload.put("resumo", request.getResumoFatico());
        payload.put("objetoProcessual", request.getObjetoProcessual());
        payload.put("pedidoPrincipal", request.getPedidoPrincipal());
        payload.put("pedidos", joinStructuredLines(request.getPedidos()));
        payload.put("provas", joinStructuredLines(request.getProvas()));
        payload.put("textoCaso", joinStructuredLines(java.util.Arrays.asList(
                firstNonBlank(request.getResumoFatico(), request.getAssunto()),
                request.getObjetoProcessual(),
                request.getPedidoPrincipal(),
                joinStructuredLines(request.getPedidos()),
                joinStructuredLines(request.getProvas()),
                request.getTipoAcao()
        )));
        payload.put("parteAutoraNome", request.getParteAutoraNome());
        payload.put("parteReuNome", request.getParteReuNome());
        payload.put("ufAutor", request.getUfAutor());
        payload.put("comarcaAutor", request.getComarcaAutor());
        payload.put("ufReu", request.getUfReu());
        payload.put("comarcaReu", request.getComarcaReu());
        payload.put("tipoAcao", request.getTipoAcao());
        payload.put("varaPretendida", request.getVaraPretendida());
        payload.put("tribunalCodigo", request.getTribunalPretendido());
        payload.put("tipoJustica", request.getTipoJusticaPretendida());
        payload.put("valorCausa", request.getValorCausa());
        payload.put("envolveMenor", request.isEnvolveMenor());
        payload.put("envolveSaude", request.isEnvolveSaude());
        return canonicalRitoSelector.select(payload, (String) null, "processo_mapper");
    }

    protected String resolveResumoInicial(ProcessoRequest request) {
        String base = firstNonBlank(request.getResumoFatico(), request.getAssunto(), request.getObjetoProcessual(), request.getPedidoPrincipal());
        return base == null ? null : truncate(base, 5000);
    }

    protected String joinStructuredLines(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        String joined = values.stream()
                .filter(v -> v != null && !v.isBlank())
                .map(String::trim)
                .distinct()
                .map(v -> "- " + v)
                .reduce((a, b) -> a + System.lineSeparator() + b)
                .orElse(null);
        return joined == null || joined.isBlank() ? null : truncate(joined, 12000);
    }

    protected String firstNonBlank(String... values) {
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

    protected String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() <= max) {
            return trimmed;
        }
        return trimmed.substring(0, Math.max(0, max));
    }
}
