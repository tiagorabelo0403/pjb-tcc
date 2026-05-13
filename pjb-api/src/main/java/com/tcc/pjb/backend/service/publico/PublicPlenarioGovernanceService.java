package com.tcc.pjb.backend.service.publico;

import java.time.Instant;
import java.util.Objects;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.dto.publico.SessaoPublicaEsclarecimentoDto;
import com.tcc.pjb.backend.model.dto.publico.SessaoPublicaMediaDto;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.julgamento.JulgamentoColegiado;
import com.tcc.pjb.backend.model.entity.publico.PublicPlenarioEsclarecimentoFato;
import com.tcc.pjb.backend.model.entity.publico.PublicPlenarioMediaAsset;
import com.tcc.pjb.backend.model.repository.PublicPlenarioEsclarecimentoFatoRepository;
import com.tcc.pjb.backend.model.repository.PublicPlenarioMediaAssetRepository;
import com.tcc.pjb.backend.model.repository.julgamento.JulgamentoColegiadoRepository;

@Service
public class PublicPlenarioGovernanceService {

    private final JulgamentoColegiadoRepository julgamentoRepository;
    private final PublicPlenarioMediaAssetRepository mediaRepository;
    private final PublicPlenarioEsclarecimentoFatoRepository esclarecimentoRepository;
    private final CurrentUserService currentUserService;

    public PublicPlenarioGovernanceService(JulgamentoColegiadoRepository julgamentoRepository,
                                           PublicPlenarioMediaAssetRepository mediaRepository,
                                           PublicPlenarioEsclarecimentoFatoRepository esclarecimentoRepository,
                                           CurrentUserService currentUserService) {
        this.julgamentoRepository = Objects.requireNonNull(julgamentoRepository);
        this.mediaRepository = Objects.requireNonNull(mediaRepository);
        this.esclarecimentoRepository = Objects.requireNonNull(esclarecimentoRepository);
        this.currentUserService = Objects.requireNonNull(currentUserService);
    }

    @Transactional
    public SessaoPublicaMediaDto registrarMidia(Long sessaoId, MediaRegistrationRequest request) {
        JulgamentoColegiado sessao = julgamentoRepository.findByIdWithProcesso(sessaoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "sessao colegiada nao encontrada"));
        PublicPlenarioMediaAsset media = new PublicPlenarioMediaAsset();
        media.setSessao(sessao);
        media.setUploadedBy(currentUserService.getOrNull());
        media.setTipo(normalizeType(request.tipo()));
        media.setTitulo(request.titulo());
        media.setUrlPublica(request.urlPublica());
        media.setHashIntegridade(request.hashIntegridade() == null || request.hashIntegridade().isBlank()
                ? Hashes.sha256Hex(request.urlPublica())
                : request.hashIntegridade());
        media.setPublico(request.publico());
        media.setOrdemExibicao(request.ordemExibicao());
        return toMedia(mediaRepository.save(media));
    }

    @Transactional
    public SessaoPublicaEsclarecimentoDto registrarEsclarecimento(Long sessaoId, EsclarecimentoRequest request) {
        JulgamentoColegiado sessao = julgamentoRepository.findByIdWithProcesso(sessaoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "sessao colegiada nao encontrada"));
        PublicPlenarioEsclarecimentoFato entity = new PublicPlenarioEsclarecimentoFato();
        entity.setSessao(sessao);
        entity.setSolicitante(currentUserService.getOrNull());
        entity.setResumoDuvida(request.resumoDuvida());
        entity.setStatus("ABERTO");
        entity.setVisivelPublicamente(request.visivelPublicamente());
        return toEsclarecimento(esclarecimentoRepository.save(entity));
    }

    @Transactional
    public SessaoPublicaEsclarecimentoDto responderEsclarecimento(Long esclarecimentoId, RespostaRequest request) {
        PublicPlenarioEsclarecimentoFato entity = esclarecimentoRepository.findById(esclarecimentoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "esclarecimento nao encontrado"));
        Usuario current = currentUserService.getOrNull();
        entity.setRespondidoPor(current);
        entity.setRespostaPublica(request.respostaPublica());
        entity.setStatus("RESPONDIDO");
        entity.setVisivelPublicamente(request.visivelPublicamente());
        entity.setRespondidoEm(Instant.now());
        return toEsclarecimento(esclarecimentoRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public SessaoGovernancaView detalharGovernanca(Long sessaoId) {
        julgamentoRepository.findByIdWithProcesso(sessaoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "sessao colegiada nao encontrada"));
        return new SessaoGovernancaView(
                sessaoId,
                mediaRepository.findBySessao_IdOrderByOrdemExibicaoAscCreatedAtAsc(sessaoId).stream().map(this::toMedia).toList(),
                esclarecimentoRepository.findBySessao_IdOrderByCreatedAtAsc(sessaoId).stream().map(this::toEsclarecimento).toList()
        );
    }

    private SessaoPublicaMediaDto toMedia(PublicPlenarioMediaAsset media) {
        return new SessaoPublicaMediaDto(media.getId(), media.getTipo(), media.getTitulo(), media.getUrlPublica(), media.getHashIntegridade(), media.getCreatedAt());
    }

    private SessaoPublicaEsclarecimentoDto toEsclarecimento(PublicPlenarioEsclarecimentoFato entity) {
        return new SessaoPublicaEsclarecimentoDto(
                entity.getId(),
                entity.getResumoDuvida(),
                entity.getRespostaPublica(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getRespondidoEm()
        );
    }

    private String normalizeType(String tipo) {
        if (tipo == null || tipo.isBlank()) {
            return "DOCUMENTO_PUBLICO";
        }
        return tipo.trim().toUpperCase(java.util.Locale.ROOT);
    }

    public record MediaRegistrationRequest(
            @NotBlank String tipo,
            @NotBlank String titulo,
            @NotBlank String urlPublica,
            String hashIntegridade,
            Integer ordemExibicao,
            boolean publico
    ) {
    }

    public record EsclarecimentoRequest(
            @NotBlank String resumoDuvida,
            boolean visivelPublicamente
    ) {
    }

    public record RespostaRequest(
            @NotBlank String respostaPublica,
            boolean visivelPublicamente
    ) {
    }

    public record SessaoGovernancaView(
            Long sessaoId,
            java.util.List<SessaoPublicaMediaDto> midias,
            java.util.List<SessaoPublicaEsclarecimentoDto> esclarecimentos
    ) {
    }
}
