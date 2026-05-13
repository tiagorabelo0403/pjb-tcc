package com.tcc.pjb.backend.service.workspace;

import java.util.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.observability.RequestContext;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.dto.workspace.localizador.WorkspaceLocalizadorCriteria;
import com.tcc.pjb.backend.model.dto.workspace.localizador.WorkspaceProcessoResumoResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.workspace.WorkspaceEtiqueta;
import com.tcc.pjb.backend.model.entity.workspace.WorkspaceLocalizador;
import com.tcc.pjb.backend.model.entity.workspace.WorkspaceProcessoEtiqueta;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.workspace.WorkspaceLocalizadorRepository;
import com.tcc.pjb.backend.model.repository.workspace.WorkspaceProcessoEtiquetaRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import java.util.Locale;

@Service
public class WorkspaceLocalizadorQueryService {

    private static final Set<String> ALLOWED_SORT = Set.of(
            "dataUltimaMovimentacao",
            "numeroUnificado",
            "statusProcesso",
            "faseAtual",
            "rito"
    );

    private final WorkspaceLocalizadorRepository localizadorRepository;
    private final ProcessoRepository processoRepository;
    private final WorkspaceProcessoEtiquetaRepository processoEtiquetaRepository;
    private final CurrentUserService currentUserService;
    private final PjbAuthorizationService authorizationService;
    private final EntityManager entityManager;

    public WorkspaceLocalizadorQueryService(WorkspaceLocalizadorRepository localizadorRepository,
                                           ProcessoRepository processoRepository,
                                           WorkspaceProcessoEtiquetaRepository processoEtiquetaRepository,
                                           CurrentUserService currentUserService,
                                           PjbAuthorizationService authorizationService,
                                           EntityManager entityManager) {
        this.localizadorRepository = localizadorRepository;
        this.processoRepository = processoRepository;
        this.processoEtiquetaRepository = processoEtiquetaRepository;
        this.currentUserService = currentUserService;
        this.authorizationService = authorizationService;
        this.entityManager = entityManager;
    }

