package com.tcc.pjb.backend.service.oab;

import java.util.*;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import jakarta.persistence.criteria.Predicate;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.oab.*;
import com.tcc.pjb.backend.model.entity.EventoInstitucional;
import com.tcc.pjb.backend.model.entity.ProvidenciaInstitucional;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.StatusEventoInstitucional;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.EventoInstitucionalRepository;
import com.tcc.pjb.backend.model.repository.ProvidenciaInstitucionalRepository;
import java.util.Locale;
import com.tcc.pjb.backend.platform.runtime.PjbTransactionalBudget;

@Service
public class OabInstitucionalService {

    private final EventoInstitucionalRepository eventoRepo;
    private final ProvidenciaInstitucionalRepository providenciaRepo;
    private final CurrentUserService currentUserService;

    public OabInstitucionalService(EventoInstitucionalRepository eventoRepo,
                                  ProvidenciaInstitucionalRepository providenciaRepo,
                                  CurrentUserService currentUserService) {
        this.eventoRepo = Objects.requireNonNull(eventoRepo);
        this.providenciaRepo = Objects.requireNonNull(providenciaRepo);
        this.currentUserService = Objects.requireNonNull(currentUserService);
    }

    @Transactional
    public OabEventoResponse criarSolicitacao(OabEventoCreateRequest req) {
        Usuario u = currentUserService.getRequired();
        if (u.getTipoUsuario() != TipoUsuario.ADVOGADO) {
            throw new IllegalStateException("Apenas ADVOGADO pode criar solicitação à OAB/UF.");
        }

        String uf = normalizeUf(req.getUf());
        EventoInstitucional e = EventoInstitucional.builder()
                .tipo(req.getTipo())
                .status(StatusEventoInstitucional.ABERTO)
                .uf(uf)
                .tribunal(req.getTribunal())
                .orgao(req.getOrgao())
                .processoId(req.getProcessoId())
                .numeroProcesso(req.getNumeroProcesso())
                .severidade(req.getSeveridade())
                .resumo(req.getResumo())
                .detalhes(req.getDetalhes())
                .criadoPorUsuarioId(u.getId())
                .criadoPor(u.getEmail())
                .build();

        EventoInstitucional salvo = eventoRepo.save(e);
        return mapEvento(salvo, List.of());
    }

    @Transactional
    public List<OabEventoResponse> listarMinhasSolicitacoes() {
        Usuario u = currentUserService.getRequired();
        return eventoRepo.findTop100ByCriadoPorUsuarioIdOrderByCriadoEmDesc(u.getId())
                .stream().map(e -> mapEvento(e, safeProvidencias(e))).toList();
    }

    @PjbTransactionalBudget(operation = "oab.institucional.listar-seccional", maxMillis = 3000)
    @Transactional
    public List<OabEventoResponse> listarSeccional(
            List<StatusEventoInstitucional> statusFilter,
            Long processoId,
            String numeroProcesso,
            String orgao,
            String tribunal,
            boolean includeProvidencias,
            int limit
    ) {
        Usuario oab = currentUserService.getRequired();
        if (oab.getTipoUsuario() != TipoUsuario.OAB_PRESIDENTE_SECCIONAL) {
            throw new IllegalStateException("Acesso restrito à OAB/UF (presidência seccional).");
        }
        String uf = normalizeUf(oab.getUf());
        if (uf == null) throw new IllegalStateException("Usuário OAB/UF sem UF configurada.");

        int safeLimit = clampLimit(limit);
        Sort sort = Sort.by(Sort.Order.desc("severidade"), Sort.Order.desc("criadoEm"));
        var pageable = PageRequest.of(0, safeLimit, sort);

        Specification<EventoInstitucional> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.equal(root.get("uf"), uf));

            if (statusFilter != null && !statusFilter.isEmpty()) {
                ps.add(root.get("status").in(statusFilter));
            }
            if (processoId != null) {
                ps.add(cb.equal(root.get("processoId"), processoId));
            }
            String np = normalizeProcessNumber(numeroProcesso);
            if (np != null) {
                ps.add(cb.equal(root.get("numeroProcesso"), np));
            }
            String org = normalizeTextKey(orgao);
            if (org != null) {
                ps.add(cb.equal(cb.upper(root.get("orgao")), org));
            }
            String tri = normalizeTextKey(tribunal);
            if (tri != null) {
                ps.add(cb.equal(cb.upper(root.get("tribunal")), tri));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };

        List<EventoInstitucional> list = eventoRepo.findAll(spec, pageable).getContent();

        Map<Long, List<OabProvidenciaResponse>> providenciasByEvento = Map.of();
        if (includeProvidencias && !list.isEmpty()) {
            List<Long> ids = list.stream().map(EventoInstitucional::getId).filter(Objects::nonNull).toList();
            if (!ids.isEmpty()) {
                providenciasByEvento = providenciaRepo.findByEvento_IdIn(ids)
                        .stream()
                        .map(providencia -> {
                            EventoInstitucional evento = providencia.getEvento();
                            Long eventoId = evento != null ? evento.getId() : null;
                            return eventoId == null
                                    ? null
                                    : Map.entry(eventoId, mapProvidencia(providencia));
                        })
                        .filter(Objects::nonNull)
                        .collect(java.util.stream.Collectors.groupingBy(
                                Map.Entry::getKey,
                                java.util.stream.Collectors.mapping(Map.Entry::getValue, java.util.stream.Collectors.toList())
                        ));
            }
        }

