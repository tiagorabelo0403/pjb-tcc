package com.tcc.pjb.backend.service.processual.celerity;

import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class MotorCeleridadeConstitucionalService {

    public record CeleridadeAnaliseInput(
            UUID processoId,
            String numeroProcesso,
            RitoProcessual rito,
            FaseProcessual fase,
            Duration tempoParado,
            boolean reuPreso,
            boolean tutelaUrgentePendente,
            boolean partePrioridade,
            boolean temExpedientePendente,
            boolean temDocumentoSemAssinatura,
            boolean temTarefaSemResponsavel,
            boolean temMandadoDevolvido,
            boolean temDecursoNaoCertificado
    ) {}

    public record CeleridadeProcessoResponse(
            UUID processoId,
            CeleridadeNivel nivel,
            List<CeleridadeAlertaNatureza> alertas,
            List<CeleridadeBottleneckDiagnosisService.DiagnosticoGargalo> gargalos,
            List<CeleridadeNextBestActionService.ProximaAcao> proximasAcoes,
            ProcessoReadinessSemaphoreService.SemaforoResultado semaforo,
            String mensagemAceleracaoSegura
    ) {}

    private final CeleridadeRitoPolicyRegistry policyRegistry;
    private final CeleridadeBottleneckDiagnosisService diagnosisService;
    private final CeleridadeNextBestActionService nextActionService;
    private final ProcessoReadinessSemaphoreService semaphoreService;

    public MotorCeleridadeConstitucionalService(
            CeleridadeRitoPolicyRegistry policyRegistry,
            CeleridadeBottleneckDiagnosisService diagnosisService,
            CeleridadeNextBestActionService nextActionService,
            ProcessoReadinessSemaphoreService semaphoreService) {
        this.policyRegistry = policyRegistry;
        this.diagnosisService = diagnosisService;
        this.nextActionService = nextActionService;
        this.semaphoreService = semaphoreService;
    }

    public CeleridadeProcessoResponse analisar(CeleridadeAnaliseInput input) {
        CeleridadeNivel nivel = calcularNivel(input);
        List<CeleridadeAlertaNatureza> alertas = calcularAlertas(input, nivel);

        CeleridadeBottleneckDiagnosisService.DiagnosticoInput diagInput =
                new CeleridadeBottleneckDiagnosisService.DiagnosticoInput(
                        input.rito(), input.fase(), input.tempoParado(),
                        input.temExpedientePendente(), input.temDocumentoSemAssinatura(),
                        input.temTarefaSemResponsavel(), input.temMandadoDevolvido(),
                        input.reuPreso());

        List<CeleridadeBottleneckDiagnosisService.DiagnosticoGargalo> gargalos =
                diagnosisService.diagnosticar(diagInput);

        List<CeleridadeNextBestActionService.ProximaAcao> acoes =
                nextActionService.sugerirProximasAcoes(gargalos, nivel);

        ProcessoReadinessSemaphoreService.SemaforoInput semaforoInput =
                new ProcessoReadinessSemaphoreService.SemaforoInput(
                        input.rito(), false, false, false,
                        input.temExpedientePendente(), false, false);

        ProcessoReadinessSemaphoreService.SemaforoResultado semaforo =
                semaphoreService.avaliar(semaforoInput);

        String mensagem = acoes.isEmpty()
                ? "Processo sem gargalo cartorário imediato identificado."
                : JudicialIndependenceSafeAccelerationPolicy.mensagemAceleracaoSegura(
                        acoes.get(0).tipo(), 1);

        return new CeleridadeProcessoResponse(
                input.processoId(), nivel, alertas, gargalos, acoes, semaforo, mensagem);
    }

    private CeleridadeNivel calcularNivel(CeleridadeAnaliseInput input) {
        if (input.reuPreso() && input.tempoParado().toDays() > 10) return CeleridadeNivel.CRITICO;
        if (input.tutelaUrgentePendente()) return CeleridadeNivel.URGENTE;
        CeleridadeNivel nivelRito = policyRegistry.avaliarNivel(input.rito(), input.tempoParado());
        if (input.partePrioridade() && nivelRito.compareTo(CeleridadeNivel.ATENCAO) < 0)
            return CeleridadeNivel.ATENCAO;
        return nivelRito;
    }

    private List<CeleridadeAlertaNatureza> calcularAlertas(
            CeleridadeAnaliseInput input, CeleridadeNivel nivel) {
        var alertas = new java.util.ArrayList<CeleridadeAlertaNatureza>();
        if (input.reuPreso()) alertas.add(CeleridadeAlertaNatureza.REU_PRESO_PRAZO);
        if (input.tutelaUrgentePendente()) alertas.add(CeleridadeAlertaNatureza.TUTELA_URGENTE_PENDENTE);
        if (input.partePrioridade()) alertas.add(CeleridadeAlertaNatureza.IDOSO_PRIORIDADE);
        if (input.rito().isFamiliaSucessoes()) alertas.add(CeleridadeAlertaNatureza.ALIMENTOS);
        if (nivel == CeleridadeNivel.CRITICO || nivel == CeleridadeNivel.URGENTE)
            alertas.add(CeleridadeAlertaNatureza.URGENCIA_CONSTITUCIONAL);
        return alertas;
    }
}