    public Page<WorkspaceProcessoResumoResponse> listarProcessos(UUID localizadorId,
                                                                int page,
                                                                int size,
                                                                String sortBy,
                                                                String dir) {
        Usuario u = currentUserService.getRequired();

        WorkspaceLocalizador l = localizadorRepository.findById(localizadorId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Localizador não encontrado"));

        if (!l.isCompartilhado() && !u.getId().equals(l.getOwnerUserId())) {
            throw new SecurityException("Localizador não pertence ao usuário");
        }

        WorkspaceLocalizadorCriteria criteria = WorkspaceLocalizadorJson.parseLenient(l.getCriterioJson());
        Pageable pageable = buildPageable(page, size, sortBy, dir);

        
        Page<Long> idsPage = queryIds(criteria, pageable, u);
        if (idsPage.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, idsPage.getTotalElements());
        }

        List<Long> ids = idsPage.getContent();
        List<Processo> processos = processoRepository.findAllById(ids);

        
        Map<Long, Integer> order = new HashMap<>();
        for (int i = 0; i < ids.size(); i++) order.put(ids.get(i), i);
        processos.sort(Comparator.comparingInt(p -> order.getOrDefault(p.getId(), Integer.MAX_VALUE)));

        
        List<Processo> visiveis = new ArrayList<>();
        for (Processo p : processos) {
            try {
                authorizationService.requireReadProcesso(p);
                visiveis.add(p);
            } catch (Exception ignored) {
                
            }
        }

        if (visiveis.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        List<Long> visibleIds = visiveis.stream().map(Processo::getId).toList();

        
        
        Map<Long, List<WorkspaceEtiqueta>> byPid = new HashMap<>();
        for (WorkspaceProcessoEtiqueta pe : processoEtiquetaRepository.findAllByProcessoIds(visibleIds)) {
            WorkspaceEtiqueta e = pe.getEtiqueta();
            if (e == null) continue;
            
            if (!e.isSistema() && !u.getId().equals(e.getOwnerUserId())) continue;
            byPid.computeIfAbsent(pe.getProcesso().getId(), k -> new ArrayList<>()).add(e);
        }

        List<WorkspaceProcessoResumoResponse> content = visiveis.stream()
                .map(p -> toResumo(p, byPid.getOrDefault(p.getId(), List.of())))
                .toList();

        
        return new PageImpl<>(content, pageable, idsPage.getTotalElements());
    }

    public Page<WorkspaceProcessoResumoResponse> preview(WorkspaceLocalizadorCriteria criteria,
                                                        int page,
                                                        int size,
                                                        String sortBy,
                                                        String dir) {
        Usuario u = currentUserService.getRequired();
        Pageable pageable = buildPageable(page, size, sortBy, dir);

        Page<Long> idsPage = queryIds(criteria, pageable, u);
        if (idsPage.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        List<Long> ids = idsPage.getContent();
        List<Processo> processos = processoRepository.findAllById(ids);

        Map<Long, Integer> order = new HashMap<>();
        for (int i = 0; i < ids.size(); i++) order.put(ids.get(i), i);
        processos.sort(Comparator.comparingInt(p -> order.getOrDefault(p.getId(), Integer.MAX_VALUE)));

        List<Processo> visiveis = new ArrayList<>();
        for (Processo p : processos) {
            try {
                authorizationService.requireReadProcesso(p);
                visiveis.add(p);
            } catch (Exception ignored) {
            }
        }

        List<Long> visibleIds = visiveis.stream().map(Processo::getId).toList();
        Map<Long, List<WorkspaceEtiqueta>> byPid = new HashMap<>();
        for (WorkspaceProcessoEtiqueta pe : processoEtiquetaRepository.findAllByProcessoIds(visibleIds)) {
            WorkspaceEtiqueta e = pe.getEtiqueta();
            if (e == null) continue;
            if (!e.isSistema() && !u.getId().equals(e.getOwnerUserId())) continue;
            byPid.computeIfAbsent(pe.getProcesso().getId(), k -> new ArrayList<>()).add(e);
        }

        List<WorkspaceProcessoResumoResponse> content = visiveis.stream()
                .map(p -> toResumo(p, byPid.getOrDefault(p.getId(), List.of())))
                .toList();

        return new PageImpl<>(content, pageable, idsPage.getTotalElements());
    }

    
    public long countFast(WorkspaceLocalizadorCriteria criteria) {
        Usuario u = currentUserService.getRequired();
        Pageable pageable = PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "dataUltimaMovimentacao"));
        return queryIds(criteria, pageable, u).getTotalElements();
    }

    private Page<Long> queryIds(WorkspaceLocalizadorCriteria criteria, Pageable pageable, Usuario u) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Processo> p = cq.from(Processo.class);
        cq.select(p.get("id")).distinct(true);

        List<Predicate> predicates = new ArrayList<>();

        
        boolean hasJust = RequestContext.getJustificativa().isPresent();
        if (!hasJust) {
            Predicate publico = cb.equal(p.get("nivelSigilo"), NivelSigilo.PUBLICO);
            Predicate nulo = cb.isNull(p.get("nivelSigilo"));
            predicates.add(cb.or(publico, nulo));
        }

