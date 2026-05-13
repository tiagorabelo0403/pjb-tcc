package com.tcc.pjb.backend.service.publico;

import java.util.List;
import java.util.Objects;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.tcc.pjb.backend.model.dto.publico.SessaoPublicaDetalheDto;
import com.tcc.pjb.backend.model.dto.publico.SessaoPublicaDto;
import com.tcc.pjb.backend.model.dto.publico.SessaoPublicaEsclarecimentoDto;
import com.tcc.pjb.backend.model.dto.publico.SessaoPublicaMediaDto;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.julgamento.Acordao;
import com.tcc.pjb.backend.model.entity.julgamento.JulgamentoColegiado;
import com.tcc.pjb.backend.model.entity.publico.PublicPlenarioEsclarecimentoFato;
import com.tcc.pjb.backend.model.entity.publico.PublicPlenarioMediaAsset;
import com.tcc.pjb.backend.model.repository.PublicPlenarioEsclarecimentoFatoRepository;
import com.tcc.pjb.backend.model.repository.PublicPlenarioMediaAssetRepository;
import com.tcc.pjb.backend.model.repository.julgamento.AcordaoRepository;
import com.tcc.pjb.backend.model.repository.julgamento.JulgamentoColegiadoRepository;

@Service
public class PublicPlenarioService {

    private final JulgamentoColegiadoRepository julgamentoRepository;
    private final AcordaoRepository acordaoRepository;
    private final PublicPlenarioMediaAssetRepository mediaAssetRepository;
    private final PublicPlenarioEsclarecimentoFatoRepository esclarecimentoRepository;

    public PublicPlenarioService(JulgamentoColegiadoRepository julgamentoRepository,
                                 AcordaoRepository acordaoRepository,
                                 PublicPlenarioMediaAssetRepository mediaAssetRepository,
                                 PublicPlenarioEsclarecimentoFatoRepository esclarecimentoRepository) {
        this.julgamentoRepository = Objects.requireNonNull(julgamentoRepository);
        this.acordaoRepository = Objects.requireNonNull(acordaoRepository);
        this.mediaAssetRepository = Objects.requireNonNull(mediaAssetRepository);
        this.esclarecimentoRepository = Objects.requireNonNull(esclarecimentoRepository);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "public_timeline", key = "'public-plenario:' + (#colegiado == null ? '' : #colegiado) + ':' + #page + ':' + #size")
    public List<SessaoPublicaDto> listarSessoes(String colegiado, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 100));
        return julgamentoRepository.findPublicSessoes(trimToNull(colegiado), PageRequest.of(safePage, safeSize))
                .stream()
                .map(this::toResumo)
                .toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "public_timeline", key = "'public-plenario-detail:' + #sessaoId")
    public SessaoPublicaDetalheDto detalhar(Long sessaoId) {
        JulgamentoColegiado julgamento = julgamentoRepository.findPublicByIdWithProcesso(sessaoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "sessao publica nao encontrada"));
        Acordao acordao = acordaoRepository.findByJulgamentoId(sessaoId).orElse(null);
        Processo processo = julgamento.getProcesso();
        return new SessaoPublicaDetalheDto(
                julgamento.getId(),
                processo != null ? processo.getId() : null,
                processo != null ? processo.getNumeroProcesso() : null,
                processo != null ? processo.getClasseProcessual() : null,
                processo != null ? processo.getAssunto() : null,
                julgamento.getTribunalSigla(),
                julgamento.getOrgaoJulgador(),
                julgamento.getRelatorNome(),
                julgamento.getRevisorNome(),
                julgamento.getStatus() != null ? julgamento.getStatus().name() : null,
                julgamento.getPautaDataHora(),
                julgamento.getSessaoInicio(),
                julgamento.getSessaoFim(),
                julgamento.getPlacarFavor(),
                julgamento.getPlacarContra(),
                julgamento.getPlacarParcial(),
                julgamento.getPlacarOutros(),
                julgamento.getAcordaoPublicado(),
                acordao != null ? acordao.getNumeroAcordao() : null,
                acordao != null ? acordao.getEmentaResumo() : null,
                acordao != null ? acordao.getInteiroTeorRef() : null,
                streamUrl(julgamento.getId()),
                mediaAssetRepository.findBySessao_IdAndPublicoTrueOrderByOrdemExibicaoAscCreatedAtAsc(sessaoId).stream().map(this::toMedia).toList(),
                esclarecimentoRepository.findBySessao_IdAndVisivelPublicamenteTrueOrderByCreatedAtAsc(sessaoId).stream().map(this::toEsclarecimento).toList()
        );
    }

    @Transactional(readOnly = true)
    public void validarSessaoPublica(Long sessaoId) {
        if (sessaoId == null || julgamentoRepository.findPublicByIdWithProcesso(sessaoId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "sessao publica nao encontrada");
        }
    }

    private SessaoPublicaDto toResumo(JulgamentoColegiado julgamento) {
        Processo processo = julgamento.getProcesso();
        return new SessaoPublicaDto(
                julgamento.getId(),
                processo != null ? processo.getId() : null,
                processo != null ? processo.getNumeroProcesso() : null,
                julgamento.getTribunalSigla(),
                julgamento.getOrgaoJulgador(),
                julgamento.getRelatorNome(),
                julgamento.getStatus() != null ? julgamento.getStatus().name() : null,
                julgamento.getPautaDataHora(),
                julgamento.getSessaoInicio(),
                julgamento.getSessaoFim(),
                julgamento.getAcordaoPublicado(),
                streamUrl(julgamento.getId())
        );
    }

    private SessaoPublicaMediaDto toMedia(PublicPlenarioMediaAsset media) {
        return new SessaoPublicaMediaDto(media.getId(), media.getTipo(), media.getTitulo(), media.getUrlPublica(), media.getHashIntegridade(), media.getCreatedAt());
    }

    private SessaoPublicaEsclarecimentoDto toEsclarecimento(PublicPlenarioEsclarecimentoFato entity) {
        return new SessaoPublicaEsclarecimentoDto(entity.getId(), entity.getResumoDuvida(), entity.getRespostaPublica(), entity.getStatus(),
                entity.getCreatedAt(), entity.getRespondidoEm());
    }

    private String streamUrl(Long julgamentoId) {
        return "/api/v1/public/plenario/sessoes/" + julgamentoId + "/stream";
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
