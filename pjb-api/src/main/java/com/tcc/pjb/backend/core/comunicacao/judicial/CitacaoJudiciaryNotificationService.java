package com.tcc.pjb.backend.core.comunicacao.judicial;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.platform.jusos.v2.notificacao.NotificacaoInteligentePJB;
import org.springframework.stereotype.Service;

@Service
public class CitacaoJudiciaryNotificationService {

    private final UsuarioRepository usuarioRepository;
    private final NotificacaoInteligentePJB notificacaoEngine;

    public CitacaoJudiciaryNotificationService(UsuarioRepository usuarioRepository, NotificacaoInteligentePJB notificacaoEngine) {
        this.usuarioRepository = usuarioRepository;
        this.notificacaoEngine = notificacaoEngine;
    }

    public void notificarJuizEvasao(ExpedicaoJudicial expedicao, Processo processo) {
        if (expedicao.getJuizResponsavelId() == null) {
            return;
        }
        usuarioRepository.findById(expedicao.getJuizResponsavelId()).ifPresent(juiz -> {
            NotificacaoInteligentePJB.NotificacaoPJB notif = notificacaoEngine.construir(
                    juiz.getId(),
                    expedicao.getProcessoId(),
                    NotificacaoInteligentePJB.TipoAlerta.ALERTA_REGRA_CRITICA,
                    NotificacaoInteligentePJB.UrgenciaMensagem.CRITICA,
                    NotificacaoInteligentePJB.CanalNotificacao.PUSH_APP_PJB
            );
            notificacaoEngine.enviarNotificacao(notif);
        });
    }

    public void notificarJuizEdital(ExpedicaoJudicial expedicao, Processo processo, String numeroEdital) {
        if (expedicao.getJuizResponsavelId() == null) {
            return;
        }
        usuarioRepository.findById(expedicao.getJuizResponsavelId()).ifPresent(juiz -> {
            NotificacaoInteligentePJB.NotificacaoPJB notif = notificacaoEngine.construir(
                    juiz.getId(),
                    expedicao.getProcessoId(),
                    NotificacaoInteligentePJB.TipoAlerta.MOVIMENTACAO_NOVA,
                    NotificacaoInteligentePJB.UrgenciaMensagem.ALTA,
                    NotificacaoInteligentePJB.CanalNotificacao.PUSH_APP_PJB
            );
            notificacaoEngine.enviarNotificacao(notif);
        });
    }

    public void notificarServidor(Long servidorId, ExpedicaoJudicial expedicao, Processo processo, String mensagem) {
        usuarioRepository.findById(servidorId).ifPresent(servidor -> {
            NotificacaoInteligentePJB.NotificacaoPJB notif = notificacaoEngine.construir(
                    servidor.getId(),
                    expedicao.getProcessoId(),
                    NotificacaoInteligentePJB.TipoAlerta.MOVIMENTACAO_NOVA,
                    NotificacaoInteligentePJB.UrgenciaMensagem.MEDIA,
                    NotificacaoInteligentePJB.CanalNotificacao.PUSH_APP_PJB
            );
            notificacaoEngine.enviarNotificacao(notif);
        });
    }
}