        if (criteria != null) {
            if (Boolean.TRUE.equals(criteria.getSomenteMeus())) {
                predicates.add(cb.equal(p.get("usuario").get("id"), u.getId()));
            }
            if (criteria.getJurisdicaoId() != null) {
                predicates.add(cb.equal(p.get("jurisdicao").get("id"), criteria.getJurisdicaoId()));
            }
            if (criteria.getStatus() != null && !criteria.getStatus().isEmpty()) {
                predicates.add(p.get("statusProcesso").in(criteria.getStatus()));
            }
            if (criteria.getFases() != null && !criteria.getFases().isEmpty()) {
                predicates.add(p.get("faseAtual").in(criteria.getFases()));
            }
            if (criteria.getRitos() != null && !criteria.getRitos().isEmpty()) {
                predicates.add(p.get("rito").in(criteria.getRitos()));
            }
            String q = safe(criteria.getQ());
            if (q != null) {
                String like = "%" + q.toLowerCase(Locale.ROOT) + "%";
                Expression<String> numeroUnificado = cb.lower(cb.coalesce(p.get("numeroUnificado"), ""));
                Expression<String> numero = cb.lower(cb.coalesce(p.get("numeroProcesso"), ""));
                Expression<String> classe = cb.lower(cb.coalesce(p.get("classeProcessual"), ""));
                Expression<String> assunto = cb.lower(cb.coalesce(p.get("assunto"), ""));
                Expression<String> autor = cb.lower(cb.coalesce(p.get("parteAutoraNome"), ""));
                Expression<String> reu = cb.lower(cb.coalesce(p.get("parteReuNome"), ""));

                predicates.add(cb.or(
                        cb.like(numeroUnificado, like),
                        cb.like(numero, like),
                        cb.like(classe, like),
                        cb.like(assunto, like),
                        cb.like(autor, like),
                        cb.like(reu, like)
                ));
            }

            if (criteria.getEtiquetaIds() != null && !criteria.getEtiquetaIds().isEmpty()) {
                Subquery<Long> sq = cq.subquery(Long.class);
                Root<WorkspaceProcessoEtiqueta> pe = sq.from(WorkspaceProcessoEtiqueta.class);
                sq.select(pe.get("processo").get("id")).distinct(true);
                sq.where(pe.get("etiqueta").get("id").in(criteria.getEtiquetaIds()));
                predicates.add(p.get("id").in(sq));
            }
        }

        if (!predicates.isEmpty()) {
            cq.where(cb.and(predicates.toArray(new Predicate[0])));
        }

        
        cq.orderBy(toOrders(pageable.getSort(), cb, p));

