package com.tcc.pjb.backend.adapter.strategies.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;
import com.tcc.pjb.backend.tracker.UserActivitySocketHandler;

@Configuration
@EnableWebSocket
@ConditionalOnBean(UserActivitySocketHandler.class)
public class WebSocketConfig implements WebSocketConfigurer {

    private final UserActivitySocketHandler socketHandler;

    public WebSocketConfig(UserActivitySocketHandler socketHandler) {
        this.socketHandler = socketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(socketHandler, "/ws/activity")
                .setAllowedOriginPatterns("*")
                .withSockJS()
                .setSessionCookieNeeded(false);
    }
}