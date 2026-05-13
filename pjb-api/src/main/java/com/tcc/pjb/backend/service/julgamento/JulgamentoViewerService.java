package com.tcc.pjb.backend.service.julgamento;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.dto.cidadao.julgamento.JulgamentoResumoDto;
import com.tcc.pjb.backend.model.dto.cidadao.julgamento.VotoResumoDto;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.julgamento.JulgamentoColegiado;
import com.tcc.pjb.backend.model.entity.julgamento.VotoColegiado;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;






@Service
public class JulgamentoViewerService {

  private final JulgamentoColegiadoService julgamentoService;
  private final ProcessoRepository processoRepo;
  private final PjbAuthorizationService authz;

  public JulgamentoViewerService(JulgamentoColegiadoService julgamentoService,
                                ProcessoRepository processoRepo,
                                PjbAuthorizationService authz) {
    this.julgamentoService = Objects.requireNonNull(julgamentoService);
    this.processoRepo = Objects.requireNonNull(processoRepo);
    this.authz = Objects.requireNonNull(authz);
  }

  public List<JulgamentoResumoDto> listarJulgamentosDoProcesso(Long processoId) {
    Processo p = processoRepo.findById(processoId).orElse(null);
    if (p == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "processo nao encontrado");
    }

    
    authz.requireReadVotosColegiados(p);

    List<JulgamentoColegiado> julg = julgamentoService.listByProcesso(processoId);
    List<JulgamentoResumoDto> out = new ArrayList<>(julg.size());

    for (JulgamentoColegiado j : julg) {
      List<VotoColegiado> votos = julgamentoService.listVotos(j.getId());
      out.add(toResumo(j, votos, true));
    }

    return out;
  }

  public JulgamentoResumoDto detalheJulgamento(Long julgamentoId) {
    JulgamentoColegiado j = julgamentoService.getRequired(julgamentoId);
    Processo p = j.getProcesso();
    if (p == null) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "julgamento sem processo");
    }

    authz.requireReadVotosColegiados(p);

    List<VotoColegiado> votos = julgamentoService.listVotos(julgamentoId);
    return toResumo(j, votos, true);
  }

  private JulgamentoResumoDto toResumo(JulgamentoColegiado j, List<VotoColegiado> votos, boolean includeSse) {
    List<VotoResumoDto> votoDtos = votos.stream().map(v -> new VotoResumoDto(
        v.getOrdem(),
        v.getMagistradoNome(),
        v.getMagistradoCargo(),
        v.getPapel() != null ? v.getPapel().name() : null,
        v.getVotoTipo() != null ? v.getVotoTipo().name() : null,
        v.getVotoResumo(),
        v.getProferidoEm(),
        v.getDocumentoRef()
    )).toList();

    var acordao = julgamentoService.findAcordao(j.getId()).orElse(null);
    String acordaoNumero = acordao != null ? acordao.getNumeroAcordao() : null;
    String acordaoEmenta = acordao != null ? acordao.getEmentaResumo() : null;
    String acordaoRef = acordao != null ? acordao.getInteiroTeorRef() : null;

    var placar = new JulgamentoResumoDto.PlacarDto(
        j.getPlacarFavor(), j.getPlacarContra(), j.getPlacarParcial(), j.getPlacarOutros()
    );

    String sseUrl = includeSse ? ("/api/v1/julgamentos/" + j.getId() + "/votos/stream") : null;

    return new JulgamentoResumoDto(
        j.getId(),
        j.getGrau() != null ? j.getGrau().name() : null,
        j.getTribunalSigla(),
        j.getOrgaoJulgador(),
        j.getRelatorNome(),
        j.getStatus() != null ? j.getStatus().name() : null,
        j.getPautaDataHora(),
        j.getSessaoInicio(),
        j.getSessaoFim(),
        placar,
        j.getAcordaoPublicado(),
        j.getAcordaoPublicadoEm(),
        acordaoNumero,
        acordaoEmenta,
        acordaoRef,
        votoDtos,
        sseUrl
    );
  }
}