        TypedQuery<Long> query = entityManager.createQuery(cq);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());
        List<Long> ids = query.getResultList();

        
        CriteriaQuery<Long> countQ = cb.createQuery(Long.class);
        Root<Processo> p2 = countQ.from(Processo.class);
        countQ.select(cb.countDistinct(p2.get("id")));

        List<Predicate> predicates2 = new ArrayList<>();
        if (!RequestContext.getJustificativa().isPresent()) {
            Predicate publico = cb.equal(p2.get("nivelSigilo"), NivelSigilo.PUBLICO);
            Predicate nulo = cb.isNull(p2.get("nivelSigilo"));
            predicates2.add(cb.or(publico, nulo));
        }

        if (criteria != null) {
            if (Boolean.TRUE.equals(criteria.getSomenteMeus())) {
                predicates2.add(cb.equal(p2.get("usuario").get("id"), u.getId()));
            }
            if (criteria.getJurisdicaoId() != null) {
                predicates2.add(cb.equal(p2.get("jurisdicao").get("id"), criteria.getJurisdicaoId()));
            }
            if (criteria.getStatus() != null && !criteria.getStatus().isEmpty()) {
                predicates2.add(p2.get("statusProcesso").in(criteria.getStatus()));
            }
            if (criteria.getFases() != null && !criteria.getFases().isEmpty()) {
                predicates2.add(p2.get("faseAtual").in(criteria.getFases()));
            }
            if (criteria.getRitos() != null && !criteria.getRitos().isEmpty()) {
                predicates2.add(p2.get("rito").in(criteria.getRitos()));
            }
            String q = safe(criteria.getQ());
            if (q != null) {
                String like = "%" + q.toLowerCase(Locale.ROOT) + "%";
                Expression<String> numeroUnificado = cb.lower(cb.coalesce(p2.get("numeroUnificado"), ""));
                Expression<String> numero = cb.lower(cb.coalesce(p2.get("numeroProcesso"), ""));
                Expression<String> classe = cb.lower(cb.coalesce(p2.get("classeProcessual"), ""));
                Expression<String> assunto = cb.lower(cb.coalesce(p2.get("assunto"), ""));
                Expression<String> autor = cb.lower(cb.coalesce(p2.get("parteAutoraNome"), ""));
                Expression<String> reu = cb.lower(cb.coalesce(p2.get("parteReuNome"), ""));

                predicates2.add(cb.or(
                        cb.like(numeroUnificado, like),
                        cb.like(numero, like),
                        cb.like(classe, like),
                        cb.like(assunto, like),
                        cb.like(autor, like),
                        cb.like(reu, like)
                ));
            }

            if (criteria.getEtiquetaIds() != null && !criteria.getEtiquetaIds().isEmpty()) {
                Subquery<Long> sq = countQ.subquery(Long.class);
                Root<WorkspaceProcessoEtiqueta> pe = sq.from(WorkspaceProcessoEtiqueta.class);
                sq.select(pe.get("processo").get("id")).distinct(true);
                sq.where(pe.get("etiqueta").get("id").in(criteria.getEtiquetaIds()));
                predicates2.add(p2.get("id").in(sq));
            }
        }

        if (!predicates2.isEmpty()) {
            countQ.where(cb.and(predicates2.toArray(new Predicate[0])));
        }

        Long total = entityManager.createQuery(countQ).getSingleResult();
        return new PageImpl<>(ids, pageable, total == null ? 0 : total);
    }

    private Pageable buildPageable(int page, int size, String sortBy, String dir) {
        int p = Math.max(0, page);
        int s = Math.min(Math.max(size, 1), 100);

        String sb = safe(sortBy);
        if (sb == null || !ALLOWED_SORT.contains(sb)) {
            sb = "dataUltimaMovimentacao";
        }
        Sort.Direction direction = "asc".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(p, s, Sort.by(direction, sb));
    }

    private List<Order> toOrders(Sort sort, CriteriaBuilder cb, Root<Processo> p) {
        if (sort == null || sort.isUnsorted()) {
            return List.of(cb.desc(p.get("dataUltimaMovimentacao")));
        }
        List<Order> orders = new ArrayList<>();
        for (Sort.Order o : sort) {
            String prop = o.getProperty();
            if (!ALLOWED_SORT.contains(prop)) continue;
            orders.add(o.isAscending() ? cb.asc(p.get(prop)) : cb.desc(p.get(prop)));
        }
        if (orders.isEmpty()) {
            orders.add(cb.desc(p.get("dataUltimaMovimentacao")));
        }
        return orders;
    }

    private WorkspaceProcessoResumoResponse toResumo(Processo p, List<WorkspaceEtiqueta> etiquetas) {
        List<WorkspaceProcessoResumoResponse.WorkspaceEtiquetaLite> lites = etiquetas.stream()
                .sorted(Comparator.comparing(WorkspaceEtiqueta::getNome, Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(e -> WorkspaceProcessoResumoResponse.WorkspaceEtiquetaLite.builder()
                        .id(e.getId())
                        .nome(e.getNome())
                        .corHex(e.getCorHex())
                        .build())
                .toList();

        return WorkspaceProcessoResumoResponse.builder()
                .processoId(p.getId())
                .numeroUnificado(p.getNumeroUnificado())
                .classeProcessual(p.getClasseProcessual())
                .assunto(p.getAssunto())
                .status(p.getStatusProcesso())
                .fase(p.getFaseAtual())
                .rito(p.getRito())
                .dataUltimaMovimentacao(p.getDataUltimaMovimentacao())
                .etiquetas(lites)
                .build();
    }

    private static String safe(String s) {
        if (s == null) return null;
        String v = s.trim();
        if (v.isBlank()) return null;
        return v;
    }

    
    static final class WorkspaceLocalizadorJson {
        private static final com.fasterxml.jackson.databind.ObjectMapper OM = new com.fasterxml.jackson.databind.ObjectMapper();

        static WorkspaceLocalizadorCriteria parseLenient(String json) {
            if (json == null || json.isBlank()) return new WorkspaceLocalizadorCriteria();
            try {
                return OM.readValue(json, WorkspaceLocalizadorCriteria.class);
            } catch (Exception ignored) {
                return new WorkspaceLocalizadorCriteria();
            }
        }
    }
}
