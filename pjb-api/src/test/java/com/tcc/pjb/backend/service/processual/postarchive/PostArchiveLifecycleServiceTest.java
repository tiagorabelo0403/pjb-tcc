package com.tcc.pjb.backend.service.processual.postarchive;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleAction;
import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleDecision;
import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleMachine;
import com.tcc.pjb.backend.core.processual.ato.AtoProcessualDescriptor;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.core.transito.PostJudgmentOperationalProfile;
import com.tcc.pjb.backend.core.transito.PostJudgmentOperationalResolver;
import com.tcc.pjb.backend.core.transito.TransitoJulgadoArquivamentoEngine;
import com.tcc.pjb.backend.model.dto.transito.PostArchiveLifecycleRequest;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.workflow.MovimentacaoProcessual;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.MovimentacaoProcessualRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class PostArchiveLifecycleServiceTest {

    @Test
    void triggersDesarquivamentoWhenRequested() {
        ProcessoRepository processoRepository = Mockito.mock(ProcessoRepository.class);
        WorkItemRepository workItemRepository = Mockito.mock(WorkItemRepository.class);
        DocumentoProcessualRepository documentoRepository = Mockito.mock(DocumentoProcessualRepository.class);
        MovimentacaoProcessualRepository movimentacaoRepository = Mockito.mock(MovimentacaoProcessualRepository.class);
        PjbAuthorizationService authorizationService = Mockito.mock(PjbAuthorizationService.class);
        TransitoJulgadoArquivamentoEngine transitoEngine = Mockito.mock(TransitoJulgadoArquivamentoEngine.class);
        ProcessoLifecycleMachine lifecycleMachine = Mockito.mock(ProcessoLifecycleMachine.class);
        PostJudgmentOperationalResolver operationalResolver = Mockito.mock(PostJudgmentOperationalResolver.class);
        PostArchiveLifecycleService service = new PostArchiveLifecycleService(
                processoRepository,
                workItemRepository,
                documentoRepository,
                movimentacaoRepository,
                authorizationService,
                transitoEngine,
                lifecycleMachine,
                operationalResolver
        );
        Processo processo = new Processo();
        processo.setId(22L);
        processo.setNumeroProcesso("0022-44");
        processo.setStatus(StatusProcesso.ARQUIVADO);
        processo.setFaseAtual(FaseProcessual.CONHECIMENTO);
        when(processoRepository.findById(22L)).thenReturn(Optional.of(processo));
        when(workItemRepository.findAllByProcesso(22L)).thenReturn(java.util.List.of(WorkItem.builder().status(WorkItemStatus.PENDENTE).build()));
        when(documentoRepository.countByProcesso_Id(22L)).thenReturn(3L);
        MovimentacaoProcessual mov = MovimentacaoProcessual.builder().dataMovimentacao(Instant.now()).build();
        when(movimentacaoRepository.findTop200ByProcesso_IdOrderByDataMovimentacaoDesc(22L)).thenReturn(java.util.List.of(mov));
        when(lifecycleMachine.preview(processo, ProcessoLifecycleAction.ARQUIVAR)).thenReturn(decision(ProcessoLifecycleAction.ARQUIVAR, true));
        when(lifecycleMachine.preview(processo, ProcessoLifecycleAction.DESARQUIVAR)).thenReturn(decision(ProcessoLifecycleAction.DESARQUIVAR, true));
        when(operationalResolver.resolve(processo, ProcessoLifecycleAction.ARQUIVAR, "novo incidente", 0D)).thenReturn(profile("ARQUIVAMENTO"));
        when(operationalResolver.resolve(processo, ProcessoLifecycleAction.DESARQUIVAR, "novo incidente", 0D)).thenReturn(profile("DESARQUIVAMENTO"));

        var response = service.evaluate(new PostArchiveLifecycleRequest(22L, true, "novo incidente", true, true, 5));

        assertTrue(response.desarquivamentoSolicitado());
        verify(transitoEngine).abrirDesarquivamento(22L, "novo incidente");
    }

    private ProcessoLifecycleDecision decision(ProcessoLifecycleAction action, boolean permitida) {
        return new ProcessoLifecycleDecision(
                action,
                FaseProcessual.CONHECIMENTO,
                StatusProcesso.ARQUIVADO,
                FaseProcessual.CONHECIMENTO,
                StatusProcesso.ARQUIVADO,
                permitida,
                permitida ? "OK" : "BLOQUEADO",
                TipoUsuario.SERVIDOR_FORUM.name(),
                new AtoProcessualDescriptor("COD", "Desc", null, null, null, null, null),
                java.util.List.of()
        );
    }

    private PostJudgmentOperationalProfile profile(String queueCode) {
        return new PostJudgmentOperationalProfile(
                "OPERACAO",
                queueCode,
                "INBOX",
                TipoUsuario.SERVIDOR_FORUM,
                1,
                false,
                1,
                ChronoUnit.DAYS,
                "BASE",
                "COISA_JULGADA",
                "EXEC_TRACK",
                "ARCHIVE_TRACK",
                "REVIEW",
                "RETENCAO",
                "SATISFACAO",
                java.util.List.of(),
                java.util.List.of(),
                java.util.List.of(),
                new LinkedHashMap<>()
        );
    }
}
