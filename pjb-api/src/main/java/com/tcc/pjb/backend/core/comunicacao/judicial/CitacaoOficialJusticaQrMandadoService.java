package com.tcc.pjb.backend.core.comunicacao.judicial;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class CitacaoOficialJusticaQrMandadoService {

    private final ObjectProvider<QrCodeMandadoService> qrCodeProvider;

    public CitacaoOficialJusticaQrMandadoService(ObjectProvider<QrCodeMandadoService> qrCodeProvider) {
        this.qrCodeProvider = qrCodeProvider;
    }

    public void gerarSeCabivel(ExpedicaoJudicial expedicao) {
        if (expedicao == null || expedicao.getModalidade() != ModalidadeExpedicaoJudicial.OFICIAL_JUSTICA_ROTA_OTIMIZADA) {
            return;
        }
        QrCodeMandadoService service = qrCodeProvider.getIfAvailable();
        if (service != null) {
            service.gerar(expedicao.getExpedicaoUuid());
        }
    }
}
