package com.tcc.pjb.backend.core.comunicacao.judicial;

import com.tcc.pjb.backend.model.entity.Processo;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CitacaoDespachoFisicoService {

    private static final Logger log = LoggerFactory.getLogger(CitacaoDespachoFisicoService.class);

    private final ExpedicaoJudicialRepository expedicaoRepository;
    private final CitacaoOficialJusticaQrMandadoService citacaoOficialJusticaQrMandadoService;
    private final CitacaoEditalCuradoriaService citacaoEditalCuradoriaService;
    private final CitacaoJudiciaryNotificationService citacaoJudiciaryNotificationService;
    private final CitacaoExpedicaoNotificacaoService citacaoExpedicaoNotificacaoService;

    public CitacaoDespachoFisicoService(ExpedicaoJudicialRepository expedicaoRepository,
                                        CitacaoOficialJusticaQrMandadoService citacaoOficialJusticaQrMandadoService,
                                        CitacaoEditalCuradoriaService citacaoEditalCuradoriaService,
                                        CitacaoJudiciaryNotificationService citacaoJudiciaryNotificationService,
                                        CitacaoExpedicaoNotificacaoService citacaoExpedicaoNotificacaoService) {
        this.expedicaoRepository = Objects.requireNonNull(expedicaoRepository, "expedicaoRepository");
        this.citacaoOficialJusticaQrMandadoService = Objects.requireNonNull(citacaoOficialJusticaQrMandadoService, "citacaoOficialJusticaQrMandadoService");
        this.citacaoEditalCuradoriaService = Objects.requireNonNull(citacaoEditalCuradoriaService, "citacaoEditalCuradoriaService");
        this.citacaoJudiciaryNotificationService = Objects.requireNonNull(citacaoJudiciaryNotificationService, "citacaoJudiciaryNotificationService");
        this.citacaoExpedicaoNotificacaoService = Objects.requireNonNull(citacaoExpedicaoNotificacaoService, "citacaoExpedicaoNotificacaoService");
    }

    public void despacharOficialJustica(ExpedicaoJudicial expedicao, Processo processo) {
        expedicao.setStatus(ExpedicaoJudicial.StatusExpedicao.PENDENTE_OFICIAL);
        expedicaoRepository.save(expedicao);
        citacaoOficialJusticaQrMandadoService.gerarSeCabivel(expedicao);
        if (expedicao.getServidorExpedidorId() != null) {
            citacaoJudiciaryNotificationService.notificarServidor(expedicao.getServidorExpedidorId(), expedicao, processo, "Mandado físico gerado. Atribuir a Oficial de Justiça.");
        }
        log.info("[CitacaoDespachoFisico][OficialJustica] Mandado criado uuid={}", expedicao.getExpedicaoUuid());
    }

    public void despacharCorreioArDigital(ExpedicaoJudicial expedicao) {
        String codigoRastreio = "PJB" + expedicao.getExpedicaoUuid().replace("-", "").substring(0, 13).toUpperCase(Locale.ROOT);
        expedicao.setCodigoRastreioCorreio(codigoRastreio);
        expedicao.setStatus(ExpedicaoJudicial.StatusExpedicao.REMETIDA_CORREIO);
        expedicaoRepository.save(expedicao);
        citacaoOficialJusticaQrMandadoService.gerarSeCabivel(expedicao);
        log.info("[CitacaoDespachoFisico][CorreioAR] uuid={} rastreio={}", expedicao.getExpedicaoUuid(), codigoRastreio);
    }

    public void despacharEdital(ExpedicaoJudicial expedicao, Processo processo) {
        String numeroEdital = "EDT-" + Instant.now().getEpochSecond() + "-" + expedicao.getProcessoId();
        expedicao.setNumeroEdital(numeroEdital);
        expedicao.setStatus(ExpedicaoJudicial.StatusExpedicao.PUBLICADA_EDITAL);
        expedicaoRepository.save(expedicao);
        citacaoEditalCuradoriaService.registrarNecessidadeSeAusente(expedicao);
        citacaoExpedicaoNotificacaoService.notificarEditalPublicado(expedicao, processo);
        citacaoJudiciaryNotificationService.notificarJuizEdital(expedicao, processo, numeroEdital);
        log.warn("[CitacaoDespachoFisico][Edital] Ultimo recurso ativado uuid={} edital={}", expedicao.getExpedicaoUuid(), numeroEdital);
    }
}
