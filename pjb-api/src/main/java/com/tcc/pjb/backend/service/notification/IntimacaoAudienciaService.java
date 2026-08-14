package com.tcc.pjb.backend.service.notification;

import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.dto.intimacao.CriarIntimacaoRequest;
import com.tcc.pjb.backend.model.dto.intimacao.IntimacaoAudienciaResponse;
import com.tcc.pjb.backend.model.entity.Audiencia;
import com.tcc.pjb.backend.model.entity.IntimacaoAudiencia;
import com.tcc.pjb.backend.model.entity.enums.AcaoProcessualServidor;
import com.tcc.pjb.backend.model.entity.enums.StatusIntimacaoAudiencia;
import com.tcc.pjb.backend.model.repository.AudienciaRepository;
import com.tcc.pjb.backend.model.repository.IntimacaoAudienciaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class IntimacaoAudienciaService {

    private static final int PRAZO_CIENCIA_DIAS_PADRAO = 5;

    private final IntimacaoAudienciaRepository intimacaoRepository;
    private final AudienciaRepository audienciaRepository;
    private final PjbAuthorizationService authorizationService;

    public IntimacaoAudienciaService(IntimacaoAudienciaRepository intimacaoRepository,
                                      AudienciaRepository audienciaRepository,
                                      PjbAuthorizationService authorizationService) {
        this.intimacaoRepository = Objects.requireNonNull(intimacaoRepository);
        this.audienciaRepository = Objects.requireNonNull(audienciaRepository);
        this.authorizationService = Objects.requireNonNull(authorizationService);
    }

    @Transactional
    public Optional<IntimacaoAudienciaResponse> intimar(Long audienciaId, CriarIntimacaoRequest request) {
        return audienciaRepository.findById(audienciaId).map(audiencia -> {
            authorizationService.requireFuncaoServidorCapability(audiencia.getProcesso(), AcaoProcessualServidor.INTIMAR);
            Instant prazo = request.prazoCiencia() != null
                    ? request.prazoCiencia()
                    : Instant.now().plus(PRAZO_CIENCIA_DIAS_PADRAO, ChronoUnit.DAYS);
            IntimacaoAudiencia intimacao = IntimacaoAudiencia.builder()
                    .audiencia(audiencia)
                    .destinatarioNome(request.destinatarioNome())
                    .destinatarioTipo(request.destinatarioTipo())
                    .destinatarioOab(request.destinatarioOab())
                    .destinatarioEmail(request.destinatarioEmail())
                    .canal(request.canal())
                    .status(StatusIntimacaoAudiencia.PENDENTE)
                    .prazoCiencia(prazo)
                    .criadoEm(Instant.now())
                    .build();
            return IntimacaoAudienciaResponse.de(intimacaoRepository.save(intimacao));
        });
    }

    @Transactional
    public List<IntimacaoAudienciaResponse> intimarLote(Long audienciaId, List<CriarIntimacaoRequest> requests) {
        Audiencia audiencia = audienciaRepository.findById(audienciaId)
                .orElseThrow(() -> new IllegalArgumentException("Audiência não encontrada: " + audienciaId));
        authorizationService.requireFuncaoServidorCapability(audiencia.getProcesso(), AcaoProcessualServidor.INTIMAR);
        Instant agora = Instant.now();
        return requests.stream().map(req -> {
            Instant prazo = req.prazoCiencia() != null
                    ? req.prazoCiencia()
                    : agora.plus(PRAZO_CIENCIA_DIAS_PADRAO, ChronoUnit.DAYS);
            IntimacaoAudiencia intimacao = IntimacaoAudiencia.builder()
                    .audiencia(audiencia)
                    .destinatarioNome(req.destinatarioNome())
                    .destinatarioTipo(req.destinatarioTipo())
                    .destinatarioOab(req.destinatarioOab())
                    .destinatarioEmail(req.destinatarioEmail())
                    .canal(req.canal())
                    .status(StatusIntimacaoAudiencia.PENDENTE)
                    .prazoCiencia(prazo)
                    .criadoEm(agora)
                    .build();
            return IntimacaoAudienciaResponse.de(intimacaoRepository.save(intimacao));
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<IntimacaoAudienciaResponse> listarPorAudiencia(Long audienciaId) {
        return intimacaoRepository.findByAudiencia_IdOrderByCriadoEmAsc(audienciaId)
                .stream().map(IntimacaoAudienciaResponse::de).toList();
    }

    @Transactional
    public Optional<IntimacaoAudienciaResponse> registrarCiencia(Long intimacaoId) {
        return intimacaoRepository.findById(intimacaoId).map(intimacao -> {
            if (intimacao.getStatus().isTerminal()) {
                throw new IllegalStateException("Intimação já encerrada");
            }
            intimacao.setStatus(StatusIntimacaoAudiencia.CIENCIA_CONFIRMADA);
            intimacao.setCienciaEm(Instant.now());
            return IntimacaoAudienciaResponse.de(intimacaoRepository.save(intimacao));
        });
    }

    @Transactional
    public Optional<IntimacaoAudienciaResponse> marcarEnviada(Long intimacaoId) {
        return intimacaoRepository.findById(intimacaoId).map(intimacao -> {
            if (intimacao.getStatus() != StatusIntimacaoAudiencia.PENDENTE) {
                throw new IllegalStateException("Intimação não está pendente");
            }
            intimacao.setStatus(StatusIntimacaoAudiencia.ENVIADA);
            intimacao.setEnviadaEm(Instant.now());
            return IntimacaoAudienciaResponse.de(intimacaoRepository.save(intimacao));
        });
    }
}
