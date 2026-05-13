package com.tcc.pjb.backend.core.frontend.virtualcounter;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class PjbSmartVirtualCounterService {

    public PjbVirtualCounterRoutingDecision route(String userMessage, boolean hasProcessNumber, boolean vulnerableUser) {
        String token = Objects.toString(userMessage, "").toLowerCase(Locale.ROOT);
        PjbVirtualCounterIntent intent = resolveIntent(token, hasProcessNumber);
        String destination = switch (intent) {
            case FIND_PROCESS -> "public.process.search";
            case FILE_NEW_CLAIM -> vulnerableUser ? "juizado.atermacao.assistida" : "peticionamento.inicial";
            case UNDERSTAND_NOTICE -> "citizen.notice.guide";
            case TALK_TO_SECRETARIAT -> "secretariat.virtual.counter";
            case VERIFY_DOCUMENT -> "public.document.verification";
            case REQUEST_LEGAL_AID -> "defensoria.triage";
            case HEARING_SUPPORT -> "digital.hearing.support";
            case UNKNOWN -> "public.portal";
        };
        boolean human = vulnerableUser || intent == PjbVirtualCounterIntent.FILE_NEW_CLAIM || intent == PjbVirtualCounterIntent.UNKNOWN;
        return new PjbVirtualCounterRoutingDecision(intent, destination, human, questions(intent));
    }

    private PjbVirtualCounterIntent resolveIntent(String token, boolean hasProcessNumber) {
        if (token.contains("documento") || token.contains("codigo") || token.contains("qr")) {
            return PjbVirtualCounterIntent.VERIFY_DOCUMENT;
        }
        if (token.contains("intimacao") || token.contains("citado") || token.contains("prazo")) {
            return PjbVirtualCounterIntent.UNDERSTAND_NOTICE;
        }
        if (token.contains("audiencia") || token.contains("videoconferencia")) {
            return PjbVirtualCounterIntent.HEARING_SUPPORT;
        }
        if (token.contains("defensoria") || token.contains("advogado") || token.contains("gratuita")) {
            return PjbVirtualCounterIntent.REQUEST_LEGAL_AID;
        }
        if (token.contains("entrar") || token.contains("processar") || token.contains("pedido")) {
            return PjbVirtualCounterIntent.FILE_NEW_CLAIM;
        }
        if (token.contains("secretaria") || token.contains("balcao") || token.contains("atendimento")) {
            return PjbVirtualCounterIntent.TALK_TO_SECRETARIAT;
        }
        if (hasProcessNumber || token.contains("processo")) {
            return PjbVirtualCounterIntent.FIND_PROCESS;
        }
        return PjbVirtualCounterIntent.UNKNOWN;
    }

    private List<String> questions(PjbVirtualCounterIntent intent) {
        return switch (intent) {
            case FIND_PROCESS -> List.of("qual é o número do processo?", "você precisa consultar andamento ou documento?");
            case FILE_NEW_CLAIM -> List.of("o que aconteceu?", "quando aconteceu?", "contra quem é o pedido?", "você tem documentos?");
            case UNDERSTAND_NOTICE -> List.of("qual documento você recebeu?", "há prazo indicado?", "você quer responder ou apenas entender?");
            case TALK_TO_SECRETARIAT -> List.of("qual unidade judicial?", "o processo é urgente?", "você já tentou consulta pública?");
            case VERIFY_DOCUMENT -> List.of("informe código, QR Code ou hash do documento");
            case REQUEST_LEGAL_AID -> List.of("qual cidade?", "há audiência ou prazo próximo?", "você possui renda familiar aproximada?");
            case HEARING_SUPPORT -> List.of("qual a data da audiência?", "você recebeu link?", "precisa testar câmera ou microfone?");
            case UNKNOWN -> List.of("você quer consultar processo, entrar com pedido, verificar documento ou falar com a secretaria?");
        };
    }
}
