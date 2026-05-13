package com.tcc.pjb.backend.core.processo.sigilo.application;

import com.tcc.pjb.backend.core.processo.sigilo.domain.ProcessoSigiloDestinatario;
import com.tcc.pjb.backend.core.processo.sigilo.domain.ProcessoSigiloInteligenteAggregate;
import com.tcc.pjb.backend.core.processo.sigilo.domain.ProcessoSigiloNotificacaoAggregate;
import com.tcc.pjb.backend.core.processo.sigilo.domain.ProcessoSigiloNotificacaoItem;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.notification.NotificationService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ProcessoSigiloNotificacaoApplicationService {

    private final ProcessoRepository processoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProcessoSigiloInteligenteApplicationService processoSigiloInteligenteApplicationService;
    private final NotificationService notificationService;

    public ProcessoSigiloNotificacaoApplicationService(ProcessoRepository processoRepository,
                                                       UsuarioRepository usuarioRepository,
                                                       ProcessoSigiloInteligenteApplicationService processoSigiloInteligenteApplicationService,
                                                       NotificationService notificationService) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.usuarioRepository = Objects.requireNonNull(usuarioRepository);
        this.processoSigiloInteligenteApplicationService = Objects.requireNonNull(processoSigiloInteligenteApplicationService);
        this.notificationService = Objects.requireNonNull(notificationService);
    }

    public ProcessoSigiloNotificacaoAggregate planejar(Long processoId) {
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        ProcessoSigiloInteligenteAggregate diagnostico = processoSigiloInteligenteApplicationService.avaliar(processoId);
        ArrayList<ProcessoSigiloNotificacaoItem> notificacoes = new ArrayList<>();
        LinkedHashSet<String> canais = new LinkedHashSet<>();
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>(diagnostico.fundamentos());
        fundamentos.add("As notificações não decretam o sigilo; apenas abrem a trilha de revisão, ciência e preparação institucional.");

        for (ProcessoSigiloDestinatario destinatario : diagnostico.destinatarios()) {
            boolean highPriority = diagnostico.revisaoJudicialObrigatoria() || diagnostico.operacaoPolicialSigilosa();
            String title = titulo(diagnostico, destinatario);
            String message = mensagem(diagnostico, destinatario, processo);
            String action = diagnostico.decretoExclusivoMagistrado()
                    ? "ABRIR_REVISAO_DE_SIGILO"
                    : "ABRIR_GUARDAS_DE_SIGILO";
            ProcessoSigiloNotificacaoItem item = new ProcessoSigiloNotificacaoItem(
                    destinatario.usuarioId(),
                    destinatario.audienceCode(),
                    destinatario.audienceLabel(),
                    destinatario.channels(),
                    highPriority,
                    title,
                    message,
                    action,
                    "/api/v1/processual/unificado/" + processoId + "/sigilo-inteligente",
                    destinatario.rationale()
            );
            notificacoes.add(item);
            canais.addAll(item.channels());
        }

        notificacoes.sort(Comparator.comparing(ProcessoSigiloNotificacaoItem::audienceCode)
                .thenComparing(item -> item.usuarioId() == null ? Long.MAX_VALUE : item.usuarioId()));

        long totalComUsuario = notificacoes.stream().filter(item -> item.usuarioId() != null).count();
        long totalAltaPrioridade = notificacoes.stream().filter(ProcessoSigiloNotificacaoItem::highPriority).count();
        return new ProcessoSigiloNotificacaoAggregate(
                diagnostico.identity(),
                diagnostico.destinatarios().isEmpty() ? "SEM_DESTINATARIOS_ELEGIVEIS" : "PLANO_PRONTO",
                notificacoes.size(),
                totalComUsuario,
                totalAltaPrioridade,
                List.copyOf(canais),
                List.copyOf(notificacoes),
                List.copyOf(fundamentos),
                Instant.now()
        );
    }

    public ProcessoSigiloNotificacaoAggregate notificar(Long processoId) {
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        ProcessoSigiloNotificacaoAggregate planejamento = planejar(processoId);
        for (ProcessoSigiloNotificacaoItem item : planejamento.notificacoes()) {
            if (item.usuarioId() == null) {
                continue;
            }
            Usuario usuario = usuarioRepository.findById(item.usuarioId()).orElse(null);
            if (usuario == null) {
                continue;
            }
            notificationService.notifyUserAdvanced(
                    usuario,
                    processo,
                    item.title(),
                    item.message(),
                    item.deepLink(),
                    item.highPriority()
            );
        }
        return planejamento;
    }

    private String titulo(ProcessoSigiloInteligenteAggregate diagnostico, ProcessoSigiloDestinatario destinatario) {
        if ("MAGISTRADO_NATURAL".equals(destinatario.audienceCode())) {
            return "Revisão judicial de sigilo pendente";
        }
        if (diagnostico.operacaoPolicialSigilosa()) {
            return "Ciência restrita de operação sigilosa";
        }
        return "Preparação institucional de sigilo";
    }

    private String mensagem(ProcessoSigiloInteligenteAggregate diagnostico,
                            ProcessoSigiloDestinatario destinatario,
                            Processo processo) {
        String numero = processo.getNumeroProcesso() == null ? String.valueOf(processo.getId()) : processo.getNumeroProcesso();
        if ("MAGISTRADO_NATURAL".equals(destinatario.audienceCode())) {
            return "Processo " + numero + " exige revisão judicial para classificar ou manter sigilo em nível "
                    + diagnostico.nivelRecomendado().name() + ".";
        }
        if (diagnostico.operacaoPolicialSigilosa()) {
            return "Processo " + numero + " entrou em audiência restrita por operação policial sensível. A ciência inicial foi limitada ao anel estritamente necessário.";
        }
        return "Processo " + numero + " foi marcado para guardas de sigilo, mascaramento documental e ciência controlada do ambiente institucional.";
    }
}
