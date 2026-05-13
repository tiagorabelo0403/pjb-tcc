package com.tcc.pjb.backend.modules.atendimento.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class AtendimentoChatServiceRefinementArchitectureTest {

    @Test
    void serviceDeveDelegarAcessoViewsEMensageriaParaSuportesDedicados() {
        Constructor<?> constructor = Arrays.stream(AtendimentoChatService.class.getDeclaredConstructors())
                .findFirst()
                .orElseThrow();

        Set<Class<?>> parameterTypes = Arrays.stream(constructor.getParameterTypes()).collect(Collectors.toSet());

        assertThat(constructor.getParameterCount()).isLessThanOrEqualTo(21);
        assertThat(parameterTypes)
                .contains(AtendimentoChatAccessSupport.class, AtendimentoChatThreadViewSupport.class, AtendimentoChatMessagingSupport.class);
    }

    @Test
    void serviceNaoDeveReabsorverHeuristicasExtraidas() {
        Set<String> methodNames = Arrays.stream(AtendimentoChatService.class.getDeclaredMethods())
                .map(method -> method.getName())
                .collect(Collectors.toSet());

        assertThat(methodNames)
                .doesNotContain(
                        "validateAttachments",
                        "publishInboxEvent",
                        "publishUiInboxNotificationNewMessage",
                        "notifyReadState",
                        "notifyDeliveredState",
                        "resolveInboxTokens",
                        "loadThreadsPage",
                        "loadProcessos",
                        "loadUsers",
                        "loadChecklistAgg",
                        "safeChecklistAgg",
                        "hasUnread",
                        "resolveCidadao",
                        "enforceThreadAccess",
                        "safeTitle",
                        "topicForUser"
                );
    }
}
