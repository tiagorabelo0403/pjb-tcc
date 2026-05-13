package com.tcc.pjb.backend.core.comunicacao.institucional.integration.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.integration.domain.InstitutionalIntegrationContractDescriptor;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InstitutionalIntegrationContractCatalogApplicationService {

    @Transactional(readOnly = true)
    public List<InstitutionalIntegrationContractDescriptor> list() {
        List<String> required = List.of("contractId", "provider", "canonicalAct", "expedicaoUuid", "tipoComunicacao", "canal", "payload.destinatario", "payload.processoNumero", "payload.hashIntegridade");
        List<String> optional = List.of("payload.unidadeCodigo", "payload.caixaCodigo", "payload.gateCode", "payload.slaCienciaHoras", "payload.slaRespostaHoras", "payload.fallbacks", "payload.metadata");
        List<String> guarantees = List.of("HTTPS", "idempotencia", "assinatura_payload", "correlation_id", "reenvio_controlado", "auditoria_outbox");
        return List.of(
                new InstitutionalIntegrationContractDescriptor("DOMICILIO_JUDICIAL_ELETRONICO", "DOMICILIO_JUDICIAL_ELETRONICO", "v1", "SHA256withRSA", required, optional, guarantees, "contractId", "payload.expedicaoUuid", "Contrato formal para citação/intimação pessoal institucional."),
                new InstitutionalIntegrationContractDescriptor("DJEN", "DJEN", "v1", "SHA256withRSA", required, optional, guarantees, "contractId", "payload.expedicaoUuid", "Contrato formal para comunicações publicadas quando não houver pessoalidade."),
                new InstitutionalIntegrationContractDescriptor("WEBHOOK_INSTITUCIONAL", "WEBHOOK_INSTITUCIONAL", "v1", "HMAC-SHA256", required, optional, guarantees, "contractId", "payload.expedicaoUuid", "Contrato para conveniados e grandes órgãos com integração API/webhook."),
                new InstitutionalIntegrationContractDescriptor("COMUNICACAO_FISICA_OFICIAL", "COMUNICACAO_FISICA_OFICIAL", "v1", "SHA256withRSA", required, optional, List.of("protocolo_expedicao", "lote_fisico", "rastreamento", "auditoria_outbox"), "contractId", "payload.expedicaoUuid", "Contrato espelhado para integração com comunicação física oficial e acompanhamento logístico.")
        );
    }
}
