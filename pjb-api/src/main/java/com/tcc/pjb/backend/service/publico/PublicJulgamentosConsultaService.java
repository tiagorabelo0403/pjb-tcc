package com.tcc.pjb.backend.service.publico;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.tcc.pjb.backend.model.dto.publico.PublicJulgamentoAcordaoDto;
import com.tcc.pjb.backend.model.dto.publico.PublicJulgamentosConsultaResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.julgamento.Acordao;
import com.tcc.pjb.backend.model.entity.julgamento.JulgamentoColegiado;
import com.tcc.pjb.backend.model.entity.julgamento.enums.StatusJulgamentoColegiado;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.julgamento.AcordaoRepository;
import com.tcc.pjb.backend.model.repository.julgamento.JulgamentoColegiadoRepository;





@Service
public class PublicJulgamentosConsultaService {

  private final ProcessoRepository processoRepo;
  private final JulgamentoColegiadoRepository julgamentoRepo;
  private final AcordaoRepository acordaoRepo;

  public PublicJulgamentosConsultaService(ProcessoRepository processoRepo,
                                         JulgamentoColegiadoRepository julgamentoRepo,
                                         AcordaoRepository acordaoRepo) {
    this.processoRepo = Objects.requireNonNull(processoRepo);
    this.julgamentoRepo = Objects.requireNonNull(julgamentoRepo);
    this.acordaoRepo = Objects.requireNonNull(acordaoRepo);
  }

  @Transactional(readOnly = true)
  public PublicJulgamentosConsultaResponse consultarPublicadosPorNumero(String numero) {
    Processo p = processoRepo.findByNumeroUnificado(numero)
        .or(() -> processoRepo.findByNumeroProcesso(numero))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "processo nao encontrado"));

    NivelSigilo sigilo = p.getNivelSigilo() == null ? NivelSigilo.PUBLICO : p.getNivelSigilo();
    if (sigilo.exigeCredencial()) {
      
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "processo nao encontrado");
    }

    List<JulgamentoColegiado> julgamentos = julgamentoRepo.findByProcessoId(p.getId());
    List<Long> ids = julgamentos.stream().map(JulgamentoColegiado::getId).toList();

    Map<Long, Acordao> acordaos = new HashMap<>();
    if (!ids.isEmpty()) {
      for (Acordao a : acordaoRepo.findByJulgamentoIdIn(ids)) {
        if (a.getJulgamento() != null && a.getJulgamento().getId() != null) {
          acordaos.put(a.getJulgamento().getId(), a);
        }
      }
    }

    List<PublicJulgamentoAcordaoDto> out = new ArrayList<>();
    for (JulgamentoColegiado j : julgamentos) {
      if (!Boolean.TRUE.equals(j.getAcordaoPublicado())) {
        continue;
      }
      Acordao a = acordaos.get(j.getId());
      if (a == null) {
        continue;
      }
      String placarFinal = buildPlacarPublicoFinal(j);
      out.add(new PublicJulgamentoAcordaoDto(
          j.getId(),
          j.getGrau() != null ? j.getGrau().name() : null,
          j.getTribunalSigla(),
          j.getOrgaoJulgador(),
          j.getRelatorNome(),
          j.getStatus() != null ? j.getStatus().name() : null,
          a.getNumeroAcordao(),
          a.getEmentaResumo(),
          a.getInteiroTeorRef(),
          a.getPublicadoEm(),
          placarFinal
      ));
    }

    return new PublicJulgamentosConsultaResponse(p.getId(), numero, true, out);
  }

  



  private static String buildPlacarPublicoFinal(JulgamentoColegiado j) {
    if (j == null) return null;
    if (j.getStatus() != StatusJulgamentoColegiado.ENCERRADO) return null;

    Integer favor = j.getPlacarFavor();
    Integer contra = j.getPlacarContra();
    Integer outros = j.getPlacarOutros();

    int f = favor != null ? favor : 0;
    int c = contra != null ? contra : 0;
    int o = outros != null ? outros : 0;

    if (f == 0 && c == 0 && o == 0) return null;
    if (o > 0) {
      
      return f + "x" + c + " (+" + o + ")";
    }
    return f + "x" + c;
  }
}
