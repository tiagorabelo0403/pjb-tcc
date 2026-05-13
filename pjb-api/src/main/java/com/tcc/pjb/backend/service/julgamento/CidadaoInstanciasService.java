package com.tcc.pjb.backend.service.julgamento;

import java.time.LocalDateTime;
import java.util.*;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.dto.cidadao.julgamento.CidadaoInstanciasResponse;
import com.tcc.pjb.backend.model.entity.Jurisdicao;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.julgamento.JulgamentoColegiado;
import com.tcc.pjb.backend.model.entity.julgamento.enums.StatusJulgamentoColegiado;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;

@Service
public class CidadaoInstanciasService {

  private final ProcessoRepository processoRepo;
  private final PjbAuthorizationService authz;
  private final JulgamentoColegiadoService julgamentoService;

  public CidadaoInstanciasService(ProcessoRepository processoRepo,
                                 PjbAuthorizationService authz,
                                 JulgamentoColegiadoService julgamentoService) {
    this.processoRepo = Objects.requireNonNull(processoRepo);
    this.authz = Objects.requireNonNull(authz);
    this.julgamentoService = Objects.requireNonNull(julgamentoService);
  }

  public CidadaoInstanciasResponse instancias(Long processoId) {
    Processo p = processoRepo.findById(processoId).orElse(null);
    if (p == null) {
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.NOT_FOUND, "processo nao encontrado");
    }
    authz.requireReadProcessoAsCidadaoParte(p);

    Jurisdicao j = p.getJurisdicao();
    GrauJurisdicao atual = j != null ? j.getGrau() : GrauJurisdicao.PRIMEIRO_GRAU;

    
    List<JulgamentoColegiado> julgamentos = julgamentoService.listByProcesso(processoId);
    Map<GrauJurisdicao, List<JulgamentoColegiado>> byGrau = new EnumMap<>(GrauJurisdicao.class);
    for (JulgamentoColegiado jc : julgamentos) {
      if (jc == null || jc.getGrau() == null) continue;
      byGrau.computeIfAbsent(jc.getGrau(), k -> new ArrayList<>()).add(jc);
    }

    List<CidadaoInstanciasResponse.InstanciaDto> list = new ArrayList<>();
    add(list, GrauJurisdicao.PRIMEIRO_GRAU, atual, byGrau.get(GrauJurisdicao.PRIMEIRO_GRAU), processoId);
    add(list, GrauJurisdicao.SEGUNDO_GRAU, atual, byGrau.get(GrauJurisdicao.SEGUNDO_GRAU), processoId);
    add(list, GrauJurisdicao.SUPERIOR, atual, byGrau.get(GrauJurisdicao.SUPERIOR), processoId);
    add(list, GrauJurisdicao.CONSTITUCIONAL, atual, byGrau.get(GrauJurisdicao.CONSTITUCIONAL), processoId);

    return new CidadaoInstanciasResponse(
        p.getId(),
        p.getNumeroUnificado(),
        LocalDateTime.now(),
        list,
        "/api/v1/cidadao/processos/" + p.getId() + "/julgamentos"
    );
  }

  private void add(List<CidadaoInstanciasResponse.InstanciaDto> list,
                   GrauJurisdicao g,
                   GrauJurisdicao atual,
                   List<JulgamentoColegiado> julgamentos,
                   Long processoId) {

    String status = computeStatus(g, atual, julgamentos);
    String url = (g.exigeColegiado())
        ? "/api/v1/cidadao/processos/" + processoId + "/julgamentos"
        : "/api/v1/cidadao/processos/" + processoId + "/overview";

    list.add(new CidadaoInstanciasResponse.InstanciaDto(
        g.name(),
        g.getLabel(),
        g.getDescricao(),
        g.exigeColegiado(),
        status,
        url
    ));
  }

  private String computeStatus(GrauJurisdicao g,
                              GrauJurisdicao atual,
                              List<JulgamentoColegiado> julgamentos) {

    
    if (julgamentos != null && !julgamentos.isEmpty()) {
      
      StatusJulgamentoColegiado best = null;
      for (JulgamentoColegiado jc : julgamentos) {
        StatusJulgamentoColegiado st = jc.getStatus();
        if (st == null) continue;
        if (best == null || rank(st) < rank(best)) best = st;
      }
      if (best != null) {
        return "JULGAMENTO: " + best.name();
      }
      return "JULGAMENTO: REGISTRADO";
    }

    if (g.ordinal() < atual.ordinal()) return "CONCLUIDA/ANTERIOR";
    if (g == atual) return "ATUAL";
    return "POSSIVEL (se houver recurso/admissibilidade)";
  }

  private int rank(StatusJulgamentoColegiado st) {
    return switch (st) {
      case EM_ANDAMENTO -> 0;
      case VISTA, SUSPENSO -> 1;
      case AGENDADO, ADIADO -> 2;
      case ENCERRADO -> 3;
    };
  }
}
