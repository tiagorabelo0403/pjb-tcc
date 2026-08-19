package com.tcc.pjb.backend.core.processo.conclusao.application;

import com.tcc.pjb.backend.core.processo.estado.application.ProcessoEstadoApplicationService;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.AcaoProcessualServidor;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.processo.ConclusaoProcessual;
import com.tcc.pjb.backend.model.repository.ConclusaoProcessualRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import jakarta.inject.Inject;
import jakarta.persistence.EntityNotFoundException;
import java.lang.SecurityException;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.platform.runtime.PjbTransactionalBudget;

@Service
public class ConclusaoProcessualApplicationService {

    private static final int BATCH_SIZE = 50;
    private static final int DIAS_UTEIS_LIMITE = 10;

    private final ConclusaoProcessualRepository conclusaoRepository;
    private final ProcessoRepository processoRepository;
    private final PjbAuthorizationService authorizationService;
    private final ProcessoEstadoApplicationService estadoService;

    @Inject
    public ConclusaoProcessualApplicationService(
            ConclusaoProcessualRepository conclusaoRepository,
            ProcessoRepository processoRepository,
            PjbAuthorizationService authorizationService,
            ProcessoEstadoApplicationService estadoService) {
        this.conclusaoRepository = conclusaoRepository;
        this.processoRepository = processoRepository;
        this.authorizationService = authorizationService;
        this.estadoService = estadoService;
    }

    @Transactional
    public ConclusaoProcessual concluir(Long processoId, Long magistradoId, Long servidorId,
                                         String tipoConclusao, String motivo) {
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new EntityNotFoundException("Processo não encontrado: " + processoId));
        authorizationService.requireFuncaoServidorCapability(processo, AcaoProcessualServidor.CONCLUIR);
        Instant dataLimite = calcularDataLimite(DIAS_UTEIS_LIMITE);
        ConclusaoProcessual conclusao = new ConclusaoProcessual(
                processoId, magistradoId, servidorId, tipoConclusao, motivo, dataLimite);
        conclusaoRepository.save(conclusao);
        StatusProcesso novoEstado = "PARA_VOTO".equals(tipoConclusao) || "PARA_RELATORIO".equals(tipoConclusao)
                ? StatusProcesso.CONCLUSO_RELATOR
                : StatusProcesso.CONCLUSO_JUIZ;
        estadoService.transitar(processoId, novoEstado, servidorId, "Conclusão: " + tipoConclusao);
        return conclusao;
    }

    @Transactional
    public void devolver(Long conclusaoId, Long magistradoId) {
        ConclusaoProcessual conclusao = conclusaoRepository.findById(conclusaoId)
                .orElseThrow(() -> new EntityNotFoundException("Conclusão não encontrada: " + conclusaoId));
        if (!conclusao.getMagistradoId().equals(magistradoId)) {
            throw new SecurityException("Somente o magistrado destinatário pode devolver.");
        }
        if (!conclusao.isPendente()) {
            throw new IllegalStateException("Conclusão não está pendente: " + conclusao.getStatus());
        }
        conclusao.devolver();
        conclusaoRepository.save(conclusao);
        estadoService.transitar(conclusao.getProcessoId(), StatusProcesso.DISTRIBUIDO,
                magistradoId, "Devolução de conclusão");
    }

    @PjbTransactionalBudget(operation = "processo.conclusao.processar-expiradas", maxMillis = 8000)
    @Transactional
    public int processarExpiradas() {
        Instant agora = Instant.now();
        List<ConclusaoProcessual> expiradas = conclusaoRepository.findExpiradas(
                agora, PageRequest.of(0, BATCH_SIZE));
        for (ConclusaoProcessual c : expiradas) {
            c.expirar();
        }
        conclusaoRepository.saveAll(expiradas);
        return expiradas.size();
    }

    @Transactional(readOnly = true)
    public List<ConclusaoProcessual> pendentesDoMagistrado(Long magistradoId) {
        return conclusaoRepository.findByMagistradoIdAndStatus(magistradoId, "PENDENTE");
    }

    private Instant calcularDataLimite(int diasUteis) {
        LocalDate data = LocalDate.now(ZoneOffset.UTC);
        int count = 0;
        while (count < diasUteis) {
            data = data.plusDays(1);
            DayOfWeek dow = data.getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
                count++;
            }
        }
        return data.atStartOfDay(ZoneOffset.UTC).toInstant();
    }
}