        Map<Long, List<OabProvidenciaResponse>> finalProv = providenciasByEvento;
        return list.stream()
                .map(e -> mapEvento(e, includeProvidencias ? finalProv.getOrDefault(e.getId(), List.of()) : List.of()))
                .toList();
    }

    @Transactional
    public OabEventoResponse atualizarStatus(Long eventoId, StatusEventoInstitucional novoStatus) {
        Usuario oab = currentUserService.getRequired();
        if (oab.getTipoUsuario() != TipoUsuario.OAB_PRESIDENTE_SECCIONAL) {
            throw new IllegalStateException("Acesso restrito à OAB/UF (presidência seccional).");
        }

        EventoInstitucional e = eventoRepo.findById(eventoId)
                .orElseThrow(() -> new IllegalArgumentException("Evento não encontrado."));

        String ufOab = normalizeUf(oab.getUf());
        if (!normalizeUf(e.getUf()).equals(ufOab)) {
            throw new IllegalStateException("A seccional não pode alterar evento de outra UF.");
        }

        e.setStatus(novoStatus);
        EventoInstitucional saved = eventoRepo.save(e);
        return mapEvento(saved, safeProvidencias(saved));
    }

    @Transactional
    public OabProvidenciaResponse adicionarProvidencia(Long eventoId, OabProvidenciaCreateRequest req) {
        Usuario oab = currentUserService.getRequired();
        if (oab.getTipoUsuario() != TipoUsuario.OAB_PRESIDENTE_SECCIONAL) {
            throw new IllegalStateException("Acesso restrito à OAB/UF (presidência seccional).");
        }

        EventoInstitucional e = eventoRepo.findById(eventoId)
                .orElseThrow(() -> new IllegalArgumentException("Evento não encontrado."));

        String ufOab = normalizeUf(oab.getUf());
        if (!normalizeUf(e.getUf()).equals(ufOab)) {
            throw new IllegalStateException("A seccional não pode atuar em evento de outra UF.");
        }

        ProvidenciaInstitucional p = ProvidenciaInstitucional.builder()
                .evento(e)
                .tipo(req.getTipo())
                .titulo(req.getTitulo())
                .descricao(req.getDescricao())
                .criadoPorUsuarioId(oab.getId())
                .criadoPor(oab.getEmail())
                .build();

        ProvidenciaInstitucional salvo = providenciaRepo.save(p);
        return mapProvidencia(salvo);
    }

    private OabEventoResponse mapEvento(EventoInstitucional e, List<OabProvidenciaResponse> providencias) {
        return OabEventoResponse.builder()
                .id(e.getId())
                .tipo(e.getTipo())
                .status(e.getStatus())
                .uf(e.getUf())
                .tribunal(e.getTribunal())
                .orgao(e.getOrgao())
                .processoId(e.getProcessoId())
                .numeroProcesso(e.getNumeroProcesso())
                .severidade(e.getSeveridade())
                .resumo(e.getResumo())
                .detalhes(e.getDetalhes())
                .criadoPor(e.getCriadoPor())
                .criadoEm(e.getCriadoEm())
                .providencias(providencias != null ? providencias : List.of())
                .build();
    }

    private List<OabProvidenciaResponse> safeProvidencias(EventoInstitucional e) {
        if (e == null || e.getProvidencias() == null) return List.of();
        return e.getProvidencias().stream().filter(Objects::nonNull).map(this::mapProvidencia).toList();
    }

    private OabProvidenciaResponse mapProvidencia(ProvidenciaInstitucional p) {
        Objects.requireNonNull(p, "providencia");
        return OabProvidenciaResponse.builder()
                .id(p.getId())
                .tipo(p.getTipo())
                .titulo(p.getTitulo())
                .descricao(p.getDescricao())
                .criadoPor(p.getCriadoPor())
                .criadoEm(p.getCriadoEm())
                .build();
    }

    private String normalizeUf(String uf) {
        if (uf == null) return null;
        String v = uf.trim().toUpperCase(Locale.ROOT);
        return v.isBlank() ? null : v;
    }

    private static int clampLimit(int limit) {
        if (limit <= 0) return 100;
        if (limit > 200) return 200;
        return limit;
    }

    private static String normalizeProcessNumber(String numero) {
        if (numero == null) return null;
        String v = numero.trim();
        return v.isBlank() ? null : v;
    }

    private static String normalizeTextKey(String raw) {
        if (raw == null) return null;
        String v = raw.trim().toUpperCase(Locale.ROOT);
        return v.isBlank() ? null : v;
    }
}
