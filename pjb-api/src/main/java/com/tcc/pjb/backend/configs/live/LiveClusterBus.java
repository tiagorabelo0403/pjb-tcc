package com.tcc.pjb.backend.configs.live;

import java.util.function.Consumer;

public interface LiveClusterBus {

    void publish(String namespace, LiveClusterEvent event);

    void registerHandler(String namespace, Consumer<LiveClusterEvent> handler);

    boolean enabled();
}
