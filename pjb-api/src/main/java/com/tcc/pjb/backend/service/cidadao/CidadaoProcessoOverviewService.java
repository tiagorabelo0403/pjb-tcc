package com.tcc.pjb.backend.service.cidadao;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.dto.cidadao.AreaLinks;
import com.tcc.pjb.backend.model.dto.cidadao.CidadaoProcessoCardDto;
import com.tcc.pjb.backend.model.dto.cidadao.CidadaoProcessoOverviewResponse;
import com.tcc.pjb.backend.model.entity.Audiencia;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.julgamento.JulgamentoColegiado;
import com.tcc.pjb.backend.model.entity.workflow.MovimentacaoProcessual;
import com.tcc.pjb.backend.model.repository.AudienciaRepository;
import com.tcc.pjb.backend.model.repository.MovimentacaoProcessualRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.julgamento.JulgamentoColegiadoRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import com.tcc.pjb.backend.service.processo.ProcessoObservabilidadeAcessoService;

@Service
public class CidadaoProcessoOverviewService {

    private final CurrentUserService currentUser;
    private final PjbAuthorizationService authz;
    private final ProcessoRepository processoRepo;
    private final MovimentacaoProcessualRepository movRepo;
    private final DocumentoProcessualRepository docRepo;
    private final AudienciaRepository audienciaRepo;
    private final JulgamentoColegiadoRepository julgamentoRepo;
    private final CidadaoProcessoCardMapper cardMapper;
    private final ProcessoObservabilidadeAcessoService processoObservabilidadeAcessoService;
    private final com.tcc.pjb.backend.service.security.access.PersonalProcessAccessGuardService personalProcessAccessGuardService;

    public CidadaoProcessoOverviewService(CurrentUserService currentUser,
                                         PjbAuthorizationService authz,
                                         ProcessoRepository processoRepo,
                                         MovimentacaoProcessualRepository movRepo,
                                         DocumentoProcessualRepository docRepo,
                                         AudienciaRepository audienciaRepo,
                                         JulgamentoColegiadoRepository julgamentoRepo,
                                         CidadaoProcessoCardMapper cardMapper,
                                         ProcessoObservabilidadeAcessoService processoObservabilidadeAcessoService,
                                         com.tcc.pjb.backend.service.security.access.PersonalProcessAccessGuardService personalProcessAccessGuardService) {
        this.currentUser = Objects.requireNonNull(currentUser);
        this.authz = Objects.requireNonNull(authz);
        this.processoRepo = Objects.requireNonNull(processoRepo);
        this.movRepo = Objects.requireNonNull(movRepo);
        this.docRepo = Objects.requireNonNull(docRepo);
        this.audienciaRepo = Objects.requireNonNull(audienciaRepo);
        this.julgamentoRepo = Objects.requireNonNull(julgamentoRepo);
        this.cardMapper = Objects.requireNonNull(cardMapper);
        this.processoObservabilidadeAcessoService = Objects.requireNonNull(processoObservabilidadeAcessoService);
        this.personalProcessAccessGuardService = Objects.requireNonNull(personalProcessAccessGuardService);
    }

    public CidadaoProcessoOverviewResponse overview(Long processoId) {
        if (processoId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "processoId obrigatório");
        }
        personalProcessAccessGuardService.requireOwnProcessAccess("OVERVIEW_PROCESSO_PESSOAL");
        currentUser.getRequired();

        Processo p = processoRepo.findProcessoCompletoById(processoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Processo não encontrado"));

        personalProcessAccessGuardService.requireCurrentUserAsParty(p);

        MovimentacaoProcessual ultima = null;
        List<MovimentacaoProcessual> latestList = movRepo.findLatestByProcessoIds(List.of(processoId));
        if (!latestList.isEmpty()) ultima = latestList.getFirst();

        long docCount = 0L;
        for (DocumentoProcessualRepository.ProcessoDocCount row : docRepo.countDocsByProcessoIds(List.of(processoId))) {
            if (row != null && Objects.equals(row.getProcessoId(), processoId)) {
                docCount = row.getCnt();
                break;
            }
        }

        Audiencia nextAud = null;
        var audList = audienciaRepo.findNextUpcomingByProcessoIds(new long[]{processoId}, LocalDateTime.now());
        if (!audList.isEmpty()) nextAud = audList.getFirst();

        JulgamentoColegiado nextJulg = null;
        var julgList = julgamentoRepo.findNextPautaByProcessoIds(new long[]{processoId}, LocalDateTime.now());
        if (!julgList.isEmpty()) nextJulg = julgList.getFirst();

        CidadaoProcessoCardDto card = cardMapper.toCard(p, ultima, docCount, nextAud, nextJulg);

        return new CidadaoProcessoOverviewResponse(
                LocalDateTime.now(),
                processoId,
                card,
                "/api/v1/ui/legend",
                defaultLinks(),
                "/api/v1/cidadao/processos/" + processoId + "/instancias",
                "/api/v1/cidadao/processos/" + processoId + "/julgamentos",
                processoObservabilidadeAcessoService.resumir(p)
        );
    }

    private static AreaLinks defaultLinks() {
        return new AreaLinks(
                "/api/v1/ui/legend",
                "/api/v1/ui/accessibility/preference",
                "/api/v1/ui/presentation/reading-preference",
                "/api/v1/ui/presentation/bundle",
                "/api/v1/chat",
                "/api/v1/chat/processo/{processoId}"
        );
    }
}
