package com.tcc.pjb.backend.service.cidadao;

import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.core.security.stepup.JwtStepUpClaims;
import com.tcc.pjb.backend.model.dto.cidadao.CidadaoDocumentoMetaDto;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.enums.DocumentoCategoria;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.recursal.RecursalEffectiveSecrecyService;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CidadaoDocumentoMetaService {

  private final ProcessoRepository processoRepo;
  private final DocumentoProcessualRepository docRepo;
  private final PjbAuthorizationService authz;
  private final RecursalEffectiveSecrecyService secrecyService;

  public CidadaoDocumentoMetaService(ProcessoRepository processoRepo,
                                    DocumentoProcessualRepository docRepo,
                                    PjbAuthorizationService authz,
                                    RecursalEffectiveSecrecyService secrecyService) {
    this.processoRepo = Objects.requireNonNull(processoRepo);
    this.docRepo = Objects.requireNonNull(docRepo);
    this.authz = Objects.requireNonNull(authz);
    this.secrecyService = Objects.requireNonNull(secrecyService);
  }

  @Transactional(readOnly = true)
  public List<CidadaoDocumentoMetaDto> listar(Long processoId) {
    Processo p = processoRepo.findById(processoId)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));

    authz.requireReadProcessoAsCidadaoParte(p);

    NivelSigilo efetivoProc = secrecyService.effectiveSecrecyForProcesso(processoId);
    authz.requireReadProcessoAtSecrecy(p, efetivoProc);

    boolean stepUpOk = JwtStepUpClaims.hasMfa();

    List<DocumentoProcessual> docs = docRepo.findByProcessoId(processoId);
    return docs.stream().map(d -> {
      DocumentoCategoria cat = d.getCategoria() == null ? DocumentoCategoria.PUBLICO : d.getCategoria();
      NivelSigilo docSig = d.getNivelSigilo() == null ? NivelSigilo.PUBLICO : d.getNivelSigilo();
      NivelSigilo minCat = (cat == DocumentoCategoria.PESSOAL) ? NivelSigilo.SIGILO_N2 : NivelSigilo.PUBLICO;
      NivelSigilo docEfetivo = maxSigilo(efetivoProc, maxSigilo(docSig, minCat));

      boolean policyAllowed = authz.canReadDocumentoAtSecrecy(p, d, efetivoProc).allowed();
      boolean high = docEfetivo.getNivel() >= NivelSigilo.SIGILO_N2.getNivel();
      boolean requiresStepUp = policyAllowed && high && !stepUpOk;
      boolean canDownload = policyAllowed && (!high || stepUpOk);

      return new CidadaoDocumentoMetaDto(
          d.getId(),
          d.getTitulo(),
          d.getNomeOriginal(),
          d.getContentType(),
          d.getTamanhoBytes(),
          d.getCriadoEm(),
          cat.name(),
          docEfetivo.name(),
          d.getSha256(),
          canDownload,
          requiresStepUp
      );
    }).toList();
  }

  private static NivelSigilo maxSigilo(NivelSigilo a, NivelSigilo b) {
    NivelSigilo x = (a == null) ? NivelSigilo.PUBLICO : a;
    NivelSigilo y = (b == null) ? NivelSigilo.PUBLICO : b;
    return (x.getNivel() >= y.getNivel()) ? x : y;
  }
}
